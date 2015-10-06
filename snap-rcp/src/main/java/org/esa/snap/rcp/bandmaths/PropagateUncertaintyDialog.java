package org.esa.snap.rcp.bandmaths;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.core.Assert;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyEditor;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.dataop.barithm.StandardUncertaintyGenerator;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.rcp.actions.window.OpenImageViewAction;
import org.esa.snap.rcp.nodes.UndoableProductNodeInsertion;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.ModalDialog;
import org.openide.awt.UndoRedo;
import org.openide.util.NbBundle;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.GridBagConstraints;
import java.beans.PropertyChangeListener;

/**
 * @author Norman Fomferra
 */
@NbBundle.Messages({
        "CTL_PropagateUncertaintyDialog_Title=Propagate Uncertainty",
})
public class PropagateUncertaintyDialog extends ModalDialog {


    private static final String PROPERTY_NAME_BAND_NAME = "bandName";
    private static final String PROPERTY_NAME_ORDER = "order";
    private static final String PROPERTY_NAME_RELATION = "relation";

    private static final String ERROR_PREFIX = "Error: ";

    private final BindingContext bindingContext;
    private VirtualBand sourceBand;

    @SuppressWarnings("UnusedDeclaration")
    private String bandName;
    @SuppressWarnings("UnusedDeclaration")
    private int order;
    @SuppressWarnings("UnusedDeclaration")
    private String relation;

    private JTextArea sourceExprArea;
    private JTextArea targetExprArea;

    public PropagateUncertaintyDialog(VirtualBand virtualBand) {
        super(SnapApp.getDefault().getMainFrame(), Bundle.CTL_PropagateUncertaintyDialog_Title(), ID_OK_CANCEL_HELP, "propagateUncertainty");
        Assert.notNull(virtualBand, "virtualBand");
        this.sourceBand = virtualBand;
        bindingContext = createBindingContext();
        bandName = virtualBand.getName() + "_unc";
        order = 1;
        initUI();
    }

    @Override
    protected void onOK() {
        String uncertaintyExpression = targetExprArea.getText();

        Product targetProduct = sourceBand.getProduct();
        int width = targetProduct.getSceneRasterWidth();
        int height = targetProduct.getSceneRasterHeight();

        ProductNodeGroup<Band> bandGroup = targetProduct.getBandGroup();

        VirtualBand uncertaintyBand = new VirtualBand(getBandName(), ProductData.TYPE_FLOAT32, width, height, uncertaintyExpression);
        uncertaintyBand.setDescription("Uncertainty propagated from band " + sourceBand.getName() + " = " + sourceBand.getExpression());
        uncertaintyBand.setUnit(sourceBand.getUnit());
        uncertaintyBand.setNoDataValue(Double.NaN);
        uncertaintyBand.setNoDataValueUsed(true);
        uncertaintyBand.setValidPixelExpression(sourceBand.getValidPixelExpression());
        ProductUtils.copySpectralBandProperties(sourceBand, uncertaintyBand);

        bandGroup.add(uncertaintyBand);
        sourceBand.addAncillaryVariable(uncertaintyBand, relation);

        UndoRedo.Manager undoManager = SnapApp.getDefault().getUndoManager(targetProduct);
        if (undoManager != null) {
            undoManager.addEdit(new UndoableProductNodeInsertion<>(bandGroup, uncertaintyBand));
        }

        hide();
        uncertaintyBand.setModified(true);
        if (SnapApp.getDefault().getPreferences().getBoolean(BandMathsDialog.PREF_KEY_AUTO_SHOW_NEW_BANDS, true)) {
            OpenImageViewAction.openImageView(uncertaintyBand);
        }
    }

    private String generateUncertaintyExpression() throws ParseException, UnsupportedOperationException {
        StandardUncertaintyGenerator propagator = new StandardUncertaintyGenerator(order, false);
        return propagator.generateUncertainty(sourceBand.getProduct(), relation, sourceBand.getExpression());
    }

    @Override
    protected boolean verifyUserInput() {

        String uncertaintyExpression = targetExprArea.getText();
        if (uncertaintyExpression == null || uncertaintyExpression.trim().isEmpty()) {
            SnapDialogs.showError("Uncertainty expression is empty.");
            return false;
        }

        if (uncertaintyExpression.startsWith(ERROR_PREFIX)) {
            SnapDialogs.showError(uncertaintyExpression.substring(ERROR_PREFIX.length()));
            return false;
        }

        if (sourceBand.getProduct().containsBand(getBandName())) {
            SnapDialogs.showError("A raster with name '" + getBandName() + "' already exists.");
            return false;
        }

        return super.verifyUserInput();
    }

    private void initUI() {
        JComponent[] components;
        final JPanel panel = GridBagUtils.createPanel();
        int line = 0;
        GridBagConstraints gbc = new GridBagConstraints();

        sourceExprArea = new JTextArea(3, 40);
        sourceExprArea.setEditable(false);
        sourceExprArea.setEnabled(false);
        sourceExprArea.setText(sourceBand.getExpression());

        targetExprArea = new JTextArea(6, 40);
        targetExprArea.setEditable(true);
        updateTargetExprArea();

        final JComboBox<String> comboBox = new JComboBox<>(new String[]{"uncertainty", "standard_deviation", "error"});
        comboBox.setEditable(true);
        bindingContext.bind(PROPERTY_NAME_RELATION, comboBox);

        gbc.gridy = ++line;
        components = createComponents(PROPERTY_NAME_BAND_NAME);
        GridBagUtils.addToPanel(panel, components[1], gbc,
                                "weightx=0, insets.right=3, insets.top=3, gridwidth=1, fill=HORIZONTAL, anchor=WEST");
        GridBagUtils.addToPanel(panel, components[0], gbc,
                                "weightx=1, insets.right=0, insets.top=3, gridwidth=2, fill=HORIZONTAL, anchor=WEST");

        gbc.gridy = ++line;
        components = createComponents(PROPERTY_NAME_ORDER);
        GridBagUtils.addToPanel(panel, components[1], gbc,
                                "weightx=0, insets.right=3, insets.top=3, gridwidth=1, fill=HORIZONTAL, anchor=WEST");
        GridBagUtils.addToPanel(panel, components[0], gbc,
                                "weightx=1, insets.right=0, insets.top=3, gridwidth=2, fill=HORIZONTAL, anchor=WEST");

        gbc.gridy = ++line;
        PropertyDescriptor relationDescriptor = bindingContext.getPropertySet().getDescriptor(PROPERTY_NAME_RELATION);
        GridBagUtils.addToPanel(panel, new JLabel(relationDescriptor.getDisplayName() + ":"), gbc,
                                "weightx=0, insets.right=3, insets.top=3, gridwidth=1, fill=HORIZONTAL, anchor=WEST");
        GridBagUtils.addToPanel(panel, comboBox, gbc,
                                "weightx=1, insets.right=0, insets.top=3, gridwidth=2, fill=HORIZONTAL, anchor=WEST");

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, new JLabel("Source expression:"), gbc,
                                "weightx=1, insets.top=8, gridwidth=3, fill=HORIZONTAL, anchor=WEST");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, new JScrollPane(sourceExprArea), gbc,
                                "weightx=1, insets.top=3, gridwidth=3, fill=HORIZONTAL, anchor=WEST");

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, new JLabel("Uncertainty expression:"), gbc,
                                "weightx=1, insets.top=3, gridwidth=3, fill=HORIZONTAL, anchor=WEST");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, new JScrollPane(targetExprArea), gbc,
                                "weightx=1, weighty=1, insets.top=3, gridwidth=3, fill=BOTH, anchor=WEST");
        setContent(panel);
    }

    private void updateTargetExprArea() {
        try {
            String uncertaintyExpression= generateUncertaintyExpression();
            targetExprArea.setText(uncertaintyExpression);
        } catch (ParseException | UnsupportedOperationException e) {
            targetExprArea.setText(ERROR_PREFIX + e.getMessage());
        }
    }

    private JComponent[] createComponents(String propertyName) {
        PropertyDescriptor descriptor = bindingContext.getPropertySet().getDescriptor(propertyName);
        PropertyEditor propertyEditor = PropertyEditorRegistry.getInstance().findPropertyEditor(descriptor);
        return propertyEditor.createComponents(descriptor, bindingContext);
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
        descriptor.setDefaultValue(getDefaultBandName(sourceBand.getName() + "_unc"));

        descriptor = container.getDescriptor(PROPERTY_NAME_ORDER);
        descriptor.setDisplayName("Order of Taylor polynomial");
        descriptor.setDescription("The number of Taylor series expansion terms used for the Standard Combined Uncertainty (GUM 1995).");
        descriptor.setDefaultValue(1);
        descriptor.setValueSet(new ValueSet(new Integer[]{1, 2, 3}));

        descriptor = container.getDescriptor(PROPERTY_NAME_RELATION);
        descriptor.setDisplayName("Relation name of ancillary bands");
        descriptor.setDescription("Relation  name of ancillary variables that represent uncertainties (NetCDF-U 'rel' attribute).");
        descriptor.setDefaultValue("uncertainty");
        descriptor.setNotNull(true);
        descriptor.setNotEmpty(true);

        container.setDefaultValues();

        PropertyChangeListener targetExprUpdater = evt -> {
            updateTargetExprArea();
        };
        context.addPropertyChangeListener(PROPERTY_NAME_ORDER, targetExprUpdater);
        context.addPropertyChangeListener(PROPERTY_NAME_RELATION, targetExprUpdater);

        return context;
    }

    private String getDefaultBandName(String nameBase) {
        String defaultName = nameBase;
        Product product = sourceBand.getProduct();
        int i = 0;
        while (product.getRasterDataNode(defaultName) != null) {
            defaultName = nameBase + (++i);
        }
        return defaultName;
    }


    private String getBandName() {
        return bandName.trim();
    }

}
