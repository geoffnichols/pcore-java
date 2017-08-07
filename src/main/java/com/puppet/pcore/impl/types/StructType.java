package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.puppet.pcore.impl.Helpers.*;
import static com.puppet.pcore.impl.types.TypeFactory.*;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableMap;

public class StructType extends AnyType {

	static final StructType DEFAULT = new StructType(Collections.emptyList());
	static final AnyType KEY_TYPE = variantType(StringType.NOT_EMPTY, optionalType(StringType.NOT_EMPTY));

	private static ObjectType ptype;
	public final List<StructElement> elements;
	public final IntegerType size;
	private Map<String,StructElement> hashedMembers;

	StructType(List<StructElement> elements) {
		this.elements = elements;
		size = integerType(count(elements, m -> !m.key.isAssignable(UndefType.DEFAULT)), elements.size());
	}

	@Override
	public Type _pcoreType() {
		return ptype;
	}

	@Override
	public AnyType generalize() {
		return DEFAULT;
	}

	public int hashCode() {
		return elements.hashCode();
	}

	public Map<String,StructElement> hashedMembers() {
		if(hashedMembers == null) {
			Map<String,StructElement> hm = new LinkedHashMap<>();
			for(StructElement m : elements)
				hm.put(m.name, m);
			hashedMembers = unmodifiableMap(hm);
		}
		return hashedMembers;
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType("Pcore::StructType", "Pcore::AnyType",
				asMap("elements", arrayType(typeReferenceType("Pcore::StructElement"))));
	}

	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, structTypeDispatcher(),
				(self) -> new Object[]{self.elements});
	}

	@Override
	void accept(Visitor visitor, RecursionGuard guard) {
		for(StructElement m : elements)
		  m.accept(visitor, guard);
		super.accept(visitor, guard);
	}

	@Override
	IterableType asIterableType(RecursionGuard guard) {
		if(this.equals(DEFAULT))
			return iterableType(HashType.DEFAULT_KEY_PAIR_TUPLE);
		return iterableType(tupleType(asList(
				variantType(map(elements, member -> member.key)),
				variantType(map(elements, member -> member.value))), HashType.KEY_PAIR_TUPLE_SIZE));
	}

	@Override
	boolean guardedEquals(Object o, RecursionGuard guard) {
		return o instanceof StructType && equals(elements, ((StructType)o).elements, guard);
	}

	@Override
	boolean isInstance(Object o, RecursionGuard guard) {
		if(o instanceof Map<?,?>) {
			Map<?,?> mo = (Map<?,?>)o;
			int matched = 0;
			for(StructElement element : elements) {
				String key = element.name;
				Object v = mo.get(key);
				if(v == null && !mo.containsKey(key)) {
					if(!element.key.isAssignable(undefType(), guard))
						return false;
				} else {
					++matched;
					if(!element.value.isInstance(v, guard))
						return false;
				}
			}
			return matched == mo.size();
		}
		return false;
	}

	@Override
	boolean isIterable(RecursionGuard guard) {
		return true;
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		if(t instanceof StructType) {
			StructType ht = (StructType)t;
			Map<String,StructElement> h2 = ht.hashedMembers();
			int[] matched = {0};
			return all(elements, e1 -> {
				StructElement e2 = h2.get(e1.name);
				if(e2 == null)
					return e1.key.isAssignable(undefType(), guard);
				matched[0]++;
				return e1.key.isAssignable(e2.key, guard) && e1.value.isAssignable(e2.value, guard);
			}) && matched[0] == h2.size();
		}

		if(t instanceof HashType) {
			HashType ht = (HashType)t;
			int[] required = {0};
			boolean requiredMembersAssignable = all(elements, e -> {
				AnyType key = e.key;
				if(key.isAssignable(undefType(), guard))
					// StructElement is optional so Hash does not need to provide it
					return true;

				required[0]++;

				// Hash must have something that is assignable. We don't care about the name or attributeCount of the key
				// though
				// because we have no instance of a hash to compare against.
				return e.value.isAssignable(ht.type) && key.generalize().isAssignable(ht.keyType, guard);
			});
			if(requiredMembersAssignable)
				return integerType(required[0], elements.size()).isAssignable(ht.size, guard);
		}
		return false;
	}
}
