package org.esa.snap.rcp.about;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ThirdPartyLicensesTest {

    @Test
    public void testLoadSolarSpectrumData() {
        // solar_spectrum_2:
        final ThirdPartyLicensesCsvTable licensesCsvTable = new ThirdPartyLicensesCsvTable();
        assertNotNull(licensesCsvTable);
        assertEquals(130, licensesCsvTable.getLength());

        assertEquals("Java SE JRE", licensesCsvTable.getName(0));
        assertEquals("olingo", licensesCsvTable.getName(67));
        assertEquals("iText", licensesCsvTable.getName(129));

        assertEquals("Java Runtime", licensesCsvTable.getDescrUse(0));
        assertEquals("Open Data Protocol", licensesCsvTable.getDescrUse(67));
        assertEquals("PDF Library", licensesCsvTable.getDescrUse(129));

        assertEquals("Oracle", licensesCsvTable.getIprOwner(0));
        assertEquals("olingo", licensesCsvTable.getIprOwner(67));
        assertEquals("lowagie", licensesCsvTable.getIprOwner(129));

        assertEquals("Oracle Binary Code License", licensesCsvTable.getLicense(0));
        assertEquals("Apache License", licensesCsvTable.getLicense(67));
        assertEquals("MPL", licensesCsvTable.getLicense(129));

        assertEquals("YES", licensesCsvTable.getCompatibleWithSnapGpl(0));
        assertEquals("YES", licensesCsvTable.getCompatibleWithSnapGpl(67));
        assertEquals("YES", licensesCsvTable.getCompatibleWithSnapGpl(129));

        assertEquals("Distribution FAQs", licensesCsvTable.getComment(0));
        assertEquals("NONE", licensesCsvTable.getComment(67));
        assertEquals("NONE", licensesCsvTable.getComment(129));

        assertEquals("http://www.oracle.com/technetwork/java/javase/overview/index.html", licensesCsvTable.getIprOwnerUrl(0));
        assertEquals("http://olingo.apache.org/", licensesCsvTable.getIprOwnerUrl(67));
        assertEquals("http://www.lowagie.com/iText/", licensesCsvTable.getIprOwnerUrl(129));

        assertEquals("http://www.oracle.com/technetwork/java/javase/terms/license/index.html", licensesCsvTable.getLicenseUrl(0));
        assertEquals("https://www.apache.org/licenses/", licensesCsvTable.getLicenseUrl(67));
        assertEquals("https://www.mozilla.org/en-US/MPL/", licensesCsvTable.getLicenseUrl(129));

        assertEquals("https://java.com/en/download/faq/", licensesCsvTable.getCommentUrl(0));
        assertEquals("NONE", licensesCsvTable.getCommentUrl(67));
        assertEquals("http://www.eclipse.org/legal/epl-2.0/", licensesCsvTable.getCommentUrl(117));
        assertEquals("NONE", licensesCsvTable.getCommentUrl(129));
    }
}
