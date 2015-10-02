package org.esa.snap.graphbuilder.rcp.utils;

import javax.swing.ImageIcon;

/**
 * Created by luis on 08/02/2015.
 */
public class IconUtils {
    public static ImageIcon esaIcon = LoadIcon("org/esa/snap/graphbuilder/icons/esa.png");
    public static ImageIcon rstbIcon = LoadIcon("org/esa/snap/graphbuilder/icons/csa.png");
    public static ImageIcon arrayIcon = LoadIcon("org/esa/snap/graphbuilder/icons/array_logo.png");
    public static ImageIcon esaPlanetIcon = LoadIcon("org/esa/snap/graphbuilder/icons/esa-planet.png");
    public static ImageIcon geoAusIcon = LoadIcon("org/esa/snap/graphbuilder/icons/geo_aus.png");

    public static ImageIcon LoadIcon(final String path) {
        final java.net.URL imageURL = IconUtils.class.getClassLoader().getResource(path);
        if (imageURL == null) return null;
        return new ImageIcon(imageURL);
    }
}
