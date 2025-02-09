package Ellithium.Utilities.helpers;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

public class JarExtractor {
    public static boolean extractFolderFromJar(File jarFile, String folderPathInJar, File targetDirectory) {
        if (!jarFile.exists()) {
            System.err.println("JAR file does not exist: " + jarFile.getPath());
            return false;
        }

        // Clean target directory if exists
        if (targetDirectory.exists()) {
            deleteDirectory(targetDirectory);
        }
        
        if (!targetDirectory.mkdirs()) {
            System.err.println("Failed to create target directory: " + targetDirectory.getPath());
            return false;
        }

        String normalizedTargetPath;
        try {
            normalizedTargetPath = targetDirectory.getCanonicalPath() + File.separator;
        } catch (IOException e) {
            System.err.println("Invalid target directory path: " + e.getMessage());
            return false;
        }
        try (JarFile jar = new JarFile(jarFile)) {
            for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements();) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().startsWith(folderPathInJar)) {
                    continue;
                }

                String targetPath = entry.getName().substring(folderPathInJar.length());
                File targetFile = new File(targetDirectory, targetPath);
                
                try {
                    String canonicalDestPath = targetFile.getCanonicalPath();
                    if (!canonicalDestPath.startsWith(normalizedTargetPath)) {
                        System.err.println("Security violation - Path traversal attempt detected: " + entry.getName());
                        deleteDirectory(targetDirectory);
                        return false;
                    }
                } catch (IOException e) {
                    System.err.println("Invalid path detected: " + entry.getName());
                    deleteDirectory(targetDirectory);
                    return false;
                }
            }
        } catch (IOException e) {
            System.err.println("Error validating JAR entries: " + e.getMessage());
            deleteDirectory(targetDirectory);
            return false;
        }
        boolean hasValidEntries = false;
        Map<String, Boolean> processedPaths = new HashMap<>();

        try (JarFile jar = new JarFile(jarFile)) {
            for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements();) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().startsWith(folderPathInJar) || !entry.isDirectory()) {
                    continue;
                }

                hasValidEntries = true;
                String targetPath = entry.getName().substring(folderPathInJar.length());
                File targetFile = new File(targetDirectory, targetPath);
                
                if (!targetFile.exists() && !targetFile.mkdirs()) {
                    System.err.println("Failed to create directory: " + targetFile.getPath());
                    deleteDirectory(targetDirectory);
                    return false;
                }
                processedPaths.put(targetFile.getPath(), true);
            }

            // Then process files
            for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements();) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().startsWith(folderPathInJar) || entry.isDirectory()) {
                    continue;
                }

                hasValidEntries = true;
                String targetPath = entry.getName().substring(folderPathInJar.length());
                File targetFile = new File(targetDirectory, targetPath);
                
                // Handle case where we have both file and directory with same name
                if (processedPaths.containsKey(targetFile.getPath())) {
                    targetFile = new File(targetFile.getPath() + ".file");
                }

                File parent = targetFile.getParentFile();
                if (!parent.exists() && !parent.mkdirs()) {
                    System.err.println("Failed to create parent directory: " + parent.getPath());
                    deleteDirectory(targetDirectory);
                    return false;
                }

                try {
                    Path tempFile = Files.createTempFile("jar_extract", null);
                    Files.copy(jar.getInputStream(entry), tempFile, StandardCopyOption.REPLACE_EXISTING);
                    Files.move(tempFile, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (IOException e) {
                    System.err.println("Failed to extract file: " + targetFile.getPath());
                    deleteDirectory(targetDirectory);
                    return false;
                }
            }

            return hasValidEntries;

        } catch (IOException e) {
            System.err.println("Error extracting from JAR: " + e.getMessage());
            deleteDirectory(targetDirectory);
            return false;
        }
    }

    private static void deleteDirectory(File directory) {
        if (directory.exists()) {
            try {
                Files.walk(directory.toPath())
                    .sorted((p1, p2) -> -p1.compareTo(p2))
                    .map(Path::toFile)
                    .forEach(File::delete);
            } catch (IOException e) {
                System.err.println("Failed to clean up directory: " + directory.getPath());
            }
        }
    }

    public static void extractFileFromJar(File jarFile, String filePathInJar, File outputFile) {
        try (JarFile jar = new JarFile(jarFile)) {
            JarEntry entry = jar.getJarEntry(filePathInJar);
            if (entry != null) {
                if (!outputFile.getParentFile().exists()) {
                    outputFile.getParentFile().mkdirs();
                }
                Files.copy(jar.getInputStream(entry), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Extracted file from JAR: " + filePathInJar);
            } else {
                System.err.println("File not found in JAR: " + filePathInJar);
            }
        }
        catch (IOException e) {
            System.err.println("Extracted file from JAR: " + e.getMessage());
        }
    }
}