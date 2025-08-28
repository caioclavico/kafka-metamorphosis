# Configuração de Deploy - Kafka Metamorphosis

## Configuração Rápida

Execute estes comandos para configurar o deploy:

```bash
# 1. Configure as credenciais (uma vez só)
source deploy-setup.sh

# 2. Faça o deploy
./deploy.sh
```

## Configuração Manual

Se preferir configurar manualmente:

```bash
# Exportar variáveis de ambiente
export CLOJARS_USERNAME="caioclavico"
export CLOJARS_PASSWORD="seu-deploy-token-aqui"

# Verificar configuração
lein check
lein test

# Deploy
lein deploy clojars
```

## Arquivos Importantes

- `project.clj` - Configuração do projeto e dependências
- `deploy-setup.sh` - Script para configurar credenciais
- `deploy.sh` - Script automatizado de deploy
- `RELEASE.md` - Guia completo de release

## Status Atual

- ✅ Projeto configurado para deploy
- ✅ Scripts automatizados criados  
- ✅ Documentação atualizada
- 🔄 Aguardando primeiro deploy

## Próximo Passo

Execute: `source deploy-setup.sh && ./deploy.sh`
