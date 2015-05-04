package org.esa.snap.framework.ui.product.metadata;

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
        metadataTableElements = MetadataTableElementFactory.getChildrenElementsFromElement(metadataElement);
//        MetadataElement[] elements = metadataElement.getElements();
//        MetadataAttribute[] attributes = metadataElement.getAttributes();
//        List<MetadataTableElement> metadataTableElementList = new ArrayList<>();
//        for (MetadataElement element : elements) {
//            metadataTableElementList.add(new MetadataElementWrapper(element));
//        }
//        for (MetadataAttribute attribute : attributes) {
//            final long dataElemSize = attribute.getNumDataElems();
//            if (dataElemSize > 0) {
//                final Object[] dataElems = (Object[]) attribute.getDataElems();
//                for (int j = 0; j < dataElemSize; j++) {
//                    final MetadataAttribute partAttribute =
//                            new MetadataAttribute(attribute.getName() + "." + j + 1, attribute.getDataType());
//                    partAttribute.setDataElems(dataElems[j]);
//                    partAttribute.setUnit(attribute.getUnit());
//                    partAttribute.setDescription(attribute.getDescription());
//                    metadataTableElementList.add(new MetadataAttributeWrapper(partAttribute));
//                }
//            } else {
//                metadataTableElementList.add(new MetadataAttributeWrapper(attribute));
//            }
//        }
//        metadataTableElements = metadataTableElementList.toArray(new MetadataTableElement[metadataTableElementList.size()]);
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
