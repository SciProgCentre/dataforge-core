package space.kscience.dataforge.meta.descriptors

public object JsonSchema {
    public val VERSION: JsonSchemaVersion = JsonSchemaVersion.DRAFT_2020_12

    /**
     * A curated set of vocabularies defined for this JSON Schema dialect
     */
    public object Vocabularies {
        /**
         * A mandatory vocabulary that defines keywords that are either required in order to process any schema
         * or meta-schema, including those split across multiple documents, or exist to reserve keywords
         * for purposes that require guaranteed interoperability
         */
        public object Core {
            /**
             * This keyword declares an identifier for the schema resource
             */
            public const val ID: String = "\$id"

            /**
             * This keyword is both used as a JSON Schema dialect identifier and as a reference to a JSON Schema
             * which describes the set of valid schemas written for this particular dialect
             */
            public const val SCHEMA: String = "\$schema"

            /**
             * This keyword is used to reference a statically identified schema
             */
            public const val REF: String = "\$ref"

            /**
             * This keyword reserves a location for comments from schema authors to readers or maintainers of the schema
             */
            public const val COMMENT: String = "\$comment"

            /**
             * This keyword reserves a location for schema authors to inline re-usable JSON Schemas into a more general schema
             */
            public const val DEFS: String = "\$defs"

            /**
             * This keyword is used to create plain name fragments that are not tied to any particular structural location
             * for referencing purposes, which are taken into consideration for static referencing
             */
            public const val ANCHOR: String = "\$anchor"

            /**
             * This keyword is used to create plain name fragments that are not tied to any particular structural location
             * for referencing purposes, which are taken into consideration for dynamic referencing
             */
            public const val DYNAMIC_ANCHOR: String = "\$dynamicAnchor"

            /**
             * This keyword is used to reference an identified schema, deferring the full resolution until runtime,
             * at which point it is resolved each time it is encountered while evaluating an instance
             */
            public const val DYNAMIC_REF: String = "\$dynamicRef"

            /**
             * This keyword is used in dialect meta-schemas to identify the required and optional vocabularies
             * available for use in schemas described by that dialect
             */
            public const val VOCABULARY: String = "\$vocabulary"
        }

        /**
         * A vocabulary that defines applicator keywords that are recommended for use as the basis of other vocabularies
         */
        public object Applicator {
            /**
             * An instance validates successfully against this keyword if it validates successfully against all schemas
             * defined by this keyword's value
             */
            public const val ALL_OF: String = "allOf"

            /**
             * An instance validates successfully against this keyword if it validates successfully against at least one
             * schema defined by this keyword's value
             */
            public const val ANY_OF: String = "anyOf"

            /**
             * An instance validates successfully against this keyword if it validates successfully against exactly one
             * schema defined by this keyword's value
             */
            public const val ONE_OF: String = "oneOf"

            /**
             * This keyword declares a condition based on the validation result of the given schema
             */
            public const val IF: String = "if"

            /**
             * When if is present, and the instance successfully validates against its subschema, then validation succeeds
             * if the instance also successfully validates against this keyword's subschema
             */
            public const val THEN: String = "then"

            /**
             * When if is present, and the instance fails to validate against its subschema, then validation succeeds
             * if the instance successfully validates against this keyword's subschema
             */
            public const val ELSE: String = "else"

            /**
             * An instance is valid against this keyword if it fails to validate successfully against the schema
             * defined by this keyword
             */
            public const val NOT: String = "not"

            /**
             * Validation succeeds if, for each name that appears in both the instance and as a name within this keyword's
             * value, the child instance for that name successfully validates against the corresponding schema
             */
            public const val PROPERTIES: String = "properties"

            /**
             * Validation succeeds if the schema validates against each value not matched by other object applicators
             * in this vocabulary
             */
            public const val ADDITIONAL_PROPERTIES: String = "additionalProperties"

            /**
             * Validation succeeds if, for each instance name that matches any regular expressions that appear as a
             * property name in this keyword's value, the child instance for that name successfully validates against
             * each schema that corresponds to a matching regular expression
             */
            public const val PATTERN_PROPERTIES: String = "patternProperties"

            /**
             * This keyword specifies subschemas that are evaluated if the instance is an object and contains a certain property
             */
            public const val DEPENDENT_SCHEMAS: String = "dependentSchemas"

            /**
             * Validation succeeds if the schema validates against every property name in the instance
             */
            public const val PROPERTY_NAMES: String = "propertyNames"

            /**
             * Validation succeeds if the instance contains an element that validates against this schema
             */
            public const val CONTAINS: String = "contains"

            /**
             * Validation succeeds if each element of the instance not covered by prefixItems validates against this schema
             */
            public const val ITEMS: String = "items"

            /**
             * Validation succeeds if each element of the instance validates against the schema at the same position, if any
             */
            public const val PREFIX_ITEMS: String = "prefixItems"
        }

        /**
         * A vocabulary that defines keywords that impose requirements for successful validation of an instance
         */
        public object Validation {
            /**
             * Validation succeeds if the type of the instance matches the type represented by the given type,
             * or matches at least one of the given types
             */
            public const val TYPE: String = "type"

            /**
             * Validation succeeds if the instance is equal to one of the elements in this keyword's array value
             */
            public const val ENUM: String = "enum"

            /**
             * Validation succeeds if the instance is equal to this keyword's value
             */
            public const val CONST: String = "const"

            /**
             * A string instance is valid against this keyword if its length is less than, or equal to, the value of this keyword
             */
            public const val MAX_LENGTH: String = "maxLength"

            /**
             * A string instance is valid against this keyword if its length is greater than, or equal to, the value of this keyword
             */
            public const val MIN_LENGTH: String = "minLength"

            /**
             * A string instance is considered valid if the regular expression matches the instance successfully
             */
            public const val PATTERN: String = "pattern"

            /**
             * Validation succeeds if the numeric instance is less than the given number
             */
            public const val EXCLUSIVE_MAXIMUM: String = "exclusiveMaximum"

            /**
             * Validation succeeds if the numeric instance is greater than the given number
             */
            public const val EXCLUSIVE_MINIMUM: String = "exclusiveMinimum"

            /**
             * Validation succeeds if the numeric instance is less than or equal to the given number
             */
            public const val MAXIMUM: String = "maximum"

            /**
             * Validation succeeds if the numeric instance is greater than or equal to the given number
             */
            public const val MINIMUM: String = "minimum"

            /**
             * A numeric instance is valid only if division by this keyword's value results in an integer
             */
            public const val MULTIPLE_OF: String = "multipleOf"

            /**
             * Validation succeeds if, for each name that appears in both the instance and as a name within this keyword's
             * value, every item in the corresponding array is also the name of a property in the instance
             */
            public const val DEPENDENT_REQUIRED: String = "dependentRequired"

            /**
             * An object instance is valid if its number of properties is less than, or equal to, the value of this keyword
             */
            public const val MAX_PROPERTIES: String = "maxProperties"

            /**
             * An object instance is valid if its number of properties is greater than, or equal to, the value of this keyword
             */
            public const val MIN_PROPERTIES: String = "minProperties"

            /**
             * An object instance is valid against this keyword if every item in the array is the name of a property in the instance
             */
            public const val REQUIRED: String = "required"

            /**
             * An array instance is valid if its size is less than, or equal to, the value of this keyword
             */
            public const val MAX_ITEMS: String = "maxItems"

            /**
             * An array instance is valid if its size is greater than, or equal to, the value of this keyword
             */
            public const val MIN_ITEMS: String = "minItems"

            /**
             * The number of times that the contains keyword (if set) successfully validates against the instance must be
             * less than or equal to the given integer
             */
            public const val MAX_CONTAINS: String = "maxContains"

            /**
             * The number of times that the contains keyword (if set) successfully validates against the instance must be
             * greater than or equal to the given integer
             */
            public const val MIN_CONTAINS: String = "minContains"

            /**
             * If this keyword is set to the boolean value true, the instance validates successfully if all of its elements are unique
             */
            public const val UNIQUE_ITEMS: String = "uniqueItems"
        }

        /**
         * A vocabulary to defines general-purpose annotation keywords
         */
        public object MetaData {
            /**
             * A preferably short description about the purpose of the instance described by the schema
             */
            public const val TITLE: String = "title"

            /**
             * An explanation about the purpose of the instance described by the schema
             */
            public const val DESCRIPTION: String = "description"

            /**
             * This keyword can be used to supply a default JSON value associated with a particular schema
             */
            public const val DEFAULT: String = "default"

            /**
             * This keyword indicates that applications should refrain from using the declared property
             */
            public const val DEPRECATED: String = "deprecated"

            /**
             * This keyword is used to provide sample JSON values associated with a particular schema
             */
            public const val EXAMPLES: String = "examples"

            /**
             * This keyword indicates that the value of the instance is managed exclusively by the owning authority
             */
            public const val READ_ONLY: String = "readOnly"

            /**
             * This keyword indicates that the value is never present when the instance is retrieved
             */
            public const val WRITE_ONLY: String = "writeOnly"
        }

        /**
         * A vocabulary to defines semantic information about string-encoded values
         */
        public object FormatAnnotation {
            /**
             * Define semantic information about a string instance
             */
            public const val FORMAT: String = "format"
        }

        /**
         * A vocabulary for annotating instances that contain non-JSON data encoded in JSON strings
         */
        public object Content {
            /**
             * The string instance should be interpreted as encoded binary data
             */
            public const val CONTENT_ENCODING: String = "contentEncoding"

            /**
             * This keyword declares the media type of the string instance
             */
            public const val CONTENT_MEDIA_TYPE: String = "contentMediaType"

            /**
             * This keyword declares a schema which describes the structure of the string
             */
            public const val CONTENT_SCHEMA: String = "contentSchema"
        }

        /**
         * A vocabulary for applying subschemas to unevaluated array items or object properties
         */
        public object Unevaluated {
            /**
             * Validates array elements that did not successfully validate against other standard array applicators
             */
            public const val UNEVALUATED_ITEMS: String = "unevaluatedItems"

            /**
             * Validates object properties that did not successfully validate against other standard object applicators
             */
            public const val UNEVALUATED_PROPERTIES: String = "unevaluatedProperties"
        }

        public object Custom {
            /**
             * An index field by which this node is identified in case of same name siblings construct
             */
            public const val INDEX_KEY: String = "__indexKey__"

            /**
             * Additional attributes of this descriptor. For example, validation and widget parameters
             */
            public const val ATTRIBUTES: String = "__attributes__"

            /**
             * True if same name siblings with this name are allowed
             */
            public const val MULTIPLE: String = "__multiple__"
        }
    }

    public enum class JsonSchemaVersion(public val value: String) {
        /**
         * JSON Schema 2020-12 is a JSON media type for defining the structure of JSON data.
         * JSON Schema is intended to define validation, documentation, hyperlink navigation,
         * and interaction control of JSON data.
         * @see <a href="https://json-schema.org/draft/2020-12/json-schema-core.html">Specification</a>
         */
        DRAFT_2020_12("https://json-schema.org/draft/2020-12/schema")
    }
}