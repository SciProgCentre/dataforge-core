# DataForge Plugin Mechanics

Plugins are the primary way to extend the functionality of a DataForge `Context`. They provide runtime features, services, and configurations that can be shared across different tasks executed within a context.

## The Plugin Interface

A plugin in DataForge is defined by the `Plugin` interface. Every plugin must have:
- A `PluginTag`: Contains the `name`, `group`, and `version` of the plugin.
- A `Meta` configuration: Allows the plugin to be configured.
- A reference to the `Context` it is attached to (via `ContextAware`).

```kotlin
public interface Plugin : Named, ContextAware, Provider, MetaRepr {
    public val tag: PluginTag
    public val meta: Meta
    public fun dependsOn(): Map<PluginFactory<*>, Meta>
    public fun attach(context: Context)
    public fun detach()
    public val isAttached: Boolean
    // ...
}
```

## Plugin Lifecycle

The lifecycle of a plugin consists of the following stages:

1. **Create**: The plugin instance is created and configured, usually via a `PluginFactory`.
2. **Attach**: The plugin is attached to a `Context`. This is when the plugin can register its services in the context and initialize itself. The `attach(context)` method is called by the `PluginManager`.
3. **Detach**: The plugin is removed from the `Context`. It should clean up any resources and registrations.
4. **Destroy**: The plugin instance is discarded.

## Plugin Identification (PluginTag)

Plugins are identified by a `PluginTag`, which consists of:
- `name`: The unique name of the plugin within its group.
- `group`: (Optional) The organization or project the plugin belongs to.
- `version`: (Optional) The version of the plugin. Version could be used for remote plugin management and for consistent result storage.

The `PluginManager` uses these tags to find and manage plugins, ensuring that no two plugins with the same tag are loaded into the same context.

## Dependency Management

Plugins can declare dependencies on other plugins. Before a plugin is attached, all its dependencies must be present and attached to the context (or its parents).

### Declaring Dependencies
In `AbstractPlugin`, dependencies are declared using the `require` function:

```kotlin
class MyPlugin(meta: Meta) : AbstractPlugin(meta) {
    // Declares a dependency on AnotherPlugin
    val anotherPlugin by require(AnotherPluginFactory)
}
```

The `require` function returns a delegate that lazily fetches the dependent plugin from the context once the plugin is attached.

## Plugin Manager

Each `Context` has a `PluginManager` that:
- Stores attached plugins.
- Manages the plugin lifecycle.
- Handles plugin lookups (including searching in parent contexts).

### Requesting a Plugin
You can request a plugin from a context using the `request` extension function:

```kotlin
val myPlugin = context.request(MyPluginFactory)
```

If the plugin is already present in the context or its parents, it is returned. Otherwise, a new child context is created with the requested plugin.

## Plugin Factories

Plugins are typically created using a `PluginFactory`. This allows the framework to instantiate plugins dynamically based on metadata or explicit requests. Also it allows using type-safe reference to plugins via their types and dependency injection.

```kotlin
public interface PluginFactory<T : Plugin> : Factory<T> {
    public val tag: PluginTag
    override fun build(context: Context, meta: Meta): T
}
```