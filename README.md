# Auth Microservice

Uma aplicação de autenticação e autorização robusta, segura e escalável construída com Spring Boot, PostgreSQL e JWT.

## Visão Geral

Este microsserviço oferece:

- Autenticação via JWT (Access Token + Refresh Token)
- Cadastro e Login de usuários
- Recuperação e reset de senha
- Verificação de e-mail
- Gerenciamento de roles e autorização
- Proteção contra brute force e rate limiting
- Auditoria de eventos
- Armazenamento seguro de senhas (Argon2id)
- Stateless e pronto para microsserviços

##  Stack Utilizada

- **Java**: 21 LTS
- **Spring Boot**: 3.4.x
- **Spring Security**: Versão gerenciada pelo Spring Boot
- **Spring Data JPA**: Versão gerenciada pelo Spring Boot
- **Hibernate**: Versão gerenciada pelo Spring Boot
- **PostgreSQL**: 16+
- **Maven**: 3.9+
- **Flyway**: Versão gerenciada pelo Spring Boot
- **JWT (jjwt)**: 0.12.3
- **Argon2**: Para hash de senha
- **Docker**: Compose 3.8+

##  Estrutura do Projeto

```
auth-microservice/
├── src/
│   ├── main/
│   │   ├── java/com/authservice/
│   │   │   ├── AuthServiceApplication.java
│   │   │   ├── config/              # Configurações
│   │   │   ├── controller/          # Endpoints REST
│   │   │   ├── service/             # Lógica de negócio
│   │   │   ├── repository/          # Acesso a dados
│   │   │   ├── entity/              # Entidades JPA
│   │   │   ├── dto/                 # Transfer Objects
│   │   │   ├── security/            # JWT, filtros, etc
│   │   │   ├── exception/           # Exceções customizadas
│   │   │   ├── event/               # Auditoria
│   │   │   └── util/                # Utilitários
│   │   └── resources/
│   │       ├── application.yml      # Configuração base
│   │       ├── application-dev.yml  # Desenvolvimento
│   │       ├── application-prod.yml # Produção
│   │       ├── application-test.yml # Testes
│   │       └── db/migration/        # Scripts Flyway
│   └── test/
│       ├── java/com/authservice/
│       └── resources/
├── docker-compose.yml               # Ambiente local
├── Dockerfile
├── pom.xml
├── .gitignore
└── README.md
```

##  Configuração Local

### Pré-requisitos

- Java 21 LTS instalado
- Maven 3.9+ instalado
- Docker e Docker Compose (para banco de dados)

### Inicializar o Ambiente

1. **Clone o repositório**:
```bash
cd auth-microservice
```

2. **Inicie PostgreSQL com Docker Compose**:
```bash
docker-compose up -d postgres
```

3. **Compile o projeto**:
```bash
mvn clean install
```

4. **Inicie a aplicação**:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

A aplicação estará disponível em `http://localhost:8080`

### Verificar Saúde do Serviço

```bash
curl http://localhost:8080/health
```

Resposta esperada:
```json
{
  "status": "UP",
  "timestamp": "2026-08-30T20:15:00",
  "service": "auth-service"
}
```

##  Executar com Docker Compose

```bash
# Iniciar todos os serviços
docker-compose up

# Parar e remover containers
docker-compose down

# Ver logs
docker-compose logs -f auth-service
```

## Banco de Dados

### Tabelas Criadas (Flyway V1)

- **users**: Armazena usuários
- **roles**: Define papéis (USER, ADMIN, SUPPORT)
- **user_roles**: Relacionamento usuário-papel
- **refresh_tokens**: Tokens de sessão revogáveis
- **password_reset_tokens**: Tokens para recuperação de senha
- **email_verification_tokens**: Tokens para verificação de email
- **audit_logs**: Registro de eventos de autenticação

## Segurança

- Senhas armazenadas com Argon2id
- JWTs assinados com chave externa (não hardcoded)
- Tokens não armazenados em texto puro
- Refresh Tokens revogáveis e rotacionáveis
- Rate limiting nos endpoints críticos (próximas fases)
- Proteção contra CSRF/CORS
- Logs estruturados sem dados sensíveis

## Configuração de Variáveis de Ambiente

### Desenvolvimento
As credenciais padrão estão em `application-dev.yml`:
- URL: `jdbc:postgresql://localhost:5432/auth_service_db`
- Usuário: `auth_user`
- Senha: `auth_password`

### Produção
Configure via variáveis de ambiente:
```bash
export DATABASE_URL=jdbc:postgresql://host:5432/auth_service_db
export DATABASE_USERNAME=user
export DATABASE_PASSWORD=password
export JWT_SECRET=sua-chave-secreta-muito-segura
```

## Testes

```bash
# Executar todos os testes
mvn test

# Executar teste específico
mvn test -Dtest=NomeDoTesteClass

# Com cobertura
mvn test jacoco:report
```

## Documentação

- **API OpenAPI/Swagger**: Será disponível em `/swagger-ui.html` (Fase 8)
- **Health Checks**: `/actuator/health`
- **Métricas**: `/actuator/metrics` (quando habilitadas)