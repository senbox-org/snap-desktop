package org.esa.snap.worldwind.layers;

import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.RenderableLayer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DataFileLayersTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private File write(final String name, final String content) throws IOException {
        final File file = folder.newFile(name);
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    @Test
    public void testTypeOf() {
        assertEquals(DataFileLayers.Type.KML, DataFileLayers.typeOf(new File("a.kml")));
        assertEquals(DataFileLayers.Type.KML, DataFileLayers.typeOf(new File("a.KMZ")));
        assertEquals(DataFileLayers.Type.SHAPEFILE, DataFileLayers.typeOf(new File("dir/a.shp")));
        assertEquals(DataFileLayers.Type.GEOJSON, DataFileLayers.typeOf(new File("a.geojson")));
        assertEquals(DataFileLayers.Type.GEOJSON, DataFileLayers.typeOf(new File("a.json")));
        assertNull(DataFileLayers.typeOf(new File("a.dbf")));
        assertNull(DataFileLayers.typeOf(new File("kml")));
    }

    @Test
    public void testKml() throws IOException {
        final File file = write("aoi.kml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<kml xmlns=\"http://www.opengis.net/kml/2.2\"><Document><Placemark><name>AOI</name>" +
                "<Polygon><outerBoundaryIs><LinearRing><coordinates>10,45 11,45 11,46 10,46 10,45</coordinates>" +
                "</LinearRing></outerBoundaryIs></Polygon></Placemark></Document></kml>");
        final Layer layer = DataFileLayers.load(file);
        assertEquals("aoi.kml", layer.getName());
        assertEquals(1, ((RenderableLayer) layer).getNumRenderables());
    }

    @Test
    public void testGeoJson() throws IOException {
        final File file = write("points.geojson", "{\"type\":\"FeatureCollection\",\"features\":[" +
                "{\"type\":\"Feature\",\"properties\":{\"name\":\"a\"},\"geometry\":{\"type\":\"Point\",\"coordinates\":[10,45]}}," +
                "{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"LineString\",\"coordinates\":[[10,45],[11,46]]}}]}");
        final Layer layer = DataFileLayers.load(file);
        assertEquals("points.geojson", layer.getName());
        assertTrue(((RenderableLayer) layer).getNumRenderables() >= 2);
    }

    @Test
    public void testShapefile() throws IOException {
        // the bundled Natural Earth borders, copied to a folder
        File shp = null;
        for (String extension : new String[]{".shp", ".shx", ".dbf", ".prj"}) {
            final File target = new File(folder.getRoot(), "borders" + extension);
            try (InputStream in = getClass().getResourceAsStream(NaturalEarthLayers.BORDERS_RESOURCE + extension)) {
                Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            if (extension.equals(".shp")) {
                shp = target;
            }
        }
        final Layer layer = DataFileLayers.load(shp);
        assertEquals("borders.shp", layer.getName());
        assertTrue(((RenderableLayer) layer).getNumRenderables() > 0);
    }

    @Test
    public void testErrors() throws IOException {
        expectIOException(new File(folder.getRoot(), "missing.kml"));
        expectIOException(write("table.csv", "a,b"));
        expectIOException(write("broken.kml", "<kml><Document><Placemark>"));
        expectIOException(write("broken.shp", "not a shapefile"));
        expectIOException(write("broken.geojson", "{\"type\": \"FeatureCollection\", \"features\": [ {"));
        expectIOException(write("other.json", "{\"some\": \"settings\"}"));
    }

    private static void expectIOException(final File file) {
        try {
            DataFileLayers.load(file);
            fail("IOException expected for " + file.getName());
        } catch (IOException expected) {
            // expected
        }
    }
}
