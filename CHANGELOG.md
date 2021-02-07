# Changelog

## [Unreleased]
### Added
- Yaml meta format based on yaml.kt
- `Path` builders
- Special ValueType for lists
- `copy` method to descriptors
- Multiplatform yaml meta

### Changed
- `ListValue` and `DoubleArrayValue` implement `Iterable`.
- Changed the logic of `Value::isList` to check for type instead of size
- `Meta{}` builder made inline
- Moved `Envelope` builder to a top level function. Companion invoke is deprecated.
- Context logging moved to the extension
- `number` and `string` methods on `Value` moved to extensions (breaking change)
- \[Major breaking change\] Schemes and configurables us `MutableItemProvider` instead of `Config`
- \[Major breaking change\] `MetaItem` renamed to `TypedMetaItem` and `MetaItem` is now an alias for `TypedMetaItem<*>`
- \[Major breaking change\] Moved `NodeItem` and `ValueItem` to a top level
- Plugins are removed from Context constructor and added lazily in ContextBuilder
- \[Major breaking change\] Full refactor of DataTree/DataSource
- \[Major Breaking change\] Replace KClass with KType in data. Remove direct access to constructors with types.

### Deprecated

### Removed

### Fixed

### Security

## [0.2.0]
### Added

### Changed
- Context content resolution refactor
- Kotlin 1.4.10 (build tools 0.6.0)
- Empty query in Name is null instead of ""
- Provider provides an empty map instead of error by default
- Hidden delegates hierarchy in favor of stdlib properties
- Removed io depdendency from `dataforge-output`. Replaced Output by Appendable.
- Configurable is no longer MutableItemProvider. All functionality moved to Scheme.

### Deprecated
- Context activation API
- TextRenderer

### Removed
- Functional server prototype
- `dataforge-output` module

### Fixed
- Global context CoroutineScope resolution
- Library mode compliance

### Security
