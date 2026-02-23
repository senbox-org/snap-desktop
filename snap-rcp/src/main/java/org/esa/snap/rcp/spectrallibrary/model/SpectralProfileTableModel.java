package org.esa.snap.rcp.spectrallibrary.model;

import org.esa.snap.speclib.model.AttributeSchema;
import org.esa.snap.speclib.model.AttributeType;
import org.esa.snap.speclib.model.AttributeValue;
import org.esa.snap.speclib.model.SpectralProfile;
import org.esa.snap.speclib.util.SpectralLibraryAttributeValueParser;

import javax.swing.table.AbstractTableModel;
import java.util.*;


public class SpectralProfileTableModel extends AbstractTableModel {


    private static final int COL_NAME = 0;
    private static final int COL_SAMPLES = 1;

    private final List<String> baseColumns = List.of("Name", "Samples");
    private List<String> attributeKeys = List.of();
    private List<SpectralProfile> profiles = List.of();

    private AttributeSchema schema;
    private ProfileEditHandler editHandler;


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

    public void setEditHandler(ProfileEditHandler h) {
        this.editHandler = h;
    }

    public void setSchema(AttributeSchema schema) {
        this.schema = schema;
        if (schema == null || schema.asMap().isEmpty()) {
            attributeKeys = List.of();
        } else {
            attributeKeys = List.copyOf(schema.asMap().keySet());
        }
        fireTableStructureChanged();
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

        if (col == COL_NAME) {
            return p.getName();
        }
        if (col == COL_SAMPLES) {
            return p.getSignature().size();
        }

        int attrIdx = col - baseColumns.size();
        String key = attributeKeys.get(attrIdx);
        return p.getAttribute(key).map(SpectralLibraryAttributeValueParser::cellValue).orElse(null);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (editHandler == null) {
            return;
        }
        SpectralProfile p = getAt(rowIndex);
        if (p == null) {
            return;
        }

        if (columnIndex == 0) {
            String nn = String.valueOf(aValue).trim();
            if (!nn.isEmpty()) {
                editHandler.rename(p.getId(), nn);
            }
            return;
        }

        String key = attributeKeys.get(columnIndex - baseColumns.size());
        AttributeType t = schema != null && schema.find(key).isPresent() ? schema.find(key).get().getType() : AttributeType.STRING;
        AttributeValue v = SpectralLibraryAttributeValueParser.parseForType(t, aValue);
        editHandler.setAttr(p.getId(), key, v);
    }

    @Override
    public Class<?> getColumnClass(int col) {
        if (col == 1) {
            return Integer.class;
        }
        if (col >= 2) {
            String key = attributeKeys.get(col - baseColumns.size());
            AttributeType t = schema != null && schema.find(key).isPresent() ? schema.find(key).get().getType() : AttributeType.STRING;
            if (t == AttributeType.BOOLEAN) {
                return Boolean.class;
            }
            if (t == AttributeType.INT) {
                return Integer.class;
            }
            if (t == AttributeType.LONG) {
                return Long.class;
            }
            if (t == AttributeType.DOUBLE) {
                return Double.class;
            }
        }
        return String.class;
    }


    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex != 1;
    }


    public interface ProfileEditHandler {
        void rename(UUID profileId, String newName);
        void setAttr(UUID profileId, String key, AttributeValue value);
    }
}
