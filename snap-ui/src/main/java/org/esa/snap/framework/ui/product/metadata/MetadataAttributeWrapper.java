package org.esa.snap.framework.ui.product.metadata;

import org.esa.snap.framework.datamodel.MetadataAttribute;
import org.esa.snap.framework.datamodel.ProductData;

/**
 * Created by tonio on 29.04.2015.
 */
public class MetadataAttributeWrapper extends MetadataAttribute implements MetadataTableElement {

    private final MetadataAttribute metadataAttribute;

    public MetadataAttributeWrapper(MetadataAttribute metadataAttribute) {
        super(metadataAttribute.getName(), metadataAttribute.getDataType());
        this.metadataAttribute = metadataAttribute;
    }

    @Override
    public MetadataTableElement[] getMetadataTableElements() {
        return new MetadataTableElement[0];
    }

    @Override
    public String getName() {
        return metadataAttribute.getName();
    }

    @Override
    public int getDataType() {
        return metadataAttribute.getDataType();
    }

    @Override
    public ProductData getData() {
        return metadataAttribute.getData();
    }

    @Override
    public String getUnit() {
        return metadataAttribute.getUnit();
    }

    @Override
    public String getDescription() {
        return metadataAttribute.getDescription();
    }
}
