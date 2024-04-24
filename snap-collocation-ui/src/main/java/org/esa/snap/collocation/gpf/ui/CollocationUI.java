package org.esa.snap.collocation.gpf.ui;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySetDescriptor;
import com.bc.ceres.swing.TableLayout;
import org.esa.snap.collocation.ResamplingType;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.ParameterDescriptorFactory;
import org.esa.snap.core.gpf.descriptor.OperatorDescriptor;
import org.esa.snap.core.gpf.descriptor.PropertySetDescriptorFactory;
import org.esa.snap.graphbuilder.gpf.ui.BaseOperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.ui.AppContext;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.GridLayout;
import java.util.Map;

/**
 * Created by obarrile on 23/04/2019.
 */
public class CollocationUI extends BaseOperatorUI {

    private final JComboBox<String> masterCombo = new JComboBox<>();
    private final JComboBox<ResamplingType> resampleTypeCombo = new JComboBox<>();
    private final JTextField productTypeField = new JTextField("COLLOCATED");
    private final JTextField slavePatternField = new JTextField("${ORIGINAL_NAME}_S${SLAVE_NUMBER_ID}");
    private final JTextField masterPatternField = new JTextField("${ORIGINAL_NAME}_M");

    private JCheckBox copySecMetadataCheckBox;
    private JCheckBox renameMasterCheckBox;
    private JCheckBox renameSlaveCheckBox;

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(operatorName);
        if (operatorSpi == null) {
            throw new IllegalArgumentException("No SPI found for operator name '" + operatorName + "'");
        }

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();

        initParameters();

        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {

        for (ResamplingType resamplingType : ResamplingType.values()) {
            resampleTypeCombo.addItem(resamplingType);
        }
    }

    @Override
    public UIValidation validateParameters() {
        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        String masterNameSelected = (String) masterCombo.getSelectedItem();
        if (hasSourceProducts()) {
            masterCombo.removeAllItems();
            for (Product product : sourceProducts) {
                if (!product.isMultiSize()) {
                    masterCombo.addItem(product.getName());
                }
            }
            if (masterCombo.getItemCount() > 0) {
                masterCombo.setSelectedItem(masterCombo.getItemAt(0));
            }
        }
        if (masterNameSelected != null && masterNameSelected.length() > 0) {
            masterCombo.setSelectedItem(masterNameSelected);
        }

        paramMap.clear();
        paramMap.put("referenceProductName", masterCombo.getSelectedItem());
        paramMap.put("targetProductName", "_collocated");
        paramMap.put("targetProductType", productTypeField.getText());
        paramMap.put("copySecondaryMetadata", copySecMetadataCheckBox.isSelected());
        paramMap.put("renameReferenceComponents", renameMasterCheckBox.isSelected());
        paramMap.put("renameSecondaryComponents", renameSlaveCheckBox.isSelected());
        paramMap.put("referenceComponentPattern", masterPatternField.getText());
        paramMap.put("secondaryComponentPattern", slavePatternField.getText());
        paramMap.put("resamplingType", resampleTypeCombo.getSelectedItem());

    }

    protected void initializeOperatorUI(final String operatorName, final Map<String, Object> parameterMap) {
        this.operatorName = operatorName;
        this.paramMap = parameterMap;

        final OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(operatorName);
        if (operatorSpi == null) {
            throw new IllegalArgumentException("operator " + operatorName + " not found");
        }

        final ParameterDescriptorFactory descriptorFactory = new ParameterDescriptorFactory();
        final OperatorDescriptor operatorDescriptor = operatorSpi.getOperatorDescriptor();
        final PropertySetDescriptor propertySetDescriptor;
        try {
            propertySetDescriptor = PropertySetDescriptorFactory.createForOperator(operatorDescriptor, descriptorFactory.getSourceProductMap());
        } catch (ConversionException e) {
            throw new IllegalStateException("Not able to init OperatorParameterSupport.", e);
        }
        propertySet = PropertyContainer.createMapBacked(paramMap, propertySetDescriptor);

        if (paramMap.isEmpty()) {
            try {
                propertySet.setDefaultValues();
            } catch (IllegalStateException e) {
                // todo - handle exception here
                e.printStackTrace();
            }
        }
    }

    private JComponent createPanel() {

        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTablePadding(4, 4);


        JPanel masterSelectionPanel = new JPanel(new GridLayout(1, 2));
        PropertyDescriptor descriptorReferenceProductName = propertySet.getProperty("referenceProductName").getDescriptor();
        JLabel masterSelectionLabel = new JLabel("Reference product name");
        masterSelectionLabel.setToolTipText(descriptorReferenceProductName.getAttribute("description").toString());
        masterSelectionPanel.add(masterSelectionLabel);
        masterSelectionPanel.add(masterCombo);

        JPanel productTypePanel = new JPanel(new GridLayout(1, 2));
        PropertyDescriptor descriptorProductType = propertySet.getProperty("targetProductType").getDescriptor();
        JLabel productTypeLabel = new JLabel(descriptorProductType.getAttribute("displayName").toString());
        productTypeLabel.setToolTipText(descriptorProductType.getAttribute("description").toString());
        productTypePanel.add(productTypeLabel);
        productTypePanel.add(productTypeField);

        JPanel secMetaPanel = new JPanel(new GridLayout(1, 1));
        PropertyDescriptor descriptorSecMetadata = propertySet.getProperty("copySecondaryMetadata").getDescriptor();
        copySecMetadataCheckBox = new JCheckBox(descriptorSecMetadata.getAttribute("displayName").toString());
        copySecMetadataCheckBox.setSelected(true);
        copySecMetadataCheckBox.setToolTipText(descriptorProductType.getAttribute("description").toString());
        secMetaPanel.add(copySecMetadataCheckBox);

        JPanel renameMasterPanel = new JPanel(new GridLayout(1, 1));
        PropertyDescriptor descriptorRenameMaster = propertySet.getProperty("renameReferenceComponents").getDescriptor();
        renameMasterCheckBox = new JCheckBox(descriptorRenameMaster.getAttribute("displayName").toString());
        renameMasterCheckBox.setSelected(true);
        renameMasterCheckBox.setToolTipText(descriptorRenameMaster.getAttribute("description").toString());
        renameMasterPanel.add(renameMasterCheckBox);

        JPanel renameSlavePanel = new JPanel(new GridLayout(1, 1));
        PropertyDescriptor descriptorRenameSlave = propertySet.getProperty("renameSecondaryComponents").getDescriptor();
        renameSlaveCheckBox = new JCheckBox(descriptorRenameSlave.getAttribute("displayName").toString());
        renameSlaveCheckBox.setSelected(true);
        renameSlaveCheckBox.setToolTipText(descriptorRenameSlave.getAttribute("description").toString());
        renameSlavePanel.add(renameSlaveCheckBox);

        JPanel masterPatternPanel = new JPanel(new GridLayout(1, 2));
        PropertyDescriptor descriptorMasterPattern = propertySet.getProperty("referenceComponentPattern").getDescriptor();
        JLabel masterPatternLabel = new JLabel(descriptorMasterPattern.getAttribute("displayName").toString());
        masterPatternLabel.setToolTipText(descriptorMasterPattern.getAttribute("description").toString());
        masterPatternPanel.add(masterPatternLabel);
        masterPatternPanel.add(masterPatternField);

        JPanel slavePatternPanel = new JPanel(new GridLayout(1, 2));
        PropertyDescriptor descriptorSlavePattern = propertySet.getProperty("secondaryComponentPattern").getDescriptor();
        JLabel slavePatternLabel = new JLabel(descriptorSlavePattern.getAttribute("displayName").toString());
        slavePatternLabel.setToolTipText(descriptorSlavePattern.getAttribute("description").toString());
        slavePatternPanel.add(slavePatternLabel);
        slavePatternPanel.add(slavePatternField);

        JPanel resampleTypePanel = new JPanel(new GridLayout(1, 2));
        PropertyDescriptor descriptorResampleType = propertySet.getProperty("resamplingType").getDescriptor();
        JLabel resampleTypeLabel = new JLabel(descriptorResampleType.getAttribute("displayName").toString());
        resampleTypeLabel.setToolTipText(descriptorResampleType.getAttribute("description").toString());
        resampleTypePanel.add(resampleTypeLabel);
        resampleTypePanel.add(resampleTypeCombo);

        final JPanel parametersPanel = new JPanel(tableLayout);
        parametersPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        parametersPanel.add(masterSelectionPanel);
        parametersPanel.add(secMetaPanel);
        parametersPanel.add(renameMasterPanel);
        parametersPanel.add(renameSlavePanel);
        parametersPanel.add(masterPatternPanel);
        parametersPanel.add(slavePatternPanel);
        parametersPanel.add(resampleTypePanel);
        parametersPanel.add(tableLayout.createVerticalSpacer());
        return parametersPanel;
    }
}
