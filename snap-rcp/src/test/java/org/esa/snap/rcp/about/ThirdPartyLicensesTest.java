package org.esa.snap.rcp.about;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ThirdPartyLicensesTest {

    @Test
    public void testLoadThirdPartyLicensesFromCsv_fromResource() {
        InputStream is = ThirdPartyLicensesTest.class.getResourceAsStream("TEST_THIRDPARTY_LICENSES.txt");
        final ThirdPartyLicensesCsvTable licensesCsvTable = new ThirdPartyLicensesCsvTable(new BufferedReader(new InputStreamReader(is)));
        assertNotNull(licensesCsvTable);

        final int javaIdx = 0;
        final int olingoIdx = 63;
        final int iTextIdx = 113;

        assertEquals("Java SE JRE", licensesCsvTable.getName(javaIdx));
        assertEquals("olingo", licensesCsvTable.getName(olingoIdx));
        assertEquals("iText", licensesCsvTable.getName(iTextIdx));

        assertEquals("Java Runtime", licensesCsvTable.getDescrUse(javaIdx));
        assertEquals("Open Data Protocol", licensesCsvTable.getDescrUse(olingoIdx));
        assertEquals("PDF Library", licensesCsvTable.getDescrUse(iTextIdx));

        assertEquals("Oracle", licensesCsvTable.getIprOwner(0));
        assertEquals("olingo", licensesCsvTable.getIprOwner(olingoIdx));
        assertEquals("lowagie", licensesCsvTable.getIprOwner(iTextIdx));

        assertEquals("Oracle Binary Code License", licensesCsvTable.getLicense(javaIdx));
        assertEquals("Apache License", licensesCsvTable.getLicense(olingoIdx));
        assertEquals("MPL", licensesCsvTable.getLicense(iTextIdx));

        assertEquals("http://www.oracle.com/technetwork/java/javase/overview/index.html", licensesCsvTable.getIprOwnerUrl(javaIdx));
        assertEquals("http://olingo.apache.org/", licensesCsvTable.getIprOwnerUrl(olingoIdx));
        assertEquals("http://www.lowagie.com/iText/", licensesCsvTable.getIprOwnerUrl(iTextIdx));

        assertEquals("http://www.oracle.com/technetwork/java/javase/terms/license/index.html", licensesCsvTable.getLicenseUrl(javaIdx));
        assertEquals("https://www.apache.org/licenses/", licensesCsvTable.getLicenseUrl(olingoIdx));
        assertEquals("https://www.mozilla.org/en-US/MPL/", licensesCsvTable.getLicenseUrl(iTextIdx));

    }

}
