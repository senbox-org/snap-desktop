package org.esa.snap.framework.ui.product.metadata;

import org.esa.snap.framework.datamodel.MetadataElement;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Sheet;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

import java.awt.*;

/**
 * Created by tonio on 29.04.2015.
 */
public class MetadataElementInnerNode extends AbstractNode {

    private Sheet updatedSheet;

    public MetadataElementInnerNode(MetadataTableElement element) {
        this(element, new InstanceContent());
    }

    private MetadataElementInnerNode(MetadataTableElement element, InstanceContent content) {
        super(Children.create(new MetadataElementChildFactory(element.getMetadataTableElements()), false),
                new AbstractLookup(content));
        content.add(element);
        setName(element.getName());
        updatedSheet = createSheet();
    }

    @Override
    public Image getIcon(int type) {
        return new EmptyImage();
    }

    @Override
    public Image getOpenedIcon(int type) {
        return getIcon(type);
    }

    @Override
    protected final Sheet createSheet() {
        Sheet sheet = Sheet.createDefault();
        Sheet.Set set = Sheet.createPropertiesSet();
//        MetadataElement metadataElement = getLookup().lookup(MetadataElement.class);
//
//        MetadataAttributePropertySetter propertySetter = new MetadataAttributePropertySetter(metadataElement);
//        MetadataAttributePropertySetter.AttributeProperty[] modelProperties = propertySetter.getAttributeProperties();
//        for (MetadataAttributePropertySetter.AttributeProperty attributeProperty : modelProperties) {
//            set.put(attributeProperty);
//        }
        sheet.put(set);
        return sheet;
    }

}
