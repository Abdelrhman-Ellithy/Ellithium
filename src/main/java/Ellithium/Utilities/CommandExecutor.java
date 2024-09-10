package Ellithium.Utilities;

import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;

public class CommandExecutor {

    // Method to execute command and wait for completion
    public static void executeCommand(String command) {
        try {
            ProcessBuilder builder = getProcessBuilder(command);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            int exitCode = process.waitFor();
            System.out.println("Command exited with code: " + exitCode);
        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to execute command: " + command);
            e.printStackTrace();
        }
    }

    // Method to execute command in non-blocking manner and return the process object
    public static Process executeCommandNonBlocking(String command) {
        try {
            ProcessBuilder builder = getProcessBuilder(command);
            builder.redirectErrorStream(true);
            return builder.start();  // Return the running process
        } catch (IOException e) {
            System.err.println("Failed to execute command: " + command);
            e.printStackTrace();
            return null;
        }
    }

    // Helper method to construct the process builder based on the OS
    private static ProcessBuilder getProcessBuilder(String command) {
        ProcessBuilder builder;
        if (SystemUtils.IS_OS_WINDOWS) {
            builder = new ProcessBuilder("cmd.exe", "/c", command);
        } else {
            builder = new ProcessBuilder("/bin/bash", "-c", command);
        }
        return builder;
    }
}
