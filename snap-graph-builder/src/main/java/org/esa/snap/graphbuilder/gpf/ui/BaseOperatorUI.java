/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.graphbuilder.gpf.ui;

import com.bc.ceres.binding.*;
import com.bc.ceres.binding.dom.DomConverter;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.binding.dom.XppDomElement;
import com.bc.ceres.swing.selection.SelectionChangeListener;
import com.thoughtworks.xstream.io.xml.xppdom.XppDom;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.ParameterDescriptorFactory;
import org.esa.snap.core.gpf.descriptor.OperatorDescriptor;
import org.esa.snap.core.gpf.descriptor.PropertySetDescriptorFactory;
import org.esa.snap.core.gpf.graph.GraphException;
import org.esa.snap.ui.AppContext;

import javax.swing.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * The abstract base class for all operator user interfaces intended to be extended by clients.
 * The following methods are intended to be implemented or overidden:
 * CreateOpTab() must be implemented in order to create the operator user interface component
 * User: lveci
 * Date: Feb 12, 2008
 */
public abstract class BaseOperatorUI implements OperatorUI {

    protected PropertySet propertySet = null;
    protected Map<String, Object> paramMap = null;
    protected Product[] sourceProducts = null;
    protected String operatorName = "";
    private String errorMessage = null;

    @Override
    public boolean hasError() {
        return errorMessage != null;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    private static Converter getItemConverter(final Object obj) {
        return ConverterRegistry.getInstance().getConverter(obj.getClass());
    }

    private static Converter getItemConverter(final PropertyDescriptor descriptor) {
        final Class<?> itemType = descriptor.getType().getComponentType();
        Converter itemConverter = descriptor.getConverter();
        if (itemConverter == null) {
            itemConverter = ConverterRegistry.getInstance().getConverter(itemType);
        }
        return itemConverter;
    }

    private static String getElementName(final Property p) {
        final String alias = p.getDescriptor().getAlias();
        if (alias != null && !alias.isEmpty()) {
            return alias;
        }
        return p.getDescriptor().getName();
    }

    public abstract JComponent CreateOpTab(final String operatorName,
                                           final Map<String, Object> parameterMap, final AppContext appContext);

    public abstract void initParameters();

    public abstract UIValidation validateParameters();

    public abstract void updateParameters();

    public String getOperatorName() {
        return operatorName;
    }

    @Override
    public void addSelectionChangeListener(SelectionChangeListener listener) {
        // default implementation does nothing tb 2025-06-06
    }

    protected void initializeOperatorUI(final String operatorName, final Map<String, Object> parameterMap) {
        this.operatorName = operatorName;
        this.paramMap = parameterMap;

        final OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(operatorName);
        if (operatorSpi == null) {
            throw new IllegalArgumentException("operator " + operatorName + " not found");
        }

        final ParameterDescriptorFactory descriptorFactory = new ParameterDescriptorFactory();
        final OperatorDescriptor operatorDescriptor = operatorSpi.getOperatorDescriptor();
        final PropertySetDescriptor propertySetDescriptor;
        try {
            propertySetDescriptor = PropertySetDescriptorFactory.createForOperator(operatorDescriptor, descriptorFactory.getSourceProductMap());
        } catch (ConversionException e) {
            throw new IllegalStateException("Not able to init OperatorParameterSupport.", e);
        }
        propertySet = PropertyContainer.createMapBacked(paramMap, propertySetDescriptor);

        if (paramMap.isEmpty()) {
            try {
                propertySet.setDefaultValues();
            } catch (IllegalStateException e) {
                //throw new RuntimeException(e); do not throw or the UI will not be created
                errorMessage = e.getMessage();
            }
        } else {
            // SNAP-3794 Handle possible parameter aliases from the incoming map
            final Property[] properties = this.propertySet.getProperties();
            for (Property property : properties) {
                final PropertyDescriptor descriptor = property.getDescriptor();
                final String alias = descriptor.getAlias();
                if (alias != null && paramMap.containsKey(alias)) {
                    try {
                        property.setValue(paramMap.get(alias));
                    } catch (ValidationException e) {
                        //throw new RuntimeException(e); do not throw or the UI will not be created
                        errorMessage = e.getMessage();
                    }
                }
            }
        }
    }

    public void setSourceProducts(final Product[] products) {
        if (sourceProducts == null || !Arrays.equals(sourceProducts, products)) {
            sourceProducts = products;
            if (paramMap != null) {
                initParameters();
            }
        }
    }

    public boolean hasSourceProducts() {
        return sourceProducts != null;
    }

    public void convertToDOM(final XppDomElement parentElement) throws GraphException {

        if (propertySet == null) {
            setParamsToConfiguration(parentElement.getXppDom());
            return;
        }

        final Property[] properties = propertySet.getProperties();
        for (Property p : properties) {
            final PropertyDescriptor descriptor = p.getDescriptor();
            final DomConverter domConverter = descriptor.getDomConverter();
            if (domConverter != null) {
                try {
                    final DomElement childElement = parentElement.createChild(getElementName(p));
                    domConverter.convertValueToDom(p.getValue(), childElement);
                } catch (ConversionException e) {
                    e.printStackTrace();
                }
            } else {

                final String itemAlias = descriptor.getItemAlias();
                if (descriptor.getType().isArray() && itemAlias != null && !itemAlias.isEmpty()) {
                    final DomElement childElement = descriptor.getBooleanProperty("itemsInlined") ? parentElement : parentElement.createChild(getElementName(p));
                    final Object array = p.getValue();
                    final Converter itemConverter = getItemConverter(descriptor);
                    if (array != null && itemConverter != null) {
                        final int arrayLength = Array.getLength(array);
                        for (int i = 0; i < arrayLength; i++) {
                            final Object component = Array.get(array, i);
                            final DomElement itemElement = childElement.createChild(itemAlias);

                            final String text = itemConverter.format(component);
                            if (text != null && !text.isEmpty()) {
                                itemElement.setValue(text);
                            }
                        }
                    }
                } else {
                    final DomElement childElement = parentElement.createChild(getElementName(p));
                    final Object childValue = p.getValue();
                    final Converter converter = descriptor.getConverter();
                    if (converter == null) {
                        throw new GraphException(operatorName + " BaseOperatorUI: no converter found for parameter " + descriptor.getName());
                    }

                    String text = converter.format(childValue);
                    if (text != null && !text.isEmpty()) {
                        childElement.setValue(text);
                    }
                }
            }
        }
    }

    /**
     * Method used for test purposes only
     * @return  The current property set
     */
    public PropertySet getPropertySet() {
        return this.propertySet;
    }

    /**
     * The method check if there are at least one multi-size source product
     *
     * @return false if there is not multi-size source product
     */
    protected boolean hasMultiSizeProducts() {
        if (sourceProducts != null) {
            for (Product prod : sourceProducts) {
                if (prod.isMultiSize())
                    return true;
            }
        }
        return false;
    }

    protected String[] getBandNames() {
        final ArrayList<String> bandNames = new ArrayList<>(5);
        if (sourceProducts != null) {
            for (Product prod : sourceProducts) {
                if (sourceProducts.length > 1) {
                    for (String name : prod.getBandNames()) {
                        bandNames.add(name + "::" + prod.getName());
                    }
                } else {
                    bandNames.addAll(Arrays.asList(prod.getBandNames()));
                }
            }
        }
        return bandNames.toArray(new String[0]);
    }

    protected String[] getTiePointGridNames(){
        final ArrayList<String> tiePointGridNames = new ArrayList<>(3);
        if (sourceProducts != null) {
            for (Product prod : sourceProducts) {
                if (sourceProducts.length > 1) {
                    for (String name : prod.getTiePointGridNames()) {
                        tiePointGridNames.add(name + "::" + prod.getName());
                    }
                } else {
                    tiePointGridNames.addAll(Arrays.asList(prod.getTiePointGridNames()));
                }
            }
        }
        return tiePointGridNames.toArray(new String[tiePointGridNames.size()]);
    }

    protected String[] getGeometries() {
        final ArrayList<String> geometryNames = new ArrayList<>(5);
        if (sourceProducts != null) {
            for (Product prod : sourceProducts) {
                if (sourceProducts.length > 1) {
                    for (String name : prod.getMaskGroup().getNodeNames()) {
                        geometryNames.add(name + "::" + prod.getName());
                    }
                } else {
                    geometryNames.addAll(Arrays.asList(prod.getMaskGroup().getNodeNames()));
                }
            }
        }
        return geometryNames.toArray(new String[0]);
    }

    private void setParamsToConfiguration(final XppDom config) {
        if (paramMap == null) return;
        final Set<String> keys = paramMap.keySet();                     // The set of keys in the map.
        for (String key : keys) {
            final Object value = paramMap.get(key);             // Get the value for that key.
            if (value == null) continue;

            XppDom xml = config.getChild(key);
            if (xml == null) {
                xml = new XppDom(key);
                config.addChild(xml);
            }

            Converter itemConverter = getItemConverter(value);
            if (itemConverter != null) {
                final String text = itemConverter.format(value);
                if (text != null && !text.isEmpty()) {
                    xml.setValue(text);
                }
            } else {
                xml.setValue(value.toString());
            }
        }
    }

    @Override
    public Map<String, Object> getParameters() {
        return this.paramMap;
    }
}
