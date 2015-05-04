package org.esa.snap.framework.ui.product.metadata;

import org.esa.snap.framework.datamodel.MetadataAttribute;
import org.esa.snap.framework.datamodel.MetadataElement;

/**
 * @author Tonio Fincke
 */
class MetadataElementWrapper extends MetadataElement implements MetadataTableElement {

    private final MetadataElement metadataElement;
    private final MetadataTableElement[] metadataTableElements;


    public MetadataElementWrapper(MetadataElement metadataElement) {
        super(metadataElement.getName());
        this.metadataElement = metadataElement;
        MetadataElement[] elements = metadataElement.getElements();
        MetadataAttribute[] attributes = metadataElement.getAttributes();
        metadataTableElements = new MetadataTableElement[elements.length + attributes.length];
        for (int i = 0; i < elements.length; i++) {
            metadataTableElements[i] = new MetadataElementWrapper(elements[i]);
        }
        for (int i = 0; i < attributes.length; i++) {
            metadataTableElements[elements.length + i] = new MetadataAttributeWrapper(attributes[i]);
        }
    }

    @Override
    public MetadataTableElement[] getMetadataTableElements() {
        return metadataTableElements;
    }

    @Override
    public String getName() {
        return metadataElement.getName();
    }
}
