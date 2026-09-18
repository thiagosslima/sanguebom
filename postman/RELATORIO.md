# Relatório da execução da collection

Gerado a partir da execução de `./scripts/api-test.sh` contra um banco recriado do zero
(`./scripts/start.sh --clean`), com todas as 7 migrations aplicadas.

**Resultado:** 136 requests, 195 asserções, **19 falhas — todas na pasta `10 - Bugs conhecidos`**.
As pastas `00` a `09` passam 100%. Cada falha da pasta 10 corresponde a um dos 10 bugs abaixo:
a asserção descreve o comportamento correto e falha enquanto o defeito existir.

Dos 59 endpoints, **14 estão total ou parcialmente quebrados**.

---

## BUG 1 — `@Size` sobre enum derruba 6 endpoints de escrita

**Severidade: alta.** POST e PUT de três recursos respondem sempre 500.

| Endpoint | Hoje |
|---|---|
| `POST /api/referenceRanges` | 500 |
| `PUT /api/referenceRanges/{id}` | 500 |
| `POST /api/rules` | 500 |
| `PUT /api/rules/{id}` | 500 |
| `POST /api/examResults` | 500 |
| `PUT /api/examResults/{id}` | 500 |

```
HV000030: No validator could be found for constraint
'jakarta.validation.constraints.Size' validating type
'br.com.fiap.sanguebom.model.enums.Sex'. Check configuration for 'sex'
```

`@Size` vale para `String` e `Collection`, não para enum. O Hibernate Validator resolve o
validador ao montar os metadados do DTO, **antes de olhar o valor** — por isso o erro acontece
mesmo quando o campo não é enviado no corpo. Não existe workaround pelo lado do cliente.

- `model/dtos/ReferenceRangeDTO.java:22` — `@Size(max = 20)` sobre `Sex sex`
- `model/dtos/RuleDTO.java:35` — `@Size(max = 30)` sobre `ExamResultFlag level`
- `model/dtos/ExamResultDTO.java:32` — `@Size(max = 30)` sobre `ExamResultFlag flag`

**Correção:** remover as três anotações. O tamanho da coluna já é garantido por
`@Column(length = …)` na entidade, e o valor já é restrito pelo próprio enum. Se a intenção era
exigir preenchimento, a anotação correta é `@NotNull`.

**Consequência prática:** nenhuma regra clínica ou faixa de referência pode ser cadastrada pela
API — só por migration.

---

## BUG 2 — `reference_range.sex = 'ALL'` não existe no enum `Sex`

**Severidade: alta.** Quebra a listagem e o catálogo público.

| Endpoint | Hoje |
|---|---|
| `GET /api/referenceRanges` | 500 |
| `GET /api/v1/exam-items/GLI_JEJUM/reference-ranges` | 500 |
| `GET /api/v1/exam-items/COL_TOTAL/reference-ranges` | 500 |

```
No enum constant br.com.fiap.sanguebom.model.enums.Sex.ALL
```

`db/migration/V20260821195000__INSERT_INICIAL_DADOS.sql:27-28` grava `'ALL'`, mas
`model/enums/Sex.java` só tem `M`, `F`, `MALE`, `FEMALE`. A coluna é
`@Enumerated(EnumType.STRING)`, então qualquer consulta que carregue essas duas linhas estoura.

Os 11 itens da `V20260902100000` gravam `sex = NULL` e funcionam normalmente — o problema é
restrito aos dois itens da carga inicial.

**Correção (escolher uma):** migration que faça `UPDATE reference_range SET sex = NULL WHERE sex = 'ALL'`
(NULL já é a convenção usada para "vale para ambos os sexos"), ou acrescentar `ALL` ao enum `Sex`.

> Observação: no banco local que existia antes deste trabalho, essas linhas estavam com `sex = NULL`
> — alguém corrigiu direto no banco em vez de na migration. Por isso o bug só reaparece em
> ambiente novo, que é exatamente o que acontece em CI e em qualquer máquina nova.

---

## BUG 3 — `rule.level = 'CRITICO'` não existe no enum `ExamResultFlag`

**Severidade: alta.** `GET /api/rules` responde 500.

```
No enum constant br.com.fiap.sanguebom.model.enums.ExamResultFlag.CRITICO
```

Mesmo padrão do BUG 2: `V20260821195000__INSERT_INICIAL_DADOS.sql:34` grava `'CRITICO'`, ausente
do enum (`IN_ANALYSIS`, `NORMAL`, `ATTENTION`, `LOW`, `HIGH`, `OPTIMAL`, `VERY_HIGH`, `ALERTA`).

**Correção:** migration trocando `'CRITICO'` por `'VERY_HIGH'` (que é o valor usado pelas regras
equivalentes da `V20260902100000`).

---

## BUG 4, 5 e 6 — `/api/userAchievements` quebrado em 3 dos 4 handlers

**Severidade: alta.** A entidade tem chave composta; o recurso trata como `Long`.

| Endpoint | Hoje | Erro |
|---|---|---|
| `POST /api/userAchievements` | 500 | `Identifier of entity '…UserAchievement' must be manually assigned before calling 'persist()'` |
| `GET /api/userAchievements/{id}` | 500 | `Supplied id had wrong type: … has id type '…UserAchievementId' but supplied id was of type 'java.lang.Long'` |
| `PUT /api/userAchievements/{id}` | 500 | idem |
| `GET /api/userAchievements` | 200 | funciona, mas os `id` vêm repetidos |

`model/entities/UserAchievement.java` usa `@EmbeddedId UserAchievementId` (chave composta
`user_id` + `achievement_id`), e a tabela `user_achievement` **não tem coluna `id`** — a PK é
`(user_id, achievement_id)`.

- **BUG 4:** `service/UserAchievementService.java:39` monta a entidade sem preencher o `@EmbeddedId`.
- **BUG 5:** `controller/UserAchievementResource.java:32` e `:45` declaram `@PathVariable Long id`.
  Um recurso por id não tem como existir enquanto a chave for composta.
- **BUG 6:** `service/UserAchievementService.java:42` devolve `getId().getAchievementId()`, e o
  `UserAchievementDTO` expõe esse valor no campo `id`. Registros de cidadãos diferentes chegam ao
  cliente com o mesmo `id` — que portanto não identifica nada e não serve para nenhuma operação
  seguinte. Confirmado na execução: 6 registros, 3 ids distintos.

**Correção:** ou dar à tabela uma PK própria (`id bigserial`) e usar `@Id`, ou trocar as rotas por
`/api/users/{userId}/achievements/{achievementId}` e remover o campo `id` do DTO.

**Consequência prática:** conquistas só podem ser concedidas pelo motor de regras, nunca pela API.

---

## BUG 7 — `PUT /api/exams/{id}` cria um exame órfão em vez de atualizar

**Severidade: crítica — corrompe dados a cada chamada.**

```java
// service/ExamService.java:273
public void update(final Long id, final ExamRecoverDTO examRecoverDTO) {
    examRepository.findById(id)
            .orElseThrow(NotFoundException::new);   // o id só serve para o 404
    Exam exam = examMapper.toEntity(examRecoverDTO); // entidade NOVA, sem id
    examRepository.save(exam);                       // INSERT, não UPDATE
}
```

O `id` do path é usado apenas para a checagem de existência. Em seguida o método monta uma entidade
nova a partir do DTO — que não traz `id` — e chama `save()`, o que resulta em **INSERT**. Como o
`ExamMapper` também não mapeia `user` nem `healthUnit` a partir de `ExamRecoverDTO`, a linha nova
nasce com `user_id` e `health_unit_id` nulos.

Reproduzido: `PUT /api/exams/1` respondeu `200`, o exame 1 permaneceu idêntico, e a tabela `exam`
ganhou uma linha nova sem cidadão e sem unidade de saúde.

**Correção:** atualizar a entidade recuperada (`mapToEntity(dto, exam)` no padrão usado pelos
outros services) em vez de construir uma nova, e mapear `user`/`healthUnit` no `ExamMapper`.

---

## BUG 8 — `POST /api/appUsers` aceita e-mail e CPF duplicados

**Severidade: alta.**

`service/AppUserService.java:33` salva direto, sem nenhuma checagem de unicidade, e as colunas
`email` e `cpf_hash` não têm unique constraint. A exceção `UserAlreadyExistsException` (422) existe
no projeto mas **nunca é lançada em lugar nenhum**.

Reproduzido: dois `POST` com o mesmo `email` e o mesmo `cpfHash` responderam `201`.

Como o `cpf_hash` é a única identificação do cidadão, duplicatas fragmentam o histórico clínico
entre registros diferentes — o oposto do objetivo do produto, que é o acompanhamento longitudinal.

**Correção:** unique constraint em `cpf_hash` e `email` via migration, mais a checagem no service
lançando `UserAlreadyExistsException`.

---

## BUG 9 — `GET /api/users/{userId}/exams` não valida o cidadão

**Severidade: baixa.**

`service/UserExamService.java:43` consulta os exames sem checar se o cidadão existe e devolve uma
página vazia. `GET /api/users/999999/exams` responde `200` com `content: []`.

Diverge de `/exam-goal`, `/notifications` e dos endpoints do médico, que devolvem `404` no mesmo
cenário. O cliente não tem como distinguir "cidadão inexistente" de "cidadão sem exames".

**Correção:** reaproveitar o `UserServiceHelper` já usado pelos outros fluxos.

---

## BUG 10 — `code` duplicado de conquista derruba o `POST /api/exams`

**Severidade: alta.** Uma escrita de catálogo quebra o fluxo principal do sistema.

`achievement.code` representa uma conquista única — o enum `AchivementCode` tem 3 valores — mas não
há unique constraint na coluna nem checagem em `AchievementService.create`. `POST /api/achievements`
com um `code` que já existe responde `201`.

A partir daí, `AchievementRepository.findByCode()` (`repository/AchievementRepository.java:13`,
retorno `Optional<Achievement>`) passa a levantar:

```
Query did not return a unique result: 2 results were returned
```

E como o motor de conquistas roda dentro do `POST /api/exams`, **a ingestão de exames passa a
responder 500** de forma intermitente, dependendo de qual conquista está sendo avaliada.

**Agravante:** o projeto **não expõe nenhum endpoint DELETE** — em nenhum dos 18 controllers. A
linha duplicada não tem como ser removida sem acesso direto ao banco. Foi por isso que
`./scripts/api-test.sh` passou a recriar o ambiente por padrão.

**Correção:** unique constraint em `achievement.code` via migration, mais a checagem no service.

---

# Achados que não são de endpoint

## O Swagger não mostra a API

`src/main/resources/application.properties:32`

```properties
springdoc.pathsToMatch=/
```

Isso limita o documento OpenAPI ao path `/`. O Swagger UI sobe, mas exibe **apenas** o
`HomeResource#index` — nenhum dos 59 endpoints reais aparece. Como o projeto tem `@Tag`,
`@Operation` e `@Schema` espalhados pelos controllers e DTOs, é documentação escrita e não
publicada.

**Correção:** `springdoc.pathsToMatch=/api/**` (ou remover a linha).

## O build estava quebrado no `develop`

`src/test/java/br/com/fiap/sanguebom/service/RiskAssessmentServiceTest.java:16` importava
`br.com.fiap.sanguebom.rulesMotor.RiskAssessmentClassifier`, mas a classe está em
`br.com.fiap.sanguebom.rulesMotor.exam`. Como `mvn package -DskipTests` ainda **compila** os testes,
o `docker compose up --build` falhava na etapa de build da imagem — ou seja, o projeto não subia.

Corrigido neste trabalho (uma linha). Com a correção, `mvn test` passa: 0 falhas, 0 erros.

## A migration `V20260902100000` estava fora de ordem

Ela entrou no repositório depois que a `V20260907120000` já havia sido aplicada. Com
`validate-on-migrate=true` e out-of-order desligado, o Flyway se recusava a subir em qualquer banco
nesse estado — o que incluía o banco local deste ambiente, onde só 5 das 7 migrations constavam
aplicadas e o seed grande de exames nunca havia rodado.

Resolvido com `spring.flyway.out-of-order=true` em `application.properties`.

## O README descreve uma API que não existe

- A seção **🔐 Segurança** descreve Spring Security, JWT e os perfis `ROLE_CITIZEN` / `ROLE_LAB` /
  `ROLE_DOCTOR`. **Nada disso existe no código**: não há `spring-boot-starter-security` no
  `pom.xml`, nenhum `SecurityFilterChain`, nenhum `@PreAuthorize`. Os 59 endpoints são abertos, e
  nenhum deles pode responder 401 ou 403.
- A frase "o isolamento dos dados do cidadão é realizado a partir do usuário identificado no token"
  não se sustenta: todo endpoint por cidadão recebe o `userId` pela URL. Qualquer chamador lê os
  exames, o risco e as notificações de qualquer pessoa — inclusive a timeline clínica completa em
  `/api/v1/doctor/patients/{userId}/timeline`.
- A seção **📡 Principais endpoints** lista rotas que não existem: `/api/v1/auth/login`,
  `/api/v1/auth/token`, toda a família `/api/v1/users/me/*` e `/api/v1/labs/*`. As rotas reais são
  `/api/appUsers`, `/api/exams`, `/api/users/{userId}/...`, e só `/api/v1/doctor/patients/{userId}/...`
  e `/api/v1/exam-items` usam o prefixo `v1`.

A collection reflete o código, não o README.

---

# Cobertura

Os 59 handlers dos 18 controllers são exercitados pelo menos uma vez:

| Pasta | Requests | O que cobre |
|---|---|---|
| `00 - Smoke` | 1 | Aplicação no ar |
| `01 - Catalogo` | 13 | Catálogo público, unidades, itens; resolve os ids usados adiante |
| `02 - Fixtures` | 12 | Cidadãos e perfis de saúde dos fluxos |
| `03 - Ingestao de exame e motor de risco` | 17 | `POST /api/exams` e seus 11 caminhos de erro |
| `04 - Cidadao` | 8 | Meta, histórico, detalhe e isolamento entre cidadãos |
| `05 - Notificacoes` | 13 | Histórico, varredura da meta, CRUD de avisos |
| `06 - Medico` | 12 | Timeline de marcador e comparação de exames |
| `07 - Risco` | 9 | Avaliações e evolução do risco |
| `08 - Gamificacao` | 9 | Conquistas e conquistas por cidadão |
| `09 - CRUD administrativo` | 22 | Os handlers restantes |
| `10 - Bugs conhecidos` | 20 | Os 10 bugs acima |
| `99 - SSE (manual)` | 1 | Fora da execução automatizada |

A pasta `99` fica de fora do runner porque o `SseEmitter` mantém a conexão aberta por 30 minutos
(`sanguebom.notifications.sse.timeout-ms=1800000`) e travaria o Newman.
