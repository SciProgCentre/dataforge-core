# Meta Serialization and JSON

DataForge `Meta` is designed to be platform-independent and easily serializable. It provides full support for `kotlinx.serialization` 
and specialized conversion logic for JSON.

## Kotlinx Serialization

`Meta` and its variants are fully compatible with the `kotlinx.serialization` framework. This allows you to use `Meta` 
as part of your serializable data classes and exchange it using various formats like JSON, CBOR, or ProtoBuf.

### Serializers

The following serializers are provided:

- `MetaSerializer`: The default serializer for the `Meta` interface.
- `MutableMetaSerializer`: Used for `MutableMeta`.
- `ObservableMutableMetaSerializer`: Used for `ObservableMutableMeta`.

When using these serializers with formats other than JSON, `Meta` is serialized as a tree structure of `SealedMeta` objects.

```kotlin
@Serializable
data class MyConfiguration(
    val name: String,
    val options: Meta
)
```

## JSON Conversion

While standard serialization works, JSON is the most common format for `Meta`. DataForge provides specialized extension 
functions in the `space.kscience.dataforge.meta` package to convert between `Meta` and `kotlinx.serialization.json` elements.

### Basic Conversion

- `Meta.toJson(descriptor: MetaDescriptor? = null): JsonElement`: Converts a `Meta` node to a `JsonElement` (usually `JsonObject`).
- `JsonElement.toMeta(descriptor: MetaDescriptor? = null): SealedMeta`: Converts a `JsonElement` back to `Meta`.

When `MetaSerializer` is used with a `Json` format, it automatically uses these specialized conversion functions.

### The `@value` Key

Unlike JSON, a `Meta` node can simultaneously contain a `Value` and child items. To represent this in JSON, the `value` 
of the node is stored under the special `@value` key:

```json
{
  "node": {
    "@value": "nodeValue",
    "child": "childValue"
  }
}
```

If a `Meta` node only has a `Value` and no children, it is converted directly to a `JsonPrimitive` (or `JsonArray` for list values).

### The `@index` Key

`Meta` supports multiple child nodes with the same name but different indices (same name siblings). These are represented as a JSON array. 
If a node in such a list has a non-null index, it is stored within the object using the `@index` key:

```json
{
  "nodes": [
    {
      "@index": "first",
      "data": 1
    },
    {
      "@index": "second",
      "data": 2
    }
  ]
}
```

### The `@jsonArray` Key

When a raw JSON array (that does not represent a simple `ListValue`) is converted to `Meta`, the items are placed under 
a dummy key `@jsonArray` to maintain the tree structure.

## Customizing JSON Mapping

You can customize the keys used for values and indices by using a `MetaDescriptor`. This is useful when you need to match a specific existing JSON schema.

```kotlin
val descriptor = MetaDescriptor {
    node("items") {
        indexKey = "id" // Use "id" instead of "@index"
    }
}

val meta = jsonObject.toMeta(descriptor)
```

## Default String Representation

The `Meta.toString()` method (and `Meta.Companion.toString(meta)`) provides a default, pretty-printed JSON representation using `MetaSerializer`.

```kotlin
val meta = Meta { "a" put 1 }
println(meta.toString())
// Output:
// {
//     "a": 1
// }
```

## Use Cases

1.  **Configuration Files**: Saving and loading application settings in JSON or YAML format.
2.  **Web APIs**: Sending metadata as part of JSON responses in RESTful services.
3.  **Data Persistence**: Storing structured data in document-based databases.
4.  **Inter-process Communication**: Exchanging complex metadata between different modules or services.
