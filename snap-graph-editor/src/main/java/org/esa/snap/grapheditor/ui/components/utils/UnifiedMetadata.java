package org.esa.snap.grapheditor.ui.components.utils;

import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.descriptor.OperatorDescriptor;

public class UnifiedMetadata {
    private String name;
    private final String name_lower;
    private final String description;
    private final String category;
    private final String category_lower;
    private final OperatorDescriptor descriptor;

    private final int minNInputs;
    private final int maxNInputs;
    private final boolean hasOutputProduct;


    public UnifiedMetadata(final OperatorMetadata opMetadata, final OperatorDescriptor opDescriptor) {
        this.descriptor = opDescriptor;

        if (descriptor.getSourceProductsDescriptor() != null) {
            minNInputs = descriptor.getSourceProductDescriptors().length + 1;
            maxNInputs = -1;
        } else {
            minNInputs = descriptor.getSourceProductDescriptors().length;
            maxNInputs = minNInputs;
        }
        hasOutputProduct = descriptor.getTargetProductDescriptor() != null;

        name = opMetadata.label();
        if (name.length() == 0) {
            name = opMetadata.alias();
        }

        description = opMetadata.description();
        category = opMetadata.category();

        category_lower = category.toLowerCase();
        name_lower = name.toLowerCase();
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "<html>\n<b>"+name+"</b><br>\n"+category+"\n</html>";
    }

    /**
     * Operator description, unused due missing description.
     *
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Operator category the same as in the menu.
     *
     * @return category
     */
    public String getCategory() {
        return category;
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
