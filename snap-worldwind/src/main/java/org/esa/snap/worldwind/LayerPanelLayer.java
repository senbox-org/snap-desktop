/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.worldwind;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.render.ScreenAnnotation;
import gov.nasa.worldwindx.examples.util.LayerManagerLayer;

import java.awt.*;

/**
 * Displays the layer list in a viewport corner.
 */
public class LayerPanelLayer extends LayerManagerLayer {
    private static final String[] HIDDEN_LAYERS = {
            "Atmosphere", "World Map", "Compass", "Stars", "NASA Blue Marble Image", "Measure Tool"
    };

    public LayerPanelLayer(WorldWindow wwd) {
        super(wwd);
    }

    private static boolean isHidden(final Layer layer) {
        final String name = layer.getName();
        if (name == null) {
            return false;
        }
        for (String hidden : HIDDEN_LAYERS) {
            if (name.equalsIgnoreCase(hidden)) {
                return true;
            }
        }
        return false;
    }

    private LayerList getValidLayers() {
        final LayerList validLayers = new LayerList();
        final LayerList allLayers = wwd.getModel().getLayers();
        for (Layer l : allLayers) {
            if (!isHidden(l)) {
                validLayers.add(l);
            }
        }
        return validLayers;
    }

    /**
     * <code>SelectListener</code> implementation.
     *
     * @param event the current <code>SelectEvent</code>
     */
    @Override
    public void selected(SelectEvent event) {
        //System.out.println("event.getEventAction(): " + event.getEventAction());

        final ScreenAnnotation annotation = getAnnotation();
        if (event.hasObjects() && event.getTopObject() == annotation) {
            boolean update = false;
            if (event.getEventAction().equals(SelectEvent.ROLLOVER)
                    || event.getEventAction().equals(SelectEvent.LEFT_CLICK)) {
                // Highlight annotation
                if (!annotation.getAttributes().isHighlighted()) {
                    annotation.getAttributes().setHighlighted(true);
                    update = true;
                }
                // Check for text or url
                final PickedObject po = event.getTopPickedObject();
                if (po.getValue(AVKey.URL) != null) {
                    // Set cursor hand on hyperlinks
                    ((Component) this.wwd).setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    int i = Integer.parseInt((String) po.getValue(AVKey.URL));
                    // Select current hyperlink
                    if (getSelectedIndex() != i) {
                        setSelectedIndex(i);
                        update = true;
                    }
                    // Enable/disable layer on left click
                    if (event.getEventAction().equals(SelectEvent.LEFT_CLICK)) {
                        final LayerList layers = getValidLayers();
                        if (i >= 0 && i < layers.size()) {
                            final Layer layer = layers.get(i);
                            final boolean enable = !layer.isEnabled();
                            layer.setEnabled(enable);
                            update = true;
                        }
                    }
                } else {
                    // Unselect if not on an hyperlink
                    if (getSelectedIndex() != -1) {
                        setSelectedIndex(-1);
                        update = true;
                    }
                    // Set cursor
                    if (this.isComponentDragEnabled())
                        ((Component) this.wwd).setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    else
                        ((Component) this.wwd).setCursor(Cursor.getDefaultCursor());
                }
            }
            if (event.getEventAction().equals(SelectEvent.DRAG)
                    || event.getEventAction().equals(SelectEvent.DRAG_END)) {
                // Handle dragging
                if (this.isComponentDragEnabled() || this.isLayerDragEnabled()) {
                    final boolean wasDraggingLayer = this.draggingLayer;
                    this.drag(event);
                    // Update list if dragging a layer, otherwise just redraw the world window
                    if (this.draggingLayer || wasDraggingLayer)
                        update = true;
                    else
                        this.wwd.redraw();
                }
            }
            // Redraw annotation if needed
            if (update)
                this.update();
        } else if (event.getEventAction().equals(SelectEvent.ROLLOVER) && annotation.getAttributes().isHighlighted()) {
            // de-highlight annotation
            annotation.getAttributes().setHighlighted(false);
            ((Component) this.wwd).setCursor(Cursor.getDefaultCursor());
            this.update();
        }
    }

    @Override
    protected void drag(SelectEvent event) {
        if (event.getEventAction().equals(SelectEvent.DRAG)) {
            if ((this.isComponentDragEnabled() && getSelectedIndex() == -1 && this.dragRefIndex == -1)
                    || this.draggingComponent) {
                // Dragging the whole list
                if (!this.draggingComponent) {
                    this.dragRefCursorPoint = event.getMouseEvent().getPoint();
                    this.dragRefPoint = getAnnotation().getScreenPoint();
                    this.draggingComponent = true;
                }
                final Point cursorOffset = new Point(event.getMouseEvent().getPoint().x - this.dragRefCursorPoint.x,
                                                     event.getMouseEvent().getPoint().y - this.dragRefCursorPoint.y);
                final Point targetPoint = new Point(this.dragRefPoint.x + cursorOffset.x,
                                                    this.dragRefPoint.y - cursorOffset.y);
                this.moveTo(targetPoint);
            } else if (this.isLayerDragEnabled()) {
                // Dragging a layer inside the list
                if (!this.draggingLayer) {
                    this.dragRefIndex = getSelectedIndex();
                    this.draggingLayer = true;
                }
                if (getSelectedIndex() != -1 && this.dragRefIndex != -1 && this.dragRefIndex != getSelectedIndex()) {
                    // Move dragged layer in the model; the displayed list is a filtered copy of it
                    final LayerList validLayers = getValidLayers();
                    if (this.dragRefIndex < validLayers.size() && getSelectedIndex() < validLayers.size()) {
                        final LayerList modelLayers = this.wwd.getModel().getLayers();
                        final Layer dragged = validLayers.get(this.dragRefIndex);
                        final Layer target = validLayers.get(getSelectedIndex());
                        modelLayers.remove(dragged);
                        final int targetIndex = modelLayers.indexOf(target);
                        modelLayers.add(this.dragRefIndex > getSelectedIndex() ? targetIndex : targetIndex + 1, dragged);
                    }
                    this.dragRefIndex = getSelectedIndex();
                }
            }
        } else if (event.getEventAction().equals(SelectEvent.DRAG_END)) {
            this.draggingComponent = false;
            this.draggingLayer = false;
            this.dragRefIndex = -1;
        }
    }

    /**
     * Compose the annotation text from the given <code>LayerList</code>.
     *
     * @param layers the <code>LayerList</code> to draw names from.
     * @return the annotation text to be displayed.
     */
    @Override
    protected String makeAnnotationText(LayerList layers) {
        // Compose html text
        final StringBuilder text = new StringBuilder(255);
        Color color;
        int i = 0;
        final LayerList validLayers = getValidLayers();
        for (Layer layer : validLayers) {
            if (!this.isMinimized() || layer == this) {
                color = (i == getSelectedIndex()) ? getHighlightColor() : getColor();
                color = (i == this.dragRefIndex) ? dragColor : color;
                text.append("<a href=\"");
                text.append(i);
                text.append("\"><font color=\"");
                text.append(encodeHTMLColor(color));
                text.append("\">");
                text.append((layer.isEnabled() ? getLayerEnabledSymbol() : getLayerDisabledSymbol()));
                text.append(' ');
                text.append((layer.isEnabled() ? "<b>" : "<i>"));
                text.append(layer.getName());
                text.append((layer.isEnabled() ? "</b>" : "</i>"));
                text.append((layer.isMultiResolution() && layer.isAtMaxResolution() ? "*" : ""));
                text.append("</a><br />");
            }
            i++;
        }
        return text.toString();
    }
}
