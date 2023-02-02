/**
 *
 */
package io.github.pakistancan.codable.processor;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.pakistancan.codable.annotation.Codable;
import io.github.pakistancan.codable.annotation.IgnoreProperty;
import io.github.pakistancan.codable.logging.LogFactory;
import io.github.pakistancan.codable.logging.Logger;
import io.github.pakistancan.codable.model.ObjectType;
import io.github.pakistancan.codable.model.TypeInfo;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic.Kind;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author muhammadali
 *
 */
@SupportedAnnotationTypes("io.github.pakistancan.codable.annotation.Codable")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedOptions(value = {"OutputDir", "PackagePrefix", "ClassModifier", "GenerateStructs"})
public class CodableGenerator extends AbstractProcessor {

    private static final String EXT = ".swift";

    private static final Set<String> reserveWords = new HashSet<>();
    private static final Set<String> restrictedWords = new HashSet<>();

    private static final String[] collectionsInterfaces = new String[]{Set.class.getCanonicalName(),
            List.class.getCanonicalName(), Map.class.getCanonicalName()};
    private static final Set<String> allowedModifiers = new HashSet<>();

    static {

        reserveWords.add("operator");
        reserveWords.add("func");
        restrictedWords.add("self");
    }

    static {
        allowedModifiers.add("public");
        allowedModifiers.add("open");
    }

    private final ConcurrentMap<String, TypeMirror> collectionTypeMirror = new ConcurrentHashMap<>();
    private final Map<String, StringBuilder> generatedEnums = new HashMap<>();
    private String outputDir = "./generated/";
    private String packagePrefix = "com.alix.";
    private String classModifier = "public";

	private boolean generateStructs = false;
    private boolean generateFormatters = false;
    private Logger logger;
    // private String string;

    /**
     *
     */
    public CodableGenerator() {
    }

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.logger = LogFactory.getInstance().getLogger();

        Map<String, String> options = this.processingEnv.getOptions();
        this.logger.info("Options :: " + options);
        String outDir = options.get("OutputDir");
        if (outDir != null) {
            this.outputDir = outDir;
        }
        if (!this.outputDir.endsWith("/")) {
            this.outputDir += "/";
        }
        if (!this.outputDir.endsWith("swift/")) {
            this.outputDir += "swift/";
        }
        String prefix = options.get("PackagePrefix");
        if (prefix != null) {
            this.packagePrefix = prefix;
        }
        String modifier = options.get("ClassModifier");
        if (modifier != null && allowedModifiers.contains(modifier)) {
            this.classModifier = modifier;
        }

		String generateStruct = options.get("GenerateStructs");
		if (generateStruct != null && generateStruct.equals("true")){
			this.generateStructs = true;
            this.classModifier = "public";
        }

        for (String ifaceName : collectionsInterfaces) {
            this.logger.info("Interface Name" + ifaceName);
            collectionTypeMirror.put(ifaceName,
                    this.processingEnv.getElementUtils().getTypeElement(ifaceName).asType());
        }

    }

    // It'll be better if we use templating to generate swift classes instead of
    // string concatenation
    private void generateCodable(TypeElement element, RoundEnvironment roundEnv) {

        TypeMirror parent = element.getSuperclass();
        this.logger.info("parent :: " + parent);
        String parentClass = "";
        if (!"java.lang.Object".equals(parent.toString())) {
            parentClass = parent.toString();
            if (parentClass.contains(".")) {
                parentClass = parentClass.substring(parentClass.lastIndexOf(".") + 1);
            }
        }

        StringBuilder builder = new StringBuilder();
        this.logger.info("Encloding Elems :: " + element.getEnclosedElements());

        builder.append("import Foundation\n\n");
		String swiftTypeName = " class ";
        logger.info("this.generateStructs: " + this.generateStructs+" parentClass.length(): "+parentClass.length());

		if (this.generateStructs && parentClass.length() == 0) {
			swiftTypeName = " struct ";
		}
        builder.append(classModifier).append(swiftTypeName).append(element.getSimpleName());
        boolean override = false;

        if (parentClass.length() > 0) {
            builder.append(": ").append(parentClass).append(" {\n");
			builder.append("    public override init() {\n        super.init()\n    }\n");
            override = true;
        } else {
            builder.append(": Codable {\n");
			if (!this.generateStructs) {
				builder.append("    public init() {\n    }\n");
			}
        }

        List<? extends Element> tElemns = processingEnv.getElementUtils().getAllMembers(element);
        tElemns = ElementFilter.fieldsIn(tElemns);
        Map<String, List<String>> enumMap = new HashMap<>();
        Map<String, String> jsonMap = new HashMap<>();

        StringBuilder encoder = new StringBuilder();
        encoder.append("\n    public ");
        if (override) {
            encoder.append("override ");
        }
        encoder.append("func encode(to encoder: Encoder) throws {\n");

        StringBuilder decoder = new StringBuilder();
        if (this.generateStructs) {
            decoder.append("\n    public init(from decoder: Decoder) throws {\n");
        } else {
            decoder.append("\n    public required init(from decoder: Decoder) throws {\n");
        }
        if (parentClass.length() > 0) {
            decoder.append("        try super.init(from: decoder)\n");
            encoder.append("        try super.encode(to: encoder)\n");

        }
        boolean addedContainer = false;

        for (Element field : tElemns) {
            VariableElement var = (VariableElement) field;
            System.err.println("Var :: " + var);
            if (var.getModifiers().contains(Modifier.STATIC)) {
                this.logger.info("continuing ");
                continue;
            }
            JsonIgnoreProperties jsonIgnoreProp = var.getAnnotation(JsonIgnoreProperties.class);
            IgnoreProperty ignoreProp = var.getAnnotation(IgnoreProperty.class);
            if (ignoreProp != null || jsonIgnoreProp != null) {
                this.logger.info("ignore property annotation found");
                continue;
            }

            if (!var.getEnclosingElement().getSimpleName().equals(element.getSimpleName())) {
                this.logger.info(" super class  continuing ");
                continue;
            }

            this.logger.info("Field :: " + var.getSimpleName() + " " + field.asType().toString() + " :: "
                    + field.asType().getClass().getCanonicalName());
            String type = field.asType().toString();

            TypeInfo newType = getTypeInfo(type, field, enumMap, roundEnv);

            if (newType == null) {
                newType = getTypeInfo(type);
            }
            String varName = field.getSimpleName().toString();
            if (Character.isUpperCase(varName.charAt(0))) {
                varName = Character.toLowerCase(varName.charAt(0)) + varName.substring(1);
            }
            if (restrictedWords.contains(varName)) {
                throw new RuntimeException("variable name not allowed" + varName);
            }
            if (reserveWords.contains(varName)) {
                varName = "`" + varName + "`";
            }
            JsonProperty prop = var.getAnnotation(JsonProperty.class);
            if (null != prop) {
                jsonMap.put(varName, prop.value());
                this.logger.info("Json Map :: " + prop);
            } else {
                jsonMap.put(varName, varName);
            }

            if (!addedContainer) {
                decoder.append("        let container = try decoder.container(keyedBy: CodingKeys.self)\n");
                encoder.append("        var container = encoder.container(keyedBy: CodingKeys.self)\n");
                addedContainer = true;
            }

            builder.append("    public var ").append(varName).append(": ").append(newType.swiftName);
            String typeToMarshal = newType.swiftName;
            if (newType.defaultValue != null) {
                builder.append(" = ").append(newType.defaultValue);
            } else {
                builder.append("?");
                typeToMarshal = typeToMarshal + "?";
            }

            String assignedValue = varName;
            String encodedValueName = "self." + varName;

            if (newType.type == ObjectType.ENUM) {
                assignedValue = newType.swiftName + "(rawValue: " + varName + ")!";
                encodedValueName = "self." + varName + ".rawValue";
                typeToMarshal = "String";
            } else if (newType.type == ObjectType.DATE) {
                generateFormatters = true;
                JsonFormat dateFormat = var.getAnnotation(JsonFormat.class);
                String pattern = "yyyy-MM-dd'T'HH:mm:ssZ";

                if (dateFormat != null) {
                    pattern = dateFormat.pattern();
                }

                if (!"".equals(pattern)) {
                    typeToMarshal = "String";

                    assignedValue = "Formatters.shared.getDate(format: \"" + pattern + "\", date: " + varName + ")";
                    encodedValueName = "Formatters.shared.formatDate(format: \"" + pattern + "\", date: " + "self."
                            + varName + ")";
                }

            }

            builder.append("\n");

            decoder.append("\n        if let ").append(varName).append(" = try container.decodeIfPresent(").
                    append(typeToMarshal).append(".self, forKey: .").append(varName).append(") {\n");

            decoder.append("            self.").append(varName).append(" = ").append(assignedValue);
            decoder.append("\n        }\n");

            encoder.append(
                    "        try container.encodeIfPresent(").append(encodedValueName).
                    append(", forKey: .").append(varName).append(")\n");

        }
        decoder.append("    }\n");
        encoder.append("    }\n");

        if (jsonMap.size() > 0) {
            builder.append("    private enum CodingKeys: String, CodingKey {\n");
            for (Map.Entry<String, String> elem : jsonMap.entrySet()) {
                builder.append("        case ").append(elem.getKey()).append(" = \"").append(elem.getValue()).append("\"\n");
            }
            builder.append("\n    }");
        }
        builder.append(decoder);
        builder.append(encoder);
        builder.append("\n}");

        String prefix = "RES/";
        if (element.getQualifiedName().toString().contains(".request.")) {
            prefix = "REQ/";
        }

        this.writeOutput(builder.toString(), outputDir + prefix, element.getSimpleName() + EXT);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element elem : roundEnv.getElementsAnnotatedWith(Codable.class)) {
            Codable complexity = elem.getAnnotation(Codable.class);
            String message = "annotation found in " + elem.getSimpleName() + " with complexity " + complexity;

            TypeElement element = (TypeElement) elem;
            generateCodable(element, roundEnv);
            processingEnv.getMessager().printMessage(Kind.NOTE, message);
        }

        if (generatedEnums.size() > 0) {
            String prefix = "Enums/";
            for (Map.Entry<String, StringBuilder> elem : generatedEnums.entrySet()) {
                this.writeOutput(elem.getValue().toString(), outputDir + prefix, elem.getKey() + EXT);
            }

        }

        if (this.generateFormatters) {
            this.generateFormatter();
        }

        return false;
    }

    public synchronized TypeInfo getTypeInfo(String type, Element field, Map<String, List<String>> enumMap,
                                RoundEnvironment roundEnv) {
        TypeInfo newType = null;

        this.logger.info("PARSING: " + type);

        if (type.endsWith("[]")) {
            String tp = type.substring(0, type.lastIndexOf("[]"));
            TypeInfo typeInfo = getTypeInfo(tp, field.getEnclosingElement(), enumMap, roundEnv);
            if (typeInfo != null) {
                newType = new TypeInfo("[" + typeInfo.swiftName + "]", "[]");
            } else {
                newType = new TypeInfo("[Any]", "[]");
            }
            return newType;
        }

        if (type.startsWith(Map.class.getCanonicalName())) {
            String tp = type.substring(type.indexOf("<") + 1);
            tp = tp.substring(0, tp.lastIndexOf(">"));

            String first = tp.substring(0, tp.lastIndexOf(",")).trim();
            tp = tp.substring(tp.lastIndexOf(",") + 1);
            tp = tp.trim();
            TypeInfo firstType = getTypeInfo(first, field.getEnclosingElement(), enumMap, roundEnv);
            TypeInfo secondType = getTypeInfo(tp, field.getEnclosingElement(), enumMap, roundEnv);

            if (firstType != null && secondType != null) {
                newType = new TypeInfo("[" + firstType.swiftName + ":" + secondType.swiftName + "]", "[:]");
            } else {
                newType = new TypeInfo("[AnyHash: Any]", "[:]");
            }
            return newType;
        }

        boolean isSet = type.startsWith(Set.class.getCanonicalName());
        boolean isList = type.startsWith(List.class.getCanonicalName());

        if (isSet || isList) {
            String tp = type.substring(type.indexOf("<") + 1);
            tp = tp.substring(0, tp.lastIndexOf(">"));

            TypeInfo typeInfo = getTypeInfo(tp, field.getEnclosingElement(), enumMap, roundEnv);
            if (typeInfo != null) {
                newType = new TypeInfo("[" + typeInfo.swiftName + "]", "[]");
            } else {
                newType = new TypeInfo("[Any]", "[]");
            }
            return newType;
        }

        if (type.startsWith(packagePrefix)) {

            TypeElement newTypeElem = processingEnv.getElementUtils().getTypeElement(field.asType().toString());
            System.err.println("Field::" + field.asType() + " TypeElement:: " + newTypeElem);

            System.err.println("newType :: " + newTypeElem.getKind());
            if (newTypeElem.getKind() != ElementKind.ENUM) {
                newTypeElem = processingEnv.getElementUtils().getTypeElement(type);
                generateCodable(newTypeElem, roundEnv);

                type = type.substring(type.lastIndexOf(".") + 1);
                newType = new TypeInfo(type, null);
            } else {
                List<? extends Element> elems = newTypeElem.getEnclosedElements();

                boolean isGenerated = generatedEnums.containsKey(newTypeElem.getSimpleName().toString());

                StringBuilder sb = new StringBuilder();
                if (!isGenerated) {
                    sb.append("\n").append(classModifier).append(" enum ").append(newTypeElem.getSimpleName()).append(": String, Codable {\n");
                }
                List<String> enumElems = new ArrayList<>();
                for (Element elm : elems) {
                    if (elm.getKind() != ElementKind.ENUM_CONSTANT) {
                        continue;
                    }
                    if (!isGenerated) {
                        sb.append("    case ").append(elm.getSimpleName()).append(" = \"").append(elm.getSimpleName()).append("\"\n");
                    }
                    enumElems.add(elm.getSimpleName().toString());
                }
                if (!isGenerated) {
                    sb.append("\n}\n");
                    generatedEnums.put(newTypeElem.getSimpleName().toString(), sb);

                }

                enumMap.put(field.getSimpleName().toString(), enumElems);
                type = type.substring(type.lastIndexOf(".") + 1);
                if (enumElems.size() > 0) {
                    newType = new TypeInfo(type, "." + enumElems.get(0), ObjectType.ENUM);
                } else {
                    newType = new TypeInfo(type, null, ObjectType.ENUM);
                }

            }
        } else {
            // Handle collection child here
            if (type.contains("<")) {

                String tp = type.substring(0, type.indexOf("<"));
                this.logger.info("type: " + tp);
                TypeElement tm = processingEnv.getElementUtils().getTypeElement(tp);

//                TypeElement tm = mirror;
                do {
                    this.logger.info("class:: " + tm);
                    for (TypeMirror iface : tm.getInterfaces()) {
                        for (Map.Entry<String, TypeMirror> infaceName : this.collectionTypeMirror.entrySet()) {
                            TypeMirror ifaceMirror = infaceName.getValue();
                            this.logger.info("iface:: " + iface + " ifaceMirror:: " + ifaceMirror);
                            this.logger.info("Assignable01:: "
                                    + processingEnv.getTypeUtils().isAssignable(tm.asType(), ifaceMirror));
                            this.logger.info("Assignable02:: "
                                    + processingEnv.getTypeUtils().isAssignable(ifaceMirror, tm.asType()));

                            this.logger.info(
                                    "Assignable1:: " + processingEnv.getTypeUtils().isAssignable(iface, ifaceMirror));
                            this.logger.info(
                                    "Assignable2:: " + processingEnv.getTypeUtils().isAssignable(ifaceMirror, iface));
                            if (processingEnv.getTypeUtils().isAssignable(ifaceMirror, iface)
                                    || iface.toString().contains(ifaceMirror.toString())) {
                                String newTypeName = type.replace(tp, infaceName.getKey());
                                return getTypeInfo(newTypeName, field, enumMap, roundEnv);

                            }
                        }

                    }

                    String superClassName = tm.getSuperclass().toString();
                    this.logger.info("superClassName:: " + superClassName);
                    if (superClassName.equals(Object.class.getCanonicalName())) {
                        break;
                    }
                    if (superClassName.contains("<")) {
                        superClassName = superClassName.substring(0, superClassName.indexOf("<"));
                    }

                    this.logger.info("superClassName:: " + superClassName);
                    tm = processingEnv.getElementUtils().getTypeElement(superClassName);
                    this.logger.info("superClassName:: " + tm + "ifaces" + tm.getInterfaces());

                } while (tm != null);

            }

        }

        if (null == newType) {
            return getTypeInfo(type);
        }
        return newType;
    }

    public TypeInfo getTypeInfo(String tp) {
        TypeInfo info = TypeInfo.getTypeInfo(tp);
        if (info != null) {
            return info;
        }

        if (tp.startsWith(packagePrefix)) {
            tp = tp.substring(tp.lastIndexOf(".") + 1);
            return new TypeInfo(tp, null);
        }
        throw new RuntimeException("Unknown type " + tp);
    }

    public void generateFormatter() {

        String prefix = "Formatter/";
        String key = "Formatters";

        String formatterClass = "import Foundation\n" + "\n" + classModifier + " class Formatters {\n"
                + "    private var formatters: [String: DateFormatter] = [:]\n"
                + "    public static let shared: Formatters = Formatters()\n" + "\n"
                + "    public func getFormatter(format: String) -> DateFormatter {\n"
                + "        if let formatter = formatters[format] {\n" + "            return formatter\n"
                + "        } else {\n" + "            let formatter = DateFormatter()\n"
                + "            formatter.dateFormat = format\n" + "            formatters[format] = formatter\n"
                + "            return formatter\n" + "        }\n" + "    }\n" + "    \n"
                + "    public func formatDate(format: String, date: Date) -> String {\n"
                + "        return self.getFormatter(format: format).string(from: date)\n" + "    }\n" + "    \n"
                + "    public func getDate(format: String, date: String) -> Date {\n"
                + "        return self.getFormatter(format: format).date(from: date) ?? Date(timeIntervalSince1970: 0)\n"
                + "    }\n" + "    \n" + "    public func getDate(interval: Int64) -> Date {\n"
                + "        return Date(timeIntervalSince1970: (Double(interval)/1000.0))\n" + "    }\n"
                + "    public func getMilliSeconds(date: Date) -> Int64 {\n"
                + "        return Int64(date.timeIntervalSince1970 * 1000)\n" + "    }\n" + "}\n" + "";

        this.writeOutput(formatterClass, outputDir + prefix, key + EXT);

    }

    public void writeOutput(String output, String directory, String filename) {
        File file = new File(directory);
        if (!file.exists()) {
            file.mkdirs();
        }
        String newPath = directory + filename;
        if (directory.endsWith(File.separator)) {
            newPath = directory + File.separator + filename;
        }

        try (FileOutputStream fout = new FileOutputStream(newPath)) {
            fout.write(output.getBytes(UTF_8));
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

    }

}
