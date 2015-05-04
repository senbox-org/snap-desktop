package org.esa.snap.framework.ui.product.metadata;

import org.esa.snap.framework.datamodel.MetadataElement;

/**
 * @author Tonio Fincke
 */
public class MetadataView {

    MetadataElement root;
    private static final String[] column_names = new String[]{"Value", "Value", "Type", "Type", "Unit", "Unit",
            "Description", "Description"};
    private static final int[] column_widths = {
            180, // 0
            180, // 1
            50, // 2
            40, // 3
            200 // 4
    };
    private static final String nodesColumnName = "Name";
    private final MetadataTableElement[] metadataTableElements;

    public MetadataView(MetadataElement element) {
        root = element;
        metadataTableElements = MetadataTableElementFactory.getChildrenElementsFromElement(element);
//        MetadataElement[] elements = root.getElements();
//        MetadataAttribute[] attributes = root.getAttributes();
//        metadataTableElements = new MetadataTableElement[elements.length + attributes.length];
//        for (int i = 0; i < elements.length; i++) {
//            metadataTableElements[i] = new MetadataElementWrapper(elements[i]);
//        }
//        for (int i = 0; i < attributes.length; i++) {
//            metadataTableElements[elements.length + i] = new MetadataAttributeWrapper(attributes[i]);
//        }

//        List<MetadataTableElement> metadataTableElementList = new ArrayList<>();
//        for (MetadataElement metadataElement : elements) {
//            metadataTableElementList.add(new MetadataElementWrapper(metadataElement));
//        }
//        for (MetadataAttribute attribute : attributes) {
//            final long dataElemSize = attribute.getNumDataElems();
//            if (dataElemSize > 1) {
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

    public MetadataElement getRootElement() {
        return root;
    }

    public String[] getColumnNames() {
        return column_names;
    }

    public int[] getColumnWidths() {
        return column_widths;
    }

    public MetadataTableElement[] getMetadataTableElements() {
        return metadataTableElements;
    }

    public String getNodesColumnName() {
        return nodesColumnName;
    }
}
