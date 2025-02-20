package org.esa.snap.core.gpf.ui.rtv;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import com.bc.ceres.swing.selection.SelectionChangeListener;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.descriptor.OperatorDescriptor;
import org.esa.snap.core.gpf.ui.DefaultIOParametersPanel;
import org.esa.snap.core.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.snap.core.gpf.ui.SourceProductSelector;
import org.esa.snap.ui.AppContext;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

public class QuantizationDialog extends DefaultSingleTargetProductDialog {

    private static final String BAND_NAME = "bandName";
    private static final String INTERVALS_MAP = "intervalsMap";
    private final Field bandsField;
    private final Field intervalsMapField;
    private final IntervalsTableEditor intervalsTableEditor;

    public QuantizationDialog(String operatorName, AppContext appContext, String title, String helpID) {
        this(operatorName, appContext, title, helpID, true);
    }

    public QuantizationDialog(String operatorName, AppContext appContext, String title, String helpID, boolean targetProductSelectorDisplay) {
        super(operatorName, appContext, title, helpID, targetProductSelectorDisplay);

        OperatorDescriptor descriptor = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(operatorName).getOperatorDescriptor();
        bandsField = Arrays.stream(descriptor.getOperatorClass().getDeclaredFields())
                .filter(f -> f.getAnnotation(Parameter.class) != null && f.getName().equals(BAND_NAME))
                .findFirst().get();

        intervalsMapField = Arrays.stream(descriptor.getOperatorClass().getDeclaredFields())
                .filter(f -> f.getAnnotation(Parameter.class) != null && f.getName().equals(INTERVALS_MAP))
                .findFirst().get();

        DefaultIOParametersPanel ioParametersPanel = getDefaultIOParametersPanel();

        List<SourceProductSelector> sourceProductSelectorList = ioParametersPanel.getSourceProductSelectorList();
        if (!sourceProductSelectorList.isEmpty()) {
            SelectionChangeListener listener = new SelectionChangeListener() {
                public void selectionChanged(SelectionChangeEvent event) {
                    processSelectedProduct();
                }

                public void selectionContextChanged(SelectionChangeEvent event) {
                }
            };
            sourceProductSelectorList.get(0).addSelectionChangeListener(listener);
        }

        BindingContext bindingContext = getBindingContext();
        PropertySet propertySet = bindingContext.getPropertySet();
        final Property propBands = propertySet.getProperty(this.bandsField.getName());

        propBands.addPropertyChangeListener(evt-> { });

        this.intervalsTableEditor = new IntervalsTableEditor();
        final Property propIntervalsMap = propertySet.getProperty(this.intervalsMapField.getName());
        propIntervalsMap.getDescriptor().setAttribute("propertyEditor", this.intervalsTableEditor);

        propIntervalsMap.addPropertyChangeListener(evt-> { });
    }

    @Override
    public int show() {
        int result = super.show();
        processSelectedProduct();
        return result;
    }

    private void processSelectedProduct() {
        Product selectedProduct = getSelectedProduct();
        if (selectedProduct != null) {
            BindingContext bindingContext = getBindingContext();
            PropertySet propertySet = bindingContext.getPropertySet();
            propertySet.setDefaultValues();
            final Property propertyBands = propertySet.getProperty(this.bandsField.getName());
            propertyBands.getDescriptor().setValueSet(new ValueSet(selectedProduct.getBandNames()));
            propertySet.setValue(this.bandsField.getName(), null);
        }
    }
    private Product getSelectedProduct() {
        DefaultIOParametersPanel ioParametersPanel = getDefaultIOParametersPanel();
        List<SourceProductSelector> sourceProductSelectorList = ioParametersPanel.getSourceProductSelectorList();
        return sourceProductSelectorList.get(0).getSelectedProduct();
    }

    @Override
    protected boolean verifyUserInput() {
        if (intervalsTableEditor == null){
            return false;
        }
        if(!intervalsTableEditor.validateIntervals()){
            showErrorDialog(intervalsTableEditor.getLastErrorMessage());
            return false;
        }
        return true;
    }
}
