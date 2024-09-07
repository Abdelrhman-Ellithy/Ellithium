package AutoEllithiumSphere.Utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CommandExecutor {

    // This method will execute a given command in the terminal
    public static void executeCommand(String command) {
        try {
            // Use ProcessBuilder to run the command
            ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
            builder.redirectErrorStream(true);

            // Start the process
            Process process = builder.start();

            // Wait for the process to complete
            int exitCode = process.waitFor();
            System.out.println("\nCommand exited with code: " + exitCode);

        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to execute command: " + command);  // Log the error message
            e.printStackTrace();  // Print the stack trace to identify the issue
        }
    }
}
