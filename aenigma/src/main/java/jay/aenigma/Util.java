package jay.aenigma;

import java.util.*;
import java.util.function.Function;

/**
 * Class that provides various general utility functions.
 * Not instantiable.
 */
public final class Util{
	private Util(){throw new UnsupportedOperationException();}
	
	/** Creates a map that contains the Entries (Key k, Value v),
	 * such that each Value v is an element of values,
	 * and for each Value v, the Key k is the result of applying the Function keyExtractor to v. <br/>
	 * If any Values v in values are mapped to the same Key k, then for each such k,
	 * k is mapped to the last corresponding Value v (in encounter order of values),
	 * and the other corresponding Values v are discarded. <br/>
	 * Otherwise, if a Value v is mapped to an unique Key k, then that mapping will be used.
	 * @param values the values to be mapped
	 * @param keyExtractor the functions to extract the Keys with
	 * @param <K> Class of Keys to be mapped by
	 * @param <V> Class of Values to be mapped
	 * @return a new Map containing (k,v) for each v in values, where k = keyExtractor.apply(v)
	 */
	public static <K,V> Map<K,V> mapBy(Collection<V> values, Function<V,K> keyExtractor){
		Map<K,V> kvMap = new HashMap<>();
		values.forEach(v -> kvMap.put(keyExtractor.apply(v), v));
		return kvMap;
	}
	
	/** Creates a multi-map that contains the Entries (Key k, List-of-Values lv),
	 * such that each List-of-Values lv is a List of elements of values,
	 * and for each List-of-Values lv, the Key k is the result of applying the Function keyExtractor to the respective Values v. <br/>
	 * If any Values v in values are mapped to the same Key k, then for each such k,
	 * k is mapped to the List-of-Values lv that contains exactly these Values v.
	 * @param values the values to be mapped
	 * @param keyExtractor the functions to extract the Keys with
	 * @param <K> Class of Keys to be mapped by
	 * @param <V> Class of Values to be mapped
	 * @return a new Map containing (k, List-of-Values lv) where lv contains all v in values, such that k == keyExtractor.apply(v)
	 */
	public static <K,V> Map<K, List<V>> multiMapBy(Collection<V> values, Function<V,K> keyExtractor){
		Map<K,List<V>> kvsMap = new HashMap<>();
		values.forEach(v -> {
			K k = keyExtractor.apply(v);
			if(!kvsMap.containsKey(k))
				kvsMap.put(k, new ArrayList<>());
			kvsMap.get(k).add(v);
		});
		return kvsMap;
	}
	
	/** Updates a map kvMap with Entries (Key k, Value v),
	 * such that each Value v is an element of values,
	 * and for each Value v, the Key k is the result of applying the Function keyExtractor to v. <br/>
	 * If any Values v in values are mapped to the same Key k, then for each such k,
	 * k is mapped to the last corresponding Value v (in encounter order of values),
	 * and the other corresponding Values v are discarded. <br/>
	 * Otherwise, if a Value v is mapped to an unique Key k, then that mapping will be used.<br/>
	 * Previously existing mappings will not be cleared, unless as a natural consequence of any occurring Key-conflict.
	 * @param values the values to be mapped
	 * @param keyExtractor the functions to extract the Keys with
	 * @param <K> Class of Keys to be mapped by
	 * @param <V> Class of Values to be mapped
	 * @param kvMap a Map K -> V, to be updated with (k,v) for each v in values, where k = keyExtractor.apply(v)
	 */
	public static <K,V> void putAllBy(Map<K, V> kvMap, Collection<V> values, Function<V,K> keyExtractor){
		values.forEach(v -> kvMap.put(keyExtractor.apply(v), v));
	}
	
	/** Updates the Entries (Key k, List-of-Values lv) of a multi-map,
	 * such that each List-of-Values lv contains is appended with the List of elements of values,
	 * for which the corresponding Key k is the result of applying the Function keyExtractor to the respective Values v. <br/>
	 * Previously existing mappings will not be cleared.
	 * @param values the values to be mappe
	 * @param keyExtractor the functions to extract the Keys with
	 * @param <K> Class of Keys to be mapped by
	 * @param <V> Class of Values to be mapped
	 * @param multiMap the Map to be updated
	 */
	public static <K,V> void addAllToBy(Map<K, List<V>> multiMap, Collection<V> values, Function<V,K> keyExtractor){
		values.forEach(v -> {
			K k = keyExtractor.apply(v);
			if(!multiMap.containsKey(k))
				multiMap.put(k, new ArrayList<>());
			multiMap.get(k).add(v);
		});
	}
}
