package org.esa.snap.rcp.cli;

import org.esa.snap.rcp.session.SessionManager;
import org.netbeans.api.sendopts.CommandException;
import org.netbeans.spi.sendopts.Arg;
import org.netbeans.spi.sendopts.ArgsProcessor;
import org.netbeans.spi.sendopts.Description;
import org.netbeans.spi.sendopts.Env;
import org.openide.util.NbBundle;

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

}

