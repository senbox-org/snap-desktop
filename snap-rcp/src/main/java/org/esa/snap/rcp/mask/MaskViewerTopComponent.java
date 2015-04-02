/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.rcp.mask;

import org.esa.snap.rcp.windows.ToolTopComponent;

import javax.swing.event.ListSelectionListener;

public class MaskViewerTopComponent extends MaskToolTopComponent {

    public static final String ID = MaskViewerTopComponent.class.getName();

    @Override
    protected MaskForm createMaskForm(ToolTopComponent topComponent, ListSelectionListener selectionListener) {
        return new MaskViewerForm(selectionListener);
    }

    @Override
    protected String getTitle() {
        return "Mask Viewer";
    }
}