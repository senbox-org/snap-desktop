/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.snap.core.io.rgb;

import org.esa.snap.core.Band;
import org.esa.snap.core.Product;
import org.esa.snap.core.io.ProductReader;
import org.esa.snap.core.io.ProductReaderSpi;

import javax.imageio.ImageIO;
import javax.media.jai.operator.BandMergeDescriptor;
import javax.media.jai.operator.BandSelectDescriptor;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Norman
 */
//@ServiceProvider(service = ProductReaderSpi.class, path = "Snap/ProductReaders")
public class RgbProductReaderSpi implements ProductReaderSpi {

    @Override
    public String getDescription() {
        return "Image Files";
    }

    @Override
    public List<String> getFileExtensions() {
        return Arrays.asList("jpg", "jpeg", "png", "gif", "tif", "tiff");
    }

    @Override
    public boolean canRead(Object input) {
        if (input instanceof File) {
            File file = (File) input;
            String name = file.getName().toLowerCase();
            if (name.endsWith(".jpg") || name.endsWith(".png")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Map<String, Object> getDefaultParameters(Object input) {
        return new HashMap<>();
    }

    @Override
    public ProductReader createProductReader(final Object input, Map<String, Object> parameters) {
        return new ProductReader(input, parameters) {

            @Override
            protected Product readProductImpl() throws IOException {
                Product product = new Product(input.toString());
                BufferedImage image = ImageIO.read((File) input);
                int n = image.getSampleModel().getNumBands();
                for (int i = 0; i < n; i++) {
                    RenderedImage data = BandSelectDescriptor.create(image, new int[]{i}, null);

                    data = BandMergeDescriptor.create(data, data, null);
                    data = BandMergeDescriptor.create(data, data, null);

                    product.addBand(new Band("band_" + i, data));
                }
                return product;
            }
            
        };
    }
    
}
