package org.esa.snap.core.gpf.ui.rtv;

import com.bc.ceres.swing.binding.ComponentAdapter;

import javax.media.jai.util.Range;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.util.Map;

public class IntervalsTableAdapter extends ComponentAdapter implements TableModelListener {
    IntervalsTablePanel intervalsTablePanel;

    public IntervalsTableAdapter( IntervalsTablePanel intervalsTablePanel){
        this.intervalsTablePanel = intervalsTablePanel;
    }

    @Override
    public JComponent[] getComponents() {
        return new JComponent[]{intervalsTablePanel};
    }

    @Override
    public void bindComponents() {
        intervalsTablePanel.intervalsTable.getModel().addTableModelListener(this);
    }

    @Override
    public void unbindComponents() {
        intervalsTablePanel.intervalsTable.getModel().removeTableModelListener(this);
    }

    @Override
    public void adjustComponents() {
        Map<Integer, Range> intervalsMap =  ( Map<Integer, Range>) getBinding().getPropertyValue();
        intervalsTablePanel.setIntervalsMap(intervalsMap);
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        getBinding().setPropertyValue(intervalsTablePanel.getIntervalsMap());
    }

}
