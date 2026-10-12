package JDecode;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parses JSON text into a {@link dictionary}.
 */
public class Parser {
    private final String data;
    private final dictionary output;
    private int index;

    /**
     * Creates a parser with a new dictionary for the parsed values.
     *
     * @param data JSON text to parse
     * @throws IllegalArgumentException if {@code data} is null
     */
    public Parser(String data) {
        this(data, new dictionary());
    }

    /**
     * Creates a parser that places parsed values in the supplied dictionary.
     *
     * @param data JSON text to parse
     * @param dict destination dictionary
     * @throws IllegalArgumentException if either argument is null
     */
    public Parser(String data, dictionary dict) {
        if (data == null) {
            throw new IllegalArgumentException("JSON input cannot be null");
        }
        if (dict == null) {
            throw new IllegalArgumentException("Output dictionary cannot be null");
        }
        this.data = data;
        this.output = dict;
    }

    /**
     * Parses the complete JSON object and adds its entries to the destination.
     *
     * @throws IllegalArgumentException if the input is not a single valid JSON object
     */
    public void parse() {
        index = 0;
        skipWhitespace();
        Map<String, Object> values = parseObject();
        skipWhitespace();
        if (index != data.length()) {
            throw error("Unexpected content after the JSON object");
        }

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            output.add(entry.getKey(), entry.getValue());
        }
        System.out.println(output.toString(3));
    }

    /**
     * Parses a JSON object at the current input position.
     *
     * @return object members in their source order
     */
    private Map<String, Object> parseObject() {
        expect('{');
        skipWhitespace();
        Map<String, Object> values = new LinkedHashMap<>();

        if (consume('}')) {
            return values;
        }

        while (true) {
            skipWhitespace();
            if (index >= data.length() || data.charAt(index) != '"') {
                throw error("Expected a quoted object key");
            }
            String key = parseString();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            values.put(key, parseValue());
            skipWhitespace();

            if (consume('}')) {
                return values;
            }
            expect(',');
        }
    }

    /**
     * Parses an array at the current input position.
     *
     * @return array elements in source order
     */
    private ArrayList<Object> parseArray() {
        expect('[');
        skipWhitespace();
        ArrayList<Object> values = new ArrayList<>();

        if (consume(']')) {
            return values;
        }

        while (true) {
            skipWhitespace();
            values.add(parseValue());
            skipWhitespace();

            if (consume(']')) {
                return values;
            }
            expect(',');
        }
    }

    /**
     * Parses a JSON value based on its leading character.
     *
     * @return the parsed scalar, object, array, or null value
     */
    private Object parseValue() {
        if (index >= data.length()) {
            throw error("Expected a JSON value");
        }

        char current = data.charAt(index);
        switch (current) {
            case '"':
                return parseString();
            case '{':
                return parseObject();
            case '[':
                return parseArray();
            case 't':
                consumeLiteral("true");
                return true;
            case 'f':
                consumeLiteral("false");
                return false;
            case 'n':
                consumeLiteral("null");
                return null;
            default:
                if (current == '-' || isDigit(current)) {
                    return parseNumber();
                }
                throw error("Expected a JSON value");
        }
    }

    /**
     * Parses a JSON string, including its supported escape sequences.
     *
     * @return the unescaped string value
     */
    private String parseString() {
        expect('"');
        StringBuilder value = new StringBuilder();

        while (index < data.length()) {
            char current = data.charAt(index++);
            if (current == '"') {
                return value.toString();
            }
            if (current == '\\') {
                if (index >= data.length()) {
                    throw error("Unterminated escape sequence");
                }
                char escaped = data.charAt(index++);
                switch (escaped) {
                    case '"':
                    case '\\':
                    case '/':
                        value.append(escaped);
                        break;
                    case 'b':
                        value.append('\b');
                        break;
                    case 'f':
                        value.append('\f');
                        break;
                    case 'n':
                        value.append('\n');
                        break;
                    case 'r':
                        value.append('\r');
                        break;
                    case 't':
                        value.append('\t');
                        break;
                    case 'u':
                        value.append(parseUnicodeEscape());
                        break;
                    default:
                        throw error("Invalid escape sequence");
                }
            } else if (current < 0x20) {
                throw error("Unescaped control character in string");
            } else {
                value.append(current);
            }
        }
        throw error("Unterminated string");
    }

    /**
     * Reads the four hexadecimal digits following a Unicode escape.
     *
     * @return the decoded UTF-16 code unit
     */
    private char parseUnicodeEscape() {
        if (index + 4 > data.length()) {
            throw error("Incomplete Unicode escape");
        }
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int digit = Character.digit(data.charAt(index++), 16);
            if (digit < 0) {
                throw error("Invalid Unicode escape");
            }
            value = value * 16 + digit;
        }
        return (char) value;
    }

    /**
     * Parses a JSON number as an integer or decimal value.
     *
     * @return an {@link Integer}, {@link Long}, {@link BigInteger}, or {@link BigDecimal}
     */
    private Number parseNumber() {
        int start = index;
        consume('-');

        if (consume('0')) {
            if (index < data.length() && isDigit(data.charAt(index))) {
                throw error("Leading zero in number");
            }
        } else {
            requireDigit();
            while (index < data.length() && isDigit(data.charAt(index))) {
                index++;
            }
        }

        boolean decimal = false;
        if (consume('.')) {
            decimal = true;
            requireDigit();
            while (index < data.length() && isDigit(data.charAt(index))) {
                index++;
            }
        }

        if (consume('e') || consume('E')) {
            decimal = true;
            if (!consume('+')) {
                consume('-');
            }
            requireDigit();
            while (index < data.length() && isDigit(data.charAt(index))) {
                index++;
            }
        }

        String number = data.substring(start, index);
        try {
            if (decimal) {
                return new BigDecimal(number);
            }
            BigInteger integer = new BigInteger(number);
            if (integer.bitLength() < 32) {
                return integer.intValue();
            }
            if (integer.bitLength() < 64) {
                return integer.longValue();
            }
            return integer;
        } catch (NumberFormatException exception) {
            throw error("Invalid number");
        }
    }

    /**
     * Consumes the expected JSON literal at the current input position.
     *
     * @param literal expected literal text
     */
    private void consumeLiteral(String literal) {
        if (!data.startsWith(literal, index)) {
            throw error("Invalid JSON literal");
        }
        index += literal.length();
    }

    /**
     * Verifies that the current character is a decimal digit.
     */
    private void requireDigit() {
        if (index >= data.length() || !isDigit(data.charAt(index))) {
            throw error("Expected a digit");
        }
    }

    /**
     * Checks whether a character is an ASCII decimal digit.
     *
     * @param value character to inspect
     * @return {@code true} when the character is between {@code 0} and {@code 9}
     */
    private boolean isDigit(char value) {
        return value >= '0' && value <= '9';
    }

    /**
     * Advances past JSON whitespace at the current input position.
     */
    private void skipWhitespace() {
        while (index < data.length()) {
            char current = data.charAt(index);
            if (current == ' ' || current == '\n' || current == '\r' || current == '\t') {
                index++;
            } else {
                return;
            }
        }
    }

    /**
     * Consumes the expected character if it is next in the input.
     *
     * @param expected character to consume
     * @return {@code true} when the character was consumed
     */
    private boolean consume(char expected) {
        if (index < data.length() && data.charAt(index) == expected) {
            index++;
            return true;
        }
        return false;
    }

    /**
     * Requires the specified character at the current input position.
     *
     * @param expected required character
     */
    private void expect(char expected) {
        if (!consume(expected)) {
            throw error("Expected '" + expected + "'");
        }
    }

    /**
     * Creates a parse exception annotated with the current input position.
     *
     * @param message explanation of the parse failure
     * @return an exception describing the failure
     */
    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException(message + " at position " + index);
    }
}
