package org.esa.snap.graphbuilder.ui.components.utils;

import java.util.ArrayList;

import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.OperatorSpiRegistry;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;

public class OperatorManager {
    
    public class SimplifiedMetadata {
        private String name;
        private String name_lower;
        private String description;
        private String category;
        private String category_lower;
        

        public SimplifiedMetadata(OperatorMetadata metadata){
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

        public boolean find(String string) {
            if (name_lower.contains(string)) {
                return true;
            }
            if (category_lower.contains(string)){
                return true;
            }
            return false;
        }
    }

    private final GPF gpf;
    private final OperatorSpiRegistry opSpiRegistry;

    private final ArrayList<OperatorMetadata> metadatas = new ArrayList<>();
    private final ArrayList<SimplifiedMetadata> simpleMetadatas = new ArrayList<>();

    public OperatorManager() {
        gpf = GPF.getDefaultInstance();
        opSpiRegistry = gpf.getOperatorSpiRegistry();
        for (final OperatorSpi opSpi : opSpiRegistry.getOperatorSpis()) {
            if (!opSpi.getOperatorDescriptor().isInternal()) {
                final OperatorMetadata operatorMetadata = opSpi.getOperatorClass()
                        .getAnnotation(OperatorMetadata.class);
                metadatas.add(operatorMetadata);
                simpleMetadatas.add(new SimplifiedMetadata(operatorMetadata));
            }
        }
    }

    public ArrayList<SimplifiedMetadata> getSimplifiedMetadata() {
        return simpleMetadatas;
    }

    public ArrayList<OperatorMetadata> getMetadata() {
        return metadatas;
    }


}