# 📅 Agendamento API

**[🇵🇹 Português](#-português) · [🇬🇧 English](#-english)**

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?logo=postgresql&logoColor=white)
![JWT](https://img.shields.io/badge/Auth-JWT-black?logo=jsonwebtokens)
![OpenAPI](https://img.shields.io/badge/Docs-OpenAPI%20%2F%20Swagger-85EA2D?logo=swagger&logoColor=black)

---

## 🇵🇹 Português

### Sobre o projeto

API REST para **agendamento de serviços** entre clientes e prestadores (cabeleireiros, barbeiros, esteticistas, personal trainers, etc.). Foi construída para reproduzir um cenário real de negócio:

- **Três perfis de utilizador** (`ADMIN`, `PROFESSIONAL`, `CUSTOMER`) com permissões distintas.
- **Fluxo de aprovação de prestadores**: um prestador regista-se e só pode iniciar sessão depois de aprovado por um administrador.
- **Propriedade dos dados**: cada utilizador vê e altera apenas o que lhe pertence.
- **Prevenção de conflitos de horário**: um prestador nunca tem dois agendamentos sobrepostos.
- **Autenticação stateless com JWT** e documentação interativa com **Swagger UI**.

### Funcionalidades

| Módulo | O que faz |
|---|---|
| **Autenticação** | Registo de clientes e de prestadores, login com emissão de token JWT. |
| **Administração** | Listagem de prestadores por estado (`PENDING`, `APPROVED`, `REJECTED`), aprovação e rejeição. |
| **Serviços** | CRUD de serviços (nome, descrição, preço, duração). Cada serviço pertence a um prestador. Catálogo público filtrável por prestador. |
| **Clientes** | Gestão de clientes (ADMIN) e consulta do próprio perfil (CUSTOMER). |
| **Agendamentos** | Criação, reagendamento, confirmação, conclusão e cancelamento, com validação de conflitos. |

### Stack tecnológica

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 (LTS) |
| Framework | Spring Boot 4.1 — Web MVC, Data JPA, Security, Validation |
| Base de dados | PostgreSQL 16 |
| Migrações | Flyway |
| Autenticação | JWT ([jjwt](https://github.com/jwtk/jjwt) 0.12) + BCrypt |
| Documentação | springdoc-openapi 3 (OpenAPI 3.1 + Swagger UI) |
| Infraestrutura | Docker / Docker Compose |
| Build / utilitários | Maven (wrapper incluído), Lombok |

### Arquitetura

O código está organizado **por funcionalidade** (*package by feature*). Cada módulo tem as suas próprias camadas:

```
src/main/java/com/bilangieri/agendamento
├── appointment/     # Agendamentos      (controller, dto, entity, repository, service)
├── auth/            # Registo, login e geração de JWT
├── customer/        # Clientes
├── professional/    # Aprovação de prestadores (área ADMIN)
├── service/         # Serviços oferecidos pelos prestadores
├── user/            # Utilizador, perfis (Role) e estado de aprovação
├── security/        # SecurityConfig, filtro JWT, utilizador autenticado
├── config/          # Beans de configuração (BCrypt, OpenAPI)
└── exception/       # Exceções de negócio e handler global
```

Fluxo de um pedido: `Controller → Service (regras de negócio e permissões) → Repository (JPA) → PostgreSQL`. Os controllers recebem e devolvem **DTOs** (Java records com Bean Validation); as entidades JPA nunca são expostas diretamente.

### Modelo de dados

```
users ──< services                 (um prestador oferece vários serviços)
users ──< appointments             (um prestador recebe vários agendamentos)
users ──1 customers                (um cliente com conta tem um registo em customers)
customers ──< appointments
services  ──< appointments
```

- `users`: todas as contas (`role`, `active`, `approval_status`).
- `customers`: dados de contacto do cliente. Pode existir **sem conta** (criado pelo ADMIN ao balcão).
- `services`: catálogo de cada prestador (`price`, `duration_minutes`, `active`).
- `appointments`: `start_at`, `end_at` (calculado a partir da duração do serviço) e `status`.

O esquema é gerido exclusivamente pelo Flyway (`src/main/resources/db/migration`; dados de teste em `db/testdata`). O Hibernate apenas valida (`ddl-auto: validate`).

### Perfis e permissões

| Ação | ADMIN | PROFESSIONAL | CUSTOMER |
|---|:-:|:-:|:-:|
| Aprovar / rejeitar prestadores | ✅ | ❌ | ❌ |
| Gerir clientes | ✅ | ❌ | ❌ |
| Ver o próprio perfil de cliente | ❌ | ❌ | ✅ |
| Listar serviços | Todos | Os seus | Catálogo disponível |
| Criar / editar / remover serviços | ✅ | Os seus | ❌ |
| Criar agendamentos | ✅ | ❌ | Para si próprio |
| Ver agendamentos | Todos | Os que recebeu | Os seus |
| Confirmar / concluir agendamentos | ✅ | Os que recebeu | ❌ |
| Cancelar agendamentos | ✅ | Os que recebeu | Os seus |

### Regras de negócio

1. **Registo de prestador**: a conta é criada com estado `PENDING` e não consegue iniciar sessão até um ADMIN a aprovar. Se for rejeitada (`REJECTED`) ou desativada, o login devolve uma mensagem explicativa.
2. **Catálogo visível aos clientes**: só aparecem serviços **ativos** de prestadores **ativos e aprovados**. Isto vale também para a consulta por id: um serviço fora do catálogo responde `404` a quem não o pode ver.
3. **Agendamento**:
   - O prestador do agendamento é sempre o dono do serviço escolhido.
   - `startAt` tem de ser no futuro. O `endAt` é calculado automaticamente (`startAt + duração do serviço`).
   - Não é possível agendar um serviço inativo ou de um prestador indisponível.
   - **Conflitos de horário**: é rejeitado qualquer agendamento que se sobreponha a outro do mesmo prestador (exceto os `CANCELLED`). A mesma validação é aplicada no reagendamento. A regra é garantida também com pedidos simultâneos: as marcações do mesmo prestador são serializadas (`SELECT ... FOR UPDATE`) e uma *exclusion constraint* do PostgreSQL (`appointments_no_overlap`, migração V7) impede sobreposições ao nível da base de dados.
4. **Estados do agendamento**:

   | De | Pode passar a |
   |---|---|
   | `SCHEDULED` | `CONFIRMED`, `COMPLETED`, `CANCELLED`, `NO_SHOW` |
   | `CONFIRMED` | `COMPLETED`, `CANCELLED`, `NO_SHOW` |
   | `COMPLETED`, `CANCELLED`, `NO_SHOW` | — (estados finais: o agendamento deixa de poder ser alterado ou reagendado) |

   `COMPLETED` e `NO_SHOW` só podem ser aplicados depois da hora de início do agendamento.
5. **Remoções**: serviços e clientes com agendamentos associados não podem ser removidos (`409`). Para retirar um serviço do catálogo, desative-o (`active = false`).

### Como executar

#### Pré-requisitos

- [Java 21](https://adoptium.net/)
- [Docker Desktop](https://www.docker.com/products/docker-desktop) (para o PostgreSQL)
- [Git](https://git-scm.com/)

#### 1. Clonar o repositório

```bash
git clone https://github.com/Thiago-Bilangieri/agendamentos.git
cd agendamentos
```

#### 2. Subir a base de dados

```bash
docker compose up -d
```

Arranca um PostgreSQL 16 em `localhost:5432` (base `agendamentodb`, utilizador `root` / password `root`).

#### 3. Executar a aplicação

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

A API fica disponível em **http://localhost:8080**. Ao arrancar, o Flyway cria as tabelas e carrega os dados de teste.

#### 4. Executar os testes

```bash
./mvnw test
```

Requer o Docker em execução: os testes de integração arrancam um PostgreSQL descartável com [Testcontainers](https://testcontainers.com/) e **nunca tocam na base de dados de desenvolvimento**. Cada teste corre numa transação revertida no fim.

| Tipo | Classes | O que cobre |
|---|---|---|
| Unitários | `AppointmentStatusTest`, `AppointmentServiceTest` (Mockito) | Transições de estado, cálculo do `endAt`, conflitos de horário, disponibilidade do serviço |
| Integração | `AuthIntegrationTest`, `ServiceIntegrationTest`, `AppointmentIntegrationTest`, `CustomerIntegrationTest`, `AppointmentConcurrencyIntegrationTest` (MockMvc) | Login e registo, `401`/`403` por perfil, visibilidade do catálogo, propriedade dos dados, conflitos e regras de estado ponta a ponta, marcações simultâneas no mesmo horário |

### Documentação da API (Swagger)

| Recurso | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Especificação OpenAPI (JSON) | http://localhost:8080/v3/api-docs |

**Como autenticar no Swagger:**

1. Execute `POST /api/auth/login` com um dos utilizadores de teste abaixo.
2. Copie o valor de `token` da resposta.
3. Clique em **Authorize** (cadeado no topo) e cole o token, **sem** o prefixo `Bearer`.
4. Todos os pedidos seguintes passam a enviar o header `Authorization: Bearer <token>`.

### Utilizadores de teste

Carregados pela migração `db/testdata/V6__insert_test_data.sql`, apenas no perfil `dev`. **A password de todos é `password`.**

| Email | Perfil | Situação |
|---|---|---|
| `admin@agendamento.com` | ADMIN | — |
| `profissional@agendamento.com` | PROFESSIONAL | Aprovado |
| `ana.ribeiro@agendamento.com` | PROFESSIONAL | Aprovado (cabeleireira) |
| `bruno.matos@agendamento.com` | PROFESSIONAL | Aprovado (barbeiro) |
| `carla.nunes@agendamento.com` | PROFESSIONAL | Aprovado (estética) |
| `diogo.ferreira@agendamento.com` | PROFESSIONAL | Pendente — login bloqueado |
| `elisa.marques@agendamento.com` | PROFESSIONAL | Rejeitado — login bloqueado |
| `filipe.sousa@agendamento.com` | PROFESSIONAL | Inativo — login bloqueado |
| `cliente@agendamento.com` | CUSTOMER | — |
| `joao.silva@email.com`, `maria.santos@email.com`, `pedro.costa@email.com`, `sofia.almeida@email.com` | CUSTOMER | — |

> ⚠️ Estes dados destinam-se apenas a desenvolvimento e nunca são carregados fora do perfil `dev`.

### Endpoints

Todos os endpoints, exceto os de registo, login e documentação, exigem o header `Authorization: Bearer <token>`.

#### Autenticação — `/api/auth` (público)

| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/api/auth/register` | Regista um cliente |
| POST | `/api/auth/register/professional` | Regista um prestador (fica `PENDING`) |
| POST | `/api/auth/login` | Autentica e devolve o token JWT |

#### Administração — `/api/admin/professionals` (ADMIN)

| Método | Endpoint | Descrição |
|---|---|---|
| GET | `/api/admin/professionals?status=PENDING` | Lista prestadores por estado (por omissão `PENDING`) |
| PATCH | `/api/admin/professionals/{id}/approve` | Aprova um prestador |
| PATCH | `/api/admin/professionals/{id}/reject` | Rejeita um prestador |

#### Serviços — `/api/services`

| Método | Endpoint | Perfis | Descrição |
|---|---|---|---|
| GET | `/api/services` | Todos | Lista serviços conforme o perfil |
| GET | `/api/services?professionalId={id}` | ADMIN, CUSTOMER | Serviços de um prestador específico |
| GET | `/api/services/{id}` | Todos | Detalhe de um serviço |
| POST | `/api/services` | ADMIN, PROFESSIONAL | Cria um serviço |
| PUT | `/api/services/{id}` | ADMIN, PROFESSIONAL | Atualiza um serviço |
| DELETE | `/api/services/{id}` | ADMIN, PROFESSIONAL | Remove um serviço |

#### Clientes — `/api/customers`

| Método | Endpoint | Perfis | Descrição |
|---|---|---|---|
| GET | `/api/customers/me` | CUSTOMER | Perfil do cliente autenticado |
| GET | `/api/customers` | ADMIN | Lista clientes |
| GET | `/api/customers/{id}` | ADMIN | Detalhe de um cliente |
| POST | `/api/customers` | ADMIN | Cria um cliente (sem conta) |
| PUT | `/api/customers/{id}` | ADMIN | Atualiza um cliente |
| DELETE | `/api/customers/{id}` | ADMIN | Remove um cliente |

#### Agendamentos — `/api/appointments`

| Método | Endpoint | Perfis | Descrição |
|---|---|---|---|
| GET | `/api/appointments` | Todos | Lista agendamentos conforme o perfil |
| GET | `/api/appointments/{id}` | Todos (dono) | Detalhe de um agendamento |
| POST | `/api/appointments` | ADMIN, CUSTOMER | Cria um agendamento |
| PUT | `/api/appointments/{id}` | ADMIN | Reagenda / altera estado e notas |
| PATCH | `/api/appointments/{id}/confirm` | ADMIN, PROFESSIONAL | Confirma |
| PATCH | `/api/appointments/{id}/complete` | ADMIN, PROFESSIONAL | Marca como concluído |
| PATCH | `/api/appointments/{id}/cancel` | Todos (dono) | Cancela |

#### Exemplo de utilização

```bash
# 1. Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"joao.silva@email.com","password":"password"}'

# 2. Ver os serviços da prestadora Ana Ribeiro
curl "http://localhost:8080/api/services?professionalId=4" \
  -H "Authorization: Bearer <token>"

# 3. Criar um agendamento
curl -X POST http://localhost:8080/api/appointments \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"customerId":2,"serviceId":2,"startAt":"2030-01-15T10:00:00","notes":"Primeira visita"}'
```

### Tratamento de erros

Os erros são devolvidos num formato JSON consistente, pelo `GlobalExceptionHandler`:

```json
{
  "timestamp": "2026-10-09T20:15:00",
  "status": 400,
  "error": "Business Rule Violation",
  "message": "O profissional já possui um agendamento conflituoso neste intervalo de horários."
}
```

| Status | Quando |
|---|---|
| `400` | Erro de validação (`messages` com o erro de cada campo) ou violação de regra de negócio |
| `401` | Token em falta, inválido ou expirado |
| `403` | Perfil sem permissão ou recurso de outro utilizador |
| `404` | Recurso não encontrado |
| `409` | Conflito com o estado dos dados (ex.: remover um registo com agendamentos) |

### Configuração

A configuração está separada por perfis do Spring:

| Ficheiro | Perfil | Conteúdo |
|---|---|---|
| `application.yml` | todos | Configuração base. **Não contém segredos**: lê-os de variáveis de ambiente. |
| `application-dev.yml` | `dev` (**ativo por omissão**) | Credenciais locais, chave JWT de desenvolvimento, SQL e logs de debug, dados de teste. |

Em desenvolvimento não é preciso configurar nada: o perfil `dev` é ativado automaticamente.

#### Variáveis de ambiente (produção)

| Variável | Descrição | Obrigatória |
|---|---|:-:|
| `SPRING_PROFILES_ACTIVE` | Use `prod` (ou qualquer perfil diferente de `dev`) | ✅ |
| `DB_URL` | URL JDBC do PostgreSQL | ✅ |
| `DB_USERNAME` / `DB_PASSWORD` | Credenciais da base de dados | ✅ |
| `JWT_SECRET` | Chave de assinatura do JWT (mínimo 32 caracteres) | ✅ |
| `JWT_EXPIRATION` | Validade do token em ms (por omissão `86400000`, 24 h) | ❌ |

```bash
SPRING_PROFILES_ACTIVE=prod \
DB_URL=jdbc:postgresql://db:5432/agendamentodb DB_USERNAME=app DB_PASSWORD=*** \
JWT_SECRET="$(openssl rand -base64 48)" \
java -jar target/agendamento-0.0.1-SNAPSHOT.jar
```

> 🔐 Se alguma variável obrigatória faltar, a aplicação **não arranca**, em vez de correr com valores inseguros. A chave JWT presente em `application-dev.yml` é pública e serve apenas para desenvolvimento. Os dados de teste (`db/testdata`) só são carregados no perfil `dev`.

> 🗄️ A migração V7 cria a extensão `btree_gist`. No PostgreSQL 13+ ela é *trusted*, por isso basta o utilizador da aplicação ser dono da base de dados; em versões anteriores, crie-a uma vez com um superutilizador.

### Próximos passos

- [x] Testes unitários e de integração (Testcontainers) para as regras de agendamento
- [ ] Categorias de serviços e pesquisa por tipo de serviço
- [ ] Horário de funcionamento e disponibilidade por prestador
- [ ] Paginação e ordenação nas listagens
- [ ] Dockerfile da aplicação e pipeline de CI

---

## 🇬🇧 English

### About

REST API for **service scheduling** between customers and service providers (hairdressers, barbers, beauticians, personal trainers, etc.). It was built to reproduce a realistic business scenario:

- **Three user roles** (`ADMIN`, `PROFESSIONAL`, `CUSTOMER`) with distinct permissions.
- **Provider approval workflow**: a provider signs up and can only log in after an administrator approves the account.
- **Data ownership**: each user can only see and change what belongs to them.
- **Double-booking prevention**: a provider can never have two overlapping appointments.
- **Stateless JWT authentication** and interactive documentation with **Swagger UI**.

### Features

| Module | What it does |
|---|---|
| **Authentication** | Customer and provider sign-up, login issuing a JWT. |
| **Administration** | List providers by status (`PENDING`, `APPROVED`, `REJECTED`), approve and reject them. |
| **Services** | Service CRUD (name, description, price, duration). Each service belongs to a provider. Public catalogue filterable by provider. |
| **Customers** | Customer management (ADMIN) and own-profile lookup (CUSTOMER). |
| **Appointments** | Booking, rescheduling, confirming, completing and cancelling, with conflict validation. |

### Tech stack

| Layer | Technology |
|---|---|
| Language | Java 21 (LTS) |
| Framework | Spring Boot 4.1 — Web MVC, Data JPA, Security, Validation |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Authentication | JWT ([jjwt](https://github.com/jwtk/jjwt) 0.12) + BCrypt |
| Documentation | springdoc-openapi 3 (OpenAPI 3.1 + Swagger UI) |
| Infrastructure | Docker / Docker Compose |
| Build / tooling | Maven (wrapper included), Lombok |

### Architecture

The code is organised **by feature** (*package by feature*). Each module has its own layers:

```
src/main/java/com/bilangieri/agendamento
├── appointment/     # Appointments      (controller, dto, entity, repository, service)
├── auth/            # Sign-up, login and JWT generation
├── customer/        # Customers
├── professional/    # Provider approval (ADMIN area)
├── service/         # Services offered by providers
├── user/            # User account, roles and approval status
├── security/        # SecurityConfig, JWT filter, current user
├── config/          # Configuration beans (BCrypt, OpenAPI)
└── exception/       # Business exceptions and global handler
```

Request flow: `Controller → Service (business rules and permissions) → Repository (JPA) → PostgreSQL`. Controllers receive and return **DTOs** (Java records with Bean Validation); JPA entities are never exposed directly.

### Data model

```
users ──< services                 (a provider offers many services)
users ──< appointments             (a provider receives many appointments)
users ──1 customers                (a customer with an account has one customers row)
customers ──< appointments
services  ──< appointments
```

- `users`: every account (`role`, `active`, `approval_status`).
- `customers`: customer contact details. May exist **without an account** (created by an ADMIN at the front desk).
- `services`: each provider's catalogue (`price`, `duration_minutes`, `active`).
- `appointments`: `start_at`, `end_at` (derived from the service duration) and `status`.

The schema is managed exclusively by Flyway (`src/main/resources/db/migration`; test data in `db/testdata`). Hibernate only validates it (`ddl-auto: validate`).

### Roles and permissions

| Action | ADMIN | PROFESSIONAL | CUSTOMER |
|---|:-:|:-:|:-:|
| Approve / reject providers | ✅ | ❌ | ❌ |
| Manage customers | ✅ | ❌ | ❌ |
| View own customer profile | ❌ | ❌ | ✅ |
| List services | All | Own | Available catalogue |
| Create / update / delete services | ✅ | Own | ❌ |
| Book appointments | ✅ | ❌ | For themselves |
| View appointments | All | Received | Own |
| Confirm / complete appointments | ✅ | Received | ❌ |
| Cancel appointments | ✅ | Received | Own |

### Business rules

1. **Provider sign-up**: the account is created as `PENDING` and cannot log in until an ADMIN approves it. If it is rejected (`REJECTED`) or deactivated, login returns an explanatory message.
2. **Customer-facing catalogue**: only **active** services from **active and approved** providers are listed. This also applies to lookups by id: a service outside the catalogue returns `404` to anyone who cannot see it.
3. **Booking**:
   - The appointment's provider is always the owner of the chosen service.
   - `startAt` must be in the future. `endAt` is computed automatically (`startAt + service duration`).
   - Inactive services and unavailable providers cannot be booked.
   - **Time conflicts**: any booking that overlaps another appointment of the same provider (except `CANCELLED` ones) is rejected. The same check applies when rescheduling. The rule also holds under concurrent requests: bookings for the same provider are serialised (`SELECT ... FOR UPDATE`) and a PostgreSQL *exclusion constraint* (`appointments_no_overlap`, migration V7) prevents overlaps at the database level.
4. **Appointment lifecycle**:

   | From | Can become |
   |---|---|
   | `SCHEDULED` | `CONFIRMED`, `COMPLETED`, `CANCELLED`, `NO_SHOW` |
   | `CONFIRMED` | `COMPLETED`, `CANCELLED`, `NO_SHOW` |
   | `COMPLETED`, `CANCELLED`, `NO_SHOW` | — (final states: the appointment can no longer be changed or rescheduled) |

   `COMPLETED` and `NO_SHOW` can only be applied after the appointment's start time.
5. **Deletions**: services and customers with appointments cannot be deleted (`409`). To remove a service from the catalogue, deactivate it (`active = false`).

### Getting started

#### Prerequisites

- [Java 21](https://adoptium.net/)
- [Docker Desktop](https://www.docker.com/products/docker-desktop) (for PostgreSQL)
- [Git](https://git-scm.com/)

#### 1. Clone the repository

```bash
git clone https://github.com/Thiago-Bilangieri/agendamentos.git
cd agendamentos
```

#### 2. Start the database

```bash
docker compose up -d
```

Starts PostgreSQL 16 on `localhost:5432` (database `agendamentodb`, user `root` / password `root`).

#### 3. Run the application

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

The API is available at **http://localhost:8080**. On startup, Flyway creates the tables and loads the test data.

#### 4. Run the tests

```bash
./mvnw test
```

Requires Docker to be running: integration tests start a throwaway PostgreSQL with [Testcontainers](https://testcontainers.com/) and **never touch the development database**. Each test runs in a transaction that is rolled back at the end.

| Type | Classes | Covers |
|---|---|---|
| Unit | `AppointmentStatusTest`, `AppointmentServiceTest` (Mockito) | Status transitions, `endAt` calculation, time conflicts, service availability |
| Integration | `AuthIntegrationTest`, `ServiceIntegrationTest`, `AppointmentIntegrationTest`, `CustomerIntegrationTest`, `AppointmentConcurrencyIntegrationTest` (MockMvc) | Login and sign-up, `401`/`403` per role, catalogue visibility, data ownership, conflicts and status rules end to end, simultaneous bookings for the same slot |

### API documentation (Swagger)

| Resource | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI spec (JSON) | http://localhost:8080/v3/api-docs |

**Authenticating in Swagger:**

1. Call `POST /api/auth/login` with one of the test users below.
2. Copy the `token` value from the response.
3. Click **Authorize** (padlock at the top) and paste the token, **without** the `Bearer` prefix.
4. Every following request sends the `Authorization: Bearer <token>` header.

### Test users

Loaded by the `db/testdata/V6__insert_test_data.sql` migration, under the `dev` profile only. **Every password is `password`.**

| Email | Role | Status |
|---|---|---|
| `admin@agendamento.com` | ADMIN | — |
| `profissional@agendamento.com` | PROFESSIONAL | Approved |
| `ana.ribeiro@agendamento.com` | PROFESSIONAL | Approved (hairdresser) |
| `bruno.matos@agendamento.com` | PROFESSIONAL | Approved (barber) |
| `carla.nunes@agendamento.com` | PROFESSIONAL | Approved (beauty) |
| `diogo.ferreira@agendamento.com` | PROFESSIONAL | Pending — login blocked |
| `elisa.marques@agendamento.com` | PROFESSIONAL | Rejected — login blocked |
| `filipe.sousa@agendamento.com` | PROFESSIONAL | Inactive — login blocked |
| `cliente@agendamento.com` | CUSTOMER | — |
| `joao.silva@email.com`, `maria.santos@email.com`, `pedro.costa@email.com`, `sofia.almeida@email.com` | CUSTOMER | — |

> ⚠️ This data is for development only and is never loaded outside the `dev` profile.

### Endpoints

Every endpoint except sign-up, login and documentation requires the `Authorization: Bearer <token>` header.

#### Authentication — `/api/auth` (public)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Registers a customer |
| POST | `/api/auth/register/professional` | Registers a provider (starts as `PENDING`) |
| POST | `/api/auth/login` | Authenticates and returns a JWT |

#### Administration — `/api/admin/professionals` (ADMIN)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/admin/professionals?status=PENDING` | Lists providers by status (defaults to `PENDING`) |
| PATCH | `/api/admin/professionals/{id}/approve` | Approves a provider |
| PATCH | `/api/admin/professionals/{id}/reject` | Rejects a provider |

#### Services — `/api/services`

| Method | Endpoint | Roles | Description |
|---|---|---|---|
| GET | `/api/services` | All | Lists services according to the caller's role |
| GET | `/api/services?professionalId={id}` | ADMIN, CUSTOMER | Services of a specific provider |
| GET | `/api/services/{id}` | All | Service details |
| POST | `/api/services` | ADMIN, PROFESSIONAL | Creates a service |
| PUT | `/api/services/{id}` | ADMIN, PROFESSIONAL | Updates a service |
| DELETE | `/api/services/{id}` | ADMIN, PROFESSIONAL | Deletes a service |

#### Customers — `/api/customers`

| Method | Endpoint | Roles | Description |
|---|---|---|---|
| GET | `/api/customers/me` | CUSTOMER | Authenticated customer's profile |
| GET | `/api/customers` | ADMIN | Lists customers |
| GET | `/api/customers/{id}` | ADMIN | Customer details |
| POST | `/api/customers` | ADMIN | Creates a customer (no account) |
| PUT | `/api/customers/{id}` | ADMIN | Updates a customer |
| DELETE | `/api/customers/{id}` | ADMIN | Deletes a customer |

#### Appointments — `/api/appointments`

| Method | Endpoint | Roles | Description |
|---|---|---|---|
| GET | `/api/appointments` | All | Lists appointments according to the caller's role |
| GET | `/api/appointments/{id}` | All (owner) | Appointment details |
| POST | `/api/appointments` | ADMIN, CUSTOMER | Books an appointment |
| PUT | `/api/appointments/{id}` | ADMIN | Reschedules / changes status and notes |
| PATCH | `/api/appointments/{id}/confirm` | ADMIN, PROFESSIONAL | Confirms |
| PATCH | `/api/appointments/{id}/complete` | ADMIN, PROFESSIONAL | Marks as completed |
| PATCH | `/api/appointments/{id}/cancel` | All (owner) | Cancels |

#### Usage example

```bash
# 1. Log in
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"joao.silva@email.com","password":"password"}'

# 2. List provider Ana Ribeiro's services
curl "http://localhost:8080/api/services?professionalId=4" \
  -H "Authorization: Bearer <token>"

# 3. Book an appointment
curl -X POST http://localhost:8080/api/appointments \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"customerId":2,"serviceId":2,"startAt":"2030-01-15T10:00:00","notes":"First visit"}'
```

### Error handling

Errors are returned in a consistent JSON format by `GlobalExceptionHandler`:

```json
{
  "timestamp": "2026-10-09T20:15:00",
  "status": 400,
  "error": "Business Rule Violation",
  "message": "O profissional já possui um agendamento conflituoso neste intervalo de horários."
}
```

| Status | When |
|---|---|
| `400` | Validation error (`messages` holds each field's error) or business rule violation |
| `401` | Missing, invalid or expired token |
| `403` | Role not allowed, or resource owned by another user |
| `404` | Resource not found |
| `409` | Conflict with the current data (e.g. deleting a record that has appointments) |

> API messages are returned in Portuguese.

### Configuration

Configuration is split by Spring profile:

| File | Profile | Contents |
|---|---|---|
| `application.yml` | all | Base configuration. **Holds no secrets**: reads them from environment variables. |
| `application-dev.yml` | `dev` (**active by default**) | Local credentials, development JWT key, SQL and debug logging, test data. |

Nothing needs to be configured for development: the `dev` profile is activated automatically.

#### Environment variables (production)

| Variable | Description | Required |
|---|---|:-:|
| `SPRING_PROFILES_ACTIVE` | Use `prod` (or any profile other than `dev`) | ✅ |
| `DB_URL` | PostgreSQL JDBC URL | ✅ |
| `DB_USERNAME` / `DB_PASSWORD` | Database credentials | ✅ |
| `JWT_SECRET` | JWT signing key (at least 32 characters) | ✅ |
| `JWT_EXPIRATION` | Token lifetime in ms (defaults to `86400000`, 24 h) | ❌ |

```bash
SPRING_PROFILES_ACTIVE=prod \
DB_URL=jdbc:postgresql://db:5432/agendamentodb DB_USERNAME=app DB_PASSWORD=*** \
JWT_SECRET="$(openssl rand -base64 48)" \
java -jar target/agendamento-0.0.1-SNAPSHOT.jar
```

> 🔐 If any required variable is missing, the application **refuses to start** instead of running with insecure values. The JWT key in `application-dev.yml` is public and meant for development only. Test data (`db/testdata`) is only loaded under the `dev` profile.

> 🗄️ Migration V7 creates the `btree_gist` extension. On PostgreSQL 13+ it is *trusted*, so the application user only needs to own the database; on older versions, create it once as a superuser.

### Roadmap

- [x] Unit and integration tests (Testcontainers) for the booking rules
- [ ] Service categories and search by service type
- [ ] Provider working hours and availability
- [ ] Pagination and sorting on list endpoints
- [ ] Application Dockerfile and CI pipeline

---

Desenvolvido por / Developed by **[Thiago Bilangieri](https://github.com/Thiago-Bilangieri)**
