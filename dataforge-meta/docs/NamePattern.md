# NamePattern

`NamePattern` is a utility in DataForge for matching `Name` objects against specific rules. It is particularly useful for filtering, selecting items in hierarchical structures (like `Meta`), and defining rules that apply to groups of names.

## Structure

A `NamePattern` consists of a list of `NameTokenPattern` objects, each corresponding to a segment of a `Name`.

### NameTokenPattern Types

- `AnySingleToken`: Matches exactly one `NameToken`. Represented by `*` in string-based patterns.
- `AnyMultipleTokens`: Matches any number of `NameToken`s (including zero). Represented by `**` in string-based patterns.
- `Token`: Matches a specific `NameToken` based on its body and (optional) index.

### String Patterns

When matching the body or index of a `NameToken`, several types of `StringPattern` are used:

- **Exact**: Matches the string exactly (e.g., `a` matches only "a").
- **Regex**: Matches the string using a regular expression. This is triggered when the pattern starts with `^` and ends with `$`.
- **Any**: Matches any string. Represented by `*`.
- **Null**: Specifically used for indices to indicate that no index should be present.

## Construction

The most common way to create a `NamePattern` is using the `NamePattern.ofName(Name)` factory method. This method parses the tokens of the provided `Name` and converts them into pattern rules.

```kotlin
val pattern = NamePattern.ofName(Name.parse("a.b.*"))
```

### Wildcards

- **Single Token (`*`)**: Matches exactly one token.
  - `a.*.c` matches `a.b.c`, `a.d.c`, and even `a.b[1].c` (since the wildcard matches any body/index combination if not specified otherwise).
- **Multiple Tokens (`**`)**: Matches zero or more tokens.
  - `a.**` matches `a`, `a.b`, `a.b.c`, etc.
  - `**.c` matches `c`, `a.c`, `a.b.c`, etc.
  - **Restriction**: `**` is only supported at the beginning or at the end of a pattern. Using it in the middle will result in an error during matching.

### Regex Matching

You can use regular expressions for individual token segments by wrapping them in `^` and `$`:

- `a.^b.*$.c` matches any name starting with `a`, having a second token starting with `b`, and ending with `c`.
- `a[^[0-9]+$].b` matches token `a` with a numeric index, followed by `b`.

### Index Matching

Patterns can explicitly match indices:

- `a[1]`: Matches token `a` with index `1`.
- `a[*]`: Matches token `a` with any index (including no index).
- `a`: Matches token `a` with no index.

## Usage

The primary way to use a `NamePattern` is with the `matches` extension function:

```kotlin
val name = Name.parse("data.sensor[123].value")
val pattern = NamePattern.ofName(Name.parse("data.sensor[*].^val.*$"))

if (name.matches(pattern)) {
    println("Name matches the pattern!")
}
```

## Applications

`NamePattern` is used across DataForge in several scenarios:

1. **Metadata Filtering**: Selecting specific nodes from a `Meta` tree based on their names.
2. **Workspace Selection**: Finding data or tasks in a `Workspace` that match a specific naming convention.
3. **Data Subscription**: Listening for changes in an `ObservableMeta` only for names that match a certain pattern.
4. **Validation**: Ensuring that names provided in a configuration follow a required format.
