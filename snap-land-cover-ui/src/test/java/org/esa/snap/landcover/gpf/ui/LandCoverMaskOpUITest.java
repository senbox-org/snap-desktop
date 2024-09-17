package org.esa.snap.landcover.gpf.ui;

import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.ui.AppContext;
import org.junit.Before;
import org.junit.Test;
import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LandCoverMaskOpUITest {

    private LandCoverMaskOpUI landCoverMaskOpUI;
    private AppContext appContext;

    @Before
    public void setUp() {
        landCoverMaskOpUI = new LandCoverMaskOpUI();
        appContext = mock(AppContext.class);
    }

    @Test
    public void createOpTab_initializesComponentsCorrectly() {
        Map<String, Object> paramMap = new HashMap<>();
        JComponent panel = landCoverMaskOpUI.CreateOpTab("Land-Cover-Mask", paramMap, appContext);

        assertNotNull(panel);
        assertEquals(1, ((JScrollPane) panel).getViewport().getComponentCount());
    }

    @Test
    public void initParameters_initializesParametersCorrectly() {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("landCoverBand", "Band1");
        paramMap.put("validPixelExpression", "expression");
        paramMap.put("includeOtherBands", true);

        landCoverMaskOpUI.CreateOpTab("Land-Cover-Mask", paramMap, appContext);
        landCoverMaskOpUI.initParameters();

        assertEquals("expression", landCoverMaskOpUI.validPixelExpressionText.getText());
        assertTrue(landCoverMaskOpUI.includeOtherBandsCheckBox.isSelected());
    }

    @Test
    public void updateParameters_updatesParameterMapCorrectly() {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("landCoverBand", "Band1");
        paramMap.put("validPixelExpression", "expression");
        paramMap.put("includeOtherBands", true);

        landCoverMaskOpUI.CreateOpTab("Land-Cover-Mask", paramMap, appContext);

        landCoverMaskOpUI.updateParameters();

        Map<String, Object> paramMap2 = landCoverMaskOpUI.getParameters();
        assertTrue(paramMap2.containsKey("landCoverBand"));
        assertTrue(paramMap2.containsKey("validPixelExpression"));
        assertTrue(paramMap2.containsKey("includeOtherBands"));
    }

    @Test
    public void validateParameters_returnsOkValidation() {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("landCoverBand", "Band1");
        paramMap.put("validPixelExpression", "expression");
        paramMap.put("includeOtherBands", true);

        landCoverMaskOpUI.CreateOpTab("Land-Cover-Mask", paramMap, appContext);

        UIValidation validation = landCoverMaskOpUI.validateParameters();

        assertEquals(UIValidation.State.OK, validation.getState());
    }
}
