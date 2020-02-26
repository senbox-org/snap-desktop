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
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * @author Thomas Storm
 */
public class BandSorterTest {

    @Test
    public void testSort_With_Wavelengths_and_Digits() {
        List<Band> bands = new ArrayList<>();
        bands.add(createBand("spec_1", 200));
        bands.add(createBand("spec_2", 300));
        bands.add(createBand("spec_3", 400));
        bands.add(createBand("spec_4", 500));
        bands.add(createBand("spec_5", 600));
        bands.add(createBand("spec_6", 700));

        Collections.shuffle(bands);
        Band[] bandsArray = bands.toArray(new Band[bands.size()]);

        Arrays.sort(bandsArray, BandSorter.createComparator());
        assertEquals("spec_1", bandsArray[0].getName());
        assertEquals("spec_2", bandsArray[1].getName());
        assertEquals("spec_3", bandsArray[2].getName());
        assertEquals("spec_4", bandsArray[3].getName());
        assertEquals("spec_5", bandsArray[4].getName());
        assertEquals("spec_6", bandsArray[5].getName());
    }

    @Test
    public void testSort_Without_Digits() {
        List<Band> bands = new ArrayList<>();
        bands.add(createBand("spec_a", 200));
        bands.add(createBand("spec_b", 300));
        bands.add(createBand("spec_c", 400));
        bands.add(createBand("spec_d", 500));
        bands.add(createBand("spec_e", 600));
        bands.add(createBand("spec_f", 700));

        Collections.shuffle(bands);
        Band[] bandsArray = bands.toArray(new Band[bands.size()]);

        BandSorter.sort(bandsArray);
        assertEquals("spec_a", bandsArray[0].getName());
        assertEquals("spec_b", bandsArray[1].getName());
        assertEquals("spec_c", bandsArray[2].getName());
        assertEquals("spec_d", bandsArray[3].getName());
        assertEquals("spec_e", bandsArray[4].getName());
        assertEquals("spec_f", bandsArray[5].getName());
    }

    @Test
    public void testSort_Without_Wavelengths() {
        List<Band> bands = new ArrayList<>();
        bands.add(createBand("spec_1", 0));
        bands.add(createBand("spec_2", 0));
        bands.add(createBand("spec_3", 0));
        bands.add(createBand("spec_4", 0));
        bands.add(createBand("spec_5", 0));
        bands.add(createBand("spec_6", 0));

        Collections.shuffle(bands);
        Band[] bandsArray = bands.toArray(new Band[bands.size()]);

        BandSorter.sort(bandsArray);
        assertEquals("spec_1", bandsArray[0].getName());
        assertEquals("spec_2", bandsArray[1].getName());
        assertEquals("spec_3", bandsArray[2].getName());
        assertEquals("spec_4", bandsArray[3].getName());
        assertEquals("spec_5", bandsArray[4].getName());
        assertEquals("spec_6", bandsArray[5].getName());
    }

    @Test
    public void testSort_Without_Wavelengths_And_Digits() {
        List<Band> bands = new ArrayList<>();
        bands.add(createBand("spec_a", 0));
        bands.add(createBand("spec_b", 0));
        bands.add(createBand("spec_c", 0));
        bands.add(createBand("spec_d", 0));
        bands.add(createBand("spec_e", 0));
        bands.add(createBand("spec_f", 0));

        Collections.shuffle(bands);
        Band[] bandsArray = bands.toArray(new Band[bands.size()]);

        BandSorter.sort(bandsArray);
        assertEquals("spec_a", bandsArray[0].getName());
        assertEquals("spec_b", bandsArray[1].getName());
        assertEquals("spec_c", bandsArray[2].getName());
        assertEquals("spec_d", bandsArray[3].getName());
        assertEquals("spec_e", bandsArray[4].getName());
        assertEquals("spec_f", bandsArray[5].getName());
    }

    @Test
    public void testTransitive() {
        List<Band> bands = new ArrayList<>();
        bands.add(createBand("spec_a", 75));
        bands.add(createBand("spec_b", 70));
        bands.add(createBand("spec_c", 65));

        final Comparator<Band> comparator = BandSorter.createComparator();
        assertEquals(-1, comparator.compare(bands.get(0), bands.get(1)));
        assertEquals(-1, comparator.compare(bands.get(1), bands.get(2)));
        assertEquals(-2, comparator.compare(bands.get(0), bands.get(2)));
    }

    @Test
    public void testSignHandlingCommutative() {
        List<Band> bands = new ArrayList<>();
        bands.add(createBand("spec_a", 75));
        bands.add(createBand("spec_b", 70));

        final Comparator<Band> comparator = BandSorter.createComparator();
        assertEquals(-1, comparator.compare(bands.get(0), bands.get(1)));
        assertEquals(1, comparator.compare(bands.get(1), bands.get(0)));
    }

    @Test
    public void testSignHandlingAtEquality() {
        List<Band> bands = new ArrayList<>();
        bands.add(createBand("spec_a", 75));
        bands.add(createBand("spec_b", 75));
        bands.add(createBand("spec_c", 72));

        final Comparator<Band> comparator = BandSorter.createComparator();
        assertEquals(-1, comparator.compare(bands.get(0), bands.get(1)));
        assertEquals(-2, comparator.compare(bands.get(0), bands.get(2)));
        assertEquals(-1, comparator.compare(bands.get(1), bands.get(2)));
    }

    public static Band createBand(String name, int wavelength) {
        Band a = new Band(name, ProductData.TYPE_INT16, 10, 10);
        a.setSpectralWavelength(wavelength);
        return a;
    }
}
