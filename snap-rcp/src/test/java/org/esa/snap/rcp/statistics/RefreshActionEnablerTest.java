package org.esa.snap.rcp.statistics;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import javax.swing.JButton;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class RefreshActionEnablerTest {

    private JButton refreshButton;

    @Before
    public void setUp() {
        refreshButton = new JButton();
        refreshButton.setEnabled(false);
    }

    @Test
    public void propertyChange_band_change() {
        PropertyContainer propertyContainer = new PropertyContainer();
        propertyContainer.addProperty(Property.create("xProduct", Product.class));
        propertyContainer.addProperty(Property.create("yProduct", Product.class));
        propertyContainer.addProperty(Property.create("zProduct", Product.class));
        propertyContainer.addProperty(Property.create("xBand", Band.class));
        propertyContainer.addProperty(Property.create("yBand", Band.class));
        propertyContainer.addProperty(Property.create("zBand", Band.class));
        RefreshActionEnabler refreshActionEnabler = new RefreshActionEnabler(refreshButton);
        refreshActionEnabler.addProductBandEnablement("xProduct", "xBand");
        refreshActionEnabler.addProductBandEnablement("yProduct", "yBand");
        refreshActionEnabler.addProductBandEnablement("zProduct", "zBand");
        propertyContainer.addPropertyChangeListener(refreshActionEnabler);

        propertyContainer.setValue("xBand", new Band("name", ProductData.TYPE_INT8, 1, 1));
        assertFalse(refreshButton.isEnabled());

        propertyContainer.setValue("yBand", new Band("name", ProductData.TYPE_INT8, 1, 1));
        assertFalse(refreshButton.isEnabled());

        propertyContainer.setValue("zBand", new Band("name", ProductData.TYPE_INT8, 1, 1));
        assertTrue(refreshButton.isEnabled());
    }

    @Test
    public void propertyChange_band_change_with_option() {
        PropertyContainer propertyContainer = new PropertyContainer();
        propertyContainer.addProperty(Property.create("xProduct", Product.class));
        propertyContainer.addProperty(Property.create("yProduct", Product.class));
        propertyContainer.addProperty(Property.create("zProduct", Product.class));
        propertyContainer.addProperty(Property.create("colorProduct", Product.class));
        propertyContainer.addProperty(Property.create("xBand", Band.class));
        propertyContainer.addProperty(Property.create("yBand", Band.class));
        propertyContainer.addProperty(Property.create("zBand", Band.class));
        propertyContainer.addProperty(Property.create("colorBand", Band.class));
        RefreshActionEnabler refreshActionEnabler = new RefreshActionEnabler(refreshButton);
        refreshActionEnabler.addProductBandEnablement("xProduct", "xBand");
        refreshActionEnabler.addProductBandEnablement("yProduct", "yBand");
        refreshActionEnabler.addProductBandEnablement("zProduct", "zBand");
        refreshActionEnabler.addProductBandEnablement("colorProduct", "colorBand", true);
        propertyContainer.addPropertyChangeListener(refreshActionEnabler);

        propertyContainer.setValue("colorBand", new Band("name", ProductData.TYPE_INT8, 1, 1));
        assertFalse(refreshButton.isEnabled());

        propertyContainer.setValue("xBand", new Band("name", ProductData.TYPE_INT8, 1, 1));
        assertFalse(refreshButton.isEnabled());

        propertyContainer.setValue("yBand", new Band("name", ProductData.TYPE_INT8, 1, 1));
        assertFalse(refreshButton.isEnabled());

        propertyContainer.setValue("colorBand", new Band("otherName", ProductData.TYPE_INT8, 1, 1));
        assertFalse(refreshButton.isEnabled());

        propertyContainer.setValue("zBand", new Band("name", ProductData.TYPE_INT8, 1, 1));
        assertTrue(refreshButton.isEnabled());

        propertyContainer.setValue("colorBand", null);
        assertTrue(refreshButton.isEnabled());
    }

    @Test
    public void propertyChange_product_change() {
        PropertyContainer propertyContainer = new PropertyContainer();
        propertyContainer.addProperty(Property.create("xProduct", Product.class));
        propertyContainer.addProperty(Property.create("yProduct", Product.class));
        propertyContainer.addProperty(Property.create("zProduct", Product.class));
        propertyContainer.addProperty(Property.create("xBand", Band.class));
        propertyContainer.addProperty(Property.create("yBand", Band.class));
        propertyContainer.addProperty(Property.create("zBand", Band.class));
        RefreshActionEnabler refreshActionEnabler = new RefreshActionEnabler(refreshButton);
        refreshActionEnabler.addProductBandEnablement("xProduct", "xBand");
        refreshActionEnabler.addProductBandEnablement("yProduct", "yBand");
        refreshActionEnabler.addProductBandEnablement("zProduct", "zBand");
        propertyContainer.addPropertyChangeListener(refreshActionEnabler);

        refreshButton.setEnabled(true);
        propertyContainer.setValue("xProduct", new Product("name", "type"));
        assertFalse(refreshButton.isEnabled());

        refreshButton.setEnabled(true);
        propertyContainer.setValue("yProduct", new Product("name", "type"));
        assertFalse(refreshButton.isEnabled());

        refreshButton.setEnabled(true);
        propertyContainer.setValue("zProduct", new Product("name", "type"));
        assertFalse(refreshButton.isEnabled());
    }

    @Test
    public void propertyChange_product_change_with_option() {
        PropertyContainer propertyContainer = new PropertyContainer();
        propertyContainer.addProperty(Property.create("xProduct", Product.class));
        propertyContainer.addProperty(Property.create("yProduct", Product.class));
        propertyContainer.addProperty(Property.create("zProduct", Product.class));
        propertyContainer.addProperty(Property.create("colorProduct", Product.class));
        propertyContainer.addProperty(Property.create("xBand", Band.class));
        propertyContainer.addProperty(Property.create("yBand", Band.class));
        propertyContainer.addProperty(Property.create("zBand", Band.class));
        propertyContainer.addProperty(Property.create("colorBand", Band.class));
        RefreshActionEnabler refreshActionEnabler = new RefreshActionEnabler(refreshButton);
        refreshActionEnabler.addProductBandEnablement("xProduct", "xBand");
        refreshActionEnabler.addProductBandEnablement("yProduct", "yBand");
        refreshActionEnabler.addProductBandEnablement("zProduct", "zBand");
        refreshActionEnabler.addProductBandEnablement("colorProduct", "colorBand", true);
        propertyContainer.addPropertyChangeListener(refreshActionEnabler);

        refreshButton.setEnabled(true);
        propertyContainer.setValue("xProduct", new Product("name", "type"));
        assertFalse(refreshButton.isEnabled());

        refreshButton.setEnabled(true);
        propertyContainer.setValue("yProduct", new Product("name", "type"));
        assertFalse(refreshButton.isEnabled());

        refreshButton.setEnabled(true);
        propertyContainer.setValue("zProduct", new Product("name", "type"));
        assertFalse(refreshButton.isEnabled());

        refreshButton.setEnabled(true);
        propertyContainer.setValue("colorProduct", new Product("name", "type"));
        assertTrue(refreshButton.isEnabled());
    }

    @Test
    public void propertyChange_other_property_change() {
        PropertyContainer propertyContainer = new PropertyContainer();
        propertyContainer.addProperty(Property.create("random", String.class));
        RefreshActionEnabler refreshActionEnabler = new RefreshActionEnabler(refreshButton, "random");
        propertyContainer.addPropertyChangeListener(refreshActionEnabler);

        propertyContainer.setValue("random", "vfsgtdnf");
        assertTrue(refreshButton.isEnabled());
    }

    @Test
    public void propertyChange_irrelevant_property_change() {
        PropertyContainer propertyContainer = new PropertyContainer();
        propertyContainer.addProperty(Property.create("random", String.class));
        propertyContainer.addProperty(Property.create("irrelevant", String.class));
        RefreshActionEnabler refreshActionEnabler = new RefreshActionEnabler(refreshButton, "random");
        propertyContainer.addPropertyChangeListener(refreshActionEnabler);

        propertyContainer.setValue("irrelevant", "vfsgtdnf");
        assertFalse(refreshButton.isEnabled());
    }

    @Test
    public void propertyChange_roi_mask_property_change() {
        PropertyContainer propertyContainer = new PropertyContainer();
        propertyContainer.addProperty(Property.create("useRoiMask", Boolean.class));
        propertyContainer.setValue("useRoiMask", false);
        propertyContainer.addProperty(Property.create("roiMask", Mask.class));
        propertyContainer.setValue("roiMask", null);
        RefreshActionEnabler refreshActionEnabler = new RefreshActionEnabler(refreshButton, "useRoiMask", "roiMask");
        propertyContainer.addPropertyChangeListener(refreshActionEnabler);

        propertyContainer.setValue("useRoiMask", true);
        assertFalse(refreshButton.isEnabled());

        propertyContainer.setValue("roiMask", new Mask("name", 1, 1, Mask.BandMathsType.INSTANCE));
        assertTrue(refreshButton.isEnabled());

        refreshButton.setEnabled(false);
        propertyContainer.setValue("useRoiMask", false);
        assertTrue(refreshButton.isEnabled());

        refreshButton.setEnabled(false);
        propertyContainer.setValue("useRoiMask", true);
        assertTrue(refreshButton.isEnabled());

        refreshButton.setEnabled(false);
        propertyContainer.setValue("roiMask", null);
        assertTrue(refreshButton.isEnabled());
    }

    @Test
    public void propertyChange_auto_min_max_property_change() {
        PropertyContainer propertyContainer = new PropertyContainer();
        propertyContainer.addProperty(Property.create("autoMinMax", Boolean.class));
        propertyContainer.setValue("autoMinMax", false);
        propertyContainer.addProperty(Property.create("min", Double.class));
        propertyContainer.setValue("min", 0.0);
        propertyContainer.addProperty(Property.create("max", Double.class));
        propertyContainer.setValue("max", 1.0);
        RefreshActionEnabler refreshActionEnabler = new RefreshActionEnabler(refreshButton, "autoMinMax", "min", "max");
        propertyContainer.addPropertyChangeListener(refreshActionEnabler);

        propertyContainer.setValue("autoMinMax", false);
        assertFalse(refreshButton.isEnabled());

        propertyContainer.setValue("min", 0.0);
        assertFalse(refreshButton.isEnabled());

        propertyContainer.setValue("min", 0.1);
        assertTrue(refreshButton.isEnabled());

        refreshButton.setEnabled(false);
        propertyContainer.setValue("max", 1.0);
        assertFalse(refreshButton.isEnabled());

        propertyContainer.setValue("max", 0.9);
        assertTrue(refreshButton.isEnabled());

        refreshButton.setEnabled(false);
        propertyContainer.setValue("autoMinMax", true);
        assertTrue(refreshButton.isEnabled());
    }

}