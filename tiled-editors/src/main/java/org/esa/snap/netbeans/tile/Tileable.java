/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.netbeans.tile;

/**
 * Something that can be arranged in tiles. Looked-up from global action context by
 * {@link TileAction}s such as {@link TileHorizontallyAction}, {@link TileVerticallyAction}, {@link TileEvenlyAction},
 * {@link TileSingleAction}.
 *
 * @author Norman Fomferra
 * @since 1.0
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
