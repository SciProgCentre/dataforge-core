# Meta

`Meta` is a central concept in DataForge, representing a hierarchical tree-like structure of metadata. It is used for configuration, state representation, and data description.

## Purpose

The primary goal of `Meta` is to provide a flexible, platform-independent way to define structured information. Unlike rigid schemas, `Meta` allows for dynamic and extensible trees that can be easily serialized, layered, and manipulated.

Key characteristics:
- **Hierarchical**: Organized as a tree of nodes.
- **Polymorphic**: Each node can contain a `Value` and/or child nodes.
- **Immutable/Mutable**: Supports both read-only (`Meta`) and editable (`MutableMeta`) variants.
- **Layered**: Allows combining multiple metadata sources with defaults using [Laminate](Laminate.md).

## Structure

A `Meta` node consists of two main components:

1.  **Value**: An optional `Value` object representing the data content of the node itself.
2.  **Items**: A map of `NameToken` to child `Meta` nodes.

Because `NameToken` can include an index, `Meta` naturally supports both named properties and indexed lists of nodes.

### Leaf Nodes vs. Tree Nodes

- A **leaf node** typically contains a `Value` and has no children.
- A **tree node** typically contains child items and may or may not have its own `Value`.
- Unlike JSON, `Meta` allows mixed nodes that have both a `Value` and child items.

## Building Meta

DataForge provides a powerful Type-Safe DSL for constructing `Meta` objects.

### Using the DSL

The `Meta` and `MutableMeta` factory functions allow building trees using a concise syntax:

```kotlin
val meta = Meta {
    "property" put "value"
    "node" put {
        "innerProperty" put 42
        "innerList" putIndexed listOf(Meta(1), Meta(2))
    }
}
```

Common building operations:
- `put`: Adds a value or a node at a given name.
- `putIndexed`: Adds a collection of nodes with auto-generated indices.
- `setValue`: Sets a raw value at a path.

### Builders and Mutability

- `Meta { ... }`: Creates an immutable `SealedMeta`.
- `MutableMeta { ... }`: Creates a `MutableMeta` that can be modified later.
- `meta.toMutableMeta()`: Creates a mutable copy of an existing `Meta`.
- `meta.copy { ... }`: Creates a new `Meta` by applying modifications to a copy of the original.

### SealedMeta

`SealedMeta` is the standard immutable implementation of the `Meta` interface. When you use the `Meta { ... }` builder, it returns a `SealedMeta` instance.

#### Immutable vs Read-Only

It is important to distinguish between the `Meta` interface and its immutable implementations:

- **`Meta` (Read-Only)**: The `Meta` interface provides a read-only view of a metadata tree. However, a reference to a `Meta` object does not guarantee that the underlying data is immutable. For example, an `ObservableMutableMeta` implements `Meta`, but its content can change over time.
- **`SealedMeta` (Immutable)**: `SealedMeta` is an implementation that is guaranteed to be immutable. Once a `SealedMeta` object is created, it and all its children will never change.

#### The `seal()` Method

The `seal()` extension function allows you to "freeze" any `Meta` object by creating an immutable copy:

```kotlin
val sealed = meta.seal()
```

- If `meta` is already a `SealedMeta`, it returns the same instance (no copy is made).
- Otherwise, it performs a deep copy of the entire tree, converting all child nodes to `SealedMeta` objects.

This is particularly useful when:
1.  You receive a `Meta` object and want to ensure it won't be modified by the provider later.
2.  You want to "snapshot" the current state of a `MutableMeta`.
3.  You need to pass metadata to a component that requires a stable, unchanging configuration.

## Accessing Meta

Nodes and values can be accessed using `Name` or string paths:

```kotlin
val value: Value? = meta.getValue("node.innerProperty")
val node: Meta? = meta["node.innerList[0]"]
```

Extension properties provide typed access:
```kotlin
val intValue: Int? = meta["node.innerProperty"].int
```

## Advanced Features

### Defaulting and Layering

You can provide defaults to a `Meta` object using `withDefault`:

```kotlin
val withDefaults = meta.withDefault(defaultMeta)
```

In this case, if a property is missing in `meta`, it will be looked up in `defaultMeta`.

For more complex scenarios involving multiple layers of metadata, see the [Meta Layering and Laminate](Laminate.md) article.

### Observable Meta

`ObservableMutableMeta` allows subscribing to changes in the metadata tree, which is useful for reactive configurations and UI synchronization.

### Serialization

`Meta` is fully compatible with `kotlinx.serialization`. It can be converted to and from JSON, YAML, and other formats. For more details on JSON conversion, special keys, and custom mapping, see the [Meta Serialization and JSON](Serialization.md) article.

### Descriptors

`MetaDescriptor` allows you to define a schema for your metadata, providing validation, default values, and documentation. For more information, see the [MetaDescriptor](MetaDescriptor.md) article.

### Type-safe Access

While `Meta` is dynamic, you can use **Scheme**, **MetaRef**, and **MetaConverter** to provide type-safe wrappers, references, and conversions. See the [Meta Type Safety](meta%20type%20safety.md) article for details.

```kotlin
val jsonString = meta.toString() // Default JSON representation
```
