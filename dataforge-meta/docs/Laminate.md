# Meta Layering and Laminate

Layering is a powerful concept in DataForge that allows combining multiple metadata sources into a single, unified view. 
This is essential for configuration management, where you might have default settings overridden by user preferences, 
which are in turn overridden by command-line arguments.

## The Laminate Class

A `Laminate` is a specialized implementation of `Meta` (specifically `TypedMeta<Laminate>`) that represents a stack of read-only metadata layers.

### Resolution Logic

When you request a value or a child node from a `Laminate`, it searches through its layers from top to bottom (first to last):

- **Value**: Returns the value from the first layer that contains it.
- **Child Node**: Returns a new `Laminate` containing the corresponding child nodes from all layers that have them.

This recursive structure ensures that layering works consistently at all levels of the metadata tree.

### Construction

You can create a `Laminate` using the factory functions:

```kotlin
val laminate = Laminate(topLayer, middleLayer, bottomLayer)
```

Adding layers to an existing `Laminate`:

```kotlin
val newLaminate = laminate.withTop(newLayer)
val withDefaults = laminate.withBottom(defaultLayer)
```

## Merging and Stacking

`Laminate` provides two primary strategies for collapsing multiple layers into a single `SealedMeta`:

### Stacking (top)

The `top()` method generates a meta tree where if a node is present in an upper layer, the corresponding nodes in lower layers are completely ignored, even if the upper node is empty.

```kotlin
val stackedMeta = laminate.top()
```

### Interweaving (merge)

The `merge()` method interweaves all layers. If a property or a child node is missing in a higher layer, it will be taken from a lower one. This is the most common way to handle defaults.

```kotlin
val mergedMeta = laminate.merge()
```

## Defaulting with `withDefault`

For simple cases where you only need one level of fallback, DataForge provides the `withDefault` extension function on `MetaProvider` and `MutableMetaProvider`.

### Immutable Defaults

```kotlin
val metaWithDefault = meta.withDefault(defaultMeta)
```

### Mutable Defaults

When used with `MutableMeta`, the `withDefault` function returns a `MutableMeta` view where:
- Reads fall back to the default if the value is missing in the source.
- Writes are always directed to the source `MutableMeta`, never to the default.

```kotlin
val mutableWithDefault = mutableMeta.withDefault(defaultMeta)
mutableWithDefault["key"] = "value" // This change goes to 'mutableMeta'
```

Multiple defaults can be chained using `withDefaults`:

```kotlin
val metaWithManyDefaults = mutableMeta.withDefaults(default1, default2)
```

## Use Cases

1.  **Hierarchical Configuration**: Combining system-wide, user-specific, and project-specific settings.
2.  **Plugin Properties**: `Context` properties are implemented as a `Laminate` of the context's own metadata and its parent's properties.
3.  **Data Processing**: Passing metadata through a chain of actions where each action can add its own metadata "layer" without modifying the original data's metadata.
4.  **Envelope Metadata**: `Envelope` objects can have multiple metadata layers added on top of the base metadata.
