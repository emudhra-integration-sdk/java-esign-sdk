package esign.text.testutils;

import esign.text.log.Logger;
import esign.text.log.LoggerFactory;
import java.io.File;
import java.io.IOException;

public abstract class ITextTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ITextTest.class.getName());

    public void runTest() throws Exception {
        LOGGER.info("Starting test.");
        String outPdf = getOutPdf();
        if (outPdf == null || outPdf.length() == 0) {
            throw new IOException("outPdf cannot be empty!");
        }
        makePdf(outPdf);
        assertPdf(outPdf);
        comparePdf(outPdf, getCmpPdf());
        LOGGER.info("Test complete.");
    }

    protected abstract void makePdf(String paramString) throws Exception;

    protected abstract String getOutPdf();

    protected void assertPdf(String outPdf) throws Exception {
    }

    protected void comparePdf(String outPdf, String cmpPdf) throws Exception {
    }

    protected String getCmpPdf() {
        return "";
    }

    protected void deleteDirectory(File path) {
        if (path == null) {
            return;
        }
        if (path.exists()) {
            for (File f : path.listFiles()) {
                if (f.isDirectory()) {
                    deleteDirectory(f);
                    f.delete();
                } else {
                    f.delete();
                }
            }
            path.delete();
        }
    }

    protected void deleteFiles(File path) {
        if (path != null && path.exists()) {
            for (File f : path.listFiles()) {
                f.delete();
            }
        }
    }
}

