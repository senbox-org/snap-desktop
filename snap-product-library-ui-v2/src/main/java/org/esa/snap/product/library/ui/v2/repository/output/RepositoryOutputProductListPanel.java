package org.esa.snap.product.library.ui.v2.repository.output;

import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.product.library.ui.v2.repository.AbstractProductsRepositoryPanel;
import org.esa.snap.product.library.ui.v2.repository.RepositorySelectionPanel;
import org.esa.snap.product.library.ui.v2.repository.remote.download.DownloadProductListTimerRunnable;
import org.esa.snap.product.library.ui.v2.repository.timeline.RepositoryProductsTimelinePanel;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelperImpl;
import org.esa.snap.product.library.v2.preferences.RepositoriesCredentialsController;
import org.esa.snap.product.library.v2.preferences.RepositoriesCredentialsPersistence;
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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The panel class contains the product list after searching then in the repository.
 *
 * Created by jcoravu on 21/8/2019.
 */
public class RepositoryOutputProductListPanel extends JPanel implements OutputProductResultsCallback {

    private static final String PAGE_PRODUCTS_CHANGED = "pageProductsChanged";

    public static final byte ASCENDING_SORTING_TYPE = 1;
    public static final byte DESCENDING_SORTING_TYPE = 2;

    private final JLabel titleLabel;
    private final JLabel sortByLabel;
    private final ProgressBarHelperImpl progressBarHelper;
    private final OutputProductListPanel productListPanel;
    private final OutputProductListPaginationPanel productListPaginationPanel;
    private final CustomComboBox<ComparatorItem> comparatorsComboBox;
    private final CustomComboBox<Byte> sortingTypeComboBox;

    private RepositoryProductsTimelinePanel productsTimelinePanel;
    private int visibleProductsPerPage;
    private DownloadProductListTimerRunnable downloadProductListTimerRunnable = null;

    public RepositoryOutputProductListPanel(RepositorySelectionPanel repositorySelectionPanel, ComponentDimension componentDimension,
                                            ActionListener stopButtonListener, int progressBarWidth, boolean showStopDownloadButton) {

        super(new BorderLayout(0, componentDimension.getGapBetweenRows()));

        this.visibleProductsPerPage = RepositoriesCredentialsPersistence.VISIBLE_PRODUCTS_PER_PAGE;

        this.titleLabel = new JLabel(getTitle());

        this.productListPanel = new OutputProductListPanel(repositorySelectionPanel, componentDimension, this);
        this.productListPanel.addDataChangedListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateProductListCountTitle();
                refreshPaginationButtons();
            }
        });

        this.productsTimelinePanel = new RepositoryProductsTimelinePanel();
        this.productsTimelinePanel.setItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent event) {
                if (event.getStateChange() == ItemEvent.SELECTED) {
                    productsTimelinePanel.refresh(getOutputProductResults());
                }
            }
        });

        ItemListener sortProductsListener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                    productListPanel.getProductListModel().sortProducts();
                }
            }
        };

        ItemRenderer<ComparatorItem> itemRenderer = new ItemRenderer<ComparatorItem>() {
            @Override
            public String getItemDisplayText(ComparatorItem item) {
                return (item == null) ? " " : item.getDisplayName();
            }
        };
        this.comparatorsComboBox = new CustomComboBox<>(itemRenderer, componentDimension.getTextFieldPreferredHeight(), false, componentDimension.getTextFieldBackgroundColor());
        this.comparatorsComboBox.addItem(buildProductNameComparator());
        this.comparatorsComboBox.addItem(buildMissionComparator());
        this.comparatorsComboBox.addItem(buildAcquisitionDateComparator());
        this.comparatorsComboBox.addItem(buildFileSizeComparator());
        this.comparatorsComboBox.addItemListener(sortProductsListener);

        ItemRenderer<Byte> sortingTypeRenderer = new ItemRenderer<Byte>() {
            @Override
            public String getItemDisplayText(Byte item) {
                if (item == null) {
                    return " ";
                }
                if (item.byteValue() == ASCENDING_SORTING_TYPE) {
                    return "Ascending";
                }
                if (item.byteValue() == DESCENDING_SORTING_TYPE) {
                    return "Descending";
                }
                throw new IllegalArgumentException("Unknown sorting type " + item.byteValue()+".");
            }
        };
        this.sortingTypeComboBox = new CustomComboBox<>(sortingTypeRenderer, componentDimension.getTextFieldPreferredHeight(), false, componentDimension.getTextFieldBackgroundColor());
        this.sortingTypeComboBox.addItem(ASCENDING_SORTING_TYPE);
        this.sortingTypeComboBox.addItem(DESCENDING_SORTING_TYPE);
        this.sortingTypeComboBox.addItemListener(sortProductsListener);

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

    public void setDownloadProductListTimerRunnable(DownloadProductListTimerRunnable downloadProductListTimerRunnable) {
        this.downloadProductListTimerRunnable = downloadProductListTimerRunnable;
    }

    public void setCurrentFullResultsListCount(Long currentFullResultsListCount) {
        getOutputProductResults().setFullResultsListCount(currentFullResultsListCount);
    }

    private boolean downloadsAllPages(){
        return RepositoriesCredentialsController.getInstance().downloadsAllPages();
    }

    @Override
    public Comparator<RepositoryProduct> getProductsComparator() {
        ComparatorItem comparator = (ComparatorItem)this.comparatorsComboBox.getSelectedItem();
        Byte sortingType = (Byte)this.sortingTypeComboBox.getSelectedItem();
        boolean sortAscending;
        if (sortingType.byteValue() == ASCENDING_SORTING_TYPE) {
            sortAscending = true;
        } else if (sortingType.byteValue() == DESCENDING_SORTING_TYPE) {
            sortAscending = false;
        } else {
            throw new IllegalStateException("unknown sorting type " + sortingType.byteValue() + ".");
        }
        return new ProductsComparator(comparator, sortAscending);
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
            setCurrentFullResultsListCount((long) products.size());
            displayPageProducts(1); // display the first page
        } else {
            // no products to display
            clearPageProducts();
        }
        this.productsTimelinePanel.refresh(getOutputProductResults());
    }

    public void clearOutputList(boolean canResetProductListCountTitle) {
        resetOutputProducts();
        clearPageProducts();
        if (canResetProductListCountTitle) {
            resetProductListCountTitle(); // remove the product count from the title
        }
        this.productsTimelinePanel.refresh(getOutputProductResults());
    }

    public void refreshOutputList() {
        OutputProductResults outputProductResults = getOutputProductResults();
        if (outputProductResults.getAvailableProductCount() > 0) {
            displayPageProducts(outputProductResults.getCurrentPageNumber());
        } else {
            clearPageProducts();
        }
        this.productsTimelinePanel.refresh(outputProductResults);
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
                if(!downloadsAllPages() && productListPageDownloaded(outputProductResults.getCurrentPageNumber() + 1)){
                    displayPageProducts(outputProductResults.getCurrentPageNumber() + 1);
                }
            }
            this.productsTimelinePanel.refresh(outputProductResults);
        }
    }

    private boolean productListPageDownloaded(int pageNumber){
        return pageNumber <= getOutputProductResults().getAvailableProductCount() / this.visibleProductsPerPage + (getOutputProductResults().getAvailableProductCount() % this.visibleProductsPerPage > 0 ? 1 : 0);
    }

    private void displayNextPageProducts() {
        int availablePagesCount = computeAvailablePagesCount();
        OutputProductResults outputProductResults = getOutputProductResults();
        if (outputProductResults.getCurrentPageNumber() < availablePagesCount) {
            displayPageProducts(outputProductResults.getCurrentPageNumber() + 1);
        } else {
            if(!downloadsAllPages() && this.downloadProductListTimerRunnable != null && !productListPageDownloaded(outputProductResults.getCurrentPageNumber() + 1)){
                this.downloadProductListTimerRunnable.downloadProductListNextPage();
                if(this.downloadProductListTimerRunnable.isFinished()){
                    refreshPaginationButtons();
                }
            } else {
                throw new IllegalStateException("The current page number " + outputProductResults.getCurrentPageNumber() + " must be < than the total page count " + availablePagesCount + ".");
            }
        }
    }

    private void displayLastPageProducts() {
        OutputProductResults outputProductResults = getOutputProductResults();
        int availablePagesCount = computeAvailablePagesCount();
        if (outputProductResults.getCurrentPageNumber() < availablePagesCount) {
            displayPageProducts(availablePagesCount);
        } else {
            throw new IllegalStateException("The current page number " + outputProductResults.getCurrentPageNumber()+" must be < than the total page count " + availablePagesCount + ".");
        }
    }

    private void displayFirstPageProducts() {
        OutputProductResults outputProductResults = getOutputProductResults();
        if (outputProductResults.getCurrentPageNumber() > 1) {
            displayPageProducts(1);
        } else {
            int availablePagesCount = computeAvailablePagesCount();
            throw new IllegalStateException("The current page number " + outputProductResults.getCurrentPageNumber()+" must be < than the total page count " + availablePagesCount + ".");
        }
    }

    private void displayPreviousPageProducts() {
        OutputProductResults outputProductResults = getOutputProductResults();
        if (outputProductResults.getCurrentPageNumber() > 1) {
            displayPageProducts(outputProductResults.getCurrentPageNumber() - 1);
        } else {
            int availablePagesCount = computeAvailablePagesCount();
            throw new IllegalStateException("The current page number " + outputProductResults.getCurrentPageNumber()+" must be < than the total page count " + availablePagesCount + ".");
        }
    }

    private void displayPageProducts(int newCurrentPageNumber) {
        if (newCurrentPageNumber <= 0) {
            throw new IllegalArgumentException("The new current page number " + newCurrentPageNumber +" must be > 0.");
        }
        OutputProductResults outputProductResults = getOutputProductResults();
        int startIndex = (newCurrentPageNumber-1) * this.visibleProductsPerPage;
        int endIndex = startIndex + this.visibleProductsPerPage;
        if (endIndex >= outputProductResults.getAvailableProductCount()) {
            endIndex = outputProductResults.getAvailableProductCount();
        }
        final int size = endIndex - startIndex;
        if(size > 0){
            List<RepositoryProduct> pageProducts = new ArrayList<>(size);
            for (int i = startIndex; i < endIndex; i++) {
                pageProducts.add(outputProductResults.getProductAt(i));
            }
            outputProductResults.setCurrentPageNumber(newCurrentPageNumber);
            this.productListPanel.setProducts(pageProducts);
            firePageProductChanged();
        }
    }

    private void clearPageProducts() {
        this.productListPanel.getProductListModel().clear();
        firePageProductChanged();
    }

    private void firePageProductChanged() {
        firePropertyChange(PAGE_PRODUCTS_CHANGED, null, null);
    }

    private void refreshPaginationButtons() {
        int availablePagesCount = computeAvailablePagesCount();
        int totalPagesCount = computeTotalPagesCount();
        boolean previousPageEnabled = false;
        boolean nextPageEnabled = false;
        boolean lastPageEnabled = false;
        String text = "";
        if (availablePagesCount > 0) {
            OutputProductResults outputProductResults = getOutputProductResults();
            if (outputProductResults.getCurrentPageNumber() > 0) {
                text = Integer.toString(outputProductResults.getCurrentPageNumber()) + " / " + Integer.toString(totalPagesCount);
                if (outputProductResults.getCurrentPageNumber() > 1) {
                    previousPageEnabled = true;
                }
                if(outputProductResults.getCurrentPageNumber() < totalPagesCount) {
                    if (outputProductResults.getCurrentPageNumber() < availablePagesCount) {
                        nextPageEnabled = true;
                        lastPageEnabled = true;
                    } else {
                        if (!downloadsAllPages() && this.downloadProductListTimerRunnable != null && !productListPageDownloaded(outputProductResults.getCurrentPageNumber() + 1)) {
                            nextPageEnabled = true;
                        }
                    }
                }
            } else {
                throw new IllegalStateException("The current page number is 0.");
            }
        }
        this.productListPaginationPanel.refreshPaginationButtons(previousPageEnabled, nextPageEnabled, lastPageEnabled, text);
    }

    private int computeAvailablePagesCount() {
        OutputProductResults outputProductResults = getOutputProductResults();
        int count = 0;
        if (outputProductResults.getAvailableProductCount() > 0) {
            long totalCount = outputProductResults.getAvailableProductCount();
            count = (int) (totalCount / this.visibleProductsPerPage);
            if (totalCount % this.visibleProductsPerPage > 0) {
                count++;
            }
        }
        return count;
    }

    private int computeTotalPagesCount() {
        OutputProductResults outputProductResults = getOutputProductResults();
        int count = 0;
        if (outputProductResults.getAvailableProductCount() > 0) {
            long totalCount = outputProductResults.getFullResultsListCount();
            count = (int) (totalCount / this.visibleProductsPerPage);
            if (totalCount % this.visibleProductsPerPage > 0) {
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
        int pageNr = getOutputProductResults().getCurrentPageNumber();
        pageNr = pageNr < 1 ? 0 : pageNr - 1;
        int pageProductStartIndex = this.visibleProductsPerPage * pageNr;
        int pageProductEndIndex = pageProductStartIndex + pageProductCount;
        long totalProductCount = getOutputProductResults().getFullResultsListCount();
        String intervalText = Integer.toString(pageProductEndIndex);
        if(pageProductCount > 0){
            intervalText = Integer.toString(pageProductStartIndex + 1) + " -> " + intervalText;
        }
        String text = getTitle() + ": " + intervalText;
        if (totalProductCount > 0) {
            text += " out of " + Long.toString(totalProductCount);
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
        northPanel.add(this.sortingTypeComboBox, c);

        c = SwingUtils.buildConstraints(4, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
        northPanel.add(this.progressBarHelper.getProgressBar(), c);

        if (showStopDownloadButton) {
            c = SwingUtils.buildConstraints(5, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
            northPanel.add(this.progressBarHelper.getStopButton(), c);
        }

        JScrollPane scrollPane = new JScrollPane(this.productListPanel);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(this.productListPanel.getBackground());
        scrollPane.setBorder(SwingUtils.LINE_BORDER);

        add(northPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(this.productsTimelinePanel, BorderLayout.SOUTH);
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
                final LocalDateTime acquisitionDate1 = o1.getAcquisitionDate();
                final LocalDateTime acquisitionDate2 = o2.getAcquisitionDate();
                if (acquisitionDate1 == null && acquisitionDate2 == null) {
                    return 0; // both acquisition dates are null
                }
                if (acquisitionDate1 == null) {
                    return -1; // the first acquisition date is null
                }
                if (acquisitionDate2 == null) {
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

    private static class ProductsComparator implements Comparator<RepositoryProduct> {

        private final Comparator<RepositoryProduct> comparator;
        private final boolean sortAscending;

        public ProductsComparator(Comparator<RepositoryProduct> comparator, boolean sortAscending) {
            this.comparator = comparator;
            this.sortAscending = sortAscending;
        }

        @Override
        public int compare(RepositoryProduct leftProduct, RepositoryProduct rightProduct) {
            int result = this.comparator.compare(leftProduct, rightProduct);
            return this.sortAscending ? result : -result;
        }
    }
}
