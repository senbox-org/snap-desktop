package org.esa.snap.product.library.ui.v2.repository;

import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.product.library.ui.v2.MissionParameterListener;
import org.esa.snap.product.library.ui.v2.repository.local.AllLocalProductsRepositoryPanel;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteProductsRepositoryPanel;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelperImpl;
import org.esa.snap.product.library.ui.v2.worldwind.WorldWindowPanelWrapper;
import org.esa.snap.remote.products.repository.RemoteProductsRepositoryProvider;
import org.esa.snap.ui.loading.LabelListCellRenderer;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.ComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Stack;

/**
 * Created by jcoravu on 22/8/2019.
 */
public class RepositorySelectionPanel extends JPanel {

    private JComboBox<AbstractProductsRepositoryPanel> repositoriesComboBox;

    private final JButton searchButton;
    private final JButton helpButton;
    private final JLabel repositoryLabel;
    private final ProgressBarHelperImpl progressBarHelper;
    private final int gapBetweenColumns;

    private JButton repositoryTopBarButton;
    private ItemListener repositoriesItemListener;

    public RepositorySelectionPanel(RemoteProductsRepositoryProvider[] productsRepositoryProviders, ComponentDimension componentDimension,
                                    MissionParameterListener missionParameterListener, WorldWindowPanelWrapper worldWindowPanel) {

        super(new GridBagLayout());

        this.gapBetweenColumns = componentDimension.getGapBetweenColumns();

        createRepositoriesComboBox(productsRepositoryProviders, componentDimension, missionParameterListener, worldWindowPanel);

        Dimension comboBoxSize = this.repositoriesComboBox.getPreferredSize();

        Dimension buttonSize = new Dimension(comboBoxSize.height, comboBoxSize.height);

        this.searchButton = buildButton("/org/esa/snap/productlibrary/icons/search24.png", null, buttonSize, 1);
        this.searchButton.setToolTipText("Search");

        ActionListener helpButtonListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        };
        this.helpButton = buildButton("/org/esa/snap/resources/images/icons/Help24.gif", helpButtonListener, buttonSize, 1);
        this.helpButton.setToolTipText("Help");

        this.progressBarHelper = new ProgressBarHelperImpl(100, buttonSize.height) {
            @Override
            protected void setParametersEnabledWhileDownloading(boolean enabled) {
                RepositorySelectionPanel.this.setParametersEnabledWhileDownloading(enabled);
            }
        };

        this.repositoryLabel = new JLabel("Repository");

        addComponents();
    }

    public ProgressBarHelperImpl getProgressBarHelper() {
        return progressBarHelper;
    }

    public void setSearchButtonListener(ActionListener searchButtonListener) {
        this.searchButton.addActionListener(searchButtonListener);
    }

    public void setStopButtonListener(ActionListener stopButtonListener) {
        this.progressBarHelper.getStopButton().addActionListener(stopButtonListener);
    }

    public AbstractProductsRepositoryPanel getSelectedRepository() {
        return (AbstractProductsRepositoryPanel)this.repositoriesComboBox.getSelectedItem();
    }

    public void refreshRepositoryParameterComponents() {
        getSelectedRepository().refreshParameterComponents();
        refreshRepositoryLabelWidth();
    }

    public void setDataSourcesBorder(Border border) {
        int count = this.repositoriesComboBox.getModel().getSize();
        for (int i=0; i<count; i++) {
            AbstractProductsRepositoryPanel repositoryPanel = this.repositoriesComboBox.getModel().getElementAt(i);
            repositoryPanel.setBorder(border);
        }
    }

    public AllLocalProductsRepositoryPanel getAllLocalProductsRepositoryPanel() {
        int count = this.repositoriesComboBox.getModel().getSize();
        for (int i=0; i<count; i++) {
            AbstractProductsRepositoryPanel repositoryPanel = this.repositoriesComboBox.getModel().getElementAt(i);
            if (repositoryPanel instanceof AllLocalProductsRepositoryPanel) {
                return (AllLocalProductsRepositoryPanel)repositoryPanel;
            }
        }
        throw new IllegalStateException("The all local products repository does not exist.");
    }

    private void setParametersEnabledWhileDownloading(boolean enabled) {
        this.searchButton.setEnabled(enabled);
        this.repositoryLabel.setEnabled(enabled);
        this.repositoriesComboBox.setEnabled(enabled);
        AbstractProductsRepositoryPanel selectedDataSource = getSelectedRepository();
        Stack<JComponent> stack = new Stack<JComponent>();
        stack.push(selectedDataSource);
        while (!stack.isEmpty()) {
            JComponent component = stack.pop();
            component.setEnabled(enabled);
            int childrenCount = component.getComponentCount();
            for (int i=0; i<childrenCount; i++) {
                Component child = component.getComponent(i);
                if (child instanceof JComponent) {
                    JComponent childComponent = (JComponent) child;
                    // add the component in the stack to be enabled/disabled
                    stack.push(childComponent);
                }
            }
        }
    }

    private void refreshRepositoryLabelWidth() {
        int maximumLabelWidth = getSelectedRepository().computeLeftPanelMaximumLabelWidth();
        RemoteProductsRepositoryPanel.setLabelSize(this.repositoryLabel, maximumLabelWidth);
        Container parentContainer = this.repositoryLabel.getParent();
        if (parentContainer != null) {
            parentContainer.revalidate();
            parentContainer.repaint();
        }
    }

    public void setRepositoriesItemListener(ItemListener repositoriesItemListener) {
        this.repositoriesItemListener = repositoriesItemListener;
    }

    private void createRepositoriesComboBox(RemoteProductsRepositoryProvider[] productsRepositoryProviders, ComponentDimension componentDimension,
                                            MissionParameterListener missionParameterListener, WorldWindowPanelWrapper worldWindowPanel) {

        AbstractProductsRepositoryPanel[] availableDataSources = new AbstractProductsRepositoryPanel[productsRepositoryProviders.length + 1];
        for (int i=0; i<productsRepositoryProviders.length; i++) {
            availableDataSources[i] = new RemoteProductsRepositoryPanel(productsRepositoryProviders[i], componentDimension, missionParameterListener, worldWindowPanel);
        }
        availableDataSources[productsRepositoryProviders.length] = new AllLocalProductsRepositoryPanel(componentDimension, worldWindowPanel);

        this.repositoriesComboBox = RemoteProductsRepositoryPanel.buildComboBox(componentDimension);
        for (int i = 0; i < availableDataSources.length; i++) {
            this.repositoriesComboBox.addItem(availableDataSources[i]);
        }
        int cellItemHeight = this.repositoriesComboBox.getPreferredSize().height;
        LabelListCellRenderer<AbstractProductsRepositoryPanel> renderer = new LabelListCellRenderer<AbstractProductsRepositoryPanel>(cellItemHeight) {
            @Override
            protected String getItemDisplayText(AbstractProductsRepositoryPanel value) {
                return (value == null) ? "" : value.getName();
            }
        };
        this.repositoriesComboBox.setRenderer(renderer);
        this.repositoriesComboBox.setMaximumRowCount(5);
        this.repositoriesComboBox.setSelectedIndex(0);
        this.repositoriesComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                    newSelectedRepository();
                }
                if (repositoriesItemListener != null) {
                    repositoriesItemListener.itemStateChanged(itemEvent);
                }
            }
        });
    }

    public void setOpenAndDeleteLocalProductListeners(ActionListener openLocalProductListener, ActionListener deleteLocalProductListener) {
        ComboBoxModel<AbstractProductsRepositoryPanel> model = this.repositoriesComboBox.getModel();
        for (int i=0; i<model.getSize(); i++) {
            AbstractProductsRepositoryPanel repositoryPanel = model.getElementAt(i);
            if (repositoryPanel instanceof AllLocalProductsRepositoryPanel) {
                ((AllLocalProductsRepositoryPanel)repositoryPanel).setOpenAndDeleteProductListeners(openLocalProductListener, deleteLocalProductListener);
            }
        }
    }

    public void setDownloadRemoteProductListener(ActionListener downloadRemoteProductListener) {
        ComboBoxModel<AbstractProductsRepositoryPanel> model = this.repositoriesComboBox.getModel();
        for (int i=0; i<model.getSize(); i++) {
            AbstractProductsRepositoryPanel repositoryPanel = model.getElementAt(i);
            if (repositoryPanel instanceof RemoteProductsRepositoryPanel) {
                ((RemoteProductsRepositoryPanel)repositoryPanel).setDownloadProductListener(downloadRemoteProductListener);
            }
        }
    }

    private void newSelectedRepository() {
        if (this.repositoryTopBarButton != null) {
            remove(this.repositoryTopBarButton);
            revalidate();
            repaint();
        }
        this.repositoryTopBarButton = getSelectedRepository().getTopBarButton();
        if (this.repositoryTopBarButton != null) {
            GridBagConstraints c = SwingUtils.buildConstraints(3, 0, GridBagConstraints.VERTICAL, GridBagConstraints.WEST, 1, 1, 0, this.gapBetweenColumns);
            add(this.repositoryTopBarButton, c);
            revalidate();
            repaint();
        }
    }

    private void addComponents() {
        this.repositoryTopBarButton = getSelectedRepository().getTopBarButton();

        GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, 0, 0);
        add(this.repositoryLabel, c);
        c = SwingUtils.buildConstraints(1, 0, GridBagConstraints.BOTH, GridBagConstraints.WEST, 1, 1, 0, this.gapBetweenColumns);
        add(this.repositoriesComboBox, c);
        c = SwingUtils.buildConstraints(2, 0, GridBagConstraints.VERTICAL, GridBagConstraints.WEST, 1, 1, 0, this.gapBetweenColumns);
        add(this.searchButton, c);
        if (this.repositoryTopBarButton != null) {
            c = SwingUtils.buildConstraints(3, 0, GridBagConstraints.VERTICAL, GridBagConstraints.WEST, 1, 1, 0, this.gapBetweenColumns);
            add(this.repositoryTopBarButton, c);
        }
        c = SwingUtils.buildConstraints(4, 0, GridBagConstraints.VERTICAL, GridBagConstraints.WEST, 1, 1, 0, this.gapBetweenColumns);
        add(this.helpButton, c);
        c = SwingUtils.buildConstraints(5, 0, GridBagConstraints.VERTICAL, GridBagConstraints.WEST, 1, 1, 0, this.gapBetweenColumns);
        add(this.progressBarHelper.getProgressBar(), c);
        c = SwingUtils.buildConstraints(6, 0, GridBagConstraints.VERTICAL, GridBagConstraints.WEST, 1, 1, 0, this.gapBetweenColumns);
        add(this.progressBarHelper.getStopButton(), c);
    }

    public static JButton buildButton(String resourceImagePath, ActionListener buttonListener, Dimension buttonSize, Integer scaledImagePadding) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL imageURL = classLoader.getResource(resourceImagePath);
        ImageIcon icon = new ImageIcon(imageURL);
        if (scaledImagePadding != null && scaledImagePadding.intValue() >= 0) {
            Image scaledImage = getScaledImage(icon.getImage(), buttonSize.width, buttonSize.height, scaledImagePadding.intValue());
            icon = new ImageIcon(scaledImage);
        }
        JButton button = new JButton(icon);
        button.setFocusable(false);
        button.addActionListener(buttonListener);
        button.setPreferredSize(buttonSize);
        button.setMinimumSize(buttonSize);
        button.setMaximumSize(buttonSize);
        return button;
    }

    private static Image getScaledImage(Image srcImg, int destinationImageWidth, int destinationImageHeight, int padding) {
        BufferedImage resizedImg = new BufferedImage(destinationImageWidth, destinationImageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resizedImg.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(srcImg, padding, padding, destinationImageWidth-padding, destinationImageHeight-padding, 0, 0, srcImg.getWidth(null), srcImg.getHeight(null), null);
        g2.dispose();
        return resizedImg;
    }
}
