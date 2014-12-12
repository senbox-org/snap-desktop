package org.esa.snap.gui.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class EditorModeGenerator {

    private static final int N = 16;
    private static final String TEMPLATE = "" +
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

    public static void main(String[] args) throws IOException {

        // Generate wsmode files into ./modes
        new File("modes").mkdir();
        List<String> modeNames = new ArrayList<>();
        for (int row = 0; row < N; row++) {
            for (int col = 0; col < N; col++) {
                String modeName = String.format(WindowUtilities.EDITOR_MODE_NAME_FORMAT, row, col);
                modeNames.add(modeName);

                File modeFile = new File(new File("modes"), modeName + ".wsmode");
                try (FileWriter fileWriter = new FileWriter(modeFile)) {
                    fileWriter.write(TEMPLATE.replace("$NAME", modeName).replace("$COL", col + "").replace("$ROW", row + ""));
                }
            }
        }

        // Output entries to pasted into 'layer.xml'
        System.out.println("<folder name=\"Windows2\" >\n"
                                   + "    <folder name=\"Modes\" >");
        for (String modeName : modeNames) {
            System.out.printf("        <file name=\"%s.wsmode\" url=\"modes/%s.wsmode\" />\n", modeName, modeName);
        }
        System.out.println("    </folder>\n"
                                   + "</folder>");
    }

}
