package org.esa.snap.rcp.spectrallibrary.util;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.speclib.model.AttributeValue;
import org.esa.snap.speclib.model.SpectralProfile;
import org.esa.snap.speclib.model.SpectralSignature;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.junit.Test;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.util.Iterator;

import static org.junit.Assert.*;


public class WktUtilsTest {


    @Test
    @STTM("SNAP-4172")
    public void test_ExtractWktReturnsNullForNullProfile() {
        assertNull(WktUtils.extractWkt(null));
    }

    @Test
    @STTM("SNAP-4172")
    public void test_ExtractWktReturnsNullWhenWktAttributeMissing() {
        SpectralProfile profile = createProfile("Profile_1");

        assertNull(WktUtils.extractWkt(profile));
    }

    @Test
    @STTM("SNAP-4172")
    public void test_ExtractWktReturnsNullForBlankWkt() {
        SpectralProfile profile = createProfile("Profile_1")
                .withAttribute("wkt", AttributeValue.ofString("   "));

        assertNull(WktUtils.extractWkt(profile));
    }

    @Test
    @STTM("SNAP-4172")
    public void test_ExtractWktReturnsTrimmedWkt() {
        SpectralProfile profile = createProfile("Profile_1")
                .withAttribute("wkt", AttributeValue.ofString("  POINT (47.982999 -22.828160999999998)  "));

        assertEquals("POINT (47.982999 -22.828160999999998)", WktUtils.extractWkt(profile));
    }

    @Test
    @STTM("SNAP-4172")
    public void test_AddProfileGeometryToCollectionReturnsFalseForNullCollection() {
        SpectralProfile profile = createProfile("Profile_1")
                .withAttribute("wkt", AttributeValue.ofString("POINT (1 2)"));

        DefaultFeatureCollection collection = WktUtils.createEmptyFeaturecollection("test_layer");
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(collection.getSchema());
        WKTReader reader = new WKTReader();

        boolean added = WktUtils.addProfileGeometryToCollection(null, profile, "test_layer", 0, reader, featureBuilder);

        assertFalse(added);
        assertEquals(0, collection.size());
    }

    @Test
    @STTM("SNAP-4172")
    public void test_AddProfileGeometryToCollectionReturnsFalseWhenProfileHasNoWkt() {
        SpectralProfile profile = createProfile("Profile_1");

        DefaultFeatureCollection collection = WktUtils.createEmptyFeaturecollection("test_layer");
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(collection.getSchema());
        WKTReader reader = new WKTReader();

        boolean added = WktUtils.addProfileGeometryToCollection(collection, profile, "test_layer", 0, reader, featureBuilder);

        assertFalse(added);
        assertEquals(0, collection.size());
    }

    @Test
    @STTM("SNAP-4172")
    public void test_AddProfileGeometryToCollectionReturnsFalseForInvalidWkt() {
        SpectralProfile profile = createProfile("Profile_1")
                .withAttribute("wkt", AttributeValue.ofString("NOT_A_WKT"));

        DefaultFeatureCollection collection = WktUtils.createEmptyFeaturecollection("test_layer");
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(collection.getSchema());
        WKTReader reader = new WKTReader();

        boolean added = WktUtils.addProfileGeometryToCollection(collection, profile, "test_layer", 0, reader, featureBuilder);

        assertFalse(added);
        assertEquals(0, collection.size());
    }

    @Test
    @STTM("SNAP-4172")
    public void test_AddProfileGeometryToCollectionAddsFeatureForValidWkt() {
        SpectralProfile profile = createProfile("Profile_1")
                .withAttribute("wkt", AttributeValue.ofString("POINT (47.982999 -22.828160999999998)"));

        DefaultFeatureCollection collection = WktUtils.createEmptyFeaturecollection("test_layer");
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(collection.getSchema());
        WKTReader reader = new WKTReader();

        boolean added = WktUtils.addProfileGeometryToCollection(collection, profile, "test_layer", 0, reader, featureBuilder);

        assertTrue(added);
        assertEquals(1, collection.size());

        Iterator<?> iterator = collection.iterator();
        assertTrue(iterator.hasNext());

        SimpleFeature feature = (SimpleFeature) iterator.next();
        assertEquals("test_layer_0", feature.getID());
        assertEquals("Profile_1", feature.getAttribute("profile_name"));
        assertEquals(profile.getId().toString(), feature.getAttribute("profile_id"));
        assertEquals("POINT (47.982999 -22.828160999999998)", feature.getDefaultGeometry().toString());
    }

    @Test
    @STTM("SNAP-4172")
    public void test_CreateEmptyFeaturecollectionCreatesExpectedSchema() {
        DefaultFeatureCollection collection = WktUtils.createEmptyFeaturecollection("my_layer");
        SimpleFeatureType schema = collection.getSchema();

        assertNotNull(collection);
        assertEquals(0, collection.size());
        assertEquals("my_layer", schema.getTypeName());
        assertNotNull(schema.getGeometryDescriptor());
        assertEquals("the_geom", schema.getGeometryDescriptor().getLocalName());
        assertNotNull(schema.getDescriptor("profile_name"));
        assertNotNull(schema.getDescriptor("profile_id"));
    }

    private static SpectralProfile createProfile(String name) {
        return SpectralProfile.create(name, SpectralSignature.of(new double[]{1.0}));
    }
}