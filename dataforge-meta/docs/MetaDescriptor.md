# MetaDescriptor

`MetaDescriptor` is a schema-like structure that defines constraints, requirements, and metadata for a `Meta` tree.
It acts as a contract for what a metadata node should contain, enabling validation, documentation, and automated UI
generation.

## Key Properties

A `MetaDescriptor` can specify the following for any node in a `Meta` tree:

- **description**: A human-readable description of the node's purpose.
- **nodes**: A map of child descriptors, defining the expected nested structure.
- **valueRestriction**: Defines whether a value is `NONE` (optional), `REQUIRED`, or `ABSENT` (prohibited).
- **valueTypes**: A list of allowed `ValueType`s (e.g., `NUMBER`, `STRING`, `BOOLEAN`).
- **defaultValue**: A default `Value` to be used if the node is missing.
- **multiple**: A boolean indicating if multiple nodes with the same name (indexed items) are allowed.
- **indexKey**: The name of the field used to identify indexed nodes (defaults to `@index`).
- **attributes**: A `Meta` object containing arbitrary additional metadata for the descriptor (e.g., UI hints,
  validation ranges).

## Building Descriptors

DataForge provides a Type-Safe DSL for constructing `MetaDescriptor` objects, similar to the `Meta` builder.

```kotlin
val descriptor = MetaDescriptor {
    description = "Application configuration"

    node("server") {
        description = "Server settings"

        value("port", ValueType.NUMBER) {
            description = "The port to listen on"
            default(8080)
            required()
        }

        value("host", ValueType.STRING) {
            default("localhost")
        }
    }

    node("database") {
        multiple = true
        description = "List of database connections"
        // ...
    }
}
```

## Validation

You can use a descriptor to validate a `Meta` tree:

- `descriptor.validate(meta)`: Returns `true` if the meta tree adheres to the descriptor.
- `descriptor.validateWithResult(meta)`: Returns a sequence of `MetaValidationResult` providing detailed information
  about any discrepancies (e.g., `RequiredValueIsMissing`, `IncorrectValueType`).

## Integration with kotlinx.serialization

`MetaDescriptor` can be automatically generated from `kotlinx.serialization` descriptors. This allows you to derive a
metadata schema directly from your Kotlin data classes.

```kotlin
@Serializable
data class Config(val port: Int, val host: String = "localhost")

val descriptor = MetaDescriptor(Config.serializer())
```

This bridge ensures that your `Meta` structures stay in sync with your serializable models.

## Connection to JSON Schema

`MetaDescriptor` is designed to be compatible with [JSON Schema](https://json-schema.org/). DataForge provides
bidirectional conversion:

- `metaDescriptor.toJsonSchema()`: Converts a `MetaDescriptor` to a JSON Schema `JsonObject`.
- `jsonObject.toMetaDescriptor()`: Parses a JSON Schema into a `MetaDescriptor`.

The conversion handles standard JSON Schema vocabularies (type, enum, required, properties, etc.) and uses custom keys
for DataForge-specific features like `indexKey` and `multiple`.

## Use Cases

1. **Validation**: Ensuring that configuration files or API responses have the correct structure before processing.
2. **UI Generation**: Automatically building configuration forms or property editors based on descriptors.
3. **Documentation**: Generating human-readable documentation or hover tooltips for metadata fields.
4. **Defaulting**: Providing a complete "default" meta tree derived from the descriptor's default values.
