/*
 *
 *  * Copyright (C) 2015 CS SI
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */
package org.esa.snap.ui.tooladapter;

import org.esa.snap.framework.gpf.GPF;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.framework.gpf.OperatorSpiRegistry;
import org.esa.snap.framework.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.snap.framework.gpf.operators.tooladapter.ToolAdapterIO;
import org.esa.snap.framework.gpf.operators.tooladapter.ToolAdapterOpSpi;
import org.esa.snap.ui.tooladapter.actions.ToolAdapterActionRegistrar;
import org.openide.modules.ModuleInstall;

import java.util.Collection;

/**
 * Tool Adapter module installer class for NetBeans.
  * and menu entries.
 *
 * @author Cosmin Cara
 */
public class ModuleInstaller extends ModuleInstall {
    @Override
    public void restored() {
        OperatorSpiRegistry spiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        if (spiRegistry != null) {
            Collection<OperatorSpi> operatorSpis = spiRegistry.getOperatorSpis();
            if (operatorSpis != null) {
                if (operatorSpis.size() == 0) {
                    operatorSpis.addAll(ToolAdapterIO.searchAndRegisterAdapters());
                }
                operatorSpis.stream().filter(spi -> spi instanceof ToolAdapterOpSpi).forEach(spi -> {
                    ToolAdapterOperatorDescriptor operatorDescriptor = (ToolAdapterOperatorDescriptor) spi.getOperatorDescriptor();
                    ToolAdapterActionRegistrar.registerOperatorMenu(operatorDescriptor);
                });
            }
        }
    }
}
