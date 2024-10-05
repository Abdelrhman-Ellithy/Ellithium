package Ellithium.Utilities.helpers;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarExtractor {
    public static void extractFolderFromJar(File jarFile, String folderPathInJar, File targetDirectory) {
        try {
            if (!targetDirectory.exists()) {
                Files.createDirectories(targetDirectory.toPath());
            }
            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().startsWith(folderPathInJar)) {
                        File targetFile = new File(targetDirectory, entry.getName().substring(folderPathInJar.length()));
                        if (entry.isDirectory()) {
                            targetFile.mkdirs();
                        } else {
                            Files.copy(jar.getInputStream(entry), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error extracting folder from JAR file: " + e.getMessage());
            e.printStackTrace();
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
            System.err.println("Extracted file from JAR:: " + e.getMessage());
        }
    }
}
