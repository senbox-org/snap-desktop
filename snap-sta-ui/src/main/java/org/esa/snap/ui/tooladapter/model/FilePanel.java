package org.esa.snap.ui.tooladapter.model;

import org.esa.snap.core.util.StringUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.nio.file.Path;

/**
 * Created by jcoravu on 10/6/2016.
 */
public class FilePanel extends JPanel {
    private final JTextField textField;
    private final JButton browseButton;

    public FilePanel() {
        super(new BorderLayout());

        this.textField = new JTextField();
        this.textField.setBorder(new EmptyBorder(0, 0, 0, 0));

        this.browseButton = new JButton("...");
        browseButton.setFocusable(false);
        Dimension size = new Dimension(26, 16);
        browseButton.setPreferredSize(size);
        browseButton.setMinimumSize(size);

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                textField.requestFocusInWindow();
            }
        });

        add(this.textField, BorderLayout.CENTER);
        add(browseButton, BorderLayout.EAST);
    }

    public void addBrowseButtonActionListener(ActionListener actionListener) {
        this.browseButton.addActionListener(actionListener);
    }

    public String getText() {
        return this.textField.getText();
    }

    public void setText(String text) {
        this.textField.setText(text);
    }

    public void addTextComponentFocusListener(FocusListener focusListener) {
        this.textField.addFocusListener(focusListener);
    }

    public Path showFileChooserDialog(int selectionMode, FileFilter filter) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(selectionMode);
        String filePath = textField.getText();
        if (!StringUtils.isNullOrEmpty(filePath)) {
            final Path currentFile = Path.of(filePath);
            fileChooser.setSelectedFile(currentFile.toFile());
        }
        if (filter != null) {
            fileChooser.setFileFilter(filter);
        }
        int resultState = fileChooser.showDialog(FilePanel.this, "Select");
        if (resultState == JFileChooser.APPROVE_OPTION && fileChooser.getSelectedFile() != null) {
            textField.setText(fileChooser.getSelectedFile().toPath().toAbsolutePath().toString());
            return fileChooser.getSelectedFile().toPath();
        }
        return null;
    }
}
