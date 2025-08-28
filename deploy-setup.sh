#!/bin/bash
# Script para configurar deploy no Clojars
# Execute: source deploy-setup.sh

echo "🚀 Configurando ambiente para deploy no Clojars..."

# Solicitar credenciais do usuário
read -p "Digite seu username do Clojars (caioclavico): " username
username=${username:-caioclavico}

read -s -p "Digite seu deploy token do Clojars: " token
echo

# Exportar variáveis de ambiente
export CLOJARS_USERNAME="$username"
export CLOJARS_PASSWORD="$token"

echo "✅ Variáveis de ambiente configuradas:"
echo "   CLOJARS_USERNAME: $username"
echo "   CLOJARS_PASSWORD: ****"
echo ""
echo "📋 Comandos disponíveis:"
echo "   lein check          # Verificar projeto"
echo "   lein test           # Executar testes"
echo "   lein deploy clojars # Deploy para Clojars"
echo ""
echo "🎯 Para fazer deploy:"
echo "   1. lein check"
echo "   2. lein test"
echo "   3. lein deploy clojars"
