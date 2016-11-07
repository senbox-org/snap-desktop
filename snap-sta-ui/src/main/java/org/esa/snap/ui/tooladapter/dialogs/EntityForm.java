package org.esa.snap.ui.tooladapter.dialogs;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.core.Assert;
import com.bc.ceres.swing.binding.PropertyPane;
import org.esa.snap.core.gpf.descriptor.annotations.Folder;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.utils.PrivilegedAccessor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Created by kraftek on 11/2/2016.
 */
public class EntityForm<T> {
    private Class<T> entityType;
    private Set<String> fieldNames;
    private Map<String, Annotation[]> annotatedFields;
    private T original;
    private T modified;
    private JPanel panel;

    public EntityForm(T object) {
        Assert.notNull(object);
        this.original = object;
        this.entityType = (Class<T>) this.original.getClass();
        Field[] fields = this.entityType.getDeclaredFields();
        this.fieldNames = new HashSet<>();
        this.annotatedFields = new HashMap<>();
        for (Field field : fields) {
            this.fieldNames.add(field.getName());
            Annotation[] annotations = field.getAnnotations();
            if (annotations != null) {
                this.annotatedFields.put(field.getName(), annotations);
            }
        }
        try {
            this.modified = duplicate(this.original, this.modified, false);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        buildUI();
    }

    public JPanel getPanel() {
        return this.panel;
    }

    public T applyChanges() {
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
        for (String fieldName : this.fieldNames) {
            Object sourceValue = PrivilegedAccessor.getValue(source, fieldName);
            if (useSetters) {
                try {
                    PrivilegedAccessor.invokeMethod(target, "set" + StringUtils.firstLetterUp(fieldName), sourceValue);
                } catch (Exception ignored) {
                    // if no setter, then set the field directly
                    PrivilegedAccessor.setValue(target, fieldName, sourceValue);
                }
            } else {
                PrivilegedAccessor.setValue(target, fieldName, sourceValue);
            }

        }
        return target;
    }

    private void buildUI() {
        PropertyContainer propertyContainer = PropertyContainer.createObjectBacked(this.modified);
        for (String field : this.fieldNames) {
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

        propertyContainer.addPropertyChangeListener(evt -> {
            try {
                PrivilegedAccessor.invokeMethod(modified, "set" + StringUtils.firstLetterUp(evt.getPropertyName()), evt.getNewValue());
            } catch (Exception e) {
                try {
                    PrivilegedAccessor.setValue(modified, evt.getPropertyName(), evt.getNewValue());
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
        PropertyPane parametersPane = new PropertyPane(propertyContainer);
        this.panel = parametersPane.createPanel();
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
}
