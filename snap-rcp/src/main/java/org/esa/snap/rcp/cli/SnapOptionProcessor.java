package org.esa.snap.rcp.cli;

import org.esa.snap.python.PyBridge;
import org.netbeans.api.sendopts.CommandException;
import org.netbeans.spi.sendopts.Env;
import org.netbeans.spi.sendopts.Option;
import org.netbeans.spi.sendopts.OptionProcessor;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * SNAP Desktop command-line options:
 * <ul>
 *     <li>{@code python <python-interpreter> [<snappy-python-module-dir>]} Configures the SNAP Java-(C)Python adapter 'snappy'</li>
 *     <li><i>add more options here...</i></li>
 * </ul>
 *
 * @author Norman Fomferra
 */
@ServiceProvider(service = OptionProcessor.class)
@NbBundle.Messages({
        "LBL_PythonOption_Name=python <python-interpreter> [<snappy-python-module-dir>]",
        "LBL_PythonOption_Description=Configures the SNAP Java-Python adapter 'snappy'",
})
public class SnapOptionProcessor extends OptionProcessor {
    private final Option pythonOption;

    public SnapOptionProcessor() {
        this.pythonOption = createPythonOption();
        // add more options here...
    }

    private static Option createPythonOption() {
        Option pythonOption = Option.additionalArguments(Option.NO_SHORT_NAME, "python");
        Option.displayName(pythonOption, "org.esa.snap.rcp.cli.Bundle", "LBL_PythonOption_Name");
        Option.shortDescription(pythonOption, "org.esa.snap.rcp.cli.Bundle", "LBL_PythonOption_Description");
        return pythonOption;
    }

    @Override
    protected Set<Option> getOptions() {
        HashSet<Option> options = new HashSet<>();
        options.add(pythonOption);
        // add more options here...
        return options;
    }

    @Override
    protected void process(Env env, Map<Option, String[]> optionValues) throws CommandException {
        if (optionValues.containsKey(pythonOption)) {
            processPythonOption(optionValues);
        }
        // handle more options here...
    }

    private void processPythonOption(Map<Option, String[]> optionValues) throws CommandException {
        String[] args = optionValues.get(pythonOption);
        Path pythonExecutable = null;
        Path pythonModuleInstallDir = null;
        Boolean forcePythonConfig = true;

        if (args.length >= 1) {
            pythonExecutable = Paths.get(args[0]);
        }
        if (args.length >= 2) {
            pythonModuleInstallDir = Paths.get(args[1]);
        }
        if (args.length >= 3) {
            forcePythonConfig = "true".equalsIgnoreCase(args[2]);
        }

        if (pythonExecutable == null) {
            throw new CommandException(-200, "Python interpreter executable must be given");
        }

        if (!Files.exists(pythonExecutable)) {
            throw new CommandException(-201, "Python interpreter executable not found: " + pythonExecutable);
        }

        try {
            System.out.flush();
            System.out.println("Configuring SNAP-Python interface...");
            Path snappyDir = PyBridge.installPythonModule(pythonExecutable, pythonModuleInstallDir, forcePythonConfig);
            System.out.flush();
            System.out.printf("Done. The SNAP-Python interface is located in '%s'%n", snappyDir);
            System.out.printf("When using SNAP from Python, either do: sys.path.append('%s')%n", snappyDir.getParent().toString().replace("\\", "\\\\"));
            System.out.printf("or copy the '%s' module into your Python's 'site-packages' directory.%n", snappyDir.getFileName());
            System.out.flush();
        } catch (IOException e) {
            e.printStackTrace(System.out);
            System.out.flush();
            throw new CommandException(-202, "Python configuration error: " + e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace(System.out);
            System.out.flush();
            throw new CommandException(-203, "Python configuration internal error: " + t.getMessage());
        }
    }
}
