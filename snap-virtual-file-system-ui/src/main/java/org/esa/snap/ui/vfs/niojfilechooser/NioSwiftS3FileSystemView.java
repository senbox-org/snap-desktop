package org.esa.snap.ui.vfs.niojfilechooser;

import org.esa.snap.core.dataio.NioFile;
import org.esa.snap.core.dataio.vfs.remote.object_storage.swift.SwiftFileSystemProvider;
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
 * FileSystemView component for OpenStack Swift Object Storage VFS.
 * FileSystemView that handles some specific OpenStack Swift Virtual File System concepts.
 *
 * @author Adrian Drăghici
 */
class NioSwiftS3FileSystemView extends NioFileSystemView {

    private FileSystem fs;

    private NioSwiftS3FileSystemView(String container, String domain, String projectId, String user, String password) throws AccessDeniedException {
        SwiftFileSystemProvider.setupConnectionData(container, domain, projectId, user, password);
        try {
            fs = SwiftFileSystemProvider.getSwiftFileSystem();
        } catch (AccessDeniedException e) {
            if (e.getMessage().contains("400: Bad Request")) {
                JOptionPane.showMessageDialog(null, "Wrong data given.", "EO Cloud OpenStack Swift - Access failed", JOptionPane.WARNING_MESSAGE);
            } else if (e.getMessage().contains("401: Unauthorized")) {
                JOptionPane.showMessageDialog(null, "The authentication credentials are not valid.", "EO Cloud OpenStack Swift - Login failed", JOptionPane.WARNING_MESSAGE);
            } else if (e.getMessage().contains("403: Forbidden")) {
                JOptionPane.showMessageDialog(null, "The identity was successfully authenticated but it is not authorized to perform the requested action.", "EO Cloud OpenStack Swift - Login failed", JOptionPane.WARNING_MESSAGE);
            } else if (e.getMessage().contains("404: Not Found")) {
                JOptionPane.showMessageDialog(null, "Container not found.", "EO Cloud OpenStack Swift - Login failed", JOptionPane.WARNING_MESSAGE);
            } else if (e.getMessage().contains("UnknownHostException")) {
                JOptionPane.showMessageDialog(null, "Connection failed.\nCheck your internet connection!", "EO Cloud OpenStack Swift - Access failed", JOptionPane.WARNING_MESSAGE);
            } else {
                e.printStackTrace();
            }
            throw e;
        }
        UIManager.put("FileChooser.readOnly", true);
    }

    private NioSwiftS3FileSystemView(String domain, String projectId, String user, String password) throws AccessDeniedException {
        this("", domain, projectId, user, password);
    }

    private NioSwiftS3FileSystemView(String container) throws AccessDeniedException {
        this(container, "", "", "", "");
    }

    private NioSwiftS3FileSystemView() throws AccessDeniedException {
        this("", "", "", "", "");
    }

    static NioSwiftS3FileSystemView getNioSwiftFileSystemView() {
        JTextField containerField = new JTextField(100) {{
            setMinimumSize(new Dimension(200, 20));
            setMaximumSize(new Dimension(200, 20));
        }};
        JTextField domainField = new JTextField(100) {{
            setMinimumSize(new Dimension(200, 20));
            setMaximumSize(new Dimension(200, 20));
        }};
        JTextField projectIdField = new JTextField(100) {{
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
        JPanel swiftInputDialog = new JPanel();
        swiftInputDialog.setPreferredSize(new Dimension(300, 130));
        Box containerBox = Box.createHorizontalBox();
        containerBox.add(new JLabel("container:") {{
            setMinimumSize(new Dimension(70, 20));
            setMaximumSize(new Dimension(80, 20));
        }});
        containerBox.add(Box.createHorizontalStrut(1)); // a spacer
        containerBox.add(containerField);
        Box domainBox = Box.createHorizontalBox();
        domainBox.add(new JLabel("domain:") {{
            setMinimumSize(new Dimension(70, 20));
            setMaximumSize(new Dimension(80, 20));
        }});
        domainBox.add(Box.createHorizontalStrut(1)); // a spacer
        domainBox.add(domainField);
        Box projectIdBox = Box.createHorizontalBox();
        projectIdBox.add(new JLabel("project ID:") {{
            setMinimumSize(new Dimension(70, 20));
            setMaximumSize(new Dimension(80, 20));
        }});
        projectIdBox.add(Box.createHorizontalStrut(1)); // a spacer
        projectIdBox.add(projectIdField);
        Box userBox = Box.createHorizontalBox();
        userBox.add(new JLabel("user:") {{
            setMinimumSize(new Dimension(70, 20));
            setMaximumSize(new Dimension(80, 20));
        }});
        userBox.add(Box.createHorizontalStrut(1)); // a spacer
        userBox.add(usernameField);
        Box passwordBox = Box.createHorizontalBox();
        passwordBox.add(new JLabel("password:") {{
            setMinimumSize(new Dimension(70, 20));
            setMaximumSize(new Dimension(80, 20));
        }});
        passwordBox.add(Box.createHorizontalStrut(1)); // a spacer
        passwordBox.add(passwordFiled);
        Box dialogBox = Box.createVerticalBox();
        dialogBox.add(containerBox);
        dialogBox.add(Box.createVerticalStrut(5)); // a spacer
        dialogBox.add(domainBox);
        dialogBox.add(Box.createVerticalStrut(5)); // a spacer
        dialogBox.add(projectIdBox);
        dialogBox.add(Box.createVerticalStrut(5)); // a spacer
        dialogBox.add(userBox);
        dialogBox.add(Box.createVerticalStrut(5)); // a spacer
        dialogBox.add(passwordBox);
        swiftInputDialog.add(dialogBox);
        int result = 0;
        do {
            try {
                result = JOptionPane.showConfirmDialog(null, swiftInputDialog,
                        "EO Cloud OpenStack Swift - Access", JOptionPane.OK_CANCEL_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    String container = containerField.getText();
                    String domain = domainField.getText();
                    String projectId = projectIdField.getText();
                    String user = usernameField.getText();
                    String password = new String(passwordFiled.getPassword());
                    if (container != null && !container.isEmpty()) {
                        if (domain != null && !domain.isEmpty() && projectId != null && !projectId.isEmpty() && user != null && !user.isEmpty() && !password.isEmpty()) {
                            return new NioSwiftS3FileSystemView(container, domain, projectId, user, password);
                        } else {
                            return new NioSwiftS3FileSystemView(container);
                        }
                    } else {
                        if (domain != null && !domain.isEmpty() && projectId != null && !projectId.isEmpty() && user != null && !user.isEmpty() && !password.isEmpty()) {
                            return new NioSwiftS3FileSystemView(domain, projectId, user, password);
                        } else {
                            return new NioSwiftS3FileSystemView();
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
     * EO Cloud OpenStack Swift, this would be the products folders.
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
        return new OpenStackSwiftFileSystemRoot(p);
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

    static class OpenStackSwiftFileSystemRoot extends NioFile {
        OpenStackSwiftFileSystemRoot(Path p) {
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

