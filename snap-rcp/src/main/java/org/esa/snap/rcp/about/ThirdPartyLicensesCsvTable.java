package org.esa.snap.rcp.about;

import org.esa.snap.core.gpf.OperatorException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

/**
 * Holder for Third Party Licenses Table as provided from csv file.
 *
 * @author olafd
 */
public class ThirdPartyLicensesCsvTable {
    private static final int THIRD_PARTY_LICENSE_TABLE_DEFAULT_LENGTH = 130;
    private static final String THIRD_PARTY_LICENSE_DEFAULT_FILE_NAME = "thirdpartylicenses.csv";

    private String filename;
    private int length;

    private String[] name;
    private String[] descrUse;
    private String[] iprOwner;
    private String[] license;
    private String[] compatibleWithSnapGpl;
    private String[] comment;
    private String[] iprOwnerUrl;
    private String[] licenseUrl;
    private String[] commentUrl;

    ThirdPartyLicensesCsvTable() {
        filename = THIRD_PARTY_LICENSE_DEFAULT_FILE_NAME;
        length = THIRD_PARTY_LICENSE_TABLE_DEFAULT_LENGTH;

        name = new String[length];
        descrUse = new String[length];
        iprOwner = new String[length];
        license = new String[length];
        compatibleWithSnapGpl = new String[length];
        comment = new String[length];
        iprOwnerUrl = new String[length];
        licenseUrl = new String[length];
        commentUrl = new String[length];

        readTableFromFile();
    }

    private void readTableFromFile() {
        final InputStream inputStream = getClass().getResourceAsStream(filename);

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        StringTokenizer st;
        try {
            int i = 0;
            String line;
            bufferedReader.readLine(); // skip header
            while ((line = bufferedReader.readLine()) != null && i < length) {
                line = line.trim();
                st = new StringTokenizer(line, ";", false);

                if (st.hasMoreTokens()) {
                    setName(i, st.nextToken());
                }
                if (st.hasMoreTokens()) {
                    setDescrUse(i, st.nextToken());
                }
                if (st.hasMoreTokens()) {
                    setIprOwner(i, st.nextToken());
                }
                if (st.hasMoreTokens()) {
                    setLicense(i, st.nextToken());
                }
                if (st.hasMoreTokens()) {
                    setCompatibleWithSnapGpl(i, st.nextToken());
                }
                if (st.hasMoreTokens()) {
                    setComment(i, st.nextToken());
                }
                if (st.hasMoreTokens()) {
                    setIprOwnerUrl(i, st.nextToken());
                }
                if (st.hasMoreTokens()) {
                    setLicenseUrl(i, st.nextToken());
                }
                if (st.hasMoreTokens()) {
                    setCommentUrl(i, st.nextToken());
                }

                i++;
            }
        } catch (IOException | NumberFormatException e) {
            throw new OperatorException("Failed to load SolarSpectrumTable: \n" + e.getMessage(), e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void setName(int index, String name) {
        this.name[index] = name;
    }
    public String getName(int index) {
        return name[index];
    }

    private void setDescrUse(int index, String descrUse) {
        this.descrUse[index] = descrUse;
    }
    String getDescrUse(int index) {
        return descrUse[index];
    }

    private void setIprOwner(int index, String iprOwner) {
        this.iprOwner[index] = iprOwner;
    }
    String getIprOwner(int index) {
        return iprOwner[index];
    }

    private void setLicense(int index, String license) {
        this.license[index] = license;
    }
    String getLicense(int index) {
        return license[index];
    }

    private void setCompatibleWithSnapGpl(int index, String compatibleWithSnapGpl) {
        this.compatibleWithSnapGpl[index] = compatibleWithSnapGpl;
    }
    String getCompatibleWithSnapGpl(int index) {
        return compatibleWithSnapGpl[index];
    }

    private void setComment(int index, String comment) {
        this.comment[index] = comment;
    }
    String getComment(int index) {
        return comment[index];
    }

    private void setIprOwnerUrl(int index, String iprOwnerUrl) {
        this.iprOwnerUrl[index] = iprOwnerUrl;
    }
    String getIprOwnerUrl(int index) {
        return iprOwnerUrl[index];
    }

    private void setLicenseUrl(int index, String licenseUrl) {
        this.licenseUrl[index] = licenseUrl;
    }
    String getLicenseUrl(int index) {
        return licenseUrl[index];
    }

    private void setCommentUrl(int index, String commentUrl) {
        this.commentUrl[index] = commentUrl;
    }
    String getCommentUrl(int index) {
        return commentUrl[index];
    }

    public int getLength() {
        return length;
    }
}
