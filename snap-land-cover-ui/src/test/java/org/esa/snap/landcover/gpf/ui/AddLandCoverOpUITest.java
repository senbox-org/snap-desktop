package org.esa.snap.landcover.gpf.ui;

import com.bc.ceres.annotation.STTM;
import com.bc.ceres.core.runtime.internal.IOHelper;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.ui.AppContext;
import org.junit.Before;
import org.junit.Test;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class AddLandCoverOpUITest {

    private AddLandCoverOpUI addLandCoverOpUI;
    private AppContext appContext;

    @Before
    public void setUp() {
        addLandCoverOpUI = new AddLandCoverOpUI();
        appContext = mock(AppContext.class);
    }

    @Test
    public void createOpTab_initializesComponentsCorrectly() {
        Map<String, Object> paramMap = new HashMap<>();
        JComponent panel = addLandCoverOpUI.CreateOpTab("AddLandCover", paramMap, appContext);

        assertNotNull(panel);
        assertEquals(1, ((JScrollPane) panel).getViewport().getComponentCount());
    }

    @Test
    public void populateNamesTree_populatesTreeWithLandCoverModels() {
        addLandCoverOpUI.populateNamesTree();

        JTree landCoverNamesTree = addLandCoverOpUI.landCoverNamesTree;
        assertTrue(landCoverNamesTree.getRowCount() > 0);
        assertEquals("Land Cover Models", landCoverNamesTree.getModel().getRoot().toString());
    }

    @Test
    @STTM("SNAP-3838")
    public void initParameters_initializesParametersCorrectly() {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("landCoverNames", new String[]{"Model1", "Model2"});
        paramMap.put("resamplingMethod", "Nearest");
        Path tempDir = IOHelper.createATempDir();
        paramMap.put("externalFiles", new Path[]{tempDir.resolve("file1"), tempDir.resolve("file2")});

        addLandCoverOpUI.CreateOpTab("AddLandCover", paramMap, appContext);
        addLandCoverOpUI.initParameters();

        assertEquals(2, addLandCoverOpUI.landCoverNamesTree.getSelectionCount());
        assertEquals(2, addLandCoverOpUI.externalFileList.getModel().getSize());
    }

    @Test
    public void updateParameters_updatesParameterMapCorrectly() {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("landCoverNames", new String[]{"Model1", "Model2"});
        paramMap.put("resamplingMethod", "Nearest");
        addLandCoverOpUI.CreateOpTab("AddLandCover", paramMap, appContext);

        addLandCoverOpUI.updateParameters();

        Map<String, Object> paramMap2 = addLandCoverOpUI.getParameters();
        assertTrue(paramMap2.containsKey("landCoverNames"));
        assertTrue(paramMap2.containsKey("resamplingMethod"));
    }

    @Test
    public void validateParameters_returnsOkValidation() {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("landCoverNames", new String[]{"Model1", "Model2"});
        paramMap.put("resamplingMethod", "Nearest");
        addLandCoverOpUI.CreateOpTab("AddLandCover", paramMap, appContext);

        UIValidation validation = addLandCoverOpUI.validateParameters();

        assertEquals(UIValidation.State.OK, validation.getState());
    }
}
