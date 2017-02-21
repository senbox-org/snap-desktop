package org.esa.snap.ui.tooladapter.dialogs;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.core.Assert;
import com.bc.ceres.swing.binding.Binding;
import com.bc.ceres.swing.binding.PropertyPane;
import org.esa.snap.core.gpf.descriptor.annotations.Folder;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.utils.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Created by kraftek on 11/2/2016.
 */
class EntityForm<T> {

    static class FieldDependency {
        private String targetFieldName;
        Function<Object, Object> action;

        FieldDependency(String targetFieldName, Function<Object, Object> action) {
            this.targetFieldName = targetFieldName;
            this.action = action;
        }

        String getTargetFieldName() {
            return targetFieldName;
        }

        Function<Object, Object> getAction() {
            return action;
        }
    }

    private Class<T> entityType;
    private Map<String, FieldDependency> fieldMap;
    private Map<String, Annotation[]> annotatedFields;
    private T original;
    private T modified;
    private JPanel panel;

    EntityForm(T object) {
        this(object, null);
    }

    EntityForm(T object, Map<String, FieldDependency> dependentFieldsActions) {
        Assert.notNull(object);
        this.original = object;
        this.entityType = (Class<T>) this.original.getClass();
        Field[] fields = this.entityType.getDeclaredFields();
        this.fieldMap = new HashMap<>();
        this.annotatedFields = new HashMap<>();
        for (Field field : fields) {
            String fieldName = field.getName();
            FieldDependency dependency = dependentFieldsActions != null ?
                    dependentFieldsActions.get(fieldName) : null;
            this.fieldMap.put(fieldName, dependency);
            Annotation[] annotations = field.getAnnotations();
            if (annotations != null) {
                this.annotatedFields.put(fieldName, annotations);
            }
        }
        try {
            this.modified = duplicate(this.original, this.modified, false);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        buildUI();
    }

    JPanel getPanel() {
        return this.panel;
    }

    T applyChanges() {
        try {
            this.original = duplicate(this.modified, this.original, true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return this.original;
    }

    private T duplicate(T source, T target, boolean useSetters) throws Exception {
        if (target == null) {
            Constructor<T> constructor = this.entityType.getConstructor();
            target = constructor.newInstance();
        }
        for (String fieldName : this.fieldMap.keySet()) {
            Object sourceValue = getValue(source, fieldName);
            if (useSetters) {
                try {
                    invokeMethod(target, "set" + StringUtils.firstLetterUp(fieldName), sourceValue);
                } catch (Exception ignored) {
                    // if no setter, then set the field directly
                    setValue(target, fieldName, sourceValue);
                }
            } else {
                setValue(target, fieldName, sourceValue);
            }

        }
        return target;
    }

    private void buildUI() {
        PropertyContainer propertyContainer = PropertyContainer.createObjectBacked(this.modified);
        for (String field : this.fieldMap.keySet()) {
            if (this.annotatedFields.containsKey(field)) {
                Annotation[] annotations = this.annotatedFields.get(field);
                Optional<Annotation> annotation = Arrays.stream(annotations)
                                                        .filter(a -> a.annotationType().equals(Folder.class))
                                                        .findFirst();
                if (annotation.isPresent()) {
                    try {
                        if (File.class.isAssignableFrom(this.entityType.getDeclaredField(field).getType())) {
                            propertyContainer.getDescriptor(field).setAttribute("directory", true);
                        }
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        PropertyPane parametersPane = new PropertyPane(propertyContainer);
        this.panel = parametersPane.createPanel();
        Arrays.stream(propertyContainer.getProperties())
                .forEach(p -> {
                    Arrays.stream(parametersPane.getBindingContext().getBinding(p.getName()).getComponents())
                            .forEach(c -> {
                                UIUtils.addPromptSupport(c, p);
                            });
                });
        propertyContainer.addPropertyChangeListener(evt -> {
            String propertyName = evt.getPropertyName();
            Object newValue = evt.getNewValue();
            try {
                invokeMethod(modified, "set" + StringUtils.firstLetterUp(propertyName), newValue);
            } catch (Exception e) {
                try {
                    setValue(modified, propertyName, newValue);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
            FieldDependency dependency = fieldMap.get(propertyName);
            if (dependency != null) {
                try {
                    Object newTargetValue = dependency.getAction().apply(newValue);
                    Binding binding = parametersPane.getBindingContext().getBinding(dependency.getTargetFieldName());
                    binding.setPropertyValue(newTargetValue);
                    binding.adjustComponents();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        this.panel.addPropertyChangeListener(evt -> {
            if (!(evt.getNewValue() instanceof JTextField)) return;
            JTextField field = (JTextField) evt.getNewValue();
            String text = field.getText();
            if (text != null && text.isEmpty()) {
                field.setCaretPosition(text.length());
            }
        });
        this.panel.setBorder(new EmptyBorder(4, 4, 4, 4));
    }

    private static Object invokeMethod(Object instance, String methodName, Object... args)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (args == null)
            args = new Object[] {};
        Class[] classTypes = getClassArray(args);
        Method[] methods = instance.getClass().getMethods();
        for (Method method : methods) {
            Class[] paramTypes = method.getParameterTypes();
            if (method.getName().equals(methodName) && compare(paramTypes, args)) {
                return method.invoke(instance, args);
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("No method named ").append(methodName).append(" found in ")
                .append(instance.getClass().getName()).append(" with parameters (");
        for (int x = 0; x < classTypes.length; x++) {
            sb.append(classTypes[x].getName());
            if (x < classTypes.length - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        throw new NoSuchMethodException(sb.toString());
    }

    private static Class[] getClassArray(Object[] args) {
        Class[] classTypes = null;
        if (args != null) {
            classTypes = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null)
                    classTypes[i] = args[i].getClass();
            }
        }
        return classTypes;
    }

    private static boolean compare(Class[] c, Object[] args) {
        if (c.length != args.length) {
            return false;
        }
        for (int i = 0; i < c.length; i++) {
            if (!c[i].isInstance(args[i])) {
                return false;
            }
        }
        return true;
    }

    private static Object getValue(Object instance, String fieldName)
            throws IllegalAccessException, NoSuchFieldException {
        Field field = getField(instance.getClass(), fieldName);
        field.setAccessible(true);
        return field.get(instance);
    }

    private static void setValue(Object instance, String fieldName, Object value)
            throws IllegalAccessException, NoSuchFieldException {
        Field field = getField(instance.getClass(), fieldName);
        field.setAccessible(true);
        field.set(instance, value);
    }

    private static Field getField(Class thisClass, String fieldName) throws NoSuchFieldException {
        if (thisClass == null)
            throw new NoSuchFieldException("Invalid field : " + fieldName);
        try {
            return thisClass.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            return getField(thisClass.getSuperclass(), fieldName);
        }
    }
}
