# Name

`Name` is a fundamental concept in DataForge used for hierarchical identification of objects, metadata nodes, and data items. It represents a structured path consisting of one or more `NameToken`s.

## Structure

A `Name` consists of an ordered list of `NameToken` objects. In its string representation, tokens are separated by dots (`.`).

### NameToken

A `NameToken` is the building block of a `Name`. It consists of:

- **Body**: A required string representing the token's identifier. It cannot be empty.
- **Index**: An optional string providing additional differentiation, typically used for elements in a list or collection.

Example string representations:
- `a`: A token with body "a" and no index.
- `a[1]`: A token with body "a" and index "1".
- `a[some.index]`: A token with body "a" and index "some.index".

### Escaping and Serialization

When a `Name` or `NameToken` is converted to a string, special characters that have structural meaning (`.`, `[`, `]`, `\`) are escaped using a backslash (`\`).

- `a.b` represents two tokens: `a` and `b`.
- `a\.b` represents a single token with the body "a.b".
- `a\[1\]` represents a single token with the body "a[1]" and no index.

## Construction

There are several ways to create `Name` objects depending on the use case.

### Parsing from String

The most common way to create a `Name` is by parsing a dot-separated string:

```kotlin
val name = Name.parse("data.sensor[123].value")
```

For performance-critical code where the same strings are parsed frequently, use the extension function with caching:

```kotlin
val name = "data.sensor[123].value".parseAsName(cache = true)
```

### Factory Methods and DSL

- `Name.of("a", "b", "c")`: Creates a name from raw strings (interpreted as token bodies without indices).
- `Name(token1, token2)`: Creates a name from existing `NameToken` objects.
- `Name.EMPTY`: Represents an empty name with zero tokens.

### Extension Properties

- `"a.b".asName()`: (Deprecated) Wraps the entire string into a single token body. Use `Name.of(this)` instead.
- `token.asName()`: Converts a single `NameToken` into a `Name` containing only that token.

## Manipulation

`Name` objects are immutable. Any operation that modifies a name returns a new `Name` instance.

### Concatenation

You can combine names, tokens, and strings using the `+` operator:

```kotlin
val base = Name.parse("data.sensor")
val full = base + "value" // data.sensor.value
val withIndex = base + NameToken("value", "1") // data.sensor.value[1]
```

### Transformation

- `name.cutFirst()`: Returns a new name with the first token removed.
- `name.cutLast()`: Returns a new name with the last token removed.
- `name.first()` / `name.last()`: Returns the first or last `NameToken`.
- `name.replaceLast { it.withIndex("2") }`: Returns a name with the last token modified.
- `name.withIndex("new")`: A shortcut to set or replace the index of the **last** token.

## Comparison and Matching

- `name.startsWith(other)`: Checks if the name begins with the tokens of another name or a specific token.
- `name.endsWith(other)`: Checks if the name ends with the tokens of another name or a specific token.
- `name.isEmpty()`: Returns `true` if the name has no tokens.
- `name.length`: Returns the number of tokens in the name.

### Pattern Matching

`Name` objects can be matched against `NamePattern` for advanced filtering and selection:

```kotlin
if (name.matches(NamePattern.ofName("data.*.value"))) {
    // ...
}
```

## Use Cases

1.  **Metadata Access**: `Name` is the standard way to address nodes and values in `Meta` trees.
2.  **Workspace Organization**: Identifying data, tasks, and results within a `Workspace` hierarchy.
3.  **Dependency Injection**: Using names as keys to look up plugins or services in a `Context`.
4.  **Event Subscription**: Filtering observable changes in metadata based on name prefixes or patterns.
