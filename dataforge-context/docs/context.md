# Context

Context is a central point in dependency injection and lifecycle management. Its extensibility is based on [plugins](plugins.md). It also serves as a `CoroutineScope` and manages lifecycle of the application. Closing a component context terminates it.

## Global Context

`Global` is a special context that is always available. It should not be used outside of tests. Similar to `GlobalScope` in coroutines, it deos not provide proper lifecycle management. `Global` could be closed, but it usually means termination of whole the application.

## Building a Context

In DataForge, the `Context` represents the environment where operations are executed. It manages plugins, properties, and coroutines. Contexts are organized in a hierarchy, with `Global` at the root.

### Basic Context Creation

The simplest way to create a context is to use the `Context` factory function:

```kotlin
val myContext = Context("my-context")
```

This creates a new child context of the `Global` context with the name `my-context`. If the context with the given name already exists, throw an exception.

### The Context Builder 

For more complex configurations, you can use the `ContextBuilder`. This is available through the `Context` factory function or by calling `buildContext` on an existing context.

```kotlin
val myContext = Context("my-context") {
    // Set a property
    properties {
        "myProperty" put 42
    }

    // Add a plugin by its factory
    plugin(MyPluginFactory) {
        // Plugin-specific configuration
        "pluginConfig" put "someValue"
    }

    // Configure the coroutine context
    coroutineContext(Dispatchers.Default)
}
```

### Properties

Properties in a context work like environment variables. They are inherited from the parent context. In the builder, you can set them using the `properties` block:

```kotlin
properties {
    "node" put {
        "value" put "something"
    }
}
```

### Plugins

Plugins provide the actual functionality to the context. You can add plugins to a context during construction:

- **By Factory**: The most common way. Allows providing configuration metadata.
  ```kotlin
  plugin(MyPluginFactory) {
      "key" put "value"
  }
  ```
- **By Tag**: If the factory is available in the parent context's registry.
  ```kotlin
  plugin("my-plugin", group = "my-group")
  ```
- **Existing Instance**: Add an already created plugin instance.
  ```kotlin
  plugin(myPluginInstance)
  ```

### Coroutine Context

Each `Context` is a `CoroutineScope`. You can customize its `CoroutineContext` during building:

```kotlin
coroutineContext(SupervisorJob() + Dispatchers.IO)
```

By default, a new context inherits the coroutine context from its parent and adds a `SupervisorJob`.

## Child Contexts

You can create child contexts from any existing context:

```kotlin
val child = parentContext.buildContext(Name.parse("child")) {
    // ...
}
```

Child context inherits all properties and plugins from their parent but could override them. For example, if a child provides a plugin with the same tag as a parent, a child plugin is used. In some cases child plugins could use some information from parent plugins. Parent context usually does not use child plugin information.

Closing parent context automatically closes all its child contexts.

`buildContext` always creates a new child context. If a child context with the same name already exists, it will be replaced in the parent's registry. 

To intelligently reuse existing contexts, use `deriveContext`. It returns the current context or an existing child if they already satisfy the requirements (requested plugins and properties). Otherwise, it calls `buildContext` to create a new one.

## Example: Complex Context Setup

```kotlin
val analysisContext = Context("analysis") {
    properties {
        "threads" put 4
    }
    
    plugin(LogManager)
    
    plugin(StoragePlugin) {
        "path" put "./data"
    }
}

val subTaskContext = analysisContext.buildContext(Name.parse("subtask")) {
    properties {
        "priority" put "high"
    }
}
```

In this example, `subTaskContext` will have access to the `LogManager` and `StoragePlugin` from its parent, as well as the `threads` property.
