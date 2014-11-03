package org.esa.snap.gui.view;

import org.esa.snap.core.Band;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

/**
 * Created by Norman on 08.07.2014.
 */
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
            graphics2D.drawRenderedImage(band.getData(), new AffineTransform());
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (band != null) {
            return new Dimension(band.getData().getWidth(), band.getData().getHeight());
        } else {
            return super.getPreferredSize();
        }
    }
}
