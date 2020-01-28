/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.ui.product;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductNode;

import java.util.Arrays;
import java.util.Comparator;

/**
 * @author Thomas Storm
 */
class BandSorter {

    static Comparator<Band> createComparator() {
        return Comparator.comparing(ProductNode::getName);
    }

    static void sort(Band[] allBands) {
        Arrays.parallelSort(allBands, createComparator());
    }
}
