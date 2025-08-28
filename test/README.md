# Testes - Kafka Metamorphosis

## 🧪 Execução de Testes

### Script Automatizado

```bash
# Executa todos os testes com relatório
./test.sh
```

### Comandos Manuais

```bash
# Todos os testes
lein test

# Teste específico
lein test kafka-metamorphosis.core-test

# Verificar compilação
lein check
```

## 📋 Cobertura de Testes

### Core Namespace (`core_test.clj`)
- ✅ **Configuração de Producer** - `producer-config` com diferentes opções
- ✅ **Configuração de Consumer** - `consumer-config` com diferentes opções  
- ✅ **Configuração de Admin** - `admin-config` básico
- ✅ **Serializers Integration** - Uso de diferentes serializers
- ✅ **JSON Configurations** - Configurações específicas para JSON
- ✅ **Health Check** - Verificação de cluster indisponível
- ✅ **Main Function** - Execução da função principal

### Producer Namespace (`producer_test.clj`)
- ✅ **Utilities** - `map->properties` e `normalize-config`
- ✅ **Configuration Creation** - Criação de configurações de producer
- 📝 **Integration Tests** - Comentados (requerem Kafka rodando)

### Admin Namespace (`admin_test.clj`)
- ✅ **Configuration Creation** - Criação de configurações de admin
- ✅ **Custom Server** - Configuração com servidor customizado
- ✅ **Function Existence** - Verificação de funções disponíveis
- 📝 **Integration Tests** - Comentados (requerem Kafka rodando)

### Serializers Namespace (`serializers_test.clj`)
- ✅ **String Serializers** - Configurações string básicas
- ✅ **JSON Serializers** - Configurações JSON simples e Confluent
- ✅ **Avro Serializers** - Configurações com Schema Registry
- ✅ **Protobuf Serializers** - Configurações com Schema Registry
- ✅ **JSON Utilities** - Funções `to-json` e `from-json`

## 🔧 Tipos de Teste

### Unit Tests
Testam funções individuais sem dependências externas:
- Configurações de mapas Clojure
- Transformações de dados
- Validações de parâmetros

### Integration Tests (Comentados)
Requerem Kafka rodando e testam:
- Criação real de clientes
- Envio/recebimento de mensagens
- Operações administrativas

### Error Handling Tests
Testam comportamento com recursos indisponíveis:
- Health check com broker inexistente
- Configurações inválidas

## 🚀 Executando Testes com Kafka

Para executar testes de integração (opcionais):

```bash
# 1. Subir Kafka com Docker
cd /home/caio/repos/kafka-metamorphosis
source deploy-setup.sh

# 2. Usar dev environment
lein repl
(require '[kafka-metamorphosis.dev :as dev])
(dev/kafka-setup-kraft!)

# 3. Descomentar e executar testes de integração
# (edite os arquivos *_test.clj e descomente as seções 'comment')
```

## 📊 Estatísticas

- **Total**: 21 testes
- **Assertions**: 85 verificações
- **Cobertura**: ~90% das funções públicas
- **Performance**: ~3-5 segundos de execução

## 🎯 Próximos Passos

- [ ] Testes de performance
- [ ] Testes de concorrência
- [ ] Testes de schema evolution
- [ ] Testes com múltiplos brokers
- [ ] Testes de failover
