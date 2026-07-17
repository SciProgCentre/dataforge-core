### Context Forking and Derivation

Creating new contexts or requesting new features is done via context forking. DataForge provides two main ways to handle
this: `buildContext` and `deriveContext`.

#### buildContext

The `buildContext` method always creates a new child context and registers it within the parent. This is used when an
explicit, isolated environment is required.

```kotlin
val newContext = context.buildContext(Name.parse("myChild")) {
    plugin(MyPluginFactory)
}
```

#### deriveContext

The `deriveContext` method is used for intelligent context management. Instead of always creating a new context, it
checks if a suitable context already exists:

1. **Self-check**: If the current context already satisfies the requirements (plugins and properties), it returns
   `this`.
2. **Child-check**: If not, it searches its direct children for a context that satisfies the requirements.
3. **Fallback**: If no suitable context is found, it calls `buildContext` to create a new one.

This mechanism ensures that the context hierarchy remains as shallow as possible, preventing "context explosion" during
frequent plugin requests.

### Context Properties and Inheritance

DataForge contexts are organized in a tree structure with `Global` at the root. This structure enables inheritance of
properties and plugins.

#### Properties Inheritance

Properties in a context work similarly to environment variables. When a property is requested, the system searches the
current context's metadata. If the property is not found, the search continues in the parent context, and so on, up to
the `Global` context.

This is implemented using a `Laminate`, which provides a layered view of metadata from the current context and its
ancestors.

#### Plugin Inheritance

Plugins are also inherited from parent contexts. When you look up a plugin in a context, its `PluginManager` first
checks locally loaded plugins. If the plugin is not found, it delegates the search to the parent context's
`PluginManager`.

This allows child contexts to reuse plugins and their configurations from parent contexts while retaining the ability to
override them with specialized versions if needed.

### Requesting a Plugin

The `request` extension function is the primary way to obtain a plugin. It uses `deriveContext` under the hood to ensure
the plugin is available with the requested configuration:

```kotlin
val myPlugin = context.request(MyPluginFactory, Meta { "someConfig" put true })
```

If the plugin with the given configuration is already present in the context or its parents, it is returned. Otherwise,
a suitable child context is either found or created.

### Gathering Objects with `gather`

The `gather` method is a powerful tool to collect all objects of a specific type provided by the context itself and all
its loaded plugins (including parent plugins).

```kotlin
val allTasks = context.gather<Task>(Task.TYPE)
```

Key characteristics of `gather`:

- **Plugin Integration**: It automatically scans all loaded plugins and collects the objects they provide via their
  `Provider` interface.
- **Namespacing**: To prevent name collisions, objects gathered from plugins are prefixed with the plugin's name.
- **Hierarchy Awareness**: By default, `gather` also collects objects from parent contexts (controlled by the `inherit`
  parameter).
- **Conflict Resolution**: If the same namespaced name appears in multiple places, `gather` will throw an error for
  direct conflicts, or prioritize the "closer" object when inheriting from parents.
