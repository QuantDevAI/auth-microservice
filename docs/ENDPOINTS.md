# Endpoints Implementados - Auth Service

## Fase 1 ✅ - Estrutura
- `GET /health` - Health check básico
- `GET /api/v1/health` - Health check API

## Fase 2 ✅ - Usuários

### POST `/api/v1/auth/register`
**Registrar novo usuário**

**Request:**
```json
{
  "name": "João Silva",
  "email": "joao@email.com",
  "password": "SenhaSegura123!"
}
```

**Validações:**
- name: obrigatório, 2-255 caracteres
- email: obrigatório, formato válido, único
- password: 8-255 chars, pelo menos 1 MAIÚSCULA, 1 minúscula, 1 dígito, 1 caractere especial

**Response (201 CREATED):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "João Silva",
  "email": "joao@email.com",
  "emailVerified": false,
  "status": "PENDING_VERIFICATION",
  "roles": ["USER"],
  "createdAt": "2026-08-30T20:00:00",
  "updatedAt": "2026-08-30T20:00:00",
  "lastLoginAt": null
}
```

**Erro 409 (Duplicate Email):**
```json
{
  "timestamp": "2026-08-30T20:00:00Z",
  "status": 409,
  "error": "CONFLICT",
  "message": "Email already registered",
  "path": "/api/v1/auth/register"
}
```

**Erro 400 (Validação):**
```json
{
  "timestamp": "2026-08-30T20:00:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Validation failed",
  "errors": {
    "password": "Password must contain at least 8 characters..."
  },
  "path": "/api/v1/auth/register"
}
```

---

## Fase 3 ✅ - Autenticação

### POST `/api/v1/auth/login`
**Fazer login do usuário**

**Request:**
```json
{
  "email": "joao@email.com",
  "password": "SenhaSegura123!"
}
```

**Validações:**
- email: obrigatório, formato válido
- password: obrigatório

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900000
}
```

**Erro 401 (Invalid Credentials):**
```json
{
  "timestamp": "2026-08-30T20:00:00Z",
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "Invalid credentials",
  "path": "/api/v1/auth/login"
}
```

**Erro 400 (Validação):**
```json
{
  "timestamp": "2026-08-30T20:00:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Validation failed",
  "errors": {
    "email": "Email must be valid"
  },
  "path": "/api/v1/auth/login"
}
```

### GET `/api/v1/users/me`
**Obter dados do usuário autenticado**

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "João Silva",
  "email": "joao@email.com",
  "emailVerified": false,
  "status": "PENDING_VERIFICATION",
  "roles": ["USER"],
  "createdAt": "2026-08-30T20:00:00",
  "updatedAt": "2026-08-30T20:00:00",
  "lastLoginAt": null
}
```

**Erro 401 (Sem token):**
```
401 Unauthorized (sem corpo)
```

**Erro 403 (Token inválido):**
```
403 Forbidden (sem corpo)
```

---

## Fase 4 ✅ - Sessões

### POST `/api/v1/auth/refresh`
**Renovar access token usando refresh token**

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9..."
}
```

**Validações:**
- refreshToken: obrigatório

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900000
}
```

**Funcionalidades:**
- Valida refresh token contra database (token_hash)
- Verifica se token não expirou
- Verifica se token não foi revogado
- Revoga o token antigo (atualiza revoked_at)
- Emite novo refresh token com novos dados de IP e User-Agent
- Emite novo access token (15 minutos)

**Erro 401 (Token Inválido/Expirado/Revogado):**
```json
{
  "timestamp": "2026-08-30T20:00:00Z",
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "Refresh token not found or invalid | Refresh token expired | Refresh token revoked",
  "path": "/api/v1/auth/refresh"
}
```

### POST `/api/v1/auth/logout`
**Fazer logout do usuário revogando sessão**

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Response (200 OK):**
```
(sem corpo)
```

**Funcionalidades:**
- Requer autenticação (Bearer token no header)
- Revoga TODOS os refresh tokens ativos do usuário
- Torna impossível renovar access token após logout
- Seguro mesmo sem passar refresh token no body

**Erro 403 (Sem autenticação):**
- Se não passar Authorization header, servidor não trata como erro
- POST /logout sem token retorna 200 OK (idempoente)

---

## Fase 6 ✅ - Conta

### POST `/api/v1/auth/verify-email`
**Confirmar o email do usuário**

**Request:**
```json
{
  "token": "token-enviado-por-email"
}
```

O token é armazenado no banco somente como hash, possui validade configurável de 24 horas e não pode ser reutilizado. Quando válido, o usuário passa de `PENDING_VERIFICATION` para `ACTIVE`.

### POST `/api/v1/auth/forgot-password`
**Solicitar recuperação de senha**

**Request:**
```json
{
  "email": "joao@email.com"
}
```

**Response (200 OK):**
```text
If the email exists, password reset instructions will be sent
```

O serviço mantém a mesma resposta quando o email não existe, evitando enumeração de usuários. O envio efetivo da mensagem de email deverá ser conectado posteriormente.

### POST `/api/v1/auth/reset-password`
**Definir uma nova senha**

**Request:**
```json
{
  "token": "token-enviado-por-email",
  "newPassword": "NovaSenha123!"
}
```

O token expira em 1 hora, só pode ser usado uma vez e a nova senha deve conter pelo menos 8 caracteres, uma letra maiúscula, uma minúscula, um número e um caractere especial. Após a troca, todas as sessões de refresh do usuário são revogadas.

**Respostas de erro:**
- `400 BAD_REQUEST` para senha fraca ou dados inválidos;
- `401 UNAUTHORIZED` para token inexistente, expirado ou já utilizado.

---

## Fases Próximas



### Fase 5 - Autorização
- Endpoints com `@PreAuthorize("hasRole('ADMIN')")`

### Fase 7 - Segurança
- Rate limiting implementado
- Proteção contra brute force

### Fase 8 - Qualidade
- Swagger/OpenAPI em `/swagger-ui.html`
- Docker Compose funcional
- Cobertura de testes
