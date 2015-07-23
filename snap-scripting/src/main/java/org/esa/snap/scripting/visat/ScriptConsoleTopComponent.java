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

package org.esa.snap.scripting.visat;

import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.scripting.visat.actions.HelpAction;
import org.esa.snap.scripting.visat.actions.NewAction;
import org.esa.snap.scripting.visat.actions.OpenAction;
import org.esa.snap.scripting.visat.actions.RunAction;
import org.esa.snap.scripting.visat.actions.SaveAction;
import org.esa.snap.scripting.visat.actions.SaveAsAction;
import org.esa.snap.scripting.visat.actions.StopAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

// todo (NF) - find out how to:
// (1) ... gracefully cancel a running script
// (2) ... remove bindings (references) in JavaScript to products, views, etc. in order to avoid memory leaks
// (3) ... debug a script
// (4) ... trace & undo changes to BEAM made by a script

/**
 * A TopComponent for the scripting console.
 * @author Norman Fomferra
 * @author Marco Peters
 */
@TopComponent.Description(
        preferredID = "ScriptConsoleTopComponent",
        iconBase = "tango/16x16/apps/utilities-terminal.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(
        mode = "output",  // it's one of the standard modes,
        openAtStartup = false,
        position = 30)
@ActionID(category = "Window", id = "org.esa.snap.scripting.visat.ScriptConsoleTopComponent")
@ActionReference(path = "Menu/View/Tool Windows", position = 0)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_ScriptConsoleTopComponent_Name",
        preferredID = "ScriptConsoleTopComponent"
)
@NbBundle.Messages({
        "CTL_ScriptConsoleTopComponent_Name=Script Console",
        "CTL_ScriptConsoleTopComponent_Description=Execute SNAP scripts using JavaScript (default) or other scripting languages (add-on)."})
public class ScriptConsoleTopComponent extends TopComponent{

    // View
    private Map<String, Action> actionMap;
    private JTextArea inputTextArea;
    private JTextArea outputTextArea;

    private ScriptManager scriptManager;
    private PrintWriter output;
    private File file;

    public ScriptConsoleTopComponent() {
        this.actionMap = new HashMap<>();

        registerAction(new NewAction(this));
        registerAction(new OpenAction(this));
        registerAction(new SaveAction(this));
        registerAction(new SaveAsAction(this));
        registerAction(new RunAction(this));
        registerAction(new StopAction(this));
        registerAction(new HelpAction(this));

        inputTextArea = new JTextArea(); // todo - replace by JIDE code editor component (nf)
        inputTextArea.setWrapStyleWord(false);
        inputTextArea.setTabSize(4);
        inputTextArea.setRows(10);
        inputTextArea.setColumns(80);
        inputTextArea.setFont(new Font("Courier", Font.PLAIN, 13));

        outputTextArea = new JTextArea();
        outputTextArea.setWrapStyleWord(false);
        outputTextArea.setTabSize(4);
        outputTextArea.setRows(3);
        outputTextArea.setColumns(80);
        outputTextArea.setEditable(false);
        outputTextArea.setBackground(Color.LIGHT_GRAY);
        outputTextArea.setFont(new Font("Courier", Font.PLAIN, 13));

        final JToolBar toolBar = new JToolBar("Script Console");
        toolBar.setFloatable(false);
        toolBar.add(getToolButton(NewAction.ID));
        toolBar.add(getToolButton(OpenAction.ID));
        toolBar.add(getToolButton(SaveAction.ID));
        toolBar.add(getToolButton(SaveAsAction.ID));
        toolBar.addSeparator();
        toolBar.add(getToolButton(RunAction.ID));
        toolBar.add(getToolButton(StopAction.ID));
        toolBar.addSeparator();
        toolBar.add(getToolButton(HelpAction.ID));

        getAction(NewAction.ID).setEnabled(true);
        getAction(OpenAction.ID).setEnabled(true);
        getAction(SaveAction.ID).setEnabled(false);
        getAction(SaveAsAction.ID).setEnabled(false);
        getAction(RunAction.ID).setEnabled(false);
        getAction(StopAction.ID).setEnabled(false);
        getAction(HelpAction.ID).setEnabled(true);
        inputTextArea.setEditable(false);
        inputTextArea.setEnabled(false);

        JScrollPane inputEditorScrollPane = new JScrollPane(inputTextArea);
        inputEditorScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        inputEditorScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        JScrollPane outputEditorScrollPane = new JScrollPane(outputTextArea);
        outputEditorScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        outputEditorScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        JSplitPane documentPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, inputEditorScrollPane, outputEditorScrollPane);
        documentPanel.setDividerLocation(0.7);
        documentPanel.setBorder(null);

        setLayout(new BorderLayout(4, 4));
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        setPreferredSize(new Dimension(800, 400));
        add(toolBar, BorderLayout.NORTH);
        add(documentPanel, BorderLayout.CENTER);

        output = new PrintWriter(new ScriptOutput(), true);
        scriptManager = new ScriptManager(getClass().getClassLoader(), output);
        updateTitle();
    }

    private void registerAction(Action action) {
        actionMap.put(action.getValue(Action.ACTION_COMMAND_KEY).toString(), action);
    }

    public ScriptManager getScriptManager() {
        return scriptManager;
    }

    public void runScript() {

        if (scriptManager.getEngine() == null) {
            showErrorMessage("No script language selected.");
            return;
        }

        final String text = inputTextArea.getText().trim();
        if (text.isEmpty()) {
            return;
        }

        outputTextArea.setText(null);

        enableRun(false);
        scriptManager.execute(text, new ExecutionObserver());
    }

    public void stopScript() {
        scriptManager.reset();
        getAction(StopAction.ID).setEnabled(false);
    }

    public void showErrorMessage(String message) {
        SnapDialogs.showError("Script Console - Error", message);
    }

    public void newScript(ScriptEngineFactory scriptEngineFactory) {
        inputTextArea.setText(null);
        outputTextArea.setText(null);

        ScriptEngine factory = scriptManager.getEngineByFactory(scriptEngineFactory);
        scriptManager.setEngine(factory);
        setFile(null);

        enableRun(true);
    }

    private void enableRun(boolean b) {
        getAction(NewAction.ID).setEnabled(b);
        getAction(OpenAction.ID).setEnabled(b);
        getAction(SaveAction.ID).setEnabled(b);
        getAction(SaveAsAction.ID).setEnabled(b);

        getAction(RunAction.ID).setEnabled(b);
        getAction(StopAction.ID).setEnabled(!b);

        inputTextArea.setEnabled(b);
        inputTextArea.setEditable(b);
    }

    private JButton getToolButton(String actionId) {
        Action action = getAction(actionId);
        final JButton button = new JButton(action);
        button.setText(null);
        return button;
    }

    public Action getAction(String actionId) {
        return actionMap.get(actionId);
    }

    public void openScript(File file) {
        enableRun(false);
        // todo - use swing worker
        try {
            String fileName = file.getName();
            int i = fileName.lastIndexOf('.');
            if (i <= 0) {
                showErrorMessage(MessageFormat.format("Unknown script type ''{0}''.", fileName));
                return;
            }
            String ext = fileName.substring(i + 1);
            ScriptEngine scriptEngine = scriptManager.getEngineByExtension(ext);
            if (scriptEngine == null) {
                showErrorMessage(MessageFormat.format("Unknown script type ''{0}''.", fileName));
                return;
            }

            StringBuilder sb = new StringBuilder();
            try {
                try (LineNumberReader reader = new LineNumberReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                        sb.append("\n");
                    }
                }
            } catch (IOException e) {
                showErrorMessage(MessageFormat.format("I/O error:\n{0}", e.getMessage()));
                return;
            }

            inputTextArea.setText(sb.toString());
            scriptManager.setEngine(scriptEngine);
            setFile(file);
        } finally {
            enableRun(true);
        }
    }

    public File getFile() {
        return file;
    }

    private void setFile(File file) {
        this.file = file;
        updateTitle();
    }

    private void updateTitle() {

        ScriptEngine scriptEngine = scriptManager.getEngine();
        String titleBase = Bundle.CTL_ScriptConsoleTopComponent_Name();
        if (scriptEngine != null) {
            String languageName = scriptEngine.getFactory().getLanguageName();
            if (file != null) {
                setDisplayName(MessageFormat.format("{0} - [{1}] - [{2}]", titleBase, languageName, file));
            } else {
                setDisplayName(MessageFormat.format("{0} - [{1}] - [unnamed]", titleBase, languageName));
            }
        } else {
            setDisplayName(titleBase);
        }
    }

    public void saveScriptAs(File file) {
        enableRun(false);
        // todo - use swing worker
        try {
            try {
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(inputTextArea.getText());
                }
                setFile(file);
            } catch (IOException e) {
                showErrorMessage(MessageFormat.format("I/O error:\n{0}", e.getMessage()));
            }
        } finally {
            enableRun(true);
        }
    }

    public void saveScript() {
        saveScriptAs(getFile());
    }

    public class ScriptOutput extends Writer {

        @Override
        public void close() {
        }

        @Override
        public void flush() {
        }

        @Override
        public void write(char characters[], int off, int len) {
            print0(new String(characters, off, len));
        }

        @Override
        public void write(String str) {
            print0(str);
        }

        /////////////////////////////////////////////////////////////////////
        // private

        private void print0(final String str) {
            if (SwingUtilities.isEventDispatchThread()) {
                print1(str);
            } else {
                SwingUtilities.invokeLater(() -> print1(str));
            }
        }

        private void print1(String text) {
            try {
                int offset = outputTextArea.getDocument().getEndPosition().getOffset();
                outputTextArea.getDocument().insertString(offset, text, null);
            } catch (BadLocationException e) {
                // ignore
            }
        }


    }

    private class ExecutionObserver implements ScriptManager.Observer {
        @Override
        public void onSuccess(Object value) {
            if (value != null) {
                output.println(String.valueOf(value));
            }
            SwingUtilities.invokeLater(() -> enableRun(true));
        }

        @Override
        public void onFailure(Throwable throwable) {
            output.println("Error: " + throwable.getMessage());
            throwable.printStackTrace(output);
            SwingUtilities.invokeLater(() -> enableRun(true));
        }
    }
}
