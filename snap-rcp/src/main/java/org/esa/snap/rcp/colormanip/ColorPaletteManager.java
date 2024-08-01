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
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.esa.snap.core.datamodel.ColorManipulationDefaults.*;

class ColorPaletteManager {


    static ColorPaletteManager manager = new ColorPaletteManager();


    public final static String  OTHER_CAT = "OTHER";

    private List<ColorPaletteDef> cpdList;
    private List<String> cpdNames;
    private List<File> filesList;

    private List<String> cpdCategory;
    private List<String> categories;


    private List<PaletteInfo> paletteInfos;

    public static ColorPaletteManager getDefault() {
        return manager;
    }

    ColorPaletteManager() {
        filesList = new ArrayList<>();
        cpdList = new ArrayList<>();
        cpdNames = new ArrayList<>();
        cpdCategory = new ArrayList<>();
        categories = new ArrayList<>();
        paletteInfos = new ArrayList<>();
    }

    public class PaletteInfo {
        private String palette;
        private String category;
        private boolean universal;



        PaletteInfo(String palette, String category, boolean universal) {
            this.palette = palette;
            this.category = category;
            this.universal = universal;
        }

        PaletteInfo(String palette, String category) {
            this.palette = palette;
            this.category = category;
            this.universal = false;
        }

        PaletteInfo(String palette) {
            this.palette = palette;
            this.category = null;
            this.universal = false;
        }

        public void setPalette(String palette) {
            this.palette = palette;
        }

        public String getPalette() {
            return palette;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getCategory() {
            return category;
        }

        public void setUniversal(boolean universal) {
            this.universal = universal;
        }

        public boolean isUniversal() {
            return universal;
        }


    }


    public void loadAvailableColorPalettes(File palettesDir) {
        cpdNames.clear();
        cpdList.clear();

        final ProductSceneView view = SnapApp.getDefault().getSelectedProductSceneView();
        PropertyMap configuration = view.getSceneImage().getConfiguration();


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
                            String universalPaletteString = getTextValue(paletteElement, "UNIVERSAL");
                            String categoryPaletteString = getTextValue(paletteElement, "CAT");

                            if (categoryPaletteString != null && categoryPaletteString.length() > 0) {
                                int match = 0;
                                for (String category : categories) {
                                    if (category.equals(categoryPaletteString)) {
                                        match = 1;
                                    }
                                }
                                if (match == 0) {
                                    categories.add(categoryPaletteString);
                                }
                            }

                            PaletteInfo paletteInfo = new PaletteInfo(id, categoryPaletteString);
                            paletteInfos.add(paletteInfo);
                        }
                    }

                    int match = 0;
                    for (String category : categories) {
                        if (category.equals(OTHER_CAT)) {
                            match = 1;
                        }
                    }
                    if (match == 0) {
                        categories.add(OTHER_CAT);
                    }
                }
            }
        }

        if (files != null) {
            for (File file : files) {
                filesList.add(file);
            }
        }

        boolean sort = configuration.getPropertyBool(PROPERTY_PALETTE_SORT_KEY, PROPERTY_PALETTE_SORT_DEFAULT);
        if (sort) {
//            Collections.sort(cpdList, new Comparator<ColorPaletteDef>() {
//                public int compare(ColorPaletteDef colorPaletteDef1, ColorPaletteDef colorPaletteDef2) {
//                    return colorPaletteDef1.getFirstPoint().getLabel().compareTo
//                            (colorPaletteDef2.getFirstPoint().getLabel());
//                }
//            });
//            Collections.sort(cpdNames);
            Arrays.sort(files);


            Collections.sort(filesList);
        }




        boolean showDisabledPalettes = configuration.getPropertyBool(PROPERTY_PALETTE_SHOW_DISABLED_KEY, PROPERTY_PALETTE_SHOW_DISABLED_DEFAULT);


        if (files != null) {
            for (File file : files) {
                try {
                    ColorPaletteDef newCpd;

                    if (file.getName().endsWith("cpt")) {
                        newCpd = ColorPaletteDef.loadCpt(file);
                    } else {
                        newCpd = ColorPaletteDef.loadColorPaletteDef(file);
                    }

                    String currCategory = null;
                    for (PaletteInfo paletteInfo: paletteInfos) {
                        if (paletteInfo.getPalette().equals(file.getName())) {
                            currCategory = paletteInfo.getCategory();
                        }
                    }
                    if (currCategory == null) {
                        currCategory = OTHER_CAT;
                    }

                    if (!"DISABLE".equals(currCategory) || showDisabledPalettes) {
                        cpdCategory.add(currCategory);

                        cpdList.add(newCpd);
                        cpdNames.add(file.getName());
                    }
                } catch (IOException e) {
                    final Logger logger = SystemUtils.LOG;
                    logger.warning("Unable to load color palette definition from file '" + file.getAbsolutePath() + "'");
                    logger.log(Level.INFO, e.getMessage(), e);
                }
            }
        }

    }


    public static String getCategoryDisplay(String category) {
        return "  ------- " + category + " Palettes -------";
    }


    public List<String> getCategories() {
        return categories;
    }

    public boolean matchesCategory(ColorPaletteDef cpdForRaster, String category) {
        if (cpdForRaster == null || category == null) {
            return false;
        }

        for (int i = 0; i < cpdList.size(); i++) {
            ColorPaletteDef colorPaletteDef = cpdList.get(i);
            if (colorPaletteDef == cpdForRaster) {
                if (category.equals(cpdCategory.get(i))) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }



    public static boolean isWrapperCategory(ColorPaletteChooser.ColorPaletteWrapper wrapper) {
        if (wrapper == null || wrapper.name == null || wrapper.name.length() == 0) {
            return false;
        }
        for (String category : ColorPaletteManager.getDefault().getCategories()) {
            if (wrapper.name.contains(ColorPaletteManager.getCategoryDisplay(category))) {
                return true;
            }
        }

        return false;
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
