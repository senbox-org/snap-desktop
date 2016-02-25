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
import net.coobird.thumbnailator.makers.FixedSizeThumbnailMaker;
import org.esa.snap.core.datamodel.quicklooks.Thumbnail;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;


/**
 * Displays a panel of thumbnails
 */
public class ThumbnailPanel extends JPanel {

    private final static int imgWidth = 200;
    private final static int imgHeight = 200;
    private final static int margin = 6;
    private final static BasicStroke thickStroke = new BasicStroke(5);
    private final static String vkControl = "VK_CONTROL";

    private enum SelectionMode {CHECK, RECT}

    private final boolean multiRow;
    private SelectionMode selectionMode;

    private List<ThumbnailDrawing> selection;
    private boolean ctrlPressed;

    public ThumbnailPanel(final boolean multiRow) {
        super(new FlowLayout(FlowLayout.LEADING));
        this.multiRow = multiRow;
        this.selectionMode = SelectionMode.RECT;
        this.selection = new ArrayList<>();

        final DragScrollListener dragScrollListener = new DragScrollListener(this);
        dragScrollListener.setDraggableElements(DragScrollListener.DRAGABLE_VERTICAL_SCROLL_BAR);
        addMouseListener(dragScrollListener);
        addMouseMotionListener(dragScrollListener);
        setKeyBindings();
    }

    private void setKeyBindings() {
        final InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_CONTROL, KeyEvent.CTRL_DOWN_MASK), vkControl+"DOWN");
        inputMap.put(KeyStroke.getKeyStroke("released CONTROL"), vkControl+"UP");

        final ActionMap actionMap = getActionMap();
        actionMap.put(vkControl+"DOWN", new KeyAction(vkControl+"DOWN"));
        actionMap.put(vkControl+"UP", new KeyAction(vkControl+"UP"));
    }

    private class KeyAction extends AbstractAction {
        public KeyAction(String actionCommand) {
            putValue(ACTION_COMMAND_KEY, actionCommand);
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            final String cmd = e.getActionCommand();
            ctrlPressed = cmd.equals(vkControl+"DOWN");
        }
    }

    public void update(final Thumbnail[] imageList) {
        this.removeAll();

        final Insets insets = getInsets();
        final int width = getWidth() - (insets.left + insets.right);

        if (imageList.length == 0) {
            setPreferredSize(new Dimension(width, imgHeight + 2 * margin));
            JLabel label = new JLabel("");
            this.add(label);
        } else {
            if (multiRow) {
                int numImages = 1;
                int effectiveImageWidth = imgWidth * numImages + margin;

                int numCol = Math.max(width / effectiveImageWidth, 1);
                int numRow = (int)Math.ceil(imageList.length / (double)numCol);

                int preferredWidth = effectiveImageWidth * numCol + margin;
                int preferredHeight = (imgHeight + margin) * numRow + margin;
                setPreferredSize(new Dimension(preferredWidth, preferredHeight));
            }

            for (Thumbnail thumbnail : imageList) {
                this.add(new ThumbnailDrawing(this, thumbnail));
            }
        }
        updateUI();
    }

    private void setSelection(final ThumbnailDrawing item) {
        if(selection.contains(item)) {
            selection.remove(item);
        } else {
            if(ctrlPressed) {
                selection.add(item);
            } else {
                selection.clear();
                selection.add(item);
            }
        }
        onSelectionChanged();
    }

    public void onSelectionChanged() {

    }

    public void selectAll() {
        selection.clear();
        for(Component component : this.getComponents()) {
            selection.add((ThumbnailDrawing)component);
        }
        repaint();
    }

    public void clearSelection() {
        selection.clear();
        repaint();
    }

    public ThumbnailDrawing[] getSelection() {
        return selection.toArray(new ThumbnailDrawing[selection.size()]);
    }

    private boolean isSelected(final ThumbnailDrawing item) {
        return selection.contains(item);
    }

    public class ThumbnailDrawing extends JLabel implements MouseListener {
        private final ThumbnailPanel parent;
        private final Thumbnail thumbnail;

        public ThumbnailDrawing(final ThumbnailPanel parent, final Thumbnail thumbnail) {
            this.parent = parent;
            this.thumbnail = thumbnail;
            setPreferredSize(new Dimension(imgWidth, imgHeight));
            setToolTipText("");

            addMouseListener(this);
        }

        public Thumbnail getThumbnail() {
            return thumbnail;
        }

        @Override
        public void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            final Graphics2D g = (Graphics2D) graphics;
            g.setColor(Color.BLACK);
            g.fillRect(0,0, imgWidth, imgHeight);

            if(thumbnail.hasImage() || thumbnail.hasCachedImage()) {
                drawIcon(g, thumbnail.getImage(ProgressMonitor.NULL));
            } else {
                drawIcon(g, null);
            }

            if (isSelected(this) && selectionMode == SelectionMode.RECT) {
                drawSelected(g);
            }
        }

        private void drawIcon(Graphics2D g, BufferedImage icon) {
            if (icon != null) {
                BufferedImage img = new FixedSizeThumbnailMaker()
                        .size(imgWidth, imgHeight)
                        .keepAspectRatio(true)
                        .fitWithinDimensions(true)
                        .make(icon);
                int xOff = (imgWidth - img.getWidth())/2;
                int yOff = (imgHeight - img.getHeight())/2;
                g.drawImage(img, xOff, yOff, img.getWidth(), img.getHeight(), null);

            } else {
                // Draw cross to indicate missing image
                g.setColor(Color.DARK_GRAY);
                g.setStroke(thickStroke);
                g.drawLine(0, 0, 0 + imgWidth, imgHeight);
                g.drawLine(0 + imgWidth, 0, 0, imgHeight);
                g.drawRect(0, 0, imgWidth-1, imgHeight-1);
            }
        }

        private void drawSelected(Graphics2D g) {
            g.setColor(new Color(0,100,255));
            g.setStroke(thickStroke);
            g.drawRect(0, 0, imgWidth, imgHeight);
            for(int i=0; i <= 20; ++i) {
                int alpha = 40-(i*2);
                g.setColor(new Color(0,100,255,alpha));
                g.drawRoundRect(i, i, imgWidth-i-i, imgHeight-i-i, 25, 25);
            }
        }

        @Override
        public Point getToolTipLocation(MouseEvent e) {
            return new Point(-700, 0);
        }

        @Override
        public JToolTip createToolTip() {
            if(!thumbnail.hasImage() || !thumbnail.hasCachedImage()) {
                return super.createToolTip();
            }
            final BufferedImage thumbnailImage = thumbnail.getImage(ProgressMonitor.NULL);

            BufferedImage img = new FixedSizeThumbnailMaker()
                    .size(imgWidth*3, imgHeight*3)
                    .keepAspectRatio(true)
                    .fitWithinDimensions(true)
                    .make(thumbnailImage);

            final JToolTip toolTip = new JToolTip() {
                {
                    setLayout(new BorderLayout());
                    add(new JLabel(new ImageIcon(img)));
                }
                public Dimension getPreferredSize() {
                    return new Dimension(img.getWidth(), img.getHeight());
                }
            };
            return toolTip;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                setSelection(this);
                parent.repaint();
            } else if (e.getButton() == MouseEvent.BUTTON3) {

            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }
    }
}
