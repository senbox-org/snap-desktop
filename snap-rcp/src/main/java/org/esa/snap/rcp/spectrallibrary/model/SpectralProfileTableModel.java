package org.esa.snap.rcp.spectrallibrary.model;

import org.esa.snap.speclib.model.AttributeSchema;
import org.esa.snap.speclib.model.AttributeType;
import org.esa.snap.speclib.model.AttributeValue;
import org.esa.snap.speclib.model.SpectralProfile;

import javax.swing.table.AbstractTableModel;
import java.util.*;
import java.util.stream.Collectors;


public class SpectralProfileTableModel extends AbstractTableModel {


    private final List<String> baseColumns = List.of("Name", "Samples");
    private List<String> attributeKeys = List.of();
    private List<SpectralProfile> profiles = List.of();

    private AttributeSchema schema;
    private ProfileEditHandler editHandler;

    public interface ProfileEditHandler {
        void rename(UUID profileId, String newName);
        void setAttr(UUID profileId, String key, AttributeValue value);
    }


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
        return p.getAttribute(key).map(SpectralProfileTableModel::cellValue).orElse(null);
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
        AttributeValue v = parseForType(t, aValue);
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
        if (columnIndex == 1) {
            return false;
        }
        return true;
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
            case DOUBLE_ARRAY -> joinDoubleArray(v.asDoubleArray());
            case INT_ARRAY -> joinIntArray(v.asIntArray());
            case STRING_MAP -> v.asStringMap().entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(","));
            case EMBEDDED_SPECTRUM -> {
                var emb = v.asEmbeddedSpectrum();
                yield "<spectrum n=" + emb.getAxis().size() + ">";
            }
        };
    }

    private static String joinDoubleArray(double[] a) {
        if (a == null || a.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(a[i]);
        }
        return sb.toString();
    }

    private static String joinIntArray(int[] a) {
        if (a == null || a.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(a[i]);
        }
        return sb.toString();
    }

    private static Object cellValue(AttributeValue v) {
        if (v == null) return null;
        return switch (v.getType()) {
            case BOOLEAN -> v.asBoolean();
            case INT -> v.asInt();
            case LONG -> v.asLong();
            case DOUBLE -> v.asDouble();
            default -> toDisplayValue(v);
        };
    }

    public void setEditHandler(ProfileEditHandler h) {
        this.editHandler = h;
    }

    private static AttributeValue parseForType(AttributeType type, Object aValue) {
        if (type == null) {
            type = AttributeType.STRING;
        }

        if (type == AttributeType.BOOLEAN && aValue instanceof Boolean b) {
            return AttributeValue.ofBoolean(b);
        }
        if (type == AttributeType.INT && aValue instanceof Integer i) {
            return AttributeValue.ofInt(i);
        }
        if (type == AttributeType.LONG && aValue instanceof Long l) {
            return AttributeValue.ofLong(l);
        }
        if (type == AttributeType.DOUBLE && aValue instanceof Double d) {
            return AttributeValue.ofDouble(d);
        }

        String s = aValue == null ? "" : String.valueOf(aValue).trim();

        if ((type == AttributeType.DOUBLE_ARRAY || type == AttributeType.INT_ARRAY) && s.startsWith("[") && s.endsWith("]")) {
            s = s.substring(1, s.length() - 1).trim();
        }

        if (s.isEmpty()) {
            throw new IllegalArgumentException("value must not be empty");
        }

        return switch (type) {
            case STRING -> AttributeValue.ofString(s);
            case INT -> {
                try { yield AttributeValue.ofInt(Integer.parseInt(s)); }
                catch (NumberFormatException e) {
                    throw new IllegalArgumentException("expected int");
                }
            }
            case LONG -> {
                try { yield AttributeValue.ofLong(Long.parseLong(s)); }
                catch (NumberFormatException e) {
                    throw new IllegalArgumentException("expected long");
                }
            }
            case DOUBLE -> {
                try { yield AttributeValue.ofDouble(Double.parseDouble(s)); }
                catch (NumberFormatException e) {
                    throw new IllegalArgumentException("expected double");
                }
            }
            case BOOLEAN -> {
                String sl = s.toLowerCase();
                if (sl.equals("true") || sl.equals("1") || sl.equals("yes")) {
                    yield AttributeValue.ofBoolean(true);
                }
                if (sl.equals("false") || sl.equals("0") || sl.equals("no")) {
                    yield AttributeValue.ofBoolean(false);
                }
                throw new IllegalArgumentException("expected true/false");
            }
            case STRING_LIST -> AttributeValue.ofStringList(parseStringList(s));
            case DOUBLE_ARRAY -> AttributeValue.ofDoubleArray(parseDoubleArray(s));
            case INT_ARRAY -> AttributeValue.ofIntArray(parseIntArray(s));
            case STRING_MAP -> AttributeValue.ofStringMap(parseStringMap(s));
            case EMBEDDED_SPECTRUM -> throw new IllegalArgumentException("EMBEDDED_SPECTRUM is not editable in table");
        };
    }

    private static List<String> parseStringList(String s) {
        String[] parts = s.split(",");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        if (out.isEmpty()) {
            throw new IllegalArgumentException("string list is empty");
        }
        return out;
    }

    private static double[] parseDoubleArray(String s) {
        String[] parts = s.split(",");
        double[] out = new double[parts.length];
        int n = 0;
        for (String p : parts) {
            String t = p.trim();
            if (t.isEmpty()) {
                continue;
            }
            try {
                out[n++] = Double.parseDouble(t);
            }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException("invalid double: " + t);
            }
        }
        if (n == 0) {
            throw new IllegalArgumentException("double array is empty");
        }
        return n == out.length ? out : Arrays.copyOf(out, n);
    }

    private static int[] parseIntArray(String s) {
        String[] parts = s.split(",");
        int[] out = new int[parts.length];
        int n = 0;
        for (String p : parts) {
            String t = p.trim();
            if (t.isEmpty()) {
                continue;
            }
            try {
                out[n++] = Integer.parseInt(t);
            }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException("invalid int: " + t);
            }
        }
        if (n == 0) {
            throw new IllegalArgumentException("int array is empty");
        }
        return n == out.length ? out : Arrays.copyOf(out, n);
    }

    private static Map<String, String> parseStringMap(String s) {
        String[] pairs = s.split(",");
        Map<String, String> out = new LinkedHashMap<>();
        for (String pair : pairs) {
            String t = pair.trim();
            if (t.isEmpty()) {
                continue;
            }
            int eq = t.indexOf('=');
            if (eq <= 0) {
                throw new IllegalArgumentException("invalid map entry (expected key=value): " + t);
            }
            String k = t.substring(0, eq).trim();
            String v = t.substring(eq + 1).trim();
            if (k.isEmpty()) {
                throw new IllegalArgumentException("empty map key");
            }
            out.put(k, v);
        }
        if (out.isEmpty()) {
            throw new IllegalArgumentException("map is empty");
        }
        return out;
    }
}
