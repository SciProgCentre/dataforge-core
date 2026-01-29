### Context Serialization Feature

DataForge Context supports integrated serialization via `kotlinx.serialization`. This allows each `Context` to have its own `SerializersModule` and `Json` instance, automatically configured based on the plugins loaded into that context.

#### Plugin Serializers

The `Plugin` interface includes an optional `serializerModule` property:

```kotlin
public interface Plugin : Named, ContextAware, Provider, MetaRepr {
    // ...
    public val serializerModule: SerializersModule? get() = null
}
```

Plugins can provide their own serialization rules (e.g., polymorphic subclasses) by overriding this property.

#### Context Serialization Module

Each `Context` has a `serializationModule` property that aggregates serialization rules from:
1. **Plugins**: All `serializerModule`s from plugins attached to the current context are combined.
2. **Parent Context**: If the current context has a parent, the parent's `serializationModule` is included.

**Conflict Resolution:**
- Rules from plugins in the current context **overwrite** rules from the parent context.
- If multiple plugins in the same context provide conflicting rules for the same type, a `SerializerAlreadyRegisteredException` is thrown when accessing the module.

#### Context JSON

A `Context` provides a pre-configured `Json` instance:

```kotlin
public val json: Json by lazy {
    Json {
        prettyPrint = meta["json.prettyPrint"]?.boolean ?: true
        serializersModule = serializationModule
    }
}
```

The `prettyPrint` setting can be configured via context metadata using the `json.prettyPrint` key.