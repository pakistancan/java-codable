# java-codable
java-codable is a Java beans to swift codable converter, it is java compile plugin, you need to add this your maven configuration to get it working.

## Configuration

Add codeable to project dependencies

```xml
		<dependency>
			<groupId>io.github.pakistancan</groupId>
			<artifactId>codable-converter</artifactId>
			<version>1.0</version>
		</dependency>
```

## Compiler configuration

```xml
	<build>
		<sourceDirectory>src/main/java</sourceDirectory>
		<plugins>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-compiler-plugin</artifactId>
				<version>3.10.1</version>
				<configuration>
					<source>1.8</source>
					<target>1.8</target>
					<annotationProcessors>
						<annotationProcessor>io.github.pakistancan.codable.processor.CodableGenerator</annotationProcessor>
					</annotationProcessors>
					<verbose>true</verbose>
					<compilerArguments>
						<Xlint />
						<AOutputDir>./generated/</AOutputDir>
						<APackagePrefix>com.alix.</APackagePrefix>

						<!-- -Xlint:all -->
					</compilerArguments>
					<showWarnings>true</showWarnings>
					<showDeprecation>true</showDeprecation>
				</configuration>
			</plugin>
		</plugins>
	</build>
```

It will convert classes referenced from `com.alix.` and all of its sub-packages, output will be generated in `<PROJECT_DIR>/generated/swift`, if package contains `.request.` then it'll generate those classes in REQ subfolder, otherwise in RES sub-folder.

Enums will be generated in `Enums` sub-folder, if there is any date referenced in any class, Formatters class will be generated in `Formatter/` sub-folder


## Usage
Decorate your beans with @Codable annotation, if it is specified on base class it is already applied on derived classes, java-codable regards `com.fasterxml.jackson.annotation.JsonProperty` to apply custom name to serialize/deserialize properties and `com.fasterxml.jackson.annotation.JsonFormat` to use specific date formats.

### Example 
```java
/**
 * 
 */
package com.alix.request;

import io.github.pakistancan.codable.annotation.Codable;
import io.github.pakistancan.codable.annotation.IgnoreProperty;

/**
 * @author muhammadali
 *
 */

@Codable
public class BookInfo {

	private String title;
	private String isbn;
	private AuthorInfo[] authors;
	
	@IgnoreProperty
	private String ignoredField;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getIsbn() {
		return isbn;
	}

	public void setIsbn(String isbn) {
		this.isbn = isbn;
	}

	public AuthorInfo[] getAuthors() {
		return authors;
	}

	public void setAuthors(AuthorInfo[] authors) {
		this.authors = authors;
	}

}
```
and 
```java
package com.alix.request;

public class AuthorInfo {

	private String author;

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

}
```

It'll generate swift files for both value objects.

All primitive classes, user defined classes and collection classes are supported(except queues), here is another example you can use to get a better overview

```java
package com.alix.request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.github.pakistancan.codable.annotation.Codable;

@Codable
public class Sample {

	@JsonProperty("sample_id")
	public int id;

	@JsonProperty("sample_list")
	public List<String> sampleList;

	@JsonProperty("sample_list1")
	public ArrayList<String> sampleList1;

	@JsonProperty("sample_set")
	public Set<String> sampleSet;

	@JsonProperty("sample_list_set")
	public Set<List<String>> sampleListSet;

	public HashSet<String> sampleHashSet;

	public TreeSet<String> sampleTreeSet;

	public LinkedHashSet<String> linkedHashSet;

	public HashMap<String, String> sampleHashMap;

	public TreeMap<String, String> sampleTreeMap;

	public LinkedHashMap<String, Set<String>> linkedHashMap;

	public LinkedHashMap<String, TreeSet<String>> linkedHashMapSet;

	public String[] inputs;

	public String[][] inputsMap;

}
```

## Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.

## License
[MIT](https://github.com/pakistancan/java-codable/blob/main/LICENSE)
