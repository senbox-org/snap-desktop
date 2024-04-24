package org.esa.snap.remote.execution.operator;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.swing.binding.Binding;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.internal.ListSelectionAdapter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductIOPlugInManager;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.descriptor.OperatorDescriptor;
import org.esa.snap.core.gpf.ui.OperatorMenu;
import org.esa.snap.core.gpf.ui.OperatorParameterSupport;
import org.esa.snap.core.gpf.ui.ParameterUpdater;
import org.esa.snap.core.gpf.ui.TargetProductSelectorModel;
import org.esa.snap.rcp.actions.file.SaveProductAsAction;
import org.esa.snap.remote.execution.RemoteExecutionOp;
import org.esa.snap.remote.execution.converters.RemoteMachinePropertiesConverter;
import org.esa.snap.remote.execution.converters.SourceProductFilesConverter;
import org.esa.snap.remote.execution.local.folder.IMountLocalSharedFolderCallback;
import org.esa.snap.remote.execution.local.folder.IMountLocalSharedFolderResult;
import org.esa.snap.remote.execution.local.folder.IUnmountLocalSharedFolderCallback;
import org.esa.snap.remote.execution.machines.EditRemoteMachineCredentialsDialog;
import org.esa.snap.remote.execution.machines.RemoteMachineProperties;
import org.esa.snap.remote.execution.topology.LinuxRemoteTopologyPanel;
import org.esa.snap.remote.execution.topology.MacRemoteTopologyPanel;
import org.esa.snap.remote.execution.topology.ReadRemoteTopologyTimerRunnable;
import org.esa.snap.remote.execution.topology.RemoteTopology;
import org.esa.snap.remote.execution.topology.RemoteTopologyPanel;
import org.esa.snap.remote.execution.topology.WindowsRemoteTopologyPanel;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.loading.AbstractModalDialog;
import org.esa.snap.ui.loading.CustomFileChooser;
import org.esa.snap.ui.loading.LabelListCellRenderer;
import org.esa.snap.ui.loading.LoadingIndicator;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.border.TitledBorder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.io.File;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 24/12/2018.
 */
public class RemoteExecutionDialog extends AbstractModalDialog {

    private static final String SOURCE_PRODUCT_FILES_PROPERTY = "sourceProductFiles";
    private static final String REMOTE_MACHINES_PROPERTY = "remoteMachines";
    private static final String REMOTE_SHARED_FOLDER_PATH_PROPERTY = "remoteSharedFolderPath";
    private static final String REMOTE_SHARED_FOLDER_USERNAME_PROPERTY = "remoteSharedFolderUsername";
    private static final String REMOTE_SHARED_FOLDER_PASSWORD_PROPERTY = "remoteSharedFolderPassword";
    public static final String LOCAL_SHARED_FOLDER_PATH_PROPERTY = "localSharedFolderPath";
    public static final String LOCAL_PASSWORD_PROPERTY = "localPassword";
    private static final String SLAVE_GRAPH_FILE_PATH_PROPERTY = "slaveGraphFilePath";
    private static final String SLAVE_PRODUCTS_FORMAT_NAME_PROPERTY = "slaveProductsFormatName";
    private static final String MASTER_GRAPH_FILE_PATH_PROPERTY = "masterGraphFilePath";
    private static final String MASTER_PRODUCT_FILE_PATH_PROPERTY = "masterProductFilePath";
    private static final String MASTER_PRODUCT_FOLDER_PATH_PROPERTY = "masterProductFolderPath";
    private static final String MASTER_PRODUCT_FILE_NAME_PROPERTY = "masterProductFileName";
    private static final String MASTER_PRODUCT_FORMAT_NAME_PROPERTY = "masterProductFormatName";
    private static final String CONTINUE_ON_FAILURE_NAME_PROPERTY = "continueOnFailure";
    private static final String CAN_SAVE_TARGET_PRODUCT_PROPERTY = "canSaveTargetProduct";
    private static final String OPEN_TARGET_PRODUCT_PROPERTY = "openTargetProduct";

    private final AppContext appContext;
    private final BindingContext bindingContext;
    private final OperatorParameterSupport parameterSupport;

    private JTextField slaveGraphFilePathTextField;
    private JTextField masterGraphFilePathTextField;
    private JList<String> sourceProductsList;
    private RemoteTopologyPanel remoteTopologyPanel;
    private JCheckBox continueOnFailureCheckBox;
    private IMountLocalSharedFolderResult mountLocalSharedFolderResult;
    private JCheckBox canSaveTargetProductCheckBox;
    private JCheckBox openTargetProductCheckBox;
    private JTextField masterProductFolderPathTextField;
    private JTextField masterProductNameTextField;
    private JComboBox<String> slaveProductsFormatNameComboBox;
    private JComboBox<String> masterProductFormatNameComboBox;
    private List<JComponent> masterProductEnabledComponents;
    private Path lastSelectedFolderPath;

    public RemoteExecutionDialog(AppContext appContext, Window parent) {
        super(parent, "Remote execution", false, "remoteExecutionProcessor");

        this.appContext = appContext;

        String operatorName = OperatorSpi.getOperatorAlias(RemoteExecutionOp.class);
        OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(operatorName);
        if (operatorSpi == null) {
            throw new IllegalArgumentException("No SPI found for operator name '" + operatorName + "'.");
        }

        ParameterUpdater parameterUpdater = new ParameterUpdater() {
            @Override
            public void handleParameterSaveRequest(Map<String, Object> parameterMap) {
                // do nothing
            }

            @Override
            public void handleParameterLoadRequest(Map<String, Object> parameterMap) {
                // set the source products
                String[] files = (String[]) parameterMap.get(SOURCE_PRODUCT_FILES_PROPERTY);
                if (files == null) {
                    files = new String[0];
                }
                setListItems(files, SOURCE_PRODUCT_FILES_PROPERTY);

                // set the remote machines
                RemoteMachineProperties[] remoteMachines = (RemoteMachineProperties[]) parameterMap.get(REMOTE_MACHINES_PROPERTY);
                if (remoteMachines == null) {
                    remoteMachines = new RemoteMachineProperties[0];
                }
                setListItems(remoteMachines, REMOTE_MACHINES_PROPERTY);

                // refresh the target product components
                refreshTargetProductEnabledComponents();
                Boolean canSaveTargetProduct = (Boolean) parameterMap.get(CAN_SAVE_TARGET_PRODUCT_PROPERTY);
                if (canSaveTargetProduct != null && canSaveTargetProduct) {
                    refreshOpenTargetProductSelectedState();
                }
            }
        };

        Map<String, Object> parameterMap = new HashMap<>();
        PropertySet propertySet = PropertyContainer.createMapBacked(parameterMap, CloudExploitationPlatformItem.class);
        propertySet.getDescriptor(REMOTE_MACHINES_PROPERTY).setConverter(new RemoteMachinePropertiesConverter());
        propertySet.getDescriptor(SOURCE_PRODUCT_FILES_PROPERTY).setConverter(new SourceProductFilesConverter());
        propertySet.setDefaultValues();
        propertySet.addPropertyChangeListener(evt -> {
            String propertyName = evt.getPropertyName();
            if (propertyName.equals(CAN_SAVE_TARGET_PRODUCT_PROPERTY)) {
                refreshTargetProductEnabledComponents();
                Boolean selected = (Boolean) evt.getNewValue();
                if (selected) {
                    refreshOpenTargetProductSelectedState();
                }
            } else if (propertyName.equals(MASTER_PRODUCT_FORMAT_NAME_PROPERTY)) {
                refreshOpenTargetProductEnabledState();
                refreshOpenTargetProductSelectedState();
            }
        });

        OperatorDescriptor baseOperatorDescriptor = operatorSpi.getOperatorDescriptor();
        OperatorDescriptor operatorDescriptor = new OperatorDescriptorWrapperImpl(baseOperatorDescriptor);

        this.parameterSupport = new OperatorParameterSupport(operatorDescriptor, propertySet, parameterMap, parameterUpdater) {
            @Override
            public void fromDomElement(DomElement parametersElement) throws ValidationException, ConversionException {
                Property remoteMachinesProperty = getPropertySet().getProperty(REMOTE_MACHINES_PROPERTY);
                remoteMachinesProperty.getDescriptor().setValueSet(new EmptyValueSetExtended(new RemoteMachineProperties[0]));

                Property sourceProductFilesProperty = getPropertySet().getProperty(SOURCE_PRODUCT_FILES_PROPERTY);
                sourceProductFilesProperty.getDescriptor().setValueSet(new EmptyValueSetExtended(new String[0]));

                try {
                    super.fromDomElement(parametersElement);
                } finally {
                    RemoteMachineProperties[] remoteMachines = (RemoteMachineProperties[]) getParameterMap().get(REMOTE_MACHINES_PROPERTY);
                    if (remoteMachines == null) {
                        remoteMachines = new RemoteMachineProperties[0];
                    }
                    remoteMachinesProperty.getDescriptor().setValueSet(new ValueSet(remoteMachines));

                    String[] sourceProductFiles = (String[]) getParameterMap().get(SOURCE_PRODUCT_FILES_PROPERTY);
                    if (sourceProductFiles == null) {
                        sourceProductFiles = new String[0];
                    }
                    sourceProductFilesProperty.getDescriptor().setValueSet(new ValueSet(sourceProductFiles));
                }
            }
        };
        this.bindingContext = new BindingContext(this.parameterSupport.getPropertySet());

        JDialog dialog = getJDialog();
        OperatorMenu operatorMenu = new OperatorMenu(dialog, operatorDescriptor, this.parameterSupport, appContext, getHelpID());
        dialog.setJMenuBar(operatorMenu.createDefaultMenu());
    }

    @Override
    protected void onAboutToShow() {
        JDialog dialog = getJDialog();
        dialog.setMinimumSize(new Dimension(650, 590));

        LoadingIndicator loadingIndicator = getLoadingIndicator();
        int threadId = getNewCurrentThreadId();

        Path remoteTopologyFilePath = getRemoteTopologyFilePath();
        ReadRemoteTopologyTimerRunnable runnable = new ReadRemoteTopologyTimerRunnable(this, loadingIndicator, threadId, remoteTopologyFilePath) {
            @Override
            protected void onSuccessfullyFinish(RemoteTopology remoteTopology) {
                onFinishReadingRemoteTopoloy(remoteTopology);
            }
        };
        runnable.executeAsync();
    }

    @Override
    protected void registerEscapeKey(ActionListener cancelActionListener) {
        // do nothing
    }

    private static String[] getAvailableFormatNames() {
        String[] formatNames = ProductIOPlugInManager.getInstance().getAllProductWriterFormatStrings();
        if (formatNames.length > 1) {
            List<String> items = new ArrayList<>(formatNames.length);
            Collections.addAll(items, formatNames);
            Comparator<String> comparator = String::compareTo;
            Collections.sort(items, comparator);
            items.toArray(formatNames);
        }
        return formatNames;
    }

    @Override
    protected void setEnabledComponentsWhileLoading(boolean enabled) {
        super.setEnabledComponentsWhileLoading(enabled);

        refreshTargetProductEnabledComponents();
    }

    private static JComboBox<String> buildProductFormatNamesComboBox(Insets defaultListItemMargins, int textFieldPreferredHeight, String[] availableFormatNames) {
        JComboBox<String> productFormatNameComboBox = new JComboBox<>(availableFormatNames);
        Dimension formatNameComboBoxSize = productFormatNameComboBox.getPreferredSize();
        formatNameComboBoxSize.height = textFieldPreferredHeight;
        productFormatNameComboBox.setPreferredSize(formatNameComboBoxSize);
        productFormatNameComboBox.setMinimumSize(formatNameComboBoxSize);
        LabelListCellRenderer<String> renderer = new LabelListCellRenderer<>(defaultListItemMargins) {
            @Override
            protected String getItemDisplayText(String value) {
                return value;
            }
        };
        productFormatNameComboBox.setMaximumRowCount(5);
        productFormatNameComboBox.setRenderer(renderer);
        productFormatNameComboBox.setBackground(new Color(0, 0, 0, 0)); // set the transparent color
        productFormatNameComboBox.setOpaque(true);

        return productFormatNameComboBox;
    }

    @Override
    protected JPanel buildContentPanel(int gapBetweenColumns, int gapBetweenRows) {
        Insets defaultTextFieldMargins = buildDefaultTextFieldMargins();
        Insets defaultListItemMargins = buildDefaultListItemMargins();

        this.slaveGraphFilePathTextField = new JTextField();
        this.slaveGraphFilePathTextField.setMargin(defaultTextFieldMargins);
        this.bindingContext.bind(SLAVE_GRAPH_FILE_PATH_PROPERTY, this.slaveGraphFilePathTextField);

        this.continueOnFailureCheckBox = new JCheckBox("Continue when a remote execution fails");
        this.continueOnFailureCheckBox.setMargin(new Insets(0, 0, 0, 0));
        this.continueOnFailureCheckBox.setFocusable(false);
        Binding canContinueOnFailureBinding = this.bindingContext.bind(CONTINUE_ON_FAILURE_NAME_PROPERTY, this.continueOnFailureCheckBox);
        canContinueOnFailureBinding.setPropertyValue(this.continueOnFailureCheckBox.isSelected());

        int textFieldPreferredHeight = this.slaveGraphFilePathTextField.getPreferredSize().height;

        createRemoteTopologyPanel(defaultTextFieldMargins, defaultListItemMargins, textFieldPreferredHeight, gapBetweenColumns, gapBetweenRows);

        String[] formatNames = getAvailableFormatNames();

        JPanel inputPanel = buildInputPanel(gapBetweenColumns, gapBetweenRows, textFieldPreferredHeight, defaultListItemMargins, formatNames);

        JPanel outputMasterProductPanel = buildOutputMasterProductPanel(gapBetweenColumns, gapBetweenRows, defaultTextFieldMargins, defaultListItemMargins,
                                                                        textFieldPreferredHeight, formatNames);

        JPanel contentPanel = new JPanel(new GridBagLayout());

        GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST, 1, 1, 0, 0);
        contentPanel.add(this.remoteTopologyPanel, c);

        c = SwingUtils.buildConstraints(0, 2, GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST, 1, 1, gapBetweenRows, 0);
        contentPanel.add(inputPanel, c);

        c = SwingUtils.buildConstraints(0, 3, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTHWEST, 1, 1, gapBetweenRows, 0);
        contentPanel.add(outputMasterProductPanel, c);

        computePanelFirstColumn(contentPanel);

        return contentPanel;
    }

    public void setData(List<File> sourceProductFiles, File slaveGraphFile, String slaveProductsFormaName) {
        String[] sourceFiles = new String[sourceProductFiles.size()];
        int i = 0;
        for (final File file : sourceProductFiles){
            sourceFiles[i] = file.getPath();
            ++i;
        }
        setListItems(sourceFiles, SOURCE_PRODUCT_FILES_PROPERTY);
        setPropertyValue(slaveGraphFile.getPath(), SLAVE_GRAPH_FILE_PATH_PROPERTY);
        setPropertyValue(slaveProductsFormaName, SLAVE_PRODUCTS_FORMAT_NAME_PROPERTY);
    }

    @Override
    public void close() {
        if (this.mountLocalSharedFolderResult == null) {
            super.close();
        } else {
            LoadingIndicator loadingIndicator = getLoadingIndicator();
            int threadId = getNewCurrentThreadId();
            IUnmountLocalSharedFolderCallback callback = exception -> {
                RemoteExecutionDialog.this.mountLocalSharedFolderResult = null;
                RemoteExecutionDialog.super.close();
            };
            this.mountLocalSharedFolderResult.unmountLocalSharedFolderAsync(loadingIndicator, threadId, callback);
        }
    }

    private void addServerCredentialsButtonPressed() {
        EditRemoteMachineCredentialsDialog dialog = new EditRemoteMachineCredentialsDialog(getJDialog(), null) {
            @Override
            protected void successfullyCloseDialog(RemoteMachineProperties oldSSHServerCredentials, RemoteMachineProperties newSSHServerCredentials) {
                super.successfullyCloseDialog(oldSSHServerCredentials, newSSHServerCredentials);

                addSSHServerCredentialItem(newSSHServerCredentials);
            }
        };
        dialog.show();
    }

    private void addSSHServerCredentialItem(RemoteMachineProperties newSSHServerCredentials) {
        RemoteMachineProperties[] remoteMachines = new RemoteMachineProperties[] {newSSHServerCredentials};
        addListItems(remoteMachines, RemoteMachineProperties.class, REMOTE_MACHINES_PROPERTY);
    }

    private void editSSHServerCredentialItem(RemoteMachineProperties oldSSHServerCredentials, RemoteMachineProperties newSSHServerCredentials) {
        ListModel<RemoteMachineProperties> listModel = this.remoteTopologyPanel.getRemoteMachinesList().getModel();
        RemoteMachineProperties[] remoteMachines = new RemoteMachineProperties[listModel.getSize()];
        for (int i=0; i<listModel.getSize(); i++) {
            RemoteMachineProperties existingRemoteMachine = listModel.getElementAt(i);
            remoteMachines[i] = (existingRemoteMachine == oldSSHServerCredentials) ? newSSHServerCredentials : existingRemoteMachine;
        }
        setListItems(remoteMachines, REMOTE_MACHINES_PROPERTY);
    }

    private void editServerCredentialsButtonPressed() {
        RemoteMachineProperties selectedSSHServerCredentials = this.remoteTopologyPanel.getRemoteMachinesList().getSelectedValue();
        if (selectedSSHServerCredentials != null) {
            EditRemoteMachineCredentialsDialog dialog = new EditRemoteMachineCredentialsDialog(getJDialog(), selectedSSHServerCredentials) {
                @Override
                protected void successfullyCloseDialog(RemoteMachineProperties oldSSHServerCredentials, RemoteMachineProperties newSSHServerCredentials) {
                    super.successfullyCloseDialog(oldSSHServerCredentials, newSSHServerCredentials);

                    editSSHServerCredentialItem(oldSSHServerCredentials, newSSHServerCredentials);
                }
            };
            dialog.show();
        }
    }

    private void removeServerCredentialsButtonPressed() {
        RemoteMachineProperties selectedRemoteMachine = this.remoteTopologyPanel.getRemoteMachinesList().getSelectedValue();
        if (selectedRemoteMachine != null) {
            ListModel<RemoteMachineProperties> listModel = this.remoteTopologyPanel.getRemoteMachinesList().getModel();
            RemoteMachineProperties[] remoteMachines = new RemoteMachineProperties[listModel.getSize()-1];
            for (int i=0, index = 0; i<listModel.getSize(); i++) {
                RemoteMachineProperties existingRemoteMachine = listModel.getElementAt(i);
                if (existingRemoteMachine != selectedRemoteMachine) {
                    remoteMachines[index++] = existingRemoteMachine;
                }
            }
            setListItems(remoteMachines, REMOTE_MACHINES_PROPERTY);
        }
    }

    @Override
    protected JPanel buildButtonsPanel(ActionListener cancelActionListener) {
        ActionListener runActionListener = event -> runButtonPressed();

        JButton finishButton = buildDialogButton("Run");
        finishButton.addActionListener(runActionListener);
        JButton cancelButton = buildDialogButton("Cancel");
        cancelButton.addActionListener(cancelActionListener);

        addComponentToAllwaysEnabledList(cancelButton);

        JPanel buttonsGridPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        buttonsGridPanel.add(finishButton);
        buttonsGridPanel.add(cancelButton);

        JPanel buttonsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 1, 1, 0, 0);
        buttonsPanel.add(new JLabel(), c);

        c = SwingUtils.buildConstraints(1, 0, GridBagConstraints.NONE, GridBagConstraints.CENTER, 1, 1, 0, 0);
        buttonsPanel.add(this.continueOnFailureCheckBox, c);

        c = SwingUtils.buildConstraints(2, 0, GridBagConstraints.NONE, GridBagConstraints.CENTER, 1, 1, 0, 5);
        buttonsPanel.add(buttonsGridPanel, c);

        return buttonsPanel;
    }

    private void createRemoteTopologyPanel(Insets defaultTextFieldMargins, Insets defaultListItemMargins, int textFieldPreferredHeight, int gapBetweenColumns, int gapBetweenRows) {
        if (SystemUtils.IS_OS_LINUX) {
            this.remoteTopologyPanel = new LinuxRemoteTopologyPanel(getJDialog(), defaultTextFieldMargins, defaultListItemMargins);
        } else if (SystemUtils.IS_OS_WINDOWS) {
            this.remoteTopologyPanel = new WindowsRemoteTopologyPanel(getJDialog(), defaultTextFieldMargins, defaultListItemMargins);
        } else if (SystemUtils.IS_OS_MAC) {
            this.remoteTopologyPanel = new MacRemoteTopologyPanel(getJDialog(), defaultTextFieldMargins, defaultListItemMargins);
        } else {
            throw new UnsupportedOperationException("Unsupported operating system '" + SystemUtils.OS_NAME + "'.");
        }

        this.remoteTopologyPanel.getRemoteSharedFolderPathTextField().setColumns(70);
        this.bindingContext.bind(REMOTE_SHARED_FOLDER_PATH_PROPERTY, this.remoteTopologyPanel.getRemoteSharedFolderPathTextField());

        this.bindingContext.bind(REMOTE_SHARED_FOLDER_USERNAME_PROPERTY, this.remoteTopologyPanel.getRemoteUsernameTextField());

        this.bindingContext.bind(REMOTE_SHARED_FOLDER_PASSWORD_PROPERTY, this.remoteTopologyPanel.getRemotePasswordTextField());

        this.bindingContext.bind(LOCAL_SHARED_FOLDER_PATH_PROPERTY, this.remoteTopologyPanel.getLocalSharedFolderPathTextField());

        JPasswordField localPasswordTextField = this.remoteTopologyPanel.getLocalPasswordTextField();
        if (localPasswordTextField != null) {
            localPasswordTextField.setMargin(defaultTextFieldMargins);
            this.bindingContext.bind(LOCAL_PASSWORD_PROPERTY, localPasswordTextField);
        }

        ActionListener addButtonListener = event -> addServerCredentialsButtonPressed();
        ActionListener editButtonListener = event -> editServerCredentialsButtonPressed();
        ActionListener removeButtonListener = event -> removeServerCredentialsButtonPressed();
        ActionListener browseLocalSharedFolderButtonListener = event -> selectLocalSharedFolderPath();

        this.bindingContext.bind(REMOTE_MACHINES_PROPERTY, new ListSelectionAdapter(this.remoteTopologyPanel.getRemoteMachinesList()));
        this.remoteTopologyPanel.getRemoteMachinesList().setVisibleRowCount(5);

        JPanel remoteMachinesButtonsPanel = RemoteTopologyPanel.buildVerticalButtonsPanel(addButtonListener, editButtonListener, removeButtonListener,
                textFieldPreferredHeight, gapBetweenRows);

        this.remoteTopologyPanel.addComponents(gapBetweenColumns, gapBetweenRows, browseLocalSharedFolderButtonListener, remoteMachinesButtonsPanel);
        this.remoteTopologyPanel.setBorder(new TitledBorder("Remote execution"));
    }

    private JPanel buildInputPanel(int gapBetweenColumns, int gapBetweenRows, int textFieldPreferredHeight, Insets defaultListItemMargins, String[] availableFormatNames) {
        ActionListener slaveGraphBrowseButtonListener = event -> selectSlaveGraphFile();

        JButton slaveGraphBrowseButton = SwingUtils.buildBrowseButton(slaveGraphBrowseButtonListener, textFieldPreferredHeight);

        this.sourceProductsList = new JList<>(new DefaultListModel<>());
        LabelListCellRenderer<String> sourceProductsRenderer = new LabelListCellRenderer<>(defaultListItemMargins) {
            @Override
            protected String getItemDisplayText(String value) {
                return value;//remoteTopologyPanel.normalizeFileSeparator(value.toString());
            }
        };
        this.sourceProductsList.setCellRenderer(sourceProductsRenderer);
        this.sourceProductsList.setVisibleRowCount(4);
        this.bindingContext.bind(SOURCE_PRODUCT_FILES_PROPERTY, new ListSelectionAdapter(this.sourceProductsList));

        this.slaveProductsFormatNameComboBox = buildProductFormatNamesComboBox(defaultListItemMargins, textFieldPreferredHeight, availableFormatNames);
        Binding formatNameBinding = this.bindingContext.bind(SLAVE_PRODUCTS_FORMAT_NAME_PROPERTY, this.slaveProductsFormatNameComboBox);

        if (org.esa.snap.core.util.StringUtils.contains(availableFormatNames, ProductIO.DEFAULT_FORMAT_NAME)) {
            formatNameBinding.setPropertyValue(ProductIO.DEFAULT_FORMAT_NAME);
        } else {
            formatNameBinding.setPropertyValue(availableFormatNames[0]);
        }

        ActionListener addSourceProductsButtonListener = event -> showDialogToSelectSourceProducts();
        ActionListener removeSourceProductsButtonListener = event -> removeSelectedSourceProducts();

        JPanel sourceProductButtonsPanel = RemoteTopologyPanel.buildVerticalButtonsPanel(addSourceProductsButtonListener, null, removeSourceProductsButtonListener, textFieldPreferredHeight, gapBetweenRows);

        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(new TitledBorder("Input"));

        GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, 0, 0);
        inputPanel.add(new JLabel("Slave graph file path"), c);

        c = SwingUtils.buildConstraints(1, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
        inputPanel.add(this.slaveGraphFilePathTextField, c);

        c = SwingUtils.buildConstraints(2, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
        inputPanel.add(slaveGraphBrowseButton, c);

        c = SwingUtils.buildConstraints(0, 1, GridBagConstraints.NONE, GridBagConstraints.NORTHWEST, 1, 1, gapBetweenRows, 0);
        inputPanel.add(new JLabel("Source products"), c);

        c = SwingUtils.buildConstraints(1, 1, GridBagConstraints.BOTH, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
        inputPanel.add(new JScrollPane(this.sourceProductsList), c);

        c = SwingUtils.buildConstraints(2, 1, GridBagConstraints.NONE, GridBagConstraints.NORTHWEST, 1, 1, gapBetweenRows, gapBetweenColumns);
        inputPanel.add(sourceProductButtonsPanel, c);

        c = SwingUtils.buildConstraints(0, 2, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
        inputPanel.add(new JLabel("Save slave products as"), c);

        c = SwingUtils.buildConstraints(1, 2, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
        inputPanel.add(this.slaveProductsFormatNameComboBox, c);

        return inputPanel;
    }

    private boolean canSaveTargetProductToFile() {
        return this.canSaveTargetProductCheckBox.isEnabled() && this.canSaveTargetProductCheckBox.isSelected();
    }

    private void refreshOpenTargetProductEnabledState() {
        boolean enabled = (existReaderPluginForMasterProduct() && canSaveTargetProductToFile());
        this.openTargetProductCheckBox.setEnabled(enabled);
    }

    private void refreshOpenTargetProductSelectedState() {
        this.bindingContext.getBinding(OPEN_TARGET_PRODUCT_PROPERTY).setPropertyValue(existReaderPluginForMasterProduct());
    }

    private boolean existReaderPluginForMasterProduct() {
        String formatName = (String) this.bindingContext.getBinding(MASTER_PRODUCT_FORMAT_NAME_PROPERTY).getPropertyValue();
        return ProductIOPlugInManager.getInstance().getReaderPlugIns(formatName).hasNext();
    }

    private JPanel buildOutputMasterProductPanel(int gapBetweenColumns, int gapBetweenRows, Insets defaultTextFieldMargins, Insets defaultListItemMargins,
                                                 int textFieldPreferredHeight, String[] availableFormatNames) {

        ActionListener masterGraphBrowseButtonListener = event -> selectMasterGraphFile();
        JButton masterGraphFileBrowseButton = SwingUtils.buildBrowseButton(masterGraphBrowseButtonListener, textFieldPreferredHeight);

        Insets noMargins = new Insets(0, 0, 0, 0);

        this.masterGraphFilePathTextField = new JTextField();
        this.masterGraphFilePathTextField.setMargin(defaultTextFieldMargins);
        this.bindingContext.bind(MASTER_GRAPH_FILE_PATH_PROPERTY, this.masterGraphFilePathTextField);

        this.canSaveTargetProductCheckBox = new JCheckBox("Save as");
        this.canSaveTargetProductCheckBox.setMargin(noMargins);
        this.canSaveTargetProductCheckBox.setFocusable(false);
        Binding canSaveTargetProductBinding = this.bindingContext.bind(CAN_SAVE_TARGET_PRODUCT_PROPERTY, this.canSaveTargetProductCheckBox);

        this.openTargetProductCheckBox = new JCheckBox("Open in application");
        this.openTargetProductCheckBox.setMargin(noMargins);
        this.openTargetProductCheckBox.setFocusable(false);
        Binding openTargetProductBinding = this.bindingContext.bind(OPEN_TARGET_PRODUCT_PROPERTY, this.openTargetProductCheckBox);

        this.masterProductFolderPathTextField = new JTextField();
        this.masterProductFolderPathTextField.setMargin(defaultTextFieldMargins);
        Binding targetProductFolderPathBinding = this.bindingContext.bind(MASTER_PRODUCT_FOLDER_PATH_PROPERTY, this.masterProductFolderPathTextField);

        this.masterProductNameTextField = new JTextField();
        this.masterProductNameTextField.setMargin(defaultTextFieldMargins);
        this.bindingContext.bind(MASTER_PRODUCT_FILE_NAME_PROPERTY, this.masterProductNameTextField);

        this.masterProductFormatNameComboBox = buildProductFormatNamesComboBox(defaultListItemMargins, textFieldPreferredHeight, availableFormatNames);
        Binding formatNameBinding = this.bindingContext.bind(MASTER_PRODUCT_FORMAT_NAME_PROPERTY, this.masterProductFormatNameComboBox);

        ActionListener targetProductBrowseButtonListener = event -> selectTargetProductFolderPath();
        JButton targetProductFolderBrowseButton = SwingUtils.buildBrowseButton(targetProductBrowseButtonListener, textFieldPreferredHeight);

        JLabel targetProductNameLabel = new JLabel("Name");
        JLabel targetProductFolderLabel = new JLabel("Directory");
        JLabel masterGraphFileLabel = new JLabel("Master graph file path");

        this.masterProductEnabledComponents = new ArrayList<>();
        this.masterProductEnabledComponents.add(this.masterProductFormatNameComboBox);
        this.masterProductEnabledComponents.add(masterGraphFileLabel);
        this.masterProductEnabledComponents.add(this.masterGraphFilePathTextField);
        this.masterProductEnabledComponents.add(masterGraphFileBrowseButton);
        this.masterProductEnabledComponents.add(targetProductNameLabel);
        this.masterProductEnabledComponents.add(this.masterProductNameTextField);
        this.masterProductEnabledComponents.add(targetProductFolderLabel);
        this.masterProductEnabledComponents.add(this.masterProductFolderPathTextField);
        this.masterProductEnabledComponents.add(targetProductFolderBrowseButton);
        this.masterProductEnabledComponents.add(this.openTargetProductCheckBox);

        canSaveTargetProductBinding.setPropertyValue(this.canSaveTargetProductCheckBox.isSelected());
        openTargetProductBinding.setPropertyValue(this.openTargetProductCheckBox.isSelected());
        String homeDirPath = org.esa.snap.core.util.SystemUtils.getUserHomeDir().getPath();
        String targetProductFolderPath = this.appContext.getPreferences().getPropertyString(SaveProductAsAction.PREFERENCES_KEY_LAST_PRODUCT_DIR, homeDirPath);
        targetProductFolderPathBinding.setPropertyValue(targetProductFolderPath);
        if (org.esa.snap.core.util.StringUtils.contains(availableFormatNames, ProductIO.DEFAULT_FORMAT_NAME)) {
            formatNameBinding.setPropertyValue(ProductIO.DEFAULT_FORMAT_NAME);
        } else {
            formatNameBinding.setPropertyValue(availableFormatNames[0]);
        }

        refreshTargetProductEnabledComponents();

        JPanel targetProductPanel = new JPanel(new GridBagLayout());
        targetProductPanel.setBorder(new TitledBorder("Output"));

        GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, 0, 0);
        targetProductPanel.add(this.canSaveTargetProductCheckBox, c);
        c = SwingUtils.buildConstraints(1, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
        targetProductPanel.add(this.masterProductFormatNameComboBox, c);

        c = SwingUtils.buildConstraints(0, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
        targetProductPanel.add(masterGraphFileLabel, c);
        c = SwingUtils.buildConstraints(1, 1, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
        targetProductPanel.add(this.masterGraphFilePathTextField, c);
        c = SwingUtils.buildConstraints(2, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
        targetProductPanel.add(masterGraphFileBrowseButton, c);

        c = SwingUtils.buildConstraints(0, 2, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
        targetProductPanel.add(targetProductNameLabel, c);
        c = SwingUtils.buildConstraints(1, 2, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 2, 1, gapBetweenRows, gapBetweenColumns);
        targetProductPanel.add(this.masterProductNameTextField, c);

        c = SwingUtils.buildConstraints(0, 3, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
        targetProductPanel.add(targetProductFolderLabel, c);
        c = SwingUtils.buildConstraints(1, 3, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
        targetProductPanel.add(this.masterProductFolderPathTextField, c);
        c = SwingUtils.buildConstraints(2, 3, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
        targetProductPanel.add(targetProductFolderBrowseButton, c);

        c = SwingUtils.buildConstraints(0, 4, GridBagConstraints.NONE, GridBagConstraints.WEST, 3, 1, gapBetweenRows, 0);
        targetProductPanel.add(this.openTargetProductCheckBox, c);

        return targetProductPanel;
    }

    private void runOperatorAsync(Map<String, Object> parametersMap, boolean openTargetProductInApplication) {
        Path remoteTopologyFilePath = getRemoteTopologyFilePath();
        RemoteTopology remoteTopologyToSave = this.remoteTopologyPanel.buildRemoteTopology();
        LoadingIndicator loadingIndicator = getLoadingIndicator();
        int threadId = getNewCurrentThreadId();
        RemoteExecutionTimerRunnable runnable = new RemoteExecutionTimerRunnable(this.appContext, this, loadingIndicator, threadId,
                                                                                parametersMap, openTargetProductInApplication, this.mountLocalSharedFolderResult,
                                                                                remoteTopologyToSave, remoteTopologyFilePath) {
            @Override
            protected void onSuccessfullyUnmountLocalFolder() {
                RemoteExecutionDialog.this.mountLocalSharedFolderResult = null;
            }
        };
        runnable.executeAsync();
    }

    private void onFinishReadingRemoteTopoloy(RemoteTopology remoteTopology) {
        if (remoteTopology != null) {
            try {
                Property property = this.bindingContext.getPropertySet().getProperty(REMOTE_SHARED_FOLDER_PATH_PROPERTY);
                property.setValue(this.remoteTopologyPanel.normalizePath(remoteTopology.getRemoteSharedFolderURL()));

                property = this.bindingContext.getPropertySet().getProperty(REMOTE_SHARED_FOLDER_USERNAME_PROPERTY);
                property.setValue(remoteTopology.getRemoteUsername());

                property = this.bindingContext.getPropertySet().getProperty(REMOTE_SHARED_FOLDER_PASSWORD_PROPERTY);
                property.setValue(remoteTopology.getRemotePassword());

                if (remoteTopology.getLocalSharedFolderPath() != null) {
                    property = this.bindingContext.getPropertySet().getProperty(LOCAL_SHARED_FOLDER_PATH_PROPERTY);
                    property.setValue(this.remoteTopologyPanel.normalizePath(remoteTopology.getLocalSharedFolderPath()));
                }

                if (remoteTopology.getLocalPassword() != null) {
                    property = this.bindingContext.getPropertySet().getProperty(LOCAL_PASSWORD_PROPERTY);
                    property.setValue(remoteTopology.getLocalPassword());
                }

                RemoteMachineProperties[] remoteMachines = new RemoteMachineProperties[remoteTopology.getRemoteMachines().size()];
                remoteTopology.getRemoteMachines().toArray(remoteMachines);
                addListItems(remoteMachines, RemoteMachineProperties.class, REMOTE_MACHINES_PROPERTY);
            } catch (ValidationException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private void removeSelectedSourceProducts() {
        int[] selectedIndices = this.sourceProductsList.getSelectedIndices();
        removeListItems(selectedIndices, String.class, SOURCE_PRODUCT_FILES_PROPERTY);
    }

    private void refreshTargetProductEnabledComponents() {
        boolean enabled = canSaveTargetProductToFile();
        for (JComponent masterProductEnabledComponent : this.masterProductEnabledComponents) {
            masterProductEnabledComponent.setEnabled(enabled);
        }
        refreshOpenTargetProductEnabledState();
    }

    private void showDialogToSelectSourceProductsNew() {
        CustomFileChooser fileChooser = CustomFileChooser.buildFileChooser("Select source products", false, JFileChooser.FILES_ONLY);
        Property property = this.bindingContext.getPropertySet().getProperty(LOCAL_SHARED_FOLDER_PATH_PROPERTY);
        String sharedFolder = property.getValue();
        if (StringUtils.isBlank(sharedFolder)) {
            property = this.bindingContext.getPropertySet().getProperty(REMOTE_SHARED_FOLDER_PATH_PROPERTY);
            sharedFolder = property.getValue();
        }
        Path currentDirectoryPath;
        if (StringUtils.isBlank(sharedFolder)) {
            currentDirectoryPath = this.lastSelectedFolderPath;
        } else {
            currentDirectoryPath = Paths.get(sharedFolder);
        }
        if (currentDirectoryPath != null) {
            fileChooser.setCurrentDirectoryPath(currentDirectoryPath);
        }

        int result = fileChooser.showDialog(getJDialog(), "Select");
        if (result == JFileChooser.APPROVE_OPTION) {
            Path selectedFilePath = fileChooser.getSelectedPath();
            this.lastSelectedFolderPath = selectedFilePath.getParent();
            String[] selectedFiles = new String[] { selectedFilePath.toString() };
            addListItems(selectedFiles, String.class, SOURCE_PRODUCT_FILES_PROPERTY);
        }
    }

    private void selectMasterGraphFile() {
        showDialogToSelectGraphFile("Select the master graph file", MASTER_GRAPH_FILE_PATH_PROPERTY, this.masterGraphFilePathTextField.getText());
    }

    private void selectSlaveGraphFile() {
        showDialogToSelectGraphFile("Select the slave graph file", SLAVE_GRAPH_FILE_PATH_PROPERTY, this.slaveGraphFilePathTextField.getText());
    }

    private void showDialogToSelectGraphFile(String dialogTitle, String bidingPropertyName, String graphFilePath) {
        CustomFileChooser fileChooser = CustomFileChooser.buildFileChooser(dialogTitle, false, JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter(CustomFileChooser.buildFileFilter(".xml", "*.xml"));
        fileChooser.setAcceptAllFileFilterUsed(false);
        Path graphPath;
        Path currentDirectoryPath;
        if (StringUtils.isBlank(graphFilePath)) {
            graphPath = null;
            currentDirectoryPath = this.lastSelectedFolderPath;
        } else {
            graphPath = Paths.get(graphFilePath);
            currentDirectoryPath = graphPath.getParent();
        }
        if (currentDirectoryPath != null) {
            fileChooser.setCurrentDirectoryPath(currentDirectoryPath);
            if (graphPath != null && Files.exists(graphPath)) {
                fileChooser.setSelectedPath(graphPath);
            }
        }

        int result = fileChooser.showDialog(getJDialog(), "Select");
        if (result == JFileChooser.APPROVE_OPTION) {
            Path selectedFilePath = fileChooser.getSelectedPath();
            this.lastSelectedFolderPath = selectedFilePath.getParent();
            setPropertyValue(selectedFilePath.toString(), bidingPropertyName);
        }
    }

    private void setPropertyValue(String propertyValue, String bidingPropertyName) {
        try {
            Property property = this.bindingContext.getPropertySet().getProperty(bidingPropertyName);
            property.setValue(propertyValue);
        } catch (ValidationException e) {
            throw new IllegalStateException(e);
        }
    }

    private void selectTargetProductFolderPath() {
        showDialogToSelectFolder("Select product folder", MASTER_PRODUCT_FOLDER_PATH_PROPERTY, this.masterProductFolderPathTextField.getText());
    }

    private void selectLocalSharedFolderPath() {
        showDialogToSelectFolder("Select local shared folder", LOCAL_SHARED_FOLDER_PATH_PROPERTY, this.remoteTopologyPanel.getLocalSharedFolderPathTextField().getText());
    }

    private void showDialogToSelectFolder(String dialogTitle, String bidingPropertyName, String existingFolderPath) {
        CustomFileChooser fileChooser = CustomFileChooser.buildFileChooser(dialogTitle, false, JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        Path currentDirectoryPath;
        if (StringUtils.isBlank(existingFolderPath)) {
            currentDirectoryPath = this.lastSelectedFolderPath;
        } else {
            currentDirectoryPath = Paths.get(existingFolderPath);
        }
        if (currentDirectoryPath != null) {
            fileChooser.setCurrentDirectoryPath(currentDirectoryPath);
        }

        int result = fileChooser.showDialog(getJDialog(), "Select");
        if (result == JFileChooser.APPROVE_OPTION) {
            this.lastSelectedFolderPath = fileChooser.getSelectedPath();
            setPropertyValue(this.lastSelectedFolderPath.toString(), bidingPropertyName);
        }
    }

    private void runButtonPressed() {
        Map<String, Object> parameterMap = this.parameterSupport.getParameterMap();

        String masterSharedFolderPath = (String) parameterMap.get(REMOTE_SHARED_FOLDER_PATH_PROPERTY);
        String masterSharedFolderUsername = (String) parameterMap.get(REMOTE_SHARED_FOLDER_USERNAME_PROPERTY);
        String masterSharedFolderPassword = (String) parameterMap.get(REMOTE_SHARED_FOLDER_PASSWORD_PROPERTY);
        String localSharedFolderPath = (String) parameterMap.get(LOCAL_SHARED_FOLDER_PATH_PROPERTY);
        String localPassword = (String) parameterMap.get(LOCAL_PASSWORD_PROPERTY);
        String slaveGraphFilePath = (String) parameterMap.get(SLAVE_GRAPH_FILE_PATH_PROPERTY);
        String slaveProductsFormatName = (String) parameterMap.get(SLAVE_PRODUCTS_FORMAT_NAME_PROPERTY);
        String masterGraphFilePath = (String) parameterMap.get(MASTER_GRAPH_FILE_PATH_PROPERTY);
        String masterProductFolderPath = (String) parameterMap.get(MASTER_PRODUCT_FOLDER_PATH_PROPERTY);
        String masterProductFileName = (String) parameterMap.get(MASTER_PRODUCT_FILE_NAME_PROPERTY);
        String masterProductFormatName = (String) parameterMap.get(MASTER_PRODUCT_FORMAT_NAME_PROPERTY);
        Boolean continueOnFailure = (Boolean) parameterMap.get(CONTINUE_ON_FAILURE_NAME_PROPERTY);
        Boolean canSaveTargetProduct = (Boolean) parameterMap.get(CAN_SAVE_TARGET_PRODUCT_PROPERTY);
        String[] selectedSourceProducts = (String[]) parameterMap.get(SOURCE_PRODUCT_FILES_PROPERTY);
        RemoteMachineProperties[] selectedRemoteMachines = (RemoteMachineProperties[]) parameterMap.get(REMOTE_MACHINES_PROPERTY);

        if (StringUtils.isBlank(masterSharedFolderPath)) {
            showErrorDialog("Enter the remote shared folder path.");
            this.remoteTopologyPanel.getRemoteSharedFolderPathTextField().requestFocus();
        } else {
            if (StringUtils.isBlank(masterSharedFolderUsername)) {
                showErrorDialog("Enter the username of the machine containing the remote shared folder path.");
                this.remoteTopologyPanel.getRemoteUsernameTextField().requestFocus();
            } else if (StringUtils.isBlank(masterSharedFolderPassword)) {
                showErrorDialog("Enter the password of the machine containing the remote shared folder path.");
                this.remoteTopologyPanel.getRemotePasswordTextField().requestFocus();
            } else {
                Property property = this.bindingContext.getPropertySet().getProperty(REMOTE_MACHINES_PROPERTY);
                ValueSet valueSet = property.getDescriptor().getValueSet();
                if (valueSet == null) {
                    // no remote machines available to run the slave graph
                    showErrorDialog("Add at least one remote machine to process the source products.");
                    this.remoteTopologyPanel.getRemoteMachinesList().requestFocus();
                } else if (selectedRemoteMachines == null || selectedRemoteMachines.length == 0) {
                    // no remote machines selected to run the slave graph
                    showErrorDialog("Select the remote machines to process the source products.");
                    this.remoteTopologyPanel.getRemoteMachinesList().requestFocus();
                } else if (StringUtils.isBlank(slaveGraphFilePath)) {
                    showErrorDialog("Enter the slave graph file to be processed on the remote machines.");
                    this.slaveGraphFilePathTextField.requestFocus();
                } else {
                    property = this.bindingContext.getPropertySet().getProperty(SOURCE_PRODUCT_FILES_PROPERTY);
                    valueSet = property.getDescriptor().getValueSet();
                    if (valueSet == null) {
                        showErrorDialog("Add at least one source product.");
                        this.sourceProductsList.requestFocus();
                    } else if (selectedSourceProducts == null || selectedSourceProducts.length == 0) {
                        showErrorDialog("Select the source products to be processed on the remote machines.");
                        this.sourceProductsList.requestFocus();
                    } else {
                        boolean canExecuteOperator = false;
                        if (canSaveTargetProduct) {
                            if (StringUtils.isBlank(masterProductFormatName)) {
                                showErrorDialog("Select the target product format name.");
                                this.masterProductFormatNameComboBox.requestFocus();
                            } else if (StringUtils.isBlank(masterGraphFilePath)) {
                                showErrorDialog("Enter the master graph file to be processed.");
                                this.masterGraphFilePathTextField.requestFocus();
                            } else if (StringUtils.isBlank(masterProductFileName)) {
                                showErrorDialog("Enter the target product name.");
                                this.masterProductNameTextField.requestFocus();
                            } else if (StringUtils.isBlank(masterProductFolderPath)) {
                                showErrorDialog("Enter the target product folder path.");
                                this.masterProductFolderPathTextField.requestFocus();
                            } else {
                                canExecuteOperator = true;
                            }
                        } else {
                            canExecuteOperator = true;
                        }
                        if (canExecuteOperator) {
                            Map<String, Object> operatatorParameters = new HashMap<>();
                            operatatorParameters.put(REMOTE_SHARED_FOLDER_PATH_PROPERTY, masterSharedFolderPath);
                            operatatorParameters.put(REMOTE_SHARED_FOLDER_USERNAME_PROPERTY, masterSharedFolderUsername);
                            operatatorParameters.put(REMOTE_SHARED_FOLDER_PASSWORD_PROPERTY, masterSharedFolderPassword);
                            operatatorParameters.put(LOCAL_SHARED_FOLDER_PATH_PROPERTY, localSharedFolderPath);
                            operatatorParameters.put(LOCAL_PASSWORD_PROPERTY, localPassword);
                            operatatorParameters.put(SLAVE_GRAPH_FILE_PATH_PROPERTY, slaveGraphFilePath);
                            operatatorParameters.put(SLAVE_PRODUCTS_FORMAT_NAME_PROPERTY, slaveProductsFormatName);
                            operatatorParameters.put(SOURCE_PRODUCT_FILES_PROPERTY, selectedSourceProducts);
                            operatatorParameters.put(REMOTE_MACHINES_PROPERTY, selectedRemoteMachines);
                            operatatorParameters.put(CONTINUE_ON_FAILURE_NAME_PROPERTY, continueOnFailure);

                            boolean openTargetProductInApplication = (Boolean) parameterMap.get(OPEN_TARGET_PRODUCT_PROPERTY);
                            if (canSaveTargetProduct) {
                                File targetProductFile = buildTargetProductFile(masterProductFormatName, masterProductFolderPath, masterProductFileName);
                                operatatorParameters.put(MASTER_GRAPH_FILE_PATH_PROPERTY, masterGraphFilePath);
                                operatatorParameters.put(MASTER_PRODUCT_FILE_PATH_PROPERTY, targetProductFile.getAbsolutePath());
                                operatatorParameters.put(MASTER_PRODUCT_FORMAT_NAME_PROPERTY, masterProductFormatName);

                                // save the target product folder path into the preferences
                                this.appContext.getPreferences().setPropertyString(SaveProductAsAction.PREFERENCES_KEY_LAST_PRODUCT_DIR, masterProductFolderPath);
                            } else {
                                openTargetProductInApplication = false;
                            }
                            runOperatorAsync(operatatorParameters, openTargetProductInApplication);
                        }
                    }
                }
            }
        }
    }

    private <ItemType> void addListItems(ItemType[] itemsToAdd, Class<? extends ItemType> arrayType, String propertyName) {
        Property property = this.bindingContext.getPropertySet().getProperty(propertyName);
        ValueSet valueSet = property.getDescriptor().getValueSet();
        Object arrayItems;
        int offset;
        if (valueSet == null) {
            arrayItems = Array.newInstance(arrayType, itemsToAdd.length);
            offset = 0;
        } else {
            Object[] existingItems = valueSet.getItems();
            offset = existingItems.length;
            arrayItems = Array.newInstance(arrayType, offset + itemsToAdd.length);
            System.arraycopy(existingItems, 0, arrayItems, 0, offset);
        }
        System.arraycopy(itemsToAdd, 0, arrayItems, offset, itemsToAdd.length);
        property.getDescriptor().setValueSet(new ValueSet((Object[]) arrayItems));
    }

    private <ItemType> void setListItems(ItemType[] items, String propertyName) {
        Property property = this.bindingContext.getPropertySet().getProperty(propertyName);
        property.getDescriptor().setValueSet(new ValueSet(items));
    }

    private void showDialogToSelectSourceProducts() {
        if (this.mountLocalSharedFolderResult == null) {
            // mount the local folder and then select the source product
            LoadingIndicator loadingIndicator = getLoadingIndicator();
            int threadId = getNewCurrentThreadId();
            IMountLocalSharedFolderCallback callback = result -> {
                RemoteExecutionDialog.this.mountLocalSharedFolderResult = result;
                showDialogToSelectSourceProductsNew();
            };
            this.remoteTopologyPanel.mountLocalSharedFolderAsync(this, loadingIndicator, threadId, callback);
        } else if (this.remoteTopologyPanel.hasChangedParameters(this.mountLocalSharedFolderResult.getLocalSharedDrive())) {
            LoadingIndicator loadingIndicator = getLoadingIndicator();
            int threadId = getNewCurrentThreadId();
            IUnmountLocalSharedFolderCallback callback = exception -> {
                RemoteExecutionDialog.this.mountLocalSharedFolderResult = null;
                RemoteExecutionDialog.this.showDialogToSelectSourceProducts(); // mount again the local shared folder
            };
            this.mountLocalSharedFolderResult.unmountLocalSharedFolderAsync(loadingIndicator, threadId, callback);
        } else {
            showDialogToSelectSourceProductsNew();
        }
    }

    private <ItemType> void removeListItems(int[] itemIndicesToRemove, Class<? extends ItemType> arrayType, String propertyName) {
        if (itemIndicesToRemove.length > 0) {
            Property property = this.bindingContext.getPropertySet().getProperty(propertyName);
            ValueSet valueSet = property.getDescriptor().getValueSet();
            if (valueSet == null) {
                throw new NullPointerException("The valueSet is null");
            } else {
                Object[] existingItems = valueSet.getItems();
                int index = 0;
                Object items = Array.newInstance(arrayType, existingItems.length - itemIndicesToRemove.length);
                for (int i = 0; i < existingItems.length; i++) {
                    boolean foundIndex = false;
                    for (int k = 0; k < itemIndicesToRemove.length && !foundIndex; k++) {
                        if (i == itemIndicesToRemove[k]) {
                            foundIndex = true;
                            break;
                        }
                    }
                    if (!foundIndex) {
                        Array.set(items, index++, existingItems[i]);
                    }
                }
                if (index == Array.getLength(items)) {
                    property.getDescriptor().setValueSet(new ValueSet((Object[]) items));
                } else {
                    throw new IllegalStateException("The remaining item count is different.");
                }
            }
        }
    }

    private static File buildTargetProductFile(String targetProductFormatName, String targetProductFolderPath, String targetProductFileName) {
        TargetProductSelectorModel targetProductSelectorModel = new TargetProductSelectorModel();
        targetProductSelectorModel.setFormatName(targetProductFormatName);
        targetProductSelectorModel.setProductDir(new File(targetProductFolderPath));
        targetProductSelectorModel.setProductName(targetProductFileName);
        return targetProductSelectorModel.getProductFile();
    }

    private static Path getRemoteTopologyFilePath() {
        Path cepFolderPath = org.esa.snap.core.util.SystemUtils.getApplicationDataDir().toPath().resolve("cloud-exploitation-platform");
        return cepFolderPath.resolve("remote-topology.json");
    }

    public static class CloudExploitationPlatformItem extends Operator {

        private String remoteSharedFolderPath;
        private String remoteSharedFolderUsername;
        private String remoteSharedFolderPassword;
        private String localSharedFolderPath;
        private String localPassword;
        private RemoteMachineProperties[] remoteMachines;
        private String[] sourceProductFiles;
        private String slaveGraphFilePath;
        private String slaveProductsFormatName;
        private String masterGraphFilePath;
        private String masterProductFolderPath;
        private String masterProductFileName;
        private String masterProductFormatName;
        private Boolean continueOnFailure;
        private Boolean canSaveTargetProduct;
        private Boolean openTargetProduct;

        public CloudExploitationPlatformItem() {
        }

        @Override
        public void initialize() throws OperatorException {
            // do nothing
        }
    }

    private static class EmptyValueSetExtended extends ValueSet {

        public EmptyValueSetExtended(Object[] items) {
            super(items);
        }

        @Override
        public boolean contains(Object value) {
            return true;//super.contains(value);
        }
    }
}
