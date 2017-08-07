package com.puppet.pcore.impl;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.Collections.*;

public class Helpers {
	private static final Pattern CLASS_NAME = Pattern.compile("^[A-Z]\\w*(?:::[A-Z]\\w*)*$");
	private static final Pattern COLON_SPLIT = Pattern.compile("::");

	public static class MapEntry<K, V> implements Entry<K, V> {
		public final K key;
		public final V value;

		public MapEntry(K key, V value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return value;
		}

		@Override
		public V setValue(V value) {
			throw new UnsupportedOperationException();
		}
	}

	@SuppressWarnings("unchecked")
	public static <K, V> Map<K,V> asMap(Object... keyValuePairs) {
		int len = keyValuePairs.length;
		if(len == 0)
			return Collections.emptyMap();
		if(len == 2)
			return Collections.singletonMap((K)keyValuePairs[0], (V)keyValuePairs[1]);

		if((len % 2) != 0)
			throw new IllegalArgumentException("asMap called with uneven number of arguments");

		Map<K, V> map = new LinkedHashMap<>();
		for(int idx = 0; idx < len; idx += 2)
			map.put((K)keyValuePairs[idx], (V)keyValuePairs[idx + 1]);
		return map;
	}

	public static <K, V> MapEntry<K, V> entry(K key, V value) {
		return new MapEntry<>(key, value);
	}

	@SuppressWarnings("unchecked")
	public static <K, V> Map<K,V> asMap(Entry<K, V>... entries) {
		if(entries.length == 0)
			return Collections.emptyMap();
		if(entries.length == 1)
			return Collections.singletonMap(entries[0].getKey(), entries[0].getValue());

		Map<K, V> map = new LinkedHashMap<>();
		for(Entry<K, V> entry : entries)
			map.put(entry.getKey(), entry.getValue());
		return map;
	}

	@SuppressWarnings("unchecked")
	public static <K, V> Map<K,V> asMap(List<?> keyValuePairs) {
		int len = keyValuePairs.size();
		if(len == 0)
			return Collections.emptyMap();

		Map<K, V> map = new LinkedHashMap<>();
		for(int idx = 0; idx < len; idx += 2)
			map.put((K)keyValuePairs.get(idx), (V)keyValuePairs.get(idx + 1));
		return map;
	}

	@SuppressWarnings("unchecked")
	public static <T> List<T> asList(T... values) {
		return values.length == 0 ? Collections.emptyList() : unmodifiableList(Arrays.asList(values));
	}

	@SuppressWarnings("unchecked")
	public static <T> List<T> asWrappingList(T[] values) {
		return values.length == 0 ? Collections.emptyList()  : Arrays.asList(values);
	}

	@SuppressWarnings("unchecked")
	public static <T> List<T> unmodifiableCopy(Collection<? extends T> collection) {
		return collection.isEmpty() ? Collections.emptyList() : unmodifiableList(new ArrayList<>(collection));
	}

	@SuppressWarnings("unchecked")
	public static <K, V> Map<K, V> unmodifiableCopy(Map<K, V> map) {
		return map.isEmpty() ? Collections.emptyMap() : unmodifiableMap(new LinkedHashMap<>(map));
	}

	public static List<?> mapAsPairs(Map<?, ?> map) {
		return map(map.entrySet(), (e) -> asList(e.getKey(), e.getValue()));
	}

	public static <K, V> Map<K, V> merge(Map<K, V> ...maps) {
		Map<K,V> result = new LinkedHashMap<>();
		for(Map<K, V> map : maps)
			result.putAll(map);
		return result;
	}

	public static Map<?,?> pairsAsMap(List<List<?>> pairs) {
		Map<Object, Object> result = new LinkedHashMap<>(pairs.size());
		for(List<?> pair : pairs) {
			if(pair.size() != 2)
				throw new IllegalArgumentException("must be a list of two element lists");
			result.put(pair.get(0), pair.get(1));
		}
		return unmodifiableMap(result);
	}

	public static String capitalizeSegment(String segment) {
		int len = segment.length();
		switch(len) {
		case 0:
			return segment;
		case 1:
			return segment.toUpperCase();
		default:
			StringBuilder bld = new StringBuilder();
			bld.append(Character.toUpperCase(segment.charAt(0)));
			for(int idx = 1; idx < len; ++idx)
				bld.append(Character.toLowerCase(segment.charAt(idx)));
			return bld.toString();
		}
	}

	public static String capitalizeSegments(String typeName) {
		String[] segments = COLON_SPLIT.split(typeName);
		int idx = segments.length;
		while(--idx >= 0)
			segments[idx] =  capitalizeSegment(segments[idx]);
		return join("::", Arrays.asList(segments));
	}

	public static String join(String delimiter, Iterable<? extends CharSequence> strings) {
		Iterator<? extends CharSequence> iter = strings.iterator();
		if(!iter.hasNext())
			return "";
		CharSequence first = iter.next();
		if(!iter.hasNext())
			return first.toString();
		StringBuilder bld = new StringBuilder();
		bld.append(first);
		do {
			bld.append(delimiter);
			bld.append(iter.next());
		} while(iter.hasNext());
		return bld.toString();
	}

	@SuppressWarnings("unchecked")
	public static <T> T getArgument(String key, Map<String,Object> args, T defaultValue) {
		return args.containsKey(key) ? (T)args.get(key) : defaultValue;
	}

	public static String joinName(String[] segments) {
		return joinName(segments, 0);
	}

	public static String joinName(String[] segments, int startAt) {
		int nsegs = segments.length - startAt;
		if(nsegs <= 0)
			return "";

		if(nsegs == 1)
			return segments[startAt];

		StringBuilder bld = new StringBuilder(segments[startAt]);
		for(int idx = startAt + 1; idx < segments.length; ++idx) {
			bld.append("::");
			bld.append(segments[idx]);
		}
		return bld.toString();
	}

	public static <T> List<T> makeUnique(List<? extends T> a) {
		return unmodifiableCopy(new LinkedHashSet<T>(a));
	}

	public static <T extends MergableRange<T>> List<T> mergeRanges(List<T> rangesToMerge) {
		switch(rangesToMerge.size()) {
		case 0:
		case 1:
			return rangesToMerge;
		default:
			List<T> result = new ArrayList<>();
			Stack<T> ranges = new Stack<>();
			ranges.addAll(rangesToMerge);
			while(!ranges.isEmpty()) {
				Stack<T> unmerged = new Stack<>();
				T x = ranges.pop();
				result.add(reduce(ranges, x, (memo, y) -> {
					T merged = memo.merge(y);
					if(merged == null)
						unmerged.add(y);
					else
						memo = merged;
					return memo;
				}));
				ranges = unmerged;
			}
			return unmodifiableCopy(result);
		}
	}

	public static <T> List<T> mergeUnique(Collection<? extends T> a, Collection<? extends T> b) {
		Set<T> uniqueSet = new LinkedHashSet<>(a);
		uniqueSet.addAll(b);
		return unmodifiableCopy(uniqueSet);
	}

	public static void puppetRegexp(String s, StringBuilder bld) {
		int top = s.length();
		bld.append('/');
		for(int idx = 0; idx < top; ++idx) {
			char c = s.charAt(idx);
			switch(c) {
			case '\t':
				bld.append("\\t");
				break;
			case '\n':
				bld.append("\\n");
				break;
			case '\r':
				bld.append("\\r");
				break;
			case '\\':
				bld.append("\\\\");
				break;
			case '/':
				bld.append("\\/");
				break;
			default:
				if(c < 0x20 || c > 0x7f)
					bld.append(format("\\u{%X}", (int)c));
				else
					bld.append(c);
			}
		}
		bld.append('/');
	}

	public static String[] splitName(String qname) {
		return COLON_SPLIT.split(qname);
	}

	public static <R> List<R> mapRange(int start, int end, Function<Integer, ? extends R> mapper) {
		@SuppressWarnings("unchecked") R[] result = (R[])new Object[end - start];
		for(int idx = start; idx < end; ++idx)
			result[idx - start] = mapper.apply(idx);
		return asList(result);
	}

	public static <T> boolean all(Collection<? extends T> collection, Predicate<? super T> condition) {
		for(T elem : collection)
			if(!condition.test(elem))
				return false;
		return true;
	}

	public static <K, V> Map<K, V> select(Map<K, V> map, Predicate<? super Entry<K, V>> condition) {
		if(map.isEmpty())
			return map;

		Map<K, V> result = new LinkedHashMap<>();
		for(Entry<K, V> entry : map.entrySet())
			if(condition.test(entry))
				result.put(entry.getKey(), entry.getValue());
		return result;
	}

	public static <T> List<T> select(Collection<? extends T> collection, Predicate<? super T> condition) {
		int top = collection.size();
		List<T> result = new ArrayList<>(top);
		for(T elem : collection)
			if(condition.test(elem))
				result.add(elem);
		return unmodifiableCopy(result);
	}

	public static <K, V> Map<K, V> reject(Map<K, V> map, Predicate<? super Entry<K, V>> condition) {
		if(map.isEmpty())
			return map;

		Map<K, V> result = new LinkedHashMap<>();
		for(Entry<K, V> entry : map.entrySet())
			if(!condition.test(entry))
				result.put(entry.getKey(), entry.getValue());
		return result;
	}

	public static <T> List<T> reject(Collection<? extends T> collection, Predicate<? super T> condition) {
		int top = collection.size();
		List<T> result = new ArrayList<>(top);
		for(T elem : collection)
			if(!condition.test(elem))
				result.add(elem);
		return unmodifiableCopy(result);
	}

	public static <T> int count(Collection<? extends T> collection, Predicate<? super T> condition) {
		int counter = 0;
		for(T elem : collection)
			if(condition.test(elem))
				++counter;
		return counter;
	}

	public static <T, R> List<R> map(Collection<? extends T> collection, Function<? super T, ? extends R> mapper) {
		int top = collection.size();
		@SuppressWarnings("unchecked") R[] result = (R[])new Object[top];
		int idx = 0;
		for(T elem : collection)
			result[idx++] = mapper.apply(elem);
		return asList(result);
	}

	public static <K, V, VR> Map<K, VR> mapValues(Map<K, V> collection, BiFunction<? super K, ? super V, ? extends VR> mapper) {
		int top = collection.size();
		if(top == 0)
			return emptyMap();
		Iterator<Entry<K, V>> iterator = collection.entrySet().iterator();
		if(top == 1) {
			Entry<K, V> entry = iterator.next();
			return singletonMap(entry.getKey(), mapper.apply(entry.getKey(), entry.getValue()));
		}
		LinkedHashMap<K, VR> result = new LinkedHashMap<>();
		while(iterator.hasNext()) {
			Entry<K, V> entry = iterator.next();
			result.put(entry.getKey(), mapper.apply(entry.getKey(), entry.getValue()));
		}
		return result;
	}

	public static <K, V, KR, VR> Map<KR, VR> map(Map<K, V> collection, BiFunction<? super K, ? super V, ? extends Entry<? extends KR, ? extends VR>> mapper) {
		int top = collection.size();
		if(top == 0)
			return emptyMap();
		Iterator<Entry<K, V>> iterator = collection.entrySet().iterator();
		if(top == 1) {
			Entry<K, V> entry = iterator.next();
			Entry<? extends KR, ? extends VR> result = mapper.apply(entry.getKey(), entry.getValue());
			return singletonMap(result.getKey(), result.getValue());
		}
		LinkedHashMap<KR, VR> result = new LinkedHashMap<>();
		while(iterator.hasNext()) {
			Entry<K, V> entry = iterator.next();
			Entry<? extends KR, ? extends VR> resultEntry = mapper.apply(entry.getKey(), entry.getValue());
			result.put(resultEntry.getKey(), resultEntry.getValue());
		}
		return result;
	}

	public static <T, R> R reduce(Collection<? extends T> collection, R identity, BiFunction<R, T, R> accumulator) {
		R result = identity;
		for(T elem : collection)
       result = accumulator.apply(result, elem);
     return result;
	}

	public static <T> boolean any(Collection<? extends T> collection, Predicate<? super T> condition) {
		for(T elem : collection)
			if(condition.test(elem))
				return true;
		return false;
	}

	public static <T> void eachWithIndex(List<T> list, BiConsumer<T, Integer> block) {
		int top = list.size();
		for(int idx = 0; idx < top; ++idx)
			block.accept(list.get(idx), idx);
	}

	public static <T,R> List<R> mapWithIndex(List<T> list, BiFunction<T, Integer, R> block) {
		int top = list.size();
		if(top == 0)
			return emptyList();
		if(top == 1)
			return singletonList(block.apply(list.get(0), 0));
		ArrayList<R> result = new ArrayList<>();
		for(int idx = 0; idx < top; ++idx)
			result.add(block.apply(list.get(idx), idx));
		return result;
	}

	@SuppressWarnings("unchecked")
	public static <T> List<T> concat(Collection<T> a, Collection<T> b) {
		Object[] all = new Object[a.size() + b.size()];
		int idx = 0;
		for(Object v : a)
			all[idx++] = v;
		for(Object v : b)
			all[idx++] = v;
		return (List<T>)Arrays.asList(all);
	}

	@SuppressWarnings("unchecked")
	public static <T> List<T> flatten(Collection<?> collection) {
		ArrayList<T> result = new ArrayList<>();
		for(Object elem : collection)
			if(elem instanceof Collection)
				result.addAll(flatten((Collection)elem));
			else
				result.add((T)elem);
		return unmodifiableList(result);
	}

	public static <T> List<T> distinct(Collection<? extends T> collection) {
		return unmodifiableCopy(new LinkedHashSet<>(collection));
	}

	@SuppressWarnings("unchecked")
	public static <T> List<T>[] partitionBy(Collection<T> collection, Predicate<? super T> condition) {
		List<T> trueList = new ArrayList<>();
		List<T> falseList = new ArrayList<>();
		for(T elem : collection) {
			if(condition.test(elem))
				trueList.add(elem);
			else
				falseList.add(elem);
		}
		return new List[] { trueList, falseList };
	}

	public static <T, G> Map<G, List<T>> groupBy(Collection<T> collection, Function<? super T, ? extends G> grouper) {
		Map<G, List<T>> result = new LinkedHashMap<>();
		for(T elem : collection) {
			G group = grouper.apply(elem);
			result.computeIfAbsent(group, k -> new ArrayList<>()).add(elem);
		}
		return unmodifiableMap(result);
	}

	public static String unindent(String heredoc, int indentStrip) {
		StringBuilder bld = new StringBuilder();
		int top = heredoc.length();
		int pos = 0;
		boolean ws = true;
		for(int idx = 0; idx < top; ++idx, ++pos) {
			char c = heredoc.charAt(idx);
			if(c == '\n') {
				pos = -1;
				bld.append(c);
				ws = true;
				continue;
			}

			if(ws && pos < indentStrip)
				ws = Character.isWhitespace(c);
			else
				bld.append(c);
		}
		return bld.toString();
	}
}
