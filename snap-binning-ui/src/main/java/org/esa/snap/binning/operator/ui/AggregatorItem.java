package org.esa.snap.binning.operator.ui;

import org.esa.snap.binning.AggregatorConfig;
import org.esa.snap.binning.AggregatorDescriptor;
import org.esa.snap.binning.TypedDescriptorsRegistry;
import org.esa.snap.binning.aggregators.AggregatorAverage;

/**
* @author Norman Fomferra
*/
class AggregatorItem {

    AggregatorDescriptor aggregatorDescriptor;
    AggregatorConfig aggregatorConfig;

    AggregatorItem() {
        this.aggregatorDescriptor = new AggregatorAverage.Descriptor();
        this.aggregatorConfig = aggregatorDescriptor.createConfig();
    }

    AggregatorItem(AggregatorConfig aggregatorConfig) {
        this.aggregatorConfig = aggregatorConfig;
        this.aggregatorDescriptor = TypedDescriptorsRegistry.getInstance().getDescriptor(AggregatorDescriptor.class, aggregatorConfig.getName());
    }
}
