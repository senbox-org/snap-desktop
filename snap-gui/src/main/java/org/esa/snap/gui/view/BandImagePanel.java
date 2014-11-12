package org.esa.snap.gui.view;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.jai.ImageManager;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;

public class BandImagePanel extends JPanel {
    private Band band;

    public BandImagePanel(Band band) {
        this.band = band;
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (band != null) {
            Graphics2D graphics2D = (Graphics2D) g;
            RenderedImage image = ImageManager.getInstance().createColoredBandImage(new RasterDataNode[]{band}, null, 0);
            graphics2D.drawRenderedImage(image, new AffineTransform());
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (band != null) {
            return new Dimension(band.getRasterWidth(), band.getRasterHeight());
        } else {
            return super.getPreferredSize();
        }
    }
}
