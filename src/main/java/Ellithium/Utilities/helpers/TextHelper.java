package Ellithium.Utilities.helpers;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
public class TextHelper {
    public static List<String> readTextFile(String filePath) {
        List<String> lines = new ArrayList<>();
        File textFile = new File(filePath);

        if (!textFile.exists()) {
            Reporter.log("Text file does not exist: ", LogLevel.ERROR, filePath);
            return lines;
        }
        Reporter.log("Attempting to read text file: ", LogLevel.INFO_BLUE, filePath);

        try (BufferedReader reader = new BufferedReader(new FileReader(textFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            Reporter.log("Successfully read text file: ", LogLevel.INFO_GREEN, filePath);

        } catch (FileNotFoundException e) {
            Reporter.log("Failed to find text file: ", LogLevel.ERROR, filePath);
                Reporter.log("Root Cause: ", LogLevel.ERROR, e.getCause().toString());
        } catch (IOException e) {
            Reporter.log("Failed to read text file: ", LogLevel.ERROR, filePath);
                Reporter.log("Root Cause: ", LogLevel.ERROR, e.getCause().toString());
        }
        return lines;
    }
    public static void writeTextFile(String filePath, List<String> lines) {
        Reporter.log("Attempting to write text file: ", LogLevel.INFO_BLUE, filePath);
        File textFile = new File(filePath );

        if (!textFile.exists()) {
            try {
                if (textFile.createNewFile()) {
                    Reporter.log("Created new text file: ", LogLevel.INFO_GREEN, filePath);
                }
            } catch (IOException e) {
                Reporter.log("Failed to create text file: ", LogLevel.ERROR, filePath);
                    Reporter.log("Root Cause: ", LogLevel.ERROR, e.getCause().toString());
            }
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(textFile))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
            Reporter.log("Successfully wrote to text file: ", LogLevel.INFO_GREEN, filePath);

        } catch (IOException e) {
            Reporter.log("Failed to write to text file: ", LogLevel.ERROR, filePath);
                Reporter.log("Root Cause: ", LogLevel.ERROR, e.getCause().toString());
        }
    }

    // Method to append a single line to a text file
    public static void appendLineToFile(String filePath, String line) {
        Reporter.log("Attempting to append line to text file: ", LogLevel.INFO_BLUE, filePath);
        File textFile = new File(filePath );

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(textFile, true))) {
            writer.write(line);
            writer.newLine();
            Reporter.log("Successfully appended line to text file: ", LogLevel.INFO_GREEN, filePath);

        } catch (IOException e) {
            Reporter.log("Failed to append to text file: ", LogLevel.ERROR, filePath);
                Reporter.log("Root Cause: ", LogLevel.ERROR, e.getCause().toString());
        }
    }

    public static String findLineWithKeyword(String filePath, String keyword) {
        File textFile = new File(filePath );

        if (!textFile.exists()) {
            Reporter.log("Text file does not exist: ", LogLevel.ERROR, filePath);
            return null;
        }

        Reporter.log("Searching for keyword in text file: ", LogLevel.INFO_BLUE, filePath);

        try (BufferedReader reader = new BufferedReader(new FileReader(textFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(keyword)) {
                    Reporter.log("Found line with keyword: " + keyword, LogLevel.INFO_GREEN, filePath);
                    return line;
                }
            }
        } catch (IOException e) {
            Reporter.log("Failed to search text file: ", LogLevel.ERROR, filePath);
                Reporter.log("Root Cause: ", LogLevel.ERROR, e.getCause().toString());
        }
        Reporter.log("No line with keyword found in text file: ", LogLevel.INFO_YELLOW, filePath);
        return null;
    }
    public static void deleteLine(String filePath, String lineToDelete) {
        File textFile = new File(filePath );
        List<String> updatedLines = new ArrayList<>();
        Reporter.log("Attempting to delete line from text file: ", LogLevel.INFO_BLUE, filePath);
        try (BufferedReader reader = new BufferedReader(new FileReader(textFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.equals(lineToDelete)) {
                    updatedLines.add(line);
                }
            }
            writeTextFile(filePath, updatedLines);
            Reporter.log("Successfully deleted line from text file: ", LogLevel.INFO_GREEN, filePath);

        } catch (IOException e) {
            Reporter.log("Failed to delete line from text file: ", LogLevel.ERROR, filePath);
                Reporter.log("Root Cause: ", LogLevel.ERROR, e.getCause().toString());
        }
    }
    // Method to read the entire file as a single String
    public static String readFileAsString(String filePath) {
        StringBuilder content = new StringBuilder();
        File textFile = new File(filePath );

        if (!textFile.exists()) {
            Reporter.log("Text file does not exist: ", LogLevel.ERROR, filePath);
            return null;
        }

        Reporter.log("Attempting to read entire text file as a String: ", LogLevel.INFO_BLUE, filePath);

        try (BufferedReader reader = new BufferedReader(new FileReader(textFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }
            Reporter.log("Successfully read text file as a String: ", LogLevel.INFO_GREEN, filePath);
        } catch (IOException e) {
            Reporter.log("Failed to read text file as a String: ", LogLevel.ERROR, filePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getCause().toString());
        }

        return content.toString();
    }
    // Method to write a String to a text file
    public static void writeStringToFile(String filePath, String content) {
        Reporter.log("Attempting to write String to text file: ", LogLevel.INFO_BLUE, filePath);
        File textFile = new File(filePath );

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(textFile))) {
            writer.write(content);
            Reporter.log("Successfully wrote String to text file: ", LogLevel.INFO_GREEN, filePath);
        } catch (IOException e) {
            Reporter.log("Failed to write String to text file: ", LogLevel.ERROR, filePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getCause().toString());
        }
    }
    // Method to delete all lines containing a specific keyword
    public static void deleteLinesContainingKeyword(String filePath, String keyword) {
        File textFile = new File(filePath );
        List<String> updatedLines = new ArrayList<>();

        Reporter.log("Attempting to delete lines containing keyword: ", LogLevel.INFO_BLUE, filePath);

        try (BufferedReader reader = new BufferedReader(new FileReader(textFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.contains(keyword)) {
                    updatedLines.add(line);
                }
            }
            writeTextFile(filePath, updatedLines);
            Reporter.log("Successfully deleted lines containing keyword in text file: ", LogLevel.INFO_GREEN, filePath);
        } catch (IOException e) {
            Reporter.log("Failed to delete lines containing keyword in text file: ", LogLevel.ERROR, filePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getCause().toString());
        }
    }
    // Method to check if any line in the file contains a specific keyword
    public static boolean containsKeyword(String filePath, String keyword) {
        File textFile = new File(filePath );

        if (!textFile.exists()) {
            Reporter.log("Text file does not exist: ", LogLevel.ERROR, filePath);
            return false;
        }

        Reporter.log("Checking if text file contains keyword: ", LogLevel.INFO_BLUE, filePath);

        try (BufferedReader reader = new BufferedReader(new FileReader(textFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(keyword)) {
                    Reporter.log("Keyword found in text file: " + keyword, LogLevel.INFO_GREEN, filePath);
                    return true;
                }
            }
        } catch (IOException e) {
            Reporter.log("Failed to check keyword in text file: ", LogLevel.ERROR, filePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getCause().toString());
        }
        Reporter.log("Keyword not found in text file: " + keyword, LogLevel.INFO_YELLOW, filePath);
        return false;
    }
    // Method to replace a line in a text file
    public static void replaceLineInFile(String filePath, String targetLine, String newLine) {
        File textFile = new File(filePath );
        List<String> updatedLines = new ArrayList<>();

        Reporter.log("Attempting to replace a line in text file: ", LogLevel.INFO_BLUE, filePath);

        try (BufferedReader reader = new BufferedReader(new FileReader(textFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals(targetLine)) {
                    updatedLines.add(newLine);
                } else {
                    updatedLines.add(line);
                }
            }
            writeTextFile(filePath, updatedLines);
            Reporter.log("Successfully replaced line in text file: ", LogLevel.INFO_GREEN, filePath);
        } catch (IOException e) {
            Reporter.log("Failed to replace line in text file: ", LogLevel.ERROR, filePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getCause().toString());
        }
    }

}
