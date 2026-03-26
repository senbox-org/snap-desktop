package org.esa.snap.rcp.spectrallibrary.util;

import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.speclib.model.AttributeValue;
import org.esa.snap.speclib.model.SpectralProfile;
import org.geotools.feature.DefaultFeatureCollection;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.simple.SimpleFeatureType;

import java.util.Map;


public class WktUtils {


    public static String extractWkt(SpectralProfile profile) {
        if (profile == null) {
            return null;
        }

        Map<String, AttributeValue> attributes = profile.getAttributes();
        if (attributes == null) {
            return null;
        }

        AttributeValue wktValue = attributes.get("wkt");
        if (wktValue == null || wktValue.asString() == null) {
            return null;
        }

        String wkt = String.valueOf(wktValue.asString()).trim();
        if (!wkt.isEmpty()) {
            return wkt;
        }
        return null;
    }


    public static boolean addProfileGeometryToCollection(DefaultFeatureCollection collection,
                                                         SpectralProfile profile,
                                                         String layerName,
                                                         int featureIndex,
                                                         WKTReader reader,
                                                         SimpleFeatureBuilder featureBuilder) {
        if (collection == null || profile == null || layerName == null || reader == null || featureBuilder == null) {
            return false;
        }

        String wkt = extractWkt(profile);
        if (wkt == null || wkt.isBlank() || profile.getId() == null) {
            return false;
        }

        try {
            Geometry geometry = reader.read(wkt.trim());

            featureBuilder.reset();
            featureBuilder.set("the_geom", geometry);
            featureBuilder.set("profile_name", SpectralLibraryUtils.nameOf(profile));
            featureBuilder.set("profile_id", profile.getId().toString());

            collection.add(featureBuilder.buildFeature(layerName + "_" + featureIndex));
            return true;
        } catch (ParseException e) {
            return false;
        }
    }


    public static DefaultFeatureCollection createEmptyFeaturecollection(String layerName) {
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName(layerName);
        typeBuilder.add("the_geom", Geometry.class);
        typeBuilder.setDefaultGeometry("the_geom");
        typeBuilder.add("profile_name", String.class);
        typeBuilder.add("profile_id", String.class);

        SimpleFeatureType featureType = typeBuilder.buildFeatureType();
        VectorDataNode vectorDataNode = new VectorDataNode(layerName, featureType);
        return vectorDataNode.getFeatureCollection();
    }
}
