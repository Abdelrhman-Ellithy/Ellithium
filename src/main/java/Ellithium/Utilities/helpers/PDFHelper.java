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

public class PDFHelper {

    public static String readPdf(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            Reporter.log("File does not exist: ", LogLevel.ERROR, filePath);
            return "";
        }

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

    public static void writePdf(String filePath, List<String> content) {
        File file = new File(filePath);
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

    public static void appendToPdf(String filePath, List<String> content) {
        File file = new File(filePath);
        try (PDDocument document = PDDocument.load(file)) {
            PDPage page = document.getPage(document.getNumberOfPages() - 1);
            
            // Get the last y-position of existing content
            float yPosition = findLastYPosition(page);
            
            PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true);
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA, 12);
            
            // Start from the calculated position or create new page if needed
            if (yPosition < 100) { // minimum space needed
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

    private static float findLastYPosition(PDPage page) {
        // Default starting position if we can't determine last position
        return 700;  // You might want to implement more sophisticated position tracking
    }

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

    public static void extractPdfPage(String inputFilePath, String outputFilePath, int pageIndex) {
        File file = new File(inputFilePath);

        try (PDDocument document = PDDocument.load(file)) {
            if (pageIndex < 0 || pageIndex >= document.getNumberOfPages()) {
                Reporter.log("Invalid page index: ", LogLevel.ERROR, String.valueOf(pageIndex));
                return;
            }

            try (PDDocument outputDocument = new PDDocument()) {
                PDPage sourcePage = document.getPage(pageIndex);
                // Import the page directly to the new document
                outputDocument.importPage(sourcePage);
                
                outputDocument.save(outputFilePath);
                Reporter.log("Successfully extracted page " + pageIndex + " to: ", LogLevel.INFO_GREEN, outputFilePath);
            }
        } catch (IOException e) {
            Reporter.log("Failed to extract page from PDF file: ", LogLevel.ERROR, inputFilePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
    }

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

    public static void removePages(String inputFilePath, String outputFilePath, int startPage, int endPage) {
        try (PDDocument document = PDDocument.load(new File(inputFilePath))) {
            if (startPage < 0 || endPage >= document.getNumberOfPages() || startPage > endPage) {
                Reporter.log("Invalid page range: ", LogLevel.ERROR, 
                           String.format("start=%d, end=%d, total pages=%d", 
                           startPage, endPage, document.getNumberOfPages()));
                return;
            }

            // Create a new document and copy pages except the ones to remove
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

    public static int getPageCount(String filePath) {
        try (PDDocument document = PDDocument.load(new File(filePath))) {
            return document.getNumberOfPages();
        } catch (IOException e) {
            Reporter.log("Failed to get page count: ", LogLevel.ERROR, filePath);
            return -1;
        }
    }

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