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
package org.esa.snap.landcover.rcp;

import org.esa.snap.landcover.dataio.LandCoverFactory;
import org.esa.snap.landcover.dataio.LandCoverModelDescriptor;
import org.esa.snap.landcover.dataio.LandCoverModelRegistry;
import org.esa.snap.landcover.gpf.AddLandCoverOp;
import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.ComponentAdapter;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.dataop.resamp.ResamplingFactory;
import org.esa.snap.rcp.SnapApp;
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

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.esa.snap.landcover.gpf.AddLandCoverOp.AddLandCover;

@ActionID(
        category = "Tools",
        id = "AddLandCoverAction"
)
@ActionRegistration(
        displayName = "#CTL_AddLandCoverAction_MenuText",
        popupText = "#CTL_AddLandCoverAction_MenuText"
)
@ActionReferences({
        @ActionReference(
                path = "Context/Product/Product",
                position = 21
        ),
        @ActionReference(
                path = "Context/Product/RasterDataNode",
                position = 251
        ),
})
@NbBundle.Messages({
        "CTL_AddLandCoverAction_MenuText=Add Land Cover Band",
        "CTL_AddLandCoverAction_ShortDescription=Create a new land cover band from a selected land cover model"
})
public class AddLandCoverAction extends AbstractAction implements ContextAwareAction, LookupListener, HelpCtx.Provider {

    private static final String HELP_ID = "addLandCoverBand";
    private final Lookup lkp;
    private Product product;
    public static final String DIALOG_TITLE = "Add Land Cover Band";

    private final DefaultMutableTreeNode landCoverNamesRoot = new DefaultMutableTreeNode("Land Cover Models");
    private final Map<String, DefaultMutableTreeNode> folderMap = new HashMap<>();

    public AddLandCoverAction() {
        this(Utilities.actionsGlobalContext());
    }

    public AddLandCoverAction(Lookup lkp) {
        super(Bundle.CTL_AddLandCoverAction_MenuText());
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        setEnableState();
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_AddLandCoverAction_ShortDescription());
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new AddLandCoverAction(actionContext);
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
    public HelpCtx getHelpCtx() {
        return new HelpCtx(HELP_ID);
    }

    @Override
    public void actionPerformed(final ActionEvent event) {
        try {
            final AddLandCoverOp.LandCoverParameters param = requestDialogData(product);
            if (param == null) {
                return;
            }

            AddLandCover(product, param);

        } catch (Exception e) {
            SnapApp.getDefault().handleError(e.getMessage(), e);
        }
    }

    private AddLandCoverOp.LandCoverParameters requestDialogData(final Product product) {

        String[] Names = LandCoverFactory.getNameList();

        // sort the list
        final List<String> sortedNames = Arrays.asList(Names);
        java.util.Collections.sort(sortedNames);
        Names = sortedNames.toArray(new String[0]);

        final AddLandCoverOp.LandCoverParameters dialogData = new AddLandCoverOp.LandCoverParameters(Names[0], ResamplingFactory.NEAREST_NEIGHBOUR_NAME);
        final PropertySet propertySet = PropertyContainer.createObjectBacked(dialogData);
        configureNameProperty(propertySet, "name", Names, Names[0]);
        configureNameProperty(propertySet, "resamplingMethod", ResamplingFactory.resamplingNames,
                ResamplingFactory.NEAREST_NEIGHBOUR_NAME);
        configureBandNameProperty(propertySet, "bandName", product);
        final BindingContext ctx = new BindingContext(propertySet);

        final JTree landCoverTree = new JTree(landCoverNamesRoot);
        ctx.bind("name", new SingleSelectionListComponentAdapter(landCoverTree, product));

        populateNamesTree(landCoverTree);

        final JTextField bandNameField = new JTextField();
        bandNameField.setColumns(30);
        ctx.bind("bandName", bandNameField);

        final TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTablePadding(4, 4);
        for (int i = 0; i < 8; i++) {
            tableLayout.setCellColspan(i, 0, 2);
        }

        final JPanel parameterPanel = new JPanel(tableLayout);
        /*row 0*/
        parameterPanel.add(new JLabel("Land Cover Model:"));
        parameterPanel.add(new JScrollPane(landCoverTree));
        /*row 1*/
        parameterPanel.add(new JLabel("Resampling method:"));
        final JComboBox<String> resamplingCombo = new JComboBox<>(ResamplingFactory.resamplingNames);
        parameterPanel.add(resamplingCombo);
        ctx.bind("resamplingMethod", resamplingCombo);
        parameterPanel.add(new JLabel("Integer data types will use nearest neighbour"));

        parameterPanel.add(new JLabel("Land cover band name:"));
        parameterPanel.add(bandNameField);

        final ModalDialog dialog = new ModalDialog(SnapApp.getDefault().getMainFrame(), DIALOG_TITLE, ModalDialog.ID_OK_CANCEL, HELP_ID);
        dialog.setContent(parameterPanel);
        if (dialog.show() == ModalDialog.ID_OK) {
            return dialogData;
        }

        return null;
    }

    private void populateNamesTree(final JTree landCoverTree) {

        String[] names = LandCoverFactory.getNameList();
        // sort the list
        final java.util.List<String> sortedNames = Arrays.asList(names);
        java.util.Collections.sort(sortedNames);
        names = sortedNames.toArray(new String[0]);

        final LandCoverModelRegistry registry = LandCoverModelRegistry.getInstance();
        for(String name : names) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(name);
            final LandCoverModelDescriptor descriptor = registry.getDescriptor(name);
            String grouping = descriptor.getGrouping();
            if(grouping != null) {
                DefaultMutableTreeNode folderNode = folderMap.get(grouping);
                if(folderNode == null) {
                    folderNode = new DefaultMutableTreeNode(grouping);
                    landCoverNamesRoot.add(folderNode);
                    folderMap.put(grouping, folderNode);
                }
                folderNode.add(node);
            } else {
                landCoverNamesRoot.add(node);
            }
        }
        landCoverTree.setRootVisible(true);
        landCoverTree.setVisibleRowCount(10);
        landCoverTree.expandRow(0);
    }

    private static void configureNameProperty(PropertySet propertySet, String propertyName, String[] names, String defaultValue) {
        final PropertyDescriptor descriptor = propertySet.getProperty(propertyName).getDescriptor();
        descriptor.setValueSet(new ValueSet(names));
        descriptor.setDefaultValue(defaultValue);
        descriptor.setNotNull(true);
        descriptor.setNotEmpty(true);
    }

    private static void configureBandNameProperty(PropertySet propertySet, String propertyName, Product product) {
        final Property property = propertySet.getProperty(propertyName);
        final PropertyDescriptor descriptor = property.getDescriptor();
        descriptor.setNotNull(true);
        descriptor.setNotEmpty(true);
        descriptor.setValidator(new BandNameValidator(product));
    }

    private static class SingleSelectionListComponentAdapter extends ComponentAdapter implements TreeSelectionListener, PropertyChangeListener {

        private final JTree list;
        private final Product product;

        public SingleSelectionListComponentAdapter(final JTree list, final Product product) {
            this.list = list;
            this.product = product;
        }

        @Override
        public JComponent[] getComponents() {
            return new JComponent[]{list};
        }

        @Override
        public void bindComponents() {
            getPropertyDescriptor().addAttributeChangeListener(this);
            list.addTreeSelectionListener(this);
        }

        @Override
        public void unbindComponents() {
            getPropertyDescriptor().removeAttributeChangeListener(this);
            list.removeTreeSelectionListener(this);
        }

        @Override
        public void adjustComponents() {
            Object value = getBinding().getPropertyValue();
            if (value != null) {
              //  list.setSelectionPaths(value, true);
            } else {
                list.clearSelection();
            }
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getSource() == getPropertyDescriptor() && evt.getPropertyName().equals("valueSet")) {
                ValueSet valueSet = getPropertyDescriptor().getValueSet();
                if (valueSet != null) {
                    // list.setListData(valueSet.getItems());
                    adjustComponents();
                }
            }
        }

        private PropertyDescriptor getPropertyDescriptor() {
            return getBinding().getContext().getPropertySet().getDescriptor(getBinding().getPropertyName());
        }

        @Override
        public void valueChanged(TreeSelectionEvent event) {
            if (getBinding().isAdjustingComponents()) {
                return;
            }
            final Property property = getBinding().getContext().getPropertySet().getProperty(getBinding().getPropertyName());

            TreePath newPath = event.getNewLeadSelectionPath();
            if (newPath != null) {
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) newPath.getLastPathComponent();
                String selectedValue = selectedNode.getUserObject().toString();

                try {
                    System.out.println("Selected node: " + selectedValue);
                    property.setValue(selectedValue);

                    final Property bandNameProperty = getBinding().getContext().getPropertySet().getProperty("bandName");
                    bandNameProperty.setValueFromText(AddLandCoverOp.getValidBandName(selectedValue, product));

                    // Now model is in sync with UI
                    getBinding().clearProblem();
                } catch (ValidationException e) {
                    getBinding().reportProblem(e);
                }
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
                        bandName));
            } else if (product.containsBand(bandName)) {
                throw new ValidationException(MessageFormat.format("The selected product already contains a band named ''{0}''.\n" +
                                "Please choose another one.",
                        bandName));
            }
        }
    }
}
