package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.product.library.ui.v2.thread.AbstractRunnable;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by jcoravu on 4/3/2020.
 */
public class ExportLocalProductListPathsRunnable extends AbstractRunnable<Void> {

    private final Component parentDialogComponent;
    private final Path filePath;
    private final Path[] localProductPaths;

    public ExportLocalProductListPathsRunnable(Component parentDialogComponent, Path filePath, Path[] localProductPaths) {
        if (parentDialogComponent == null) {
            throw new NullPointerException("The parent component dialog is null.");
        }
        if (filePath == null) {
            throw new NullPointerException("The file path is null.");
        }
        if (parentDialogComponent == null) {
            throw new NullPointerException("The local product paths array is null.");
        }
        this.parentDialogComponent = parentDialogComponent;
        this.filePath = filePath;
        this.localProductPaths = localProductPaths;
    }

    @Override
    protected Void execute() throws Exception {
        try (OutputStream outputStream = Files.newOutputStream(this.filePath)) {
            try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream)) {
                try (BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter)) {
                    for (int i=0; i<this.localProductPaths.length; i++) {
                        bufferedWriter.write(this.localProductPaths[i].toString());
                        bufferedWriter.newLine();
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to export the local product list paths.";
    }

    @Override
    protected void failedExecuting(Exception exception) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                onFailedExecuting();
            }
        });
    }

    private void onFailedExecuting() {
        StringBuilder message = new StringBuilder();
        message.append("The product list could not be exported to the file '")
                .append(this.filePath.toString())
                .append("'.");
        JOptionPane.showMessageDialog(this.parentDialogComponent, message.toString(), "Failed to export the list", JOptionPane.ERROR_MESSAGE);
    }
}
