/*
 * Copyright (C) 2026 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.rcp.actions.window;

import com.bc.ceres.annotation.STTM;
import eu.esa.snap.core.datamodel.group.BandGroup;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import static org.junit.Assert.assertEquals;

public class OpenCommonRasterDataActionTest {


    @Test
    @STTM("SNAP-4243")
    public void testGetCommonRasterDataNodeNames() {
        Product p1 = createProduct("p1", "B1", "B2", "B3");
        Product p2 = createProduct("p2", "B1", "B3", "B4");
        Product p3 = createProduct("p3", "B3", "B5", "B1");

        List<String> names = OpenCommonRasterDataAction.getCommonRasterDataNodeNames(Arrays.asList(p1, p2, p3));

        assertEquals(Arrays.asList("B1", "B3"), names);
    }

    @Test
    @STTM("SNAP-4243")
    public void testGetCommonRasterDataNodeNames_ordersGroupedBandsFirst() {
        Product p1 = createProduct("p1", "A", "B", "C");
        Product p2 = createProduct("p2", "A", "B", "C");
        p1.setAutoGrouping(BandGroup.parse("C"));
        p2.setAutoGrouping(BandGroup.parse("C"));

        List<String> names = OpenCommonRasterDataAction.getCommonRasterDataNodeNames(Arrays.asList(p1, p2));

        assertEquals(Arrays.asList("C", "A", "B"), names);
    }

    @Test
    @STTM("SNAP-4243")
    public void testGetCommonRasterDataNodeNames_includesTiePointGrids() {
        Product p1 = createProduct("p1", "B1");
        Product p2 = createProduct("p2", "B2");
        p1.addTiePointGrid(new TiePointGrid("latitude", 2, 2, 0, 0, 1, 1, new float[4]));
        p2.addTiePointGrid(new TiePointGrid("latitude", 2, 2, 0, 0, 1, 1, new float[4]));

        List<String> names = OpenCommonRasterDataAction.getCommonRasterDataNodeNames(Arrays.asList(p1, p2));

        assertEquals(Arrays.asList("latitude"), names);
    }

    @Test
    @STTM("SNAP-4243")
    public void testGetRasterDataNodes_resolvesSelectedNamesForAllProducts() {
        Product p1 = createProduct("p1", "B1", "B2");
        Product p2 = createProduct("p2", "B1", "B2");

        List<RasterDataNode> nodes = OpenCommonRasterDataAction.getRasterDataNodes(
                Arrays.asList(p1, p2), Arrays.asList("B2")
        );

        assertEquals(2, nodes.size());
        assertEquals(p1.getBand("B2"), nodes.get(0));
        assertEquals(p2.getBand("B2"), nodes.get(1));
    }

    @Test
    @STTM("SNAP-4243")
    public void testGetRegexMatchingRasterNames() {
        List<String> names = OpenCommonRasterDataAction.getRegexMatchingRasterNames(
                Arrays.asList("Oa01_radiance", "Oa02_radiance", "latitude", "quality_flags"), "Oa.._radiance"
        );

        assertEquals(Arrays.asList("Oa01_radiance", "Oa02_radiance"), names);
    }

    @Test
    @STTM("SNAP-4243")
    public void testGetRegexMatchingRasterNames_usesContainsMatching() {
        List<String> names = OpenCommonRasterDataAction.getRegexMatchingRasterNames(
                Arrays.asList("Oa01_radiance", "Oa02_reflectance", "latitude"), "radiance"
        );

        assertEquals(Arrays.asList("Oa01_radiance"), names);
    }

    @Test(expected = PatternSyntaxException.class)
    @STTM("SNAP-4243")
    public void testGetRegexMatchingRasterNames_rejectsInvalidRegex() {
        OpenCommonRasterDataAction.getRegexMatchingRasterNames(Arrays.asList("B1"), "[");
    }

    private static Product createProduct(String name, String... bandNames) {
        Product product = new Product(name, "type", 2, 2);
        for (String bandName : bandNames) {
            product.addBand(bandName, "0");
        }
        return product;
    }
}
