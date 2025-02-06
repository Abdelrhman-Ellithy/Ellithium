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
    public static int countLinesInFile(String filePath) {
        File textFile = new File(filePath);
        int count = 0;
        Reporter.log("Attempting to count lines in file: ", LogLevel.INFO_BLUE, filePath);

        if (!textFile.exists()) {
            Reporter.log("Text file does not exist: ", LogLevel.ERROR, filePath);
            return 0;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(textFile))) {
            while (reader.readLine() != null) {
                count++;
            }
            Reporter.log("Successfully counted " + count + " lines in file: ", LogLevel.INFO_GREEN, filePath);
            return count;
        } catch (IOException e) {
            Reporter.log("Failed to count lines in file: ", LogLevel.ERROR, filePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getCause().toString());
            return 0;
        }
    }

    public static boolean isEmptyFile(String filePath) {
        File textFile = new File(filePath);
        Reporter.log("Checking if file is empty: ", LogLevel.INFO_BLUE, filePath);

        if (!textFile.exists()) {
            Reporter.log("Text file does not exist: ", LogLevel.ERROR, filePath);
            return false;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(textFile))) {
            boolean isEmpty = reader.readLine() == null;
            String msg = isEmpty ? "File is empty" : "File is not empty";
            LogLevel level = isEmpty ? LogLevel.INFO_GREEN : LogLevel.INFO_YELLOW;
            Reporter.log(msg + ": ", level, filePath);
            return isEmpty;
        } catch (IOException e) {
            Reporter.log("Failed to check file emptiness: ", LogLevel.ERROR, filePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getCause().toString());
            return false;
        }
    }

    public static void replaceAllOccurrences(String filePath, String target, String replacement) {
        Reporter.log("Replacing all '" + target + "' with '" + replacement + "' in: ", LogLevel.INFO_BLUE, filePath);
        File textFile = new File(filePath);

        if (!textFile.exists()) {
            Reporter.log("Text file does not exist: ", LogLevel.ERROR, filePath);
            return;
        }

        try {
            String content = new String(java.nio.file.Files.readAllBytes(textFile.toPath()), "UTF-8");
            String updatedContent = content.replace(target, replacement);
            java.nio.file.Files.write(textFile.toPath(), updatedContent.getBytes("UTF-8"));
            Reporter.log("Successfully replaced all occurrences in: ", LogLevel.INFO_GREEN, filePath);
        } catch (IOException e) {
            Reporter.log("Failed to replace content in file: ", LogLevel.ERROR, filePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getCause().toString());
        }
    }

    public static List<String> getAllLinesWithKeyword(String filePath, String keyword) {
        List<String> matches = new ArrayList<>();
        File textFile = new File(filePath);
        Reporter.log("Searching for all lines containing '" + keyword + "' in: ", LogLevel.INFO_BLUE, filePath);

        if (!textFile.exists()) {
            Reporter.log("Text file does not exist: ", LogLevel.ERROR, filePath);
            return matches;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(textFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(keyword)) matches.add(line);
            }
            String resultMsg = matches.size() + " lines found with keyword";
            LogLevel level = matches.isEmpty() ? LogLevel.INFO_YELLOW : LogLevel.INFO_GREEN;
            Reporter.log(resultMsg + ": ", level, filePath);
            return matches;
        } catch (IOException e) {
            Reporter.log("Failed to search for keyword in file: ", LogLevel.ERROR, filePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getCause().toString());
            return matches;
        }
    }

    public static void copyTextFile(String sourcePath, String destPath) {
        Reporter.log("Copying from " + sourcePath + " to " + destPath, LogLevel.INFO_BLUE);
        File sourceFile = new File(sourcePath);
        File destFile = new File(destPath);

        if (!sourceFile.exists()) {
            Reporter.log("Source file does not exist: ", LogLevel.ERROR, sourcePath);
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(sourceFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(destFile))) {

            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
            Reporter.log("File copied successfully to: ", LogLevel.INFO_GREEN, destPath);
        } catch (IOException e) {
            Reporter.log("Failed to copy file to: ", LogLevel.ERROR, destPath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getCause().toString());
        }
    }

    public static boolean areFilesIdentical(String firstPath, String secondPath) {
        Reporter.log("Comparing files for identity: " + firstPath + " vs " + secondPath, LogLevel.INFO_BLUE);
        File firstFile = new File(firstPath);
        File secondFile = new File(secondPath);

        if (!firstFile.exists() || !secondFile.exists()) {
            Reporter.log("One or both files missing", LogLevel.ERROR);
            return false;
        }

        try (BufferedReader reader1 = new BufferedReader(new FileReader(firstFile));
             BufferedReader reader2 = new BufferedReader(new FileReader(secondFile))) {

            String line1, line2;
            while ((line1 = reader1.readLine()) != null && (line2 = reader2.readLine()) != null) {
                if (!line1.equals(line2)) {
                    Reporter.log("Files differ in content", LogLevel.INFO_YELLOW);
                    return false;
                }
            }

            // Check if one file has remaining lines
            if (reader1.readLine() != null || reader2.readLine() != null) {
                Reporter.log("Files have different lengths", LogLevel.INFO_YELLOW);
                return false;
            }

            Reporter.log("Files are identical", LogLevel.INFO_GREEN);
            return true;
        } catch (IOException e) {
            Reporter.log("Failed to compare files", LogLevel.ERROR);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getCause().toString());
            return false;
        }
    }

    public static List<Integer> getLineNumbersWithKeyword(String filePath, String keyword) {
        List<Integer> lineNumbers = new ArrayList<>();
        File textFile = new File(filePath);
        Reporter.log("Finding line numbers with '" + keyword + "' in: ", LogLevel.INFO_BLUE, filePath);

        if (!textFile.exists()) {
            Reporter.log("Text file does not exist: ", LogLevel.ERROR, filePath);
            return lineNumbers;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(textFile))) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.contains(keyword)) lineNumbers.add(lineNum);
            }
            String resultMsg = lineNumbers.size() + " line numbers found with keyword";
            LogLevel level = lineNumbers.isEmpty() ? LogLevel.INFO_YELLOW : LogLevel.INFO_GREEN;
            Reporter.log(resultMsg + ": ", level, filePath);
            return lineNumbers;
        } catch (IOException e) {
            Reporter.log("Failed to find line numbers in file: ", LogLevel.ERROR, filePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getCause().toString());
            return lineNumbers;
        }
    }
    public static void insertLineAt(String filePath, int lineNumber, String newLine) {
        Reporter.log("Attempting to insert line at position " + lineNumber + " in: ", LogLevel.INFO_BLUE, filePath);
        List<String> lines = readTextFile(filePath);

        try {
            if(lineNumber < 1) {
                lines.add(0, newLine);
            } else if(lineNumber > lines.size()) {
                lines.add(newLine);
            } else {
                lines.add(lineNumber - 1, newLine);
            }

            writeTextFile(filePath, lines);
            Reporter.log("Successfully inserted line at position " + lineNumber + " in: ", LogLevel.INFO_GREEN, filePath);
        } catch (Exception e) {
            Reporter.log("Failed to insert line in: ", LogLevel.ERROR, filePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getCause().toString());
        }
    }

    public static void appendAfterLine(String filePath, int lineNumber, String newLine) {
        Reporter.log("Attempting to append after line " + lineNumber + " in: ", LogLevel.INFO_BLUE, filePath);
        List<String> lines = readTextFile(filePath);

        try {
            if(lineNumber < 0) {
                lines.add(0, newLine);
            } else if(lineNumber >= lines.size()) {
                lines.add(newLine);
            } else {
                lines.add(lineNumber, newLine);
            }

            writeTextFile(filePath, lines);
            Reporter.log("Successfully appended after line " + lineNumber + " in: ", LogLevel.INFO_GREEN, filePath);
        } catch (Exception e) {
            Reporter.log("Failed to append line in: ", LogLevel.ERROR, filePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getCause().toString());
        }
    }

    public static List<String> readSpecificLines(String filePath, int startLine, int endLine) {
        Reporter.log("Attempting to read lines " + startLine + "-" + endLine + " from: ",
                LogLevel.INFO_BLUE, filePath);

        List<String> result = new ArrayList<>();
        List<String> allLines = readTextFile(filePath);
        int size = allLines.size();

        if(size == 0) {
            Reporter.log("File is empty: ", LogLevel.INFO_YELLOW, filePath);
            return result;
        }

        try {
            int start = Math.max(1, startLine);
            int end = Math.min(size, endLine);

            if(start > end) {
                Reporter.log("Invalid line range (start > end) in: ", LogLevel.INFO_YELLOW, filePath);
                return result;
            }

            result = allLines.subList(start - 1, end);
            Reporter.log("Successfully read lines " + start + "-" + end + " from: ",
                    LogLevel.INFO_GREEN, filePath);
        } catch (IndexOutOfBoundsException e) {
            Reporter.log("Invalid line range in: ", LogLevel.ERROR, filePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getCause().toString());
        }
        return result;
    }

    public static void writeAtLine(String filePath, int lineNumber, String content) {
        Reporter.log("Attempting to overwrite line " + lineNumber + " in: ", LogLevel.INFO_BLUE, filePath);
        List<String> lines = readTextFile(filePath);

        try {
            if(lineNumber < 1) {
                Reporter.log("Invalid line number (must be >=1): ", LogLevel.ERROR, filePath);
                return;
            }

            while(lines.size() < lineNumber) {
                lines.add("");
            }

            lines.set(lineNumber - 1, content);
            writeTextFile(filePath, lines);
            Reporter.log("Successfully wrote to line " + lineNumber + " in: ", LogLevel.INFO_GREEN, filePath);
        } catch (Exception e) {
            Reporter.log("Failed to write to line in: ", LogLevel.ERROR, filePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getCause().toString());
        }
    }
}
