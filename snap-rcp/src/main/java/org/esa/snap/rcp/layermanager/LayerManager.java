package org.esa.snap.rcp.layermanager;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ExtensionFactory;
import com.bc.ceres.core.ExtensionManager;
import com.bc.ceres.core.SingleTypeExtensionFactory;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import org.esa.snap.ui.layer.DefaultLayerSourceDescriptor;
import org.esa.snap.ui.layer.LayerEditor;
import org.esa.snap.ui.layer.LayerSource;
import org.esa.snap.ui.layer.LayerSourceDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.ModuleInfo;
import org.openide.modules.OnStart;
import org.openide.util.Lookup;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Either 'editor' or 'editorFactory' must be given:
 * <ul>
 * <li>'editor' must be a <code>LayerEditor</code></li>
 * <li>'editorFactory' must be a <code>com.bc.ceres.core.ExtensionFactory</code> that produces
 * instances of <code>LayerEditor</code>.</li>
 * </ul>
 * <pre>
 * &lt;editor field="layerEditorClass" type="java.lang.Class"/&gt;
 * &lt;editorFactory field="layerEditorFactoryClass" type="java.lang.Class"/&gt;
 * </pre>
 * At least 'layer' or 'layerType' must be given:
 * <ul>
 * <li>'layer' must be a <code>com.bc.ceres.glayer.Layer</code></li>
 * <li>'layerType' must be a <code>com.bc.ceres.glayer.LayerType</code>.</li>
 * </ul>
 * <pre>
 * &lt;layer field="layerClass" type="java.lang.Class"/&gt;
 * &lt;layerType field="layerTypeClass" type="java.lang.Class"/&gt;
 * </pre>
 *
 * @author Norman Fomferra
 */
public class LayerManager {

    public static final Logger LOG = Logger.getLogger(LayerManager.class.getName());
    private static LayerManager layerManager;
    private final Map<String, LayerSourceDescriptor> layerSourceDescriptors;

    public static LayerManager getDefault() {
        if (layerManager == null) {
            layerManager = new LayerManager();
        }
        return layerManager;
    }

    public Map<String, LayerSourceDescriptor> getLayerSourceDescriptors() {
        return Collections.unmodifiableMap(layerSourceDescriptors);
    }

    protected LayerManager() {
        registerLayerEditors();
        this.layerSourceDescriptors = lookupLayerSourceDescriptors();
    }

    private static Map<String, LayerSourceDescriptor> lookupLayerSourceDescriptors() {
        Map<String, LayerSourceDescriptor> layerSourceDescriptors = new LinkedHashMap<>();
        FileObject[] files = FileUtil.getConfigFile("LayerSources").getChildren();
        //System.out.println("Files in SNAP/LayerSources: " + Arrays.toString(files));
        List<FileObject> orderedFiles = FileUtil.getOrder(Arrays.asList(files), true);
        for (FileObject file : orderedFiles) {
            LayerSourceDescriptor layerSourceDescriptor = null;
            try {
                layerSourceDescriptor = createLayerSourceDescriptor(file);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, String.format("Failed to create layer source from layer.xml path '%s'", file.getPath()), e);
            }
            if (layerSourceDescriptor != null) {
                layerSourceDescriptors.put(layerSourceDescriptor.getId(), layerSourceDescriptor);
                LOG.info(String.format("New layer source added from layer.xml path '%s': %s",
                                       file.getPath(), layerSourceDescriptor.getName()));
            }
        }
        return layerSourceDescriptors;
    }

    public static DefaultLayerSourceDescriptor createLayerSourceDescriptor(FileObject fileObject) {
        String id = fileObject.getName();
        String name = (String) fileObject.getAttribute("displayName");
        String description = (String) fileObject.getAttribute("description");
        Class<? extends LayerSource> layerSourceClass = getClassAttribute(fileObject, "layerSourceClass", LayerSource.class, false);
        String layerTypeClassName = (String) fileObject.getAttribute("layerTypeClass");
        Assert.argument(name != null && !name.isEmpty(), "Missing attribute 'displayName'");
        Assert.argument(layerSourceClass != null || layerTypeClassName != null, "Either attribute 'class' or 'layerType' must be provided");
        if (layerSourceClass != null) {
            return new DefaultLayerSourceDescriptor(id, name, description, layerSourceClass);
        } else {
            return new DefaultLayerSourceDescriptor(id, name, description, layerTypeClassName);
        }
    }

    private static void registerLayerEditors() {
        FileObject[] files = FileUtil.getConfigFile("LayerEditors").getChildren();
        //System.out.println("Files in SNAP/LayerEditors: " + Arrays.toString(files));
        List<FileObject> orderedFiles = FileUtil.getOrder(Arrays.asList(files), true);
        for (FileObject file : orderedFiles) {
            try {
                registerLayerEditorDescriptor(file);
                LOG.info(String.format("New layer editor registered from layer.xml path '%s'", file.getPath()));
            } catch (Exception e) {
                LOG.log(Level.SEVERE, String.format("Failed to register layer editor from layer.xml path '%s'", file.getPath()), e);
            }
        }
    }

    public static void registerLayerEditorDescriptor(FileObject fileObject) throws Exception {
        Class<? extends LayerEditor> editorClass = getClassAttribute(fileObject, "editorClass", LayerEditor.class, false);
        Class<? extends ExtensionFactory> editorFactoryClass = getClassAttribute(fileObject, "editorFactoryClass", ExtensionFactory.class, false);
        Assert.argument(editorClass != null || editorFactoryClass != null,
                        "Either 'editorClass' or 'editorFactoryClass' attributes must be given");
        Class<? extends Layer> layerClass = getClassAttribute(fileObject, "layerClass", Layer.class, false);
        Class<? extends LayerType> layerTypeClass = getClassAttribute(fileObject, "layerTypeClass", LayerType.class, false);
        Assert.argument(layerClass != null || layerTypeClass != null,
                        "Either 'layerClass' or 'layerTypeClass' attributes must be given");
        if (layerClass != null) {
            ExtensionManager.getInstance().register(layerClass, createExtensionFactory(editorClass, editorFactoryClass));
        } else {
            ExtensionManager.getInstance().register(layerTypeClass, createExtensionFactory(editorClass, editorFactoryClass));
        }
    }

    public static <T> Class<T> getClassAttribute(FileObject fileObject,
                                                 String attributeName,
                                                 Class<T> expectedType,
                                                 boolean required) {
        String className = (String) fileObject.getAttribute(attributeName);
        if (className == null || className.isEmpty()) {
            if (required) {
                throw new IllegalArgumentException(String.format("Missing attribute '%s' of type %s",
                                                                 attributeName, expectedType.getName()));
            }
            return null;
        }

        Collection<? extends ModuleInfo> modules = Lookup.getDefault().lookupAll(ModuleInfo.class);
        for (ModuleInfo module : modules) {
            if (module.isEnabled()) {
                try {
                    Class<?> implClass = module.getClassLoader().loadClass(className);
                    if (expectedType.isAssignableFrom(implClass)) {
                        //noinspection unchecked
                        return (Class<T>) implClass;
                    } else {
                        throw new IllegalArgumentException(String.format("Value %s of attribute '%s' must be a %s",
                                                                         implClass.getName(),
                                                                         attributeName,
                                                                         expectedType.getName()));
                    }
                } catch (ClassNotFoundException e) {
                    // it's ok, continue
                }
            }
        }
        return null;
    }

    /**
     * Creates an extension factory that maps an instances of a {@link com.bc.ceres.glayer.Layer} or
     * a {@link com.bc.ceres.glayer.LayerType} to an instance of a {@link LayerEditor}.
     */
    private static ExtensionFactory createExtensionFactory(Class<? extends LayerEditor> editorClass, Class<? extends ExtensionFactory> editorFactoryClass) throws Exception {
        if (editorClass != null) {
            return new SingleTypeExtensionFactory<LayerType, LayerEditor>(LayerEditor.class, editorClass);
        }
        if (editorFactoryClass != null) {
            try {
                return editorFactoryClass.newInstance();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        // should never get here
        throw new IllegalStateException();
    }

    @SuppressWarnings("UnusedDeclaration")
    @OnStart
    public static class Runner implements Runnable {
        @Override
        public void run() {
            // test!
            LayerManager.getDefault();
        }
    }
}
