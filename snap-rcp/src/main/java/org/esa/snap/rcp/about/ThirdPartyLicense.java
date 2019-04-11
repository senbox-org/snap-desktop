package org.esa.snap.rcp.about;

/**
 * Holder providing a description of a third party license as used in SNAP.
 *
 * @author olafd
 */
public class ThirdPartyLicense {
    private String name;
    private String descriptionUse;
    private String iprOwner;
    private String license;
    private String isSnapCompatible;
    private String comments;
    private String iprOwnerUrl;
    private String licenseUrl;
    private String commentsUrl;

    ThirdPartyLicense(String name,
                             String descriptionUse,
                             String iprOwner,
                             String license,
                             String isSnapCompatible,
                             String comments,
                             String iprOwnerUrl,
                             String licenseUrl,
                             String commentsUrl) {
        this.name = name;
        this.descriptionUse = descriptionUse;
        this.iprOwner = iprOwner;
        this.license = license;
        this.isSnapCompatible = isSnapCompatible;
        this.comments = comments;
        this.iprOwnerUrl = iprOwnerUrl;
        this.licenseUrl = licenseUrl;
        this.commentsUrl = commentsUrl;
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

    String getIsSnapCompatible() {
        return isSnapCompatible;
    }

    public String getComments() {
        return comments;
    }

    private String getIprOwnerUrl() {
        return iprOwnerUrl;
    }

    private String getLicenseUrl() {
        return licenseUrl;
    }

    private String getCommentsUrl() {
        return commentsUrl;
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
            case ThirdPartyLicensesTableModel.COMMENTS_COL_NAME:
                return getCommentsUrl();
            default:
                break;
        }
        return null;
    }
}
