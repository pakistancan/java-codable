[![Java CI with Maven](https://github.com/pakistancan/java-codable/actions/workflows/maven.yml/badge.svg)](https://github.com/pakistancan/java-codable/actions/workflows/maven.yml) [![CircleCI](https://dl.circleci.com/status-badge/img/gh/pakistancan/java-codable/tree/main.svg?style=svg)](https://dl.circleci.com/status-badge/redirect/gh/pakistancan/java-codable/tree/main)

# java-codable
java-codable is a Java beans to swift codable converter, it is java compile plugin, you need to add this your maven configuration to get it working.

## Configuration

Add codeable to project dependencies

```xml
		<dependency>
			<groupId>io.github.pakistancan</groupId>
			<artifactId>codable-converter</artifactId>
			<version>1.0.2</version>
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
						<AClassModifier>open</AClassModifier>
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

If you are using gradle, it should be as simple as adding anootation processor to dependencies section of your build.gradle file

```gradle
	annotationProcessor 'io.github.pakistancan:codable-converter:1.0.3'
```

compiler arguments can be configured using

```gradle
	compileJava {
		options.compilerArgs += '-AClassModifier=public'
	}
```

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

Output files for the above classes looks like
`BookInfo.swift`

```swift
import Foundation

public class BookInfo: Codable {
    public init() {
    }
    public var title: String = ""
    public var isbn: String = ""
    public var authors: [AuthorInfo] = []
    private enum CodingKeys: String, CodingKey {
        case isbn = "isbn"
        case title = "title"
        case authors = "authors"

    }
    public required init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)

        if let title = try container.decodeIfPresent(String.self, forKey: .title) {
            self.title = title
        }

        if let isbn = try container.decodeIfPresent(String.self, forKey: .isbn) {
            self.isbn = isbn
        }

        if let authors = try container.decodeIfPresent([AuthorInfo].self, forKey: .authors) {
            self.authors = authors
        }
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encodeIfPresent(self.title, forKey: .title)
        try container.encodeIfPresent(self.isbn, forKey: .isbn)
        try container.encodeIfPresent(self.authors, forKey: .authors)
    }

}
```

`AuthorInfo.swift`
```swift
import Foundation

public class AuthorInfo: Codable {
    public init() {
    }
    public var author: String = ""
    private enum CodingKeys: String, CodingKey {
        case author = "author"

    }
    public required init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)

        if let author = try container.decodeIfPresent(String.self, forKey: .author) {
            self.author = author
        }
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encodeIfPresent(self.author, forKey: .author)
    }

}
```

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
Output of above class should be something like this

`Sample.swift`
```swift
import Foundation

public class Sample: Codable {
    public init() {
    }
    public var id: Int = 0
    public var sampleList: [String] = []
    public var sampleList1: [String] = []
    public var sampleSet: [String] = []
    public var sampleListSet: [[String]] = []
    public var sampleHashSet: [String] = []
    public var sampleTreeSet: [String] = []
    public var linkedHashSet: [String] = []
    public var sampleHashMap: [String:String] = [:]
    public var sampleTreeMap: [String:String] = [:]
    public var linkedHashMap: [String:[String]] = [:]
    public var linkedHashMapSet: [String:[String]] = [:]
    public var inputs: [String] = []
    public var inputsMap: [[String]] = []
    private enum CodingKeys: String, CodingKey {
        case linkedHashSet = "linkedHashSet"
        case sampleTreeMap = "sampleTreeMap"
        case sampleHashSet = "sampleHashSet"
        case inputs = "inputs"
        case sampleListSet = "sample_list_set"
        case sampleHashMap = "sampleHashMap"
        case sampleList = "sample_list"
        case sampleSet = "sample_set"
        case linkedHashMap = "linkedHashMap"
        case sampleTreeSet = "sampleTreeSet"
        case linkedHashMapSet = "linkedHashMapSet"
        case sampleList1 = "sample_list1"
        case inputsMap = "inputsMap"
        case id = "sample_id"

    }
    public required init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)

        if let id = try container.decodeIfPresent(Int.self, forKey: .id) {
            self.id = id
        }

        if let sampleList = try container.decodeIfPresent([String].self, forKey: .sampleList) {
            self.sampleList = sampleList
        }

        if let sampleList1 = try container.decodeIfPresent([String].self, forKey: .sampleList1) {
            self.sampleList1 = sampleList1
        }

        if let sampleSet = try container.decodeIfPresent([String].self, forKey: .sampleSet) {
            self.sampleSet = sampleSet
        }

        if let sampleListSet = try container.decodeIfPresent([[String]].self, forKey: .sampleListSet) {
            self.sampleListSet = sampleListSet
        }

        if let sampleHashSet = try container.decodeIfPresent([String].self, forKey: .sampleHashSet) {
            self.sampleHashSet = sampleHashSet
        }

        if let sampleTreeSet = try container.decodeIfPresent([String].self, forKey: .sampleTreeSet) {
            self.sampleTreeSet = sampleTreeSet
        }

        if let linkedHashSet = try container.decodeIfPresent([String].self, forKey: .linkedHashSet) {
            self.linkedHashSet = linkedHashSet
        }

        if let sampleHashMap = try container.decodeIfPresent([String:String].self, forKey: .sampleHashMap) {
            self.sampleHashMap = sampleHashMap
        }

        if let sampleTreeMap = try container.decodeIfPresent([String:String].self, forKey: .sampleTreeMap) {
            self.sampleTreeMap = sampleTreeMap
        }

        if let linkedHashMap = try container.decodeIfPresent([String:[String]].self, forKey: .linkedHashMap) {
            self.linkedHashMap = linkedHashMap
        }

        if let linkedHashMapSet = try container.decodeIfPresent([String:[String]].self, forKey: .linkedHashMapSet) {
            self.linkedHashMapSet = linkedHashMapSet
        }

        if let inputs = try container.decodeIfPresent([String].self, forKey: .inputs) {
            self.inputs = inputs
        }

        if let inputsMap = try container.decodeIfPresent([[String]].self, forKey: .inputsMap) {
            self.inputsMap = inputsMap
        }
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encodeIfPresent(self.id, forKey: .id)
        try container.encodeIfPresent(self.sampleList, forKey: .sampleList)
        try container.encodeIfPresent(self.sampleList1, forKey: .sampleList1)
        try container.encodeIfPresent(self.sampleSet, forKey: .sampleSet)
        try container.encodeIfPresent(self.sampleListSet, forKey: .sampleListSet)
        try container.encodeIfPresent(self.sampleHashSet, forKey: .sampleHashSet)
        try container.encodeIfPresent(self.sampleTreeSet, forKey: .sampleTreeSet)
        try container.encodeIfPresent(self.linkedHashSet, forKey: .linkedHashSet)
        try container.encodeIfPresent(self.sampleHashMap, forKey: .sampleHashMap)
        try container.encodeIfPresent(self.sampleTreeMap, forKey: .sampleTreeMap)
        try container.encodeIfPresent(self.linkedHashMap, forKey: .linkedHashMap)
        try container.encodeIfPresent(self.linkedHashMapSet, forKey: .linkedHashMapSet)
        try container.encodeIfPresent(self.inputs, forKey: .inputs)
        try container.encodeIfPresent(self.inputsMap, forKey: .inputsMap)
    }

}
```

## Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.
Please have a look at [CONTRIBUTING](CONTRIBUTING.md)

## License
[MIT](https://github.com/pakistancan/java-codable/blob/main/LICENSE)
