package org.esa.snap.gui.util;

/**
 * Things that can be arranged in tiles.
 *
 * @author Norman Fomferra
 */
public interface Tileable {
    boolean canTile();

    void tileEvenly();

    void tileHorizontally();

    void tileVertically();

    void tileSingle();

    static Tileable getDefault() {
        return new TileableImpl();
    }
}
