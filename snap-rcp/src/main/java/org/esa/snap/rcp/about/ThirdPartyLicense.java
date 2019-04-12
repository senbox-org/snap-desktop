package org.esa.snap.rcp.about;

/**
 * Holder providing a description of a third party license as used in SNAP.
 *
 * @author olafd
 */
public class ThirdPartyLicense {
    // currently we use just 5 columns from the 9 columns in the csv file:
    private String name;
    private String descriptionUse;
    private String iprOwner;
    private String license;
    private String iprOwnerUrl;
    private String licenseUrl;

    ThirdPartyLicense(String name,
                             String descriptionUse,
                             String iprOwner,
                             String license,
                             String iprOwnerUrl,
                             String licenseUrl) {
        this.name = name;
        this.descriptionUse = descriptionUse;
        this.iprOwner = iprOwner;
        this.license = license;
        this.iprOwnerUrl = iprOwnerUrl;
        this.licenseUrl = licenseUrl;
    }

    public String getName() {
        return name;
    }

    String getDescriptionUse() {
        return descriptionUse;
    }

    String getIprOwner() {
        return iprOwner;
    }

    public String getLicense() {
        return license;
    }

    private String getIprOwnerUrl() {
        return iprOwnerUrl;
    }

    private String getLicenseUrl() {
        return licenseUrl;
    }


    /**
     * Provides the correct URL related to the colunm of the corresponding {@link ThirdPartyLicensesTableModel}
     *
     * @param tableColumnName - column name
     *
     * @return String
     */
    String getUrlByTableColumnName(String tableColumnName) {
        switch (tableColumnName) {
            case ThirdPartyLicensesTableModel.IPR_OWNER_COL_NAME:
                return getIprOwnerUrl();
            case ThirdPartyLicensesTableModel.LICENSE_COL_NAME:
                return getLicenseUrl();
            default:
                break;
        }
        return null;
    }
}
