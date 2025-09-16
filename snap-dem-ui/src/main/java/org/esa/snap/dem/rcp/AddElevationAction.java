/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.dem.rcp;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.multilevel.support.AbstractMultiLevelSource;
import com.bc.ceres.multilevel.support.DefaultMultiLevelImage;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.ComponentAdapter;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.dem.ElevationModelRegistry;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.dataop.resamp.ResamplingFactory;
import org.esa.snap.core.image.RasterDataNodeSampleOpImage;
import org.esa.snap.core.image.ResolutionLevel;
import org.esa.snap.dem.dataio.DEMFactory;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.ModalDialog;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.image.RenderedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

@ActionID(
        category = "Tools",
        id = "AddElevationAction"
)
@ActionRegistration(
        displayName = "#CTL_AddElevationAction_MenuText",
        popupText = "#CTL_AddElevationAction_MenuText"
)
@ActionReferences({
        @ActionReference(
                path = "Menu/Raster/DEM Tools",
                position = 250
        ),
        @ActionReference(
                path = "Shortcuts",
                name = "D-E"
        ),
        @ActionReference(
                path = "Context/Product/Product",
                position = 20
        ),
        @ActionReference(
                path = "Context/Product/RasterDataNode",
                position = 10
        ),
})
@NbBundle.Messages({
        "CTL_AddElevationAction_MenuText=Add Elevation Band",
        "CTL_AddElevationAction_ShortDescription=Create a new elevation band from a DEM"
})
public class AddElevationAction extends AbstractAction implements ContextAwareAction, LookupListener, HelpCtx.Provider {

    private static final String HELP_ID = "createElevation";
    private final Lookup lkp;
    private Product product;

    public static final String DIALOG_TITLE = "Add Elevation Band";
    public static final String DEFAULT_ELEVATION_BAND_NAME = "elevation";
    public static final String DEFAULT_LATITUDE_BAND_NAME = "corr_latitude";
    public static final String DEFAULT_LONGITUDE_BAND_NAME = "corr_longitude";

    public AddElevationAction() {
        this(Utilities.actionsGlobalContext());
    }

    public AddElevationAction(Lookup lkp) {
        super(Bundle.CTL_AddElevationAction_MenuText());
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        setEnableState();
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_AddElevationAction_ShortDescription());
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new AddElevationAction(actionContext);
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        setEnableState();
    }

    private void setEnableState() {
        ProductNode productNode = lkp.lookup(ProductNode.class);
        boolean state = false;
        if (productNode != null) {
            product = productNode.getProduct();
            state = product.getSceneGeoCoding() != null;
        }
        setEnabled(state);
    }

    @Override
    public void actionPerformed(ActionEvent event) {

        final DialogData dialogData = requestDialogData(product);
        if (dialogData == null) {
            return;
        }

        final String demName = DEMFactory.getProperDEMName(dialogData.demName);
        final ElevationModelRegistry elevationModelRegistry = ElevationModelRegistry.getInstance();
        final ElevationModelDescriptor demDescriptor = elevationModelRegistry.getDescriptor(demName);
        if (demDescriptor == null) {
            Dialogs.showError(DIALOG_TITLE, "The DEM '" + demName + "' is not supported.");
            return;
        }

        Resampling resampling = Resampling.BILINEAR_INTERPOLATION;
        if (dialogData.resamplingMethod != null) {
            resampling = ResamplingFactory.createResampling(dialogData.resamplingMethod);
        }

        computeBands(product,
                demDescriptor,
                dialogData.outputElevationBand ? dialogData.elevationBandName : null,
                resampling);
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(HELP_ID);
    }

    private static void computeBands(final Product product,
                                     final ElevationModelDescriptor demDescriptor,
                                     final String elevationBandName,
                                     final Resampling resampling) {

        final ElevationModel dem = demDescriptor.createDem(resampling);
        if (elevationBandName != null) {
            addElevationBand(product, dem, elevationBandName);
        }
    }

    private static void addElevationBand(Product product, ElevationModel dem, String elevationBandName) {
        final GeoCoding geoCoding = product.getSceneGeoCoding();
        ElevationModelDescriptor demDescriptor = dem.getDescriptor();
        final float noDataValue = dem.getDescriptor().getNoDataValue();
        final Band elevationBand = product.addBand(elevationBandName, ProductData.TYPE_FLOAT32);
        elevationBand.setNoDataValueUsed(true);
        elevationBand.setNoDataValue(noDataValue);
        elevationBand.setUnit(Unit.METERS);
        elevationBand.setDescription(demDescriptor.getName());
        elevationBand.setSourceImage(createElevationSourceImage(dem, geoCoding, elevationBand));
    }

    private static RenderedImage createElevationSourceImage(final ElevationModel dem, final GeoCoding geoCoding, final Band band) {
        return new DefaultMultiLevelImage(new AbstractMultiLevelSource(band.createMultiLevelModel()) {
            @Override
            protected RenderedImage createImage(final int level) {
                return new ElevationSourceImage(dem, geoCoding, band, ResolutionLevel.create(getModel(), level));
            }
        });
    }

    private static boolean isOrtorectifiable(Product product) {
        return product.getNumBands() > 0 && product.getBandAt(0).canBeOrthorectified();
    }

    private DialogData requestDialogData(final Product product) {

        boolean ortorectifiable = isOrtorectifiable(product);

        String[] demNames = DEMFactory.getDEMNameList();

        // sort the list
        final List<String> sortedDEMNames = Arrays.asList(demNames);
        java.util.Collections.sort(sortedDEMNames);
        demNames = sortedDEMNames.toArray(new String[sortedDEMNames.size()]);

        final DialogData dialogData = new DialogData("SRTM 3sec (Auto Download)", ResamplingFactory.BILINEAR_INTERPOLATION_NAME, ortorectifiable);
        PropertySet propertySet = PropertyContainer.createObjectBacked(dialogData);
        configureDemNameProperty(propertySet, "demName", demNames, "SRTM 3sec (Auto Download)");
        configureDemNameProperty(propertySet, "resamplingMethod", ResamplingFactory.resamplingNames,
                ResamplingFactory.BILINEAR_INTERPOLATION_NAME);
        configureBandNameProperty(propertySet, "elevationBandName", product);
        configureBandNameProperty(propertySet, "latitudeBandName", product);
        configureBandNameProperty(propertySet, "longitudeBandName", product);
        final BindingContext ctx = new BindingContext(propertySet);

        JList demList = new JList();
        demList.setVisibleRowCount(10);
        ctx.bind("demName", new SingleSelectionListComponentAdapter(demList));

        JTextField elevationBandNameField = new JTextField();
        elevationBandNameField.setColumns(10);
        ctx.bind("elevationBandName", elevationBandNameField);

        JCheckBox outputDemCorrectedBandsChecker = new JCheckBox("Output DEM-corrected bands");
        ctx.bind("outputDemCorrectedBands", outputDemCorrectedBandsChecker);

        JLabel latitudeBandNameLabel = new JLabel("Latitude band name:");
        JTextField latitudeBandNameField = new JTextField();
        latitudeBandNameField.setEnabled(ortorectifiable);
        ctx.bind("latitudeBandName", latitudeBandNameField).addComponent(latitudeBandNameLabel);
        ctx.bindEnabledState("latitudeBandName", true, "outputGeoCodingBands", true);

        JLabel longitudeBandNameLabel = new JLabel("Longitude band name:");
        JTextField longitudeBandNameField = new JTextField();
        longitudeBandNameField.setEnabled(ortorectifiable);
        ctx.bind("longitudeBandName", longitudeBandNameField).addComponent(longitudeBandNameLabel);
        ctx.bindEnabledState("longitudeBandName", true, "outputGeoCodingBands", true);

        TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setCellColspan(0, 0, 2);
        tableLayout.setCellColspan(1, 0, 2);
      /*  tableLayout.setCellColspan(3, 0, 2);
        tableLayout.setCellWeightX(0, 0, 1.0);
        tableLayout.setRowWeightX(1, 1.0);
        tableLayout.setCellWeightX(2, 1, 1.0);
        tableLayout.setCellWeightX(4, 1, 1.0);
        tableLayout.setCellWeightX(5, 1, 1.0);
        tableLayout.setCellPadding(4, 0, new Insets(0, 24, 0, 4));
        tableLayout.setCellPadding(5, 0, new Insets(0, 24, 0, 4));   */

        JPanel parameterPanel = new JPanel(tableLayout);
        /*row 0*/
        parameterPanel.add(new JLabel("Digital elevation model (DEM):"));
        parameterPanel.add(new JScrollPane(demList));
        /*row 1*/
        parameterPanel.add(new JLabel("Resampling method:"));
        final JComboBox resamplingCombo = new JComboBox(DEMFactory.getDEMResamplingMethods());
        parameterPanel.add(resamplingCombo);
        ctx.bind("resamplingMethod", resamplingCombo);

        parameterPanel.add(new JLabel("Elevation band name:"));
        parameterPanel.add(elevationBandNameField);
        if (ortorectifiable) {
            /*row 2*/
            parameterPanel.add(outputDemCorrectedBandsChecker);
            /*row 3*/
            parameterPanel.add(latitudeBandNameLabel);
            parameterPanel.add(latitudeBandNameField);
            /*row 4*/
            parameterPanel.add(longitudeBandNameLabel);
            parameterPanel.add(longitudeBandNameField);

            outputDemCorrectedBandsChecker.setSelected(ortorectifiable);
            outputDemCorrectedBandsChecker.setEnabled(ortorectifiable);
        }

        final ModalDialog dialog = new ModalDialog(SnapApp.getDefault().getMainFrame(), DIALOG_TITLE, ModalDialog.ID_OK_CANCEL, HELP_ID);
        dialog.setContent(parameterPanel);
        dialog.setResizable(false);
        if (dialog.show() == ModalDialog.ID_OK) {
            return dialogData;
        }

        return null;
    }

    private static void configureDemNameProperty(PropertySet propertySet, String propertyName, String[] demNames, String defaultValue) {
        PropertyDescriptor descriptor = propertySet.getProperty(propertyName).getDescriptor();
        descriptor.setValueSet(new ValueSet(demNames));
        descriptor.setDefaultValue(defaultValue);
        descriptor.setNotNull(true);
        descriptor.setNotEmpty(true);
    }

    private static void configureBandNameProperty(PropertySet propertySet, String propertyName, Product product) {
        Property property = propertySet.getProperty(propertyName);
        PropertyDescriptor descriptor = property.getDescriptor();
        descriptor.setNotNull(true);
        descriptor.setNotEmpty(true);
        descriptor.setValidator(new BandNameValidator(product));
        setValidBandName(property, product);
    }

    private static void setValidBandName(Property property, Product product) {
        String bandName = (String) property.getValue();
        String bandNameStub = bandName;
        for (int i = 2; product.containsBand(bandName); i++) {
            bandName = String.format("%s_%d", bandNameStub, i);
        }
        try {
            property.setValue(bandName);
        } catch (ValidationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static class SingleSelectionListComponentAdapter extends ComponentAdapter implements ListSelectionListener, PropertyChangeListener {

        private final JList list;

        public SingleSelectionListComponentAdapter(JList list) {
            this.list = list;
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }

        @Override
        public JComponent[] getComponents() {
            return new JComponent[]{list};
        }

        @Override
        public void bindComponents() {
            updateListModel();
            getPropertyDescriptor().addAttributeChangeListener(this);
            list.addListSelectionListener(this);
        }

        @Override
        public void unbindComponents() {
            getPropertyDescriptor().removeAttributeChangeListener(this);
            list.removeListSelectionListener(this);
        }

        @Override
        public void adjustComponents() {
            Object value = getBinding().getPropertyValue();
            if (value != null) {
                list.setSelectedValue(value, true);
            } else {
                list.clearSelection();
            }
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getSource() == getPropertyDescriptor() && evt.getPropertyName().equals("valueSet")) {
                updateListModel();
            }
        }

        private PropertyDescriptor getPropertyDescriptor() {
            return getBinding().getContext().getPropertySet().getDescriptor(getBinding().getPropertyName());
        }

        private void updateListModel() {
            ValueSet valueSet = getPropertyDescriptor().getValueSet();
            if (valueSet != null) {
                list.setListData(valueSet.getItems());
                adjustComponents();
            }
        }

        @Override
        public void valueChanged(ListSelectionEvent event) {
            if (event.getValueIsAdjusting()) {
                return;
            }
            if (getBinding().isAdjustingComponents()) {
                return;
            }
            final Property property = getBinding().getContext().getPropertySet().getProperty(getBinding().getPropertyName());
            Object selectedValue = list.getSelectedValue();
            try {
                property.setValue(selectedValue);
                // Now model is in sync with UI
                getBinding().clearProblem();
            } catch (ValidationException e) {
                getBinding().reportProblem(e);
            }
        }
    }

    private static class BandNameValidator implements Validator {
        private final Product product;

        public BandNameValidator(Product product) {
            this.product = product;
        }

        @Override
        public void validateValue(Property property, Object value) throws ValidationException {
            final String bandName = value.toString().trim();
            if (!ProductNode.isValidNodeName(bandName)) {
                throw new ValidationException(MessageFormat.format("The band name ''{0}'' appears not to be valid.\n" +
                                "Please choose another one.",
                        bandName
                ));
            } else if (product.containsBand(bandName)) {
                throw new ValidationException(MessageFormat.format("The selected product already contains a band named ''{0}''.\n" +
                                "Please choose another one.",
                        bandName
                ));
            }

        }
    }

    private static class ElevationSourceImage extends RasterDataNodeSampleOpImage {
        private final ElevationModel dem;
        private final GeoCoding geoCoding;
        private double noDataValue;

        public ElevationSourceImage(ElevationModel dem, GeoCoding geoCoding, Band band, ResolutionLevel level) {
            super(band, level);
            this.dem = dem;
            this.geoCoding = geoCoding;
            noDataValue = band.getNoDataValue();
        }

        @Override
        protected double computeSample(int sourceX, int sourceY) {
            try {
                return dem.getElevation(geoCoding.getGeoPos(new PixelPos(sourceX, sourceY), null));
            } catch (Exception e) {
                return noDataValue;
            }
        }
    }

    private static class DialogData {
        String demName;
        String resamplingMethod;
        boolean outputElevationBand;
        boolean outputDemCorrectedBands;
        String elevationBandName = DEFAULT_ELEVATION_BAND_NAME;
        String latitudeBandName = DEFAULT_LATITUDE_BAND_NAME;
        String longitudeBandName = DEFAULT_LONGITUDE_BAND_NAME;

        public DialogData(String demName, String resamplingMethod, boolean ortorectifiable) {
            this.demName = demName;
            this.resamplingMethod = resamplingMethod;
            outputElevationBand = true;
            outputDemCorrectedBands = ortorectifiable;
        }
    }

}
