package Ellithium.Utilities.helpers;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.apache.commons.lang3.SystemUtils;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.awt.Desktop;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class to execute OS commands and interact with the system.
 */
public class CommandExecutor {

    /**
     * Executes a command synchronously.
     * @param command the command to execute.
     */
    public static void executeCommand(String command) {
        Reporter.log("Attempting to execute command: " + command, LogLevel.INFO_GREEN);
        try {
            ProcessBuilder builder = getProcessBuilder(sanitizeCommand(command));
            builder.redirectErrorStream(true);
            Process process = builder.start();
            String output = captureProcessOutput(process);
            int exitCode = process.waitFor();
            logCommandResult(command, exitCode, output);
        } catch (IOException | InterruptedException e) {
            Reporter.log("Command execution error: " + e.getMessage(), LogLevel.ERROR);
        }
    }
    
    private static String captureProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }
        return output.toString();
    }
    
    private static void logCommandResult(String command, int exitCode, String output) {
        if (exitCode == 0) {
            Reporter.log("Command executed successfully. Exit code: " + exitCode, LogLevel.INFO_GREEN);
        } else {
            Reporter.log("Command failed. Exit code: " + exitCode, LogLevel.ERROR);
            Reporter.log("Output: " + output, LogLevel.DEBUG);
        }
    }
    /**
     * Executes a command asynchronously and returns the running process.
     * @param command the command to execute.
     * @return the Process object, or null if execution fails.
     */
    public static Process executeCommandNonBlocking(String command) {
        Reporter.log("Attempting to execute command in non-blocking mode: ", LogLevel.INFO_GREEN, command);
        try {
            ProcessBuilder builder = getProcessBuilder(sanitizeCommand(command));
            builder.redirectErrorStream(true);
            Process process = builder.start();
            Reporter.log("Non-blocking command executed: ", LogLevel.INFO_GREEN, command);
            return process;  // Return the running process
        } catch (IOException e) {
            Reporter.log("Failed to execute non-blocking command: ", LogLevel.ERROR, command);
            return null;
        }
    }

    /**
     * Opens a file using the default application.
     * @param filePath the file path.
     */
    public static void openFile(String filePath) {
        try {
            File file = new File(filePath);
            if (Desktop.isDesktopSupported() && file.exists()) {
                Desktop.getDesktop().open(file);
                Reporter.log("Successfully opened file: ", LogLevel.INFO_GREEN, filePath);
            } else {
                executeOSSpecificOpen(filePath);
            }
        } catch (IOException e) {
            Reporter.log("Failed to open file: ", LogLevel.ERROR, filePath);
            Reporter.log("Root Cause: ", LogLevel.ERROR, e.getMessage());
        }
    }

    /**
     * Executes an OS-specific open command.
     * @param filePath the file to open.
     * @throws IOException if command execution fails.
     */
    private static void executeOSSpecificOpen(String filePath) throws IOException {
        String[] command;
        if (SystemUtils.IS_OS_WINDOWS) {
            command = new String[]{"cmd", "/c", "start", filePath};
        } else if (SystemUtils.IS_OS_MAC) {
            command = new String[]{"open", filePath};
        } else if (SystemUtils.IS_OS_LINUX) {
            command = new String[]{"xdg-open", filePath};
        } else {
            throw new UnsupportedOperationException("Unsupported operating system");
        }
        new ProcessBuilder(command).start();
    }

    /**
     * Retrieves basic system information.
     * @return String containing system info.
     */
    public static String getSystemInfo() {
        StringBuilder info = new StringBuilder();
        info.append("OS: ").append(System.getProperty("os.name")).append("\n");
        info.append("OS Version: ").append(System.getProperty("os.version")).append("\n");
        info.append("Architecture: ").append(System.getProperty("os.arch")).append("\n");
        info.append("Java Version: ").append(System.getProperty("java.version")).append("\n");
        info.append("User Name: ").append(System.getProperty("user.name")).append("\n");
        info.append("User Home: ").append(System.getProperty("user.home")).append("\n");
        return info.toString();
    }

    /**
     * Lists the running processes.
     * @return List of process descriptions.
     */
    public static List<String> listProcesses() {
        List<String> processes = new ArrayList<>();
        try {
            String[] command;
            if (SystemUtils.IS_OS_WINDOWS) {
                command = new String[]{"tasklist"};
            } else {
                command = new String[]{"ps", "aux"};
            }
            Process process = new ProcessBuilder(command).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    processes.add(line);
                }
            }
        } catch (IOException e) {
            Reporter.log("Failed to list processes: ", LogLevel.ERROR, e.getMessage());
        }
        return processes;
    }

    /**
     * Kills a process given its process id.
     * @param processId the process identifier.
     */
    public static void killProcess(String processId) {
        try {
            String[] command;
            if (SystemUtils.IS_OS_WINDOWS) {
                command = new String[]{"taskkill", "/F", "/PID", processId};
            } else {
                command = new String[]{"kill", "-9", processId};
            }
            Process process = new ProcessBuilder(command).start();
            process.waitFor();
            Reporter.log("Successfully killed process: ", LogLevel.INFO_GREEN, processId);
        } catch (IOException | InterruptedException e) {
            Reporter.log("Failed to kill process: ", LogLevel.ERROR, processId);
        }
    }

    /**
     * Creates a directory at the specified path.
     * @param path the directory path.
     */
    public static void createDirectory(String path) {
        try {
            Files.createDirectories(Paths.get(path));
            Reporter.log("Successfully created directory: ", LogLevel.INFO_GREEN, path);
        } catch (IOException e) {
            Reporter.log("Failed to create directory: ", LogLevel.ERROR, path);
        }
    }

    /**
     * Deletes a directory at the specified path.
     * @param path the directory path.
     */
    public static void deleteDirectory(String path) {
        try {
            Files.walk(Paths.get(path))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            Reporter.log("Successfully deleted directory: ", LogLevel.INFO_GREEN, path);
        } catch (IOException e) {
            Reporter.log("Failed to delete directory: ", LogLevel.ERROR, path);
        }
    }

    /**
     * Copies a file from source to target.
     * @param sourcePath the source file path.
     * @param targetPath the target file path.
     */
    public static void copyFile(String sourcePath, String targetPath) {
        try {
            Files.copy(Paths.get(sourcePath), Paths.get(targetPath), StandardCopyOption.REPLACE_EXISTING);
            Reporter.log("Successfully copied file: ", LogLevel.INFO_GREEN, sourcePath + " to " + targetPath);
        } catch (IOException e) {
            Reporter.log("Failed to copy file: ", LogLevel.ERROR, sourcePath);
        }
    }

    /**
     * Executes a command and returns its output.
     * @param command the command to execute.
     * @return output from the command execution.
     */
    public static String executeCommandWithOutput(String command) {
        try {
            ProcessBuilder builder = getProcessBuilder(sanitizeCommand(command));
            builder.redirectErrorStream(true);
            Process process = builder.start();
            String output = captureProcessOutput(process);
            process.waitFor();
            return output;
        } catch (IOException | InterruptedException e) {
            Reporter.log("Failed to execute command with output: ", LogLevel.ERROR, command);
            return null;
        }
    }
    
    public static void executeScript(String scriptPath) {
        try {
            String[] command = buildScriptCommand(scriptPath);
            Process process = new ProcessBuilder(command).start();
            process.waitFor();
            Reporter.log("Successfully executed script: ", LogLevel.INFO_GREEN, scriptPath);
        } catch (IOException | InterruptedException e) {
            Reporter.log("Failed to execute script: ", LogLevel.ERROR, scriptPath);
        }
    }
    
    private static String[] buildScriptCommand(String scriptPath) {
        if (SystemUtils.IS_OS_WINDOWS) {
            if (scriptPath.endsWith(".bat") || scriptPath.endsWith(".cmd")) {
                return new String[]{"cmd", "/c", scriptPath};
            } else if (scriptPath.endsWith(".ps1")) {
                return new String[]{"powershell", "-File", scriptPath};
            } else {
                throw new IllegalArgumentException("Unsupported script type");
            }
        } else {
            return new String[]{"/bin/bash", scriptPath};
        }
    }

    /**
     * Retrieves the current environment variables.
     * @return Map of environment variables.
     */
    public static Map<String, String> getEnvironmentVariables() {
        return new HashMap<>(System.getenv());
    }

    /**
     * Sets or updates an environment variable.
     * @param key variable name.
     * @param value variable value.
     */
    public static void setEnvironmentVariable(String key, String value) {
        try {
            if (SystemUtils.IS_OS_WINDOWS) {
                executeCommand("setx " + key + " " + value);
            } else {
                executeCommand("export " + key + "=" + value);
            }
            Reporter.log("Successfully set environment variable: ", LogLevel.INFO_GREEN, key);
        } catch (Exception e) {
            Reporter.log("Failed to set environment variable: ", LogLevel.ERROR, key);
        }
    }

    /**
     * Checks if a process with the given name is running.
     * @param processName the process name.
     * @return true if running; otherwise false.
     */
    public static boolean isProcessRunning(String processName) {
        try {
            String command;
            if (SystemUtils.IS_OS_WINDOWS) {
                command = "tasklist /FI \"IMAGENAME eq " + processName + "\"";
            } else {
                command = "pgrep " + processName;
            }
            String output = executeCommandWithOutput(command);
            return output != null && output.contains(processName);
        } catch (Exception e) {
            Reporter.log("Failed to check process status: ", LogLevel.ERROR, processName);
            return false;
        }
    }

    /**
     * Returns a configured ProcessBuilder for command execution.
     * @param command the command arguments.
     * @return ProcessBuilder instance.
     */
    private static ProcessBuilder getProcessBuilder(String[] command) {
        List<String> commandList = new ArrayList<>();
        if (SystemUtils.IS_OS_WINDOWS) {
            commandList.add("cmd.exe");
            commandList.add("/c");
            Collections.addAll(commandList, command);
        } else {
            commandList.add("/bin/bash");
            commandList.add("-c");
            commandList.add(String.join(" ", command));
        }
        return new ProcessBuilder(commandList);
    }

    /**
     * Sanitizes a command string into an array of command arguments.
     * @param command the input command string.
     * @return an array of command components.
     */
    private static String[] sanitizeCommand(String command) {
        List<String> tokens = new ArrayList<>();
        Pattern pattern = Pattern.compile("\"([^\"]*)\"|(\\S+)");
        Matcher matcher = pattern.matcher(command);
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                tokens.add(matcher.group(1));
            } else {
                tokens.add(matcher.group(2));
            }
        }
        return tokens.toArray(new String[0]);
    }
}