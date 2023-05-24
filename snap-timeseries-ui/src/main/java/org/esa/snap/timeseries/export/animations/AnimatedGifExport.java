/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.timeseries.export.animations;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.rcp.SnapApp;
import org.w3c.dom.Node;

import javax.imageio.*;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.esa.snap.timeseries.export.util.TimeSeriesExportHelper.FileWithLevel;
import static org.esa.snap.timeseries.export.util.TimeSeriesExportHelper.getOutputFileWithLevelOption;

public class AnimatedGifExport extends ProgressMonitorSwingWorker<Void, Void> {

    private final File outputFile;
    private static final String EXPORT_DIR_PREFERENCES_KEY = "user.export.dir";
    private RenderedImage[] frames;
    private int level;

    public AnimatedGifExport(Component parentComponent, String title) {
        super(parentComponent, title);
        FileWithLevel fileWithLevel = fetchOutputFile();
        this.outputFile = fileWithLevel.file;
        this.level = fileWithLevel.level;
    }

    @Override
    protected Void doInBackground(ProgressMonitor pm) throws Exception {
        exportAnimation("50", outputFile, pm);
        return null;
    }

    public void createFrames(List<Band> bandsForVariable) {
        List<RenderedImage> images = new ArrayList<>();
        for (Band band : bandsForVariable) {
            images.add(band.getGeophysicalImage().getImage(level));
        }

        frames = images.toArray(new RenderedImage[images.size()]);
    }

    private void exportAnimation(String delayTime, File file, ProgressMonitor pm) {

        ImageWriter imageWriter = ImageIO.getImageWritersByFormatName("gif").next();

        try {
            ImageOutputStream outputStream = ImageIO.createImageOutputStream(file);
            imageWriter.setOutput(outputStream);
            imageWriter.prepareWriteSequence(null);

            pm.beginTask("Exporting time series as animated gif", frames.length);

            for (int i = 0; i < frames.length; i++) {
                RenderedImage currentImage = frames[i];
                ImageWriteParam writeParameters = imageWriter.getDefaultWriteParam();
                IIOMetadata metadata = imageWriter.getDefaultImageMetadata(new ImageTypeSpecifier(currentImage),
                        writeParameters);

                configure(metadata, delayTime, i);
                IIOImage image = new IIOImage(currentImage, null, metadata);
                imageWriter.writeToSequence(image, null);
                pm.worked(1);
            }
            imageWriter.endWriteSequence();
            outputStream.close();
            pm.done();
        } catch (IOException e) {
            SnapApp.getDefault().handleError("Unable to create animated gif", e);
        }
    }

    private static void configure(IIOMetadata meta, String delayTime, int imageIndex) {
        String metaFormat = meta.getNativeMetadataFormatName();

        if (!"javax_imageio_gif_image_1.0".equals(metaFormat)) {
            throw new IllegalArgumentException(
                    "Unfamiliar gif metadata format: " + metaFormat);
        }

        Node root = meta.getAsTree(metaFormat);

        //find the GraphicControlExtension node
        Node child = root.getFirstChild();
        while (child != null) {
            if ("GraphicControlExtension".equals(child.getNodeName())) {
                IIOMetadataNode gce = (IIOMetadataNode) child;
                gce.setAttribute("userInputFlag", "FALSE");
                gce.setAttribute("delayTime", delayTime);
                break;
            }
            child = child.getNextSibling();
        }


        //only the first node needs the ApplicationExtensions node
        if (imageIndex == 0) {
            IIOMetadataNode parentNode = new IIOMetadataNode("ApplicationExtensions");
            IIOMetadataNode childNode = new IIOMetadataNode("ApplicationExtension");
            childNode.setAttribute("applicationID", "NETSCAPE");
            childNode.setAttribute("authenticationCode", "2.0");
            byte[] userObject = new byte[]{
                    //last two bytes is an unsigned short (little endian) that
                    //indicates the number of times to loop.
                    //0 means loop forever.
                    0x1, 0x0, 0x0
            };
            childNode.setUserObject(userObject);
            parentNode.appendChild(childNode);
            root.appendChild(parentNode);
        }

        try {
            meta.setFromTree(metaFormat, root);
        } catch (IIOInvalidTreeException e) {
            SnapApp.getDefault().handleError(e.getMessage(), e);
        }
    }

    private FileWithLevel fetchOutputFile() {
        final RasterDataNode currentRaster = SnapApp.getDefault().getSelectedProductSceneView().getRaster();
        SnapFileFilter gifFilter = new SnapFileFilter("gif", "gif", "Animated GIF");
        return getOutputFileWithLevelOption(currentRaster,
                "Export time series as animated GIF",
                "time_series_",
                EXPORT_DIR_PREFERENCES_KEY,
                gifFilter,
                null);
    }

}
