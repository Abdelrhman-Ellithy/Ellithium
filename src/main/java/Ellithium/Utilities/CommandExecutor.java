package Ellithium.Utilities;

import Ellithium.Internal.LogLevel;
import Ellithium.Internal.Reporter;
import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;

public class CommandExecutor {

    // Method to execute command and wait for completion
    public static void executeCommand(String command) {
        Reporter.log("Attempting to execute command: ", LogLevel.INFO_GREEN, command);
        try {
            ProcessBuilder builder = getProcessBuilder(command);
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
            ProcessBuilder builder = getProcessBuilder(command);
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
    private static ProcessBuilder getProcessBuilder(String command) {
        if (SystemUtils.IS_OS_WINDOWS) {
            return new ProcessBuilder("cmd.exe", "/c", command);
        } else {
            return new ProcessBuilder("/bin/bash", "-c", command);
        }
    }
}
