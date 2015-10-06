package org.esa.snap.ui.product.metadata;

import org.openide.nodes.AbstractNode;

/**
 * @author Tonio Fincke
 */
interface MetadataTableElement {

    MetadataTableElement[] getMetadataTableElements();

    String getName();

    AbstractNode createNode();
}
