package Ellithium.Utilities.helpers;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for performing various PDF operations such as reading, writing, merging, and encryption.
 */
public class PDFHelper {

    private static final ConcurrentHashMap<String, Object> fileLocks = new ConcurrentHashMap<>();
    private static Object getFileLock(String filePath) {
        return fileLocks.computeIfAbsent(filePath, k -> new Object());
    }

    /**
     * Reads text content from a PDF file.
     * @param filePath the PDF file path.
     * @return extracted text as a String.
     */
    public static String readPdf(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            Reporter.log("File does not exist: ", LogLevel.ERROR, filePath);
            return "";
        }
        synchronized (getFileLock(filePath)) {
            try (PDDocument document = PDDocument.load(file)) {
                PDFTextStripper stripper = new PDFTextStripper();
                String textContent = stripper.getText(document);
                Reporter.log("Successfully read PDF file: ", LogLevel.INFO_GREEN, filePath);
                return textContent;
            } catch (IOException e) {
                Reporter.log("Failed to read PDF file: ", LogLevel.ERROR, filePath);
                Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage() != null ? e.getMessage() : "Unknown error");
                return "";
            }
        }
    }

    /**
     * Writes a list of strings as content into a PDF file.
     * @param filePath the target PDF file path.
     * @param content list of text lines to be written.
     */
    public static void writePdf(String filePath, List<String> content) {
        File file = new File(filePath);
        synchronized (getFileLock(filePath)) {
            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage();
                document.addPage(page);
                float yPosition = 700;
                PDPageContentStream contentStream = null;
                try {
                    contentStream = new PDPageContentStream(document, page);
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA, 12);
                    contentStream.newLineAtOffset(100, yPosition);
                    for (String line : content) {
                        if (yPosition < 100) {
                            contentStream.endText();
                            contentStream.close();
                            page = new PDPage();
                            document.addPage(page);
                            contentStream = new PDPageContentStream(document, page);
                            contentStream.beginText();
                            contentStream.setFont(PDType1Font.HELVETICA, 12);
                            yPosition = 700;
                            contentStream.newLineAtOffset(100, yPosition);
                        }
                        contentStream.showText(line);
                        yPosition -= 15;
                        contentStream.newLineAtOffset(0, -15);
                    }
                    contentStream.endText();
                } finally {
                    if (contentStream != null) {
                        contentStream.close();
                    }
                }
                document.save(file);
                Reporter.log("Successfully wrote content to PDF file: ", LogLevel.INFO_GREEN, filePath);
            } catch (IOException e) {
                Reporter.log("Failed to write PDF file: ", LogLevel.ERROR, filePath);
                Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage() != null ? e.getMessage() : "Unknown error");
            }
        }
    }

    /**
     * Appends text content to an existing PDF file.
     * @param filePath the PDF file path.
     * @param content list of text lines to append.
     */
    public static void appendToPdf(String filePath, List<String> content) {
        File file = new File(filePath);
        synchronized (getFileLock(filePath)) {
            try (PDDocument document = PDDocument.load(file)) {
                PDPage page = document.getPage(document.getNumberOfPages() - 1);
                float yPosition = findLastYPosition(page);
                PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true);
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                if (yPosition < 100) {
                    page = new PDPage();
                    document.addPage(page);
                    yPosition = 700;
                }
                contentStream.newLineAtOffset(100, yPosition);
                for (String line : content) {
                    if (yPosition < 100) {
                        contentStream.endText();
                        contentStream.close();
                        page = new PDPage();
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        contentStream.beginText();
                        contentStream.setFont(PDType1Font.HELVETICA, 12);
                        yPosition = 700;
                        contentStream.newLineAtOffset(100, yPosition);
                    }
                    contentStream.showText(line);
                    yPosition -= 15;
                    contentStream.newLineAtOffset(0, -15);
                }
                contentStream.endText();
                contentStream.close();
                document.save(file);
                Reporter.log("Successfully appended content to PDF file: ", LogLevel.INFO_GREEN, filePath);
            } catch (IOException e) {
                Reporter.log("Failed to append to PDF file: ", LogLevel.ERROR, filePath);
                Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage() != null ? e.getMessage() : "Unknown error");
            }
        }
    }

    private static float findLastYPosition(PDPage page) {
        return 700;
    }

    /**
     * Merges multiple PDF files into one.
     * @param inputFilePaths list of input PDF file paths.
     * @param outputFilePath the output merged PDF file path.
     */
    public static void mergePdfs(List<String> inputFilePaths, String outputFilePath) {
        PDFMergerUtility merger = new PDFMergerUtility();
        merger.setDestinationFileName(outputFilePath);
        try {
            for (String path : inputFilePaths) {
                merger.addSource(new File(path));
            }
            merger.mergeDocuments(null);
            Reporter.log("Successfully merged PDF files into: ", LogLevel.INFO_GREEN, outputFilePath);
        } catch (IOException e) {
            Reporter.log("Failed to merge PDF files: ", LogLevel.ERROR, outputFilePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
    }

    /**
     * Extracts a single page from a PDF file.
     * @param inputFilePath the source PDF file path.
     * @param outputFilePath the output PDF file path.
     * @param pageIndex zero-based index of the page to extract.
     */
    public static void extractPdfPage(String inputFilePath, String outputFilePath, int pageIndex) {
        File file = new File(inputFilePath);

        try (PDDocument document = PDDocument.load(file)) {
            if (pageIndex < 0 || pageIndex >= document.getNumberOfPages()) {
                Reporter.log("Invalid page index: ", LogLevel.ERROR, String.valueOf(pageIndex));
                return;
            }

            try (PDDocument outputDocument = new PDDocument()) {
                PDPage sourcePage = document.getPage(pageIndex);
                outputDocument.importPage(sourcePage);
                
                outputDocument.save(outputFilePath);
                Reporter.log("Successfully extracted page " + pageIndex + " to: ", LogLevel.INFO_GREEN, outputFilePath);
            }
        } catch (IOException e) {
            Reporter.log("Failed to extract page from PDF file: ", LogLevel.ERROR, inputFilePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
    }

    /**
     * Compares two PDF files by their text content.
     * @param filePath1 the first PDF file path.
     * @param filePath2 the second PDF file path.
     * @return true if contents are equal; otherwise false.
     */
    public static boolean comparePdfFiles(String filePath1, String filePath2) {
        try (PDDocument doc1 = PDDocument.load(new File(filePath1));
             PDDocument doc2 = PDDocument.load(new File(filePath2))) {
            String text1 = new PDFTextStripper().getText(doc1);
            String text2 = new PDFTextStripper().getText(doc2);
            boolean isEqual = text1.equals(text2);
            Reporter.log("Comparison result for PDF files: ", LogLevel.INFO_GREEN, String.valueOf(isEqual));
            return isEqual;
        } catch (IOException e) {
            Reporter.log("Failed to compare PDF files: ", LogLevel.ERROR, filePath1 + " & " + filePath2);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage() != null ? e.getMessage() : "Unknown error");
            return false;
        }
    }

    /**
     * Splits a PDF into individual pages.
     * @param inputFilePath the source PDF file path.
     * @param outputDirectoryPath the directory where split pages will be stored.
     */
    public static void splitPdf(String inputFilePath, String outputDirectoryPath) {
        File inputFile = new File(inputFilePath);
        try (PDDocument document = PDDocument.load(inputFile)) {
            String baseName = inputFile.getName().replace(".pdf", "");
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                try (PDDocument singlePageDoc = new PDDocument()) {
                    singlePageDoc.addPage(document.getPage(i));
                    String outputPath = outputDirectoryPath + File.separator + baseName + "_page" + (i + 1) + ".pdf";
                    singlePageDoc.save(outputPath);
                }
            }
            Reporter.log("Successfully split PDF into individual pages: ", LogLevel.INFO_GREEN, inputFilePath);
        } catch (IOException e) {
            Reporter.log("Failed to split PDF file: ", LogLevel.ERROR, inputFilePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
    }

    /**
     * Adds an image to a PDF file.
     * @param inputFilePath the source PDF file path.
     * @param outputFilePath the target PDF file path.
     * @param imagePath the image file path.
     * @param x the x-coordinate.
     * @param y the y-coordinate.
     * @param width image width.
     * @param height image height.
     */
    public static void addImageToPdf(String inputFilePath, String outputFilePath, String imagePath, float x, float y, float width, float height) {
        try (PDDocument document = PDDocument.load(new File(inputFilePath))) {
            PDPage page = document.getPage(document.getNumberOfPages() - 1);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true)) {
                PDImageXObject pdImage = PDImageXObject.createFromFile(imagePath, document);
                contentStream.drawImage(pdImage, x, y, width, height);
            }
            document.save(outputFilePath);
            Reporter.log("Successfully added image to PDF: ", LogLevel.INFO_GREEN, outputFilePath);
        } catch (IOException e) {
            Reporter.log("Failed to add image to PDF: ", LogLevel.ERROR, outputFilePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
    }

    /**
     * Encrypts a PDF file with owner and user passwords.
     * @param inputFilePath the source PDF file path.
     * @param outputFilePath the output encrypted PDF file path.
     * @param ownerPassword the owner password.
     * @param userPassword the user password.
     */
    public static void encryptPdf(String inputFilePath, String outputFilePath, String ownerPassword, String userPassword) {
        try (PDDocument document = PDDocument.load(new File(inputFilePath))) {
            StandardProtectionPolicy protectionPolicy = new StandardProtectionPolicy(
                    ownerPassword, userPassword, AccessPermission.getOwnerAccessPermission()
            );
            protectionPolicy.setEncryptionKeyLength(128);
            document.protect(protectionPolicy);
            document.save(outputFilePath);
            Reporter.log("Successfully encrypted PDF: ", LogLevel.INFO_GREEN, outputFilePath);
        } catch (IOException e) {
            Reporter.log("Failed to encrypt PDF: ", LogLevel.ERROR, outputFilePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
    }

    /**
     * Adds a watermark text to a PDF file.
     * @param inputFilePath the source PDF file path.
     * @param outputFilePath the output PDF file path.
     * @param watermarkText the watermark text to add.
     */
    public static void addWatermark(String inputFilePath, String outputFilePath, String watermarkText) {
        try (PDDocument document = PDDocument.load(new File(inputFilePath))) {
            for (PDPage page : document.getPages()) {
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true)) {
                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 48);
                    contentStream.setNonStrokingColor(200, 200, 200);
                    contentStream.beginText();
                    contentStream.newLineAtOffset(100, 300);
                    contentStream.showText(watermarkText);
                    contentStream.endText();
                }
            }
            document.save(outputFilePath);
            Reporter.log("Successfully added watermark to PDF: ", LogLevel.INFO_GREEN, outputFilePath);
        } catch (IOException e) {
            Reporter.log("Failed to add watermark: ", LogLevel.ERROR, outputFilePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
    }

    /**
     * Updates the metadata (title and author) of a PDF file.
     * @param inputFilePath the source PDF file path.
     * @param outputFilePath the output PDF file path.
     * @param title the new title.
     * @param author the new author.
     */
    public static void updateMetadata(String inputFilePath, String outputFilePath, String title, String author) {
        try (PDDocument document = PDDocument.load(new File(inputFilePath))) {
            PDDocumentInformation info = document.getDocumentInformation();
            info.setTitle(title);
            info.setAuthor(author);
            document.save(outputFilePath);
            Reporter.log("Successfully updated PDF metadata: ", LogLevel.INFO_GREEN, outputFilePath);
        } catch (IOException e) {
            Reporter.log("Failed to update metadata: ", LogLevel.ERROR, outputFilePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
    }

    /**
     * Checks whether a PDF file is encrypted.
     * @param filePath the PDF file path.
     * @param password the password to attempt opening the file.
     * @return true if the PDF is encrypted; otherwise false.
     */
    public static boolean isEncrypted(String filePath, String password) {
        try (PDDocument document = PDDocument.load(new File(filePath), password)) {
            boolean isEncrypted = document.isEncrypted();
            Reporter.log("Successfully checked encryption status: ", LogLevel.INFO_GREEN, filePath);
            return isEncrypted;
        } catch (IOException e) {
            Reporter.log("Failed to check encryption status: ", LogLevel.ERROR, filePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage() != null ? e.getMessage() : "Unknown error");
            return false;
        }
    }

    /**
     * Removes pages from a PDF file within the specified range.
     * @param inputFilePath the source PDF file path.
     * @param outputFilePath the output PDF file path.
     * @param startPage starting page index (inclusive).
     * @param endPage ending page index (inclusive).
     */
    public static void removePages(String inputFilePath, String outputFilePath, int startPage, int endPage) {
        try (PDDocument document = PDDocument.load(new File(inputFilePath))) {
            if (startPage < 0 || endPage >= document.getNumberOfPages() || startPage > endPage) {
                Reporter.log("Invalid page range: ", LogLevel.ERROR, 
                           String.format("start=%d, end=%d, total pages=%d", 
                           startPage, endPage, document.getNumberOfPages()));
                return;
            }

            try (PDDocument newDocument = new PDDocument()) {
                for (int i = 0; i < document.getNumberOfPages(); i++) {
                    if (i < startPage || i > endPage) {
                        newDocument.addPage(document.getPage(i));
                    }
                }
                newDocument.save(outputFilePath);
            }
            Reporter.log("Successfully removed pages from PDF: ", LogLevel.INFO_GREEN, outputFilePath);
        } catch (IOException e) {
            Reporter.log("Failed to remove pages: ", LogLevel.ERROR, outputFilePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
    }

    /**
     * Rotates a specific page in a PDF file.
     * @param inputFilePath the source PDF file path.
     * @param outputFilePath the output PDF file path.
     * @param pageIndex index of the page to rotate.
     * @param degrees rotation in degrees.
     */
    public static void rotatePage(String inputFilePath, String outputFilePath, int pageIndex, int degrees) {
        try (PDDocument document = PDDocument.load(new File(inputFilePath))) {
            PDPage page = document.getPage(pageIndex);
            page.setRotation(degrees);
            document.save(outputFilePath);
            Reporter.log("Successfully rotated page in PDF: ", LogLevel.INFO_GREEN, outputFilePath);
        } catch (IOException e) {
            Reporter.log("Failed to rotate page: ", LogLevel.ERROR, outputFilePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
    }

    /**
     * Retrieves the total number of pages in a PDF file.
     * @param filePath the PDF file path.
     * @return the page count, or -1 if retrieval fails.
     */
    public static int getPageCount(String filePath) {
        try (PDDocument document = PDDocument.load(new File(filePath))) {
            return document.getNumberOfPages();
        } catch (IOException e) {
            Reporter.log("Failed to get page count: ", LogLevel.ERROR, filePath);
            return -1;
        }
    }

    /**
     * Adds a specified number of blank pages to a PDF file.
     * @param filePath the PDF file path.
     * @param numberOfPages number of blank pages to add.
     */
    public static void addBlankPages(String filePath, int numberOfPages) {
        try (PDDocument document = PDDocument.load(new File(filePath))) {
            for (int i = 0; i < numberOfPages; i++) {
                document.addPage(new PDPage());
            }
            document.save(filePath);
            Reporter.log("Successfully added blank pages: ", LogLevel.INFO_GREEN, filePath);
        } catch (IOException e) {
            Reporter.log("Failed to add blank pages: ", LogLevel.ERROR, filePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
    }

    /**
     * Retrieves the dimensions of a particular page in the PDF.
     * @param filePath the PDF file path.
     * @param pageIndex index of the page.
     * @return a PDRectangle representing the page dimensions, or null if unsuccessful.
     */
    public static PDRectangle getPageDimensions(String filePath, int pageIndex) {
        try (PDDocument document = PDDocument.load(new File(filePath))) {
            if (pageIndex >= 0 && pageIndex < document.getNumberOfPages()) {
                return document.getPage(pageIndex).getMediaBox();
            }
            return null;
        } catch (IOException e) {
            Reporter.log("Failed to get page dimensions: ", LogLevel.ERROR, filePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage() != null ? e.getMessage() : "Unknown error");
            return null;
        }
    }

    /**
     * Flattens any form fields in the PDF file.
     * @param inputPath the source PDF file path.
     * @param outputPath the output PDF file path.
     */
    public static void flattenFormFields(String inputPath, String outputPath) {
        try (PDDocument document = PDDocument.load(new File(inputPath))) {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            if (acroForm != null) {
                acroForm.flatten();
            }
            document.save(outputPath);
            Reporter.log("Successfully flattened form fields: ", LogLevel.INFO_GREEN, outputPath);
        } catch (IOException e) {
            Reporter.log("Failed to flatten form fields: ", LogLevel.ERROR, outputPath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
    }

    /**
     * Compresses a PDF file.
     * @param inputPath the source PDF file path.
     * @param outputPath the output PDF file path.
     */
    public static void compressPDF(String inputPath, String outputPath) {
        try (PDDocument document = PDDocument.load(new File(inputPath))) {
            document.getDocumentCatalog().setPageMode(PageMode.USE_NONE);
            document.getDocumentCatalog().setPageLayout(PageLayout.SINGLE_PAGE);
            document.save(outputPath);
            Reporter.log("Successfully compressed PDF: ", LogLevel.INFO_GREEN, outputPath);
        } catch (IOException e) {
            Reporter.log("Failed to compress PDF: ", LogLevel.ERROR, outputPath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
    }
}