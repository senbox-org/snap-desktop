/*
 *
 *  * Copyright (C) 2016 CS ROMANIA
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
 *  *  with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.snap.ui.tooladapter.dialogs.progress;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.gpf.operators.tooladapter.DefaultOutputConsumer;
import org.esa.snap.ui.tooladapter.dialogs.ConsolePane;

import javax.swing.*;

/**
 * @author kraftek
 * @date 3/14/2017
 */
public class ConsoleConsumer extends DefaultOutputConsumer {
    private ConsolePane consolePane;

    public ConsoleConsumer(String progressPattern, String errorPattern, String stepPattern, ProgressMonitor pm, ConsolePane consolePane) {
        super(progressPattern, errorPattern, stepPattern, pm);
        this.consolePane = consolePane;
    }

    @Override
    public void consumeOutput(String line) {
        super.consumeOutput(line);
        if (consolePane != null) {
            if (SwingUtilities.isEventDispatchThread()) {
                consume(line);
            } else {
                SwingUtilities.invokeLater(() -> consume(line));
            }
        }
    }

    void setVisible(boolean value) {
        if (this.consolePane != null) {
            this.consolePane.setVisible(value);
        }
    }

    private void consume(String line) {
        if (this.error == null || !this.error.matcher(line).matches()) {
            consolePane.appendInfo(line);
        } else {
            consolePane.appendError(line);
        }
    }
}
