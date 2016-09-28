package org.esa.snap.rcp.colormanip;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * The <code>SliderPanel</code> class contains the slider and input text field components.
 *
 * @author Jean Coravu
 */
public class SliderPanel extends JPanel {
    private final ChangeListener localChangeListener;
    private JSlider slider;
    private JTextField input;
    private ChangeListener sliderChangeListener;
    private int previousValue;
    private JLabel titleLabel;

    /**
     * Constructs a new item.
     *
     * @param title the panel title
     * @param sliderChangeListener the slider listener
     */
    public SliderPanel(String title, ChangeListener sliderChangeListener) {
        super(new BorderLayout());

        int maximumNumber = 100;

        this.sliderChangeListener = sliderChangeListener;

        this.previousValue = 0;

        this.titleLabel = new JLabel(title, JLabel.LEFT);
        this.titleLabel.setBorder(new EmptyBorder(0, 0, 0, 0));
        this.slider = new JSlider(JSlider.HORIZONTAL, -maximumNumber, maximumNumber, this.previousValue);
        this.slider.setFocusable(false);
        this.slider.setMajorTickSpacing(maximumNumber);
        this.slider.setMinorTickSpacing(0);
        this.slider.setPaintTicks(true);
        this.slider.setPaintLabels(true);
        MouseListener[] listeners = this.slider.getMouseListeners();
        for (int i=0; i<listeners.length; i++) {
            this.slider.removeMouseListener(listeners[i]);
        }

        final BasicSliderUI ui = (BasicSliderUI) this.slider.getUI();
        BasicSliderUI.TrackListener trackListener = ui.new TrackListener() {
            @Override
            public void mouseClicked(MouseEvent event) {
                Point mousePoint = event.getPoint();
                int value = ui.valueForXPosition(mousePoint.x);
                slider.setValue(value);
            }

            @Override
            public boolean shouldScroll(int direction) {
                return false;
            }
        };
        this.slider.addMouseListener(trackListener);

        this.localChangeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent event) {
                sliderValueChanged(event);
            }
        };
        this.slider.addChangeListener(this.localChangeListener);

        this.input = new JTextField(5);
        this.input.setDocument(new NumberPlainDocument(this.slider.getMinimum(), this.slider.getMaximum()));
        this.input.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_ENTER) {
                    int number = Integer.parseInt(input.getText());
                    slider.setValue(number);
                }
            }
        });
        refreshInputValue();

        JPanel panelInput = new JPanel(new FlowLayout());
        panelInput.add(this.input);

        add(this.titleLabel, BorderLayout.WEST);
        add(this.slider, BorderLayout.CENTER);
        add(panelInput, BorderLayout.EAST);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        this.titleLabel.setEnabled(enabled);
        this.slider.setEnabled(enabled);
        this.input.setEnabled(enabled);
    }

    /**
     * Returns the preferred width of the title label.
     *
     * @return the preferred width of the title label
     */
    public int getTitlePreferredWidth() {
        return this.titleLabel.getPreferredSize().width;
    }

    /**
     * Sets the preferred width of the title label.
     *
     * @param preferredWidth the width to set
     */
    public void setTitlePreferredWidth(int preferredWidth) {
        Dimension size = this.titleLabel.getPreferredSize();
        size.width = preferredWidth;
        this.titleLabel.setPreferredSize(size);
    }

    /**
     * Returns the slider maximum value.
     *
     * @return the slider maximum value
     */
    public int getSliderMaximumValue() {
        return this.slider.getMaximum();
    }

    /**
     * Returns the slider value.
     *
     * @return the slider value
     */
    public int getSliderValue() {
        return this.slider.getValue();
    }

    /**
     * Sets the slider value.
     * @param sliderValue the slider value
     */
    public void setSliderValue(int sliderValue) {
        this.slider.removeChangeListener(this.localChangeListener);
        this.previousValue = sliderValue;
        this.slider.setValue(this.previousValue);
        refreshInputValue();
        this.slider.addChangeListener(this.localChangeListener);
    }

    /**
     * Sets the slider value in the input text field.
     */
    private void refreshInputValue() {
        String value = Integer.toString(this.slider.getValue());
        this.input.setText(value);
    }

    /**
     * Fire the slider change event if the current slider value has changed.
     * @param event the slider change event
     */
    private void sliderValueChanged(ChangeEvent event) {
        int currentSliderValue = slider.getValue();
        if (!slider.getValueIsAdjusting() && currentSliderValue != previousValue) {
            previousValue = currentSliderValue;
            refreshInputValue();
            sliderChangeListener.stateChanged(event);
        }
    }
}

