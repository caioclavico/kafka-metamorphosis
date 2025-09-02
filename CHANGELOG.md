# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]

## [0.3.0] - 2025-09-02
### Added - Schema Composition System 🔗
- **Schema Reference**: `schema-ref` function for referencing other registered schemas
- **Any-of Composition**: `any-of` function for OR logic validation (field matches any of the provided schemas)
- **All-of Composition**: `all-of` function for AND logic validation (field must match all provided schemas)
- **Nested Composition**: Support for complex nested schema composition patterns
- **Dynamic Schema Validation**: Conditional validation based on schema combinations
- **Advanced Composition Examples**: Real-world patterns for flexible user profiles, product catalogs, and organization structures

### Documentation
- **Schema Composition Guide**: Added comprehensive composition section to `docs/SCHEMA_VALIDATION.md`
- **Composition Examples**: Added `schema-composition-examples` and `advanced-composition-examples` functions
- **Updated API Reference**: Added composition functions documentation
- **Best Practices**: Guidelines for effective schema composition patterns

### Testing
- **Composition Test Suite**: 2 new test functions with comprehensive edge case coverage
- **Schema Reference Tests**: Validation of cross-schema references
- **Any-of Logic Tests**: OR composition validation scenarios
- **All-of Logic Tests**: AND composition validation scenarios
- **Edge Case Tests**: Non-existent schemas, empty compositions, and nested patterns

## [0.2.0] - 2025-09-01
### Added - Schema Validation System 🛡️
- **Custom Schema Validation**: Complete schema validation system without clojure.spec dependency
- **Schema Definition API**: `defschema` function for defining message schemas with simple predicate maps
- **Message Validation**: `validate-message` and `explain-validation` functions with detailed error reporting
- **Built-in Predicates**: `one-of`, `min-count`, `max-count`, `map-of` for common validation patterns
- **Complex Structure Support**: Nested maps, collections, and arrays with full validation
- **Custom Predicates**: Support for any function that returns boolean for validation
- **Kafka Integration**: Schema-validated producers and consumers with `send-schema-message!` and `consume-schema-messages!`
- **Error Path Reporting**: Precise field paths for validation errors (e.g., "user.address.zip-code")
- **Schema Registry**: In-memory schema storage and retrieval system
- **Performance Optimized**: Custom implementation without clojure.spec overhead

### Documentation
- **Schema Validation Guide**: Complete documentation at `docs/SCHEMA_VALIDATION.md`
- **Schema Examples**: Comprehensive examples in `src/kafka_metamorphosis/exemples/schema_examples.clj`
- **Updated README**: Added schema validation section and feature highlights
- **API Reference**: Complete schema validation API documentation
- **Best Practices**: Schema organization, versioning, and performance guidelines

### Testing
- **Schema Test Suite**: 10 new tests with 36 assertions covering all schema functionality
- **Complex Validation Tests**: Nested structures, collections, and error handling
- **Custom Predicate Tests**: Validation of user-defined predicates
- **Integration Tests**: Kafka producer/consumer integration with schema validation
- **Error Explanation Tests**: Detailed validation error reporting verification

### Examples
- **Basic Schemas**: Simple field validation examples
- **E-commerce Schemas**: Complex order, cart, and transaction validation
- **Event Logging**: System event and audit log schema examples
- **User Profiles**: Nested user data with preferences and addresses
- **Custom Predicates**: Email, CPF, and domain-specific validation examples

### Breaking Changes
- **Schema Module**: New `kafka-metamorphosis.schema` namespace
- **Validation Functions**: New API for message validation (non-breaking, additive)

### Summary
Version 0.2.0 introduces a powerful schema validation system that provides type safety and message integrity for Kafka applications. This major feature addition includes comprehensive documentation, examples, and full integration with existing Kafka operations. The schema system offers better performance than clojure.spec while maintaining simplicity and flexibility.

## [0.1.0] - 2025-08-28
### Added
- **Core Library**: Complete Kafka wrapper with idiomatic Clojure APIs
- **Producer Module**: High-level and low-level producer functions with sync/async support
- **Consumer Module**: Consumer groups, manual offset management, partition assignment
- **Admin Module**: Topic creation, deletion, listing, and cluster management
- **Serializers Module**: Support for String, JSON, Avro, and Protobuf serialization
- **Development Utilities**: Docker setup helpers and development topic management
- **High-level APIs**: `send-message!`, `consume-messages`, `create-topic!`, `health-check`
- **Configuration Builders**: `producer-config`, `consumer-config`, `admin-config` with sensible defaults
- **JSON Support**: Built-in JSON serialization/deserialization utilities
- **Error Handling**: Comprehensive error handling with meaningful messages

### Documentation
- **Complete README**: Usage examples, API reference, and installation instructions
- **Docker Setup Guide**: Multiple Docker architectures (KRaft, Simple, Zookeeper)
- **KRaft Mode Guide**: Modern Kafka setup without Zookeeper
- **Topic Creation Guide**: Comprehensive topic management examples
- **API Documentation**: Detailed function documentation with examples
- **Development Workflow**: Docker-based development environment setup

### Testing
- **Comprehensive Test Suite**: 21 tests covering all major namespaces
- **Core Tests**: Configuration and high-level API validation
- **Serializers Tests**: All serialization format testing
- **Producer Tests**: Producer utilities and configuration testing
- **Admin Tests**: Administrative function testing
- **85 Assertions**: Complete coverage of public APIs

### Deployment
- **Clojars Publication**: Available as `org.clojars.caioclavico/kafka-metamorphosis`
- **Maven Support**: Full Maven/deps.edn compatibility
- **AOT Compilation**: Optimized JAR with main class support
- **Deploy Scripts**: Automated deployment pipeline to Clojars

### Infrastructure
- **Leiningen Project**: Complete project.clj with dependencies and deployment config
- **Git Integration**: SCM configuration for Maven deployment
- **Developer Info**: Contributor and license information
- **Build Tools**: Test runner, compilation checker, and deployment scripts

### Refactoring
- **Namespace Separation**: Moved serializers to dedicated namespace
- **Code Organization**: Clean separation of concerns across modules
- **Function Signatures**: Consistent parameter ordering and naming
- **Documentation**: Comprehensive docstrings for all public functions

### International Support
- **English Documentation**: All guides and documentation translated to English
- **Global Accessibility**: Ready for international open-source community
- **Consistent Terminology**: Standardized technical terms across all docs

[Unreleased]: https://github.com/caioclavico/kafka-metamorphosis/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/caioclavico/kafka-metamorphosis/releases/tag/v0.2.0
[0.1.0]: https://github.com/caioclavico/kafka-metamorphosis/releases/tag/v0.1.0
