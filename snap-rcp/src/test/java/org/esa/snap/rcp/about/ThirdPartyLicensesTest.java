package org.esa.snap.rcp.about;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ThirdPartyLicensesTest {

    @Test
    public void testLoadThirdPartyLicensesFromCsv_fromResource() {
        final ThirdPartyLicensesCsvTable licensesCsvTable = new ThirdPartyLicensesCsvTable();
        assertNotNull(licensesCsvTable);

        assertEquals("Java SE JRE", licensesCsvTable.getName(0));
        assertEquals("olingo", licensesCsvTable.getName(62));
        assertEquals("iText", licensesCsvTable.getName(112));

        assertEquals("Java Runtime", licensesCsvTable.getDescrUse(0));
        assertEquals("Open Data Protocol", licensesCsvTable.getDescrUse(62));
        assertEquals("PDF Library", licensesCsvTable.getDescrUse(112));

        assertEquals("Oracle", licensesCsvTable.getIprOwner(0));
        assertEquals("olingo", licensesCsvTable.getIprOwner(62));
        assertEquals("lowagie", licensesCsvTable.getIprOwner(112));

        assertEquals("Oracle Binary Code License", licensesCsvTable.getLicense(0));
        assertEquals("Apache License", licensesCsvTable.getLicense(62));
        assertEquals("MPL", licensesCsvTable.getLicense(112));

        assertEquals("YES", licensesCsvTable.getCompatibleWithSnapGpl(0));
        assertEquals("YES", licensesCsvTable.getCompatibleWithSnapGpl(62));
        assertEquals("YES", licensesCsvTable.getCompatibleWithSnapGpl(112));

        assertEquals("Distribution FAQs", licensesCsvTable.getComment(0));
        assertEquals("NONE", licensesCsvTable.getComment(62));
        assertEquals("NONE", licensesCsvTable.getComment(112));

        assertEquals("http://www.oracle.com/technetwork/java/javase/overview/index.html", licensesCsvTable.getIprOwnerUrl(0));
        assertEquals("http://olingo.apache.org/", licensesCsvTable.getIprOwnerUrl(62));
        assertEquals("http://www.lowagie.com/iText/", licensesCsvTable.getIprOwnerUrl(112));

        assertEquals("http://www.oracle.com/technetwork/java/javase/terms/license/index.html", licensesCsvTable.getLicenseUrl(0));
        assertEquals("https://www.apache.org/licenses/", licensesCsvTable.getLicenseUrl(62));
        assertEquals("https://www.mozilla.org/en-US/MPL/", licensesCsvTable.getLicenseUrl(112));

        assertEquals("https://java.com/en/download/faq/", licensesCsvTable.getCommentUrl(0));
        assertEquals("NONE", licensesCsvTable.getCommentUrl(62));
        assertEquals("NONE", licensesCsvTable.getCommentUrl(112));
    }

    @Test
    public void testLoadThirdPartyLicensesFromCsv_fromFile() {
        final String licensesFileString = ThirdPartyLicensesCsvTable.class.getResource("THIRDPARTY_LICENSES.txt").getPath();

        final ThirdPartyLicensesCsvTable licensesCsvTable = new ThirdPartyLicensesCsvTable(licensesFileString);
        assertNotNull(licensesCsvTable);

        assertEquals("Java SE JRE", licensesCsvTable.getName(0));
        assertEquals("olingo", licensesCsvTable.getName(62));
        assertEquals("iText", licensesCsvTable.getName(112));

        assertEquals("Java Runtime", licensesCsvTable.getDescrUse(0));
        assertEquals("Open Data Protocol", licensesCsvTable.getDescrUse(62));
        assertEquals("PDF Library", licensesCsvTable.getDescrUse(112));

        assertEquals("Oracle", licensesCsvTable.getIprOwner(0));
        assertEquals("olingo", licensesCsvTable.getIprOwner(62));
        assertEquals("lowagie", licensesCsvTable.getIprOwner(112));

        assertEquals("Oracle Binary Code License", licensesCsvTable.getLicense(0));
        assertEquals("Apache License", licensesCsvTable.getLicense(62));
        assertEquals("MPL", licensesCsvTable.getLicense(112));

        assertEquals("YES", licensesCsvTable.getCompatibleWithSnapGpl(0));
        assertEquals("YES", licensesCsvTable.getCompatibleWithSnapGpl(62));
        assertEquals("YES", licensesCsvTable.getCompatibleWithSnapGpl(112));

        assertEquals("Distribution FAQs", licensesCsvTable.getComment(0));
        assertEquals("NONE", licensesCsvTable.getComment(62));
        assertEquals("NONE", licensesCsvTable.getComment(112));

        assertEquals("http://www.oracle.com/technetwork/java/javase/overview/index.html", licensesCsvTable.getIprOwnerUrl(0));
        assertEquals("http://olingo.apache.org/", licensesCsvTable.getIprOwnerUrl(62));
        assertEquals("http://www.lowagie.com/iText/", licensesCsvTable.getIprOwnerUrl(112));

        assertEquals("http://www.oracle.com/technetwork/java/javase/terms/license/index.html", licensesCsvTable.getLicenseUrl(0));
        assertEquals("https://www.apache.org/licenses/", licensesCsvTable.getLicenseUrl(62));
        assertEquals("https://www.mozilla.org/en-US/MPL/", licensesCsvTable.getLicenseUrl(112));

        assertEquals("https://java.com/en/download/faq/", licensesCsvTable.getCommentUrl(0));
        assertEquals("NONE", licensesCsvTable.getCommentUrl(62));
        assertEquals("NONE", licensesCsvTable.getCommentUrl(112));
    }
}
