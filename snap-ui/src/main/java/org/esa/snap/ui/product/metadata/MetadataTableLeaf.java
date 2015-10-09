package org.esa.snap.ui.product.metadata;

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.ProductData;
import org.openide.nodes.AbstractNode;

/**
 * @author Tonio Fincke
 */
class MetadataTableLeaf implements MetadataTableElement {
    private String name;
    private int dataType;
    private ProductData data;
    private String unit;
    private String description;

    public MetadataTableLeaf(MetadataAttribute attribute) {
        this(attribute.getName(), attribute.getDataType(), attribute.getData(),
             attribute.getUnit(), attribute.getDescription());
    }

    public MetadataTableLeaf(String name, int dataType, ProductData data, String unit, String description) {
        this.name = name;
        this.dataType = dataType;
        this.data = data;
        this.unit = unit;
        this.description = description;
    }

    @Override
    public MetadataTableElement[] getMetadataTableElements() {
        return new MetadataTableElement[0];
    }

    @Override
    public String getName() {
        return name;
    }

    public int getDataType() {
        return dataType;
    }

    public ProductData getData() {
        return data;
    }

    public String getUnit() {
        return unit;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public AbstractNode createNode() {
        return new MetadataElementLeafNode(this);
    }
}
