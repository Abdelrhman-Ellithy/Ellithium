package Ellithium.Utilities.helpers;

import Ellithium.Utilities.helpers.PDFHelper;
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

public class PDFHelperTests {
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
            Reporter.log("Test cleanup completed successfully", LogLevel.INFO_GREEN);
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
        try {
            List<String> content = Arrays.asList("Appended Line 1", "Appended Line 2");
            PDFHelper.appendToPdf(TEST_PDF, content);
            String readContent = PDFHelper.readPdf(TEST_PDF);
            assertTrue(readContent.contains("Test Content"));
            assertTrue(readContent.contains("Appended Line 1"));
            Reporter.log("PDF append test passed successfully", LogLevel.INFO_GREEN);
        } catch (AssertionError e) {
            Reporter.log("PDF append test failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testMergePdfs() {
        try {
            PDFHelper.writePdf(COMPARE_PDF, Arrays.asList("Compare Content"));
            List<String> inputFiles = Arrays.asList(TEST_PDF, COMPARE_PDF);
            PDFHelper.mergePdfs(inputFiles, MERGED_PDF);
            assertTrue(Files.exists(Path.of(MERGED_PDF)));
            String mergedContent = PDFHelper.readPdf(MERGED_PDF);
            assertTrue(mergedContent.contains("Test Content"));
            assertTrue(mergedContent.contains("Compare Content"));
            Reporter.log("PDF merge test passed successfully", LogLevel.INFO_GREEN);
        } catch (AssertionError e) {
            Reporter.log("PDF merge test failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testExtractPage() {
        try {
            PDFHelper.extractPdfPage(TEST_PDF, OUTPUT_PDF, 0);
            assertTrue(Files.exists(Path.of(OUTPUT_PDF)));
            String extractedContent = PDFHelper.readPdf(OUTPUT_PDF);
            assertTrue(extractedContent.contains("Test Content"));
            Reporter.log("PDF page extraction test passed successfully", LogLevel.INFO_GREEN);
        } catch (AssertionError e) {
            Reporter.log("PDF page extraction test failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testComparePdfFiles() {
        try {
            PDFHelper.writePdf(COMPARE_PDF, Arrays.asList("Test Content"));
            assertTrue(PDFHelper.comparePdfFiles(TEST_PDF, COMPARE_PDF));
            Reporter.log("PDF comparison test passed successfully", LogLevel.INFO_GREEN);
        } catch (AssertionError e) {
            Reporter.log("PDF comparison test failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testAddWatermark() {
        try {
            PDFHelper.addWatermark(TEST_PDF, OUTPUT_PDF, "WATERMARK");
            assertTrue(Files.exists(Path.of(OUTPUT_PDF)));
            Reporter.log("PDF watermark test passed successfully", LogLevel.INFO_GREEN);
        } catch (AssertionError e) {
            Reporter.log("PDF watermark test failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
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
        try {
            try (PDDocument doc = PDDocument.load(new File(TEST_PDF))) {
                doc.addPage(new PDPage());
                doc.addPage(new PDPage());
                doc.save(TEST_PDF);
            }
            PDFHelper.removePages(TEST_PDF, OUTPUT_PDF, 1, 1);
            assertEquals(2, PDFHelper.getPageCount(OUTPUT_PDF));
            Reporter.log("PDF remove pages test passed successfully", LogLevel.INFO_GREEN);
        } catch (IOException e) {
            Reporter.log("Failed to prepare or verify remove pages test: ", LogLevel.ERROR, e.getMessage());
            fail("Failed to prepare test document");
        } catch (AssertionError e) {
            Reporter.log("PDF remove pages test failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testRotatePage() {
        try {
            PDFHelper.rotatePage(TEST_PDF, OUTPUT_PDF, 0, 90);
            try (PDDocument doc = PDDocument.load(new File(OUTPUT_PDF))) {
                assertEquals(90, doc.getPage(0).getRotation());
            }
            Reporter.log("PDF rotate page test passed successfully", LogLevel.INFO_GREEN);
        } catch (IOException e) {
            Reporter.log("Failed to verify page rotation: ", LogLevel.ERROR, e.getMessage());
            fail("Failed to verify page rotation");
        } catch (AssertionError e) {
            Reporter.log("PDF rotate page test failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testGetPageCount() {
        try {
            assertEquals(1, PDFHelper.getPageCount(TEST_PDF));
            Reporter.log("PDF page count test passed successfully", LogLevel.INFO_GREEN);
        } catch (AssertionError e) {
            Reporter.log("PDF page count test failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testAddBlankPages() {
        try {
            PDFHelper.addBlankPages(TEST_PDF, 2);
            assertEquals(3, PDFHelper.getPageCount(TEST_PDF));
            Reporter.log("PDF add blank pages test passed successfully", LogLevel.INFO_GREEN);
        } catch (AssertionError e) {
            Reporter.log("PDF add blank pages test failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }

    @Test
    public void testGetPageDimensions() {
        try {
            PDRectangle dimensions = PDFHelper.getPageDimensions(TEST_PDF, 0);
            assertNotNull(dimensions);
            assertEquals(dimensions.getWidth(), PDRectangle.LETTER.getWidth(), 0.1, "Page width should match LETTER width");
            assertEquals(dimensions.getHeight(), PDRectangle.LETTER.getHeight(), 0.1, "Page height should match LETTER height");
            Reporter.log("PDF page dimensions test passed successfully", LogLevel.INFO_GREEN);
        } catch (AssertionError e) {
            Reporter.log("PDF page dimensions test failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        }
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

    @Test(groups = {"pdf"})
    public void testEncryptAndDecryptPdf() {
        String originalFile = "src/test/resources/TestData/sample.pdf";
        String encryptedFile = "src/test/resources/TestData/encrypted.pdf";
        String ownerPassword = "owner123";
        String userPassword = "user123";
        try {
            PDFHelper.writePdf(originalFile, java.util.Arrays.asList("Test PDF Encryption"));
            PDFHelper.encryptPdf(originalFile, encryptedFile, ownerPassword, userPassword);
            boolean isEncrypted = PDFHelper.isEncrypted(encryptedFile, userPassword);
            assertTrue(isEncrypted);
            Reporter.log("PDF encrypt/decrypt test passed successfully", LogLevel.INFO_GREEN);
        } catch (AssertionError e) {
            Reporter.log("PDF encrypt/decrypt test failed: ", LogLevel.ERROR, e.getMessage());
            throw e;
        } finally {
            try {
                Files.deleteIfExists(Path.of(originalFile));
                Files.deleteIfExists(Path.of(encryptedFile));
            } catch (IOException e) {
                Reporter.log("Failed to clean up encrypt test files: ", LogLevel.WARN, e.getMessage());
            }
        }
    }
}
