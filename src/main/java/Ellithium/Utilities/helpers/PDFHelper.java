package Ellithium.Utilities.helpers;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class PDFHelper {

    // Method to read text from a PDF file
    public static String readPdf(String filePath) {
        File file = new File(filePath + ".pdf");
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
        File file = new File(filePath + ".pdf");
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
        File file = new File(filePath + ".pdf");
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
        merger.setDestinationFileName(outputFilePath + ".pdf");
        try {
            for (String path : inputFilePaths) {
                merger.addSource(new File(path + ".pdf"));
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
        File file = new File(inputFilePath + ".pdf");

        try (PDDocument document = PDDocument.load(file);
             PDDocument outputDocument = new PDDocument()) {
            PDPage page = document.getPage(pageIndex);
            outputDocument.addPage(page);
            outputDocument.save(outputFilePath + ".pdf");
            Reporter.log("Successfully extracted page " + pageIndex + " to: ", LogLevel.INFO_GREEN, outputFilePath);
        } catch (IOException e) {
            Reporter.log("Failed to extract page from PDF file: ", LogLevel.ERROR, inputFilePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage());
        }
    }
    public static boolean comparePdfFiles(String filePath1, String filePath2) {
        try (PDDocument doc1 = PDDocument.load(new File(filePath1 + ".pdf"));
             PDDocument doc2 = PDDocument.load(new File(filePath2 + ".pdf"))) {
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

}