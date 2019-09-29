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

    private ParameterDescriptor[] params;
    private SourceProductDescriptor[] sourceProducts;

    public OperatorEmptyDescriptorImpl() {
        this.params = new ParameterDescriptor[0];
        this.sourceProducts = new SourceProductDescriptor[0];
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getAlias() {
        return null;
    }

    @Override
    public String getLabel() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public String getAuthors() {
        return null;
    }

    @Override
    public String getCopyright() {
        return null;
    }

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public boolean isAutoWriteDisabled() {
        return false;
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
