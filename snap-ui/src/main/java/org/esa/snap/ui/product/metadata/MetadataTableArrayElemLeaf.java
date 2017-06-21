package org.esa.snap.ui.product.metadata;

import org.esa.snap.core.datamodel.ProductData;
import org.openide.nodes.AbstractNode;

import static org.esa.snap.core.datamodel.ProductData.TYPE_FLOAT32;
import static org.esa.snap.core.datamodel.ProductData.TYPE_FLOAT64;
import static org.esa.snap.core.datamodel.ProductData.TYPE_INT16;
import static org.esa.snap.core.datamodel.ProductData.TYPE_INT32;
import static org.esa.snap.core.datamodel.ProductData.TYPE_INT64;
import static org.esa.snap.core.datamodel.ProductData.TYPE_INT8;
import static org.esa.snap.core.datamodel.ProductData.TYPE_UINT16;
import static org.esa.snap.core.datamodel.ProductData.TYPE_UINT32;
import static org.esa.snap.core.datamodel.ProductData.TYPE_UINT8;

/**
 * @author Tonio Fincke
 */
class MetadataTableArrayElemLeaf extends MetadataTableLeaf {
    private ProductData data;
    private final int elemIndex;


    public MetadataTableArrayElemLeaf(String name, String unit, String description, ProductData data, int elemIndex) {
        super(name, data.getType(), data, unit, description);
        this.data = data;
        this.elemIndex = elemIndex;
    }

    @Override
    public MetadataTableElement[] getMetadataTableElements() {
        return new MetadataTableElement[0];
    }


    public ProductData getData() {
        Object dataElemArray = getDataElemArray(data, elemIndex);
        return ProductData.createInstance(data.getType(), dataElemArray);
    }


    @Override
    public AbstractNode createNode() {
        return new MetadataElementLeafNode(this);
    }

    private static Object getDataElemArray(ProductData data, int index) {
        switch (data.getType()) {
            case TYPE_INT8:
                return new byte[]{(byte)data.getElemIntAt(index)};
            case TYPE_INT16:
                return new short[]{(short)data.getElemIntAt(index)};
            case TYPE_INT32:
                return new int[]{data.getElemIntAt(index)};
            case TYPE_UINT8:
                return new byte[]{(byte)data.getElemUIntAt(index)};
            case TYPE_UINT16:
                return new short[]{(short)data.getElemUIntAt(index)};
            case TYPE_UINT32:
                return new int[]{(int)data.getElemUIntAt(index)};
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
