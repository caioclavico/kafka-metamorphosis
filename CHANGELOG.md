# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]

## [0.1.0-SNAPSHOT] - 2025-08-28
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

[Unreleased]: https://github.com/caioclavico/kafka-metamorphosis/compare/v0.1.0-SNAPSHOT...HEAD
[0.1.0-SNAPSHOT]: https://github.com/caioclavico/kafka-metamorphosis/releases/tag/v0.1.0-SNAPSHOT
