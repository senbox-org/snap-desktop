package org.esa.snap.framework.ui.product.metadata;

import org.esa.snap.framework.datamodel.MetadataAttribute;
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
        MetadataElement[] elements = root.getElements();
        MetadataAttribute[] attributes = root.getAttributes();
        metadataTableElements = new MetadataTableElement[elements.length + attributes.length];
        for (int i = 0; i < elements.length; i++) {
            metadataTableElements[i] = new MetadataElementWrapper(elements[i]);
        }
        for (int i = 0; i < attributes.length; i++) {
            metadataTableElements[elements.length + i] = new MetadataAttributeWrapper(attributes[i]);
        }
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
