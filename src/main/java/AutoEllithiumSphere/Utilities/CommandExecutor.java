package AutoEllithiumSphere.Utilities;

import org.apache.commons.lang3.SystemUtils;
import java.io.IOException;

public class CommandExecutor {

    public static void executeCommand(String command) {
        try {
            ProcessBuilder builder;

            // Check if the OS is Windows or Unix-based
            if (SystemUtils.IS_OS_WINDOWS) {
                // Windows: Use cmd.exe to execute the command
                builder = new ProcessBuilder("cmd.exe", "/c", command);
            } else {
                // Unix-based systems: Use bash or sh to execute the command
                builder = new ProcessBuilder("/bin/bash", "-c", command);
            }

            // Redirect error stream to standard output
            builder.redirectErrorStream(true);

            // Start the process
            Process process = builder.start();

            // Wait for the process to complete and get exit code
            int exitCode = process.waitFor();
            System.out.println("Command exited with code: " + exitCode);

        } catch (IOException | InterruptedException e) {
            // Handle errors during command execution
            System.err.println("Failed to execute command: " + command);
            e.printStackTrace();  // Print stack trace for debugging
        }
    }
}