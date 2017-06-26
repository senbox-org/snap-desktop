package org.esa.snap.ui.tooladapter.model;

import org.esa.snap.core.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.snap.tango.TangoIcons;

import javax.swing.ImageIcon;
import javax.swing.table.AbstractTableModel;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Ramona Manda
 */
public class OperatorsTableModel extends AbstractTableModel {

    private static ImageIcon STATUS_OK;
    private static ImageIcon STATUS_NOK;

    private String[] columnNames = {"Status", "Alias", "Description"};
    //private boolean[] toolsChecked = null;
    private List<ToolAdapterOperatorDescriptor> data = null;

    static {
        try {
            STATUS_OK = new ImageIcon(OperatorsTableModel.class.getResource("/org/esa/snap/ui/tooladapter/dialogs/check_ok.png"));
        } catch (Exception e) {
            Logger.getLogger(OperatorsTableModel.class.getName()).warning("Image resource not loaded");
        }
        STATUS_NOK = TangoIcons.emblems_emblem_important(TangoIcons.Res.R16);
    }

    public OperatorsTableModel(List<ToolAdapterOperatorDescriptor> operators) {
        this.data = operators;
        //this.toolsChecked = new boolean[this.data.size()];
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case 0:
                org.esa.snap.core.gpf.descriptor.dependency.Bundle bundle = data.get(rowIndex).getBundle();
                return (bundle != null && bundle.isInstalled()) ? STATUS_OK : STATUS_NOK;
            case 1:
                //return toolsChecked[rowIndex];
                return data.get(rowIndex).getAlias();
            case 2:
                //return data.get(rowIndex).getAlias();
                return data.get(rowIndex).getDescription();
            /*case 2:
                return data.get(rowIndex).getDescription();*/
        }
        return "";
    }

    @Override
    public String getColumnName(int col) {
        return columnNames[col];
    }

    @Override
    public Class getColumnClass(int c) {
        if (c == 0) {
            return ImageIcon.class;
        } else {
            return String.class;
        }
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        /*if (col == 0) {
            return true;
        } else {*/
            return false;
//        }
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        //this.toolsChecked[row] = (boolean) value;
    }

    /*public ToolAdapterOperatorDescriptor getFirstCheckedOperator() {
        for (int i = 0; i < this.toolsChecked.length; i++) {
            if (this.toolsChecked[i]) {
                return this.data.get(i);
            }
        }
        return null;
    }*/

   /* public List<ToolAdapterOperatorDescriptor> getCheckedOperators() {
        List<ToolAdapterOperatorDescriptor> result = new ArrayList<>();
        for (int i = 0; i < this.toolsChecked.length; i++) {
            if (this.toolsChecked[i]) {
                result.add(this.data.get(i));
            }
        }
        return result;
    }*/

    public ToolAdapterOperatorDescriptor getObjectAt(int rowIndex) {
        ToolAdapterOperatorDescriptor result = null;
        if (rowIndex >= 0 && rowIndex <= this.data.size() - 1) {
            result = this.data.get(rowIndex);
        }
        return result;
    }
}
