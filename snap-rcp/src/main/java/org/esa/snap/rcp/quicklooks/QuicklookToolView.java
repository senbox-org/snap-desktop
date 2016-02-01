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
import org.esa.snap.core.dataop.downloadable.StatusProgressMonitor;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.gpf.ThreadManager;
import org.esa.snap.engine_utilities.util.MemUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.window.OpenRGBImageViewAction;
import org.esa.snap.rcp.util.SelectionSupport;
import org.esa.snap.tango.TangoIcons;
import org.esa.snap.ui.tool.ToolButtonFactory;
import org.netbeans.api.annotations.common.NullAllowed;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
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
        @ActionReference(path = "Menu/View/Tool Windows"),
        @ActionReference(path = "Toolbars/Analysis")
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
public class QuicklookToolView extends TopComponent {

    private Product currentProduct;
    private final SortedSet<Product> productSet;
    private final SortedSet<String> quicklookNameSet = new TreeSet<>();
    private final JComboBox<String> quicklookNameCombo = new JComboBox<>();
    private final JLabel nameLabel = new JLabel();
    private final ImagePanel imgPanel = new ImagePanel();
    private final BufferedImage noDataImage;
    private JScrollPane imgScrollPanel;
    private JButton nextBtn, prevBtn, startBtn, endBtn, openBtn, closeBtn, refreshBtn;
    private ButtonActionListener actionListener = new ButtonActionListener();
    private boolean updateQuicklooks = false;
    private ProductNode oldNode = null;

    private int zoom = 1;

    private static final String DEFAULT_QUICKLOOK = "Default";

    private static final ImageIcon openIcon = TangoIcons.actions_document_open(TangoIcons.Res.R22);
    private static final ImageIcon closeIcon = TangoIcons.actions_list_remove(TangoIcons.Res.R22);

    private static final ImageIcon firstIcon = TangoIcons.actions_go_first(TangoIcons.Res.R22);
    private static final ImageIcon lastIcon = TangoIcons.actions_go_last(TangoIcons.Res.R22);
    private static final ImageIcon nextIcon = TangoIcons.actions_go_next(TangoIcons.Res.R22);
    private static final ImageIcon previousIcon = TangoIcons.actions_go_previous(TangoIcons.Res.R22);
    private static final ImageIcon refreshIcon = TangoIcons.actions_view_refresh(TangoIcons.Res.R22);

    public QuicklookToolView() {
        setLayout(new BorderLayout());
        setDisplayName(Bundle.CTL_QuicklookToolView_Name());
        setToolTipText(Bundle.CTL_QuicklookToolView_Description());
        add(createPanel(), BorderLayout.CENTER);
        noDataImage = createNoDataImage();

        final SnapApp snapApp = SnapApp.getDefault();
        snapApp.getProductManager().addListener(new ProductManagerListener());

        productSet = new TreeSet<>(new Comparator<Product>() {
            public int compare(Product p1, Product p2) {
                int ref1 = p1.getRefNo();
                int ref2 = p2.getRefNo();
                return ref1 < ref2 ? -1 : ref1 == ref2 ? 0 : 1;
            }
        });

        quicklookNameSet.add(DEFAULT_QUICKLOOK);
        addProducts();

        updateButtons();

        quicklookNameCombo.setSelectedItem(DEFAULT_QUICKLOOK);
        quicklookNameCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showProduct(currentProduct);
            }
        });

        snapApp.getSelectionSupport(ProductNode.class).addHandler(new SelectionSupport.Handler<ProductNode>() {
            @Override
            public void selectionChange(@NullAllowed ProductNode oldValue, @NullAllowed ProductNode newValue) {
                if (newValue != null && newValue != oldNode) {
                    showProduct(newValue.getProduct());
                    oldNode = newValue;
                }
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

        openBtn = createButton("Open", "Open RGB", sidePanel, actionListener, openIcon);
        sidePanel.add(openBtn);
        closeBtn = createButton("Close", "Close Product", sidePanel, actionListener, closeIcon);
        sidePanel.add(closeBtn);

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
        if (productSet.isEmpty()) {
            startBtn.setEnabled(false);
            prevBtn.setEnabled(false);
            nextBtn.setEnabled(false);
            endBtn.setEnabled(false);
            openBtn.setEnabled(false);
            closeBtn.setEnabled(false);
            imgPanel.setImage(null);
            nameLabel.setText("");
            currentProduct = null;
        } else {
            startBtn.setEnabled(true);
            prevBtn.setEnabled(getPreviousProduct() != null);
            nextBtn.setEnabled(getNextProduct() != null);
            endBtn.setEnabled(true);
            openBtn.setEnabled(true);
            closeBtn.setEnabled(true);
        }
        refreshBtn.setEnabled(true);
    }

    private BufferedImage createNoDataImage() {
        final int w = 100, h = 100;
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
        g.drawLine(0, 0, 0 + w, h);
        g.drawLine(0 + w, 0, 0, h);
        g.drawRect(0, 0, w - 1, h - 1);

        return image;
    }

    private synchronized void loadProducts(final String qlName) {
        try {
            final ThreadManager threadManager = new ThreadManager();
            threadManager.setNumConsecutiveThreads(Math.min(threadManager.getNumConsecutiveThreads(), 4));

            ProgressMonitorSwingWorker<Boolean, Object> worker = new ProgressMonitorSwingWorker<Boolean, Object>
                    (SnapApp.getDefault().getMainFrame(), "Loading quicklooks") {
                @Override
                protected Boolean doInBackground(com.bc.ceres.core.ProgressMonitor pm) throws Exception {
                    final int total = productSet.size();
                    pm.beginTask("Generating quicklooks", total);
                    int cnt = 1;
                    for (final Product product : productSet) {
                        if(pm.isCanceled())
                            break;
                        if (product.getFileLocation() != null) {

                            final Thread worker = new Thread() {

                                @Override
                                public void run() {
                                    try {
                                        final Quicklook quicklook;
                                        if (qlName.equals(DEFAULT_QUICKLOOK)) {
                                            quicklook = product.getDefaultQuicklook();
                                        } else {
                                            quicklook = product.getQuicklook(qlName);
                                        }
                                        quicklook.getImage(SubProgressMonitor.create(pm, 1));
                                    } catch (Throwable e) {
                                        SystemUtils.LOG.warning("Unable to create quicklook for " + product.getName() + '\n' + e.getMessage());
                                    }
                                }
                            };
                            threadManager.add(worker);
                        }
                        MemUtils.freeAllMemory();
                        pm.setTaskName("Generating quicklooks " + cnt + " of " + total);
                        ++cnt;
                        //pm.worked(1);
                    }
                    pm.done();
                    threadManager.finish();

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
    }

    private synchronized void showProduct(final Product product) {
        if (product == null) {
            return;
        }

        final String qlName = (String)quicklookNameCombo.getSelectedItem();
        if(qlName != null) {
            if (updateQuicklooks) {
                if (product.getFileLocation() != null) {
                    loadImage(product, qlName);
                }
            }
            setImage(product, qlName);
        }
    }

    private void loadImage(final Product product, final String qlName) {
        final StatusProgressMonitor qlPM = new StatusProgressMonitor(StatusProgressMonitor.TYPE.SUBTASK);
        qlPM.beginTask("Creating quicklook " + product.getName() + "... ", 100);

        ProgressMonitorSwingWorker<BufferedImage, Object> loader = new ProgressMonitorSwingWorker<BufferedImage, Object>
                (SnapApp.getDefault().getMainFrame(), "Loading quicklook image...") {

            @Override
            protected BufferedImage doInBackground(com.bc.ceres.core.ProgressMonitor pm) throws Exception {
                final Quicklook quicklook;
                if (qlName.equals(DEFAULT_QUICKLOOK)) {
                    quicklook = product.getDefaultQuicklook();
                } else {
                    quicklook = product.getQuicklook(qlName);
                }
                return quicklook.getImage(qlPM);
            }

            @Override
            protected void done() {
                qlPM.done();
            }
        };
        loader.execute();
    }

    private synchronized void setImage(final Product product, final String qlName) {

        currentProduct = product;
        nameLabel.setText(product.getDisplayName());

        final Quicklook quicklook;
        if (qlName.equals(DEFAULT_QUICKLOOK)) {
            quicklook = currentProduct.getDefaultQuicklook();
        } else {
            quicklook = currentProduct.getQuicklook(qlName);
        }

        final BufferedImage img;
        if(quicklook != null && (quicklook.hasImage() || quicklook.hasCachedImage())) {
            img = quicklook.getImage(ProgressMonitor.NULL);
        } else {
            img = noDataImage;
        }
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
        for(int i=0; i < product.getQuicklookGroup().getNodeCount(); ++i) {
            quicklookNameSet.add(product.getQuicklookGroup().get(i).getName());
        }
        updateQuicklookNameCombo();
    }

    private synchronized void removeProduct(final Product product) {
        productSet.remove(product);
        cleanUpQuicklookNameSet();
        updateQuicklookNameCombo();
    }

    private void cleanUpQuicklookNameSet() {
        Set<String> toRemove = new HashSet<>();
        for(String name : quicklookNameSet) {
            if(name.equals(DEFAULT_QUICKLOOK))
                continue;

            boolean exists = false;
            for(Product product : productSet) {
                for(int i=0; i < product.getQuicklookGroup().getNodeCount(); ++i) {
                    if(name.equals(product.getQuicklookGroup().get(i).getName())) {
                        exists = true;
                        break;
                    }
                }
            }
            if(!exists) {
                toRemove.add(name);
            }
        }
        for(String name : toRemove) {
            quicklookNameSet.remove(name);
        }
    }

    private void updateQuicklookNameCombo() {
        String selected = (String)quicklookNameCombo.getSelectedItem();
        quicklookNameCombo.removeAllItems();
        for(String name : quicklookNameSet) {
            quicklookNameCombo.addItem(name);
        }
        // restore selection
        if(selected != null && ((DefaultComboBoxModel)quicklookNameCombo.getModel()).getIndexOf(selected) != -1 ) {
            quicklookNameCombo.setSelectedItem(selected);
        } else {
            quicklookNameCombo.setSelectedItem(DEFAULT_QUICKLOOK);
        }
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

    private void openProduct() {
        final OpenRGBImageViewAction rgbAction = new OpenRGBImageViewAction(currentProduct);
        rgbAction.openProductSceneViewRGB(currentProduct, "");
    }

    private void closeProduct() {
        if (currentProduct != null) {
            Product productToClose = currentProduct;
            if(productToClose == getLastProduct()) {
                showProduct(getPreviousProduct());
            } else {
                showProduct(getNextProduct());
            }
            SnapApp.getDefault().getProductManager().removeProduct(productToClose);
        }
    }

    private class ImagePanel extends JLabel implements MouseListener {

        private BufferedImage img = null;

        public ImagePanel() {
            setHorizontalAlignment(JLabel.CENTER);
            setVerticalAlignment(JLabel.CENTER);

            addMouseListener(this);
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
                openProduct();
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
                case "Open":
                    openProduct();
                    break;
                case "Close":
                    closeProduct();
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
        zoomInItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                if(zoom < 10) {
                    zoom += 2;
                }
            }
        });
        popup.add(zoomInItem);

        final JMenuItem zoomOutItem = new JMenuItem("Zoom out");
        zoomOutItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                if(zoom > 2) {
                    zoom -= 2;
                }
            }
        });
        popup.add(zoomOutItem);

        popup.addSeparator();

        final JMenuItem closeItem = new JMenuItem("Close Product");
        closeItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                closeProduct();
            }
        });
        popup.add(closeItem);

        return popup;
    }
}
