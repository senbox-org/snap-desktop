package org.esa.snap.ui.product.metadata;

import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Sheet;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 * @author Tonio Fincke
 */
class MetadataElementInnerNode extends AbstractNode {

    public MetadataElementInnerNode(MetadataTableInnerElement element) {
        this(element, new InstanceContent());
    }

    private MetadataElementInnerNode(MetadataTableInnerElement element, InstanceContent content) {
        super(Children.create(new MetadataElementChildFactory(element.getMetadataTableElements()), false),
                new AbstractLookup(content));
        content.add(element);
        setName(element.getName());
        createSheet();
    }

    @Override
    protected final Sheet createSheet() {
        Sheet sheet = Sheet.createDefault();
        Sheet.Set set = Sheet.createPropertiesSet();
        sheet.put(set);
        return sheet;
    }

}
