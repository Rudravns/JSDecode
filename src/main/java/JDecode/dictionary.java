
package JDecode;

import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stores named values and formats them as JSON.
 */
public class dictionary {

    private ArrayList<Container<?>> all_data_types;

    /**
     * Creates an empty dictionary.
     */
    public dictionary() {
        all_data_types = new ArrayList<>();
    }

    /**
     * Creates a dictionary populated from a map.
     *
     * @param values map whose keys must be strings
     * @throws IllegalArgumentException if any map key is not a string
     */
    @SuppressWarnings("this-escape")
    public dictionary(Map<?, ?> values) {
        this();
        if (values == null) {
            return;
        }

        for (Map.Entry<?, ?> entry : values.entrySet()) {
            if (!(entry.getKey() instanceof String)) {
                throw new IllegalArgumentException("Dictionary keys must be strings");
            }
            putValue((String) entry.getKey(), entry.getValue());
        }
    }

    /**
     * Adds or replaces all entries from a string-keyed map.
     *
     * @param map entries to add; a null map is ignored
     */
    public void putAll(Map<String, ?> map) {
        if (map == null) {
            return;
        }
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            add(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Adds or replaces a named value. A null value is retained and serialized
     * as the JSON literal {@code null}; null keys are ignored.
     *
     * @param key entry name
     * @param value value to store
     */
    public void add(String key, Object value) {
        putValue(key, value);
    }

    /**
     * Normalizes and stores a value without invoking an overridable method.
     *
     * @param key entry name
     * @param value value to store
     */
    @SuppressWarnings("this-escape")
    private void putValue(String key, Object value) {
        if (key == null) {
            return;
        }

        value = normalizeValue(value);

        removeValue(key);

        Container<?> targetContainer = getOrCreateContainer(value == null ? Object.class : value.getClass());
        @SuppressWarnings("unchecked")
        Container<Object> finalContainer = (Container<Object>) targetContainer;
        finalContainer.put(key, value);
    }

    /**
     * Converts supported Java values into forms understood by the serializer.
     *
     * @param value value to normalize
     * @return normalized value
     */
    private Object normalizeValue(Object value) {
        if (value instanceof Character) {
            return value.toString();
        }
        if (value instanceof Map<?, ?>) {
            return new dictionary((Map<?, ?>) value);
        }
        if (value instanceof List<?>) {
            ArrayList<Object> normalized = new ArrayList<>();
            for (Object element : (List<?>) value) {
                normalized.add(element == null ? null : normalizeValue(element));
            }
            return normalized;
        }
        if (value instanceof char[]) {
            return new String((char[]) value);
        }
        return value;
    }

    /**
     * Removes an entry from every value-type bucket.
     *
     * @param key entry name to remove
     */
    public void remove(String key) {
        removeValue(key);
    }

    /**
     * Removes an entry from all type buckets.
     *
     * @param key entry name to remove
     */
    private void removeValue(String key) {
        if (key == null) {
            return;
        }

        Iterator<Container<?>> containers = all_data_types.iterator();
        while (containers.hasNext()) {
            Container<?> container = containers.next();
            container.remove(key);
            if (container.isEmpty()) {
                containers.remove();
            }
        }
    }

    /**
     * Looks up an entry by name.
     *
     * @param key entry name
     * @return the stored value, or null when absent or explicitly null
     */
    public Object get(String key) {
        if (key == null) {
            return null;
        }

        for (Container<?> container : all_data_types) {
            if (container.containsKey(key)) {
                return container.get(key);
            }
        }
        return null;
    }

    /**
     * Finds the bucket for a type, creating one when necessary.
     *
     * @param type value type
     * @return existing or newly created value bucket
     */
    private Container<?> getOrCreateContainer(Class<?> type) {
        for (Container<?> container : all_data_types) {
            if (container.getType() == type) {
                return container;
            }
        }

        Container<?> newContainer = new Container<>(type);
        all_data_types.add(newContainer);
        return newContainer;
    }


    /**
     * Formats a supported value as JSON at the requested nesting depth.
     *
     * @param obj value to format
     * @param spacing indentation width
     * @param depth current nesting depth
     * @return JSON representation of the value
     */
    private String universalToString(Object obj, int spacing, int depth) {
        if (obj == null) {
            return "null";
        }

        if (obj instanceof String) {
            return quoteString((String) obj);
        }

        if (obj instanceof dictionary) {
            return ((dictionary) obj).formatDictionary(spacing, depth);
        }

        if (obj instanceof Iterable<?>) {
            StringBuilder formatted = new StringBuilder("[");
            boolean first = true;
            for (Object element : (Iterable<?>) obj) {
                if (!first) {
                    formatted.append(", ");
                }
                formatted.append(universalToString(element, spacing, depth));
                first = false;
            }
            return formatted.append("]").toString();
        }

        if (obj.getClass().isArray()) {
            StringBuilder formatted = new StringBuilder("[");
            for (int i = 0; i < Array.getLength(obj); i++) {
                if (i > 0) {
                    formatted.append(", ");
                }
                formatted.append(universalToString(Array.get(obj, i), spacing, depth));
            }
            return formatted.append("]").toString();
        }

        return obj.toString();
    }

    /**
     * Quotes a string and escapes characters required by JSON.
     *
     * @param value string to quote
     * @return quoted JSON string
     */
    private String quoteString(String value) {
        StringBuilder quoted = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '"':
                    quoted.append("\\\"");
                    break;
                case '\\':
                    quoted.append("\\\\");
                    break;
                case '\b':
                    quoted.append("\\b");
                    break;
                case '\f':
                    quoted.append("\\f");
                    break;
                case '\n':
                    quoted.append("\\n");
                    break;
                case '\r':
                    quoted.append("\\r");
                    break;
                case '\t':
                    quoted.append("\\t");
                    break;
                default:
                    if (character < 0x20) {
                        quoted.append(String.format("\\u%04x", (int) character));
                    } else {
                        quoted.append(character);
                    }
            }
        }
        return quoted.append("\"").toString();
    }

    /**
     * Groups dictionary entries that share a Java value type.
     *
     * @param <T> type of values in this container
     */
    public class Container<T> {

        private LinkedHashMap<String, T> myMap = new LinkedHashMap<>();
        private final Class<T> typeToken;

        /**
         * Creates a typed entry container.
         *
         * @param typeToken runtime class associated with the container
         */
        public Container(Class<T> typeToken) {
            this.typeToken = typeToken;
        }

        /**
         * Stores a value under a key.
         *
         * @param key entry name
         * @param value value to store
         */
        public void put(String key, T value) {
            myMap.put(key, value);
        }

        /**
         * Retrieves a value by key.
         *
         * @param key entry name
         * @return stored value, or null if absent or explicitly null
         */
        public T get(String key) {
            return myMap.get(key);
        }

        /**
         * Checks whether this container has the specified key.
         *
         * @param key entry name
         * @return {@code true} when the key is present
         */
        private boolean containsKey(String key) {
            return myMap.containsKey(key);
        }

        /**
         * Removes the specified key from this container.
         *
         * @param key entry name
         */
        private void remove(String key) {
            myMap.remove(key);
        }

        /**
         * Checks whether this container has any entries.
         *
         * @return {@code true} when the container is empty
         */
        private boolean isEmpty() {
            return myMap.isEmpty();
        }

        /**
         * Returns the runtime type associated with this container.
         *
         * @return type token
         */
        public Class<T> getType() {
            return typeToken;
        }

        /**
         * Returns the number of entries in this container.
         *
         * @return entry count
         */
        public int size(){
            return myMap.size();
        }

        /**
         * Returns the keys in this container.
         *
         * @return live key set
         */
        public Set<String> keySet(){
            return myMap.keySet();
        }

        /**
         * Collects the keys from all type containers in the owning dictionary.
         *
         * @return all dictionary entry names
         */
        public ArrayList<String> getAllDictionaryKeys() {
            ArrayList<String> masterKeyList = new ArrayList<>();

            for (Container<?> container : all_data_types) {
                masterKeyList.addAll(container.myMap.keySet());
            }

            return masterKeyList;
        }
    }

    /**
     * Creates a string containing the requested number of spaces.
     *
     * @param count number of spaces
     * @return the spaces
     */
    private String spaces(int count) {
        StringBuilder result = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            result.append(' ');
        }
        return result.toString();
    }

    /**
     * Formats this dictionary as indented JSON.
     *
     * @param spacing number of spaces per indentation level
     * @return JSON object text
     */
    public String toString(int spacing) {
        return formatDictionary(Math.max(0, spacing), 0);
    }

    /**
     * Formats this dictionary and its nested values.
     *
     * @param spacing number of spaces per indentation level
     * @param depth current object nesting depth
     * @return formatted JSON object text
     */
    private String formatDictionary(int spacing, int depth) {
        StringBuilder formatted = new StringBuilder();
        formatted.append("{\n");

        boolean hasEntries = false;

        for (Container<?> container : all_data_types) {
            for (String key : container.myMap.keySet()) {
                Object value = container.get(key);
                formatted.append(spaces(spacing * (depth + 1)));
                formatted.append(quoteString(key))
                        .append(": ");

                formatted.append(universalToString(value, spacing, depth + 1));

                formatted.append(", \n");
                hasEntries = true;
            }
        }


        // If we added at least one entry, remove the very last trailing ", \n"
        if (hasEntries) {
            formatted.setLength(formatted.length() - 3);
            formatted.append('\n');
        }

        formatted.append(spaces(spacing * depth));
        formatted.append("}");

        return formatted.toString();
    }

    /**
     * Formats this dictionary as compact JSON.
     *
     * @return compact JSON object text
     */
    @Override
    public String toString(){
       return toString(0);
    }

    /**
     * Copies the entries into a Java hash map.
     *
     * @return a map containing this dictionary's entries
     */
    public HashMap<String, Object> toHashMap() {
        HashMap<String, Object> values = new HashMap<>();
        for (Container<?> container : all_data_types) {
            values.putAll(container.myMap);
        }
        return values;
    }

    /**
     * Encodes compact JSON as UTF-8 bytes.
     *
     * @return UTF-8 JSON bytes
     */
    public byte[] toBytes(){
        return this.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Encodes indented JSON as UTF-8 bytes.
     *
     * @param spacing number of spaces per indentation level
     * @return UTF-8 JSON bytes
     */
    public byte[] toBytes(int spacing) {
        return toString(spacing).getBytes(StandardCharsets.UTF_8);
    }

}
