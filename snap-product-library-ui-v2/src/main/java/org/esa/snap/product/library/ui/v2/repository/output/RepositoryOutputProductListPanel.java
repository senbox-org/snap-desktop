package org.esa.snap.product.library.ui.v2.repository.output;

import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.product.library.ui.v2.repository.AbstractProductsRepositoryPanel;
import org.esa.snap.product.library.ui.v2.repository.RepositorySelectionPanel;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelperImpl;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.ui.loading.CustomComboBox;
import org.esa.snap.ui.loading.ItemRenderer;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created by jcoravu on 21/8/2019.
 */
public class RepositoryOutputProductListPanel extends JPanel implements OutputProductResultsCallback {

    private static final String PAGE_PRODUCTS_CHANGED = "pageProductsChanged";

    public static final byte VISIBLE_PRODUCTS_PER_PAGE = 20;

    private final JLabel titleLabel;
    private final JLabel sortByLabel;
    private final ProgressBarHelperImpl progressBarHelper;
    private final OutputProductListPanel productListPanel;
    private final OutputProductListPaginationPanel productListPaginationPanel;
    private final CustomComboBox<ComparatorItem> comparatorsComboBox;

    private int visibleProductsPerPage;

    public RepositoryOutputProductListPanel(RepositorySelectionPanel repositorySelectionPanel, ComponentDimension componentDimension,
                                            ActionListener stopButtonListener, int progressBarWidth, boolean showStopDownloadButton) {

        super(new BorderLayout(0, componentDimension.getGapBetweenRows()));

        this.visibleProductsPerPage = RepositoryOutputProductListPanel.VISIBLE_PRODUCTS_PER_PAGE;

        this.titleLabel = new JLabel(getTitle());

        this.productListPanel = new OutputProductListPanel(repositorySelectionPanel, componentDimension, this);
        this.productListPanel.addDataChangedListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateProductListCountTitle();
                refreshPaginationButtons();
            }
        });

        ItemRenderer<ComparatorItem> itemRenderer = new ItemRenderer<ComparatorItem>() {
            @Override
            public String getItemDisplayText(ComparatorItem item) {
                return (item == null) ? " " : " " + item.getDisplayName();
            }
        };
        this.comparatorsComboBox = new CustomComboBox<>(itemRenderer, componentDimension.getTextFieldPreferredHeight(), false, componentDimension.getTextFieldBackgroundColor());
        this.comparatorsComboBox.addItem(buildProductNameComparator());
        this.comparatorsComboBox.addItem(buildMissionComparator());
        this.comparatorsComboBox.addItem(buildAcquisitionDateComparator());
        this.comparatorsComboBox.addItem(buildFileSizeComparator());
        this.comparatorsComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                    productListPanel.getProductListModel().sortProducts();
                }
            }
        });
        int maximumPreferredWidth = 0;
        JLabel label = new JLabel();
        for (int i=0; i<this.comparatorsComboBox.getItemCount(); i++) {
            label.setText(itemRenderer.getItemDisplayText(this.comparatorsComboBox.getItemAt(i)));
            maximumPreferredWidth = Math.max(maximumPreferredWidth, label.getPreferredSize().width);
        }
        Dimension comboBoxSize = new Dimension(maximumPreferredWidth + componentDimension.getTextFieldPreferredHeight(), componentDimension.getTextFieldPreferredHeight());
        this.comparatorsComboBox.setPreferredSize(comboBoxSize);
        this.comparatorsComboBox.setMaximumSize(comboBoxSize);
        this.comparatorsComboBox.setMinimumSize(comboBoxSize);

        this.sortByLabel = label;
        this.sortByLabel.setText("Sort By");

        ActionListener firstPageButtonListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                displayFirstPageProducts();
            }
        };
        ActionListener previousPageButtonListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                displayPreviousPageProducts();
            }
        };
        ActionListener nextPageButtonListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                displayNextPageProducts();
            }
        };
        ActionListener lastPageButtonListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                displayLastPageProducts();
            }
        };
        this.productListPaginationPanel = new OutputProductListPaginationPanel(componentDimension, firstPageButtonListener, previousPageButtonListener,
                                                                               nextPageButtonListener, lastPageButtonListener);

        this.progressBarHelper = new ProgressBarHelperImpl(progressBarWidth, componentDimension.getTextFieldPreferredHeight()) {
            @Override
            protected void setParametersEnabledWhileDownloading(boolean enabled) {
                // do nothing
            }
        };
        this.progressBarHelper.getStopButton().addActionListener(stopButtonListener);

        addComponents(componentDimension, showStopDownloadButton);
    }

    @Override
    public Comparator<RepositoryProduct> getProductsComparator() {
        return (ComparatorItem)this.comparatorsComboBox.getSelectedItem();
    }

    @Override
    public OutputProductResults getOutputProductResults() {
        AbstractProductsRepositoryPanel selectedProductsRepositoryPanel = this.productListPanel.getRepositorySelectionPanel().getSelectedProductsRepositoryPanel();
        return selectedProductsRepositoryPanel.getOutputProductResults();
    }

    public void setVisibleProductsPerPage(int visibleProductsPerPage) {
        this.visibleProductsPerPage = visibleProductsPerPage;
    }

    public OutputProductListPaginationPanel getProductListPaginationPanel() {
        return this.productListPaginationPanel;
    }

    public ProgressBarHelperImpl getProgressBarHelper() {
        return progressBarHelper;
    }

    public OutputProductListPanel getProductListPanel() {
        return productListPanel;
    }

    public void addPageProductsChangedListener(PropertyChangeListener changeListener) {
        addPropertyChangeListener(PAGE_PRODUCTS_CHANGED, changeListener);
    }

    public void setProducts(List<RepositoryProduct> products) {
        resetOutputProducts();
        if (products.size() > 0) {
            getOutputProductResults().addProducts(products);
            displayPageProducts(1); // display the first page
        } else {
            // no products to display
            clearPageProducts();
        }
    }

    public void clearOutputList(boolean canResetProductListCountTitle) {
        resetOutputProducts();
        clearPageProducts();
        if (canResetProductListCountTitle) {
            resetProductListCountTitle(); // remove the product count from the title
        }
    }

    public void refreshOutputList() {
        OutputProductResults outputProductResults = getOutputProductResults();
        if (outputProductResults.getAvailableProductCount() > 0) {
            displayPageProducts(outputProductResults.getCurrentPageNumber());
        } else {
            clearPageProducts();
        }
    }

    public void addProducts(List<RepositoryProduct> products) {
        if (products.size() > 0) {
            OutputProductResults outputProductResults = getOutputProductResults();
            if (outputProductResults.getAvailableProductCount() > 0) {
                if (outputProductResults.getCurrentPageNumber() <= 0) {
                    throw new IllegalStateException("The current page number " + outputProductResults.getCurrentPageNumber() + " must be > 0.");
                }
            } else if (outputProductResults.getCurrentPageNumber() != 0) {
                throw new IllegalStateException("The current page number " + outputProductResults.getCurrentPageNumber() + " must be 0.");
            }
            outputProductResults.addProducts(products);
            if (outputProductResults.getCurrentPageNumber() == 0) {
                // the first page
                int productCount = this.productListPanel.getProductListModel().getProductCount();
                if (productCount > 0) {
                    throw new IllegalStateException("The product count " + productCount + " of the first page must be 0.");
                }
                displayPageProducts(1); // display the first page
            } else {
                refreshPaginationButtons(); // refresh the pagination button after received the products
            }
        }
    }

    private void displayNextPageProducts() {
        int totalPageCount = computeTotalPageCount();
        OutputProductResults outputProductResults = getOutputProductResults();
        if (outputProductResults.getCurrentPageNumber() < totalPageCount) {
            displayPageProducts(outputProductResults.getCurrentPageNumber() + 1);
        } else {
            throw new IllegalStateException("The current page number " + outputProductResults.getCurrentPageNumber()+" must be < than the total page count " + totalPageCount + ".");
        }
    }

    private void displayLastPageProducts() {
        OutputProductResults outputProductResults = getOutputProductResults();
        int totalPageCount = computeTotalPageCount();
        if (outputProductResults.getCurrentPageNumber() < totalPageCount) {
            displayPageProducts(totalPageCount);
        } else {
            throw new IllegalStateException("The current page number " + outputProductResults.getCurrentPageNumber()+" must be < than the total page count " + totalPageCount + ".");
        }
    }

    private void displayFirstPageProducts() {
        OutputProductResults outputProductResults = getOutputProductResults();
        if (outputProductResults.getCurrentPageNumber() > 1) {
            displayPageProducts(1);
        } else {
            int totalPageCount = computeTotalPageCount();
            throw new IllegalStateException("The current page number " + outputProductResults.getCurrentPageNumber()+" must be < than the total page count " + totalPageCount + ".");
        }
    }

    private void displayPreviousPageProducts() {
        OutputProductResults outputProductResults = getOutputProductResults();
        if (outputProductResults.getCurrentPageNumber() > 1) {
            displayPageProducts(outputProductResults.getCurrentPageNumber() - 1);
        } else {
            int totalPageCount = computeTotalPageCount();
            throw new IllegalStateException("The current page number " + outputProductResults.getCurrentPageNumber()+" must be < than the total page count " + totalPageCount + ".");
        }
    }

    private void displayPageProducts(int newCurrentPageNumber) {
        if (newCurrentPageNumber <= 0) {
            throw new IllegalArgumentException("The new current page number " + newCurrentPageNumber +" must be > 0.");
        }
        OutputProductResults outputProductResults = getOutputProductResults();
        int startIndex = (newCurrentPageNumber-1) * this.visibleProductsPerPage;
        int endIndex = startIndex + this.visibleProductsPerPage - 1;
        if (endIndex >= outputProductResults.getAvailableProductCount()) {
            endIndex = outputProductResults.getAvailableProductCount() - 1;
        }
        List<RepositoryProduct> pageProducts = new ArrayList<>((endIndex - startIndex));
        for (int i = startIndex; i <= endIndex; i++) {
            pageProducts.add(outputProductResults.getProductAt(i));
        }
        outputProductResults.setCurrentPageNumber(newCurrentPageNumber);
        this.productListPanel.setProducts(pageProducts);
        firePropertyChange(PAGE_PRODUCTS_CHANGED, null, null);
    }

    private void clearPageProducts() {
        this.productListPanel.getProductListModel().clear();
        firePropertyChange(PAGE_PRODUCTS_CHANGED, null, null);
    }

    private void refreshPaginationButtons() {
        int totalPageCount = computeTotalPageCount();
        boolean previousPageEnabled = false;
        boolean nextPageEnabled = false;
        String text = "";
        if (totalPageCount > 0) {
            OutputProductResults outputProductResults = getOutputProductResults();
            if (outputProductResults.getCurrentPageNumber() > 0) {
                text = Integer.toString(outputProductResults.getCurrentPageNumber()) + " / " + Integer.toString(totalPageCount);
                if (outputProductResults.getCurrentPageNumber() > 1) {
                    previousPageEnabled = true;
                }
                if (outputProductResults.getCurrentPageNumber() < totalPageCount) {
                    nextPageEnabled = true;
                }
            } else {
                throw new IllegalStateException("The current page number is 0.");
            }
        }
        this.productListPaginationPanel.refreshPaginationButtons(previousPageEnabled, nextPageEnabled, text);
    }

    private int computeTotalPageCount() {
        OutputProductResults outputProductResults = getOutputProductResults();
        int count = 0;
        if (outputProductResults.getAvailableProductCount() > 0) {
            count = outputProductResults.getAvailableProductCount() / this.visibleProductsPerPage;
            if (outputProductResults.getAvailableProductCount() % this.visibleProductsPerPage > 0) {
                count++;
            }
        }
        return count;
    }

    public void resetProductListCountTitle() {
        this.titleLabel.setText(getTitle());
    }

    public void updateProductListCountTitle() {
        int pageProductCount = this.productListPanel.getProductListModel().getProductCount();
        int totalProductCount = getOutputProductResults().getAvailableProductCount();
        String text = getTitle() + ": " + Integer.toString(pageProductCount);
        if (totalProductCount > 0) {
            text += " out of " + Integer.toString(totalProductCount);
        }
        this.titleLabel.setText(text);
    }

    private String getTitle() {
        return "Products";
    }

    private void resetOutputProducts() {
        AbstractProductsRepositoryPanel selectedProductsRepositoryPanel = this.productListPanel.getRepositorySelectionPanel().getSelectedProductsRepositoryPanel();
        selectedProductsRepositoryPanel.resetOutputProducts();
    }

    private void addComponents(ComponentDimension componentDimension, boolean showStopDownloadButton) {
        int gapBetweenRows = 0;
        int gapBetweenColumns = componentDimension.getGapBetweenColumns();

        JPanel northPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.BOTH, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
        northPanel.add(this.titleLabel, c);
        c = SwingUtils.buildConstraints(1, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
        northPanel.add(this.sortByLabel, c);
        c = SwingUtils.buildConstraints(2, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
        northPanel.add(this.comparatorsComboBox, c);
        c = SwingUtils.buildConstraints(3, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
        northPanel.add(this.progressBarHelper.getProgressBar(), c);
        if (showStopDownloadButton) {
            c = SwingUtils.buildConstraints(4, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
            northPanel.add(this.progressBarHelper.getStopButton(), c);
        }

        JScrollPane scrollPane = new JScrollPane(this.productListPanel);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(this.productListPanel.getBackground());
        scrollPane.setBorder(SwingUtils.LINE_BORDER);

        add(northPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private static ComparatorItem buildProductNameComparator() {
        return new ComparatorItem("Product Name") {
            @Override
            public int compare(RepositoryProduct o1, RepositoryProduct o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        };
    }

    private static ComparatorItem buildAcquisitionDateComparator() {
        return new ComparatorItem("Acquisition Date") {
            @Override
            public int compare(RepositoryProduct o1, RepositoryProduct o2) {
                Date acquisitionDate1 = o1.getAcquisitionDate();
                Date acquisitionDate2 = o2.getAcquisitionDate();
                if (acquisitionDate1 == null && acquisitionDate2 == null) {
                    return 0; // both acquisition dates are null
                }
                if (acquisitionDate1 == null && acquisitionDate2 != null) {
                    return -1; // the first acquisition date is null
                }
                if (acquisitionDate1 != null && acquisitionDate2 == null) {
                    return 1; // the second acquisition date is null
                }
                return acquisitionDate1.compareTo(acquisitionDate2);
            }
        };
    }

    private static ComparatorItem buildMissionComparator() {
        return new ComparatorItem("Mission") {
            @Override
            public int compare(RepositoryProduct leftProduct, RepositoryProduct rigtProduct) {
                String leftMissionName = (leftProduct.getRemoteMission() == null) ? "" : leftProduct.getRemoteMission().getName();
                String rightMissionName = (rigtProduct.getRemoteMission() == null) ? "" : rigtProduct.getRemoteMission().getName();
                return leftMissionName.compareToIgnoreCase(rightMissionName);
            }
        };
    }

    private static ComparatorItem buildFileSizeComparator() {
        return new ComparatorItem("File size") {
            @Override
            public int compare(RepositoryProduct o1, RepositoryProduct o2) {
                long fileSize1 = o1.getApproximateSize();
                long fileSize2 = o2.getApproximateSize();
                if (fileSize1 == fileSize2) {
                    return 0;
                }
                if (fileSize1 < fileSize2) {
                    return -1;
                }
                return 1;
            }
        };
    }

    private static abstract class ComparatorItem implements Comparator<RepositoryProduct> {

        private final String displayName;

        private ComparatorItem(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
