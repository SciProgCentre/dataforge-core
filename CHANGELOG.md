# Changelog

## Unreleased

### Added
- Coroutine exception logging in context
- ObservableMutableMetaSerializer

### Changed
- Simplify inheritance logic in `MutableTypedMeta`
- Full rework of `DataTree` and associated interfaces (`DataSource`, `DataSink`, etc).

### Deprecated
- MetaProvider `spec` is replaced by `readable`. `listOfSpec` replaced with `listOfReadable`

### Removed

### Fixed
- Fixed NameToken parsing.
- Top level string list meta conversion.

### Security

## 0.9.0 - 2024-06-04

### Added

- Custom CoroutineContext during `Context` creation.

### Changed

- Kotlin 2.0
- `MetaSpec` renamed to `MetaReader`. MetaSpec is now reserved for builder-based generation of meta descriptors.
- Add self-type for Meta. Remove unsafe cast method for meta instances.

### Removed

- Automatic descriptors for schema. It is not possible to implement them without heavy reflection.

## 0.8.2 - 2024-04-27

### Added

- Name index comparator
- Specialized ByteArrayValue

### Changed

- DataSink `branch` is replaced with `putAll` to avoid confusion with DataTree methods
- Meta delegate now uses a specific class that has a descriptor

### Fixed

- `listOfScheme` and `listOfConvertable` delegates provides correct items order.
- Scheme meta setter works with proper sub-branch.
- NameToken.parse improper work with indices.
- Proper data handling for cache.

## 0.8.0 - 2024-02-03

### Added

- Wasm artifacts
- Add automatic MetaConverter for serializeable objects
- Add Meta and MutableMeta delegates for convertable and serializeable
- Meta mapping for data.

### Changed

- Descriptor `children` renamed to `nodes`
- `MetaConverter` now inherits `MetaSpec` (former `Specifiction`). So `MetaConverter` could be used more universally.
- Meta copy and modification now use lightweight non-observable meta builders.
- Full refactor of Data API. DataTree now works similar to Meta: contains optional anonymous root element and data items. Updates are available for `ObservaleDataSource` and `ObservableDataTree` variants.

### Deprecated

- `node(key,converter)` in favor of `serializable` delegate

### Fixed

- Partially fixed a bug with `MutableMeta` observable wrappers.
- `valueSequence` now include root value. So `meta.update` works properly.

## 0.7.0 - 2023-11-26

### Added

- Obligatory `type: KType` and `descriptor` property for `MetaConverters`
- Added separate `Meta`, `SealedMeta` and `ObservableMutableMeta` builders.

### Changed

- Meta converter `metaToObject` returns a non-nullable type. Additional method `metaToObjectOrNull` for nullable return.
- Kotlin 1.9.20.
- Migrated from ktor-io to kotlinx-io.
- `MutableMeta` builder now returns a simplified version of meta that does not hold listeners.
- More concise names for read/write methods in IO.
- Remove unnecessary confusion with `get`/`getMeta` by removing `getMeta` from the interface.

### Deprecated

- `String.parseValue` is replaced with `Value.parse`

### Fixed

- Memory leak in SealedMeta builder

## 0.6.2 - 2023-07-29

### Changed

- Meta to Json serializer now serializes a single item with index as an array. It is important for plotly integration.
- Meta to Json serializes Meta without children a value as literal or array instead of an object with `@value` field.

## 0.6.1 - 2023-03-31

### Added

- File cache for workspace
- Smart task metadata transformation for workspace
- Add `readOnly` property to descriptors
- Add `specOrNull` delegate to meta and Scheme
- Suspended read methods to the `Binary`
- Synchronously accessed `meta` to all `DataSet`s
- More fine-grained types in Action builders.

### Changed

- `Name::replaceLast` API
- `PluginFactory` no longer requires plugin class
- Collection<Named> toMap -> associateByName
- Simplified `DFTL` envelope format. Closing symbols are unnecessary. Properties are discontinued.
- Meta `get` method allows nullable receiver
- `withDefault` functions do not add new keys to meta children and are consistent.
- `dataforge.meta.values` package is merged into `dataforge.meta` for better star imports
- Kotlin 1.8.20
- `Factory` is now `fun interface` and uses `build` instead of `invoke`. `invoke moved to an extension.
- KTor 2.0
- DataTree `items` call is blocking.
- DataSet `getData` is no longer suspended and renamed to `get`
- DataSet operates with sequences of data instead of flows
- PartialEnvelope uses `Int` instead `UInt`.
- `ActiveDataSet` renamed to `DataSource`
- `selectOne`->`getByType`
- Data traversal in `DataSet` is done via iterator
- Remove all unnecessary properties for `IOFormat`
- Separate interfaces for `IOReader` and `IOWriter`

### Deprecated

- Context.fetch -> Context.request

### Fixed

- `readDataDirectory` does not split names with dots
- Front matter reader does not crash on non-UTF files
- Meta file name in readMeta from directory
- Tagless and FrontMatter envelope partial readers fix.

## 0.5.2

### Added

- Yaml plugin
- Partial fix to #53

### Fixed

- MutableMetaImpl attachment and checks
- Listeners in observable meta are replaced by lists
- JS number comparison bug.

## 0.5.0

### Added

- Experimental `listOfSpec` delegate.

### Changed

- **API breaking** Config is deprecated, use `ObservableMeta` instead.
- **API breaking** Descriptor no has a member property `defaultValue` instead of `defaultItem()` extension. It caches default value state on the first call. It is done because computing default on each call is too expensive.
- Kotlin 1.5.10
- Build tools 0.10.0
- Relaxed type restriction on `MetaConverter`. Now nullables are available.
- **Huge API-breaking refactoring of Meta**. Meta now can have both value and children. There is only one kind of descriptor now.
- **API breaking** `String.toName()` is replaced by `Name.parse()`
- **API breaking** Configurable`config` changed to `meta`

### Removed

- `Config`
- Public PluginManager mutability
- Tables and tables-exposed moved to the separate project `tables.kt`
- BinaryMetaFormat. Use CBOR encoding instead

### Fixed

- Proper json array index treatment.
- Proper json index for single-value array.

## 0.4.0

### Added

- LogManager plugin
- dataforge-context API dependency on SLF4j
- Context `withEnv` and `fetch` methods to manipulate plugins without changing plugins after creation.
- Split `ItemDescriptor` into builder and read-only part

### Changed

- Kotlin-logging moved from common to JVM and JS. Replaced by console for native.
- Package changed to `space.kscience`
- Scheme made observable
- Global context is a variable (the singleton is hidden and will be deprecated in future)
- Kotlin 1.5
- Added blank builders for children context.
- Refactor loggers

### Deprecated

- Direct use of PluginManager

### Removed

- Common dependency on Kotlin-logging
- Kotlinx-io fork dependency. Replaced by Ktor-io.

### Fixed

- Scheme properties properly handle children property change.

## 0.3.0

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

## 0.2.0

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
