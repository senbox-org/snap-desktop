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

        final int abderaIdx = 0;
        final int oroIdx = 63;
        final int iTextIdx = 112;

        assertEquals("abdera", licensesCsvTable.getName(abderaIdx));
        assertEquals("oro", licensesCsvTable.getName(oroIdx));
        assertEquals("iText", licensesCsvTable.getName(iTextIdx));

        assertEquals("Atom client", licensesCsvTable.getDescrUse(abderaIdx));
        assertEquals("Text processing", licensesCsvTable.getDescrUse(oroIdx));
        assertEquals("PDF Library", licensesCsvTable.getDescrUse(iTextIdx));

        assertEquals("Apache", licensesCsvTable.getIprOwner(0));
        assertEquals("ORO", licensesCsvTable.getIprOwner(oroIdx));
        assertEquals("lowagie", licensesCsvTable.getIprOwner(iTextIdx));

        assertEquals("Apache License", licensesCsvTable.getLicense(abderaIdx));
        assertEquals("Apache License", licensesCsvTable.getLicense(oroIdx));
        assertEquals("MPL", licensesCsvTable.getLicense(iTextIdx));

        assertEquals("http://abdera.apache.org/", licensesCsvTable.getIprOwnerUrl(abderaIdx));
        assertEquals("http://jakarta.apache.org/oro/", licensesCsvTable.getIprOwnerUrl(oroIdx));
        assertEquals("http://www.lowagie.com/iText/", licensesCsvTable.getIprOwnerUrl(iTextIdx));

        assertEquals("https://www.apache.org/licenses/", licensesCsvTable.getLicenseUrl(abderaIdx));
        assertEquals("https://www.apache.org/licenses/", licensesCsvTable.getLicenseUrl(oroIdx));
        assertEquals("https://www.mozilla.org/en-US/MPL/", licensesCsvTable.getLicenseUrl(iTextIdx));

    }

}
