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
package org.esa.snap.netbeans.tile.cli;

import org.esa.snap.netbeans.tile.TileUtilities;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * CLI tool to generate the editor tile mode files into directory ./modes.
 * Will also output the required layer.xml content to stdout.
 *
 * @author Norman Fomferra
 */
public class TileModeGenerator {

    private static final String MODE_XML_TEMPLATE = "" +
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
        for (int row = 0; row < TileUtilities.MAX_TILE_ROW_COUNT; row++) {
            for (int col = 0; col < TileUtilities.MAX_TILE_COLUMN_COUNT; col++) {
                String modeName = String.format(TileUtilities.EDITOR_MODE_NAME_FORMAT, row, col);
                modeNames.add(modeName);

                File modeFile = new File(new File("modes"), modeName + ".wsmode");
                try (FileWriter fileWriter = new FileWriter(modeFile)) {
                    fileWriter.write(MODE_XML_TEMPLATE
                                             .replace("$NAME", modeName)
                                             .replace("$COL", col + "")
                                             .replace("$ROW", row + ""));
                }
            }
        }

        // Output entries to be pasted into your 'layer.xml'
        System.out.println("<folder name=\"Windows2\" >\n"
                                   + "    <folder name=\"Modes\" >");
        for (String modeName : modeNames) {
            System.out.printf("        <file name=\"%s.wsmode\" url=\"modes/%s.wsmode\" />\n", modeName, modeName);
        }
        System.out.println("    </folder>\n"
                                   + "</folder>");
    }

}
