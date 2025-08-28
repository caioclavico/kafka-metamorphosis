# Guia de Release - Kafka Metamorphosis

## 🚀 Como Fazer Deploy para Clojars

### 1. Pré-requisitos

- Conta criada no [Clojars.org](https://clojars.org)
- Deploy token gerado em [Clojars Tokens](https://clojars.org/tokens)
- Leiningen instalado

### 2. Configuração Inicial

```bash
# 1. Configure as credenciais
source deploy-setup.sh

# 2. Ou configure manualmente as variáveis
export CLOJARS_USERNAME="caioclavico"
export CLOJARS_PASSWORD="seu-deploy-token"
```

### 3. Deploy Automático

```bash
# Script automatizado que faz tudo
./deploy.sh
```

### 4. Deploy Manual

```bash
# 1. Verificar projeto
lein check

# 2. Executar testes
lein test

# 3. Deploy
lein deploy clojars
```

### 5. Versionamento

Para uma nova versão:

```bash
# 1. Atualizar versão no project.clj
# Exemplo: "0.1.0-SNAPSHOT" -> "0.1.0"

# 2. Commit e tag
git add .
git commit -m "Release 0.1.0"
git tag v0.1.0
git push origin main --tags

# 3. Deploy
./deploy.sh

# 4. Bump para próxima versão
# Exemplo: "0.1.0" -> "0.2.0-SNAPSHOT"
```

### 6. Verificar Deploy

Após o deploy, verifique em:

- **Clojars**: https://clojars.org/kafka-metamorphosis
- **Repo Central**: https://repo.clojars.org/kafka-metamorphosis/

### 7. Usando a Biblioteca

Depois do deploy, outros podem usar:

```clojure
;; project.clj
[kafka-metamorphosis "0.1.0"]

;; deps.edn
{:deps {kafka-metamorphosis/kafka-metamorphosis {:mvn/version "0.1.0"}}}

;; No código
(require '[kafka-metamorphosis.core :as km])
(km/health-check)
```

## 📋 Checklist de Release

- [ ] ✅ Todos os testes passando (`lein test`)
- [ ] ✅ Documentação atualizada (README, docs/)
- [ ] ✅ Versão incrementada no project.clj
- [ ] ✅ CHANGELOG.md atualizado
- [ ] ✅ Credenciais Clojars configuradas
- [ ] ✅ Git tag criada
- [ ] ✅ Deploy realizado
- [ ] ✅ Deploy verificado no Clojars

## 🔧 Troubleshooting

### Erro de Autenticação

```bash
# Verificar credenciais
echo $CLOJARS_USERNAME
echo $CLOJARS_PASSWORD

# Reconfigurar se necessário
source deploy-setup.sh
```

### Erro de Dependências

```bash
# Verificar dependências
lein deps :tree

# Resolver conflitos
lein clean
lein deps
```

### Erro de Compilação

```bash
# Verificar sintaxe
lein check

# Limpar e recompilar
lein clean
lein compile
```

## 🎯 Próximos Passos

1. Deploy inicial: `0.1.0-SNAPSHOT` → `0.1.0`
2. Testes em projeto real
3. Feedback da comunidade
4. Release estável: `0.1.0` → `1.0.0`

## 📖 Documentação Adicional

- [Clojars Deploy Guide](https://github.com/clojars/clojars-web/wiki/Pushing)
- [Leiningen Deploy](https://github.com/technomancy/leiningen/blob/master/doc/DEPLOY.md)
- [Semantic Versioning](https://semver.org/)
