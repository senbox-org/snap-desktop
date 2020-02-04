package org.esa.snap.graphbuilder.rcp.dialogs.support;

import org.esa.snap.core.util.SystemUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActionFileSystem {

    final Map<String, FileObject> operatorFileMap = new HashMap<>();

    public ActionFileSystem() {
        this("Actions/Operators");
    }

    public ActionFileSystem(final String path) {
        final FileObject fileObj = FileUtil.getConfigFile(path);
        if (fileObj == null) {
            return;
        }
        final FileObject[] files = fileObj.getChildren();
        for (FileObject file : files) {
            final String operatorName = (String) file.getAttribute("operatorName");
            if(operatorName != null) {
                operatorFileMap.put(operatorName, file);
            }
        }
    }

    public FileObject getOperatorFile(final String opName) {
        return operatorFileMap.get(opName);
    }

    public static FileObject findOperatorFile(final String opName) {
        final FileObject fileObj = FileUtil.getConfigFile("Actions/Operators");
        if (fileObj == null) {
            SystemUtils.LOG.warning("No Operator Actions found.");
            return null;
        }
        final FileObject[] files = fileObj.getChildren();
        final List<FileObject> orderedFiles = FileUtil.getOrder(Arrays.asList(files), true);
        for (FileObject file : orderedFiles) {
            final String operatorName = (String) file.getAttribute("operatorName");
            if(opName.equals(operatorName)) {
                return file;
            }
        }
        return null;
    }
}
