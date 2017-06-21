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

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Set;

/**
 * Shows product counts by months
 */
class MonthsPlot extends AbstractTimelinePlot {

    private float interval;
    private final String[] monthNames = new String[]{
            "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    MonthsPlot(final DatabaseStatistics stats) {
        super(stats);
    }

    /**
     * Paints the panel component
     *
     * @param g2d The Graphics
     */
    @Override
    protected void paintPlot(final Graphics2D g2d) {

        final DatabaseStatistics.MonthData monthData = stats.getMonthData();
        final Set<Integer> months = monthData.getMonthSet();
        final int numMonths = months.size();
        final int maxMonthCnt = monthData.getMaxMonthCnt();

        final int w = getWidth();
        final int h = getHeight() - 15;
        interval = w / (float) numMonths;
        final float halfInterval = interval / 2;
        final float barWidth = halfInterval / 2;

        final int y = getHeight() - 2;

        float x = halfInterval;
        for (Integer month : months) {
            drawButton(g2d, monthNames[month], (int) x - 10, y, monthData.isSelected(month));

            final float newH = (monthData.get(month) / (float) maxMonthCnt) * h;
            drawBar(g2d, (int) (x - barWidth), h - (int) newH, (int) halfInterval, (int) newH, h);

            x += interval;
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        int x = (int) (e.getX() / interval);

        final DatabaseStatistics.MonthData monthData = stats.getMonthData();
        monthData.setSelected(x, !monthData.isSelected(x));

        repaint();
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        int x = (int) (event.getX() / interval);

        final Set<Integer> monthSet = stats.getMonthData().getMonthSet();
        final Integer[] months = monthSet.toArray(new Integer[monthSet.size()]);

        int value = stats.getMonthData().get(x);
        if (value == 0)
            return "";
        return monthNames[months[x]] + ": " + value;
    }
}
