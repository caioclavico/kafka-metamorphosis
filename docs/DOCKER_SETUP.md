# Kafka com Docker - Guia de Desenvolvimento

## 🐳 Configuração Rápida com Docker

O Kafka Metamorphosis inclui funções utilitárias para facilitar o desenvolvimento local usando Docker com **3 arquiteturas diferentes**:

### 🏗️ Escolha sua Arquitetura

#### **🆕 KRaft Mode (Recomendado) - Kafka sem Zookeeper**

```clojure
(require '[kafka-metamorphosis.dev :as dev])

;; Setup completo com KRaft (moderno, mais rápido)
(dev/kafka-setup-kraft!)

;; Ou apenas subir o Kafka
(dev/kafka-up-kraft!)
```

#### **⚡ Simple KRaft (Mais Rápido) - Minimal**

```clojure
;; Setup minimalista (sem UI, super rápido)
(dev/kafka-setup-simple!)

;; Ou apenas subir o Kafka
(dev/kafka-up-simple!)
```

#### **🏛️ Traditional Mode - Kafka + Zookeeper**

```clojure
;; Setup tradicional com Zookeeper
(dev/kafka-setup-zookeeper!)

;; Ou apenas subir o Kafka
(dev/kafka-up-zookeeper!)
```

### 🚀 Setup Genérico

```clojure
;; Método genérico - escolha o modo
(dev/kafka-dev-setup!)                    ; Padrão: Zookeeper
(dev/kafka-dev-setup! :kraft)             ; KRaft mode
(dev/kafka-dev-setup! :simple)            ; Simple KRaft
(dev/kafka-dev-setup! :kraft ["my-topic"]) ; KRaft com tópicos customizados
```

### 📊 Comparação dos Modos

| Modo          | Containers            | Startup   | UI  | Zookeeper | Uso                      |
| ------------- | --------------------- | --------- | --- | --------- | ------------------------ |
| **KRaft**     | 2 (Kafka + UI)        | Médio     | ✅  | ❌        | Desenvolvimento completo |
| **Simple**    | 1 (Kafka)             | ⚡ Rápido | ❌  | ❌        | Testes rápidos           |
| **Zookeeper** | 3 (Zook + Kafka + UI) | Lento     | ✅  | ✅        | Compatibilidade legacy   |

### 🔧 Controle Manual

```clojure
;; Subir Kafka
(dev/kafka-docker-up!)

;; Aguardar ficar pronto
(dev/wait-for-kafka)

;; Verificar status
(dev/kafka-docker-status)

;; Ver logs
(dev/kafka-docker-logs!)
(dev/kafka-docker-logs! "kafka" true)  ; Seguir logs do Kafka

;; Reiniciar serviços
(dev/kafka-docker-restart!)

;; Parar serviços
(dev/kafka-docker-down!)

;; Parar e remover volumes (limpa dados)
(dev/kafka-docker-down! true)
```

### 🧹 Limpeza Completa

```clojure
;; Parar tudo e manter dados
(dev/kafka-dev-teardown!)

;; Parar tudo e remover dados
(dev/kafka-dev-teardown! true)
```

## 📊 Serviços Incluídos

### Kafka Broker

- **Porta**: 9092 (principal)
- **Porta alternativa**: 29092
- **Endereço**: `localhost:9092`

### Zookeeper

- **Porta**: 2181
- **Endereço**: `localhost:2181`

### Kafka UI

- **Porta**: 8080
- **URL**: http://localhost:8080
- **Funcionalidades**:
  - Visualizar tópicos
  - Monitorar mensagens
  - Gerenciar consumidores
  - Ver métricas

## 🐳 Docker Compose Gerado

O arquivo `docker-compose.yml` inclui:

```yaml
version: "3.8"

services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: kafka-metamorphosis-zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: kafka-metamorphosis-kafka
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
      - "29092:29092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092,PLAINTEXT_HOST://localhost:29092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: true

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: kafka-metamorphosis-ui
    depends_on:
      - kafka
    ports:
      - "8080:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
```

## 🛠️ Workflow de Desenvolvimento

### 1. Primeira vez / Setup inicial

```clojure
;; Configurar ambiente completo
(dev/kafka-dev-setup!)
```

### 2. Desenvolvimento diário

```clojure
;; Verificar se está rodando
(dev/kafka-docker-status)

;; Se não estiver, subir
(dev/kafka-docker-up!)

;; Trabalhar com tópicos e mensagens...
(dev/setup-dev-topic "meu-projeto")
(dev/send-test-messages "meu-projeto" 10)
(dev/read-test-messages "meu-projeto")
```

### 3. Debug / Troubleshooting

```clojure
;; Ver logs do Kafka
(dev/kafka-docker-logs! "kafka")

;; Reiniciar se necessário
(dev/kafka-docker-restart! "kafka")

;; Verificar se consegue conectar
(dev/wait-for-kafka 30)
```

### 4. Fim do trabalho

```clojure
;; Parar mas manter dados
(dev/kafka-docker-down!)

;; Ou limpar tudo
(dev/kafka-dev-teardown! true)
```

## 🐛 Troubleshooting

### Kafka não sobe

```bash
# Verificar se Docker está rodando
docker --version

# Verificar portas ocupadas
netstat -an | findstr ":9092"
```

### Logs de erro

```clojure
;; Ver todos os logs
(dev/kafka-docker-logs!)

;; Ver logs específicos
(dev/kafka-docker-logs! "kafka")
(dev/kafka-docker-logs! "zookeeper")
```

### Limpar estado corrompido

```clojure
;; Parar tudo e remover volumes
(dev/kafka-dev-teardown! true)

;; Recriar do zero
(dev/kafka-dev-setup!)
```

## 🎯 Comandos Docker Manuais

Se precisar executar comandos Docker diretamente:

```bash
# Subir serviços
docker-compose up -d

# Ver status
docker-compose ps

# Ver logs
docker-compose logs kafka

# Parar serviços
docker-compose down

# Parar e remover volumes
docker-compose down -v
```

## 🦋 A Metamorfose Docker Está Completa!

Com essas funções, você pode facilmente gerenciar um ambiente Kafka local para desenvolvimento, transformando a complexidade de configuração em simplicidade funcional! 🪲➡️🦋
