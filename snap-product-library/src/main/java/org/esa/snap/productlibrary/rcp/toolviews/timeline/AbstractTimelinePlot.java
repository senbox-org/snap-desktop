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
package org.esa.snap.productlibrary.rcp.toolviews.timeline;

import org.esa.snap.productlibrary.rcp.toolviews.model.DatabaseStatistics;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Shows product counts over time
 */
abstract class AbstractTimelinePlot extends JPanel implements MouseListener {

    final DatabaseStatistics stats;

    AbstractTimelinePlot(final DatabaseStatistics stats) {
        this.stats = stats;
        setToolTipText("");
        addMouseListener(this);
    }

    /**
     * Paints the panel component
     *
     * @param g The Graphics
     */
    @Override
    protected void paintComponent(Graphics g) {
        try {
            super.paintComponent(g);
            final Graphics2D g2d = (Graphics2D) g;
            paintPlot(g2d);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    protected abstract void paintPlot(final Graphics2D g2d);

    static void drawBar(final Graphics2D g2d, final int x, final int y, final int w, final int h, final int maxH) {

        for (int i = 0; i < h; ++i) {
            double pct = Math.max(0.4, (i / (double) maxH));
            g2d.setColor(new Color(7, (int) (150 * pct), (int) (255 * pct)));
            g2d.drawLine(x, y + h - i, x + w, y + h - i);
        }
    }

    static void drawButton(final Graphics2D g2d, final String text, final int x, final int y, boolean selected) {

        final int rw = g2d.getFontMetrics().stringWidth(text) + 10;
        final int rh = g2d.getFontMetrics().getHeight() - 4;
        final int rx = x - 5;
        final int ry = y - rh + 1;

        if (selected) {
            g2d.setColor(Color.lightGray);
            g2d.fillRoundRect(rx, ry, rw, rh, 5, 5);
        } //else {
        // g2d.draw3DRect(rx, ry, rw, rh, true);
        //}

        g2d.setColor(Color.blue);
        g2d.drawRoundRect(rx, ry, rw, rh, 5, 5);

        g2d.setColor(Color.BLACK);
        g2d.drawString(text, x, y);
    }

    /**
     * Invoked when the mouse button has been clicked (pressed
     * and released) on a component.
     */
    public void mouseClicked(MouseEvent e) {
    }

    /**
     * Invoked when a mouse button has been pressed on a component.
     */
    public void mousePressed(MouseEvent e) {
    }

    /**
     * Invoked when a mouse button has been released on a component.
     */
    public void mouseReleased(MouseEvent e) {
    }

    /**
     * Invoked when the mouse enters a component.
     */
    public void mouseEntered(MouseEvent e) {
    }

    /**
     * Invoked when the mouse exits a component.
     */
    public void mouseExited(MouseEvent e) {
    }
}
