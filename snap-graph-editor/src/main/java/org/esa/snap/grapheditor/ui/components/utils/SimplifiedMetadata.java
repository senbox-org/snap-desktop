package org.esa.snap.grapheditor.ui.components.utils;

import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.descriptor.OperatorDescriptor;

public class SimplifiedMetadata {
    private String name;
    private final String name_lower;
    private final String description;
    private final String category;
    private final String category_lower;
    private final OperatorDescriptor descriptor;
    private final OperatorMetadata metadata;

    private final int minNInputs;
    private final int maxNInputs;
    private final boolean hasOutputProduct;


    public SimplifiedMetadata(final OperatorMetadata opMetadatada, final OperatorDescriptor opDescriptor) {
        this.descriptor = opDescriptor;

        if (descriptor.getSourceProductsDescriptor() != null) {
            minNInputs = descriptor.getSourceProductDescriptors().length + 1;
            maxNInputs = -1;
        } else {
            minNInputs = descriptor.getSourceProductDescriptors().length;
            maxNInputs = minNInputs;
        }
        hasOutputProduct = descriptor.getTargetProductDescriptor() != null;

        metadata = opMetadatada;

        name = metadata.label();
        if (name == null || name.length() == 0) {
            name = metadata.alias();
        }

        description = metadata.description();
        category = metadata.category();

        category_lower = category.toLowerCase();
        name_lower = name.toLowerCase();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public boolean find(final String string) {
        if (name_lower.contains(string)) {
            return true;
        }
        if (category_lower.contains(string)) {
            return true;
        }
        return false;
    }

    public double fuzzySearch(final String[] keywords) {
        double res = -1.0;
        for (String keyword: keywords) {
            if (name_lower.contains(keyword) && res < keyword.length()) {
                res = keyword.length();
            } else if (category_lower.contains(keyword) && res < 0) {
                res = 0;
            }
        }
        if (res > 0)
            return res / (double)name_lower.length();
        return res;
    }

    public int getMinNumberOfInputs() {
        return minNInputs;
    }

    public int getMaxNumberOfInputs() {
        return maxNInputs;
    }

    public boolean hasInputs() {
        return (minNInputs > 0);
    }

    public boolean hasOutput() {
        return hasOutputProduct;
    }

    public String getOutputDescription() {
        if (hasOutput())
            return descriptor.getDescription(); // TODO or get label??
        return "";
    }

    public String getInputDescription(int index) {
        if (hasInputs()) {
            if (index <  descriptor.getSourceProductDescriptors().length) {
                return descriptor.getSourceProductDescriptors()[index].getDescription(); // TODO or label?
            } else if (descriptor.getSourceProductsDescriptor() != null) {
                return descriptor.getSourceProductsDescriptor().getDescription();
            }
        }
        return "";
    }

    public OperatorDescriptor getDescriptor() {
        return descriptor;
    }

}
