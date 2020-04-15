package org.esa.snap.remote.execution.operator;

import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.descriptor.OperatorDescriptor;
import org.esa.snap.core.gpf.descriptor.ParameterDescriptor;
import org.esa.snap.core.gpf.descriptor.SourceProductDescriptor;
import org.esa.snap.core.gpf.descriptor.SourceProductsDescriptor;
import org.esa.snap.core.gpf.descriptor.TargetProductDescriptor;
import org.esa.snap.core.gpf.descriptor.TargetPropertyDescriptor;

/**
 * Created by jcoravu on 11/3/2019.
 */
public class OperatorEmptyDescriptorImpl implements OperatorDescriptor {

    private final OperatorDescriptor baseOperatorDescriptor;
    private final ParameterDescriptor[] params;
    private final SourceProductDescriptor[] sourceProducts;

    public OperatorEmptyDescriptorImpl(OperatorDescriptor baseOperatorDescriptor) {
        if (baseOperatorDescriptor == null) {
            throw new NullPointerException("The operator descriptor is null.");
        }
        this.baseOperatorDescriptor = baseOperatorDescriptor;
        this.params = new ParameterDescriptor[0];
        this.sourceProducts = new SourceProductDescriptor[0];
    }

    @Override
    public String getName() {
        return this.baseOperatorDescriptor.getName();
    }

    @Override
    public String getAlias() {
        return this.baseOperatorDescriptor.getAlias();
    }

    @Override
    public String getLabel() {
        return this.baseOperatorDescriptor.getLabel();
    }

    @Override
    public String getDescription() {
        return this.baseOperatorDescriptor.getDescription();
    }

    @Override
    public String getVersion() {
        return this.baseOperatorDescriptor.getVersion();
    }

    @Override
    public String getAuthors() {
        return this.baseOperatorDescriptor.getAuthors();
    }

    @Override
    public String getCopyright() {
        return this.baseOperatorDescriptor.getCopyright();
    }

    @Override
    public boolean isInternal() {
        return this.baseOperatorDescriptor.isInternal();
    }

    @Override
    public boolean isAutoWriteDisabled() {
        return this.baseOperatorDescriptor.isAutoWriteDisabled();
    }

    @Override
    public Class<? extends Operator> getOperatorClass() {
        return RemoteExecutionDialog.CloudExploitationPlatformItem.class;
    }

    @Override
    public SourceProductDescriptor[] getSourceProductDescriptors() {
        return this.sourceProducts;
    }

    @Override
    public SourceProductsDescriptor getSourceProductsDescriptor() {
        return null;
    }

    @Override
    public ParameterDescriptor[] getParameterDescriptors() {
        return this.params;
    }

    @Override
    public TargetProductDescriptor getTargetProductDescriptor() {
        return null;
    }

    @Override
    public TargetPropertyDescriptor[] getTargetPropertyDescriptors() {
        return new TargetPropertyDescriptor[0];
    }
}
