package Ellithium.Utilities.helpers;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.apache.commons.lang3.SystemUtils;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.awt.Desktop;

public class CommandExecutor {
    public static void executeCommand(String command) {
        Reporter.log("Attempting to execute command: ", LogLevel.INFO_GREEN, command);
        try {
            ProcessBuilder builder = getProcessBuilder(sanitizeCommand(command));
            builder.redirectErrorStream(true);
            Process process = builder.start();
            int exitCode = process.waitFor();
            Reporter.log("Command executed successfully. Exit code: ", LogLevel.INFO_GREEN, String.valueOf(exitCode));
        } catch (IOException | InterruptedException e) {
            Reporter.log("Failed to execute command: ", LogLevel.ERROR, command);
        }
    }

    // Method to execute command in non-blocking manner and return the process object
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

    public static void createDirectory(String path) {
        try {
            Files.createDirectories(Paths.get(path));
            Reporter.log("Successfully created directory: ", LogLevel.INFO_GREEN, path);
        } catch (IOException e) {
            Reporter.log("Failed to create directory: ", LogLevel.ERROR, path);
        }
    }

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

    public static void copyFile(String sourcePath, String targetPath) {
        try {
            Files.copy(Paths.get(sourcePath), Paths.get(targetPath), StandardCopyOption.REPLACE_EXISTING);
            Reporter.log("Successfully copied file: ", LogLevel.INFO_GREEN, sourcePath + " to " + targetPath);
        } catch (IOException e) {
            Reporter.log("Failed to copy file: ", LogLevel.ERROR, sourcePath);
        }
    }

    public static String executeCommandWithOutput(String command) {
        try {
            ProcessBuilder builder = getProcessBuilder(sanitizeCommand(command));
            builder.redirectErrorStream(true);
            Process process = builder.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            process.waitFor();
            return output.toString();
        } catch (IOException | InterruptedException e) {
            Reporter.log("Failed to execute command with output: ", LogLevel.ERROR, command);
            return null;
        }
    }

    public static Map<String, String> getEnvironmentVariables() {
        return new HashMap<>(System.getenv());
    }

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

    public static void executeScript(String scriptPath) {
        try {
            String[] command;
            if (SystemUtils.IS_OS_WINDOWS) {
                if (scriptPath.endsWith(".bat") || scriptPath.endsWith(".cmd")) {
                    command = new String[]{"cmd", "/c", scriptPath};
                } else if (scriptPath.endsWith(".ps1")) {
                    command = new String[]{"powershell", "-File", scriptPath};
                } else {
                    throw new IllegalArgumentException("Unsupported script type");
                }
            } else {
                command = new String[]{"/bin/bash", scriptPath};
            }
            Process process = new ProcessBuilder(command).start();
            process.waitFor();
            Reporter.log("Successfully executed script: ", LogLevel.INFO_GREEN, scriptPath);
        } catch (IOException | InterruptedException e) {
            Reporter.log("Failed to execute script: ", LogLevel.ERROR, scriptPath);
        }
    }

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

    private static ProcessBuilder getProcessBuilder(String[] command) {
        List<String> commandList = new ArrayList<>();
        if (SystemUtils.IS_OS_WINDOWS) {
            commandList.add("cmd.exe");
            commandList.add("/c");
        } else {
            commandList.add("/bin/bash");
            commandList.add("-c");
        }
        for (String arg : command) {
            commandList.add(arg);
        }
        return new ProcessBuilder(commandList);
    }

    private static String[] sanitizeCommand(String command) {
        return command.split("\\s+");
    }
}