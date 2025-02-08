package Helpers;

import Ellithium.Utilities.helpers.PDFHelper;
import Ellithium.core.base.NonBDDSetup;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static Ellithium.Utilities.assertion.AssertionExecutor.hard.*;

public class PDFHelperTests extends NonBDDSetup {
    private static final String TEST_DIR = "src/test/resources/TestData/";
    private static final String TEST_PDF = TEST_DIR + "test.pdf";
    private static final String OUTPUT_PDF = TEST_DIR + "output.pdf";
    private static final String MERGED_PDF = TEST_DIR + "merged.pdf";
    private static final String COMPARE_PDF = TEST_DIR + "compare.pdf";

    @BeforeMethod
    public void setUp() throws IOException {
        try {
            new File(TEST_DIR).mkdirs();
            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA, 12);
                    contentStream.newLineAtOffset(100, 700);
                    contentStream.showText("Test Content");
                    contentStream.endText();
                }
                document.save(TEST_PDF);
            }
            Reporter.log("Test setup completed successfully", LogLevel.INFO_GREEN);
        } catch (IOException e) {
            Reporter.log("Failed to set up test environment: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @AfterMethod
    public void tearDown() throws IOException {
        try {
            Files.deleteIfExists(Path.of(TEST_PDF));
            Files.deleteIfExists(Path.of(OUTPUT_PDF));
            Files.deleteIfExists(Path.of(MERGED_PDF));
            Files.deleteIfExists(Path.of(COMPARE_PDF));
        } catch (IOException e) {
            Reporter.log("Failed to clean up test files: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testReadPdf() {
        try {
            String content = PDFHelper.readPdf(TEST_PDF);
            assertTrue(content.contains("Test Content"));
            Reporter.log("PDF read test passed successfully", LogLevel.INFO_GREEN);
        } catch (AssertionError e) {
            Reporter.log("PDF read test failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testWritePdf() {
        try {
            List<String> content = Arrays.asList("Line 1", "Line 2");
            PDFHelper.writePdf(OUTPUT_PDF, content);
            String readContent = PDFHelper.readPdf(OUTPUT_PDF);
            assertTrue(readContent.contains("Line 1"));
            assertTrue(readContent.contains("Line 2"));
            Reporter.log("PDF write test passed successfully", LogLevel.INFO_GREEN);
        } catch (AssertionError e) {
            Reporter.log("PDF write test failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testAppendToPdf() {
        List<String> content = Arrays.asList("Appended Line 1", "Appended Line 2");
        PDFHelper.appendToPdf(TEST_PDF, content);
        String readContent = PDFHelper.readPdf(TEST_PDF);
        assertTrue(readContent.contains("Test Content"));
        assertTrue(readContent.contains("Appended Line 1"));
    }

    @Test
    public void testMergePdfs() {
        PDFHelper.writePdf(COMPARE_PDF, Arrays.asList("Compare Content"));
        List<String> inputFiles = Arrays.asList(TEST_PDF, COMPARE_PDF);
        PDFHelper.mergePdfs(inputFiles, MERGED_PDF);
        assertTrue(Files.exists(Path.of(MERGED_PDF)));
        String mergedContent = PDFHelper.readPdf(MERGED_PDF);
        assertTrue(mergedContent.contains("Test Content"));
        assertTrue(mergedContent.contains("Compare Content"));
    }

    @Test
    public void testExtractPage() {
        PDFHelper.extractPdfPage(TEST_PDF, OUTPUT_PDF, 0);
        assertTrue(Files.exists(Path.of(OUTPUT_PDF)));
        String extractedContent = PDFHelper.readPdf(OUTPUT_PDF);
        assertTrue(extractedContent.contains("Test Content"));
    }

    @Test
    public void testComparePdfFiles() {
        PDFHelper.writePdf(COMPARE_PDF, Arrays.asList("Test Content"));
        assertTrue(PDFHelper.comparePdfFiles(TEST_PDF, COMPARE_PDF));
    }

    @Test
    public void testAddWatermark() {
        PDFHelper.addWatermark(TEST_PDF, OUTPUT_PDF, "WATERMARK");
        assertTrue(Files.exists(Path.of(OUTPUT_PDF)));
    }

    @Test
    public void testUpdateMetadata() {
        try {
            PDFHelper.updateMetadata(TEST_PDF, OUTPUT_PDF, "Test Title", "Test Author");
            try (PDDocument doc = PDDocument.load(new File(OUTPUT_PDF))) {
                assertEquals("Test Title", doc.getDocumentInformation().getTitle());
                assertEquals("Test Author", doc.getDocumentInformation().getAuthor());
                Reporter.log("Metadata update test passed successfully", LogLevel.INFO_GREEN);
            }
        } catch (IOException e) {
            Reporter.log("Failed to read metadata: ", LogLevel.ERROR, e.getMessage());
            fail("Failed to read metadata: " + e.getMessage());
        } catch (AssertionError e) {
            Reporter.log("Metadata verification failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testEncryptAndIsEncrypted() {
        try {
            String ownerPassword = "owner";
            String userPassword = "user";
            
            // First encrypt the PDF
            PDFHelper.encryptPdf(TEST_PDF, OUTPUT_PDF, ownerPassword, userPassword);
            
            // Verify encryption status using owner password
            boolean isEncryptedWithOwner = PDFHelper.isEncrypted(OUTPUT_PDF, ownerPassword);
            assertTrue(isEncryptedWithOwner, "PDF should be encrypted when checked with owner password");
            
            // Verify encryption status using user password
            boolean isEncryptedWithUser = PDFHelper.isEncrypted(OUTPUT_PDF, userPassword);
            assertTrue(isEncryptedWithUser, "PDF should be encrypted when checked with user password");
            
            Reporter.log("Encryption test passed successfully", LogLevel.INFO_GREEN);
        } catch (AssertionError e) {
            Reporter.log("Encryption test failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testRemovePages() {
        try (PDDocument doc = PDDocument.load(new File(TEST_PDF))) {
            doc.addPage(new PDPage());
            doc.addPage(new PDPage());
            doc.save(TEST_PDF);
        } catch (IOException e) {
            fail("Failed to prepare test document");
        }

        PDFHelper.removePages(TEST_PDF, OUTPUT_PDF, 1, 1);
        assertEquals(2, PDFHelper.getPageCount(OUTPUT_PDF));
    }

    @Test
    public void testRotatePage() {
        PDFHelper.rotatePage(TEST_PDF, OUTPUT_PDF, 0, 90);
        try (PDDocument doc = PDDocument.load(new File(OUTPUT_PDF))) {
            assertEquals(90, doc.getPage(0).getRotation());
        } catch (IOException e) {
            fail("Failed to verify page rotation");
        }
    }

    @Test
    public void testGetPageCount() {
        assertEquals(1, PDFHelper.getPageCount(TEST_PDF));
    }

    @Test
    public void testAddBlankPages() {
        PDFHelper.addBlankPages(TEST_PDF, 2);
        assertEquals(3, PDFHelper.getPageCount(TEST_PDF));
    }

    @Test
    public void testGetPageDimensions() {
        PDRectangle dimensions = PDFHelper.getPageDimensions(TEST_PDF, 0);
        assertNotNull(dimensions);
        assertEquals(PDRectangle.LETTER.getWidth(), dimensions.getWidth(), String.valueOf(0.1));
        assertEquals(PDRectangle.LETTER.getHeight(), dimensions.getHeight(), String.valueOf(0.1));
    }

    @Test
    public void testSplitPdf() {
        try {
            try (PDDocument doc = PDDocument.load(new File(TEST_PDF))) {
                doc.addPage(new PDPage());
                doc.save(TEST_PDF);
            }

            String splitDir = TEST_DIR + "split/";
            new File(splitDir).mkdirs();
            PDFHelper.splitPdf(TEST_PDF, splitDir);
            
            boolean page1Exists = Files.exists(Path.of(splitDir + "test_page1.pdf"));
            boolean page2Exists = Files.exists(Path.of(splitDir + "test_page2.pdf"));
            
            if (!page1Exists || !page2Exists) {
                Reporter.log("Split PDF verification failed", LogLevel.ERROR, 
                    "Page1 exists: " + page1Exists + ", Page2 exists: " + page2Exists);
                fail("Split PDF files not found");
            }

            Reporter.log("PDF split test passed successfully", LogLevel.INFO_GREEN);

            Files.walk(Path.of(splitDir))
                 .sorted((p1, p2) -> -p1.compareTo(p2))
                 .forEach(path -> {
                     try {
                         Files.delete(path);
                     } catch (IOException e) {
                         Reporter.log("Failed to delete split file: ", LogLevel.ERROR, path.toString());
                     }
                 });
        } catch (IOException e) {
            Reporter.log("Failed to execute split PDF test: ", LogLevel.ERROR, e.getMessage());
            fail("Split PDF test failed: " + e.getMessage());
        }
    }
}
