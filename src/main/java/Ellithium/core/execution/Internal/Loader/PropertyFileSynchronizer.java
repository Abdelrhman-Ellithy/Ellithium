package Ellithium.core.execution.Internal.Loader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Detects property keys present in the framework's bundled default file but missing from a
 * consuming project's own copy (e.g. a new key introduced by a version upgrade), and appends
 * the missing entries — default value and original explanatory comment — to the end of the
 * target file. Existing content in the target file is never modified or reordered.
 */
public class PropertyFileSynchronizer {

    private PropertyFileSynchronizer() {}

    private record TemplateEntry(String key, List<String> commentLines, String rawLine) {}

    /**
     * Appends any key present in {@code filePathInJar} (inside {@code jarFile}) but absent from
     * {@code targetFilePath} to the end of the target file. No-ops if the JAR, the target file,
     * or the JAR entry cannot be read, or if nothing is missing.
     */
    public static void syncMissingKeys(File jarFile, String filePathInJar, String targetFilePath,
                                       String frameworkVersion) {
        if (jarFile == null || !jarFile.exists()) return;
        File target = new File(targetFilePath);
        if (!target.exists()) return;

        List<String> templateLines = readJarEntryLines(jarFile, filePathInJar);
        if (templateLines == null) return;

        Set<String> existingKeys = readExistingKeys(target);
        List<TemplateEntry> missing = new ArrayList<>();
        for (TemplateEntry entry : parseEntries(templateLines)) {
            if (!existingKeys.contains(entry.key())) missing.add(entry);
        }
        if (missing.isEmpty()) return;

        appendMissing(target, missing, frameworkVersion);
    }

    private static List<String> readJarEntryLines(File jarFile, String filePathInJar) {
        try (JarFile jar = new JarFile(jarFile)) {
            JarEntry entry = jar.getJarEntry(filePathInJar);
            if (entry == null) return null;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(jar.getInputStream(entry), StandardCharsets.UTF_8))) {
                List<String> lines = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) lines.add(line);
                return lines;
            }
        } catch (IOException e) {
            return null;
        }
    }

    private static Set<String> readExistingKeys(File target) {
        Properties prop = new Properties();
        try (FileInputStream fis = new FileInputStream(target)) {
            prop.load(fis);
        } catch (IOException ignored) {}
        Set<String> keys = new HashSet<>();
        for (Object k : prop.keySet()) keys.add(k.toString());
        return keys;
    }

    /**
     * Splits the template into (key, value-line, leading comment block) entries. Scoped to the
     * simple {@code key=value} convention every bundled properties file in this project actually
     * uses (no {@code key: value}/space-separated forms, no line continuations) — not a
     * general-purpose Properties-spec parser. A key's comment block is the contiguous run of
     * {@code #}/{@code !} lines immediately above it; a blank line resets the run so unrelated
     * comments above a preceding key aren't misattributed.
     */
    private static List<TemplateEntry> parseEntries(List<String> lines) {
        List<TemplateEntry> entries = new ArrayList<>();
        List<String> pendingComments = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                pendingComments = new ArrayList<>();
                continue;
            }
            if (trimmed.startsWith("#") || trimmed.startsWith("!")) {
                pendingComments.add(line);
                continue;
            }
            int eq = line.indexOf('=');
            if (eq > 0) {
                String key = line.substring(0, eq).trim();
                entries.add(new TemplateEntry(key, pendingComments, line));
            }
            pendingComments = new ArrayList<>();
        }
        return entries;
    }

    private static void appendMissing(File target, List<TemplateEntry> missing, String frameworkVersion) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(target, true), StandardCharsets.UTF_8))) {
            writer.newLine();
            writer.write("# =============================================================================");
            writer.newLine();
            writer.write("# Properties added automatically by Ellithium v" + frameworkVersion
                    + " (missing from this file after an upgrade)");
            writer.newLine();
            writer.write("# =============================================================================");
            writer.newLine();
            for (TemplateEntry entry : missing) {
                writer.newLine();
                for (String comment : entry.commentLines()) {
                    writer.write(comment);
                    writer.newLine();
                }
                writer.write(entry.rawLine());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Failed to append missing properties to " + target.getPath() + ": " + e.getMessage());
            return;
        }
        for (TemplateEntry entry : missing) {
            System.out.println("[Ellithium] Added missing property to " + target.getPath() + ": " + entry.key());
        }
    }
}
