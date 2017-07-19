package com.puppet.pcore.impl;

import com.puppet.pcore.Pcore;
import com.puppet.pcore.TypeAssertionException;
import com.puppet.pcore.TypeEvaluator;
import com.puppet.pcore.impl.types.AnyType;
import com.puppet.pcore.semver.VersionRange;
import com.puppet.pcore.time.InstantFormat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;

import static com.puppet.pcore.TestHelper.multiline;
import static com.puppet.pcore.impl.types.TypeFactory.*;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("unused")
@DisplayName("The TypeEvaluatorImpl")
public class TypeEvaluatorTest {
	@Nested
	@DisplayName("when evaluating types without parameters")
	class Unparameterized {
		@Test
		@DisplayName("can resolve all core types")
		public void resolvesCoreTypes() {
			for(AnyType type : TypeEvaluatorImpl.BASIC_TYPES.values())
				Assertions.assertEquals(type, resolveType(type.toString()));
		}
	}

	@Nested
	@DisplayName("when evaluating parameterized type")
	class Parameterized {
		@Nested
		@DisplayName("fails with")
		class Fails {
			@Test
			@DisplayName("Integer[2,1]")
			public void negativeRange() {
				assertThrows(TypeAssertionException.class, () -> resolveType("Integer[2,1]"));
			}

			@Test
			@DisplayName("Integer[2,-4]")
			public void negativeRange2() {
				assertThrows(TypeAssertionException.class, () -> resolveType("Integer[2,-4]"));
			}
		}

		@Nested
		@DisplayName("succeeds with")
		class Succeeds {
			@Test
			@DisplayName("Array[Integer]")
			public void arrayType1() {
				org.junit.jupiter.api.Assertions.assertEquals(arrayType(integerType()), resolveType("Array[Integer]"));
			}

			@Test
			@DisplayName("Array[2,8]")
			public void arrayType2() {
				Assertions.assertEquals(arrayType(integerType(2, 8)), resolveType("Array[2,8]"));
			}

			@Test
			@DisplayName("Array[Integer,Integer[2,8]]")
			public void arrayType3() {
				Assertions.assertEquals(arrayType(integerType(), integerType(2, 8)), resolveType("Array[Integer,Integer[2," +
						"8]]"));
			}

			@Test
			@DisplayName("Array[Integer,2,8]")
			public void arrayType4() {
				Assertions.assertEquals(arrayType(integerType(), integerType(2, 8)), resolveType("Array[Integer,2,8]"));
			}

			@Test
			@DisplayName("Callable[String[1], Integer[-1,3]]")
			public void callableType2() {
				Assertions.assertEquals(
						callableType(tupleType(asList(stringType(integerType(1)),
								integerType(-1, 3)))),
						resolveType("Callable[String[1], Integer[-1,3]]"));
			}

			@Test
			@DisplayName("Callable[[String[1], Integer[-1,3]], Float]")
			public void callableType3() {
				Assertions.assertEquals(
						callableType(tupleType(asList(stringType(integerType(1)), integerType(-1, 3))), null, floatType()),
						resolveType("Callable[[String[1], Integer[-1,3]], Float]"));
			}

			@Test
			@DisplayName("Callable[String[1], Integer[-1,3], Callable[[0,0],String]]")
			public void callableType4() {
				Assertions.assertEquals(
						callableType(tupleType(asList(stringType(integerType(1)), integerType(-1, 3))),
								callableType(tupleType(emptyList()), null, stringType())),
						resolveType("Callable[String[1], Integer[-1,3], Callable[[0,0],String]]"));
			}

			@Test
			@DisplayName("Class['MyClass']")
			public void classType1() {
				Assertions.assertEquals(classType("MyClass"), resolveType("Class['MyClass']"));
			}

			@Test
			@DisplayName("Collection[2]")
			public void collectionType1() {
				Assertions.assertEquals(collectionType(integerType(2)), resolveType("Collection[2]"));
			}

			@Test
			@DisplayName("Collection[Integer[1,2]]")
			public void collectionType2() {
				Assertions.assertEquals(collectionType(integerType(1, 2)), resolveType("Collection[Integer[1,2]]"));
			}

			@Test
			@DisplayName("Collection[1,2]")
			public void collectionType3() {
				Assertions.assertEquals(collectionType(integerType(1, 2)), resolveType("Collection[1,2]"));
			}

			@Test
			@DisplayName("Enum['a', 'b', 'c']")
			public void enumType1() {
				Assertions.assertEquals(enumType("a", "b", "c"), resolveType("Enum['a','b','c']"));
			}

			@Test
			@DisplayName("Enum[a, b, c]")
			public void enumType2() {
				Assertions.assertEquals(enumType("a", "b", "c"), resolveType("Enum[a, b, c]"));
			}

			@Test
			@DisplayName("Float[1.0]")
			public void floatType1() {
				Assertions.assertEquals(floatType(1.0), resolveType("Float[1.0]"));
			}

			@Test
			@DisplayName("Float[1.0,3.8]")
			public void floatType2() {
				Assertions.assertEquals(floatType(1.0, 3.8), resolveType("Float[1.0,3.8]"));
			}

			@Test
			@DisplayName("Hash[String, Integer]")
			public void hashType1() {
				Assertions.assertEquals(hashType(stringType(), integerType()), resolveType("Hash[String, Integer]"));

			}

			@Test
			@DisplayName("Hash[0, 10]")
			public void hashType2() {
				Assertions.assertEquals(hashType(unitType(), unitType(), 0, 10), resolveType("Hash[0, 10]"));
			}

			@Test
			@DisplayName("Hash[String, Float, 3]")
			public void hashType3() {
				Assertions.assertEquals(hashType(stringType(), floatType(), integerType(3)), resolveType("Hash[String, Float, " +
						"3]"));
			}

			@Test
			@DisplayName("Hash[String, Float, 3, 10]")
			public void hashType4() {
				Assertions.assertEquals(hashType(stringType(), floatType(), 3, 10), resolveType("Hash[String, Float, 3, 10]"));
			}

			@Test
			@DisplayName("Integer[1]")
			public void integerType1() {
				Assertions.assertEquals(integerType(1), resolveType("Integer[1]"));
			}

			@Test
			@DisplayName("Integer[1,2]")
			public void integerType2() {
				Assertions.assertEquals(integerType(1, 2), resolveType("Integer[1,2]"));
			}

			@Test
			@DisplayName("Iterable[String]")
			public void iterableType1() {
				Assertions.assertEquals(iterableType(stringType()), resolveType("Iterable[String]"));
			}

			@Test
			@DisplayName("Iterator[String]")
			public void iteratorType1() {
				Assertions.assertEquals(iteratorType(stringType()), resolveType("Iterator[String]"));
			}

			@Test
			@DisplayName("NotUndef[Variant[Optional[Integer],String]]")
			public void notUndefType1() {
				Assertions.assertEquals(notUndefType(variantType(optionalType(integerType()), stringType())), resolveType
						("NotUndef[Variant[Optional[Integer],String]]"));
			}

			@Test
			@DisplayName("Pattern[/abc/]")
			public void patternType1() {
				Assertions.assertEquals(patternType(regexpType(Pattern.compile("abc"))), resolveType("Pattern[/abc/]"));
			}

			@Test
			@DisplayName("Pattern[Regexp[/a\\/b\\\\c/]]")
			public void patternType2() {
				Assertions.assertEquals(patternType(regexpType(Pattern.compile("a/b\\\\c"))), resolveType
						("Pattern[Regexp[/a\\/b\\\\c/]]"));

			}

			@Test
			@DisplayName("Resource['TypeName']")
			public void resourceType1() {
				Assertions.assertEquals(resourceType("TypeName"), resolveType("Resource['TypeName']"));
			}

			@Test
			@DisplayName("Resource[TypeName]]")
			public void resourceType2() {
				Assertions.assertEquals(resourceType("TypeName"), resolveType("Resource[TypeName]"));
			}

			@Test
			@DisplayName("Resource[TypeName['title']]")
			public void resourceType3() {
				Assertions.assertEquals(resourceType("TypeName", "title"), resolveType("Resource[TypeName['title']]"));
			}

			@Test
			@DisplayName("Resource['TypeName', 'title']")
			public void resourceType4() {
				Assertions.assertEquals(resourceType("TypeName", "title"), resolveType("Resource['TypeName', 'title']"));
			}

			@Test
			@DisplayName("Runtime[ruby,'Semantic::Version']")
			public void runtimeType1() {
				Assertions.assertEquals(runtimeType("ruby", "Semantic::Version"), resolveType("Runtime[ruby," +
						"'Semantic::Version']"));
			}

			@Test
			@DisplayName("SemVer['1.0.0']")
			public void semVerType1() {
				Assertions.assertEquals(semVerType(VersionRange.create("1.0.0")), resolveType("SemVer['1.0.0']"));
			}

			@Test
			@DisplayName("Sensitive[String]")
			public void sensitiveType1() {
				Assertions.assertEquals(sensitiveType(stringType()), resolveType("Sensitive[String]"));
			}

			@Test
			@DisplayName("String[1]")
			public void stringType1() {
				Assertions.assertEquals(stringType(integerType(1)), resolveType("String[1]"));
			}

			@Test
			@DisplayName("String[Integer[1,2]]")
			public void stringType2() {
				Assertions.assertEquals(stringType(integerType(1, 2)), resolveType("String[Integer[1,2]]"));
			}

			@Test
			@DisplayName("String[1,8]")
			public void stringType3() {
				Assertions.assertEquals(stringType(1, 8), resolveType("String[1,8]"));
			}

			@Test
			@DisplayName("Struct[{'a' => String, 'b' => Integer[1,0775]}]")
			public void structType1() {
				Assertions.assertEquals(
						//noinspection OctalInteger
						structType(structElement("a", stringType()), structElement
								("b", integerType(1, 0775))),
						resolveType("Struct[{'a' => String, 'b' => Integer[1,0775]}]"));

			}

			@Test
			@DisplayName("Struct[{a::b => String, b::c => Integer[1,0x5a]}]")
			public void structType2() {
				Assertions.assertEquals(
						structType(structElement("a::b", stringType()), structElement("b::c", integerType(1, 0x5a))),
						resolveType("Struct[{a::b => String, b::c => Integer[1,0x5a]}]"));
			}

			@Test
			@DisplayName("Struct[{Optional[a::b] => String, b::c => Integer[1,0x5a]}]")
			public void structType3() {
				Assertions.assertEquals(
						structType(structElement(optionalType("a::b"), stringType()), structElement("b::c", integerType(1, 0x5a))),
						resolveType("Struct[{Optional[a::b] => String, b::c => Integer[1,0x5a]}]"));
			}

			@Test
			@DisplayName("TimeSpan['00:10:00']")
			public void timeSpanType1() {
				Assertions.assertEquals(
						timeSpanType(Duration.ofMinutes(10)),
						resolveType("TimeSpan['00:10:00']"));
			}

			@Test
			@DisplayName("TimeSpan['00:10:00', default]")
			public void timeSpanType2() {
				Assertions.assertEquals(
						timeSpanType(Duration.ofMinutes(10)),
						resolveType("TimeSpan['00:10:00', default]"));
			}

			@Test
			@DisplayName("TimeSpan['00:00:00', '23:59:59.999999999']")
			public void timeSpanType3() {
				Assertions.assertEquals(
						timeSpanType(Duration.ofNanos(0), Duration.ofDays(1).minusNanos(1)),
						resolveType("TimeSpan['00:00:00', '23:59:59.999999999']"));
			}

			@Test
			@DisplayName("Timestamp['2016-11-04T04:49:30Z']")
			public void timestampType1() {
				Assertions.assertEquals(
						timestampType(Instant.parse("2016-11-04T04:49:30Z")),
						resolveType("Timestamp['2016-11-04T04:49:30']"));
			}

			@Test
			@DisplayName("Timestamp['2016-11-04T04:49:30', default]")
			public void timestampType2() {
				Assertions.assertEquals(
						timestampType(Instant.parse("2016-11-04T04:49:30Z")),
						resolveType("Timestamp['2016-11-04T04:49:30', default]"));
			}

			@Test
			@DisplayName("Timestamp['2016-11-04T04:49:30Z', '2016-12-31T23:59:59Z']")
			public void timestampType3() {
				Assertions.assertEquals(
						timestampType(Instant.parse("2016-11-04T04:49:30Z"), Instant.parse("2016-12-31T23:59:59Z")),
						resolveType("Timestamp['2016-11-04T04:49:30', '2016-12-31T23:59:59']"));
			}

			@Test
			@DisplayName("Tuple[Integer,Float]")
			public void tupleType1() {
				Assertions.assertEquals(tupleType(asList(integerType(), floatType())), resolveType("Tuple[Integer,Float]"));
			}

			@Test
			@DisplayName("Tuple[Integer,Float,4]")
			public void tupleType2() {
				Assertions.assertEquals(tupleType(asList(integerType(), floatType()), integerType(4)), resolveType
						("Tuple[Integer,Float,4]"));
			}

			@Test
			@DisplayName("Tuple[Integer,Float,4, 8]")
			public void tupleType3() {
				Assertions.assertEquals(tupleType(asList(integerType(), floatType()), integerType(4, 8)), resolveType
						("Tuple[Integer,Float,4, 8]"));
			}

			@Test
			@DisplayName("Type[Integer]")
			public void typeType1() {
				Assertions.assertEquals(typeType(integerType()), resolveType("Type[Integer]"));
			}

			@Test
			@DisplayName("Variant[String,Integer]")
			public void variantType1() {
				Assertions.assertEquals(variantType(stringType(), integerType()), resolveType("Variant[String,Integer]"));

			}

			@Test
			@DisplayName("Type with ML comments]")
			public void variantTypeWithComments() {
				Assertions.assertEquals(variantType(stringType(), integerType()), resolveType(
						multiline(
								"Variant[String, # this is the name",
								"Integer # this is the numeric index",
								"]")));
			}

			@Test
			@DisplayName("Type with ML comments]")
			public void variantTypeWithMLComments() {
				Assertions.assertEquals(variantType(stringType(), integerType()), resolveType(
						multiline(
								"Variant[String /* this is",
								"the name */,",
								"Integer /* this is",
								"the numeric index*/]")));
			}
		}

		@Nested
		@DisplayName("that is a variant")
		class Variants {
			@Test
			@DisplayName("will normalize overlapping integer ranges")
			public void variantType1() {
				Assertions.assertEquals(integerType(10, 28), resolveType("Variant[Integer[10,20],Integer[18,28]]"));
			}

			@Test
			@DisplayName("will normalize adjacent integer ranges")
			public void variantType2() {
				Assertions.assertEquals(integerType(8, 20), resolveType("Variant[Integer[10,20],Integer[8,9]]"));
			}

			@Test
			@DisplayName("will normalize mix of adjacent and overlapping integer ranges")
			public void variantType3() {
				Assertions.assertEquals(integerType(8, 28), resolveType("Variant[Integer[10,20], Integer[18,28], Integer[8," +
						"9]]"));
			}

			@Test
			@DisplayName("will normalize overlapping float ranges")
			public void variantType4() {
				Assertions.assertEquals(floatType(10.0, 28.0), resolveType("Variant[Float[10.0,20.0],Float[18.0,28.0]]"));
			}

			@Test
			@DisplayName("will normalize overlapping timespan ranges")
			public void variantType5() {
				Assertions.assertEquals(timeSpanType(Duration.ofHours(1), Duration.ofHours(8)), resolveType("Variant[TimeSpan['01:00:00','04:00:00'], TimeSpan['03:00:00','08:00:00']]"));
			}

			@Test
			@DisplayName("will normalize overlapping timestamp ranges")
			public void variantType6() {
				Assertions.assertEquals(timestampType(InstantFormat.SINGLETON.parse("1990-01-01T11:12:00"), InstantFormat.SINGLETON.parse("2010-04-14T00:03:59")),
						resolveType("Variant[Timestamp['1990-01-01T11:12:00','2000-01-01T00:00:00'], Timestamp['2000-01-01T00:00:00','2010-04-14T00:03:59']]"));
			}

			@Test
			@DisplayName("will normalize enums")
			public void variantType7() {
				Assertions.assertEquals(enumType("a", "b", "c"), resolveType("Variant[Enum[a,b],Enum[b,c]]"));
			}

			@Test
			@DisplayName("will normalize patterns")
			public void variantType8() {
				Assertions.assertEquals(patternType(regexpType("a"), regexpType("b"), regexpType("c")), resolveType
						("Variant[Pattern[/a/,/b/],Pattern[/b/,/c/]]"));
			}
		}

		@Nested
		@DisplayName("declared with assignment")
		class Assignment {
			@Test
			@DisplayName("declares a TypeAlias")
			public void typeAlias1() {
				Assertions.assertEquals(typeAliasType("MyType", typeReferenceType("String[1,20]")).resolve(), resolveType("type MyType = String[1,20]"));
			}
		}
	}

	private static TypeEvaluator typeEvaluator;

	@BeforeAll
	public static void init() {
		typeEvaluator = Pcore.typeEvaluator();
	}

	public static AnyType resolveType(String typeString) {
		return ((TypeEvaluatorImpl)typeEvaluator).resolveType(typeString);
	}
}
