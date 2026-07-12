# Value

`Value` is a fundamental concept in DataForge that represents a polymorphic leaf node in a metadata tree. It is a sum type that can hold various types of data, including numbers, strings, booleans, and lists of values.

## Structure

`Value` is an interface with the following core properties:

- **value**: The raw underlying object.
- **type**: A `ValueType` enum indicating the nature of the data.

### Supported Types

The `ValueType` enum defines the following categories:

- **NULL**: Represents a missing or null value.
- **NUMBER**: Represents numeric data (integers, doubles, etc.).
- **STRING**: Represents text data.
- **BOOLEAN**: Represents logical `true` or `false`.
- **LIST**: Represents an ordered sequence of other `Value` objects.

## Implementations

DataForge provides several implementations of the `Value` interface for efficiency and specific use cases:

- **Null**: A singleton representing a null value (string representation: `@null`).
- **True / False**: Singletons for boolean values.
- **NumberValue**: Wraps a `Number` object.
- **StringValue**: A JVM inline value class wrapping a `String`.
- **EnumValue**: Wraps an `Enum` constant.
- **ListValue**: A collection of `Value` objects.

### Exotic Implementations

For performance optimization or specific data types, additional implementations are available:

- **LazyParsedValue**: Defers parsing of a string into a typed `Value` until it is actually accessed.
- **DoubleArrayValue**: A memory-efficient wrapper for `DoubleArray` that avoids boxing individual elements into `NumberValue` objects until necessary.
- **ByteArrayValue**: Similar to `DoubleArrayValue`, but for `ByteArray`.

## Construction

### Factory Methods

The `Value.of` method is the primary way to create a `Value` from an arbitrary object:

```kotlin
val v1 = Value.of(10)          // NumberValue
val v2 = Value.of("text")      // StringValue
val v3 = Value.of(listOf(1, 2)) // ListValue
```

### Parsing from String

`Value.parse` converts a string into its most appropriate `Value` representation:

```kotlin
val v1 = Value.parse("10")      // NumberValue(10)
val v2 = Value.parse("true")    // True
val v3 = Value.parse("\"10\"")  // StringValue("10") - quotes force literal string
```

### Extension Functions

Most primitive types and collections have an `asValue()` extension function:

```kotlin
val v = 42.asValue()
val listV = listOf("a", "b").map { it.asValue() }.asValue()
```

## Usage Scenarios

### Metadata Leaf Nodes

`Value` is used to store data in `Meta` nodes. A `Meta` item can either be another `Meta` node or a `Value`.

### Value Providers

DataForge defines `ValueProvider` and `MutableValueProvider` interfaces for objects that can provide or consume values identified by `Name`. `Meta` objects implement these interfaces, allowing hierarchical access to metadata values.

### Typed Access

Extension properties provide convenient, typed access to the contents of a `Value`:

```kotlin
val value: Value = ...

val intVal: Int = value.int
val doubleVal: Double = value.double
val boolVal: Boolean = value.boolean
val stringVal: String = value.string
val enumVal: MyEnum = value.enum<MyEnum>()
```

Properties like `int`, `double`, etc., will attempt to convert the underlying data if possible (e.g., parsing a string as a number) or throw an error if the conversion is invalid.

## Serialization

`Value` is serializable using `kotlinx.serialization`. The default `ValueSerializer` handles polymorphic serialization by including type information.

### Serialization Strategy

When a `Value` is serialized, it typically includes:
1.  A flag indicating if it is a list.
2.  The `ValueType`.
3.  The actual data (as a primitive or a list of values).

This ensures that the exact type and structure are preserved across different serialization formats (JSON, CBOR, etc.).

```kotlin
@Serializable
data class MyData(val param: Value)
```

## Manipulation

`Value` objects are immutable. Any operation that appears to modify a value (like converting a list) returns a new instance.

- **isNull()**: Check if the value is `Null`.
- **isList()**: Check if the value is a `ListValue` (or one of its variants).
- **toMeta()**: Wraps the `Value` into a single-node `Meta` object.
