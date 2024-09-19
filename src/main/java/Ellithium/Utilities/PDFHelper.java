package Ellithium.Utilities;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;  // Import for font
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class PDFHelper {

    // Method to read text from a PDF file
    public static String readPdf(String filePath) {
        File file = new File(filePath + ".pdf");
        String textContent = "";
        try (PDDocument document = PDDocument.load(file)) {  // Static method load
            PDFTextStripper stripper = new PDFTextStripper();
            textContent = stripper.getText(document);
            logsUtils.info(Colors.GREEN + "Successfully read PDF file: " + filePath + Colors.RESET);
        } catch (IOException e) {
            logsUtils.error(Colors.RED + "Failed to read PDF file: " + filePath + Colors.RESET);
            logsUtils.logException(e);
            throw new RuntimeException(e);
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
                contentStream.setFont(PDType1Font.HELVETICA, 12);  // Set font here
                contentStream.newLineAtOffset(100, 700);

                for (String line : content) {
                    contentStream.showText(line);
                    contentStream.newLineAtOffset(0, -15);  // Move to the next line
                }

                contentStream.endText();
            }

            document.save(file);
            logsUtils.info(Colors.GREEN + "Successfully wrote content to PDF file: " + filePath + Colors.RESET);
        } catch (IOException e) {
            logsUtils.error(Colors.RED + "Failed to write PDF file: " + filePath + Colors.RESET);
            logsUtils.logException(e);
            throw new RuntimeException(e);
        }
    }

    // Method to append text to an existing PDF file
    public static void appendToPdf(String filePath, List<String> content) {
        File file = new File(filePath + ".pdf");
        try (PDDocument document = PDDocument.load(file)) {  // Static method load
            PDPage page = document.getPage(document.getNumberOfPages() - 1);  // Get the last page
            PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true);

            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA, 12);  // Set font here
            contentStream.newLineAtOffset(100, 700);

            for (String line : content) {
                contentStream.showText(line);
                contentStream.newLineAtOffset(0, -15);
            }
            contentStream.endText();
            contentStream.close();

            document.save(file);
            logsUtils.info(Colors.GREEN + "Successfully appended content to PDF file: " + filePath + Colors.RESET);
        } catch (IOException e) {
            logsUtils.error(Colors.RED + "Failed to append to PDF file: " + filePath + Colors.RESET);
            logsUtils.logException(e);
            throw new RuntimeException(e);
        }
    }
}
