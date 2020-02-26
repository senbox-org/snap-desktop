package org.esa.snap.rcp.colormanip;

import org.esa.snap.core.datamodel.ColorSchemeInfo;
import org.esa.snap.core.datamodel.ColorSchemeDefaults;
import org.esa.snap.core.util.PropertyMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static org.esa.snap.core.datamodel.ColorSchemeDefaults.*;

/**
 * Manages all the color schemes
 *
 * @author Daniel Knowles (NASA)
 * @author Bing Yang (NASA)
 * @date Jan 2020
 */

public class ColorSchemeManager {

    private ArrayList<ColorSchemeInfo> colorSchemeInfos = new ArrayList<ColorSchemeInfo>();

    private ArrayList<ColorSchemeInfo> colorSchemePrimaryInfos = new ArrayList<ColorSchemeInfo>();
    private ArrayList<ColorSchemeInfo> colorSchemeAdditionalInfos = new ArrayList<ColorSchemeInfo>();

    private ArrayList<ColorSchemeInfo> colorSchemeSortedInfos = new ArrayList<ColorSchemeInfo>();
    private ArrayList<ColorSchemeInfo> colorSchemeSortedVerboseInfos = new ArrayList<ColorSchemeInfo>();

    private ArrayList<ColorSchemeLookupInfo> colorSchemeLookupInfos = new ArrayList<ColorSchemeLookupInfo>();

    private File colorSchemesFile = null;
    private File colorSchemeLutFile = null;

    private JComboBox jComboBox = null;
    private boolean jComboBoxShouldFire = true;

    private final String COLOR_SCHEME_NONE_LABEL = "-- none --";
    private String jComboBoxNoneEntryName = null;
    private ColorSchemeInfo noneColorSchemeInfo = null;

    private File colorPaletteAuxDir = null;
    private File colorSchemesAuxDir = null;

    private boolean verbose = true;
    private boolean showDisabled = false;
    private boolean sortComboBox = false;

    private boolean initialized = false;

    private ColorSchemeInfo currentSelection = null;


    static ColorSchemeManager manager = new ColorSchemeManager();

    public static ColorSchemeManager getDefault() {
        return manager;
    }


    public ColorSchemeManager() {
        init();
    }


    public void init() {

        if (!initialized) {
            setjComboBoxShouldFire(false);

            Path getColorSchemesAuxDir = ColorSchemeUtils.getDirNameColorSchemes();
            if (getColorSchemesAuxDir != null) {
                this.colorSchemesAuxDir = getColorSchemesAuxDir.toFile();
                if (!colorSchemesAuxDir.exists()) {
                    return;
                }
            } else {
                return;
            }

            Path getColorPalettesAuxDir = ColorSchemeUtils.getDirNameColorPalettes();
            if (getColorPalettesAuxDir != null) {
                this.colorPaletteAuxDir = getColorPalettesAuxDir.toFile();
                if (!colorPaletteAuxDir.exists()) {
                    return;
                }
            } else {
                return;
            }

            colorSchemesFile = new File(this.colorSchemesAuxDir, ColorSchemeDefaults.COLOR_SCHEMES_FILENAME);
            colorSchemeLutFile = new File(this.colorSchemesAuxDir, ColorSchemeDefaults.COLOR_SCHEME_LUT_FILENAME);

            if (colorSchemesFile.exists() && colorSchemeLutFile.exists()) {
                initColorSchemeInfos();
                createSortedVerboseInfos();
                createSortedInfos();

                setjComboBoxNoneEntryName(COLOR_SCHEME_NONE_LABEL);
                setNoneColorSchemeInfo(new ColorSchemeInfo(getjComboBoxNoneEntryName(), false, getjComboBoxNoneEntryName(), null, null, 0, 0, false, true, null, null, null, colorPaletteAuxDir));
                colorSchemeInfos.add(0,getNoneColorSchemeInfo());
                colorSchemeSortedInfos.add(0, getNoneColorSchemeInfo());
                colorSchemeSortedVerboseInfos.add(0, getNoneColorSchemeInfo());


                initComboBox();
                initColorSchemeLookup();

                reset();
            }

            initialized = true;
            setjComboBoxShouldFire(true);
        }
    }


//    private void numericalSort() {
//        Collections.sort(colorSchemeInfos, new Comparator<ColorSchemeInfo>() {
//            @Override
//            public int compare(ColorSchemeInfo p1, ColorSchemeInfo p2) {
//                return p1.getEntryNumber() - p2.getEntryNumber(); // Ascending
//            }
//        });
//    }

    private void createSortedInfos() {

        for (ColorSchemeInfo colorSchemeInfo : colorSchemeInfos) {
            if (!colorSchemeInfo.isDivider()) {
                colorSchemeSortedInfos.add(colorSchemeInfo);
            }
        }

        Collections.sort(colorSchemeSortedInfos, new Comparator<ColorSchemeInfo>() {
            public int compare(ColorSchemeInfo s1, ColorSchemeInfo s2) {
                return s1.toString().compareToIgnoreCase(s2.toString());
            }
        });
    }


    private void createSortedVerboseInfos() {

//        String NULL_VALUE = "zzzzz";

        for (ColorSchemeInfo colorSchemeInfo : colorSchemeInfos) {
            if (!colorSchemeInfo.isDivider()) {
                colorSchemeSortedVerboseInfos.add(colorSchemeInfo);
            }
        }

        Collections.sort(colorSchemeSortedVerboseInfos, new Comparator<ColorSchemeInfo>() {
            public int compare(ColorSchemeInfo s1, ColorSchemeInfo s2) {
                return s1.toString(true).compareToIgnoreCase(s2.toString(true));
            }
        });

//
//        Collections.sort(colorSchemeSortedVerboseInfos, new Comparator<ColorSchemeInfo>() {
//
//            public int compare(ColorSchemeInfo o1, ColorSchemeInfo o2) {
//
//                String x1 = o1.getDisplayName();
//                String x2 = o2.getDisplayName();
//
//                if (x1 == null) {
//                    x1 = NULL_VALUE;
//                }
//                if (x2 == null) {
//                    x2 = NULL_VALUE;
//                }
//                int sComp = x1.compareToIgnoreCase(x2);
//
//                if (sComp != 0) {
//                    return sComp;
//                }
//
//                String y1 = o1.getName();
//                String y2 = o2.getName();
//                if (y1 == null) {
//                    y1 = NULL_VALUE;
//                }
//                if (y1 == null) {
//                    y1 = NULL_VALUE;
//                }
//
//                return y1.compareToIgnoreCase(y2);
//            }
//        });
    }


    public void checkPreferences(PropertyMap configuration) {

        boolean updateSchemeSelector = false;

        boolean useDisplayName = configuration.getPropertyBool(PROPERTY_SCHEME_VERBOSE_KEY, PROPERTY_SCHEME_VERBOSE_DEFAULT);
        if (isVerbose() != useDisplayName) {
            setVerbose(useDisplayName);
            updateSchemeSelector = true;
        }

        boolean sortComboBox = configuration.getPropertyBool(PROPERTY_SCHEME_SORT_KEY, PROPERTY_SCHEME_SORT_DEFAULT);
        if (isSortComboBox() != sortComboBox) {
            setSortComboBox(sortComboBox);
            updateSchemeSelector = true;
        }

        boolean showDisabled = configuration.getPropertyBool(PROPERTY_SCHEME_SHOW_DISABLED_KEY, PROPERTY_SCHEME_SHOW_DISABLED_DEFAULT);
        if (isShowDisabled() != showDisabled) {
            setShowDisabled(showDisabled);
            updateSchemeSelector = true;
        }

        if (updateSchemeSelector) {
            refreshComboBox();
        }


        ColorSchemeManager.getDefault().validateSelection();
    }


    private boolean isShowDisabled() {
        return showDisabled;
    }

    private void setShowDisabled(boolean showDisabled) {
        if (this.showDisabled != showDisabled) {
            this.showDisabled = showDisabled;
        }
    }


    private boolean isSortComboBox() {
        return sortComboBox;
    }


    private void setSortComboBox(boolean sortComboBox) {
        if (this.sortComboBox != sortComboBox) {
            this.sortComboBox = sortComboBox;
        }
    }


    private void refreshComboBox() {
        boolean shouldFire = isjComboBoxShouldFire();
        setjComboBoxShouldFire(false);
        ColorSchemeInfo selectedColorSchemeInfo = (ColorSchemeInfo) jComboBox.getSelectedItem();
        if (selectedColorSchemeInfo != null && selectedColorSchemeInfo.isEnabled()) {
            setCurrentSelection(selectedColorSchemeInfo);
        }
        jComboBox.removeAllItems();
        populateComboBox();
        if (getCurrentSelection() != null) {
            ColorSchemeDefaults.debug("Setting selected to stored value=" + getCurrentSelection());
            setSelected(getCurrentSelection());
        }
        jComboBox.repaint();
        setjComboBoxShouldFire(shouldFire);
    }


    // If user selects an invalid scheme then use the current selection, otherwise update to the selected scheme
    private void validateSelection() {
        ColorSchemeDefaults.debug("Checking validation");

        if (jComboBox != null) {
            ColorSchemeInfo selectedColorSchemeInfo = (ColorSchemeInfo) jComboBox.getSelectedItem();
            if (selectedColorSchemeInfo != null && selectedColorSchemeInfo != getCurrentSelection()) {
                if (selectedColorSchemeInfo.isEnabled()) {
                    ColorSchemeDefaults.debug("Validated Selected");
                    setCurrentSelection(selectedColorSchemeInfo);
                } else if (getCurrentSelection() != null) {
                    ColorSchemeDefaults.debug("Setting to prior selection");
                    setSelected(getCurrentSelection());
                }
            }
        }

        ColorSchemeDefaults.debug("Finished Checking validation");
    }


    private void populateComboBox() {

        ArrayList<ColorSchemeInfo> colorSchemeCurrentInfos = null;

        if (isSortComboBox()) {
            if (isVerbose()) {
                colorSchemeCurrentInfos = colorSchemeSortedVerboseInfos;

            } else {
                colorSchemeCurrentInfos = colorSchemeSortedInfos;
            }
        } else {
            colorSchemeCurrentInfos = colorSchemeInfos;
        }

        final String[] toolTipsArray = new String[colorSchemeCurrentInfos.size()];
        final Boolean[] enabledArray = new Boolean[colorSchemeCurrentInfos.size()];
        final Boolean[] dividerArray = new Boolean[colorSchemeCurrentInfos.size()];
        final Boolean[] duplicateArray = new Boolean[colorSchemeCurrentInfos.size()];

        int i = 0;
        for (ColorSchemeInfo colorSchemeInfo : colorSchemeCurrentInfos) {
            if (colorSchemeInfo.isEnabled() || (!colorSchemeInfo.isEnabled() && (showDisabled || colorSchemeInfo.isDivider()))) {
                toolTipsArray[i] = colorSchemeInfo.getDescription();
                enabledArray[i] = colorSchemeInfo.isEnabled();
                dividerArray[i] = colorSchemeInfo.isDivider();
                duplicateArray[i] = colorSchemeInfo.isDuplicateEntry();
                jComboBox.addItem(colorSchemeInfo);
                i++;
            }
        }

        final MyComboBoxRenderer myComboBoxRenderer = new MyComboBoxRenderer();
        myComboBoxRenderer.setTooltipList(toolTipsArray);
        myComboBoxRenderer.setEnabledList(enabledArray);
        myComboBoxRenderer.setDividerList(dividerArray);
        myComboBoxRenderer.setDuplicateList(duplicateArray);
        jComboBox.setRenderer(myComboBoxRenderer);
    }


    private void initComboBox() {

        jComboBox = new JComboBox();
        jComboBox.setEditable(false);
        if (colorSchemeLutFile != null) {
            jComboBox.setToolTipText("To modify see file: " + colorSchemesAuxDir + "/" + colorSchemeLutFile.getName());
        }

        jComboBox.setMaximumRowCount(15);
        populateComboBox();
    }


    private Document getFileDocument(File file) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document dom = null;

        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            dom = db.parse(new FileInputStream(file));
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (SAXException se) {
            se.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return dom;
    }


    private boolean initColorSchemeInfos() {

        Document dom = getFileDocument(colorSchemesFile);

        Element rootElement = dom.getDocumentElement();
        NodeList schemeNodeList = rootElement.getElementsByTagName("Scheme");


        if (schemeNodeList != null && schemeNodeList.getLength() > 0) {
            for (int i = 0; i < schemeNodeList.getLength(); i++) {

                Element schemeElement = (Element) schemeNodeList.item(i);

                if (schemeElement != null) {
                    boolean validEntry = true;
                    boolean fieldsInitialized = false;

                    String id = null;
                    Double min = null;
                    Double max = null;
                    boolean logScaled = false;
                    String standardCpdFilename = null;
                    String universalCpdFilename = null;
                    String colorBarTitle = null;
                    String colorBarLabels = null;
                    String description = null;
                    String displayName = null;

                    File standardCpdFile = null;
                    File colorBlindCpdFile = null;

                    boolean enabled = true;

                    boolean divider = false;
                    boolean primaryScheme = false;


                    id = schemeElement.getAttribute("name");
                    description = getTextValue(schemeElement, "DESCRIPTION");
                    displayName = getTextValue(schemeElement, "DISPLAY_NAME");
                    colorBarLabels = getTextValue(schemeElement, "COLORBAR_LABELS");
                    colorBarTitle = getTextValue(schemeElement, "COLORBAR_TITLE");
                    String minStr = getTextValue(schemeElement, "MIN");
                    String maxStr = getTextValue(schemeElement, "MAX");
                    String logScaledStr = getTextValue(schemeElement, "LOG_SCALE");
                    standardCpdFilename = getTextValue(schemeElement, "CPD_FILENAME");
                    universalCpdFilename = getTextValue(schemeElement, "CPD_FILENAME_COLORBLIND");
                    String dividerString = getTextValue(schemeElement, "DIVIDER");
                    String primarySchemeString = getTextValue(schemeElement, "PRIMARY");


                    if (minStr != null && minStr.length() > 0) {
                        min = Double.valueOf(minStr);
                    } else {
                        min = ColorSchemeDefaults.DOUBLE_NULL;
                    }

                    if (maxStr != null && maxStr.length() > 0) {
                        max = Double.valueOf(maxStr);
                    } else {
                        max = ColorSchemeDefaults.DOUBLE_NULL;
                    }

                    logScaled = false;
                    if (logScaledStr != null && logScaledStr.length() > 0 && logScaledStr.toLowerCase().equals("true")) {
                        logScaled = true;
                    }

                    divider = false;
                    if (dividerString != null && dividerString.length() > 0 && dividerString.toLowerCase().equals("true")) {
                        divider = true;
                    }

                    primaryScheme = false;
                    if (primarySchemeString != null && primarySchemeString.length() > 0 && primarySchemeString.toLowerCase().equals("true")) {
                        primaryScheme = true;
                    }

                    if (id != null && id.length() > 0) {
                        fieldsInitialized = true;
                    }

                    if (fieldsInitialized) {

                        if (!testMinMax(min, max, logScaled)) {
                            validEntry = false;
                        }

                        if (validEntry) {
                            if (standardCpdFilename != null) {
                                standardCpdFile = new File(colorPaletteAuxDir, standardCpdFilename);
                                if (standardCpdFile == null || !standardCpdFile.exists()) {
                                    validEntry = false;
                                }
                            } else {
                                validEntry = false;
                            }
                        }

                        if (validEntry) {
                            if (universalCpdFilename != null) {
                                colorBlindCpdFile = new File(colorPaletteAuxDir, universalCpdFilename);
                                if (colorBlindCpdFile == null || !colorBlindCpdFile.exists()) {
                                    validEntry = false;
                                }
                            } else {
                                validEntry = false;
                            }
                        }


                        ColorSchemeInfo colorSchemeInfo = null;

                        enabled = validEntry;

                        colorSchemeInfo = new ColorSchemeInfo(id, divider, displayName, description, standardCpdFilename, min, max, logScaled, enabled, universalCpdFilename, colorBarTitle, colorBarLabels, colorPaletteAuxDir);

                        if (!colorSchemeInfo.isEnabled()) {
                            description = checkScheme(colorSchemeInfo);
                            colorSchemeInfo.setDescription(description);
                        }


                        if (colorSchemeInfo != null) {
                            // determine if this is a duplicate entry
                            for (ColorSchemeInfo storedColorSchemeInfo : colorSchemePrimaryInfos) {
                                if (id.equals(storedColorSchemeInfo.getName())) {
                                    colorSchemeInfo.setDuplicateEntry(true);
                                    break;
                                }
                            }
                            if (!colorSchemeInfo.isDuplicateEntry()) {
                                for (ColorSchemeInfo storedColorSchemeInfo : colorSchemeAdditionalInfos) {
                                    if (id.equals(storedColorSchemeInfo.getName())) {
                                        colorSchemeInfo.setDuplicateEntry(true);
                                        break;
                                    }
                                }
                            }

                            if (colorSchemeInfo.isDuplicateEntry()) {
                                colorSchemeInfo.setDescription("WARNING!: duplicate scheme entry");
                            }

                            if (primaryScheme) {
                                colorSchemePrimaryInfos.add(colorSchemeInfo);
                            } else {
                                colorSchemeAdditionalInfos.add(colorSchemeInfo);
                            }
                        }
                    }
                }
            }
        }


        Collections.sort(colorSchemePrimaryInfos, new Comparator<ColorSchemeInfo>() {
            public int compare(ColorSchemeInfo s1, ColorSchemeInfo s2) {
                return s1.toString().compareToIgnoreCase(s2.toString());
            }
        });

        Collections.sort(colorSchemeAdditionalInfos, new Comparator<ColorSchemeInfo>() {
            public int compare(ColorSchemeInfo s1, ColorSchemeInfo s2) {
                return s1.toString().compareToIgnoreCase(s2.toString());
            }
        });


        for (ColorSchemeInfo colorSchemeInfo : colorSchemePrimaryInfos) {
            colorSchemeInfos.add(colorSchemeInfo);
        }

        ColorSchemeInfo divider = new ColorSchemeInfo("-- Additional Schemes --", true, "-- Additional Schemes --", null, null, 0, 0, false, false, null, null, null, colorPaletteAuxDir);
        colorSchemeInfos.add(divider);

        for (ColorSchemeInfo colorSchemeInfo : colorSchemeAdditionalInfos) {
            colorSchemeInfos.add(colorSchemeInfo);
        }


        return true;
    }


    private void initColorSchemeLookup() {

        Document dom = getFileDocument(colorSchemeLutFile);

        Element rootElement = dom.getDocumentElement();
        NodeList keyNodeList = rootElement.getElementsByTagName("KEY");

        if (keyNodeList != null && keyNodeList.getLength() > 0) {

            for (int i = 0; i < keyNodeList.getLength(); i++) {
                boolean checksOut = true;

                Element schemeElement = (Element) keyNodeList.item(i);

                if (schemeElement != null) {
                    String regex = schemeElement.getAttribute("REGEX");
                    String schemeId = getTextValue(schemeElement, "SCHEME_ID");
                    String description = getTextValue(schemeElement, "DESCRIPTION");


                    if (regex == null || regex.length() == 0) {
                        checksOut = false;
                    }

                    if (schemeId == null || schemeId.length() == 0) {
                        schemeId = regex;
                    }

                    if (schemeId == null || schemeId.length() == 0) {
                        checksOut = false;
                    }

                    ColorSchemeInfo colorSchemeInfo = getColorSchemeInfoBySchemeId(schemeId);
                    if (colorSchemeInfo == null) {
                        checksOut = false;
                    }

                    if (checksOut) {
                        ColorSchemeLookupInfo colorSchemeLookupInfo = new ColorSchemeLookupInfo(regex, schemeId, description, colorSchemeInfo);

                        if (colorSchemeLookupInfo != null) {
                            colorSchemeLookupInfos.add(colorSchemeLookupInfo);
                        }
                    }
                }
            }
        }
    }

    public ColorSchemeInfo getColorSchemeInfoBySchemeId(String schemeId) {
        if (schemeId != null && schemeId.length() > 0) {
            for (ColorSchemeInfo colorSchemeInfo : colorSchemeInfos) {
                if (colorSchemeInfo != null && colorSchemeInfo.getName() != null) {
                    if (schemeId.toLowerCase().equals(colorSchemeInfo.getName().toLowerCase())) {
                        return colorSchemeInfo;
                    }
                }
            }
        }

        return null;
    }


    private boolean testMinMax(double min, double max, boolean isLogScaled) {
        boolean checksOut = true;

        if (min != ColorSchemeDefaults.DOUBLE_NULL && max != ColorSchemeDefaults.DOUBLE_NULL) {
            if (min >= max) {
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

    // don't allow to fire
    public void setSelected(ColorSchemeInfo colorSchemeInfo) {

        boolean shouldFire = isjComboBoxShouldFire();
        setjComboBoxShouldFire(false);
        setCurrentSelection(colorSchemeInfo);

        // loop through and find rootScheme
        if (jComboBox != null) {
            if (colorSchemeInfo != null) {
                jComboBox.setSelectedItem(colorSchemeInfo);
            } else {
                reset();
            }
        }

        setjComboBoxShouldFire(shouldFire);
    }

    public void reset() {
        setSelected(getNoneColorSchemeInfo());
    }

    public boolean isSchemeSet() {
        if (jComboBox != null && getNoneColorSchemeInfo() != jComboBox.getSelectedItem()) {
            return true;
        }

        return false;
    }


    public static String getTextValue(Element ele, String tagName) {
        String textVal = null;
        NodeList nl = ele.getElementsByTagName(tagName);
        if (nl != null && nl.getLength() > 0) {
            Element el = (Element) nl.item(0);

            if (el.hasChildNodes()) {
                textVal = el.getFirstChild().getNodeValue();
            }

        }

        return textVal;
    }


    public JComboBox getjComboBox() {
        return jComboBox;
    }


    public ArrayList<ColorSchemeLookupInfo> getColorSchemeLookupInfos() {
        return colorSchemeLookupInfos;
    }


    private boolean isVerbose() {
        return verbose;
    }

    private void setVerbose(boolean verbose) {
        this.verbose = verbose;

        for (ColorSchemeInfo colorSchemeInfo : colorSchemeInfos) {
            colorSchemeInfo.setUseDisplayName(verbose);
        }
    }

    public ColorSchemeInfo getNoneColorSchemeInfo() {
        return noneColorSchemeInfo;
    }

    public void setNoneColorSchemeInfo(ColorSchemeInfo noneColorSchemeInfo) {
        this.noneColorSchemeInfo = noneColorSchemeInfo;
    }

    public ColorSchemeInfo getCurrentSelection() {
        return currentSelection;
    }

    public void setCurrentSelection(ColorSchemeInfo currentSelection) {

        if (currentSelection == null) {
            currentSelection = noneColorSchemeInfo;
        }

        ColorSchemeDefaults.debug("Setting currentSelection=" + currentSelection.toString());
        this.currentSelection = currentSelection;
    }



    class MyComboBoxRenderer extends BasicComboBoxRenderer {

        private String[] tooltips;
        private Boolean[] enabledList;
        private Boolean[] dividerList;
        private Boolean[] duplicateList;

        public Component getListCellRendererComponent(JList list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {


//            if (index >= 0) {
//                if (index < enabledList.length) {
//                    setEnabled(enabledList[index]);
//                    setFocusable(enabledList[index]);
//                }
//
//                if (index < deviderList.length && deviderList[index]) {
//                    setEnabled(true);
//                    setFocusable(false);
//                }
//            }


            if (index >= 0 && dividerList != null && index < dividerList.length && dividerList[index]) {
                setBackground(Color.lightGray);
                setForeground(Color.black);
            } else {

                if (isSelected) {
                    setBackground(Color.blue);
                    setForeground(Color.white);

                    if (index >= 0 && enabledList != null && index < enabledList.length && !enabledList[index]) {
                        setForeground(Color.gray);
                    }

                    if (index >= 0 && tooltips != null && index < tooltips.length) {
                        list.setToolTipText(tooltips[index]);
                    }

                } else {
                    setBackground(Color.white);
                    setForeground(Color.black);

                    if (index >= 0 && duplicateList != null && index < duplicateList.length && duplicateList[index]) {
                        setForeground(Color.red);
                    } else if (index >= 0 && enabledList != null && index < enabledList.length && !enabledList[index]) {
                        setForeground(Color.gray);
                    }
                }
            }


            if (index == 0) {
                setBackground(Color.white);
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

        public void setDividerList(Boolean[] dividerList) {
            this.dividerList = dividerList;
        }

        public void setDuplicateList(Boolean[] duplicateList) {
            this.duplicateList = duplicateList;
        }
    }


    public String getjComboBoxNoneEntryName() {
        return jComboBoxNoneEntryName;
    }

    private void setjComboBoxNoneEntryName(String jComboBoxNoneEntryName) {
        this.jComboBoxNoneEntryName = jComboBoxNoneEntryName;
    }


    public boolean isNoneScheme(ColorSchemeInfo colorSchemeInfo) {
        return (noneColorSchemeInfo != null && noneColorSchemeInfo == colorSchemeInfo) ? true : false;
    }



    public String checkScheme(ColorSchemeInfo colorSchemeInfo) {
        String message = "";


        if (colorSchemeInfo != null) {
//            String description = "";
//
//            if (colorSchemeInfo.getDescription() != null) {
//                description = colorSchemeInfo.getDescription() + "<br>";
//            }

            String standardFileMessage = "";
            String universalFileMessage = "";

            String message_head = "WARNING!: Configuration issue for scheme = '" + colorSchemeInfo.getName() + "':<br>";

            String standardFilename = colorSchemeInfo.getCpdFilename(false);

            if (standardFilename != null && standardFilename.length() > 0) {
                File standardFile = new File(colorPaletteAuxDir, standardFilename);

                if (standardFile == null || !standardFile.exists()) {
                    standardFileMessage = "Scheme standard file '" + standardFilename + "' does not exist<br>";
                }
            } else {
                standardFileMessage = "Scheme does not contain a standard file<br>";
            }

            String universalFilename = colorSchemeInfo.getCpdFilename(true);

            if (universalFilename != null && universalFilename.length() > 0) {
                File universalFile = new File(colorPaletteAuxDir, universalFilename);

                if (universalFile == null || !universalFile.exists()) {
                    universalFileMessage = "Scheme universal file '" + universalFilename + "' does not exist<br>";
                }
            } else {
                universalFileMessage = "Scheme does not contain a universal file<br>";
            }

            String minMaxIssue = "";
            if (!testMinMax(colorSchemeInfo.getMinValue(), colorSchemeInfo.getMaxValue(), colorSchemeInfo.isLogScaled())) {
                minMaxIssue = "Issue with min, max values: min= '"
                        + colorSchemeInfo.getMinValue()
                        + "', max= '"
                        + colorSchemeInfo.getMaxValue()
                        + "' <br>";
            }

            message = "<html>" + message_head + standardFileMessage + universalFileMessage + minMaxIssue + "</html>";

        } else {
            message = "Configuration Error";
        }


        return message;
    }

    public boolean isjComboBoxShouldFire() {
        return jComboBoxShouldFire;
    }

    public void setjComboBoxShouldFire(boolean jComboBoxShouldFire) {
        this.jComboBoxShouldFire = jComboBoxShouldFire;
    }
}
