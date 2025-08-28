#!/bin/bash
# Script automatizado para deploy no Clojars
# Execute: ./deploy.sh

set -e  # Parar em caso de erro

echo "🪲 Kafka Metamorphosis - Deploy para Clojars"
echo "=============================================="

# Verificar se as variáveis de ambiente estão configuradas
if [ -z "$CLOJARS_USERNAME" ] || [ -z "$CLOJARS_PASSWORD" ]; then
    echo "❌ Erro: Variáveis de ambiente não configuradas"
    echo "Execute primeiro: source deploy-setup.sh"
    exit 1
fi

echo "✅ Credenciais Clojars encontradas para: $CLOJARS_USERNAME"

# 1. Verificar sintaxe do projeto
echo ""
echo "🔍 1. Verificando sintaxe do projeto..."
lein check

# 2. Executar testes
echo ""
echo "🧪 2. Executando testes..."
lein test

# 3. Limpar builds anteriores
echo ""
echo "🧹 3. Limpando builds anteriores..."
lein clean

# 4. Compilar projeto
echo ""
echo "🔨 4. Compilando projeto..."
lein compile

# 5. Confirmar deploy
echo ""
echo "🚀 5. Pronto para deploy!"
echo "Versão atual: $(grep defproject project.clj | cut -d'"' -f4)"
echo ""
read -p "Confirma deploy para Clojars? (y/N): " confirm

if [[ $confirm =~ ^[Yy]$ ]]; then
    echo ""
    echo "📤 Fazendo deploy para Clojars..."
    lein deploy clojars
    
    echo ""
    echo "🎉 Deploy concluído com sucesso!"
    echo "📋 Biblioteca disponível em: https://clojars.org/kafka-metamorphosis"
    echo "📦 Para usar:"
    echo "   [kafka-metamorphosis \"$(grep defproject project.clj | cut -d'"' -f4)\"]"
else
    echo "❌ Deploy cancelado pelo usuário"
    exit 1
fi
