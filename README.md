# Vet Clinic API

API REST para gestão de uma clínica veterinária, construída em **Java 21 + Spring Boot 3**. Projeto pensado para portfólio: cobre autenticação com JWT, controle de acesso por papéis, relacionamentos entre entidades, uma regra de negócio não trivial (conflito de agenda) e documentação interativa via Swagger.

## Funcionalidades

- **Autenticação JWT** com papéis: `ADMIN`, `VET` (veterinário) e `RECEPTIONIST` (recepcionista).
- **Clientes (tutores)** e **pets**, com relacionamento 1:N e paginação.
- **Agendamento de consultas**, com validação de conflito de horário por veterinário (um mesmo veterinário não pode ter duas consultas sobrepostas) e transições de status controladas (`SCHEDULED` → `CONFIRMED` → `COMPLETED` / `CANCELED`).
- **Prontuários médicos**, vinculados a uma consulta concluída (regra de negócio: só é possível registrar prontuário de uma consulta com status `COMPLETED`).
- **Catálogo de serviços** (consulta, vacinação, banho e tosa, etc), opcionalmente vinculado a cada consulta.
- **Dashboard** com estatísticas agregadas (contagens, consultas por status, pets por espécie) calculadas no banco.
- **Busca por nome** em clientes e pets (`?q=`), usada pelos campos de busca do painel em vez de carregar a lista inteira.
- **Anexos de prontuário**: upload/download de exames e fotos (imagem ou PDF, até 5MB).
- **Relatórios em CSV**: agenda do dia e faturamento por serviço num período.
- **Lembrete de consulta por email** na véspera — desligado por padrão, liga com `REMINDERS_ENABLED=true` + SMTP configurado.
- **Troca de senha self-service**, sem depender do ADMIN.
- **Documentação OpenAPI/Swagger** navegável em `/docs`.
- **Seed automático** de dados de exemplo ao subir a aplicação pela primeira vez.

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.3 (Web, Data JPA, Security, Validation) |
| Autenticação | JWT (io.jsonwebtoken / jjwt) |
| Banco (dev/portfólio) | H2 em arquivo — zero configuração |
| Banco (produção) | PostgreSQL |
| Documentação | springdoc-openapi (Swagger UI) |
| Build | Maven |
| Testes | JUnit 5, MockMvc, Spring Security Test |
| Container | Docker / docker-compose |

## Arquitetura

O código é organizado por **módulo de domínio** (não por camada técnica), o que facilita navegar e evoluir o projeto:

```
src/main/java/com/vetclinic/api/
├── auth/              # login, endpoint /me
├── user/              # funcionários da clínica (staff) e seus papéis
├── client/             # tutores
├── pet/                # pets
├── appointment/        # agendamento de consultas + regra de conflito de horário
├── medicalrecord/      # prontuários médicos
├── service/            # catálogo de serviços da clínica
├── security/           # JWT, UserDetails, filtro de autenticação
├── config/             # Spring Security, OpenAPI, seed de dados
└── common/             # BaseEntity, exceções e tratamento de erro global
```

Cada módulo segue o mesmo padrão: `Entity` → `Repository` (Spring Data JPA) → `Service` (regras de negócio, transações) → `Controller` (HTTP + autorização) → `dto/` (records de request/response, isolando a API do modelo de persistência).

### Decisões técnicas relevantes

- **DTOs como Java records**: imutáveis, concisos, e com Bean Validation (`@NotBlank`, `@Email`, etc.) direto nos campos.
- **`BaseEntity`** centraliza `id` (UUID) e timestamps (`createdAt`/`updatedAt`), evitando repetição em cada entidade.
- **Regra de conflito de agenda**: ao criar/reagendar uma consulta, o serviço busca os agendamentos ativos do veterinário no mesmo dia e verifica sobreposição de intervalos (`scheduledAt` até `scheduledAt + durationMin`) em memória — simples de entender e testar, e evita depender de sintaxe de data específica de cada banco (H2 vs Postgres).
- **`@PreAuthorize` por papel** nos controllers (ex: só `ADMIN`/`RECEPTIONIST` agendam consultas; só `VET`/`ADMIN` registram prontuários), demonstrando autorização granular além de "autenticado ou não".
- **`GlobalExceptionHandler`** centraliza a tradução de exceções (validação, recurso não encontrado, conflito, credenciais inválidas) em respostas HTTP consistentes.
- **Perfis Spring** (`dev`, `test`, `prod`) separam configuração de banco: H2 em arquivo para rodar localmente sem setup, H2 em memória para os testes, Postgres para produção/Docker.

## Como rodar localmente

Pré-requisitos: JDK 21 e Maven (ou use o wrapper, se preferir gerar um com `mvn -N wrapper:wrapper`).

```bash
# Sobe a aplicação com o perfil "dev" (H2 em arquivo, sem precisar de banco externo)
mvn spring-boot:run
```

A API sobe em `http://localhost:8080`. Documentação interativa em `http://localhost:8080/docs`.

Na primeira execução, o `DataSeeder` popula o banco com usuários e dados de exemplo:

| Papel | Email | Senha |
|---|---|---|
| ADMIN | admin@vetclinic.com | admin123 |
| VET | vet@vetclinic.com | vet12345 |
| RECEPTIONIST | recepcao@vetclinic.com | recep1234 |

Use o endpoint `POST /api/auth/login` com um desses usuários para obter o token JWT e testar as rotas protegidas pelo Swagger (botão **Authorize**, formato `Bearer <token>`).

## Rodando com Docker (Postgres)

```bash
cp .env.example .env   # preencha JWT_SECRET e SEED_ADMIN_PASSWORD antes de continuar
docker compose up --build
```

O container da API recusa subir sem `JWT_SECRET` e `SEED_ADMIN_PASSWORD` definidos no `.env` — de propósito, para não rodar com os segredos padrão do repositório. Isso sobe um Postgres e a API com o perfil `prod`; o schema é criado por migrations versionadas do Flyway (`src/main/resources/db/migration`), não por `ddl-auto`. O seed de dados de exemplo roda por padrão para você testar de imediato — em produção real, defina `SEED_ENABLED=false`.

## Testes

```bash
mvn test
```

Os testes de integração usam `MockMvc` + H2 em memória (perfil `test`) e cobrem:

- Login com credenciais válidas/inválidas e acesso ao perfil autenticado (`/api/auth/me`).
- CRUD de clientes, incluindo autorização por papel (recepcionista pode criar, veterinário não) e validação de campos.
- A regra de negócio central de agendamento: duas consultas sobrepostas para o mesmo veterinário devem falhar com `409 Conflict`, enquanto consultas sequenciais (sem sobreposição) devem funcionar; também valida que só um usuário com papel `VET` pode ser atribuído como veterinário responsável.

## Principais endpoints

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| POST | `/api/auth/login` | Autentica e retorna o JWT | Público (limitado a 5 tentativas / 15min por email) |
| POST | `/api/auth/logout` | Revoga o token atual | Autenticado |
| GET | `/api/auth/me` | Dados do usuário autenticado | Autenticado |
| PATCH | `/api/users/me/password` | Troca a própria senha | Autenticado |
| CRUD | `/api/users` | Gestão de funcionários | ADMIN |
| CRUD | `/api/clients` | Tutores (busca `?q=`) | Leitura: autenticado · Escrita: ADMIN/RECEPTIONIST |
| CRUD | `/api/pets` | Pets (filtros `?clientId=` `?q=`) | Leitura: autenticado · Escrita: ADMIN/RECEPTIONIST |
| CRUD | `/api/appointments` | Consultas (filtros `?vetId=` `?petId=`) | Leitura: autenticado · Escrita: ADMIN/RECEPTIONIST |
| PATCH | `/api/appointments/{id}/status` | Atualiza status da consulta | ADMIN/RECEPTIONIST/VET |
| CRUD | `/api/medical-records` | Prontuários (filtro `?petId=`) | Leitura: autenticado · Escrita: VET/ADMIN |
| CRUD | `/api/medical-records/{id}/attachments` | Anexos do prontuário | Leitura: autenticado · Upload: VET/ADMIN · Exclusão: ADMIN |
| CRUD | `/api/services` | Catálogo de serviços | Leitura: autenticado · Escrita: ADMIN |
| GET | `/api/dashboard/stats` | Estatísticas agregadas do painel | Autenticado |
| GET | `/api/reports/daily-agenda` | Agenda de um dia (CSV) | ADMIN/RECEPTIONIST |
| GET | `/api/reports/billing` | Faturamento por serviço num período (CSV) | ADMIN |

Lista completa, com schemas de request/response, em `/docs`.

## Possíveis evoluções

- Exportação de relatórios também em PDF (hoje só CSV).
- Portal do tutor: agendar e ver o prontuário do próprio pet.
- Auditoria (quem alterou o quê e quando).
- CI: build + testes em cada PR.
- Testes unitários adicionais para `AppointmentService` isolando o repositório com mocks (Mockito), complementando os testes de integração já existentes.

---

Projeto de portfólio desenvolvido por Pedro Linard.
