package org.esa.snap.rcp.spectrallibrary.model;

import org.esa.snap.speclib.model.SpectralProfile;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


public class SpectralProfileTableModel extends AbstractTableModel {


    private final String[] columns = {"Name", "Samples"};
    private List<SpectralProfile> profiles = List.of();

    public void setProfiles(List<SpectralProfile> profiles) {
        this.profiles = profiles == null ? List.of() : List.copyOf(profiles);
        fireTableDataChanged();
    }

    public SpectralProfile getAt(int row) {
        if (row < 0 || row >= profiles.size()) {
            return null;
        }
        return profiles.get(row);
    }

    public UUID getIdAt(int row) {
        SpectralProfile p = getAt(row);
        return p == null ? null : p.getId();
    }

    public List<SpectralProfile> getProfilesSnapshot() {
        return new ArrayList<>(profiles);
    }

    @Override
    public int getRowCount() {
        return profiles.size();
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        SpectralProfile p = profiles.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> p.getName();
            case 1 -> p.getSignature().size();
            default -> "";
        };
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return Objects.equals(columns[columnIndex], "Samples") ? Integer.class : String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

}
