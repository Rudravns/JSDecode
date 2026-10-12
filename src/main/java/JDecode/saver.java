
package JDecode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Serializes dictionaries to UTF-8 JSON files.
 */
public class saver {

    /** Path used by clients that track the most recently saved file. */
    public static String lastPath = "null.json";

    /**
     * Creates a saver.
     */
    public saver() {
    }

    /**
     * Writes a dictionary to a file using compact formatting.
     *
     * @param stringPath destination file path
     * @param data dictionary values to serialize
     * @throws UncheckedIOException if the file cannot be written
     */
    public static void Save(String stringPath, dictionary data) {
        Save(stringPath, data, 0);
    }

    /**
     * Writes a dictionary to a file with the requested indentation width.
     *
     * @param stringPath destination file path
     * @param data dictionary values to serialize
     * @param spacing number of spaces per indentation level
     * @throws UncheckedIOException if the file cannot be written
     */
    public static void Save(String stringPath, dictionary data, int spacing) {
        try {
            Path path = Paths.get(stringPath);
            Path resultPath = Files.write(path, data.toBytes(spacing), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            lastPath = stringPath;
            System.out.println("Successfully saved data to: " + resultPath.toAbsolutePath());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write file: " + stringPath, e);
        }
    }
}
