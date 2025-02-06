package Ellithium.Utilities.helpers;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class PDFHelper {

    // Method to read text from a PDF file
    public static String readPdf(String filePath) {
        File file = new File(filePath );
        String textContent = "";

        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            textContent = stripper.getText(document);
            Reporter.log("Successfully read PDF file: ", LogLevel.INFO_GREEN,filePath);
        } catch (IOException e) {
            Reporter.log("Failed to read PDF file: ",LogLevel.ERROR,filePath);
            Reporter.log("Root Cause: ",LogLevel.ERROR,e.getCause().toString());
        }
        return textContent;
    }

    // Method to write a list of strings into a PDF file
    public static void writePdf(String filePath, List<String> content) {
        File file = new File(filePath );
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);  // Set font
                contentStream.newLineAtOffset(100, 700);

                for (String line : content) {
                    contentStream.showText(line);
                    contentStream.newLineAtOffset(0, -15);  // Move to next line
                }

                contentStream.endText();
            }
            document.save(file);
            Reporter.log("Successfully wrote content to PDF file: ", LogLevel.INFO_GREEN,filePath);
        } catch (IOException e) {
            Reporter.log("Failed to write PDF file: ",LogLevel.ERROR,filePath);
            Reporter.log("Root Cause: ",LogLevel.ERROR,e.getCause().toString());
        }
    }

    // Method to append text to an existing PDF file
    public static void appendToPdf(String filePath, List<String> content) {
        File file = new File(filePath );
        try (PDDocument document = PDDocument.load(file)) {
            PDPage page = document.getPage(document.getNumberOfPages() - 1);  // Get last page
            PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true);
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA, 12);  // Set font
            contentStream.newLineAtOffset(100, 700);

            for (String line : content) {
                contentStream.showText(line);
                contentStream.newLineAtOffset(0, -15);
            }
            contentStream.endText();
            contentStream.close();
            document.save(file);
            Reporter.log("Successfully appended content to PDF file: ", LogLevel.INFO_GREEN,filePath);
        } catch (IOException e) {
            Reporter.log("Failed to append to PDF file: ",LogLevel.ERROR,filePath);
            Reporter.log("Root Cause: ",LogLevel.ERROR,e.getCause().toString());
        }
    }
    // Method to merge multiple PDF files
    public static void mergePdfs(List<String> inputFilePaths, String outputFilePath) {
        PDFMergerUtility merger = new PDFMergerUtility();
        merger.setDestinationFileName(outputFilePath );
        try {
            for (String path : inputFilePaths) {
                merger.addSource(new File(path ));
            }
            merger.mergeDocuments(null);
            Reporter.log("Successfully merged PDF files into: ", LogLevel.INFO_GREEN, outputFilePath);
        } catch (IOException e) {
            Reporter.log("Failed to merge PDF files: ", LogLevel.ERROR, outputFilePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage());
        }
    }
    // Method to extract a specific page from a PDF
    public static void extractPdfPage(String inputFilePath, String outputFilePath, int pageIndex) {
        File file = new File(inputFilePath );

        try (PDDocument document = PDDocument.load(file);
             PDDocument outputDocument = new PDDocument()) {
            PDPage page = document.getPage(pageIndex);
            outputDocument.addPage(page);
            outputDocument.save(outputFilePath );
            Reporter.log("Successfully extracted page " + pageIndex + " to: ", LogLevel.INFO_GREEN, outputFilePath);
        } catch (IOException e) {
            Reporter.log("Failed to extract page from PDF file: ", LogLevel.ERROR, inputFilePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage());
        }
    }
    public static boolean comparePdfFiles(String filePath1, String filePath2) {
        try (PDDocument doc1 = PDDocument.load(new File(filePath1 ));
             PDDocument doc2 = PDDocument.load(new File(filePath2 ))) {
            String text1 = new PDFTextStripper().getText(doc1);
            String text2 = new PDFTextStripper().getText(doc2);
            boolean isEqual = text1.equals(text2);
            Reporter.log("Comparison result for PDF files: ", LogLevel.INFO_GREEN, String.valueOf(isEqual));
            return isEqual;
        } catch (IOException e) {
            Reporter.log("Failed to compare PDF files: ", LogLevel.ERROR, filePath1 + " & " + filePath2);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage());
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
                    String outputPath = outputDirectoryPath + File.separator + baseName + "_page" + (i+1) + ".pdf";
                    singlePageDoc.save(outputPath);
                }
            }
            Reporter.log("Successfully split PDF into individual pages: ", LogLevel.INFO_GREEN, inputFilePath);
        } catch (IOException e) {
            Reporter.log("Failed to split PDF file: ", LogLevel.ERROR, inputFilePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage());
        }
    }

    // Method to add image to PDF
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
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage());
        }
    }

    // Method to encrypt PDF with password
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
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage());
        }
    }

    // Method to add watermark text
    public static void addWatermark(String inputFilePath, String outputFilePath, String watermarkText) {
        try (PDDocument document = PDDocument.load(new File(inputFilePath))) {
            for (PDPage page : document.getPages()) {
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true)) {
                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 48);
                    contentStream.setNonStrokingColor(200, 200, 200);  // Light gray
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
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage());
        }
    }

    // Method to update PDF metadata
    public static void updateMetadata(String inputFilePath, String outputFilePath, String title, String author) {
        try (PDDocument document = PDDocument.load(new File(inputFilePath))) {
            PDDocumentInformation info = document.getDocumentInformation();
            info.setTitle(title);
            info.setAuthor(author);
            document.save(outputFilePath);
            Reporter.log("Successfully updated PDF metadata: ", LogLevel.INFO_GREEN, outputFilePath);
        } catch (IOException e) {
            Reporter.log("Failed to update metadata: ", LogLevel.ERROR, outputFilePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage());
        }
    }

    // Method to check if PDF is encrypted
    public static boolean isEncrypted(String filePath) {
        try (PDDocument document = PDDocument.load(new File(filePath))) {
            return document.isEncrypted();
        } catch (IOException e) {
            Reporter.log("Failed to check encryption status: ", LogLevel.ERROR, filePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage());
            return false;
        }
    }

    // Method to remove pages from PDF
    public static void removePages(String inputFilePath, String outputFilePath, int startPage, int endPage) {
        try (PDDocument document = PDDocument.load(new File(inputFilePath))) {
            for (int i = endPage; i >= startPage; i--) {
                document.removePage(i);
            }
            document.save(outputFilePath);
            Reporter.log("Successfully removed pages from PDF: ", LogLevel.INFO_GREEN, outputFilePath);
        } catch (IOException e) {
            Reporter.log("Failed to remove pages: ", LogLevel.ERROR, outputFilePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage());
        }
    }

    // Method to rotate PDF page
    public static void rotatePage(String inputFilePath, String outputFilePath, int pageIndex, int degrees) {
        try (PDDocument document = PDDocument.load(new File(inputFilePath))) {
            PDPage page = document.getPage(pageIndex);
            page.setRotation(degrees);
            document.save(outputFilePath);
            Reporter.log("Successfully rotated page in PDF: ", LogLevel.INFO_GREEN, outputFilePath);
        } catch (IOException e) {
            Reporter.log("Failed to rotate page: ", LogLevel.ERROR, outputFilePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage());
        }
    }
}