package org.esa.snap.ui.product.metadata;

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;
import org.openide.nodes.AbstractNode;

import java.util.ArrayList;
import java.util.List;

import static org.esa.snap.core.datamodel.ProductData.*;

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
                final int dataType = attribute.getDataType();
                ProductData data = attribute.getData();
                if ((ProductData.isFloatingPointType(dataType) || ProductData.isIntType(dataType)) && !(data instanceof ProductData.UTC)) {
                    addMetadataAttributes(attribute, data, metadataTableElementList);
                } else {
                    metadataTableElementList.add(new MetadataTableLeaf(attribute));
                }
            } else {
                metadataTableElementList.add(new MetadataTableLeaf(attribute));
            }
        }
        return metadataTableElementList.toArray(new MetadataTableElement[metadataTableElementList.size()]);
    }

    private static void addMetadataAttributes(MetadataAttribute attribute, ProductData data,
                                                   List<MetadataTableElement> metadataTableElementList) {
        final String name = attribute.getName();
        final int dataType = attribute.getDataType();
        final String unit = attribute.getUnit();
        final String description = attribute.getDescription();
        for (int j = 0; j < data.getNumElems(); j++) {
            final MetadataAttribute partAttribute = new MetadataAttribute(name + "." + (j + 1), dataType);
            try {
                partAttribute.setDataElems(getDataElemArray(data, j));
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            partAttribute.setUnit(unit);
            partAttribute.setDescription(description);
            metadataTableElementList.add(new MetadataTableLeaf(partAttribute));
        }
    }

    private static Object getDataElemArray(ProductData data, int index) {
        switch (data.getType()) {
            case TYPE_INT8:
            case TYPE_INT16:
            case TYPE_INT32:
                return new int[]{data.getElemIntAt(index)};
            case TYPE_UINT8:
            case TYPE_UINT16:
            case TYPE_UINT32:
                return new long[]{data.getElemUIntAt(index)};
            case TYPE_INT64:
                return new long[]{data.getElemLongAt(index)};
            case TYPE_FLOAT32:
                return new float[]{data.getElemFloatAt(index)};
            case TYPE_FLOAT64:
                return new double[]{data.getElemDoubleAt(index)};
            default:
                return null;
        }
    }
}
