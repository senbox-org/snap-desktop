package org.esa.snap.rcp.statistics;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.annotations.ParameterDescriptorFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Marco Peters
 */
@SuppressWarnings("unused")
class MetadataPlotSettings {

    static final String PROP_NAME_METADATA_ELEMENT = "metadataElement";
    static final String PROP_NAME_RECORD_START_INDEX = "recordStartIndex";
    static final String PROP_NAME_RECORDS_PER_PLOT = "recordsPerPlot";
    static final String PROP_NAME_FIELD_X = "fieldX";
    static final String PROP_NAME_FIELD_Y1 = "fieldY1";
    static final String PROP_NAME_FIELD_Y2 = "fieldY2";
    private static final String FIELD_NAME_RECORD_INDEX = "Record Index";
    private static final String FIELD_NAME_ARRAY_FIELD_INDEX = "Array Field Index [n]";

    private MetadataElement metadataElement;
    private int numAvailableRecords;
    private String fieldX;
    private String fieldY1;
    private String fieldY2;
    private double recordStartIndex = 1.0;
    private int recordsPerPlot = 1;

    private BindingContext context;
    private AtomicBoolean isSynchronising = new AtomicBoolean(false);

    public MetadataPlotSettings() {
        context = new BindingContext(PropertyContainer.createObjectBacked(this, new ParameterDescriptorFactory()));
        Property propertyRecordStart = context.getPropertySet().getProperty(PROP_NAME_RECORD_START_INDEX);
        propertyRecordStart.getDescriptor().setAttribute("stepSize", 1);
        Property propertyMetaElement = context.getPropertySet().getProperty(PROP_NAME_METADATA_ELEMENT);
        propertyMetaElement.addPropertyChangeListener(evt -> {
            try {
                if (!isSynchronising.getAndSet(true)) {
                    // todo - swingworker?
                    numAvailableRecords = getNumRecords(metadataElement);

                    PropertySet propertySet = context.getPropertySet();
                    Property recordStartProperty = propertySet.getProperty(PROP_NAME_RECORD_START_INDEX);
                    Property numDispRecordProperty = propertySet.getProperty(PROP_NAME_RECORDS_PER_PLOT);
                    if (PROP_NAME_METADATA_ELEMENT.equals(evt.getPropertyName())) {
                        recordStartProperty.setValue(1.0);
                        numDispRecordProperty.setValue(1);
                    }


                    List<String> usableFieldNames = retrieveUsableFieldNames(metadataElement);
                    PropertyDescriptor propertyFieldY1 = propertySet.getProperty(PROP_NAME_FIELD_Y1).getDescriptor();
                    propertyFieldY1.setValueSet(new ValueSet(usableFieldNames.toArray(new String[0])));
                    PropertyDescriptor propertyFieldY2 = propertySet.getProperty(PROP_NAME_FIELD_Y2).getDescriptor();
                    propertyFieldY2.setValueSet(new ValueSet(usableFieldNames.toArray(new String[0])));

                    PropertyDescriptor propertyFieldX = propertySet.getProperty(PROP_NAME_FIELD_X).getDescriptor();
                    usableFieldNames.add(0, FIELD_NAME_RECORD_INDEX);
                    usableFieldNames.add(1, FIELD_NAME_ARRAY_FIELD_INDEX);
                    propertyFieldX.setValueSet(new ValueSet(usableFieldNames.toArray(new String[0])));
                }
            } catch (ValidationException e) {
                e.printStackTrace();
            }

        });

    }

    public BindingContext getContext() {
        return context;
    }

    void setMetadataElements(MetadataElement[] elements) {
        if(elements == null) {
            context.getPropertySet().setDefaultValues();
            return;
        }
        Property property = context.getPropertySet().getProperty(PROP_NAME_METADATA_ELEMENT);
        property.getDescriptor().setValueSet(new ValueSet(filterElements(elements)));
        try {
            property.setValue(elements[0]);
        } catch (ValidationException e) {
            e.printStackTrace();
        }
    }

    int getNumRecords() {
        return getNumRecords(metadataElement);
    }

    static List<String> retrieveUsableFieldNames(MetadataElement element) {

        int numRecords = getNumRecords(element);
        if (numRecords > 1) {
            return retrieveUsableFieldNames(element.getElements()[0]);
        } else {
            List<String> list = new ArrayList<>();
            String[] attributeNames = element.getAttributeNames();
            for (String fullAttribName : attributeNames) {
                String fieldName = getFieldName(fullAttribName);
                if (list.contains(fieldName)) { // skip over split array attributes if already added
                    continue;
                }
                MetadataAttribute attribute = element.getAttribute(fullAttribName);
                if (isNumericType(attribute)) {
                    list.add(fieldName);
                }

            }
            return list;
        }


    }

    private static String getFieldName(String fullAttribName) {
        String fieldName;
        Pattern p = Pattern.compile("(.*)\\.(\\d+)");
        final Matcher m = p.matcher(fullAttribName);
        if (m.matches()) {
            fieldName = m.group(1);
        } else {
            fieldName = fullAttribName;
        }
        return fieldName;
    }

    private static boolean isNumericType(MetadataAttribute attribute) {
        return ProductData.isIntType(attribute.getDataType()) || ProductData.isFloatingPointType(attribute.getDataType());
    }

    private MetadataElement[] filterElements(MetadataElement[] elements) {
        return elements;
    }

    private static int getNumRecords(MetadataElement metadataElement) {
        if (metadataElement == null) {
            return 0;
        }
        int numSubElements = metadataElement.getNumElements();
        if (numSubElements > 0) {
            MetadataElement[] subElements = metadataElement.getElements();
            int count = 0;
            for (MetadataElement subElement : subElements) {
                if (subElement.getName().matches(metadataElement.getName() + "\\.\\d+")) {   // subelements should only have a number suffix
                    count++;
                }
            }
            if (count == numSubElements) {
                return count;
            }
        }
        return 1;
    }

}
