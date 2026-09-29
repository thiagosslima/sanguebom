# 🩸 Sangue Bom

Plataforma de acompanhamento longitudinal de índices sanguíneos para cidadãos atendidos pelo SUS.

O **Sangue Bom** centraliza o histórico de exames, acompanha a evolução dos indicadores de saúde, ajuda a identificar possíveis situações de risco e facilita o acesso do profissional de saúde à evolução dos dados do cidadão.

O projeto é um MVP do **Hackathon FIAP Pós Tech, Arquitetura e Desenvolvimento Java, Fase 5**, com foco em inovação para otimização do atendimento no SUS.

## 🚀 Stack

| Camada | Tecnologia |
| --- | --- |
| Linguagem | Java 21 |
| Framework | Spring Boot 4.1 (Web MVC, Data JPA, Validation, Actuator) |
| Banco de dados | PostgreSQL 18 + Flyway |
| Mapeamento | MapStruct 1.6 + Lombok |
| Documentação da API | springdoc-openapi 3 (Swagger UI) |
| Tratamento de erros | error-handling-spring-boot-starter |
| Testes | JUnit 5, Mockito, MockMvc, JaCoCo |
| Qualidade | SpotBugs, CodeQL (GitHub Actions) |
| Infra | Docker + Docker Compose |

## 🏗️ Arquitetura

A aplicação é um monólito Spring MVC organizado em camadas:

```text
            ┌───────────────────────────┐
            │  Cliente (Swagger/Postman)│
            └─────────────┬─────────────┘
                          │ HTTP / JSON · SSE
                          ▼
            ┌───────────────────────────┐
            │  Controller  (*Resource)  │  DTO + Bean Validation
            └─────────────┬─────────────┘
                          ▼
            ┌───────────────────────────┐        ┌──────────────────────────┐
            │  Service                  │───────▶│  rulesMotor              │
            │  regras + @Transactional  │        │  análise de exame, score │
            └──────┬──────────────┬─────┘        │  de risco, conquistas    │
                   │              │              └──────────────────────────┘
                   │              │ NotificationCreatedEvent
                   │              ▼ (após o commit)
                   │     ┌──────────────────────────┐
                   │     │  Notificações            │──▶ SSE para o cidadão
                   │     │  listener + dispatcher   │
                   │     └──────────────────────────┘
                   ▼                        ▲
            ┌───────────────────────────┐   │ @Scheduled (job diário
            │  Repository (Spring Data) │   │ da meta de exames)
            └─────────────┬─────────────┘
                          ▼
            ┌───────────────────────────┐
            │  PostgreSQL (Flyway)      │
            └───────────────────────────┘
```

Regras seguidas no código:

* Controllers só recebem e devolvem DTOs. Não têm regra de negócio e não expõem entidades JPA. A conversão fica nos mappers MapStruct (`mapper/`).
* Os Services concentram as regras e as fronteiras transacionais.
* O schema é controlado só pelo Flyway. O Hibernate roda com `ddl-auto=validate`.
* Os erros são padronizados pelo `GlobalExceptionHandler` e pelo error-handling-spring-boot-starter.
* A data e hora vêm de um `Clock` injetável (`ClockConfig`), o que torna os testes determinísticos.

### Estrutura de pacotes

```text
src/main/java/br/com/fiap/sanguebom/
├── config/          # Clock, Jackson, OpenAPI/Swagger, agendamento, propriedades
│   └── swagger/     # documentação dos endpoints (textos em resources/swagger/docs)
├── controller/      # endpoints REST (*Resource)
├── exception/       # exceções de domínio e handler global
├── mapper/          # mappers MapStruct entidade <-> DTO
├── model/
│   ├── entities/    # entidades JPA
│   ├── enums/       # status, flags, níveis de risco, tipos de notificação
│   ├── dtos/        # DTOs dos CRUDs
│   └── ...          # DTOs por feature (exam, doctor, userexam, notification, catalog...)
├── repository/      # Spring Data JPA
├── rulesMotor/
│   ├── exam/        # motor de pré-diagnóstico (flags, score, nível de risco)
│   └── achievement/ # motor de conquistas (gamificação)
├── service/         # regras de negócio
│   └── notification/# eventos, entrega, SSE e job da meta de exames
└── util/
```

## ❤️ Funcionalidades e serviços

### Cadastro

* **Cidadão** (`AppUserService`): cadastro, consulta e atualização.
* **Perfil de saúde** (`HealthProfileService`): sexo e demais dados usados pelo motor de regras.
* **Unidades de saúde** (`HealthUnitService`): locais onde os exames são coletados.

### Catálogo de exames

* **Itens de exame** (`ExamItemService`): marcadores como hemoglobina e glicose, com unidade de medida.
* **Faixas de referência** (`ReferenceRangeService`): faixas por item, sexo e idade.
* **Regras** (`RuleService`): intervalos de valor dentro de cada faixa. Cada regra define uma flag (`NORMAL`, `LOW`, `HIGH`, `ATTENTION`...) e uma pontuação.
* A consulta pública do catálogo fica em `/api/v1/exam-items`.

### Registro e pré-diagnóstico de exames

Fluxo de `ExamService.create`, executado em uma única transação:

```text
Exame recebido ──▶ COLLECTED ──▶ IN_ANALYSIS ──▶ ExamAnalysisService
                                                   │  para cada item: acha a faixa (sexo + idade)
                                                   │  e a regra que contém o valor → flag + pontos
                                                   ▼
                                     score = média dos pontos dos itens
                                                   ▼
                                     RiskAssessmentLevelResolver
                                       LOW < 3 ≤ MODERATE < 6 ≤ HIGH < 8 ≤ VERY_HIGH
                                                   ▼
                        RELEASED ──▶ notificação "resultado disponível" ──▶ avaliação de conquistas
```

Para ser analisado, o exame precisa de um cidadão com data de nascimento e perfil de saúde com sexo informado. Itens duplicados no mesmo exame são rejeitados.

### Meta e histórico do cidadão

* **Meta de exames** (`ExamGoalService`): calcula a próxima data de exame a partir do último exame e da periodicidade do perfil (padrão: anual). O status é `UP_TO_DATE`, `DUE_SOON` (≤ 30 dias), `OVERDUE` ou `NO_HISTORY`.
* **Exames do cidadão** (`UserExamService`): listagem paginada e detalhe do exame com os resultados por item.
* **Avaliações de risco** (`RiskAssessmentService`): histórico e evolução do score ao longo do tempo.

### Notificações

* Tipos: `EXAM_RESULT_AVAILABLE`, `EXAM_GOAL_DUE_SOON`, `EXAM_GOAL_OVERDUE` e `REMINDER`.
* Quando uma notificação é criada, é publicado um `NotificationCreatedEvent`. Depois do commit, o `NotificationDeliveryListener` entrega a notificação pelo `NotificationDispatcher`.
* Entrega em tempo real via **Server-Sent Events** (`NotificationStreamService`), com heartbeat periódico para manter a conexão aberta.
* Job diário `ExamGoalNotificationJob` que varre as metas de exame. Padrão: 08:00, America/Sao_Paulo. Também pode ser disparado manualmente em `POST /api/notifications/exam-goal-scan`.

### Gamificação

O `AchievementEngine` avalia as regras de conquista depois de cada exame analisado:

| Código | Regra |
| --- | --- |
| `SANGUE_BOM` | `SangueBomAchievementRule` |
| `HEALTH_CHAMPION` | `HealthChampionAchievementRule` |
| `PUNCTUAL` | `PunctualAchievementRule` |

### Visão do médico

* **Timeline de um marcador** (`DoctorTimelineService`): evolução de um item de exame no tempo, com a faixa de referência vigente em cada ponto.
* **Comparação de exames** (`DoctorExamComparisonService`): compara itens entre dois ou mais exames do paciente e mostra a variação.

### 🩺 Disclaimer médico

O pré-diagnóstico do Sangue Bom tem caráter exclusivamente informativo.

> Este é um resultado informativo gerado automaticamente a partir de valores de referência da literatura médica. Não constitui diagnóstico e não substitui a avaliação de um profissional de saúde.

## 📡 Endpoints

A documentação completa, com schemas e exemplos, está no Swagger (veja [Verificando a aplicação](#-verificando-a-aplicação)).

### Fluxos do cidadão e do médico

| Método | Rota | Descrição |
| --- | --- | --- |
| `GET` | `/api/users/{userId}/exam-goal` | Situação da meta de exames |
| `GET` | `/api/users/{userId}/exams` | Exames do cidadão (paginado) |
| `GET` | `/api/users/{userId}/exams/{examId}` | Detalhe de um exame |
| `GET` | `/api/users/{userId}/notifications` | Notificações do cidadão |
| `GET` | `/api/users/{userId}/notifications/stream` | Stream SSE de notificações |
| `GET` | `/api/riskAssessments/{userId}/timeline` | Evolução do score de risco |
| `GET` | `/api/v1/doctor/patients/{userId}/timeline?itemCode=` | Timeline de um marcador |
| `GET` | `/api/v1/doctor/patients/{userId}/exams/compare?examIds=` | Comparação entre exames |
| `GET` | `/api/v1/exam-items` | Catálogo de itens de exame |
| `GET` | `/api/v1/exam-items/{code}/reference-ranges` | Faixas e regras de um item |
| `POST` | `/api/notifications/exam-goal-scan` | Dispara manualmente a varredura das metas |

### CRUDs

Todos seguem o padrão `GET /recurso`, `GET /recurso/{id}`, `POST /recurso` e `PUT /recurso/{id}`:

| Recurso | Rota base |
| --- | --- |
| Cidadãos | `/api/appUsers` |
| Perfis de saúde | `/api/healthProfiles` |
| Unidades de saúde | `/api/healthUnits` |
| Exames (o `POST` dispara a análise) | `/api/exams` |
| Resultados de exame | `/api/examResults` |
| Itens de exame | `/api/examItems` |
| Faixas de referência | `/api/referenceRanges` |
| Regras | `/api/rules` |
| Avaliações de risco | `/api/riskAssessments` |
| Notificações | `/api/notifications` |
| Conquistas | `/api/achievements` |
| Conquistas do cidadão | `/api/userAchievements` |

## ▶️ Como rodar

### Pré-requisitos

* Docker e Docker Compose
* Para rodar fora do container: JDK 21 e Maven 3.9+

### 1. Configurar o `.env`

```bash
cp .env.example .env
```

| Variável | Descrição | Padrão |
| --- | --- | --- |
| `DB_HOST` | Host do PostgreSQL (no Compose é sobrescrito para `postgres`) | `localhost` |
| `DB_PORT` | Porta do PostgreSQL exposta no host | `5432` |
| `DB_NAME` | Nome do banco | `sanguebom` |
| `DB_USERNAME` | Usuário do banco (obrigatório) | `postgres` |
| `DB_PASSWORD` | Senha do banco (obrigatório) | `postgres` |
| `DB_PARAMS` | Parâmetros extras da URL JDBC (ex.: `?sslmode=require`) | vazio |

> O `.env` não é versionado. Não coloque credenciais reais no `.env.example`.

### 2a. Subir tudo com Docker Compose

```bash
docker compose up --build        # em primeiro plano
docker compose up --build -d     # em segundo plano
```

O Compose sobe dois serviços na rede `sanguebom-network`:

| Serviço | Imagem | Porta | Papel |
| --- | --- | --- | --- |
| `postgres` | `postgres:18.4` | `${DB_PORT}` → 5432 | Banco de dados, com healthcheck `pg_isready` |
| `sanguebom` | build do `Dockerfile` (multi-stage, Corretto 21) | `8080` | API. Só sobe quando o banco está saudável |

Dentro da rede Docker, a aplicação acessa o banco pelo hostname `postgres`. Do host, o banco fica em `localhost:${DB_PORT}`.

Comandos úteis:

```bash
docker compose logs -f sanguebom   # logs da aplicação
docker compose logs -f postgres    # logs do banco
docker compose down                # para os containers
docker compose build --no-cache    # rebuild sem cache
docker compose down -v             # para e APAGA os dados do banco
```

### 2b. Rodar a aplicação localmente (fora do container)

Suba só o banco e rode a aplicação pelo Maven:

```bash
docker compose up -d postgres
mvn spring-boot:run
```

As variáveis do `.env` precisam estar exportadas no shell (ou configuradas na IDE). Como o `spring-boot-docker-compose` está no classpath com `lifecycle-management=start-only`, a aplicação também consegue subir o banco do `docker-compose.yml` sozinha na inicialização.

### 3. Verificando a aplicação

| O quê | URL |
| --- | --- |
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| OpenAPI (JSON) | http://localhost:8080/v3/api-docs |

As migrations de seed já carregam o catálogo de exames, as faixas e regras de referência, as conquistas, as unidades de saúde e cidadãos de demonstração com exames. Assim, os fluxos podem ser testados direto pelo Swagger.

## 🧬 Migrations

O Flyway aplica as migrations automaticamente na inicialização (`src/main/resources/db/migration`):

```text
V20260816000000__CRIACAO_TABELAS.sql
V20260821195000__INSERT_INICIAL_DADOS.sql
V20260825220000__INCLUSAO_CAMPO_SEX_TABELA_APP_USER.sql
V20260826001500__CREATE_PRIMARY_SEQUENCE.sql
V20260902100000__INSERT_EXAMES_CITIZENS_E_NOVOS_ITENS.sql
V20260907120000__NOTIFICACAO_EVENTOS.sql
V20260917120000__ACHIEVEMENTS_GAMIFICACAO.sql
V20260928120000__CORRECAO_SEXO_FAIXAS_REFERENCIA.sql
V20260929120000__REVERTE_SEXO_FAIXAS_REFERENCIA_INICIAIS.sql
V20260929140000__DEFINE_SEXO_OBRIGATORIO_FAIXAS_REFERENCIA.sql
```

Novas migrations seguem o padrão `V<yyyyMMddHHmmss>__DESCRICAO.sql`. Nunca altere uma migration que já foi aplicada, porque o `validate-on-migrate` está ligado.

## 🧪 Testes e qualidade

O teste `SanguebomApplicationTests` sobe o contexto completo e aplica as migrations, então precisa de um PostgreSQL rodando:

```bash
docker compose up -d postgres
DB_USERNAME=postgres DB_PASSWORD=postgres SPRING_DOCKER_COMPOSE_ENABLED=false mvn verify
```

* Relatório de cobertura (JaCoCo): `target/site/jacoco/index.html`
* Análise estática local: `mvn spotbugs:spotbugs`

No GitHub Actions, os PRs para `develop` e `main` rodam os testes, o gate de **80% de cobertura nas linhas novas** e a análise estática (CodeQL + SpotBugs). Detalhes em [`.github/README.md`](.github/README.md).

## 👥 Projeto

**Sangue Bom**, Hackathon FIAP, Pós Tech, Arquitetura e Desenvolvimento Java, Turma 11ADJT.

**Tema:** Inovação para otimização de atendimento no SUS.

---

## 📄 Licença

Projeto desenvolvido para fins acadêmicos no Hackathon FIAP.
