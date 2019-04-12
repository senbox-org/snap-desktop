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
    static final String COMMENTS_COL_NAME = "Comments";

    private static final String NAME_COL_NAME = "Name";
    private static final String DESCRIPTION_USE_COL_NAME = "Description/Use";
//    private static final String COMPATIBLE_WITH_SNAP_GPL_COL_NAME = "Compatible with \n SNAP/GPLv3";
//    private static final String COMPATIBLE_WITH_SNAP_GPL_COL_NAME = "<html><b>Compatible with <br> SNAP/GPLv3</b></html>";
    private static final String COMPATIBLE_WITH_SNAP_GPL_COL_NAME = "<html><b>SNAP/GPLv3 <br> compatible</b></html>";

    static final int NAME_COL_INDEX = 0;
    static final int DESCRIPTION_USE_COL_INDEX = 1;
    static final int IPR_OWNER_COL_INDEX = 2;
    static final int LICENSE_COL_INDEX = 3;
    static final int COMPATIBLE_WITH_SNAP_GPL_COL_INDEX = 4;
    static final int COMMENTS_COL_INDEX = 5;

    private static final int NUMBER_OF_COLUMNS = 6;

    ThirdPartyLicensesTableModel(ThirdPartyLicense[] thirdPartyLicenses, int rowCount) {
        super(toArray(thirdPartyLicenses), new Object[]{NAME_COL_NAME,
                DESCRIPTION_USE_COL_NAME,
                IPR_OWNER_COL_NAME,
                LICENSE_COL_NAME,
                COMPATIBLE_WITH_SNAP_GPL_COL_NAME,
                COMMENTS_COL_NAME});
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
            case COMPATIBLE_WITH_SNAP_GPL_COL_INDEX:
                return COMPATIBLE_WITH_SNAP_GPL_COL_NAME;
            case COMMENTS_COL_INDEX:
                return COMMENTS_COL_NAME;
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
            case COMMENTS_COL_INDEX:
            case COMPATIBLE_WITH_SNAP_GPL_COL_INDEX:
                return String.class;
//            case IPR_OWNER_COL_INDEX:
//                return URI.class;
        }
        throw new IllegalStateException();
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        // todo if needed
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
        return column == IPR_OWNER_COL_INDEX || column == LICENSE_COL_INDEX || column == COMMENTS_COL_INDEX;
    }

    private static Object[][] toArray(ThirdPartyLicense[] thirdPartyLicenses) {
        Object[][] result = new Object[thirdPartyLicenses.length][NUMBER_OF_COLUMNS];
        for (int i = 0; i < thirdPartyLicenses.length; i++) {
            // create entries
            result[i][NAME_COL_INDEX] = thirdPartyLicenses[i].getName();
            result[i][DESCRIPTION_USE_COL_INDEX] = thirdPartyLicenses[i].getDescriptionUse();
            result[i][IPR_OWNER_COL_INDEX] = thirdPartyLicenses[i].getIprOwner();
            result[i][LICENSE_COL_INDEX] = thirdPartyLicenses[i].getLicense();
            result[i][COMPATIBLE_WITH_SNAP_GPL_COL_INDEX] = thirdPartyLicenses[i].getIsSnapCompatible();
            result[i][COMMENTS_COL_INDEX] = thirdPartyLicenses[i].getComments();
        }
        return result;
    }

}
