package org.esa.snap.rcp.session;

import org.esa.snap.core.util.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

public class XStreamSessionIOTest {

    private Path tempDirectory;

    @Before
    public void setUp() throws IOException {
        tempDirectory = Files.createTempDirectory("xstream_test");
    }

    @After
    public void tearDown()  {
        if (tempDirectory != null) {
            FileUtils.deleteTree(tempDirectory.toFile());
        }
    }

    @Test
    public void testWriteReadSession() throws Exception {
        Session session = new Session();

        Session.ProductRef productRef = new Session.ProductRef();
        productRef.refNo = 2;
        productRef.productReaderPlugin = "theReaderPlug";
        productRef.uri = new URI("file://some/where");
        session.productRefs = new Session.ProductRef[] {productRef};

        Session.ViewRef viewRef = new Session.ViewRef();
        viewRef.type = "testType";
        viewRef.productRefNo = 2;
        session.viewRefs = new Session.ViewRef[] {viewRef};
        session.modelVersion = "heffalump";

        XStreamSessionIO xStreamSessionIO = new XStreamSessionIO();

        File file = new File(tempDirectory.toFile(), "test,xml");
        xStreamSessionIO.writeSession(session, file);

        Session readSession = xStreamSessionIO.readSession(new FileReader(file));

        assertEquals("heffalump", readSession.modelVersion);

        assertEquals(1, readSession.productRefs.length);
        assertEquals(productRef.refNo, readSession.productRefs[0].refNo);
        assertEquals(productRef.productReaderPlugin, readSession.productRefs[0].productReaderPlugin);
        assertEquals(productRef.uri.toString(), readSession.productRefs[0].uri.toString());

        assertEquals(1, readSession.viewRefs.length);
        assertEquals(viewRef.type, readSession.viewRefs[0].type);
        assertEquals(viewRef.productRefNo, readSession.viewRefs[0].productRefNo);
    }
}
