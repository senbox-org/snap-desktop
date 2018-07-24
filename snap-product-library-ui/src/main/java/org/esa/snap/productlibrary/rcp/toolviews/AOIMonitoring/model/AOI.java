/*
 * Copyright (C) 2017 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.productlibrary.rcp.toolviews.AOIMonitoring.model;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.dataop.downloadable.XMLSupport;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.productlibrary.db.DBQuery;
import org.esa.snap.productlibrary.db.GeoPosList;
import org.esa.snap.rcp.SnapApp;
import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**

 */

public class AOI implements GeoPosList {

    public static final String EXT = ".xml";

    private final File aoiFile;
    private String name = "aoi";
    private String inputFolder = "";
    private String outputFolder = "";
    private String processingGraph = "";
    private String lastProcessed = "";
    private boolean findSlaves = false;
    private int maxSlaves = 1;

    private GeoPos[] aoiPoints = new GeoPos[]{};
    private DBQuery slaveDBQuery = null;

    public AOI(final File file) {
        this.aoiFile = file;
        if (!aoiFile.exists() || !load(aoiFile)) {
            this.name = FileUtils.getFilenameWithoutExtension(file);
            this.inputFolder = AOIManager.getLastInputPath();
            this.outputFolder = AOIManager.getLastOutputPath();
            this.processingGraph = null; //todo VisatApp.getApp().getPreferences().getPropertyString(AOIManager.LAST_GRAPH_PATH,
        }
    }

    public File getFile() {
        return aoiFile;
    }

    public String getName() {
        return name;
    }

    public void setName(final String n) {
        name = n;
    }

    public String getInputFolder() {
        return inputFolder;
    }

    public void setInputFolder(final String file) {
        inputFolder = file;
    }

    public String getOutputFolder() {
        return outputFolder;
    }

    public void setOutputFolder(final String file) {
        outputFolder = file;
    }

    public String getProcessingGraph() {
        return processingGraph;
    }

    public void setProcessingGraph(final String file) {
        processingGraph = file;
    }

    public boolean getFindSlaves() {
        return findSlaves;
    }

    public void setFindSlaves(final boolean flag) {
        findSlaves = flag;
    }

    public void setMaxSlaves(final int max) {
        maxSlaves = max;
    }

    public int getMaxSlaves() {
        return maxSlaves;
    }

    public void setPoints(final GeoPos[] selectionBox) {
        aoiPoints = selectionBox;
    }

    public GeoPos[] getPoints() {
        return aoiPoints;
    }

    public void setSlaveDBQuery(final DBQuery query) {
        slaveDBQuery = query;
    }

    public DBQuery getSlaveDBQuery() {
        return slaveDBQuery;
    }

    public String getLastProcessed() {
        return lastProcessed;
    }

    public void setLastProcessed(final String date) {
        lastProcessed = date;
    }

    public void save() {
        final Element root = new Element("AOI");
        root.setAttribute("name", name);
        final Document doc = new Document(root);

        final Element elem = new Element("param");
        elem.setAttribute("inputFolder", inputFolder);
        elem.setAttribute("outputFolder", outputFolder);
        elem.setAttribute("graph", processingGraph);
        elem.setAttribute("lastProcessed", lastProcessed);
        elem.setAttribute("findSlaves", String.valueOf(findSlaves));
        elem.setAttribute("maxSlaves", String.valueOf(maxSlaves));
        root.addContent(elem);

        final Element pntsElem = new Element("points");
        for (GeoPos pnt : aoiPoints) {
            final Element pntElem = new Element("point");
            pntElem.setAttribute("lat", String.valueOf(pnt.getLat()));
            pntElem.setAttribute("lon", String.valueOf(pnt.getLon()));
            pntsElem.addContent(pntElem);
        }
        root.addContent(pntsElem);

        if (slaveDBQuery != null) {
            //todo root.addContent(slaveDBQuery.toXML());
        }

        try {
            XMLSupport.SaveXML(doc, aoiFile.getAbsolutePath());
        } catch (IOException e) {
            SystemUtils.LOG.severe("Unable to save AOI " + e.getMessage());
        }
    }

    private boolean load(final File file) {
        Document doc;
        try {
            doc = XMLSupport.LoadXML(file.getAbsolutePath());

            final Element root = doc.getRootElement();
            final Attribute nameAttrib = root.getAttribute("name");
            if (nameAttrib != null)
                this.name = nameAttrib.getValue();

            final List<GeoPos> geoPosList = new ArrayList<GeoPos>();

            final List<Content> children = root.getContent();
            for (Object aChild : children) {
                if (aChild instanceof Element) {
                    final Element child = (Element) aChild;
                    if (child.getName().equals("param")) {
                        inputFolder = XMLSupport.getAttrib(child, "inputFolder");
                        outputFolder = XMLSupport.getAttrib(child, "outputFolder");
                        processingGraph = XMLSupport.getAttrib(child, "graph");
                        lastProcessed = XMLSupport.getAttrib(child, "lastProcessed");
                        final Attribute findSlavesAttrib = child.getAttribute("findSlaves");
                        if (findSlavesAttrib != null)
                            findSlaves = Boolean.parseBoolean(findSlavesAttrib.getValue());
                        final Attribute maxSlavesAttrib = child.getAttribute("maxSlaves");
                        if (maxSlavesAttrib != null)
                            maxSlaves = Integer.parseInt(maxSlavesAttrib.getValue());
                    } else if (child.getName().equals("points")) {
                        final List<Content> pntsList = child.getContent();
                        for (Object o : pntsList) {
                            if (o instanceof Element) {
                                final Element pntElem = (Element) o;
                                final String latStr = XMLSupport.getAttrib(pntElem, "lat");
                                final String lonStr = XMLSupport.getAttrib(pntElem, "lon");
                                if (!latStr.isEmpty() && !lonStr.isEmpty()) {
                                    final float lat = Float.parseFloat(latStr);
                                    final float lon = Float.parseFloat(lonStr);
                                    geoPosList.add(new GeoPos(lat, lon));
                                }
                            }
                        }
                    } else if (child.getName().equals(DBQuery.DB_QUERY)) {
                        slaveDBQuery = new DBQuery();
                        //todo slaveDBQuery.fromXML(child);
                    }
                }
            }

            aoiPoints = geoPosList.toArray(new GeoPos[geoPosList.size()]);
        } catch (IOException e) {
            SnapApp.getDefault().handleError("Unable to load AOI", e);
            return false;
        }
        return true;
    }
}
