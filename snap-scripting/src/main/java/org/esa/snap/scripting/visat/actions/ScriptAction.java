/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.scripting.visat.actions;

import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.scripting.visat.ScriptManager;

import javax.script.ScriptEngine;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;


// currently not used
public class ScriptAction extends AbstractSnapAction {

    private static final String KEY_TYPE = "type";
    private static final String KEY_SCRIPT = "script";
    private static final String KEY_SRC = "src";
    private static final List<String> KNOWN_KEYS = Arrays.asList(KEY_SCRIPT, KEY_TYPE, KEY_SRC);

    private ScriptManager scriptManager;
    private ClassLoader classLoader;


    public static ScriptAction create(Map<String, Object> properties) {
        ScriptAction action = new ScriptAction();
        Stream<Map.Entry<String, Object>> stream = properties.entrySet().stream();
        stream.filter(entry -> KNOWN_KEYS.contains(entry.getKey())).forEach(entry -> action.putValue(entry.getKey(), entry.getValue()));
        return action;
    }

    private ScriptAction() {
        // Note:
        // In former code the class loader was retrieved by the module (ceres)
        //      cl = config.getDeclaringExtension().getDeclaringModule().getClassLoader();
        // properly the following is not the right replacement, but as the action
        // is not used anyway I won't spent time on it.
        classLoader = Thread.currentThread().getContextClassLoader();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (scriptManager == null) {
            PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(System.out), true);
            scriptManager = new ScriptManager(classLoader, printWriter);
        }

        Object eventSource = e.getSource();
        final Component component = eventSource instanceof Component ? (Component) eventSource : getAppContext().getApplicationWindow();

        ScriptEngine scriptEngine = getScriptEngine();
        if (scriptEngine == null) {
            JOptionPane.showMessageDialog(component, "Undefined scripting language.",
                                          getName(), JOptionPane.ERROR_MESSAGE);
            return;
        }

        scriptManager.setEngine(scriptEngine);

        String src = getSource();
        if (src != null) {
            try {
                URL resource = classLoader.getResource(src);
                if (resource == null) {
                    resource = new File(src).toURI().toURL();
                }
                scriptManager.execute(resource, new MyObserver(component));
            } catch (IOException ioe) {
                JOptionPane.showMessageDialog(component, "Error:\n" + ioe.getMessage(),
                                              getName(), JOptionPane.ERROR_MESSAGE);

            }
        }

        if (getScript() != null) {
            scriptManager.execute(getScript(), new MyObserver(component));
        }
    }

    private String getType() {
        return getKeyValue(KEY_TYPE);
    }

    private String getScript() {
        return getKeyValue(KEY_SCRIPT);
    }

    private String getSource() {
        return getKeyValue(KEY_SRC);
    }

    private String getName() {
        return getKeyValue(NAME);
    }

    private ScriptEngine getScriptEngine() {
        ScriptEngine scriptEngine = null;
        if (getType() != null) {
            scriptEngine = scriptManager.getEngineByMimeType(getType());
        }
        if (scriptEngine == null && getScript() != null) {
            int i = getScript().lastIndexOf(".");
            if (i > 0) {
                String ext = getScript().substring(i + 1);
                scriptEngine = scriptManager.getEngineByExtension(ext);
            }
        }
        return scriptEngine;
    }

    private String getKeyValue(String keyName) {
        Object value = getValue(keyName);
        if (value instanceof String) {
            return (String) value;
        }
        return null;
    }

    private class MyObserver implements ScriptManager.Observer {
        private final Component component;

        public MyObserver(Component component) {
            this.component = component;
        }

        @Override
        public void onSuccess(Object value) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(component, "Success."));
        }

        @Override
        public void onFailure(final Throwable throwable) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(component, "Error:\n" + throwable.getMessage(),
                                              getName(), JOptionPane.ERROR_MESSAGE);
                throwable.printStackTrace(System.out);
            });
        }
    }

}
