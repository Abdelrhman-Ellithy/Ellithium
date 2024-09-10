package Ellithium.Utilities;

import org.apache.commons.lang3.SystemUtils;
import org.checkerframework.checker.units.qual.C;

import java.io.IOException;

public class CommandExecutor {

    // Method to execute command and wait for completion
    public static void executeCommand(String command) {
        try {
            ProcessBuilder builder = getProcessBuilder(command);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            int exitCode = process.waitFor();
            logsUtils.info(Colors.GREEN+"Command exited with code: " + exitCode+Colors.RESET);
        } catch (IOException | InterruptedException e) {
            logsUtils.error(Colors.RED+"Failed to execute command: " + command+ Colors.RESET);
            logsUtils.logException(e);
        }
    }

    // Method to execute command in non-blocking manner and return the process object
    public static Process executeCommandNonBlocking(String command) {
        try {
            ProcessBuilder builder = getProcessBuilder(command);
            builder.redirectErrorStream(true);
            return builder.start();  // Return the running process
        } catch (IOException e) {
            logsUtils.error(Colors.RED+"Failed to execute command: " + command+ Colors.RESET);
            logsUtils.logException(e);
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
