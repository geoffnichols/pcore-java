package com.puppet.pcore.impl.types;

import com.puppet.pcore.*;
import com.puppet.pcore.impl.Constants;
import com.puppet.pcore.impl.PcoreImpl;
import com.puppet.pcore.impl.TypeEvaluatorImpl;
import com.puppet.pcore.impl.loader.TypeSetLoader;
import com.puppet.pcore.impl.parser.HashExpression;
import com.puppet.pcore.impl.parser.TypeNameExpression;
import com.puppet.pcore.loader.Loader;
import com.puppet.pcore.loader.TypedName;
import com.puppet.pcore.parser.Expression;
import com.puppet.pcore.semver.Version;
import com.puppet.pcore.semver.VersionRange;
import com.puppet.pcore.serialization.ArgumentsAccessor;

import java.io.IOException;
import java.net.URI;
import java.util.*;

import static com.puppet.pcore.impl.Constants.*;
import static com.puppet.pcore.impl.Helpers.*;
import static com.puppet.pcore.impl.types.TypeFactory.*;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

public class TypeSetType extends MetaType implements PuppetObjectWithHash {
	public class Reference implements Annotatable {
		public final Map<AnyType,Map<String,?>> annotations;
		public final String name;
		public final URI nameAuthority;
		public final VersionRange versionRange;
		private TypeSetType typeSet;

		public Reference(Map<String,Object> initHash) {
			Object nameAuthority = initHash.get(KEY_NAME_AUTHORITY);
			if(nameAuthority == null)
				this.nameAuthority = TypeSetType.this.nameAuthority;
			else {
				if(nameAuthority instanceof String)
					nameAuthority = URI.create((String)nameAuthority);
				this.nameAuthority = (URI)nameAuthority;
			}
			this.name = (String)initHash.get(KEY_NAME);
			Object versionRange = initHash.get(KEY_VERSION_RANGE);
			this.versionRange = versionRange instanceof String ? VersionRange.create((String)versionRange) : (VersionRange)versionRange;
			@SuppressWarnings("unchecked") Map<AnyType,Map<String,?>> annotations = (Map<AnyType,Map<String,?>>)initHash.get(KEY_ANNOTATIONS);
			this.annotations = annotations == null ? emptyMap() : unmodifiableCopy(annotations);
		}

		@Override
		public Map<AnyType,Map<String,?>> getAnnotations() {
			return annotations;
		}

		public Map<String,Object> initHash() {
			Map<String,Object> result = new LinkedHashMap<>();
			Map<AnyType,Map<String,?>> annotations = getAnnotations();
			if(!annotations.isEmpty())
				result.put(Constants.KEY_ANNOTATIONS, annotations);
			if(!nameAuthority.equals(TypeSetType.this.nameAuthority))
				result.put(KEY_NAME_AUTHORITY, nameAuthority.toString());
			result.put(KEY_NAME, name);
			result.put(KEY_VERSION_RANGE, versionRange.toString());
			return unmodifiableCopy(result);
		}

		public void resolve() {
			TypedName tn = new TypedName("type", name, nameAuthority);
			Object type = Pcore.loader().load(tn);
			if(!(type instanceof TypeSetType))
				throw new TypeResolverException(format("%s resolves to a %s", this, type));

			this.typeSet = (TypeSetType)((TypeSetType)type).resolve();
			if(!versionRange.includes(typeSet.version))
				throw new TypeResolverException(format(
						"%s resolves to an incompatible version. Expected %s, got %s", this, versionRange, typeSet.version));
		}

		void accept(Visitor visitor, RecursionGuard guard) {
			for(AnyType key : getAnnotations().keySet())
				key.accept(visitor, guard);
		}
	}
	private static final AnyType TYPE_STRING_OR_VERSION = variantType(StringType.NOT_EMPTY, semVerType());
	private static final AnyType TYPE_STRING_OR_RANGE = variantType(StringType.NOT_EMPTY, semVerRangeType());
	private static final AnyType TYPE_TYPE_REFERENCE_INIT = structType(
			structElement(KEY_NAME, TYPE_QUALIFIED_REFERENCE),
			structElement(KEY_VERSION_RANGE, TYPE_STRING_OR_RANGE),
			structElement(optionalType(KEY_NAME_AUTHORITY), TYPE_URI),
			structElement(optionalType(KEY_ANNOTATIONS), TYPE_ANNOTATIONS)
	);
	static final StructType TYPE_TYPESET_INIT = structType(
			structElement(optionalType(KEY_PCORE_URI), TYPE_URI),
			structElement(KEY_PCORE_VERSION, TYPE_STRING_OR_VERSION),
			structElement(optionalType(KEY_NAME_AUTHORITY), TYPE_URI),
			structElement(optionalType(KEY_NAME), TYPE_QUALIFIED_REFERENCE),
			structElement(KEY_VERSION, TYPE_STRING_OR_VERSION),
			structElement(optionalType(KEY_TYPES), hashType(TYPE_SIMPLE_TYPE_NAME, typeType(), integerType(1))),
			structElement(optionalType(KEY_REFERENCES), hashType(TYPE_SIMPLE_TYPE_NAME, TYPE_TYPE_REFERENCE_INIT, integerType(1))),
			structElement(optionalType(KEY_ANNOTATIONS), TYPE_ANNOTATIONS)
	);

	static final TypeSetType DEFAULT = new TypeSetType(asMap(
			KEY_NAME, "DefaultTypeSet",
			KEY_NAME_AUTHORITY, RUNTIME_NAME_AUTHORITY.toString(),
			KEY_PCORE_URI, PCORE_URI.toString(),
			KEY_PCORE_VERSION, PCORE_VERSION,
			KEY_VERSION, Version.create(0, 0, 0)
	));

	private static ObjectType ptype;
	private final Map<String,String> dcToCcMap = new HashMap<>();
	private Map<AnyType,Map<String,?>> annotations;
	private String name;
	private URI nameAuthority;
	private URI pcoreUri;
	private Version pcoreVersion;
	private Map<String,Reference> references = emptyMap();
	private Map<String,AnyType> types = emptyMap();
	private Version version;

	@SuppressWarnings("unchecked")
	TypeSetType(ArgumentsAccessor args) throws IOException {
		super((Expression)null);
		args.remember(this);
		setInitHashExpression((Map<String,Object>)args.get(0));
	}

	TypeSetType(String name, URI nameAuthority, Expression initHashExpression) {
		super(initHashExpression);
		this.name = TYPE_QUALIFIED_REFERENCE.assertInstanceOf(name, true, () -> "TypeSet name");
		this.nameAuthority = nameAuthority;
	}

	TypeSetType(Map<String,Object> initHash) {
		super((Expression)null);
		initializeFromHash(initHash);
	}

	@Override
	public Type _pcoreType() {
		return ptype;
	}

	public boolean definesType(AnyType type) {
		return types.containsValue(type);
	}

	@Override
	public AnyType generalize() {
		return DEFAULT;
	}

	/**
	 * Resolve a type in this type set using a qualified name. The resolved type may either be a type defined in this type set
	 * or a type defined in a type set that is referenced by this type set (nesting may occur to any level).
	 * The name resolution is case insensitive.
	 *
	 * @param qName the qualified name of the type to resolve
	 * @return the resolved type, or {@code null} in case no type could be found
	 */
	public AnyType get(String qName) {
		AnyType type = types.get(qName);
		if(type == null) {
			String ccName = ccName(qName);
			if(ccName != null)
				type = types.get(ccName);
		}
		if(type != null)
			return type;

		if(references.isEmpty())
			return null;

		String[] segments = splitName(qName);
		if(segments.length == 0)
			return null;

		String first = segments[0];
		Reference typeSetRef = references.get(first);
		if(typeSetRef == null) {
			String ccName = ccName(first);
			if(ccName != null)
				typeSetRef = references.get(ccName);
		}
		if(typeSetRef == null)
			return null;

		TypeSetType typeSet = typeSetRef.typeSet;
		return segments.length == 1 ? typeSet : typeSet.get(joinName(segments, 1));
	}

	public URI getNameAuthority() {
		return nameAuthority;
	}

	@Override
	public Map<String,Object> _pcoreInitHash() {
		if(pcoreVersion == null)
			throw new TypeResolverException("Attempt to retrieve initHash of unresolved TypeSet");

		Map<String,Object> result = super._pcoreInitHash();
		if(pcoreUri != null)
			result.put(KEY_PCORE_URI, pcoreUri.toString());
		result.put(KEY_PCORE_VERSION, pcoreVersion.toString());
		if(nameAuthority != null)
			result.put(KEY_NAME_AUTHORITY, nameAuthority.toString());
		if(name != null)
			result.put(KEY_NAME, name);
		result.put(KEY_VERSION, version);
		if(!types.isEmpty())
			result.put(KEY_TYPES, unmodifiableCopy(types));
		if(!references.isEmpty()) {
			LinkedHashMap<String,Map<String,Object>> refs = new LinkedHashMap<>();
			for(Map.Entry<String,Reference> entry : references.entrySet())
				refs.put(entry.getKey(), entry.getValue().initHash());
			result.put(KEY_REFERENCES, unmodifiableCopy(refs));
		}
		return unmodifiableCopy(result);
	}

	/**
	 * Returns the name by which the given type is referenced from within this type set
	 *
	 * @param type the type to get a TypeSet relative name for
	 * @return the name by which the type is referenced within this type set
	 */
	public String nameFor(AnyType type) {
		for(Map.Entry<String,AnyType> te : types.entrySet())
			if(te.getValue().equals(type))
				return te.getKey();

		final String qName = type.name();
		if(references.isEmpty())
			return qName;

		String[] segments = splitName(qName);
		if(segments.length < 2)
			return qName;

		Reference typeSetRef = references.get(segments[0]);
		if(typeSetRef == null)
			return qName;

		String subName = typeSetRef.typeSet.nameFor(type);
		return subName.equals(qName) ? qName : segments[0] + "::" + subName;
	}

	@Override
	public AnyType resolve() {
		super.resolve();
		for(Reference ref : references.values())
			ref.resolve();
		return Pcore.withTypeSetScope(this, () -> {
			for(Map.Entry<String,AnyType> entry : types.entrySet())
				entry.setValue(entry.getValue().resolve());
			types = unmodifiableCopy(types);
			return this;
		});
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType("Pcore::TypeSetType", "Pcore::AnyType",
			asMap(
					KEY_PCORE_URI, asMap(
						KEY_TYPE, optionalType(TYPE_URI),
						KEY_VALUE, null),
					KEY_PCORE_VERSION, TYPE_STRING_OR_VERSION,
					KEY_NAME_AUTHORITY, asMap(
							KEY_TYPE, optionalType(TYPE_URI),
							KEY_VALUE, null),
					KEY_NAME, asMap(
							KEY_TYPE, optionalType(TYPE_QUALIFIED_REFERENCE),
							KEY_VALUE, null),
					KEY_VERSION, TYPE_STRING_OR_VERSION,
					KEY_TYPES, asMap(
							KEY_TYPE, optionalType(hashType(TYPE_SIMPLE_TYPE_NAME, typeType(), integerType(1))),
							KEY_VALUE, null),
					KEY_REFERENCES, asMap(
							KEY_TYPE, optionalType(hashType(TYPE_SIMPLE_TYPE_NAME, TYPE_TYPE_REFERENCE_INIT, integerType(1))),
							KEY_VALUE, null),
					KEY_ANNOTATIONS, asMap(
							KEY_TYPE, optionalType(KEY_ANNOTATIONS),
							KEY_VALUE, null)
			));
	}

	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, typeSetTypeDispatcher(),
				(self) -> new Object[]{self._pcoreInitHash()});
	}

	@Override
	void accept(Visitor visitor, RecursionGuard guard) {
		super.accept(visitor, guard);
		for(AnyType type : types.values())
			type.accept(visitor, guard);
		for(Reference typeSetRef : references.values())
			typeSetRef.accept(visitor, guard);
	}

	@SuppressWarnings("unchecked")
	@Override
	void initializeFromHash(Map<String,Object> initHash) {
		TYPE_TYPESET_INIT.assertInstanceOf(initHash, () -> "TypeSet initializer");
		if(name == null)
			name = (String)initHash.get(KEY_NAME);
		if(nameAuthority == null) {
			String ns = (String)initHash.get(KEY_NAME_AUTHORITY);
			nameAuthority = ns == null ? RUNTIME_NAME_AUTHORITY : URI.create(ns);
		}

		Object pcoreVersion = initHash.get(KEY_PCORE_VERSION);
		if(pcoreVersion instanceof String)
			pcoreVersion = Version.create((String)pcoreVersion);

		this.pcoreVersion = (Version)pcoreVersion;
		if(!PCORE_PARSABLE_VERSIONS.includes(this.pcoreVersion))
			throw new TypeResolverException(format("The pcore version for TypeSet '%s' is not understood by this runtime. Expected range %s, got %s",
					name, PCORE_PARSABLE_VERSIONS, this.pcoreVersion));

		Object pcoreURI = initHash.get(KEY_PCORE_URI);
		if(pcoreURI instanceof String)
			pcoreURI = URI.create((String)pcoreURI);
		this.pcoreUri = (URI)pcoreURI;

		Object version = initHash.get(KEY_VERSION);
		if(version instanceof String)
			version = Version.create((String)version);
		this.version = (Version)version;

		Map<String,AnyType> types = (Map<String,AnyType>)initHash.get(KEY_TYPES);
		if(types != null) {
			this.types = new LinkedHashMap<>(types);
			for(String typeName : this.types.keySet())
				dcToCcMap.put(typeName.toLowerCase(Locale.ENGLISH), typeName);
		}

		Map<String,Map<String,Object>> refs = (Map<String,Map<String,Object>>)initHash.get(KEY_REFERENCES);
		if(refs != null) {
			Map<String,Reference> refMap = new HashMap<>();
			Map<URI,Map<String,List<VersionRange>>> rootMap = new HashMap<>();
			for(Map.Entry<String,Map<String,Object>> entry : refs.entrySet()) {
				Reference ref = new Reference(entry.getValue());

				// Protect against importing the exact same name_authority/name combination twice if the version ranges intersect
				String refName = ref.name;
				URI refNa = ref.nameAuthority;

				Map<String,List<VersionRange>> naRoots = rootMap.computeIfAbsent(refNa, k -> new HashMap<>());
				List<VersionRange> ranges = naRoots.computeIfAbsent(refName, k -> new ArrayList<>());
				for(VersionRange range : ranges) {
					if(range.isOverlap(ref.versionRange))
						throw new TypeResolverException(format(
								"TypeSet '%s' references TypeSet '%s/%s' more than once using overlapping version ranges",
								name, refNa, refName));
				}
				ranges.add(ref.versionRange);

				String refAlias = entry.getKey();
				if(refMap.containsKey(refAlias))
					throw new TypeResolverException(format(
							"TypeSet '%s' references a TypeSet using alias '%s' more than once", name, refAlias));

				if(this.types.containsKey(refAlias))
					throw new TypeResolverException(format(
							"TypeSet '%s' references a TypeSet using alias '%s'. The alias collides with the name of a declared type", name, refAlias));

				refMap.put(refAlias, ref);
				dcToCcMap.put(refAlias.toLowerCase(Locale.ENGLISH), refAlias);
			}
			this.references = unmodifiableMap(refMap);
		}
		super.initializeFromHash(initHash);
	}

	@Override
	boolean isInstance(Object o, RecursionGuard guard) {
		return isAssignable(infer(o), guard);
	}

	@Override
	boolean isUnsafeAssignable(AnyType type, RecursionGuard guard) {
		return getClass().equals(type.getClass()) && equals(DEFAULT) || guardedEquals(type, guard);
	}

	@SuppressWarnings("unchecked")
	@Override
	Map<String,Object> resolveHash(Map<String,Object> initHash) {
		Map<String,Object> result = new LinkedHashMap<>();
		for(Map.Entry<String,Object> entry : initHash.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if(!KEY_TYPES.equals(key) && value instanceof Map)
				value = resolveTypeRefs(value);
			result.put(key, value);
		}

		URI nameAuth = resolveNameAuthority(result);
		Map<String,Object> types = (Map<String,Object>)result.get(KEY_TYPES);
		if(types != null && !types.isEmpty()) {
			types = new LinkedHashMap<>(types);
			for(Map.Entry<String,Object> entry : types.entrySet()) {
				Object value = entry.getValue();
				if(value instanceof Map)
					value = objectType((Map<String,Object>)value);
				else {
					value = resolveTypeRefs(value);
					if(!(value instanceof AnyType))
						throw new TypeResolverException(format("Unexpected value of class '%s' in types hash", value.getClass().getName()));
				}
				types.put(
						entry.getKey(),
						((TypeEvaluatorImpl)Pcore.typeEvaluator()).bindByName(name + "::" + entry.getKey(), (AnyType)value, nameAuth));
			}
			result.put(KEY_TYPES, unmodifiableCopy(types));
		}
		return result;
	}

	@Override
	Map<String,Object> resolveLiteralHash(HashExpression i12e) {
		Map<String,Object> result = new LinkedHashMap<>();

		TypeEvaluator evaluator = Pcore.typeEvaluator();
		List<Expression> elements = i12e.elements;
		int top = elements.size();
		for(int idx = 0; idx < top; ) {
			Object key = evaluator.resolve(elements.get(idx++));
			Expression value = elements.get(idx++);
			boolean isTypes = KEY_TYPES.equals(key);
			if((isTypes || KEY_REFERENCES.equals(key)) && value instanceof HashExpression) {
				// Skip evaluation and convert qualified references directly to String keys
				Map<String,Object> hash = new LinkedHashMap<>();
				List<Expression> vElements = ((HashExpression)value).elements;
				int vTop = vElements.size();
				for(int vIdx = 0; vIdx < vTop; ) {
					Expression kex = vElements.get(vIdx++);
					Expression vax = vElements.get(vIdx++);
					String name = kex instanceof TypeNameExpression
							? ((TypeNameExpression)kex).name
							: (String)evaluator.resolve(kex);
					hash.put(name, isTypes ? vax : evaluator.resolve(vax));
				}
				result.put((String)key, hash);
			} else
				result.put((String)key, evaluator.resolve(value));
		}

		URI nameAuth = resolveNameAuthority(result);
		Object types = result.get(KEY_TYPES);
		if(types instanceof Map<?,?>) {
			@SuppressWarnings("unchecked") Map<String,Object> typesMap = (Map<String,Object>)types;
			for(Map.Entry<String,Object> entry : typesMap.entrySet())
				typesMap.put(entry.getKey(), evaluator.declareType(name + "::" + entry.getKey(), (Expression)entry.getValue(), nameAuth));
		}
		return result;
	}

	private String ccName(String name) {
		return dcToCcMap.get(name.toLowerCase(Locale.ENGLISH));
	}

	private URI resolveNameAuthority(Map<String,Object> initHash) {
		URI nameAuth = nameAuthority;
		if(nameAuth != null)
			return nameAuth;

		Object ne = initHash.get(KEY_NAME_AUTHORITY);
		nameAuth = ne instanceof String ? URI.create((String)ne) : (URI)ne;
		if(nameAuth != null)
			return nameAuth;

		Loader loader = Pcore.loader();
		if(loader instanceof TypeSetLoader)
			return loader.getNameAuthority();

		String n = name;
		if(n == null)
			n = (String)initHash.get(KEY_NAME);
		throw new TypeResolverException(format("No 'name_authority' is declared in TypeSet '%s' and it cannot be inferred", n));
	}
}
