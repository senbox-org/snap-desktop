/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.rcp.quicklooks;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import net.coobird.thumbnailator.makers.FixedSizeThumbnailMaker;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.quicklooks.Quicklook;
import org.esa.snap.core.datamodel.quicklooks.Thumbnail;
import org.esa.snap.core.dataop.downloadable.StatusProgressMonitor;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.window.OpenRGBImageViewAction;
import org.esa.snap.tango.TangoIcons;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.tool.ToolButtonFactory;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.media.jai.JAI;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

@TopComponent.Description(
        preferredID = "QuicklookToolView",
        iconBase = "org/esa/snap/rcp/icons/quicklook.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(
        mode = "navigator",
        openAtStartup = false,
        position = 1
)
@ActionID(category = "Window", id = "org.esa.snap.rcp.quicklooks.QuicklookToolView")
@ActionReferences({
        @ActionReference(path = "Menu/View/Tool Windows", position = 50),
        @ActionReference(path = "Toolbars/Tool Windows")
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_QuicklookToolView_Name",
        preferredID = "QuicklookToolView"
)
@NbBundle.Messages({
        "CTL_QuicklookToolView_Name=Quicklooks",
        "CTL_QuicklookToolView_Description=Quicklooks of all bands",
})
/**
 * Tool window to display quicklooks
 */
public class QuicklookToolView extends TopComponent implements Thumbnail.ThumbnailListener {

    private Product currentProduct;
    private final SortedSet<Product> productSet;
    private final Map<Product, ProductState> productStateMap = new HashMap<>();
    private final JComboBox<String> quicklookNameCombo = new JComboBox<>();
    private final JLabel nameLabel = new JLabel();
    private final ImagePanel imgPanel = new ImagePanel();
    private final BufferedImage noDataImage;
    private JScrollPane imgScrollPanel;
    private JButton nextBtn, prevBtn, startBtn, endBtn, openRGBBtn, refreshBtn, viewBtn;
    private final ButtonActionListener actionListener = new ButtonActionListener();
    private boolean updateQuicklooks = false;
    private ProductNode oldNode = null;

    private int zoom = 1;

    private static final String DEFAULT_QUICKLOOK = "Default";

    private static final ImageIcon firstIcon = TangoIcons.actions_go_first(TangoIcons.Res.R22);
    private static final ImageIcon lastIcon = TangoIcons.actions_go_last(TangoIcons.Res.R22);
    private static final ImageIcon nextIcon = TangoIcons.actions_go_next(TangoIcons.Res.R22);
    private static final ImageIcon previousIcon = TangoIcons.actions_go_previous(TangoIcons.Res.R22);
    private static final ImageIcon refreshIcon = TangoIcons.actions_view_refresh(TangoIcons.Res.R22);

    private static final ImageIcon openIcon = TangoIcons.actions_document_open(TangoIcons.Res.R22);
    private static final ImageIcon singleViewIcon = UIUtils.loadImageIcon("/org/esa/snap/rcp/icons/view_single24.png", ThumbnailPanel.class);
    private static final ImageIcon thumbnailViewIcon = UIUtils.loadImageIcon("/org/esa/snap/rcp/icons/view_thumbnails24.png", ThumbnailPanel.class);

    static class ProductState {
        SortedSet<String> quicklookNameSet;
        String selectedQuicklookName;

        public ProductState(Product product) {
            this.quicklookNameSet = new TreeSet<>();
            this.quicklookNameSet.add(DEFAULT_QUICKLOOK);
            for(int i=0; i < product.getQuicklookGroup().getNodeCount(); ++i) {
                this.quicklookNameSet.add(product.getQuicklookGroup().get(i).getName());
            }
            this.selectedQuicklookName = DEFAULT_QUICKLOOK;
        }
    }

    public QuicklookToolView() {
        setLayout(new BorderLayout());
        setDisplayName(Bundle.CTL_QuicklookToolView_Name());
        setToolTipText(Bundle.CTL_QuicklookToolView_Description());
        add(createPanel(), BorderLayout.CENTER);
        noDataImage = createNoDataImage();

        final SnapApp snapApp = SnapApp.getDefault();
        snapApp.getProductManager().addListener(new ProductManagerListener());

        productSet = new TreeSet<>((p1, p2) -> {
            int ref1 = p1.getRefNo();
            int ref2 = p2.getRefNo();
            return Integer.compare(ref1, ref2);
        });

        addProducts();

        updateButtons();

        quicklookNameCombo.setSelectedItem(DEFAULT_QUICKLOOK);
        quicklookNameCombo.addActionListener(e -> {
            if(!productSet.isEmpty()) {
                String selectedQuicklookName = (String)quicklookNameCombo.getSelectedItem();
                if(currentProduct != null && selectedQuicklookName != null && quicklookNameCombo.getItemCount() > 1) {
                    productStateMap.get(currentProduct).selectedQuicklookName = selectedQuicklookName;
                    showQuicklook(currentProduct);
                }
            }
        });

        snapApp.getSelectionSupport(ProductNode.class).addHandler((oldValue, newValue) -> {
            if (newValue != null && newValue != oldNode) {
                showProduct(newValue.getProduct());
                oldNode = newValue;
            }
        });
    }

    public JComponent createPanel() {

        final JPanel panel = new JPanel(new BorderLayout());
        panel.add(createTopPanel(), BorderLayout.NORTH);
        panel.add(createSidePanel(), BorderLayout.EAST);
        panel.add(createImagePanel(), BorderLayout.CENTER);
        panel.add(createButtonPanel(), BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createTopPanel() {
        final JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(nameLabel, BorderLayout.CENTER);
        topPanel.add(quicklookNameCombo, BorderLayout.EAST);

        return topPanel;
    }

    private JPanel createSidePanel() {

        final JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));

//        viewBtn = createButton("viewButton", "Change View", sidePanel, actionListener, thumbnailViewIcon);
//        viewBtn.addActionListener(new ActionListener() {
//            public synchronized void actionPerformed(final ActionEvent e) {
//
//            }
//        });
//        sidePanel.add(viewBtn);

        openRGBBtn = createButton("OpenRGB", "Open RGB", sidePanel, actionListener, openIcon);
        sidePanel.add(openRGBBtn);

        return sidePanel;
    }

    private JScrollPane createImagePanel() {
        imgScrollPanel = new JScrollPane(imgPanel);
        imgPanel.setComponentPopupMenu(createImagePopup());
        return imgScrollPanel;
    }

    private JPanel createButtonPanel() {
        final JPanel buttonPanel = new JPanel();

        startBtn = createButton("Start", "Go to first product", buttonPanel, actionListener, firstIcon);
        prevBtn = createButton("Prev", "Previous product", buttonPanel, actionListener, previousIcon);
        nextBtn = createButton("Next", "Next product", buttonPanel, actionListener, nextIcon);
        endBtn = createButton("End", "Go to last product", buttonPanel, actionListener, lastIcon);
        refreshBtn = createButton("Refresh", "Update products", buttonPanel, actionListener, refreshIcon);

        buttonPanel.add(startBtn);
        buttonPanel.add(prevBtn);
        buttonPanel.add(nextBtn);
        buttonPanel.add(endBtn);
        buttonPanel.add(refreshBtn);

        return buttonPanel;
    }

    private static JButton createButton(final String name, final String text, final JPanel panel,
                                        final ButtonActionListener actionListener, final ImageIcon icon) {
        final JButton btn = (JButton) ToolButtonFactory.createButton(icon, false);
        btn.setName(name);
        btn.setIcon(icon);
        if (panel != null) {
            btn.setBackground(panel.getBackground());
        }
        btn.setToolTipText(text);
        btn.setActionCommand(name);
        btn.addActionListener(actionListener);

        return btn;
    }

    private void updateButtons() {

        boolean hasProducts = !productSet.isEmpty();
        boolean hasPrevProd = getPreviousProduct() != null;
        boolean hasNextProd = getNextProduct() != null;

        startBtn.setEnabled(hasPrevProd);
        prevBtn.setEnabled(hasPrevProd);
        nextBtn.setEnabled(hasNextProd);
        endBtn.setEnabled(hasNextProd);
        refreshBtn.setEnabled(hasProducts);

        if(!hasProducts) {
            imgPanel.setImage(null);
            nameLabel.setText("");
            currentProduct = null;
        }

        openRGBBtn.setEnabled(currentProduct != null);
    }

    private static BufferedImage createNoDataImage() {
        final int w = 200, h = 200;
        final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = image.createGraphics();

        g.addRenderingHints(new RenderingHints(
                RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));
        g.addRenderingHints(new RenderingHints(
                RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
        g.addRenderingHints(new RenderingHints(
                RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC));

        g.setColor(Color.DARK_GRAY);
        g.setStroke(new BasicStroke(1));
        g.drawLine(0, 0, w, h);
        g.drawLine(w, 0, 0, h);
        g.drawRect(0, 0, w - 1, h - 1);

        return image;
    }

    private synchronized void loadProducts(final String qlName) {
        try {
            final List<Thread> threadList = new ArrayList<>();
            final int numConsecutiveThreads = Runtime.getRuntime().availableProcessors();

            // collect quicklooks that need to load
            final List<Quicklook> quicklooksToLoad = new ArrayList<>();
            for (final Product product : productSet) {
                if(product.getFileLocation() != null) {
                    final Quicklook quicklook;
                    if (qlName == null || qlName.equals(DEFAULT_QUICKLOOK)) {
                        quicklook = product.getDefaultQuicklook();
                    } else {
                        quicklook = product.getQuicklook(qlName);
                    }
                    if (quicklook != null && !quicklook.hasImage() && !quicklook.hasCachedImage()) {
                        quicklooksToLoad.add(quicklook);
                    }
                }
            }
            if(quicklooksToLoad.isEmpty()) {
                updateQuicklooks = true;
                showProduct(currentProduct);
                return;
            }

            ProgressMonitorSwingWorker<Boolean, Object> worker = new ProgressMonitorSwingWorker<>
                    (SnapApp.getDefault().getMainFrame(), "Loading quicklooks") {
                @Override
                protected Boolean doInBackground(com.bc.ceres.core.ProgressMonitor pm) throws Exception {
                    final int total = productSet.size();
                    pm.beginTask("Generating quicklooks", total);
                    int cnt = 1;
                    for (final Quicklook quicklook : quicklooksToLoad) {
                        if(pm.isCanceled())
                            break;

                        final Thread worker = new Thread(() -> {
                            try {
                                quicklook.getImage(SubProgressMonitor.create(pm, 1));
                            } catch (Throwable e) {
                                SystemUtils.LOG.warning("Unable to create quicklook for " + quicklook.getProductFile() + '\n' + e.getMessage());
                            }
                        });

                        threadList.add(worker);
                        worker.start();

                        if (threadList.size() >= numConsecutiveThreads) {
                            for (Thread t : threadList) {
                                t.join();
                            }
                            threadList.clear();
                        }

                        JAI.getDefaultInstance().getTileCache().flush();
                        JAI.getDefaultInstance().getTileCache().memoryControl();
                        System.gc();

                        pm.setTaskName("Generating quicklooks " + cnt + " of " + total);
                        ++cnt;
                        //pm.worked(1);
                    }
                    pm.done();

                    if (!threadList.isEmpty()) {
                        for (Thread t : threadList) {
                            t.join();
                        }
                    }

                    return true;
                }

                @Override
                protected void done() {
                    super.done();
                    updateQuicklooks = true;
                    showProduct(currentProduct);
                }
            };
            worker.execute();

        } catch (Exception e) {
            SnapApp.getDefault().handleError("Unable to load quicklooks", e);
        }
    }

    public void setSelectedQuicklook(final Quicklook ql) {
        updateQuicklooks = true;
        showProduct(ql.getProduct());
        quicklookNameCombo.setSelectedItem(ql.getName());
    }

    private synchronized void showProduct(final Product product) {
        if (product == null) {
            return;
        }
        currentProduct = product;
        updateQuicklookNameCombo();

        showQuicklook(product);
    }

    private void showQuicklook(final Product product) {
        final String qlName = (String)quicklookNameCombo.getSelectedItem();
        if(qlName != null) {
            final Quicklook quicklook;
            if (qlName.equals(DEFAULT_QUICKLOOK)) {
                quicklook = product.getDefaultQuicklook();
            } else {
                quicklook = product.getQuicklook(qlName);
            }

            if(quicklook != null) {
                quicklook.addListener(this);
                if ((quicklook.hasImage() || quicklook.hasCachedImage())) {
                    setImage(product, quicklook);
                } else if (updateQuicklooks) {
                    if (product.getFileLocation() != null) {
                        loadImage(product, quicklook);
                    }
                } else {
                    setImage(product, null);
                }
            } else {
                setImage(product, null);
            }
        }
    }

    private static void loadImage(final Product product, final Quicklook quicklook) {
        final StatusProgressMonitor qlPM = new StatusProgressMonitor(StatusProgressMonitor.TYPE.SUBTASK);
        qlPM.beginTask("Creating quicklook " + product.getName() + "... ", 100);

        ProgressMonitorSwingWorker<BufferedImage, Object> loader = new ProgressMonitorSwingWorker<>
                (SnapApp.getDefault().getMainFrame(), "Loading quicklook image...") {

            @Override
            protected BufferedImage doInBackground(com.bc.ceres.core.ProgressMonitor pm) {

                return quicklook.getImage(qlPM);
            }

            @Override
            protected void done() {
                qlPM.done();
            }
        };
        loader.execute();
    }

    private void setImage(final Product product, final Quicklook quicklook) {

        final BufferedImage img;
        if(quicklook != null && (quicklook.hasImage() || quicklook.hasCachedImage())) {
            img = quicklook.getImage(ProgressMonitor.NULL);
        } else {
            img = noDataImage;
        }

        if(currentProduct == product && imgPanel.getImage() == img) {
            return;
        }
        currentProduct = product;
        nameLabel.setText(product.getDisplayName());
        imgPanel.setImage(img);

        updateButtons();
    }

    private void addProducts() {

        final Product[] products = SnapApp.getDefault().getProductManager().getProducts();
        for(Product product : products) {
            addProduct(product);
        }
    }

    private synchronized void addProduct(final Product product) {
        productSet.add(product);
        final ProductState productState = new ProductState(product);
        this.productStateMap.put(product, productState);
        updateQuicklookNameCombo();
    }

    private synchronized void removeProduct(final Product product) {
        if(currentProduct == product) {
            currentProduct = null;
        }
        productSet.remove(product);
        productStateMap.remove(product);
        updateQuicklookNameCombo();
    }

    private void updateQuicklookNameCombo() {
        quicklookNameCombo.removeAllItems();
        if(currentProduct == null) {
            return;
        }
        final ProductState productState = productStateMap.get(currentProduct);
        quicklookNameCombo.setSelectedItem(productState.selectedQuicklookName);
        for(String name : productState.quicklookNameSet) {
            quicklookNameCombo.addItem(name);
        }
        // restore selection
        quicklookNameCombo.setSelectedItem(productState.selectedQuicklookName);
    }

    private Product getFirstProduct() {
        return productSet.isEmpty() ? null : productSet.first();
    }

    private Product getLastProduct() {
        return productSet.isEmpty() ? null : productSet.last();
    }

    private Product getNextProduct() {
        final Iterator<Product> itr = productSet.iterator();
        while (itr.hasNext()) {
            Product p = itr.next();
            if (p == currentProduct) {
                if (itr.hasNext()) {
                    return itr.next();
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    private Product getPreviousProduct() {
        final Iterator<Product> itr = productSet.iterator();
        Product prev = null;
        while (itr.hasNext()) {
            Product p = itr.next();
            if (p == currentProduct) {
                return prev;
            }
            prev = p;
        }
        return null;
    }

    private void openRGB() {
        if(currentProduct != null) {
            final OpenRGBImageViewAction rgbAction = new OpenRGBImageViewAction(currentProduct);
            rgbAction.openProductSceneViewRGB(currentProduct, "");
        }
    }

    private class ImagePanel extends JLabel implements MouseListener {

        private BufferedImage img = null;

        public ImagePanel() {
            setHorizontalAlignment(JLabel.CENTER);
            setVerticalAlignment(JLabel.CENTER);

            addMouseListener(this);
        }

        public BufferedImage getImage() {
            return img;
        }

        public void setImage(final BufferedImage img) {
            this.img = img;
            if (img == null) {
                setIcon(null);
            }
            repaint();
        }

        @Override
        public void paintComponent(Graphics g) {
            if (img != null) {
                Graphics2D g2 = (Graphics2D)g;
                g2.addRenderingHints(new RenderingHints(
                        RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));
                g2.addRenderingHints(new RenderingHints(
                        RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
                g2.addRenderingHints(new RenderingHints(
                        RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC));

                int w = imgScrollPanel.getWidth()*zoom - 10;
                int h = imgScrollPanel.getHeight()*zoom - 10;
                setIcon(new ImageIcon(new FixedSizeThumbnailMaker()
                        .size(w, h)
                        .keepAspectRatio(true)
                        .fitWithinDimensions(true)
                        .make(img)));
            }
            super.paintComponent(g);
        }

        public void mouseClicked(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
            if (e.getClickCount() == 2 && currentProduct != null) {
                openRGB();
            }
        }

        public void mouseReleased(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }
    }

    private class ButtonActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            switch (e.getActionCommand()) {
                case "Start":
                    showProduct(getFirstProduct());
                    break;
                case "Prev":
                    showProduct(getPreviousProduct());
                    break;
                case "Next":
                    showProduct(getNextProduct());
                    break;
                case "End":
                    showProduct(getLastProduct());
                    break;
                case "Refresh":
                    if(!productSet.isEmpty()) {
                        loadProducts((String)quicklookNameCombo.getSelectedItem());
                    }
                    break;
                case "OpenRGB":
                    openRGB();
                    break;
            }
        }
    }

    public class ProductManagerListener implements ProductManager.Listener {

        @Override
        public void productAdded(ProductManager.Event event) {
            addProduct(event.getProduct());
            updateButtons();
        }

        @Override
        public void productRemoved(ProductManager.Event event) {
            if (event.getProduct() == currentProduct) {
                getNextProduct();
            }
            removeProduct(event.getProduct());
            updateButtons();
        }
    }

    public JPopupMenu createImagePopup() {
        final JPopupMenu popup = new JPopupMenu();

        final JMenuItem zoomInItem = new JMenuItem("Zoom in");
        zoomInItem.addActionListener(e -> {
            if(zoom < 10) {
                zoom += 2;
            }
        });
        popup.add(zoomInItem);

        final JMenuItem zoomOutItem = new JMenuItem("Zoom out");
        zoomOutItem.addActionListener(e -> {
            if(zoom > 2) {
                zoom -= 2;
            }
        });
        popup.add(zoomOutItem);

        return popup;
    }

    public void notifyImageUpdated(Thumbnail thumbnail) {
        showProduct(thumbnail.getProduct());
    }
}
