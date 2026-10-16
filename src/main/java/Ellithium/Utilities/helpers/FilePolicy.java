package Ellithium.Utilities.helpers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Strict I/O & Encoding Policy for Ellithium framework.
 * Enforces uniform {@link StandardCharsets#UTF_8} and {@link Path}-based
 * access across all operating systems and file utilities.
 */
final class FilePolicy {

    /**
     * Default canonical charset used for all framework text I/O operations.
     */
    public static final Charset ENCODING = StandardCharsets.UTF_8;

    private FilePolicy() {
        // utility class
    }

    /**
     * Converts a string path to a validated, normalized {@link Path}.
     *
     * @param pathString string representation of file path
     * @return normalized {@link Path}
     * @throws IllegalArgumentException if pathString is null or blank
     */
    public static Path toPath(String pathString) {
        if (pathString == null || pathString.isBlank()) {
            throw new IllegalArgumentException("File path must not be null or blank");
        }
        return Paths.get(pathString).normalize();
    }

    /**
     * Validates that the provided path is non-null.
     *
     * @param path the path to validate
     * @return the verified path
     * @throws IllegalArgumentException if path is null
     */
    public static Path requirePath(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null");
        }
        return path.normalize();
    }

    /**
     * Opens a file for reading using strict UTF-8 decoding.
     *
     * @param path file path
     * @return configured BufferedReader
     * @throws IOException on I/O error
     */
    public static BufferedReader newUtf8Reader(Path path) throws IOException {
        return Files.newBufferedReader(requirePath(path), ENCODING);
    }

    /**
     * Opens a file for writing using strict UTF-8 encoding.
     *
     * @param path file path
     * @param options open options (e.g. StandardOpenOption.CREATE, StandardOpenOption.APPEND)
     * @return configured BufferedWriter
     * @throws IOException on I/O error
     */
    public static BufferedWriter newUtf8Writer(Path path, OpenOption... options) throws IOException {
        Path p = requirePath(path);
        if (p.getParent() != null && !Files.exists(p.getParent())) {
            Files.createDirectories(p.getParent());
        }
        return Files.newBufferedWriter(p, ENCODING, options);
    }
}
