package org.esa.snap.framework.ui.product.metadata;

import org.esa.snap.framework.datamodel.MetadataAttribute;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

import java.awt.*;

/**
 * Created by tonio on 29.04.2015.
 */
public class MetadataElementLeafNode extends AbstractNode {

    private Sheet updatedSheet;

    public MetadataElementLeafNode(MetadataAttribute attribute) {
        this(attribute, new InstanceContent());
    }

    private MetadataElementLeafNode(MetadataAttribute attribute, InstanceContent content) {
        super(Children.LEAF, new AbstractLookup(content));
        content.add(attribute);
        setName(attribute.getName());
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
        MetadataAttribute metadataAttribute = getLookup().lookup(MetadataAttribute.class);
        MetadataAttributePropertySetter propertySetter = new MetadataAttributePropertySetter(metadataAttribute);
        PropertySupport[] properties = propertySetter.getAttributeProperties();
        for (PropertySupport attributeProperty : properties) {
            set.put(attributeProperty);
        }
        sheet.put(set);
        return sheet;
    }

}
