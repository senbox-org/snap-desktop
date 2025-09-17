package org.esa.snap.rcp.preferences.layer;

import com.bc.ceres.annotation.STTM;
import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValidationException;
import org.esa.snap.core.layer.WorldMapLayerType;
import org.junit.Test;

import javax.swing.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class WorldMapLayerControllerTest {

    @Test
    @STTM("SNAP-3762")
    public void testUpdateProperty_empty() throws ValidationException {
        final JComboBox<WorldMapLayerType> comboBox = new JComboBox<>();
        final Property property = mock(Property.class);

        WorldMapLayerController.updateProperty(comboBox, property);
        verify(property, times(0)).setValue(any());
    }

    @Test
    @STTM("SNAP-3762")
    public void testUpdateProperty() throws ValidationException {
        final JComboBox<WorldMapLayerType> comboBox = new JComboBox<>();
        final WorldMapLayerType mapLayerType = mock(WorldMapLayerType.class);
        when(mapLayerType.getName()).thenReturn("GlobCoverLayerType");
        when(mapLayerType.getLabel()).thenReturn("woduwolle");
        comboBox.addItem(mapLayerType);

        final Property property = mock(Property.class);

        WorldMapLayerController.updateProperty(comboBox, property);
        verify(property, times(1)).setValue("GlobCoverLayerType");
    }
}
