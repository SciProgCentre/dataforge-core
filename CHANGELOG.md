# Changelog

## [Unreleased]
### Added
- Yaml meta format based on yaml.kt

### Changed
- `ListValue` and `DoubleArrayValue` implement `Iterable`.
- Changed the logic of `Value::isList` to check for type instead of size
- `Meta{}` builder made inline

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
