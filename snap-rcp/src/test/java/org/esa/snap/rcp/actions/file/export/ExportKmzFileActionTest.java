package org.esa.snap.rcp.actions.file.export;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Config;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class ExportKmzFileActionTest {

    @Test
    @STTM("SNAP-237")
    public void testSetGetLastDir() {
        final String oldValue = Config.instance().load().preferences().get(AbstractExportImageAction.IMAGE_EXPORT_DIR_PREFERENCES_KEY, null);

        try {
            final String expectedDefault = SystemUtils.getUserHomeDir().getPath();

            String lastDir = ExportKmzFileAction.getLastDir();
            assertEquals(expectedDefault, lastDir);

            final Path newPath = Paths.get(expectedDefault).resolve("subDir");
            ExportKmzFileAction.setLastDir(newPath.toFile());

            lastDir = ExportKmzFileAction.getLastDir();
            assertEquals(newPath.toString(), lastDir);
        } finally {
            if (oldValue != null) {
                Config.instance().load().preferences().put(AbstractExportImageAction.IMAGE_EXPORT_DIR_PREFERENCES_KEY, oldValue);
            }
        }
    }
}
