package org.esa.snap.ui.product.metadata;

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;
import org.openide.nodes.AbstractNode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tonio Fincke
 */
public class MetadataTableInnerElement implements MetadataTableElement {

    private final MetadataElement metadataElement;
    private final MetadataTableElement[] metadataTableElements;


    public MetadataTableInnerElement(MetadataElement metadataElement) {
        this.metadataElement = metadataElement;
        metadataTableElements = getChildrenElementsFromElement(metadataElement);
    }

    @Override
    public MetadataTableElement[] getMetadataTableElements() {
        return metadataTableElements;
    }

    @Override
    public String getName() {
        return metadataElement.getName();
    }

    @Override
    public AbstractNode createNode() {
        return new MetadataElementInnerNode(this);
    }

    private static MetadataTableElement[] getChildrenElementsFromElement(MetadataElement metadataElement) {
        MetadataElement[] elements = metadataElement.getElements();
        MetadataAttribute[] attributes = metadataElement.getAttributes();
        List<MetadataTableElement> metadataTableElementList = new ArrayList<>();
        for (MetadataElement element : elements) {
            metadataTableElementList.add(new MetadataTableInnerElement(element));
        }
        for (MetadataAttribute attribute : attributes) {
            final long dataElemSize = attribute.getNumDataElems();
            if (dataElemSize > 1) {
                //todo refactor this
                final Object dataElems = attribute.getDataElems();
                final int dataType = attribute.getDataType();
                if (ProductData.isFloatingPointType(dataType)) {
                    if(dataElems instanceof float[]) {
                        addFloatMetadataAttributes(attribute, (float[]) dataElems, metadataTableElementList);
                    } else {
                        addDoubleMetadataAttributes(attribute, (double[]) dataElems, metadataTableElementList);
                    }
                } else if (ProductData.isIntType(dataType)) {
                    if(dataElems instanceof byte[]) {
                        addByteMetadataAttributes(attribute, (byte[]) dataElems, metadataTableElementList);
                    } else if(dataElems instanceof short[]) {
                        addShortMetadataAttributes(attribute, (short[]) dataElems, metadataTableElementList);
                    } else {
                        addIntMetadataAttributes(attribute, (int[]) dataElems, metadataTableElementList);
                    }
                } else {
                    metadataTableElementList.add(new MetadataTableLeaf(attribute));
                }
            } else {
                metadataTableElementList.add(new MetadataTableLeaf(attribute));
            }
        }
        return metadataTableElementList.toArray(new MetadataTableElement[metadataTableElementList.size()]);
    }

    private static void addFloatMetadataAttributes(MetadataAttribute attribute, float[] elems,
                                                   List<MetadataTableElement> metadataTableElementList) {
        final String name = attribute.getName();
        final String unit = attribute.getUnit();
        final int dataType = attribute.getDataType();
        final String description = attribute.getDescription();
        for (int j = 0; j < elems.length; j++) {
            final MetadataAttribute partAttribute =
                    new MetadataAttribute(name + "." + (j + 1), dataType);
            partAttribute.setDataElems(new float[]{elems[j]});
            partAttribute.setUnit(unit);
            partAttribute.setDescription(description);
            metadataTableElementList.add(new MetadataTableLeaf(partAttribute));
        }
    }

    private static void addDoubleMetadataAttributes(MetadataAttribute attribute, double[] elems,
                                                   List<MetadataTableElement> metadataTableElementList) {
        final String name = attribute.getName();
        final String unit = attribute.getUnit();
        final int dataType = attribute.getDataType();
        final String description = attribute.getDescription();
        for (int j = 0; j < elems.length; j++) {
            final MetadataAttribute partAttribute =
                    new MetadataAttribute(name + "." + (j + 1), dataType);
            partAttribute.setDataElems(new double[]{elems[j]});
            partAttribute.setUnit(unit);
            partAttribute.setDescription(description);
            metadataTableElementList.add(new MetadataTableLeaf(partAttribute));
        }
    }

    private static void addByteMetadataAttributes(MetadataAttribute attribute, byte[] elems,
                                                  List<MetadataTableElement> metadataTableElementList) {
        final String name = attribute.getName();
        final int dataType = attribute.getDataType();
        final String unit = attribute.getUnit();
        final String description = attribute.getDescription();
        for (int j = 0; j < elems.length; j++) {
            final MetadataAttribute partAttribute =
                    new MetadataAttribute(name + "." + (j + 1), dataType);
            partAttribute.setDataElems(new byte[]{elems[j]});
            partAttribute.setUnit(unit);
            partAttribute.setDescription(description);
            metadataTableElementList.add(new MetadataTableLeaf(partAttribute));
        }
    }

    private static void addShortMetadataAttributes(MetadataAttribute attribute, short[] elems,
                                                  List<MetadataTableElement> metadataTableElementList) {
        final String name = attribute.getName();
        final int dataType = attribute.getDataType();
        final String unit = attribute.getUnit();
        final String description = attribute.getDescription();
        for (int j = 0; j < elems.length; j++) {
            final MetadataAttribute partAttribute =
                    new MetadataAttribute(name + "." + (j + 1), dataType);
            partAttribute.setDataElems(new short[]{elems[j]});
            partAttribute.setUnit(unit);
            partAttribute.setDescription(description);
            metadataTableElementList.add(new MetadataTableLeaf(partAttribute));
        }
    }

    private static void addIntMetadataAttributes(MetadataAttribute attribute, int[] elems,
                                                 List<MetadataTableElement> metadataTableElementList) {
        final String name = attribute.getName();
        final int dataType = attribute.getDataType();
        final String unit = attribute.getUnit();
        final String description = attribute.getDescription();
        for (int j = 0; j < elems.length; j++) {
            final MetadataAttribute partAttribute =
                    new MetadataAttribute(name + "." + (j + 1), dataType);
            partAttribute.setDataElems(new int[]{elems[j]});
            partAttribute.setUnit(unit);
            partAttribute.setDescription(description);
            metadataTableElementList.add(new MetadataTableLeaf(partAttribute));
        }
    }

}
