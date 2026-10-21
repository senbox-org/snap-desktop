/*
 * Copyright (C) 2026 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.rcp.actions.window;

import eu.esa.snap.core.datamodel.group.BandGroup;
import eu.esa.snap.core.datamodel.group.BandGroupsManager;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.ui.ModalDialog;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

@ActionID(
        category = "View",
        id = "OpenCommonRasterDataAction"
)
@ActionRegistration(
        displayName = "#CTL_OpenCommonRasterDataAction_MenuText",
        popupText = "#CTL_OpenCommonRasterDataAction_MenuText",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Context/Product/Product", position = 55, separatorAfter = 56)
})
@NbBundle.Messages({
        "CTL_OpenCommonRasterDataAction_MenuText=Open Common Raster Data...",
        "CTL_OpenCommonRasterDataAction_ShortDescription=Open raster data with the same name in all selected products"
})
public class OpenCommonRasterDataAction extends AbstractAction implements ContextAwareAction, LookupListener {

    private static final String HELP_ID = "openCommonRasterData";

    private final Lookup lookup;

    public OpenCommonRasterDataAction() {
        this(Utilities.actionsGlobalContext());
    }

    public OpenCommonRasterDataAction(Lookup lookup) {
        super(Bundle.CTL_OpenCommonRasterDataAction_MenuText());
        this.lookup = lookup;
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_OpenCommonRasterDataAction_ShortDescription());

        Lookup.Result<ProductNode> productNodeResult = lookup.lookupResult(ProductNode.class);
        productNodeResult.addLookupListener(WeakListeners.create(LookupListener.class, this, productNodeResult));
        updateState();
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new OpenCommonRasterDataAction(actionContext);
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {
        updateState();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        List<Product> selectedProducts = getSelectedProducts();
        List<String> commonRasterNames = getCommonRasterDataNodeNames(selectedProducts);
        if (selectedProducts.size() < 2 || commonRasterNames.isEmpty()) {
            return;
        }

        Window parent = e.getSource() instanceof JComponent
                ? SwingUtilities.getWindowAncestor((JComponent) e.getSource())
                : null;
        CommonRasterDataDialog dialog = new CommonRasterDataDialog(parent, commonRasterNames);
        if (dialog.show() == ModalDialog.ID_OK) {
            for (RasterDataNode rasterDataNode : getRasterDataNodes(selectedProducts, dialog.getSelectedRasterNames())) {
                OpenImageViewAction.openImageView(rasterDataNode);
            }
        }
    }

    private void updateState() {
        List<Product> selectedProducts = getSelectedProducts();
        setEnabled(selectedProducts.size() > 1 && !getCommonRasterDataNodeNames(selectedProducts).isEmpty());
    }

    private List<Product> getSelectedProducts() {
        Collection<? extends ProductNode> selectedNodes = lookup.lookupAll(ProductNode.class);
        return selectedNodes.stream()
                .map(ProductNode::getProduct)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    static List<String> getCommonRasterDataNodeNames(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> commonNames = getRasterDataNodeNames(products.get(0));
        for (int i = 1; i < products.size() && !commonNames.isEmpty(); i++) {
            commonNames.retainAll(getRasterDataNodeNames(products.get(i)));
        }

        return sortGroupedRasterNamesFirst(products, commonNames);
    }

    static List<RasterDataNode> getRasterDataNodes(List<Product> products, List<String> rasterNames) {
        List<RasterDataNode> rasterDataNodes = new ArrayList<>();
        for (String rasterName : rasterNames) {
            for (Product product : products) {
                RasterDataNode rasterDataNode = product.getRasterDataNode(rasterName);
                if (rasterDataNode != null) {
                    rasterDataNodes.add(rasterDataNode);
                }
            }
        }
        return rasterDataNodes;
    }

    private static Set<String> getRasterDataNodeNames(Product product) {
        Set<String> names = new LinkedHashSet<>();
        for (RasterDataNode rasterDataNode : product.getRasterDataNodes()) {
            names.add(rasterDataNode.getName());
        }
        return names;
    }

    private static List<String> sortGroupedRasterNamesFirst(List<Product> products, Set<String> commonNames) {
        Set<String> groupedBandNames = getGroupedBandNames(products);
        List<String> groupedNames = new ArrayList<>();
        List<String> otherNames = new ArrayList<>();
        for (String commonName : commonNames) {
            if (groupedBandNames.contains(commonName)) {
                groupedNames.add(commonName);
            } else {
                otherNames.add(commonName);
            }
        }
        Collections.sort(groupedNames);
        Collections.sort(otherNames);
        groupedNames.addAll(otherNames);
        return groupedNames;
    }

    private static Set<String> getGroupedBandNames(List<Product> products) {
        Set<String> groupedBandNames = new HashSet<>();
        for (Product product : products) {
            addMatchingBandNames(groupedBandNames, product.getAutoGrouping(), product);
        }
        try {
            BandGroupsManager bandGroupsManager = BandGroupsManager.getInstance();
            for (Product product : products) {
                for (BandGroup bandGroup : bandGroupsManager.getGroupsMatchingProduct(product)) {
                    addMatchingBandNames(groupedBandNames, bandGroup, product);
                }
            }
        } catch (IOException ignore) {
            // User band groups are only used for preferred ordering. The action remains usable without them.
        }
        return groupedBandNames;
    }

    private static void addMatchingBandNames(Set<String> target, BandGroup bandGroup, Product product) {
        if (bandGroup == null) {
            return;
        }
        Collections.addAll(target, bandGroup.getMatchingBandNames(product));
    }

    static List<String> getRegexMatchingRasterNames(List<String> rasterNames, String regex) throws PatternSyntaxException {
        Pattern pattern = Pattern.compile(regex);
        return rasterNames.stream()
                .filter(rasterName -> pattern.matcher(rasterName).find())
                .collect(Collectors.toList());
    }

    private static class CommonRasterDataDialog extends ModalDialog {

        private final List<JCheckBox> rasterCheckBoxes;
        private final List<String> rasterNames;
        private JTextField regexField;

        private CommonRasterDataDialog(Window parent, List<String> rasterNames) {
            super(parent, Bundle.CTL_OpenCommonRasterDataAction_MenuText(), ModalDialog.ID_OK_CANCEL, HELP_ID);
            this.rasterNames = new ArrayList<>(rasterNames);
            rasterCheckBoxes = new ArrayList<>(rasterNames.size());
            for (String rasterName : rasterNames) {
                JCheckBox checkBox = new JCheckBox(rasterName);
                rasterCheckBoxes.add(checkBox);
            }
            if (!rasterCheckBoxes.isEmpty()) {
                rasterCheckBoxes.get(0).setSelected(true);
            }
            setContent(createContent());
        }

        private JComponent createContent() {
            JPanel content = new JPanel(new BorderLayout(4, 4));
            content.add(createRegexPanel(), BorderLayout.NORTH);

            JPanel checkBoxPanel = new JPanel();
            checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel, BoxLayout.Y_AXIS));
            for (JCheckBox rasterCheckBox : rasterCheckBoxes) {
                checkBoxPanel.add(rasterCheckBox);
            }

            JScrollPane scrollPane = new JScrollPane(checkBoxPanel);
            scrollPane.setPreferredSize(new Dimension(360, 260));
            scrollPane.getVerticalScrollBar().setUnitIncrement(20);
            content.add(scrollPane, BorderLayout.CENTER);

            JCheckBox selectAllCheckBox = new JCheckBox("Select all");
            JCheckBox selectNoneCheckBox = new JCheckBox("Select none");
            selectAllCheckBox.setMnemonic('a');
            selectNoneCheckBox.setMnemonic('n');
            selectAllCheckBox.addActionListener(e -> setAllSelected(true));
            selectNoneCheckBox.addActionListener(e -> setAllSelected(false));

            JPanel checkPane = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
            checkPane.add(selectAllCheckBox);
            checkPane.add(selectNoneCheckBox);
            content.add(checkPane, BorderLayout.SOUTH);

            return content;
        }

        private JComponent createRegexPanel() {
            regexField = new JTextField();
            regexField.addActionListener(e -> applyRegexSelection());

            JButton applyRegexButton = new JButton("Apply regex");
            applyRegexButton.addActionListener(e -> applyRegexSelection());

            JPanel regexPanel = new JPanel(new BorderLayout(4, 4));
            regexPanel.add(new JLabel("Regex:"), BorderLayout.WEST);
            regexPanel.add(regexField, BorderLayout.CENTER);
            regexPanel.add(applyRegexButton, BorderLayout.EAST);
            return regexPanel;
        }

        private void setAllSelected(boolean selected) {
            for (JCheckBox rasterCheckBox : rasterCheckBoxes) {
                rasterCheckBox.setSelected(selected);
            }
        }

        private void applyRegexSelection() {
            String regex = regexField.getText();
            try {
                setSelectedRasterNames(getRegexMatchingRasterNames(rasterNames, regex));
            } catch (PatternSyntaxException e) {
                showInformationDialog("Invalid regular expression:\n" + e.getDescription());
            }
        }

        private void setSelectedRasterNames(List<String> selectedRasterNames) {
            Set<String> selectedRasterNameSet = new HashSet<>(selectedRasterNames);
            for (JCheckBox rasterCheckBox : rasterCheckBoxes) {
                rasterCheckBox.setSelected(selectedRasterNameSet.contains(rasterCheckBox.getText()));
            }
        }

        private List<String> getSelectedRasterNames() {
            List<String> selectedRasterNames = new ArrayList<>();
            for (JCheckBox rasterCheckBox : rasterCheckBoxes) {
                if (rasterCheckBox.isSelected()) {
                    selectedRasterNames.add(rasterCheckBox.getText());
                }
            }
            return selectedRasterNames;
        }

        @Override
        protected boolean verifyUserInput() {
            if (getSelectedRasterNames().isEmpty()) {
                showInformationDialog("No raster data selected.\nPlease select at least one raster data item.");
                return false;
            }
            return true;
        }
    }
}
