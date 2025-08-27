# Guia de Criação de Tópicos - Kafka Metamorphosis

## 🪲 Como Criar Tópicos

### Método 1: Usando o namespace admin (Recomendado)

```clojure
(require '[kafka-metamorphosis.admin :as admin])

;; Criar um cliente admin
(def admin-client (admin/create-admin-client {:bootstrap-servers "localhost:9092"}))

;; Criar um tópico simples
(admin/create-topic! admin-client "meu-topico")

;; Criar um tópico com configurações específicas
(admin/create-topic! admin-client "topico-avancado"
                     {:partitions 5
                      :replication-factor 1
                      :configs {"cleanup.policy" "compact"
                               "retention.ms" "86400000"}})

;; Verificar se um tópico existe
(admin/topic-exists? admin-client "meu-topico")

;; Criar apenas se não existir
(admin/create-topic-if-not-exists! admin-client "topico-seguro")

;; Listar todos os tópicos
(admin/list-topics admin-client)

;; Obter detalhes de um tópico
(admin/describe-topic admin-client "meu-topico")

;; Fechar o cliente
(admin/close! admin-client)
```

### Método 2: Usando utilitários de desenvolvimento

```clojure
(require '[kafka-metamorphosis.dev :as dev])

;; Criar um tópico de desenvolvimento (3 partições, replicação 1)
(dev/setup-dev-topic "topico-dev")

;; Criar com opções customizadas
(dev/setup-dev-topic "topico-custom" {:partitions 10})

;; Listar todos os tópicos
(dev/list-all-topics)

;; Descrever um tópico
(dev/describe-dev-topic "topico-dev")

;; Deletar um tópico (com confirmação)
(dev/delete-dev-topic "topico-dev")

;; Ver informações do cluster
(dev/cluster-info)
```

### Método 3: Linha de Comando (Kafka Tools)

Se você preferir usar as ferramentas nativas do Kafka:

```bash
# Criar um tópico
kafka-topics.sh --create --topic meu-topico --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1

# Listar tópicos
kafka-topics.sh --list --bootstrap-server localhost:9092

# Descrever um tópico
kafka-topics.sh --describe --topic meu-topico --bootstrap-server localhost:9092

# Deletar um tópico
kafka-topics.sh --delete --topic meu-topico --bootstrap-server localhost:9092
```

## 📊 Configurações Comuns de Tópicos

### Tópico para Logs (Retenção por Tempo)

```clojure
(admin/create-topic! admin-client "logs-topic"
                     {:partitions 6
                      :configs {"retention.ms" "604800000"        ; 7 dias
                               "cleanup.policy" "delete"
                               "segment.ms" "86400000"}})        ; 1 dia
```

### Tópico Compactado (para Estado)

```clojure
(admin/create-topic! admin-client "state-topic"
                     {:partitions 3
                      :configs {"cleanup.policy" "compact"
                               "min.cleanable.dirty.ratio" "0.1"
                               "delete.retention.ms" "86400000"}})
```

### Tópico de Alto Throughput

```clojure
(admin/create-topic! admin-client "high-throughput-topic"
                     {:partitions 12
                      :configs {"batch.size" "65536"
                               "linger.ms" "5"
                               "compression.type" "snappy"}})
```

## 🔧 Dicas Importantes

1. **Partições**: Mais partições = maior paralelismo, mas também mais overhead
2. **Replicação**: Para produção, use replication-factor >= 3
3. **Compactação**: Use `cleanup.policy=compact` para tópicos de estado/chaves únicas
4. **Retenção**: Configure `retention.ms` baseado nos requisitos de negócio
5. **Compressão**: Use `compression.type=snappy` ou `lz4` para economizar espaço

## 🚀 Exemplo Completo

```clojure
(require '[kafka-metamorphosis.admin :as admin]
         '[kafka-metamorphosis.producer :as producer]
         '[kafka-metamorphosis.consumer :as consumer])

;; 1. Criar cliente admin
(def admin-client (admin/create-admin-client {:bootstrap-servers "localhost:9092"}))

;; 2. Criar tópico se não existir
(admin/create-topic-if-not-exists! admin-client "exemplo-completo"
                                   {:partitions 3})

;; 3. Criar producer e enviar mensagem
(def p (producer/create {:bootstrap-servers "localhost:9092"
                         :key-serializer "org.apache.kafka.common.serialization.StringSerializer"
                         :value-serializer "org.apache.kafka.common.serialization.StringSerializer"}))

(producer/send! p "exemplo-completo" "chave1" "Olá, Kafka!")

;; 4. Criar consumer e ler mensagem
(def c (consumer/create {:bootstrap-servers "localhost:9092"
                         :group-id "grupo-exemplo"
                         :key-deserializer "org.apache.kafka.common.serialization.StringDeserializer"
                         :value-deserializer "org.apache.kafka.common.serialization.StringDeserializer"}))

(consumer/subscribe! c ["exemplo-completo"])
(let [records (consumer/poll! c 5000)]
  (doseq [record records]
    (println "Recebido:" (:key record) "->" (:value record))))

;; 5. Limpar recursos
(producer/close! p)
(consumer/close! c)
(admin/close! admin-client)
```

## 🦋 A Metamorfose Está Completa!

Com essas funções, você pode facilmente gerenciar tópicos do Kafka usando uma interface Clojure idiomática, transformando a complexidade da administração em simplicidade funcional.
