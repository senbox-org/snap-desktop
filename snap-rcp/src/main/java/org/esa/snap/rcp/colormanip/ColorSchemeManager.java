package org.esa.snap.rcp.colormanip;

import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.datamodel.ColorPaletteInfo;
import org.esa.snap.core.datamodel.ColorSchemeDefaults;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class ColorSchemeManager {

    public boolean isjComboBoxShouldFire() {
        return jComboBoxShouldFire;
    }

    public void setjComboBoxShouldFire(boolean jComboBoxShouldFire) {
        this.jComboBoxShouldFire = jComboBoxShouldFire;
    }


    private final String STANDARD_SCHEME_COMBO_BOX_FIRST_ENTRY_NAME = "-- none --";

    private ArrayList<ColorPaletteInfo> colorSchemeInfos = new ArrayList<ColorPaletteInfo>();
    private ArrayList<ColorPaletteInfo> colorSchemeLutInfos = new ArrayList<ColorPaletteInfo>();

    private File colorSchemesFile = null;
    private File colorSchemeLutFile = null;

    private JComboBox jComboBox = null;
    private boolean jComboBoxShouldFire = true;

    private ColorPaletteInfo jComboBoxFirstEntryColorSchemeInfo = null;

    private File colorPaletteDir = null;
    private String jComboBoxFirstEntryName = null;


    private boolean initialized = false;
    static ColorSchemeManager manager = new ColorSchemeManager();

    public static ColorSchemeManager getDefault() {
        return manager;
    }


    public ColorSchemeManager() {
    }


    public void init(File palettesDir) {
        if (!initialized) {
            this.colorPaletteDir = palettesDir;

            colorSchemesFile = new File(this.colorPaletteDir, ColorSchemeDefaults.COLOR_SCHEMES_FILENAME);
            colorSchemeLutFile = new File(this.colorPaletteDir, ColorSchemeDefaults.COLOR_SCHEME_LUT_FILENAME);

            if (colorSchemesFile.exists() && colorSchemeLutFile.exists()) {
                initColorSchemeInfos();


                initComboBox();
                initColorSchemeLut();

                reset();
            }

            initialized = true;
        }
    }


    public ColorSchemeManager(File colorPaletteDir) {
        this.colorPaletteDir = colorPaletteDir;

        colorSchemesFile = new File(this.colorPaletteDir, ColorSchemeDefaults.COLOR_SCHEMES_FILENAME);
        colorSchemeLutFile = new File(this.colorPaletteDir, ColorSchemeDefaults.COLOR_SCHEME_LUT_FILENAME);

        if (colorSchemesFile.exists() && colorSchemeLutFile.exists()) {

            setjComboBoxFirstEntryName(STANDARD_SCHEME_COMBO_BOX_FIRST_ENTRY_NAME);

            initColorSchemeInfos();


            initComboBox();
            initColorSchemeLut();

            reset();
        }
    }


    private void initComboBox() {

        Object[] colorSchemeInfosArray = colorSchemeInfos.toArray();

        final String[] toolTipsArray = new String[colorSchemeInfos.size()];

        int i = 0;
        for (ColorPaletteInfo colorSchemeInfo : colorSchemeInfos) {
            toolTipsArray[i] = colorSchemeInfo.getDescription();
            i++;
        }

        final Boolean[] enabledArray = new Boolean[colorSchemeInfos.size()];

        i = 0;
        for (ColorPaletteInfo colorSchemeInfo : colorSchemeInfos) {
            enabledArray[i] = colorSchemeInfo.isEnabled();
            i++;
        }

        final ColorSchemeManager.MyComboBoxRenderer myComboBoxRenderer = new ColorSchemeManager.MyComboBoxRenderer();
        myComboBoxRenderer.setTooltipList(toolTipsArray);
        myComboBoxRenderer.setEnabledList(enabledArray);

        jComboBox = new JComboBox(colorSchemeInfosArray);
        jComboBox.setRenderer(myComboBoxRenderer);
        jComboBox.setEditable(false);
        jComboBox.setMaximumRowCount(20);
        if (colorSchemeLutFile != null) {
            jComboBox.setToolTipText("To modify see file: " + colorPaletteDir + "/" + colorSchemeLutFile.getName());
        }

    }


    private boolean initColorSchemeInfos() {

        setjComboBoxFirstEntryName(STANDARD_SCHEME_COMBO_BOX_FIRST_ENTRY_NAME);
        jComboBoxFirstEntryColorSchemeInfo = new ColorPaletteInfo(getjComboBoxFirstEntryName(), null, null, null, 0, 0, false, true, true, null, null, null, colorPaletteDir);
        colorSchemeInfos.add(jComboBoxFirstEntryColorSchemeInfo);

        ArrayList<String> lines = readFileIntoArrayList(colorSchemesFile);

        int i = 0;
        for (String line : lines) {
            line.trim();
            if (!line.startsWith("#")) {
                String[] values = line.split(":");

                if (values != null) {
                    boolean validEntry = true;
                    boolean fieldsInitialized = false;

                    String id = null;
                    Double min = null;
                    Double max = null;
                    boolean logScaled = false;
                    String cpdFileNameStandard = null;
                    String cpdFileNameColorBlind = null;
                    String colorBarTitle = null;
                    String colorBarLabels = null;
                    String description = null;
                    String rootSchemeName = null;

                    File standardCpdFile = null;
                    File colorBlindCpdFile = null;
                    File cpdFile = null;

                    boolean overRide = false;

                    //        ID    MIN   MAX     LOG_SCALE  CPD_FILENAME   CPD_FILENAME(COLORBLIND)  COLORBAR_TITLE    COLORBAR_LABELS     DESCRIPTION


                    if (values.length >= 8) {

                        if (values.length >= 9) {
                            description = values[8].trim();
                        } else {
                            description = "";
                        }

                        if (values.length >= 8) {
                            colorBarLabels = values[7].trim();
                        } else {
                            colorBarLabels = "";
                        }

                        if (values.length >= 7) {
                            colorBarTitle = values[6].trim();
                        } else {
                            colorBarTitle = "";
                        }

                        id = values[0].trim();
                        String minStr = values[1].trim();
                        String maxStr = values[2].trim();
                        String logScaledStr = values[3].trim();
                        cpdFileNameStandard = values[4].trim();
                        cpdFileNameColorBlind = values[5].trim();


                        if (cpdFileNameStandard == null ||
                                cpdFileNameStandard.length() == 0 ||
                                ColorSchemeDefaults.NULL_ENTRY.toLowerCase().equals(cpdFileNameStandard.toLowerCase())) {
                            cpdFileNameStandard = ColorSchemeDefaults.DEFAULT_CPD_FILENAME;
                        }


                        if (minStr.length() > 0 && !ColorSchemeDefaults.NULL_ENTRY.toLowerCase().equals(minStr.toLowerCase())) {
                            min = Double.valueOf(minStr);
                        } else {
                            min = ColorSchemeDefaults.DOUBLE_NULL;
                        }

                        if (maxStr.length() > 0 && !ColorSchemeDefaults.NULL_ENTRY.toLowerCase().equals(maxStr.toLowerCase())) {
                            max = Double.valueOf(maxStr);
                        } else {
                            max = ColorSchemeDefaults.DOUBLE_NULL;
                        }

                        logScaled = false;
                        if (logScaledStr != null && logScaledStr.length() > 0 && logScaledStr.toLowerCase().equals("true")) {
                            logScaled = true;
                        }


                        if (id != null && id.length() > 0) {
                            fieldsInitialized = true;
                        }
                    }

                    if (fieldsInitialized) {

                        if (!testMinMax(min, max, logScaled)) {
                            validEntry = false;
                        }

                        if (validEntry) {
                            standardCpdFile = new File(colorPaletteDir, cpdFileNameStandard);

                            if (!standardCpdFile.exists()) {
                                validEntry = false;
                                //  standardCpdFile = new File(dirName, DEFAULT_CPD_FILENAME);
                            }
                        }

                        if (validEntry) {
                            colorBlindCpdFile = new File(colorPaletteDir, cpdFileNameColorBlind);

                            if (!colorBlindCpdFile.exists()) {
                                validEntry = false;
                            }
                        }

                        if (validEntry) {
                            cpdFile = standardCpdFile;
                        }


                        if (validEntry) {
                            ColorPaletteInfo colorPaletteInfo = null;

                            ColorPaletteDef colorPaletteDef;
                            try {
                                colorPaletteDef = ColorPaletteDef.loadColorPaletteDef(cpdFile);
                                colorPaletteInfo = new ColorPaletteInfo(id, rootSchemeName, description, cpdFileNameStandard, min, max, logScaled, overRide, true, cpdFileNameColorBlind, colorBarTitle, colorBarLabels, colorPaletteDir);

                            } catch (IOException e) {
                                //        colorPaletteInfo = new ColorPaletteInfo(name, description);
                            }


                            if (colorPaletteInfo != null) {
                                if (overRide) {
                                    // look for previous name which user may be overriding and delete it in the colorPaletteInfo object
                                    ColorPaletteInfo colorPaletteInfoToDelete = null;
                                    for (ColorPaletteInfo storedColorPaletteInfo : colorSchemeInfos) {
                                        if (storedColorPaletteInfo.getName().equals(id)) {
                                            colorPaletteInfoToDelete = storedColorPaletteInfo;
                                            break;
                                        }
                                    }
                                    if (colorPaletteInfoToDelete != null) {
                                        colorSchemeInfos.remove(colorPaletteInfoToDelete);
                                    }
                                }
                                colorSchemeInfos.add(colorPaletteInfo);
                            }
                        }
                    }
                }
            }
        }

        return true;
    }


    private void initColorSchemeLut() {

        ArrayList<String> lines = readFileIntoArrayList(colorSchemeLutFile);

        int i = 0;
        for (String line : lines) {
            line.trim();
            if (!line.startsWith("#")) {
                String[] values = line.split(":");

                if (values != null) {
                    boolean validEntry = false;
                    boolean fieldsInitialized = false;

                    String name = null;
                    Double minVal = null;
                    Double maxVal = null;
                    boolean logScaled = false;
                    String cpdFileNameStandard = null;
                    boolean overRide = false;
                    String description = null;
                    String rootSchemeName = null;
                    String cpdFileNameColorBlind = null;
                    String colorBarTitle = null;
                    String colorBarLabels = null;

                    String desiredScheme = null;

                    File standardCpdFile = null;
                    File colorBlindCpdFile = null;
                    File cpdFile = null;



                    if (values.length >= 1) {
                        name = values[0].trim();


                        if (values.length >= 2) {
                            desiredScheme = values[1].trim();
                            if (desiredScheme.length() == 0) {
                                desiredScheme = name;
                            }
                        } else {
                            desiredScheme = name;
                        }


                        if (values.length >= 3) {
                            description = values[2].trim();
                            if (description.length() == 0) {
                                description = null;
                            }
                        }

                        if (name != null && name.length() > 0 && desiredScheme != null && desiredScheme.length() > 0) {

                            for (ColorPaletteInfo storedColorPaletteInfo : colorSchemeInfos) {
                                if (storedColorPaletteInfo.getName().equals(desiredScheme)) {
                                    if (!fieldsInitialized ||
                                            (fieldsInitialized && overRide)) {

                                        cpdFileNameStandard = storedColorPaletteInfo.getCpdFilenameStandard();
                                        minVal = storedColorPaletteInfo.getMinValue();
                                        maxVal = storedColorPaletteInfo.getMaxValue();
                                        logScaled = storedColorPaletteInfo.isLogScaled();
                                        rootSchemeName = desiredScheme;
                                        cpdFileNameColorBlind = storedColorPaletteInfo.getCpdFilenameColorBlind();
                                        colorBarTitle = storedColorPaletteInfo.getColorBarTitle();
                                        colorBarLabels = storedColorPaletteInfo.getColorBarLabels();
                                        description = storedColorPaletteInfo.getDescription();

                                        fieldsInitialized = true;
                                    }
                                }
                            }
                        }
                    }


                    if (fieldsInitialized) {

                        colorBlindCpdFile = new File(colorPaletteDir, cpdFileNameColorBlind);
                        standardCpdFile = new File(colorPaletteDir, cpdFileNameStandard);

                        cpdFile = standardCpdFile;

                        ColorPaletteInfo colorPaletteInfo = null;
                        ColorPaletteDef colorPaletteDef;


                        try {
                            colorPaletteDef = ColorPaletteDef.loadColorPaletteDef(cpdFile);
                            colorPaletteInfo = new ColorPaletteInfo(name, rootSchemeName, description, cpdFileNameStandard, minVal, maxVal, logScaled, overRide, true, cpdFileNameColorBlind, colorBarTitle, colorBarLabels, colorPaletteDir);

                        } catch (IOException e) {
                            //        colorPaletteInfo = new ColorPaletteInfo(name, description);
                        }


                        if (colorPaletteInfo != null) {
                            colorSchemeLutInfos.add(colorPaletteInfo);
                        }
                    }
                }
            }
        }
    }


    private boolean testMinMax(double min, double max, boolean isLogScaled) {
        boolean checksOut = true;

        if (min != ColorSchemeDefaults.DOUBLE_NULL && max != ColorSchemeDefaults.DOUBLE_NULL) {
            if (min == max) {
                checksOut = false;
            }
        }


        if (min != ColorSchemeDefaults.DOUBLE_NULL && max != ColorSchemeDefaults.DOUBLE_NULL) {
            if (isLogScaled && min == 0) {
                checksOut = false;
            }
        }

        return checksOut;
    }

    public void setSelected(ColorPaletteInfo colorPaletteInfo) {
        // loop through and find rootScheme
        if (jComboBox != null) {
            if (colorPaletteInfo != null) {
                if (colorPaletteInfo.getRootName() != null) {
                    for (ColorPaletteInfo storedColorPaletteInfo : colorSchemeInfos) {
                        if (colorPaletteInfo.getRootName().equals(storedColorPaletteInfo.getName())) {
                            jComboBox.setSelectedItem(storedColorPaletteInfo);
                        }
                    }
                } else {
                    jComboBox.setSelectedItem(colorPaletteInfo);
                }
            } else {
                reset();
            }
        }

    }

    public void reset() {
        if (jComboBox != null) {
            jComboBox.setSelectedItem(jComboBoxFirstEntryColorSchemeInfo);
        }
    }

    public boolean isSchemeSet() {
        if (jComboBox != null && jComboBoxFirstEntryColorSchemeInfo != jComboBox.getSelectedItem()) {
            return true;
        }

        return false;
    }


    public ColorPaletteInfo setSchemeName(String schemeName) {

        if (schemeName != null) {
            for (ColorPaletteInfo colorPaletteInfo : colorSchemeLutInfos) {
                if (schemeName.trim().equals(colorPaletteInfo.getName().trim())) {
                    jComboBox.setSelectedItem(colorPaletteInfo);
                    return colorPaletteInfo;
                }
            }
        }

        return null;
    }


    public ArrayList<String> readFileIntoArrayList(File file) {
        String lineData;
        ArrayList<String> fileContents = new ArrayList<String>();
        BufferedReader moFile = null;
        try {
            moFile = new BufferedReader(new FileReader(file));
            while ((lineData = moFile.readLine()) != null) {

                fileContents.add(lineData);
            }
        } catch (IOException e) {
            ;
        } finally {
            try {
                moFile.close();
            } catch (Exception e) {
                //Ignore
            }
        }
        return fileContents;
    }


    public JComboBox getjComboBox() {
        return jComboBox;
    }

    public ArrayList<ColorPaletteInfo> getColorSchemeLutInfos() {
        return colorSchemeLutInfos;
    }


    class MyComboBoxRenderer extends BasicComboBoxRenderer {

        private String[] tooltips;
        private Boolean[] enabledList;

        public Component getListCellRendererComponent(JList list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {

            if (index >= 0 && index < enabledList.length) {
                setEnabled(enabledList[index]);
                setFocusable(enabledList[index]);
            }


            if (isSelected) {
                setBackground(Color.blue);

                if (index >= 0 && index < tooltips.length) {
                    list.setToolTipText(tooltips[index]);
                }

                if (index >= 0 && index < enabledList.length) {

                    if (enabledList[index]) {
                        setForeground(Color.white);
                    } else {
                        setForeground(Color.gray);
                    }
                }

            } else {
                setBackground(Color.white);

                if (index >= 0 && index < enabledList.length) {
                    if (enabledList[index] == true) {
                        setForeground(Color.black);
                    } else {
                        setForeground(Color.gray);
                    }

                }
            }


            if (index == 0) {
                setForeground(Color.black);
            }

            setFont(list.getFont());
            setText((value == null) ? "" : value.toString());


            return this;
        }

        public void setTooltipList(String[] tooltipList) {
            this.tooltips = tooltipList;
        }


        public void setEnabledList(Boolean[] enabledList) {
            this.enabledList = enabledList;
        }
    }


    public String getjComboBoxFirstEntryName() {
        return jComboBoxFirstEntryName;
    }

    private void setjComboBoxFirstEntryName(String jComboBoxFirstEntryName) {
        this.jComboBoxFirstEntryName = jComboBoxFirstEntryName;
    }

}
