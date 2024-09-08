package AutoEllithiumSphere.Utilities;

import java.io.IOException;

public class CommandExecutor {

    public static void executeCommand(String command) {
        try {
            ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
            builder.redirectErrorStream(true);

            Process process = builder.start();

            int exitCode = process.waitFor();
            System.out.println("\nCommand exited with code: " + exitCode);

        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to execute command: " + command);  // Log the error message
            e.printStackTrace();  // Print the stack trace to identify the issue
        }
    }
}
