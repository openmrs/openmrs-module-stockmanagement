package org.openmrs.module.stockmanagement.api.utils;

import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.OpenmrsMetadata;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Used to generate simplified representations of data or metadata, making it easier to serialize to
 * json
 */
public class SimpleObject extends LinkedHashMap<String, Object> {
	
	private static final long serialVersionUID = 1L;
	
	public SimpleObject() {
		super();
	}
	
	/**
	 * Convenience constructor for creating a {@link SimpleObject} representing
	 * {@link OpenmrsMetadata}, which will set 'id' and 'label' properties
	 * 
	 * @param metadata
	 */
	public SimpleObject(OpenmrsMetadata metadata) {
		super();
		put("id", metadata.getId());
		put("label", metadata.getName());
	}
	
	/**
	 * Utility method to create a {@link SimpleObject} given a varargs style list of property names
	 * and values. The array passed in must have even length. Every other element (starting from the
	 * 0-index one) must be a String (representing a property name) and be followed by its value.
	 * 
	 * @param propertyNamesAndValues
	 * @return
	 */
	public static SimpleObject create(Object... propertyNamesAndValues) {
		SimpleObject ret = new SimpleObject();
		for (int i = 0; i < propertyNamesAndValues.length; i += 2) {
			String prop = (String) propertyNamesAndValues[i];
			ret.put(prop, propertyNamesAndValues[i + 1]);
		}
		return ret;
	}
	
	private static Map<String, Set<String>> splitIntoLevels(String[] propertiesToInclude) {
		Map<String, Set<String>> ret = new LinkedHashMap<String, Set<String>>();
		for (String prop : propertiesToInclude) {
			String[] components = prop.split("\\.");
			for (int i = 0; i < components.length; ++i) {
				splitIntoLevelsHelper(ret, Arrays.asList(components), i);
			}
		}
		return ret;
	}
	
	private static void splitIntoLevelsHelper(Map<String, Set<String>> ret, List<String> components, int index) {
		String level = OpenmrsUtil.join(components.subList(0, index), ".");
		Set<String> atLevel = ret.get(level);
		if (atLevel == null) {
			atLevel = new LinkedHashSet<String>();
			ret.put(level, atLevel);
		}
		atLevel.add(components.get(index));
	}
	
	public String toJson() {
		try {
			ObjectMapper mapper = new ObjectMapper();
			StringWriter sw = new StringWriter();
			mapper.writeValue(sw, this);
			return sw.toString();
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		return ("");
	}
	
}
