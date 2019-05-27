package org.esa.snap.rcp.about;

import javax.swing.table.DefaultTableModel;

/**
 * Table model for 3rd party licenses displayed in AboutBox
 *
 * @author olafd
 */
public class ThirdPartyLicensesTableModel extends DefaultTableModel {

    static final String IPR_OWNER_COL_NAME = "IPR owner";
    static final String LICENSE_COL_NAME = "License";

    private static final String NAME_COL_NAME = "Name";
    private static final String DESCRIPTION_USE_COL_NAME = "Description/Use";

    static final int NAME_COL_INDEX = 0;
    static final int DESCRIPTION_USE_COL_INDEX = 1;
    static final int IPR_OWNER_COL_INDEX = 2;
    static final int LICENSE_COL_INDEX = 3;

    private static final int NUMBER_OF_COLUMNS = 4;

    ThirdPartyLicensesTableModel(ThirdPartyLicense[] thirdPartyLicenses) {
        super(toArray(thirdPartyLicenses), new Object[]{NAME_COL_NAME,
                DESCRIPTION_USE_COL_NAME,
                IPR_OWNER_COL_NAME,
                LICENSE_COL_NAME});
    }

    @Override
    public int getColumnCount() {
        return NUMBER_OF_COLUMNS;
    }

    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case NAME_COL_INDEX:
                return NAME_COL_NAME;
            case DESCRIPTION_USE_COL_INDEX:
                return DESCRIPTION_USE_COL_NAME;
            case IPR_OWNER_COL_INDEX:
                return IPR_OWNER_COL_NAME;
            case LICENSE_COL_INDEX:
                return LICENSE_COL_NAME;
        }
        throw new IllegalStateException("Should never come here");
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case NAME_COL_INDEX:
            case DESCRIPTION_USE_COL_INDEX:
            case LICENSE_COL_INDEX:
            case IPR_OWNER_COL_INDEX:
                return String.class;
        }
        throw new IllegalStateException();
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    /**
     * Checks if given column contains URLs
     *
     * @param column - column index
     *
     * @return boolean
     */
    boolean containsURLs(int column) {
        return column == IPR_OWNER_COL_INDEX || column == LICENSE_COL_INDEX;
    }

    private static Object[][] toArray(ThirdPartyLicense[] thirdPartyLicenses) {
        Object[][] result = new Object[thirdPartyLicenses.length][NUMBER_OF_COLUMNS];
        for (int i = 0; i < thirdPartyLicenses.length; i++) {
            // create entries
            result[i][NAME_COL_INDEX] = thirdPartyLicenses[i].getName();
            result[i][DESCRIPTION_USE_COL_INDEX] = thirdPartyLicenses[i].getDescriptionUse();
            result[i][IPR_OWNER_COL_INDEX] = thirdPartyLicenses[i].getIprOwner();
            result[i][LICENSE_COL_INDEX] = thirdPartyLicenses[i].getLicense();
        }
        return result;
    }

}
