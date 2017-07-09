package com.sgrvg.security.util;

import java.net.URL;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Strings;

/**
 * Util taken from https://stackoverflow.com/questions/13592236/parse-a-uri-string-into-name-value-collection
 * 
 * @author pabloc
 *
 */
public final class URLUtil {

	private URLUtil() {
		super();
	}
	
	/**
	 * Get distinct query parameter names
	 * 
	 * @param url the url to parse
	 * @return 
	 */
	public static List<String> getQueryParameterNames(URL url) {
		if (Strings.isNullOrEmpty(url.getQuery())) {
			return Collections.emptyList();
		}
		return Arrays.stream(url.getQuery().split("&"))
				.map(URLUtil::splitQueryParameter)
				.map(Map.Entry::getKey)
				.distinct()
				.collect(Collectors.toList());
	}
	
	public static Map<String, List<String>> splitQuery(URL url) {
	    if (Strings.isNullOrEmpty(url.getQuery())) {
	        return Collections.emptyMap();
	    }
	    return Arrays.stream(url.getQuery().split("&"))
	            .map(URLUtil::splitQueryParameter)
	            .collect(Collectors.groupingBy(SimpleImmutableEntry::getKey, LinkedHashMap::new, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
	}

	public static SimpleImmutableEntry<String, String> splitQueryParameter(String it) {
	    final int idx = it.indexOf("=");
	    final String key = idx > 0 ? it.substring(0, idx) : it;
	    final String value = idx > 0 && it.length() > idx + 1 ? it.substring(idx + 1) : null;
	    return new SimpleImmutableEntry<>(key, value);
	}
}
