# KRaft Mode - Kafka sem Zookeeper

## 🆕 O que é KRaft?

KRaft (Kafka Raft) é a nova arquitetura do Apache Kafka que **elimina a dependência do Zookeeper**. Disponível desde Kafka 2.8 e estável desde 3.3.

### ✅ Vantagens do KRaft

- **🚀 Startup mais rápido** - Menos componentes para inicializar
- **📦 Menos complexidade** - Um componente a menos para gerenciar
- **🔧 Configuração simplificada** - Sem necessidade de configurar Zookeeper
- **⚡ Melhor performance** - Metadata gerenciado diretamente pelo Kafka
- **🎯 Futuro do Kafka** - Zookeeper será descontinuado

### ❌ Limitações Atuais

- **🧪 Relativamente novo** - Menos tempo em produção
- **📚 Menos documentação** - Comunidade ainda migrando
- **🔌 Algumas ferramentas** - Podem ainda não suportar completamente

## 🚀 Como Usar no Kafka Metamorphosis

### Modo KRaft Completo

```clojure
;; Setup completo com KRaft + Kafka UI
(dev/kafka-setup-kraft!)

;; Ou com tópicos específicos
(dev/kafka-setup-kraft! ["orders" "payments" "notifications"])

;; Apenas subir sem criar tópicos
(dev/kafka-up-kraft!)
```

### Modo KRaft Simples (Recomendado para Testes)

```clojure
;; Setup minimalista - super rápido
(dev/kafka-setup-simple!)

;; Apenas subir Kafka sem UI
(dev/kafka-up-simple!)
```

## 🏗️ Arquitetura KRaft vs Zookeeper

### Tradicional (Zookeeper)

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  Zookeeper  │    │    Kafka    │    │  Kafka UI   │
│   :2181     │◄──►│   :9092     │◄──►│   :8080     │
└─────────────┘    └─────────────┘    └─────────────┘
```

### KRaft Mode

```
┌─────────────┐    ┌─────────────┐
│    Kafka    │    │  Kafka UI   │
│   :9092     │◄──►│   :8080     │
│ (+ metadata)│    └─────────────┘
└─────────────┘
```

### Simple KRaft

```
┌─────────────┐
│    Kafka    │
│   :9092     │
│ (+ metadata)│
└─────────────┘
```

## ⚙️ Configurações Geradas

### KRaft Completo (`kraft-docker-compose`)

```yaml
services:
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: "broker,controller"
      KAFKA_CONTROLLER_QUORUM_VOTERS: "1@localhost:29093"
      # ... sem Zookeeper

  kafka-ui:
    # Interface web em localhost:8080
```

### KRaft Simples (`kraft-simple-docker-compose`)

```yaml
services:
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    ports:
      - "9092:9092"
    environment:
      KAFKA_PROCESS_ROLES: "broker,controller"
      # ... configuração minimal
```

## 🛠️ Workflow Recomendado

### Para Desenvolvimento Diário

```clojure
;; 1. Setup rápido para desenvolvimento
(dev/kafka-setup-kraft!)

;; 2. Usar normalmente
(dev/send-test-messages "meu-topico" 5)
(dev/read-test-messages "meu-topico")

;; 3. Abrir UI no navegador: http://localhost:8080

;; 4. Parar quando terminar
(dev/kafka-dev-teardown!)
```

### Para Testes Rápidos

```clojure
;; 1. Setup minimalista (sem UI)
(dev/kafka-setup-simple!)

;; 2. Testar rapidamente
(dev/setup-dev-topic "test")
(dev/send-test-messages "test" 3)

;; 3. Parar
(dev/kafka-dev-teardown!)
```

## 🔧 Troubleshooting KRaft

### Problema: Kafka não inicia

```clojure
;; Ver logs específicos
(dev/kafka-docker-logs! "kafka")

;; Verificar se portas estão livres
;; 9092 (Kafka), 29093 (Controller)
```

### Problema: Não consegue conectar

```clojure
;; Aguardar inicialização completa
(dev/wait-for-kafka 60)

;; Verificar status
(dev/kafka-docker-status)
```

### Reset Completo

```clojure
;; Parar e limpar tudo
(dev/kafka-dev-teardown! true)

;; Recriar do zero
(dev/kafka-setup-kraft!)
```

## 🎯 Recomendações

### Use KRaft Quando:

- ✅ Desenvolvimento local
- ✅ Testes automatizados
- ✅ Projetos novos
- ✅ Quer startup mais rápido

### Use Zookeeper Quando:

- ⚠️ Ambiente de produção crítico
- ⚠️ Ferramentas legacy que não suportam KRaft
- ⚠️ Conformidade com setup existente

## 🦋 Migração do Futuro

O Kafka Metamorphosis facilita a migração:

```clojure
;; Atualmente usando Zookeeper
(dev/kafka-setup-zookeeper!)

;; Migrar para KRaft (mesma API)
(dev/kafka-setup-kraft!)

;; Código da aplicação permanece o mesmo!
```

A metamorfose do Kafka elimina a complexidade do Zookeeper! 🪲➡️🦋
