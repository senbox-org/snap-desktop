package org.esa.snap.ui.product.metadata;

import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Tonio Fincke
 */
class MetadataElementChildFactory extends ChildFactory.Detachable<MetadataTableElement> {

    private List<MetadataTableElement> metadataTableElementList;

    public MetadataElementChildFactory(MetadataTableElement[] metadataElements) {
        metadataTableElementList = new ArrayList<>();
        Collections.addAll(metadataTableElementList, metadataElements);
    }

    @Override
    protected boolean createKeys(List<MetadataTableElement> toPopulate) {
        if (metadataTableElementList.size() == 0) {
            return true;
        }
        return toPopulate.addAll(metadataTableElementList);
    }

    @Override
    protected Node createNodeForKey(MetadataTableElement metadataElement) {
        return metadataElement.createNode();
    }

}
