package org.esa.snap.rcp.about;

import org.esa.snap.core.gpf.OperatorException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Holder for Third Party Licenses Table as provided from csv file.
 *
 * @author olafd
 */
public class ThirdPartyLicensesCsvTable {
    private static final String THIRD_PARTY_LICENSE_DEFAULT_FILE_NAME = "THIRDPARTY_LICENSES.txt";

    private List<String> name;
    private List<String> descrUse;
    private List<String> iprOwner;
    private List<String> license;
    private List<String> compatibleWithSnapGpl;
    private List<String> comment;
    private List<String> iprOwnerUrl;
    private List<String> licenseUrl;
    private List<String> commentUrl;

    private BufferedReader bufferedReader;
    private InputStream inputStream;

    /**
     * Provides the licenses csv table from a resource file.
     *
     */
    ThirdPartyLicensesCsvTable() {
        inputStream = getClass().getResourceAsStream(THIRD_PARTY_LICENSE_DEFAULT_FILE_NAME);
        bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        loadTable();
    }

    /**
     * Provides the licenses csv table from an arbitrary file.
     *
     * @param filename - full path of arbitrary csv file
     */
    ThirdPartyLicensesCsvTable(String filename) {
        try {
            inputStream = new FileInputStream(filename);
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        loadTable();
    }

    private void loadTable() {
        // currently we have 9 columns in file:
        this.name = new ArrayList<>();
        this.descrUse = new ArrayList<>();
        this.iprOwner = new ArrayList<>();
        this.license = new ArrayList<>();
        this.compatibleWithSnapGpl = new ArrayList<>();
        this.comment = new ArrayList<>();
        this.iprOwnerUrl = new ArrayList<>();
        this.licenseUrl = new ArrayList<>();
        this.commentUrl = new ArrayList<>();

        readTableFromFile();
    }

    private void readTableFromFile() {
        StringTokenizer st;
        try {
            int i = 0;
            String line;
            bufferedReader.readLine(); // skip header
            while ((line = bufferedReader.readLine()) != null) {
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

    public List<String> getName() {
        return name;
    }

    private void setName(int index, String name) {
        this.name.add(index, name);
    }
    public String getName(int index) {
        return name.get(index);
    }

    private void setDescrUse(int index, String descrUse) {
        this.descrUse.add(index, descrUse);
    }
    String getDescrUse(int index) {
        return descrUse.get(index);
    }

    private void setIprOwner(int index, String iprOwner) {
        this.iprOwner.add(index, iprOwner);
    }
    String getIprOwner(int index) {
        return iprOwner.get(index);
    }

    private void setLicense(int index, String license) {
        this.license.add(index, license);
    }
    String getLicense(int index) {
        return license.get(index);
    }

    private void setCompatibleWithSnapGpl(int index, String compatibleWithSnapGpl) {
        this.compatibleWithSnapGpl.add(index, compatibleWithSnapGpl);
    }
    String getCompatibleWithSnapGpl(int index) {
        return compatibleWithSnapGpl.get(index);
    }

    private void setComment(int index, String comment) {
        this.comment.add(index, comment);
    }
    String getComment(int index) {
        return comment.get(index);
    }

    private void setIprOwnerUrl(int index, String iprOwnerUrl) {
        this.iprOwnerUrl.add(index, iprOwnerUrl);
    }
    String getIprOwnerUrl(int index) {
        return iprOwnerUrl.get(index);
    }

    private void setLicenseUrl(int index, String licenseUrl) {
        this.licenseUrl.add(index, licenseUrl);
    }
    String getLicenseUrl(int index) {
        return licenseUrl.get(index);
    }

    private void setCommentUrl(int index, String commentUrl) {
        this.commentUrl.add(index, commentUrl);
    }
    String getCommentUrl(int index) {
        return commentUrl.get(index);
    }

}
