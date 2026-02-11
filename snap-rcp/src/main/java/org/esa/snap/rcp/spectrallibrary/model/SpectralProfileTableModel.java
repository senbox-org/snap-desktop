package org.esa.snap.rcp.spectrallibrary.model;

import org.esa.snap.speclib.model.AttributeSchema;
import org.esa.snap.speclib.model.AttributeValue;
import org.esa.snap.speclib.model.SpectralProfile;

import javax.swing.table.AbstractTableModel;
import java.util.*;
import java.util.stream.Collectors;


public class SpectralProfileTableModel extends AbstractTableModel {


    private final List<String> baseColumns = List.of("Name", "Samples");
    private List<String> attributeKeys = List.of();
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
        return baseColumns.size() + attributeKeys.size();
    }

    @Override
    public String getColumnName(int col) {
        if (col < baseColumns.size()) {
            return baseColumns.get(col);
        }
        return attributeKeys.get(col - baseColumns.size());
    }


    @Override
    public Object getValueAt(int row, int col) {
        SpectralProfile p = profiles.get(row);
        if (col == 0) {
            return p.getName();
        }
        if (col == 1) {
            return p.getSignature().size();
        }
        String key = attributeKeys.get(col - baseColumns.size());
        return p.getAttribute(key).map(SpectralProfileTableModel::toDisplayValue).orElse("");
    }

    @Override
    public Class<?> getColumnClass(int col) {
        if (col == 1) {
            return Integer.class;
        }
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public void setSchema(AttributeSchema schema) {
        if (schema == null || schema.asMap().isEmpty()) {
            attributeKeys = List.of();
        } else {
            attributeKeys = List.copyOf(schema.asMap().keySet());
        }
        fireTableStructureChanged();
    }

    private static String toDisplayValue(AttributeValue v) {
        if (v == null) {
            return "";
        }

        return switch (v.getType()) {
            case STRING -> v.asString();
            case INT -> String.valueOf(v.asInt());
            case LONG -> String.valueOf(v.asLong());
            case DOUBLE -> String.valueOf(v.asDouble());
            case BOOLEAN -> String.valueOf(v.asBoolean());
            case STRING_LIST -> String.join(",", v.asStringList());
            case DOUBLE_ARRAY -> Arrays.toString(v.asDoubleArray());
            case INT_ARRAY -> Arrays.toString(v.asIntArray());
            case STRING_MAP -> v.asStringMap().entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(","));
            case EMBEDDED_SPECTRUM -> {
                var emb = v.asEmbeddedSpectrum();
                yield "<spectrum n=" + emb.getAxis().size() + ">";
            }
        };
    }
}
