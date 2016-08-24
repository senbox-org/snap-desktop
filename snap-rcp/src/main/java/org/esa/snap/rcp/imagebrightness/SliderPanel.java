package org.esa.snap.rcp.imagebrightness;

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

    /**
     * Constructs a new item.
     *
     * @param title the panel title
     * @param sliderChangeListener the slider listener
     * @param minimumNumber the minimum number
     * @param maximumNumber the maximum number
     */
    public SliderPanel(String title, ChangeListener sliderChangeListener, int minimumNumber, int maximumNumber) {
        super(new BorderLayout());

        this.sliderChangeListener = sliderChangeListener;

        this.previousValue = 0;

        JLabel titleLabel = new JLabel(title, JLabel.LEFT);
        titleLabel.setBorder(new EmptyBorder(0, 10, 0, 0));
        this.slider = new JSlider(JSlider.HORIZONTAL, minimumNumber, maximumNumber, this.previousValue);
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
        this.input.setDocument(new NumberPlainDocument(minimumNumber, maximumNumber));
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

        add(titleLabel, BorderLayout.WEST);
        add(this.slider, BorderLayout.CENTER);
        add(panelInput, BorderLayout.EAST);
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
