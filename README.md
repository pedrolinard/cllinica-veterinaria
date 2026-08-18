# Vet Clinic API

API REST para gestão de uma clínica veterinária, construída em **Java 21 + Spring Boot 3**. Projeto pensado para portfólio: cobre autenticação com JWT, controle de acesso por papéis, relacionamentos entre entidades, uma regra de negócio não trivial (conflito de agenda) e documentação interativa via Swagger.

## Funcionalidades

- **Autenticação JWT** com papéis: `ADMIN`, `VET` (veterinário) e `RECEPTIONIST` (recepcionista).
- **Clientes (tutores)** e **pets**, com relacionamento 1:N e paginação.
- **Agendamento de consultas**, com validação de conflito de horário por veterinário (um mesmo veterinário não pode ter duas consultas sobrepostas) e transições de status controladas (`SCHEDULED` → `CONFIRMED` → `COMPLETED` / `CANCELED`).
- **Prontuários médicos**, vinculados a uma consulta concluída (regra de negócio: só é possível registrar prontuário de uma consulta com status `COMPLETED`).
- **Catálogo de serviços** (consulta, vacinação, banho e tosa, etc).
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
cp .env.example .env   # opcional, usado pelo docker-compose

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
docker compose up --build
```

Isso sobe um Postgres e a API já configurada com o perfil `prod`, apontando para o banco em container. O schema é criado automaticamente (`ddl-auto: update`) e o seed de dados de exemplo roda por padrão, para você conseguir testar de imediato — em um ambiente de produção real, defina `SEED_ENABLED=false` e adote uma ferramenta de migration versionada (Flyway/Liquibase) em vez de `ddl-auto`.

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
| POST | `/api/auth/login` | Autentica e retorna o JWT | Público |
| GET | `/api/auth/me` | Dados do usuário autenticado | Autenticado |
| CRUD | `/api/users` | Gestão de funcionários | ADMIN |
| CRUD | `/api/clients` | Tutores | Leitura: autenticado · Escrita: ADMIN/RECEPTIONIST |
| CRUD | `/api/pets` | Pets (filtro `?clientId=`) | Leitura: autenticado · Escrita: ADMIN/RECEPTIONIST |
| CRUD | `/api/appointments` | Consultas (filtros `?vetId=` `?petId=`) | Leitura: autenticado · Escrita: ADMIN/RECEPTIONIST |
| PATCH | `/api/appointments/{id}/status` | Atualiza status da consulta | ADMIN/RECEPTIONIST/VET |
| CRUD | `/api/medical-records` | Prontuários (filtro `?petId=`) | Leitura: autenticado · Escrita: VET/ADMIN |
| CRUD | `/api/services` | Catálogo de serviços | Leitura: autenticado · Escrita: ADMIN |

Lista completa, com schemas de request/response, em `/docs`.

## Possíveis evoluções

- Paginação e busca textual (ex: por nome de pet/tutor) mais avançada.
- Notificações por email/SMS ao agendar ou confirmar uma consulta.
- Upload de exames/imagens anexados ao prontuário.
- Testes unitários adicionais para `AppointmentService` isolando o repositório com mocks (Mockito), complementando os testes de integração já existentes.
- Migrations versionadas com Flyway (hoje o schema é gerenciado por `ddl-auto`, adequado para portfólio, mas não recomendado em produção real).

---

Projeto de portfólio desenvolvido por Pedro Linard.
