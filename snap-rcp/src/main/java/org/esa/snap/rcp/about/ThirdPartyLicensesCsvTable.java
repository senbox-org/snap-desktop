package org.esa.snap.rcp.about;

import org.esa.snap.core.gpf.OperatorException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Holder for Third Party Licenses Table as provided from csv file.
 *
 * @author olafd
 */
public class ThirdPartyLicensesCsvTable {

    private List<String> name;
    private List<String> descrUse;
    private List<String> iprOwner;
    private List<String> iprOwnerUrl;
    private List<String> license;
    private List<String> licenseUrl;

    private BufferedReader bufferedReader;

    /**
     * Provides the licenses csv table from a resource file.
     */
    ThirdPartyLicensesCsvTable(Reader licensesReader) {
        bufferedReader = new BufferedReader(licensesReader);
        loadTable();
    }

    private void loadTable() {
        // currently we have 6 columns in file:
        this.name = new ArrayList<>();
        this.descrUse = new ArrayList<>();
        this.iprOwner = new ArrayList<>();
        this.license = new ArrayList<>();
        this.iprOwnerUrl = new ArrayList<>();
        this.licenseUrl = new ArrayList<>();

        readTable();
    }

    private void readTable() {
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
                    setIprOwnerUrl(i, st.nextToken());
                }
                if (st.hasMoreTokens()) {
                    setLicenseUrl(i, st.nextToken());
                }

                i++;
            }
        } catch (IOException | NumberFormatException e) {
            throw new OperatorException("Failed to load 3rd-party license table: \n" + e.getMessage(), e);
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

}
