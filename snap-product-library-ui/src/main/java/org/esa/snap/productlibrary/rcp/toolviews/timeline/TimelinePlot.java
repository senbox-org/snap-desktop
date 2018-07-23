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
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Shows product counts over time
 */
class TimelinePlot extends AbstractTimelinePlot {

    TimelinePlot(final DatabaseStatistics stats) {
        super(stats);
    }

    protected void paintPlot(final Graphics2D g2d) {

        final Map<Integer, DatabaseStatistics.YearData> yearDataMap = stats.getYearDataMap();
        final SortedSet<Integer> years = new TreeSet<>(yearDataMap.keySet());
        final int numYears = years.size();
        final int maxDayCnt = stats.getOverallMaxDayCnt();

        final int w = getWidth();
        final int h = getHeight() - 15;
        final float interval = w / (float) numYears;
        final float halfInterval = interval / 2f;

        final int y = getHeight() - 2;
        float x = halfInterval;

        for (Integer year : years) {
            g2d.setColor(Color.BLACK);
            g2d.drawString(String.valueOf(year), x - 20, y);

            final DatabaseStatistics.YearData data = yearDataMap.get(year);

            g2d.setColor(Color.BLACK);
            g2d.drawLine((int) (x - halfInterval), h - 10, (int) (x - halfInterval), h + 5);
            for (int d = 1; d < 366; d++) {
                final float pctX = d / (float) 365;
                final float newH = (data.dayOfYearMap.get(d) / (float) maxDayCnt) * h;
                drawBar(g2d, (int) (x - halfInterval + (pctX * interval)), (int) (h - newH), 1, (int) newH, h);
            }

            x += interval;
        }
    }
}
