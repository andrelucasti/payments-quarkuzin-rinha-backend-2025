# Sistema de Pagamentos - Rinha Backend 2025

Este projeto é um sistema de processamento de pagamentos construído com **Quarkus** para a Rinha de Backend 2025, implementando uma arquitetura reativa e resiliente com processamento assíncrono.

## 🏗️ Arquitetura

O sistema utiliza uma arquitetura de microserviços com os seguintes componentes:

- **Load Balancer (Nginx)**: Distribui requisições entre instâncias da aplicação
- **API Gateway**: 2 instâncias da aplicação Quarkus
- **Redis**: Cache e sistema de streams para processamento assíncrono
- **Payment Processors**: Serviços externos para processamento de pagamentos (default + fallback)
- **PostgreSQL**: Banco de dados dos processadores de pagamento

## 🚀 Tecnologias Utilizadas

### Framework e Runtime
- **Java 21**: Linguagem principal com Virtual Threads habilitado
- **Quarkus 3.25.0**: Framework reativo "Supersonic Subatomic"
- **Maven**: Gerenciamento de dependências e build

### Dependências Principais
- **quarkus-rest**: API REST reativa
- **quarkus-rest-jackson**: Serialização JSON
- **quarkus-rest-client-jackson**: Cliente HTTP reativo
- **quarkus-redis-client**: Cliente Redis com suporte a streams
- **quarkus-arc**: Injeção de dependência (CDI)

### Infraestrutura
- **Docker + Docker Compose**: Containerização e orquestração
- **Nginx**: Load balancer e proxy reverso
- **Redis**: Cache, streams e pub/sub
- **PostgreSQL**: Banco de dados dos processadores

## 📋 Funcionalidades

### 1. Processamento de Pagamentos (`/payments`)
- **Endpoint**: `POST /payments`
- **Funcionamento**: 
  - Recebe requisições de pagamento
  - Publica no Redis Stream para processamento assíncrono
  - Retorna resposta imediata (fire-and-forget)

### 2. Sistema de Fallback
- **Processador Principal**: Taxa de 5%
- **Processador Fallback**: Taxa de 15% (ativado em caso de falha)
- **Resilência**: Tratamento automático de falhas com retry

### 3. Relatório de Pagamentos (`/payments-summary`)
- **Endpoint**: `GET /payments-summary?from={instant}&to={instant}`
- **Funcionamento**:
  - Consulta timeline de pagamentos no Redis
  - Agrupa dados por processador (default/fallback)
  - Retorna totais e quantidades processadas

### 4. Processamento Assíncrono
- **Redis Streams**: Fila de mensagens confiável
- **Consumer Groups**: Processamento distribuído entre instâncias
- **Virtual Threads**: Processamento não-bloqueante
- **Acknowledge**: Confirmação de processamento

## 🔧 Configurações

### Aplicação (`application.properties`)
```properties
# Redis
quarkus.redis.hosts=${REDIS_URL:redis://localhost:6379}
quarkus.redis.max-pool-size=${REDIS_MAX_POOL_SIZE:50}

# Virtual Threads
quarkus.virtual-threads.enabled=true

# Consumer
app.consumer-name=${CONSUMER_NAME:payment-consumer}
app.consumer-count=${CONSUMER_COUNT:3}

# Payment Processors
payment-processor.default.base-url=${PAYMENT_PROCESSOR_DEFAULT_BASE_URL:http://localhost:8001}
payment-processor.fallback.base-url=${PAYMENT_PROCESSOR_FALLBACK_BASE_URL:http://localhost:8002}
```

### Load Balancer (Nginx)
```nginx
upstream api {
    server api01:8080;
    server api02:8080;
}
```

## 🚀 Executando o Projeto

### Desenvolvimento Local
```bash
./mvnw quarkus:dev
```

### Com Docker Compose (Produção)
```bash
# Iniciar processadores de pagamento
cd infra-processor
docker-compose up -d

# Iniciar aplicação principal
docker-compose up -d
```

A aplicação estará disponível em: `http://localhost:9999`

### Teste dos Endpoints

**Criar Pagamento:**
```bash
curl -X POST http://localhost:9999/payments \
  -H "Content-Type: application/json" \
  -d '{"correlationId":"12345","amount":100.50}'
```

**Consultar Resumo:**
```bash
curl "http://localhost:9999/payments-summary?from=2025-01-01T00:00:00Z&to=2025-12-31T23:59:59Z"
```

## 📊 Recursos de Performance

### Otimizações Implementadas
- **Virtual Threads**: Processamento não-bloqueante
- **Redis Connection Pool**: Pool otimizado de conexões
- **Async Processing**: Processamento assíncrono com streams
- **Native Transport**: Vert.x com transporte nativo
- **Thread Pool**: Pool customizado (16-64 threads)

### Limites de Recursos (Docker)
- **API**: 0.4 CPU, 120MB RAM (por instância)
- **Nginx**: 0.2 CPU, 40MB RAM
- **Redis**: 0.5 CPU, 70MB RAM

## 🧪 Testes

```bash
# Executar testes
./mvnw test

# Executar com cobertura
./mvnw test jacoco:report
```

## 📦 Build

### JAR Tradicional
```bash
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

### Über JAR
```bash
./mvnw package -Dquarkus.package.jar.type=uber-jar
java -jar target/*-runner.jar
```

### Nativo (GraalVM)
```bash
./mvnw package -Dnative
./target/payments-quarkuszin-1.0.0-SNAPSHOT-runner
```

## 📖 Documentação Adicional

- [Quarkus Framework](https://quarkus.io/)
- [Redis Streams](https://redis.io/topics/streams-intro)
- [Virtual Threads](https://quarkus.io/guides/virtual-threads)
