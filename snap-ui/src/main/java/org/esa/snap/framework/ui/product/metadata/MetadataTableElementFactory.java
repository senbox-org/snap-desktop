package org.esa.snap.framework.ui.product.metadata;

import org.esa.snap.framework.datamodel.MetadataAttribute;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.ProductData;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tonio Fincke
 */
public class MetadataTableElementFactory {

    public static MetadataTableElement[] getChildrenElementsFromElement(MetadataElement metadataElement) {
        MetadataElement[] elements = metadataElement.getElements();
        MetadataAttribute[] attributes = metadataElement.getAttributes();
        List<MetadataTableElement> metadataTableElementList = new ArrayList<>();
        for (MetadataElement element : elements) {
            metadataTableElementList.add(new MetadataElementWrapper(element));
        }
        for (MetadataAttribute attribute : attributes) {
            final long dataElemSize = attribute.getNumDataElems();
            if (dataElemSize > 1) {
                final Object dataElems = attribute.getDataElems();
                final int dataType = attribute.getDataType();
                if (ProductData.isFloatingPointType(dataType)) {
                    addFloatMetadataAttributes(attribute, (float[]) dataElems, metadataTableElementList);
                } else if (ProductData.isIntType(dataType)) {
                    if(dataElems instanceof byte[]) {
                        addByteMetadataAttributes(attribute, (byte[]) dataElems, metadataTableElementList);
                    } else {
                        addIntMetadataAttributes(attribute, (int[]) dataElems, metadataTableElementList);
                    }
                } else {
                    metadataTableElementList.add(new MetadataAttributeWrapper(attribute));
                }
            } else {
                metadataTableElementList.add(new MetadataAttributeWrapper(attribute));
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
            metadataTableElementList.add(new MetadataAttributeWrapper(partAttribute));
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
            metadataTableElementList.add(new MetadataAttributeWrapper(partAttribute));
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
            metadataTableElementList.add(new MetadataAttributeWrapper(partAttribute));
        }
    }

}
