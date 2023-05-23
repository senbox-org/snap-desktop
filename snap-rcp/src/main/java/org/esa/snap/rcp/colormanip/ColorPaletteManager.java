package org.esa.snap.rcp.colormanip;

import org.esa.snap.core.datamodel.ColorManipulationDefaults;
import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.datamodel.ColorSchemeInfo;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.product.ProductSceneView;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.esa.snap.core.datamodel.ColorManipulationDefaults.*;

class ColorPaletteManager {

    static ColorPaletteManager manager = new ColorPaletteManager();

    private List<ColorPaletteDef> cpdList;
    private List<String> cpdNames;
    private List<String> primaryPaletteList;
    private List<String> universalPaletteList;
    private List<String> standardPaletteList;

    public static ColorPaletteManager getDefault() {
        return manager;
    }

    ColorPaletteManager() {
        cpdList = new ArrayList<>();
        cpdNames = new ArrayList<>();
        primaryPaletteList = new ArrayList<>();
        universalPaletteList = new ArrayList<>();
        standardPaletteList = new ArrayList<>();
    }

    public void loadAvailableColorPalettes(File palettesDir) {
        cpdNames.clear();
        cpdList.clear();

        final ProductSceneView view = SnapApp.getDefault().getSelectedProductSceneView();
        PropertyMap configuration = view.getSceneImage().getConfiguration();

        boolean sort = configuration.getPropertyBool(PROPERTY_PALETTE_SORT_KEY, PROPERTY_PALETTE_SORT_DEFAULT);

        final File[] files = palettesDir.listFiles((dir, name) -> {
            return name.toLowerCase().endsWith(".cpd") || name.toLowerCase().endsWith(".cpt");
        });
        final File colorPaletteFile = new File(palettesDir,COLOR_PALETTE_LIST_FILENAME );
        if (colorPaletteFile.exists()) {
            Document dom = getFileDocument(colorPaletteFile);
            if (dom != null) {
                Element rootElement = dom.getDocumentElement();
                NodeList paletteNodeList = rootElement.getElementsByTagName("Palette");

                if (paletteNodeList != null && paletteNodeList.getLength() > 0) {
                    for (int i = 0; i < paletteNodeList.getLength(); i++) {
                        Element paletteElement = (Element) paletteNodeList.item(i);
                        if (paletteElement != null) {
                            String id = paletteElement.getAttribute("name");
                            String primaryPaletteString = getTextValue(paletteElement, "PRIMARY");
                            String universalPaletteString = getTextValue(paletteElement, "UNIVERSAL");
                            String standardPaletteString = getTextValue(paletteElement, "STANDARD");
                            if (primaryPaletteString != null && primaryPaletteString.length() > 0 && primaryPaletteString.toLowerCase().equals("true")) {
                                primaryPaletteList.add(id);
                            }
                            if (universalPaletteString != null && universalPaletteString.length() > 0 && universalPaletteString.toLowerCase().equals("true")) {
                                universalPaletteList.add(id);
                            }
                            if (standardPaletteString != null && standardPaletteString.length() > 0 && standardPaletteString.toLowerCase().equals("true")) {
                                standardPaletteList.add(id);
                            }
                        }
                    }
                }
            }
        }
        if (files != null) {
            for (File file : files) {
                try {
                    ColorPaletteDef newCpd;

                    if (file.getName().endsWith("cpt")) {
                        newCpd = ColorPaletteDef.loadCpt(file);
                    } else {
                        newCpd = ColorPaletteDef.loadColorPaletteDef(file);
                    }
                    if (primaryPaletteList.contains(file.getName())) {
                        newCpd.setPrimary(true);
                    }
                    if (universalPaletteList.contains(file.getName())) {
                        newCpd.setUniversal(true);
                    }
                    if (standardPaletteList.contains(file.getName())) {
                        newCpd.setStandard(true);
                    }
                    cpdList.add(newCpd);
                    cpdNames.add(file.getName());
                } catch (IOException e) {
                    final Logger logger = SystemUtils.LOG;
                    logger.warning("Unable to load color palette definition from file '" + file.getAbsolutePath() + "'");
                    logger.log(Level.INFO, e.getMessage(), e);
                }
            }
        }
//        need to sort cpdList too
        if (sort) {
            Collections.sort(cpdList, new Comparator<ColorPaletteDef>() {
                public int compare(ColorPaletteDef colorPaletteDef1, ColorPaletteDef colorPaletteDef2) {
                    return colorPaletteDef1.getFirstPoint().getLabel().compareTo
                            (colorPaletteDef2.getFirstPoint().getLabel());
                }
            });
            Collections.sort(cpdNames);
        }
    }

    public List<ColorPaletteDef> getColorPaletteDefList() {
        return Collections.unmodifiableList(cpdList);
    }

    public String getNameFor(ColorPaletteDef cpdForRaster) {
        for (int i = 0; i < cpdList.size(); i++) {
            ColorPaletteDef colorPaletteDef = cpdList.get(i);
            if (colorPaletteDef == cpdForRaster)
                return cpdNames.get(i);
        }
        return null;
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
            Dialogs.showWarning("Unable to read File " + file.toString());
            return null;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return dom;
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
}
