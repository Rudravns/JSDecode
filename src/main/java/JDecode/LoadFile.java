package JDecode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads UTF-8 text files and remembers the most recently used path.
 */
public class LoadFile {

    /** Path used by the no-argument {@link #load()} overload. */
    public static String lastPath = "null.json";

    /**
     * Creates a file loader.
     */
    public LoadFile() {
    }

    /**
     * Reads a file into a string and remembers its path for later calls.
     *
     * @param path path to the file to read
     * @return the file contents
     * @throws UncheckedIOException if the file cannot be read
     */
    public static String load(String path) {
        try {
            String content = Files.readString(Path.of(path));
            lastPath = path;
            return content;
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Could not read file: " + Path.of(path).toAbsolutePath().normalize(), e);
        }
    }

    /**
     * Reads the file identified by {@link #lastPath}.
     *
     * @return the remembered file contents
     * @throws UncheckedIOException if the file cannot be read
     */
    public static String load(){
        return load(lastPath);
    }
}
