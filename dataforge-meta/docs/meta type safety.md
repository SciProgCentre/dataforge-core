# Type-safe Meta Access: Scheme, MetaRef, and MetaConverter

DataForge `Meta` is a dynamic, tree-like structure that is very flexible but can be prone to runtime errors due to typos in property names or incorrect type assumptions. To address this, DataForge provides three primary mechanisms for type-safe access: **Scheme**, **MetaRef**, and **MetaConverter**.

## Schemes

A `Scheme` is an object-oriented wrapper around a `MutableMeta` (or `Meta`). It allows you to define a set of properties that are mapped to specific metadata paths using Kotlin property delegates.

### Defining a Scheme

To create a scheme, inherit from the `Scheme` class and use delegate functions for its properties:

```kotlin
class ServerConfig : Scheme() {
    var host by string("localhost")
    var port by int(8080)
    var useSsl by boolean(false)

    companion object : SchemeSpec<ServerConfig>(::ServerConfig)
}
```

The `SchemeSpec` companion object is recommended as it provides factory methods and conversion logic.

### Using Schemes

You can create a new scheme instance or wrap an existing `MutableMeta`:

```kotlin
// Creating a new instance with DSL
val config = ServerConfig {
    host = "example.com"
    port = 443
    useSsl = true
}

// Wrapping existing meta
val meta = MutableMeta()
val wrapped = ServerConfig.write(meta)
wrapped.port = 9000 // This change is reflected in 'meta'
```

### Supported Delegates

`Scheme` provides a wide range of delegates:
- **Primitives**: `string()`, `int()`, `double()`, `boolean()`, `long()`, `float()`.
- **Enums**: `enum<MyEnum>()`.
- **Nested Schemes**: `scheme(OtherScheme)` for nested structures.
- **Lists**: `listOfScheme(OtherScheme)` for collections of nested schemes.
- **Values**: `value()` for raw `Value` access.
- **Serializable**: `serializable<MyData>()` for types supported by `kotlinx.serialization`.

## MetaRef

`MetaRef` (Metadata Reference) is a more lightweight, stateless approach to type safety. A `MetaRef<T>` represents a typed reference to a specific node in a `Meta` tree.

### Defining References

Typically, references are grouped within a `MetaSpec` object:

```kotlin
object ServerSpec : MetaSpec() {
    val host by string { 
        default("localhost")
        description = "Server hostname"
    }
    val port by int { 
        default(8080) 
    }
}
```

### Using References

References can be used to access or modify any `Meta` or `MutableMeta` object:

```kotlin
val meta = MutableMeta()

// Setting values
meta[ServerSpec.host] = "example.com"
meta[ServerSpec.port] = 443

// Getting values
val host: String? = meta[ServerSpec.host]
```

### Validation

One of the key advantages of `MetaSpec` is its ability to validate a `Meta` tree against the defined references:

```kotlin
val isValid = ServerSpec.validate(meta)
val results = ServerSpec.validateWithResult(meta)
```

## MetaConverter

`MetaConverter<T>` is an interface designed for bidirectional conversion between a custom type `T` and `Meta`. It extends `MetaReader<T>`, providing both reading and writing capabilities.

### Key Methods

- `readOrNull(source: Meta): T?`: Attempts to read an object of type `T` from the provided `Meta`.
- `convert(obj: T): Meta`: Converts an object of type `T` into a `Meta` structure.
- `descriptor: MetaDescriptor?`: An optional descriptor that defines the expected structure of the `Meta` produced or consumed by this converter.

### Standard Converters

The `MetaConverter` companion object provides several pre-defined converters for common types:

- **Primitives**: `string`, `boolean`, `number`, `double`, `float`, `int`, `long`.
- **Enums**: `enum<MyEnum>()` creates a converter for a specific enum type.
- **DataForge Types**: `meta` (identity), `value`, `name`.
- **Collections**: `stringList`, `valueList` (for custom list conversion).

### Serializable Converter

You can automatically create a `MetaConverter` for any class annotated with `@Serializable`:

```kotlin
@Serializable
data class MyData(val id: String, val value: Double)

val myDataConverter = MetaConverter.serializable<MyData>()

val meta = myDataConverter.convert(MyData("test", 1.0))
val obj = myDataConverter.read(meta)
```

This is particularly useful when you want to use existing serializable models within a `Meta`-based environment without manually wrapping them in a `Scheme`.

## Comparison and Use Cases

| Feature | Scheme | MetaRef | MetaConverter |
| :--- | :--- | :--- | :--- |
| **Paradigm** | Object-Oriented (Wrapper) | Functional (Reference) | Conversion (Bridge) |
| **State** | Stateful (holds a target meta) | Stateless | Stateless |
| **Mutability** | Mutates its internal target | Used to mutate any provided meta | Creates new Meta or object |
| **Overhead** | Higher (creates a wrapper object) | Lower (references are static) | Low (direct conversion) |
| **Best Use Case** | Configuration objects, complex domain models | Validation, lightweight access, static specs | DTO conversion, primitive mapping |

### When to use Scheme
- When you need a persistent object that represents a piece of configuration.
- When you want to provide a rich API on top of metadata (adding methods to the `Scheme` class).
- When you need to observe changes in a specific part of the metadata tree reactively.

### When to use MetaRef
- When you want to define a "vocabulary" for metadata without creating wrapper objects.
- When you need to validate metadata received from an external source.
- When you are performing one-off reads or writes to a large metadata tree.

### When to use MetaConverter
- When you need to convert between existing domain objects and `Meta`.
- When you want to treat `Meta` as a simple serialization format for a specific type.
- When working with primitives or simple collections in a generic way.
