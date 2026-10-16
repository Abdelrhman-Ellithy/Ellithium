package Ellithium.Utilities.helpers;

import org.apache.commons.lang3.SystemUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CommandExecutorTests {

    @Test
    public void testGetSystemInfo() {
        String info = CommandExecutor.getSystemInfo();
        Assert.assertNotNull(info);
        Assert.assertTrue(info.contains("OS:"));
        Assert.assertTrue(info.contains("Java Version:"));
        Assert.assertTrue(info.contains("Architecture:"));
    }

    @Test
    public void testGetEnvironmentVariables() {
        Map<String, String> env = CommandExecutor.getEnvironmentVariables();
        Assert.assertNotNull(env);
        Assert.assertFalse(env.isEmpty());
        Assert.assertTrue(env.containsKey("PATH") || env.containsKey("Path"));
    }

    @Test
    public void testExecuteCommandWithOutput_echo() {
        String output;
        if (SystemUtils.IS_OS_WINDOWS) {
            output = CommandExecutor.executeCommandWithOutput("cmd /c echo EllithiumAutomation");
        } else {
            output = CommandExecutor.executeCommandWithOutput("echo EllithiumAutomation");
        }
        Assert.assertNotNull(output);
        Assert.assertTrue(output.contains("EllithiumAutomation"));
    }

    @Test
    public void testExecuteCommandList() {
        List<String> cmd;
        if (SystemUtils.IS_OS_WINDOWS) {
            cmd = List.of("cmd.exe", "/c", "echo", "ParallelTest");
        } else {
            cmd = List.of("echo", "ParallelTest");
        }
        int exitCode = CommandExecutor.executeCommand(cmd);
        Assert.assertEquals(exitCode, 0);
    }

    @Test
    public void testExecuteCommandList_withTimeout_succeeds() {
        List<String> cmd;
        if (SystemUtils.IS_OS_WINDOWS) {
            cmd = List.of("cmd.exe", "/c", "echo", "FastEcho");
        } else {
            cmd = List.of("echo", "FastEcho");
        }
        int exitCode = CommandExecutor.executeCommand(cmd, 10, TimeUnit.SECONDS);
        Assert.assertEquals(exitCode, 0);
    }

    @Test
    public void testExecuteCommand_nullAndBlankGuards() {
        // None should throw exception
        CommandExecutor.executeCommand((String) null);
        CommandExecutor.executeCommand("");
        CommandExecutor.executeCommand("   ");

        int code1 = CommandExecutor.executeCommand((List<String>) null);
        Assert.assertEquals(code1, -1);

        int code2 = CommandExecutor.executeCommand(List.of());
        Assert.assertEquals(code2, -1);

        String out1 = CommandExecutor.executeCommandWithOutput(null);
        Assert.assertNull(out1);

        String out2 = CommandExecutor.executeCommandWithOutput("  ");
        Assert.assertNull(out2);

        Process p = CommandExecutor.executeCommandNonBlocking(null);
        Assert.assertNull(p);

        Process p2 = CommandExecutor.executeCommandNonBlocking("  ");
        Assert.assertNull(p2);
    }

    @Test
    public void testSanitizeCommand_tokensAndQuotes() {
        String[] empty = CommandExecutor.sanitizeCommand(null);
        Assert.assertEquals(empty.length, 0);

        String[] empty2 = CommandExecutor.sanitizeCommand("   ");
        Assert.assertEquals(empty2.length, 0);

        String[] tokens = CommandExecutor.sanitizeCommand("git commit -m \"initial commit\" --quiet");
        Assert.assertEquals(tokens.length, 5);
        Assert.assertEquals(tokens[0], "git");
        Assert.assertEquals(tokens[1], "commit");
        Assert.assertEquals(tokens[2], "-m");
        Assert.assertEquals(tokens[3], "initial commit");
        Assert.assertEquals(tokens[4], "--quiet");
    }

    @Test
    public void testCreateAndDeleteDirectory() throws IOException {
        Path tempDir = Files.createTempDirectory("ellithium_dir_test_");
        Path subDir = tempDir.resolve("nested").resolve("deep");

        CommandExecutor.createDirectory(subDir.toString());
        Assert.assertTrue(Files.exists(subDir));

        // Create a dummy file inside
        Files.writeString(subDir.resolve("test.txt"), "hello");

        CommandExecutor.deleteDirectory(tempDir.toString());
        Assert.assertFalse(Files.exists(tempDir));

        // Calling on null or non-existent path should not throw
        CommandExecutor.createDirectory(null);
        CommandExecutor.deleteDirectory(null);
        CommandExecutor.deleteDirectory(tempDir.resolve("does_not_exist").toString());
    }

    @Test
    public void testCopyFile() throws IOException {
        Path src = Files.createTempFile("src_", ".txt");
        Path destDir = Files.createTempDirectory("dest_dir_");
        Path dest = destDir.resolve("nested_folder").resolve("dest_" + System.currentTimeMillis() + ".txt");
        try {
            Files.writeString(src, "Payload Data");
            // Tests that copyFile automatically creates parent directories when missing!
            CommandExecutor.copyFile(src.toString(), dest.toString());

            Assert.assertTrue(Files.exists(dest));
            Assert.assertEquals(Files.readString(dest), "Payload Data");

            // Null checks
            CommandExecutor.copyFile(null, dest.toString());
            CommandExecutor.copyFile(src.toString(), null);
        } finally {
            Files.deleteIfExists(src);
            Files.deleteIfExists(dest);
            CommandExecutor.deleteDirectory(destDir.toString());
        }
    }

    @Test
    public void testKillProcess_nullAndBlankGuards() {
        CommandExecutor.killProcess(null);
        CommandExecutor.killProcess("");
        CommandExecutor.killProcess("   ");
    }

    @Test
    public void testOpenFile_nullAndBlankGuards() {
        CommandExecutor.openFile(null);
        CommandExecutor.openFile("   ");
    }

    @Test
    public void testExecuteScript_nullAndBlankGuards() {
        CommandExecutor.executeScript(null);
        CommandExecutor.executeScript("   ");
    }

    @Test
    public void testListProcessesAndIsProcessRunning() {
        List<String> processes = CommandExecutor.listProcesses();
        Assert.assertNotNull(processes);
        Assert.assertFalse(processes.isEmpty());

        boolean runningJava = CommandExecutor.isProcessRunning("java");
        boolean runningFake = CommandExecutor.isProcessRunning("definitely_not_a_real_process_12345");
        Assert.assertFalse(runningFake);

        Assert.assertFalse(CommandExecutor.isProcessRunning(null));
        Assert.assertFalse(CommandExecutor.isProcessRunning("   "));
    }
}
