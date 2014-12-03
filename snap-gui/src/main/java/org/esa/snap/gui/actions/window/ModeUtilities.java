package org.esa.snap.gui.actions.window;

import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

class ModeUtilities {

    private static final String MODE_NAME_FORMAT = "editor_r%dc%d";

    public static boolean openInEditorMode(int rowIndex, int colIndex, TopComponent editorWindow) {
        String modeName = String.format(MODE_NAME_FORMAT, rowIndex, colIndex);
        return openInEditorMode(modeName, editorWindow);
    }

    public static boolean openInEditorMode(String modeName, TopComponent editorWindow) {
        Mode editorMode = WindowManager.getDefault().findMode(modeName);
        if (editorMode != null) {
            if (!Arrays.asList(editorMode.getTopComponents()).contains(editorWindow)) {
                if (editorMode.dockInto(editorWindow)) {
                    System.out.printf(">>> win '%s' docked: %s%n", editorWindow.getDisplayName(), editorMode.getName());
                    editorWindow.open();
                    return true;
                } else {
                    System.out.printf(">>> win '%s' NOT docked: %s%n", editorWindow.getDisplayName(), editorMode.getName());
                    return false;
                }
            } else {
                System.out.printf(">>> win '%s' already in: %s%n", editorWindow.getDisplayName(), editorMode.getName());
                editorWindow.open();
                return true;
            }
        } else {
            System.out.printf(">>> mode NOT found: %s%n", modeName);
            return false;
        }
    }

    public static void main(String[] args) throws IOException {
        final  int N = 16;
        final  String TEMPLATE = "" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<mode version=\"2.4\">\n" +
                "    <name unique=\"$NAME\"/>\n" +
                "    <kind type=\"editor\"/>\n" +
                "    <state type=\"joined\"/>\n" +
                "    <constraints>\n" +
                "        <path orientation=\"vertical\" number=\"$ROW\" weight=\"0.5\"/>\n" +
                "        <path orientation=\"horizontal\" number=\"$COL\" weight=\"0.5\"/>\n" +
                "    </constraints>\n" +
                "    <bounds x=\"0\" y=\"0\" width=\"400\" height=\"400\"/>\n" +
                "    <frame state=\"0\"/>\n" +
                "    <empty-behavior permanent=\"true\"/>\n" +
                "</mode>\n";

        new File("modes").mkdir();

        System.out.println("" +
                                   "<folder name=\"Windows2\" >\n" +
                                   "    <folder name=\"Modes\" >");

        for (int row = 0; row < N; row++) {
            for (int col = 0; col < N; col++) {
                String modeName = String.format(MODE_NAME_FORMAT, row, col);
                File modeFile = new File(new File("modes"), modeName + ".wsmode");
                try (FileWriter fileWriter = new FileWriter(modeFile)) {
                    fileWriter.write(TEMPLATE.replace("$NAME", modeName).replace("$COL", col + "").replace("$ROW", row + ""));
                }
                System.out.printf("        <file name=\"%s.wsmode\" url=\"modes/%s.wsmode\" />\n", modeName, modeName);
            }
        }

        System.out.println("" +
                                   "    </folder>\n" +
                                   "</folder>");
    }

    public static int[] getBestSubdivisionIntoSquares(int windowCount, int maxRowCount, int maxColCount) {
        double bestDeltaValue = Double.POSITIVE_INFINITY;
        int bestRowCount = -1;
        int bestColCount = -1;
        for (int rowCount = 1; rowCount <= Math.max(windowCount, maxRowCount); rowCount++) {
            for (int colCount = 1; colCount <= Math.max(windowCount, maxColCount); colCount++) {
                if (colCount * rowCount >= windowCount && colCount * rowCount <= 2 * windowCount) {
                    double deltaRatio = Math.abs(1.0 - rowCount / (double) colCount);
                    double deltaCount = Math.abs(1.0 - (colCount * rowCount) / ((double) windowCount));
                    double deltaValue = deltaRatio + deltaCount;
                    if (deltaValue < bestDeltaValue) {
                        bestDeltaValue = deltaValue;
                        bestRowCount = rowCount;
                        bestColCount = colCount;
                    }
                }
            }
        }
        return new int[]{bestRowCount, bestColCount};
    }

}
