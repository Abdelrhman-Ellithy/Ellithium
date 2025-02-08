package Ellithium.Utilities.helpers;

import java.io.*;
import java.nio.file.*;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarExtractor {
    public static boolean extractFolderFromJar(File jarFile, String folderPathInJar, File targetDirectory) {
        if (!jarFile.exists()) {
            System.err.println("JAR file does not exist: " + jarFile.getPath());
            return false;
        }

        // Check directory writability before attempting any operations
        if (!targetDirectory.exists()) {
            boolean created = targetDirectory.mkdirs();
            if (!created) {
                System.err.println("Failed to create target directory: " + targetDirectory.getPath());
                return false;
            }
        }
        if (!targetDirectory.canWrite()) {
            System.err.println("Target directory is not writable: " + targetDirectory.getPath());
            return false;
        }

        String canonicalTargetPath;
        try {
            canonicalTargetPath = targetDirectory.getCanonicalPath() + File.separator;
        } catch (IOException e) {
            System.err.println("Invalid target directory path: " + e.getMessage());
            return false;
        }

        try (JarFile jar = new JarFile(jarFile)) {
            // First verify all entries are safe
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().startsWith(folderPathInJar)) {
                    continue;
                }

                File targetFile = new File(targetDirectory, entry.getName().substring(folderPathInJar.length()));
                String canonicalDestPath;
                try {
                    canonicalDestPath = targetFile.getCanonicalPath();
                } catch (IOException e) {
                    System.err.println("Invalid entry path: " + entry.getName());
                    return false;
                }

                if (!canonicalDestPath.startsWith(canonicalTargetPath)) {
                    System.err.println("Entry is outside of target directory: " + entry.getName());
                    return false;
                }
            }

            // If we got here, all entries are safe - now extract them
            boolean hasValidEntries = false;
            entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().startsWith(folderPathInJar)) {
                    continue;
                }

                hasValidEntries = true;
                File targetFile = new File(targetDirectory, entry.getName().substring(folderPathInJar.length()));

                if (entry.isDirectory()) {
                    if (!targetFile.exists() && !targetFile.mkdirs()) {
                        System.err.println("Failed to create directory: " + targetFile.getPath());
                        return false;
                    }
                } else {
                    File parent = targetFile.getParentFile();
                    if (!parent.exists() && !parent.mkdirs()) {
                        System.err.println("Failed to create parent directory: " + parent.getPath());
                        return false;
                    }

                    try {
                        Path tempFile = Files.createTempFile("jar_extract", null);
                        Files.copy(jar.getInputStream(entry), tempFile, StandardCopyOption.REPLACE_EXISTING);
                        Files.move(tempFile, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    } catch (IOException e) {
                        System.err.println("Error extracting file: " + targetFile.getPath() + " - " + e.getMessage());
                        return false;
                    }
                }
            }
            return hasValidEntries;

        } catch (IOException e) {
            System.err.println("Error extracting from JAR: " + e.getMessage());
            return false;
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