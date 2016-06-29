/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.gpf.ui;

import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.binding.dom.XppDomElement;
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier;
import com.thoughtworks.xstream.io.xml.XppDomWriter;
import com.thoughtworks.xstream.io.xml.XppReader;
import com.thoughtworks.xstream.io.xml.xppdom.XppDom;
import org.apache.commons.lang.StringEscapeUtils;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.OperatorSpiRegistry;
import org.esa.snap.core.gpf.descriptor.OperatorDescriptor;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.ui.AbstractDialog;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.UIUtils;
import org.openide.util.HelpCtx;
import org.xmlpull.mxp1.MXParser;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * WARNING: This class belongs to a preliminary API and may change in future releases.
 * <p>
 * Provides an operator menu with action for loading, saving and displaying the parameters of an operator
 * in the file menu section and actions for help and about in the help menu section.
 *
 * @author Norman Fomferra
 * @author Marco Zühlke
 */
public class OperatorMenu {

    private final Component parentComponent;
    private final OperatorParameterSupport parameterSupport;
    private final OperatorDescriptor opDescriptor;
    private final AppContext appContext;
    private final String helpId;
    private final Action loadParametersAction;
    private final Action saveParametersAction;
    private final Action displayParametersAction;
    private final Action aboutAction;
    private final String lastDirPreferenceKey;

    /**
     * @deprecated since BEAM 5, use {@link #OperatorMenu(Component, OperatorDescriptor, OperatorParameterSupport, AppContext, String)} instead
     */
    @Deprecated
    public OperatorMenu(Component parentComponent,
                        Class<? extends Operator> opType,
                        OperatorParameterSupport parameterSupport,
                        String helpId) {
        this(parentComponent, getOperatorDescriptor(opType), parameterSupport, null, helpId);
    }

    public OperatorMenu(Component parentComponent,
                        OperatorDescriptor opDescriptor,
                        OperatorParameterSupport parameterSupport,
                        AppContext appContext,
                        String helpId) {
        this.parentComponent = parentComponent;
        this.parameterSupport = parameterSupport;
        this.opDescriptor = opDescriptor;
        this.appContext = appContext;
        this.helpId = helpId;
        lastDirPreferenceKey = opDescriptor.getName() + ".lastDir";
        loadParametersAction = new LoadParametersAction();
        saveParametersAction = new SaveParametersAction();
        displayParametersAction = new DisplayParametersAction();
        aboutAction = new AboutOperatorAction();
    }

    /**
     * Creates the default menu.
     *
     * @return The menu
     */
    public JMenuBar createDefaultMenu() {
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(loadParametersAction);
        fileMenu.add(saveParametersAction);
        fileMenu.addSeparator();
        fileMenu.add(displayParametersAction);

        JMenu helpMenu = new JMenu("Help");
        helpMenu.add(createHelpMenuItem());
        helpMenu.add(aboutAction);

        final JMenuBar menuBar = new JMenuBar();
        menuBar.add(fileMenu);
        menuBar.add(helpMenu);

        return menuBar;
    }

    private JMenuItem createHelpMenuItem() {
        JMenuItem menuItem = new JMenuItem("Help");
        if (helpId != null && !helpId.isEmpty()) {
            menuItem.addActionListener(e -> new HelpCtx(helpId).display());
        } else {
            menuItem.setEnabled(false);
        }
        return menuItem;
    }

    private class LoadParametersAction extends AbstractAction {

        private static final String TITLE = "Load Parameters";

        LoadParametersAction() {
            super(TITLE + "...");
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            JFileChooser fileChooser = new JFileChooser();
            FileNameExtensionFilter parameterFileFilter = createParameterFileFilter();
            fileChooser.addChoosableFileFilter(parameterFileFilter);
            fileChooser.setFileFilter(parameterFileFilter);
            fileChooser.setDialogTitle(TITLE);
            fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
            applyCurrentDirectory(fileChooser);
            int response = fileChooser.showDialog(parentComponent, "Load");
            if (JFileChooser.APPROVE_OPTION == response) {
                try {
                    preserveCurrentDirectory(fileChooser);
                    readFromFile(fileChooser.getSelectedFile());
                } catch (Exception e) {
                    Debug.trace(e);
                    JOptionPane.showMessageDialog(parentComponent, "Could not load parameters.\n" + e.getMessage(),
                                                  TITLE, JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        @Override
        public boolean isEnabled() {
            return super.isEnabled() && parameterSupport != null;
        }

        private void readFromFile(File selectedFile) throws Exception {
            try (FileReader reader = new FileReader(selectedFile)) {
                DomElement domElement = readXml(reader);
                parameterSupport.fromDomElement(domElement);
            }
        }

        private DomElement readXml(Reader reader) throws IOException {
            try (BufferedReader br = new BufferedReader(reader)) {
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();
                while (line != null) {
                    sb.append(line);
                    line = br.readLine();
                }
                return new XppDomElement(createDom(sb.toString()));
            }
        }

    }

    static XppDom createDom(String xml) {
        XppDomWriter domWriter = new XppDomWriter();
        new HierarchicalStreamCopier().copy(new XppReader(new StringReader(xml), new MXParser()), domWriter);
        return domWriter.getConfiguration();
    }

    private class SaveParametersAction extends AbstractAction {

        private static final String TITLE = "Save Parameters";

        SaveParametersAction() {
            super(TITLE + "...");
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            JFileChooser fileChooser = new JFileChooser();
            final FileNameExtensionFilter parameterFileFilter = createParameterFileFilter();
            fileChooser.addChoosableFileFilter(parameterFileFilter);
            fileChooser.setFileFilter(parameterFileFilter);
            fileChooser.setDialogTitle(TITLE);
            fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
            applyCurrentDirectory(fileChooser);
            int response = fileChooser.showDialog(parentComponent, "Save");
            if (JFileChooser.APPROVE_OPTION == response) {
                try {
                    preserveCurrentDirectory(fileChooser);
                    File selectedFile = fileChooser.getSelectedFile();
                    selectedFile = FileUtils.ensureExtension(selectedFile,
                                                             "." + parameterFileFilter.getExtensions()[0]);
                    DomElement domElement = parameterSupport.toDomElement();
                    escapeXmlElements(domElement);
                    String xmlString = domElement.toXml();
                    writeToFile(xmlString, selectedFile);
                } catch (Exception e) {
                    Debug.trace(e);
                    JOptionPane.showMessageDialog(parentComponent, "Could not save parameters.\n" + e.getMessage(),
                                                  TITLE, JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        @Override
        public boolean isEnabled() {
            return super.isEnabled() && parameterSupport != null;
        }

        private void writeToFile(String s, File outputFile) throws IOException {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
                bw.write(s);
            }
        }
    }

    static void escapeXmlElements(DomElement domElement) {
        domElement.setValue(StringEscapeUtils.escapeXml(domElement.getValue()));
        String[] attributeNames = domElement.getAttributeNames();
        for (String attributeName : attributeNames) {
            domElement.setAttribute(attributeName, StringEscapeUtils.escapeXml(domElement.getAttribute(attributeName)));
        }
        DomElement[] children = domElement.getChildren();
        for (DomElement child : children) {
            escapeXmlElements(child);
        }
    }

    private FileNameExtensionFilter createParameterFileFilter() {
        return new FileNameExtensionFilter("GPF Parameter Files (XML)", "xml");
    }

    private class DisplayParametersAction extends AbstractAction {

        private static final String TITLE = "Display Parameters";

        DisplayParametersAction() {
            super(TITLE + "...");
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            String parameterXml;
            try {
                DomElement domElement = parameterSupport.toDomElement();
                parameterXml = domElement.toXml();
            } catch (Exception e) {
                Debug.trace(e);
                JOptionPane.showMessageDialog(UIUtils.getRootWindow(parentComponent),
                                              "Failed to convert parameters to XML.",
                                              TITLE,
                                              JOptionPane.ERROR_MESSAGE);
                return;
            }
            JTextArea textArea = new JTextArea(parameterXml);
            textArea.setEditable(false);
            JScrollPane textAreaScrollPane = new JScrollPane(textArea);
            textAreaScrollPane.setPreferredSize(new Dimension(360, 360));
            showInformationDialog(getOperatorName() + " Parameters", textAreaScrollPane);
        }

        @Override
        public boolean isEnabled() {
            return super.isEnabled() && parameterSupport != null;
        }
    }


    private class AboutOperatorAction extends AbstractAction {

        AboutOperatorAction() {
            super("About...");
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            showInformationDialog("About " + getOperatorName(), new JLabel(getOperatorAboutText()));
        }
    }

    private void showInformationDialog(String title, Component component) {
        final ModalDialog modalDialog = new ModalDialog(UIUtils.getRootWindow(parentComponent),
                                                        title,
                                                        AbstractDialog.ID_OK,
                                                        null); /*I18N*/
        modalDialog.setContent(component);
        modalDialog.show();
    }

    String getOperatorName() {
        return opDescriptor.getAlias() != null ? opDescriptor.getAlias() : opDescriptor.getName();
    }

    String getOperatorAboutText() {
        return makeHtmlConform(String.format("" +
                                             "<html>" +
                                             "<h2>%s Operator</h2>" +
                                             "<table>" +
                                             "<tr><td><b>Name:</b></td><td><code>%s</code></td></tr>" +
                                             "<tr><td><b>Version:</b></td><td>%s</td></tr>" +
                                             "<tr><td><b>Full name:</b></td><td><code>%s</code></td></tr>" +
                                             "<tr><td><b>Description:</b></td><td>%s</td></tr>" +
                                             "<tr><td><b>Authors:</b></td><td>%s</td></tr>" +
                                             "<tr><td><b>Copyright:</b></td><td>%s</td></tr></table></html>",
                                             getOperatorName(),
                                             getOperatorName(),
                                             opDescriptor.getVersion(),
                                             opDescriptor.getName(),
                                             opDescriptor.getDescription(),
                                             opDescriptor.getAuthors(),
                                             opDescriptor.getCopyright()
        ));
    }

    private static String makeHtmlConform(String text) {
        return text.replace("\n", "<br/>");
    }

    private static OperatorDescriptor getOperatorDescriptor(Class<? extends Operator> opType) {
        String operatorAlias = OperatorSpi.getOperatorAlias(opType);

        OperatorDescriptor operatorDescriptor;
        OperatorSpiRegistry spiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        operatorDescriptor = spiRegistry.getOperatorSpi(operatorAlias).getOperatorDescriptor();
        if (operatorDescriptor == null) {
            Class<?>[] declaredClasses = opType.getDeclaredClasses();
            for (Class<?> declaredClass : declaredClasses) {
                if (OperatorSpi.class.isAssignableFrom(declaredClass)) {
                    operatorDescriptor = spiRegistry.getOperatorSpi(declaredClass.getName()).getOperatorDescriptor();
                }
            }
        }
        if (operatorDescriptor == null) {
            throw new IllegalStateException("Not able to find SPI for operator class '" + opType.getName() + "'");
        }
        return operatorDescriptor;
    }

    private void applyCurrentDirectory(JFileChooser fileChooser) {
        if (appContext != null) {
            String homeDirPath = SystemUtils.getUserHomeDir().getPath();
            String lastDir = appContext.getPreferences().getPropertyString(lastDirPreferenceKey, homeDirPath);
            fileChooser.setCurrentDirectory(new File(lastDir));
        }
    }

    private void preserveCurrentDirectory(JFileChooser fileChooser) {
        if (appContext != null) {
            String lastDir = fileChooser.getCurrentDirectory().getAbsolutePath();
            appContext.getPreferences().setPropertyString(lastDirPreferenceKey, lastDir);
        }
    }

}
