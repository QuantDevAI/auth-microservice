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

## Como subir

No Filmaê o **front não chama este serviço**. Ele chama o `back-end` (`:8080`), que faz a fachada para as rotas `/api/v1/auth/*` daqui.

### Com a stack do back-end (recomendado no app)

No repositório `back-end`:

```bash
cp .env.example .env
docker compose up --build
```

O auth sobe em **http://localhost:8081** com o mesmo `JWT_SECRET` do back-end (mínimo 64 bytes, HS512). Não use o `docker-compose.yml` deste repo ao mesmo tempo: os dois ocupam a porta 5432.

### Sozinho (só este microsserviço)

Pré-requisitos: Java 21, Maven 3.9+, Docker.

```bash
cd auth-microservice
docker compose up -d postgres
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

Fica em **http://localhost:8080** (health: `/health` ou `/actuator/health`, Swagger: `/swagger-ui/index.html`).

Compose só deste repo (auth + Postgres, auth na 8080):

```bash
# JWT_SECRET com >= 64 bytes. O fallback do compose é curto demais para HS512.
export JWT_SECRET='troque-por-um-segredo-forte-com-pelo-menos-64-bytes-0123456789abcdefghijklmnop'
docker compose up --build
```

```bash
docker compose down
docker compose logs -f auth-service
```

### Conferir

```bash
curl http://localhost:8080/health          # se rodou sozinho
curl http://localhost:8081/actuator/health # se veio no compose do back-end
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
- Tokens de verificação de email e recuperação de senha armazenados apenas como hash
- Tokens de conta expiram e não podem ser reutilizados
- Todas as sessões são revogadas após redefinição de senha
- Rate limiting nos endpoints críticos (planejado como evoluções futuras)
- Proteção contra CSRF/CORS
- Logs estruturados sem dados sensíveis

## Fase 8 — Qualidade

A implementação final inclui:

- Cobertura de testes com JUnit + Spring Test
- Documentação OpenAPI/Swagger em `/swagger-ui/index.html`
- Endpoints de saúde em `/health` e `/actuator/health`
- Docker e Docker Compose para PostgreSQL e aplicação
- Configuração final de observabilidade e documentação
- README com instruções para execução local e produção

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

- **API OpenAPI/Swagger**: disponível em `/swagger-ui/index.html`
- **Health Checks**: `/health` e `/actuator/health`
- **Liveness / Readiness**: `/actuator/health/liveness` e `/actuator/health/readiness`
- **Métricas**: `/actuator/metrics` (quando habilitadas)