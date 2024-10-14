package Ellithium.Utilities.helpers;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.apache.commons.lang3.SystemUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
public class CommandExecutor {
    // Method to execute command and wait for completion
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

    // Helper method to construct the process builder based on the OS
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
    // Sanitize input command to prevent injection
    private static String[] sanitizeCommand(String command) {
        // Add sanitization logic here, e.g., escape special characters or limit allowed characters
        // You could also split the command into an array and sanitize each part
        return command.split("\\s+"); // Example: Basic splitting of command to avoid injection
    }
}