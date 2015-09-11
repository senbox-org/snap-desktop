package org.esa.snap.rcp.cli;

import org.esa.snap.python.PyBridge;
import org.esa.snap.rcp.session.SessionManager;
import org.netbeans.api.sendopts.CommandException;
import org.netbeans.spi.sendopts.Arg;
import org.netbeans.spi.sendopts.ArgsProcessor;
import org.netbeans.spi.sendopts.Description;
import org.netbeans.spi.sendopts.Env;
import org.openide.util.NbBundle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Extra SNAP Desktop command-line arguments:
 * <ul>
 * <li>{@code --open  [<session-file>] [<file-1> <file-2> ...]} </li>
 * <li>{@code --python <python-interpreter> [<snappy-python-module-dir>]}</li>
 * </ul>
 *
 * @author Norman Fomferra
 */
@NbBundle.Messages({
        "TXT_OpenOption_Description=Open SNAP session file (*.snap) or any number of EO data product files",
        "TXT_PythonOption_Description=Configure the SNAP Java-Python adapter 'snappy': python <python-interpreter> [<snappy-python-module-dir>]",
})
public class SnapArgsProcessor implements ArgsProcessor {

    @Arg(longName = "open")
    @Description(shortDescription = "#TXT_OpenOption_Description")
    public String[] openArgs;

    @Arg(longName = "python")
    @Description(shortDescription = "#TXT_PythonOption_Description")
    public String[] pythonArgs;

    public void process(Env env) throws CommandException {

        if (openArgs != null) {
            processOpen(openArgs);
        }

        if (pythonArgs != null) {
            processPython(pythonArgs);
        }
    }

    private static void processOpen(String[] args) throws CommandException {
        int errorExitCode = 100;

        Path sessionFile = null;
        List<Path> fileList = new ArrayList<>();

        for (String arg : args) {
            Path file = Paths.get(arg);
            if (Files.exists(file)) {
                if (file.toFile() != null && SessionManager.getDefault().getSessionFileFilter().accept(file.toFile())) {
                    if (sessionFile != null) {
                        throw new CommandException(errorExitCode, "Only a single SNAP session file can be given.");
                    }
                    sessionFile = file;
                } else {
                    fileList.add(file);
                }
            } else {
                System.err.println("File not found: " + file);
            }
        }

        SnapArgs.getDefault().setSessionFile(sessionFile);
        SnapArgs.getDefault().setFileList(fileList);
    }

    private static void processPython(String[] args) throws CommandException {
        int errorExitCode = 200;

        Path pythonExecutable = null;
        Path pythonModuleInstallDir = null;

        if (args.length >= 1) {
            pythonExecutable = Paths.get(args[0]);
        }
        if (args.length >= 2) {
            pythonModuleInstallDir = Paths.get(args[1]);
        }

        if (pythonExecutable == null) {
            throw new CommandException(errorExitCode, "Python interpreter executable must be given");
        }

        if (!Files.exists(pythonExecutable)) {
            throw new CommandException(errorExitCode, "Python interpreter executable not found: " + pythonExecutable);
        }

        try {
            System.out.flush();
            System.out.println("Configuring SNAP-Python interface...");
            Path snappyDir = PyBridge.installPythonModule(pythonExecutable, pythonModuleInstallDir, true);
            System.out.flush();
            System.out.printf("Done. The SNAP-Python interface is located in '%s'%n", snappyDir);
            System.out.printf("When using SNAP from Python, either do: sys.path.append('%s')%n", snappyDir.getParent().toString().replace("\\", "\\\\"));
            System.out.printf("or copy the '%s' module into your Python's 'site-packages' directory.%n", snappyDir.getFileName());
            System.out.flush();
        } catch (IOException e) {
            e.printStackTrace(System.out);
            System.out.flush();
            throw new CommandException(errorExitCode, "Python configuration error: " + e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace(System.out);
            System.out.flush();
            throw new CommandException(errorExitCode, "Python configuration internal error: " + t.getMessage());
        }
    }
}

