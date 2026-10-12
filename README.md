# JDecode

JDecode is a small Java library for parsing JSON objects into a typed
dictionary and serializing dictionary values back to JSON. It supports JSON
objects, arrays, strings and escapes, numbers, booleans, and `null`, including
nested collections.

## Requirements

- Java 11 or newer
- Maven 3.6 or newer or Gradle 8.14.3 or newer to build the JAR

## Build with Maven

From the project root, build the package with:

```powershell
mvn package
```

The JAR is created at `target/jsdecode-1.0.0.jar`.

## Build with Gradle

Build the JAR with an installed Gradle version:

```powershell
gradle build
```

Or run the included example program directly:

```powershell
gradle run --args="test_data/settings.json"
```

The JAR is created at `build/libs/JDecode-1.0.0.jar`. Its manifest allows
running the example program with either build:

```powershell
java -jar target/jsdecode-1.0.0.jar test_data/settings.json
```

For a Gradle-built JAR, use `build/libs/JDecode-1.0.0.jar` in the command.
Omit the path argument to load `test_data/settings.json` by default. To compile
without Maven or Gradle:

```powershell
javac -d build\classes src\main\java\JDecode\*.java
java -cp build\classes JDecode.Main test_data\settings.json
```

## Use the library

All classes are in the `JDecode` package:

```java
import JDecode.LoadFile;
import JDecode.Parser;
import JDecode.dictionary;
import JDecode.saver;

dictionary settings = new dictionary();
new Parser(LoadFile.load("settings.json"), settings).parse();

dictionary output = new dictionary();
output.add("enabled", true);
output.add("retries", 3);
output.add("optionalValue", null);
saver.Save("output.json", output, 2);
```

`Parser.parse()` parses one top-level JSON object and adds its entries to the
provided dictionary. `dictionary.toString(int)` returns formatted JSON, and
`saver.Save(...)` writes it as UTF-8. File read/write failures are reported as
`UncheckedIOException` rather than being silently replaced with empty data.

The existing lowercase public class names (`dictionary` and `saver`) are kept
for compatibility with the original source.
