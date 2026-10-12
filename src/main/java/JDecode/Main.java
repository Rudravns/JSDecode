package JDecode;

import java.util.List;
import java.util.Map;

/**
 * Command-line entry point and usage examples for the JDecode library.
 */
public class Main {

    /**
     * Creates the example program entry point.
     */
    public Main() {
    }

    /**
     * Loads and parses a JSON file. The first argument overrides the default
     * example file path.
     *
     * @param args optional path to a JSON file
     */
    public static void main(String[] args) {
        String path = args.length == 0 ? "test_data/settings.json" : args[0];
        LoadJson(path).parse();
    }

    /**
     * Reads the specified JSON file and creates a parser for its contents.
     *
     * @param path path to the JSON file
     * @return a parser containing the file contents
     */
    public static Parser LoadJson(String path) {
        return new Parser(LoadFile.load(path));
    }

    /**
     * Demonstrates storing supported Java values and writing them as JSON.
     */
    public static void test_all_data(){
        dictionary dict = new dictionary();
        // --- 1. Integers & Whole Numbers ---
        dict.add("random_seed", 12345);
        dict.add("max_iterations", 1000);
        dict.add("large_id", 9876543210L); // Long

        // --- 2. Decimals / Floating Point Numbers ---
        dict.add("learning_rate", 0.001);   // Double
        dict.add("threshold", 4.5f);        // Float

        // --- 3. Booleans ---
        dict.add("enable_logging", true);
        dict.add("is_production", false);

        // --- 4. Strings & Characters ---
        dict.add("log_level", "DEBUG");
        dict.add("output_directory", "results/");
        dict.add("currency_symbol", 'u'); // Character

        // --- 5. Arrays ---
        dict.add("input_files", new String[]{"data/input1.txt", "data/input2.txt"});
        dict.add("prime_numbers", new int[]{2, 3, 5, 7, 11});

        // --- 6. Collections (Lists & Maps for nested structures) ---
        dict.add("tags_list", List.of("ai", "testing", "java"));
        dict.add("metadata_map", Map.of("version", "1.0", "author", "AI Developer"));
      
        // --- 7. Null & Edge Cases ---
        dict.add("backup_directory", null); // Testing null handling
 
        // --- Output & Save ---
        System.out.println(dict.toString(5));
        saver.Save("test_data/settings.json", dict, 4);
    }
}
