package org.esa.snap.ui.vfs.niojfilechooser;

import org.esa.snap.core.dataio.NioFile;
import org.esa.snap.core.dataio.vfs.remote.object_storage.aws.S3FileSystemProvider;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * FileSystemView component for Amazon AWS S3 Object Storage VFS.
 * FileSystemView that handles some specific Amazon AWS S3 Virtual File System concepts.
 *
 * @author Adrian Drăghici
 */
class NioAmazonS3FileSystemView extends NioFileSystemView {

    private FileSystem fs;

    private NioAmazonS3FileSystemView(String bucketAddress, String username, String password) throws AccessDeniedException {
        S3FileSystemProvider.setupConnectionData(bucketAddress, username, password);
        try {
            fs = S3FileSystemProvider.getS3FileSystem();
        } catch (AccessDeniedException e) {
            if (e.getMessage().contains("401: Unauthorized")) {
                JOptionPane.showMessageDialog(null, "Username or password incorrect", "AWS S3 - Login failed", JOptionPane.WARNING_MESSAGE);
            } else if (e.getMessage().contains("400: Bad Request")) {
                JOptionPane.showMessageDialog(null, "Bucket Address incorrect", "AWS S3 - Access failed", JOptionPane.WARNING_MESSAGE);
            } else if (e.getMessage().contains("UnknownHostException")) {
                JOptionPane.showMessageDialog(null, "Connection failed.\nCheck your internet connection!", "AWS S3 - Access failed", JOptionPane.WARNING_MESSAGE);
            } else {
                e.printStackTrace();
            }
            throw e;
        }
        UIManager.put("FileChooser.readOnly", true);
    }

    private NioAmazonS3FileSystemView(String bucketAddress) throws AccessDeniedException {
        this(bucketAddress, "", "");
    }

    static NioAmazonS3FileSystemView getNioS3FileSystemView() {
        JTextField bucketAddressField = new JTextField(100) {{
            setMinimumSize(new Dimension(200, 20));
            setMaximumSize(new Dimension(200, 20));
        }};
        JTextField usernameField = new JTextField(100) {{
            setMinimumSize(new Dimension(200, 20));
            setMaximumSize(new Dimension(200, 20));
        }};
        JPasswordField passwordFiled = new JPasswordField(100) {{
            setMinimumSize(new Dimension(200, 20));
            setMaximumSize(new Dimension(200, 20));
        }};
        JPanel s3InputDialog = new JPanel();
        s3InputDialog.setPreferredSize(new Dimension(320, 80));
        Box bucketAddressBox = Box.createHorizontalBox();
        bucketAddressBox.add(new JLabel("(*)bucket address:") {{
            setMinimumSize(new Dimension(110, 20));
            setMaximumSize(new Dimension(110, 20));
        }});
        bucketAddressBox.add(Box.createHorizontalStrut(1)); // a spacer
        bucketAddressBox.add(bucketAddressField);
        Box usernameBox = Box.createHorizontalBox();
        usernameBox.add(new JLabel("username:") {{
            setMinimumSize(new Dimension(110, 20));
            setMaximumSize(new Dimension(110, 20));
        }});
        usernameBox.add(Box.createHorizontalStrut(1)); // a spacer
        usernameBox.add(usernameField);
        Box passwordBox = Box.createHorizontalBox();
        passwordBox.add(new JLabel("password:") {{
            setMinimumSize(new Dimension(110, 20));
            setMaximumSize(new Dimension(110, 20));
        }});
        passwordBox.add(Box.createHorizontalStrut(1)); // a spacer
        passwordBox.add(passwordFiled);
        Box dialogBox = Box.createVerticalBox();
        dialogBox.add(bucketAddressBox);
        dialogBox.add(Box.createVerticalStrut(5)); // a spacer
        dialogBox.add(usernameBox);
        dialogBox.add(Box.createVerticalStrut(5)); // a spacer
        dialogBox.add(passwordBox);
        dialogBox.add(Box.createVerticalStrut(1)); // a spacer
        s3InputDialog.add(dialogBox);
        int result = 0;
        do {
            try {
                result = JOptionPane.showConfirmDialog(null, s3InputDialog,
                        "AWS S3 - Access", JOptionPane.OK_CANCEL_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    String bucketAddress = bucketAddressField.getText();
                    String username = usernameField.getText();
                    String password = new String(passwordFiled.getPassword());
                    if (bucketAddress != null) {
                        if (username != null && !username.isEmpty() && !password.isEmpty()) {
                            return new NioAmazonS3FileSystemView(bucketAddress, username, password);
                        } else {
                            return new NioAmazonS3FileSystemView(bucketAddress);
                        }
                    }
                }
            } catch (AccessDeniedException ignored) {
            }
        } while (result == JOptionPane.OK_OPTION);
        return null;
    }

    /**
     * Determines if the given file is a root in the navigable tree(s).
     * Examples: Windows 98 has one root, the Desktop folder. DOS has one root
     * per drive letter, <code>C:\</code>, <code>D:\</code>, etc. Unix has one root,
     * the <code>"/"</code> directory.
     * <p>
     * The default implementation gets information from the <code>NioShellFolder</code> class.
     *
     * @param f a <code>File</code> object representing a directory
     * @return <code>true</code> if <code>f</code> is a root in the navigable tree.
     * @see #isFileSystemRoot
     */
    public boolean isRoot(File f) {
        return f != null && f.isAbsolute();
    }

    /**
     * Returns all root directories on this system. For example, on
     * AWS S3, this would be the products folders.
     */
    public File[] getRoots() {
        // Don't cache this array, because filesystem might change
        List<File> roots = new ArrayList<>();
        for (Path p : fs.getRootDirectories()) {
            roots.add(createFileSystemRoot(p));
        }
        return roots.toArray(new File[0]);
    }

    /**
     * Creates a new <code>File</code> object for <code>f</code> with correct
     * behavior for a file system root directory.
     *
     * @param p a <code>File</code> object representing a file system root
     *          directory, for example "/" on Unix or "C:\" on Windows.
     * @return a new <code>File</code> object
     * @since 1.4
     */
    private NioFile createFileSystemRoot(Path p) {
        return new AmazonS3FileSystemRoot(p);
    }

    /**
     * Providing default implementations for the remaining methods
     * because most OS file systems will likely be able to use this
     * code. If a given OS can't, override these methods in its
     * implementation.
     */
    public File getHomeDirectory() {
        File[] roots = getRoots();
        return (roots.length == 0) ? null : roots[0];
    }

    static class AmazonS3FileSystemRoot extends NioFile {
        AmazonS3FileSystemRoot(Path p) {
            super(p);
        }

        public boolean isDirectory() {
            return true;
        }

        public boolean isAbsolute() {
            return false;
        }

        public boolean exists() {
            return true;
        }

        @NotNull
        public String getName() {
            String name = super.getName();
            if (name.isEmpty()) {
                name = super.getPath();
            }
            return name;
        }

        @NotNull
        public String getPath() {
            return getName();
        }

    }

}

