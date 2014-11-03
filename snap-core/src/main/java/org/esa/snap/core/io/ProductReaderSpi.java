/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.snap.core.io;

import java.util.List;
import java.util.Map;

/**
 * @author Norman
 */
public interface ProductReaderSpi {
    boolean canRead(Object input);

    Map<String, Object> getDefaultParameters(Object input);

    ProductReader createProductReader(Object input, Map<String, Object> parameters);

    String getDescription();

    List<String> getFileExtensions();
}
