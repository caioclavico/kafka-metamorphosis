#!/bin/bash
# Script para executar todos os testes com relatório detalhado

echo "🧪 Kafka Metamorphosis - Executando Testes"
echo "==========================================="

# Verificar se o projeto compila
echo ""
echo "🔍 1. Verificando compilação..."
lein check
if [ $? -ne 0 ]; then
    echo "❌ Erro de compilação!"
    exit 1
fi

# Executar testes
echo ""
echo "🧪 2. Executando testes..."
lein test

# Verificar resultado
if [ $? -eq 0 ]; then
    echo ""
    echo "🎉 Todos os testes passaram!"
    echo ""
    echo "📋 Cobertura de Testes:"
    echo "  ✅ Core namespace - Configurações e high-level API"
    echo "  ✅ Producer namespace - Configurações de producer"  
    echo "  ✅ Admin namespace - Configurações de admin"
    echo "  ✅ Serializers namespace - Todos os tipos de serialização"
    echo ""
    echo "🚀 Projeto pronto para deploy!"
else
    echo ""
    echo "❌ Alguns testes falharam!"
    echo "Verifique os erros acima e corrija antes de fazer deploy."
    exit 1
fi
