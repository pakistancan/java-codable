package io.github.pakistancan.codable.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TypeInfo {

	private static final Map<String, TypeInfo> mapping = new HashMap<String, TypeInfo>();

	static {
		mapping.put("byte", new TypeInfo("Int8", "0"));
		mapping.put(Byte.class.getCanonicalName(), new TypeInfo("Int8", "0"));
		mapping.put(String.class.getCanonicalName(), new TypeInfo("String", "\"\""));
		mapping.put("int", new TypeInfo("Int", "0"));
		mapping.put(Integer.class.getCanonicalName(), new TypeInfo("Int", "0"));
		mapping.put(Number.class.getCanonicalName(), new TypeInfo("Int", "0"));
		mapping.put("short", new TypeInfo("Int16", "0"));
		mapping.put(Short.class.getCanonicalName(), new TypeInfo("Int16", "0"));
		mapping.put("float", new TypeInfo("Float", "0.0"));
		mapping.put(Float.class.getCanonicalName(), new TypeInfo("Float", "0.0"));
		mapping.put("double", new TypeInfo("Double", "0.0"));
		mapping.put(Double.class.getCanonicalName(), new TypeInfo("Double", "0.0"));
		mapping.put("boolean", new TypeInfo("Bool", "false"));
		mapping.put(Boolean.class.getCanonicalName(), new TypeInfo("Bool", "false"));
		mapping.put(Date.class.getCanonicalName(),
				new TypeInfo("Date", "Date(timeIntervalSince1970: 0)", ObjectType.DATE));
		mapping.put("long", new TypeInfo("Int64", "0"));
		mapping.put(Long.class.getCanonicalName(), new TypeInfo("Int64", "0"));
		mapping.put(Object.class.getCanonicalName(), new TypeInfo("Any", null));
		mapping.put("?", new TypeInfo("Any", null));
		mapping.put("char", new TypeInfo("Int16", null));
		mapping.put(Character.class.getCanonicalName(), new TypeInfo("Int16", null));

	}

	public String defaultValue;
	public String swiftName;
	public ObjectType type;

	/**
	 * @param defaultValue
	 * @param swiftName
	 */
	public TypeInfo(String swiftName, String defaultValue) {
		this(swiftName, defaultValue, ObjectType.SIMPLE);
	}

	public TypeInfo(String swiftName, String defaultValue, ObjectType type) {
		super();
		this.defaultValue = defaultValue;
		this.swiftName = swiftName;
		this.type = type;
	}

	public static TypeInfo getTypeInfo(String name) {
		if (mapping.containsKey(name)) {
			return mapping.get(name);
		}
		return null;
	}

}
