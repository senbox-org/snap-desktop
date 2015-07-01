package org.esa.snap.rcp.bandmaths;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.core.Assert;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyEditor;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import com.bc.ceres.swing.binding.internal.TextFieldEditor;
import com.bc.jexp.ParseException;
import com.bc.jexp.Term;
import com.bc.jexp.impl.TermDecompiler;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.datamodel.ProductNodeGroup;
import org.esa.snap.framework.datamodel.VirtualBand;
import org.esa.snap.framework.dataop.barithm.StandardUncertaintyGenerator;
import org.esa.snap.framework.ui.GridBagUtils;
import org.esa.snap.framework.ui.ModalDialog;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.rcp.actions.view.OpenImageViewAction;
import org.esa.snap.rcp.nodes.UndoableProductNodeInsertion;
import org.esa.snap.util.ProductUtils;
import org.openide.awt.UndoRedo;
import org.openide.util.NbBundle;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;

/**
 * @author Norman Fomferra
 */
@NbBundle.Messages({
        "CTL_PropagateUncertaintyDialog_Title=Propagate Uncertainty",
})
public class PropagateUncertaintyDialog extends ModalDialog {


    private static final String PROPERTY_NAME_BAND_NAME = "bandName";
    private static final String PROPERTY_NAME_TAYLOR_SERIES_TERM_COUNT = "taylorSeriesTermCount";

    private final BindingContext bindingContext;
    private VirtualBand sourceBand;

    @SuppressWarnings("UnusedDeclaration")
    private String bandName;
    @SuppressWarnings("UnusedDeclaration")
    private int taylorSeriesTermCount;

    public PropagateUncertaintyDialog(VirtualBand virtualBand) {
        super(SnapApp.getDefault().getMainFrame(), Bundle.CTL_PropagateUncertaintyDialog_Title(), ID_OK_CANCEL_HELP, "propagateUncertainty");
        Assert.notNull(virtualBand, "virtualBand");
        this.sourceBand = virtualBand;
        bindingContext = createBindingContext();
        bandName = virtualBand.getName() + "_unc";
        taylorSeriesTermCount = 1;
        makeUI();
    }

    @Override
    protected void onOK() {
        Product targetProduct = sourceBand.getProduct();
        int width = targetProduct.getSceneRasterWidth();
        int height = targetProduct.getSceneRasterHeight();

        StandardUncertaintyGenerator propagator = new StandardUncertaintyGenerator();
        Term term;
        try {
            term = propagator.generateUncertainty(targetProduct, sourceBand.getExpression());
        } catch (ParseException | UnsupportedOperationException e) {
            SnapDialogs.showError(Bundle.CTL_PropagateUncertaintyDialog_Title() + " - Error", e.getMessage());
            return;
        }
        String uncertaintyExpression = new TermDecompiler().decompile(term);

        ProductNodeGroup<Band> bandGroup = targetProduct.getBandGroup();

        VirtualBand uncertaintyBand = new VirtualBand(getBandName() + "_unc", ProductData.TYPE_FLOAT32, width, height, uncertaintyExpression);
        bandGroup.add(uncertaintyBand);
        uncertaintyBand.setDescription("Uncertainty propagated from band '" + sourceBand.getName() + "', Expr.: " + sourceBand.getExpression());
        uncertaintyBand.setUnit(sourceBand.getUnit());
        ProductUtils.copySpectralBandProperties(sourceBand, uncertaintyBand);
        sourceBand.addAncillaryVariable(uncertaintyBand, "uncertainty");

        UndoRedo.Manager undoManager = SnapApp.getDefault().getUndoManager(targetProduct);
        if (undoManager != null) {
            undoManager.addEdit(new UndoableProductNodeInsertion<>(bandGroup, uncertaintyBand));
        }

        hide();
        uncertaintyBand.setModified(true);
        if (SnapApp.getDefault().getPreferences().getBoolean(BandMathsDialog.PREF_KEY_AUTO_SHOW_NEW_BANDS, true)) {
            new OpenImageViewAction(uncertaintyBand).openProductSceneView();
        }
    }

    @Override
    protected boolean verifyUserInput() {
        if (sourceBand.getProduct().containsBand(getBandName())) {
            SnapDialogs.showError("A raster with name '" + getBandName() + "' already exists.");
            return false;
        }
        return super.verifyUserInput();
    }

    private void makeUI() {
        JComponent[] components;
        final JPanel panel = GridBagUtils.createPanel();
        int line = 0;
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridy = ++line;
        components = createComponents(PROPERTY_NAME_BAND_NAME, TextFieldEditor.class);
        GridBagUtils.addToPanel(panel, components[1], gbc,
                                "weightx=0, insets.top=3, gridwidth=1, fill=HORIZONTAL, anchor=WEST");
        GridBagUtils.addToPanel(panel, components[0], gbc,
                                "weightx=1, insets.top=3, gridwidth=2, fill=HORIZONTAL, anchor=WEST");

        gbc.gridy = ++line;
        components = createComponents(PROPERTY_NAME_TAYLOR_SERIES_TERM_COUNT, TextFieldEditor.class);
        GridBagUtils.addToPanel(panel, components[1], gbc,
                                "weightx=0, insets.top=3, gridwidth=1, fill=HORIZONTAL, anchor=WEST");
        GridBagUtils.addToPanel(panel, components[0], gbc,
                                "weightx=1, insets.top=3, gridwidth=2, fill=HORIZONTAL, anchor=WEST");

        setContent(panel);
    }

    private JComponent[] createComponents(String propertyName, Class<? extends PropertyEditor> editorClass) {
        PropertyDescriptor descriptor = bindingContext.getPropertySet().getDescriptor(propertyName);
        PropertyEditor editor = PropertyEditorRegistry.getInstance().getPropertyEditor(editorClass.getName());
        return editor.createComponents(descriptor, bindingContext);
    }

    private BindingContext createBindingContext() {
        final PropertyContainer container = PropertyContainer.createObjectBacked(this);
        final BindingContext context = new BindingContext(container);
        PropertyDescriptor descriptor;

        descriptor = container.getDescriptor(PROPERTY_NAME_BAND_NAME);
        descriptor.setDisplayName("Uncertainty band name");
        descriptor.setDescription("The name for the new uncertainty band.");
        descriptor.setNotEmpty(true);
        descriptor.setValidator(new ProductNodeNameValidator(sourceBand.getProduct()));
        descriptor.setDefaultValue(sourceBand.getName() + "_unc");

        descriptor = container.getDescriptor(PROPERTY_NAME_TAYLOR_SERIES_TERM_COUNT);
        descriptor.setDisplayName("Number of Taylor terms");
        descriptor.setDescription("The number of terms used in the Taylor series expansion.");
        descriptor.setDefaultValue(1);
        descriptor.setValueRange(new ValueRange(1, 5, true, true));

        container.setDefaultValues();

        return context;
    }

    private String getBandName() {
        return bandName.trim();
    }

}
