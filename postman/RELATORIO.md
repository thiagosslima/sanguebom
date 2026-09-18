# Bugs encontrados pela collection

Este documento registra os defeitos que a collection expôs quando foi executada pela primeira vez
contra um banco recriado do zero, e como cada um foi corrigido.

**Todos os 10 bugs estão corrigidos.** A collection hoje roda com **139 requests e 204 asserções,
zero falhas**, e cada bug abaixo tem pelo menos uma asserção que falharia de novo se a correção
fosse revertida.

Antes das correções, **14 dos 59 endpoints estavam total ou parcialmente quebrados**.

| # | Bug | Endpoints | Correção |
|---|---|---|---|
| 1 | `@Size` sobre enum derruba a escrita | 6 | `9f0afa8` |
| 2 | `reference_range.sex = 'ALL'` | 3 | `72e2570` |
| 3 | `rule.level = 'CRITICO'` | 1 | `72e2570` |
| 4 | `POST /api/userAchievements` não persiste | 1 | `036c91c` |
| 5 | `/api/userAchievements/{id}` com chave composta | 2 | `036c91c` |
| 6 | `id` repetido na listagem de conquistas | 1 | `036c91c` |
| 7 | `PUT /api/exams` cria exame órfão | 1 | `b3eeeb2` |
| 8 | Cidadão duplicado | 1 | `3ab3142` |
| 9 | Histórico não valida o cidadão | 1 | `514b61d` |
| 10 | `code` de conquista duplicado derruba o `POST /api/exams` | 1 | `3ab3142` |

---

## BUG 1 — `@Size` sobre enum derrubava 6 endpoints de escrita

**Severidade: alta.** POST e PUT de três recursos respondiam sempre 500.

```
HV000030: No validator could be found for constraint
'jakarta.validation.constraints.Size' validating type
'br.com.fiap.sanguebom.model.enums.Sex'. Check configuration for 'sex'
```

`@Size` vale para `String` e `Collection`, não para enum. O Hibernate Validator resolve o validador
ao montar os metadados do DTO, **antes de olhar o valor** — por isso o erro acontecia mesmo quando o
campo não era enviado. Não havia workaround pelo lado do cliente: `POST`/`PUT` de
`/api/referenceRanges`, `/api/rules` e `/api/examResults` eram 500 incondicionalmente, e nenhuma
faixa de referência ou regra clínica podia ser cadastrada pela API.

**Correção:** removidas as três anotações (`ReferenceRangeDTO.sex`, `RuleDTO.level`,
`ExamResultDTO.flag`). O tamanho da coluna já é garantido por `@Column(length)` na entidade e o
conjunto de valores válidos já é restrito pelo próprio enum.

**Regressão:** os requests de `POST`/`PUT` desses três recursos na pasta `09` enviam `sex`, `level`
e `flag` preenchidos de propósito, e conferem o valor gravado na leitura seguinte.

## BUG 2 e 3 — valores de seed fora dos enums Java

**Severidade: alta.**

```
No enum constant br.com.fiap.sanguebom.model.enums.Sex.ALL
No enum constant br.com.fiap.sanguebom.model.enums.ExamResultFlag.CRITICO
```

A `V20260821195000` gravava `reference_range.sex = 'ALL'` e `rule.level = 'CRITICO'`, valores que
nunca existiram no código. Como as colunas são `@Enumerated(EnumType.STRING)`, qualquer consulta que
carregasse essas linhas estourava: `GET /api/referenceRanges`, `GET /api/rules` e o catálogo público
dos dois itens da carga inicial (`GLI_JEJUM` e `COL_TOTAL`).

**Correção:** `V20260918120000` normaliza os dois valores — `sex` para `NULL`, que já é a convenção
da `V20260902100000` para "vale para ambos os sexos", e `level` para `VERY_HIGH`, usado pelas regras
equivalentes. A correção veio em migration nova, e não na original, porque alterar um arquivo já
aplicado quebraria o checksum do Flyway.

> No banco local de quem já trabalhava no projeto essas linhas estavam corrigidas direto no SQL, e
> não por migration. Por isso o bug só aparecia em ambiente novo — CI ou máquina nova.

## BUG 4, 5 e 6 — `/api/userAchievements` quebrado em 3 dos 4 handlers

**Severidade: alta.**

A tabela nascia com PK composta `(user_id, achievement_id)` e a entidade usava `@EmbeddedId`, mas o
`UserAchievementResource` expõe `/api/userAchievements/{id}` com um `Long` e o repositório declara
`JpaRepository<..., Long>`:

| Endpoint | Erro |
|---|---|
| `POST /api/userAchievements` | `Identifier of entity '…UserAchievement' must be manually assigned before calling 'persist()'` |
| `GET /api/userAchievements/{id}` | `Supplied id had wrong type: … has id type '…UserAchievementId' but supplied id was of type 'java.lang.Long'` |
| `PUT /api/userAchievements/{id}` | idem |

E o quarto, a listagem, devolvia `getId().getAchievementId()` no campo `id` do DTO — registros de
cidadãos diferentes chegavam ao cliente com o mesmo `id`. Na prática, conquistas só podiam ser
concedidas pelo motor de regras.

**Correção:** `V20260918124000` dá à tabela uma chave própria vinda de `primary_sequence`, alinhando
com todas as demais entidades do projeto. O par `(user_id, achievement_id)` continua único, agora
como constraint, preservando a garantia de que cada cidadão ganha cada conquista uma única vez; o
service checa antes de gravar, para que a violação vire 422 e não 500. `UserAchievementId` deixou de
existir.

**Regressão:** a pasta `08` faz o ciclo completo (POST → GET por id → PUT), confere que o `id` do
DTO é o mesmo usado no path, verifica que a listagem não tem `id` repetido e exige 422 na tentativa
de conceder a mesma conquista duas vezes.

## BUG 7 — `PUT /api/exams/{id}` criava um exame órfão

**Severidade: crítica — corrompia dados a cada chamada.**

```java
examRepository.findById(id).orElseThrow(NotFoundException::new); // o id só servia para o 404
Exam exam = examMapper.toEntity(examRecoverDTO);                 // entidade NOVA, sem id
examRepository.save(exam);                                       // INSERT, não UPDATE
```

Como o DTO enviado pelo cliente não traz `id`, o `save()` virava INSERT. E como o `ExamMapper` não
mapeia `user` nem `healthUnit` a partir de `ExamRecoverDTO`, a linha nova nascia com `user_id` e
`health_unit_id` nulos. Reproduzido: `PUT /api/exams/1` respondia 200, o exame 1 permanecia
idêntico, e a tabela `exam` ganhava uma linha órfã.

**Correção:** o método passa a copiar os campos editáveis sobre o exame já carregado, no mesmo padrão
dos demais services, e a resolver as associações a partir dos ids do DTO. Como `ExamRecoverDTO` não
tem nenhuma validação, uma associação só é trocada quando vem informada — assim um PUT parcial deixa
de apagar vínculos.

**Regressão:** três requests encadeados na pasta `09` — conta os exames, faz o PUT com um marcador
único, e confere que o exame do path mudou, que o total não aumentou e que nenhum exame ficou sem
cidadão ou unidade.

## BUG 8 — `POST /api/appUsers` aceitava e-mail e CPF duplicados

**Severidade: alta.**

`AppUserService.create` salvava direto, sem checagem, e as colunas não tinham índice único. Dois POST
com o mesmo `email` e `cpfHash` respondiam 201. A exceção `UserAlreadyExistsException` (422) existia
no projeto mas **nunca era lançada**.

Como o `cpf_hash` é a única identificação do cidadão, duplicatas fragmentam o histórico clínico entre
registros diferentes — o oposto do objetivo do produto, que é o acompanhamento longitudinal.

**Correção:** `V20260918123000` cria índices únicos em `cpf_hash` e `lower(email)`, e o service passa
a lançar `UserAlreadyExistsException` no create e no update (neste, excluindo o próprio cidadão da
checagem). A migration neutraliza duplicatas pré-existentes com um sufixo, sem apagar nenhuma linha,
para que exames e perfis continuem apontando para os mesmos ids.

## BUG 9 — `GET /api/users/{userId}/exams` não validava o cidadão

**Severidade: baixa.**

`UserExamService.history` consultava os exames sem checar se o cidadão existe, então
`GET /api/users/999999/exams` respondia 200 com `content: []`. Divergia de `/exam-goal`,
`/notifications` e dos endpoints do médico, que devolvem 404 no mesmo cenário, e deixava o cliente
sem como distinguir "cidadão inexistente" de "cidadão sem exames".

**Correção:** passa a usar o `UserServiceHelper`, já empregado pelos demais fluxos. Coberto também
por dois testes JUnit, que verificam inclusive que o repositório de exames não chega a ser
consultado.

## BUG 10 — `code` de conquista duplicado derrubava o `POST /api/exams`

**Severidade: alta.** Uma escrita de catálogo quebrava o fluxo principal do sistema.

`achievement.code` identifica a conquista — o enum `AchivementCode` tem 3 valores — mas não havia
constraint nem checagem. Com duas linhas do mesmo code, `AchievementRepository.findByCode()`
(retorno `Optional`) passava a levantar:

```
Query did not return a unique result: 2 results were returned
```

E como o motor de conquistas roda dentro do `POST /api/exams`, a **ingestão de exames** começava a
responder 500. Agravante: o projeto não expõe nenhum `DELETE`, então a linha duplicada não podia ser
removida sem acesso direto ao banco.

**Correção:** índice único em `achievement.code` na `V20260918123000` e checagem no service, com a
nova `DuplicatedAchievementException` (422, acompanhando a `DuplicatedExamResultException` que o
projeto já usa para o mesmo tipo de conflito). A migration remove duplicatas pré-existentes
preservando o registro original de cada code; ali o sufixo não serve, porque a coluna é
`@Enumerated(EnumType.STRING)` e um valor fora do enum recriaria o BUG 2.

> Efeito colateral esperado: como os 3 codes do enum já vêm semeados, `POST /api/achievements`
> passa a responder 422 sempre. O catálogo de conquistas é fechado por natureza — criar uma nova
> exige antes acrescentar o valor ao enum.

---

# Achados de configuração

## O Swagger não mostrava a API — corrigido

`springdoc.pathsToMatch=/` limitava o documento OpenAPI ao path `/`. O Swagger UI subia, mas exibia
**apenas** o `HomeResource#index`. Como os controllers e DTOs já trazem `@Tag`, `@Operation` e
`@Schema` escritos, era documentação pronta e não publicada.

Passou a casar também `/api/**`; o `/v3/api-docs` agora descreve as 36 rotas com as 59 operações.

## O build estava quebrado no `develop` — corrigido

`RiskAssessmentServiceTest` importava `RiskAssessmentClassifier` do pacote `rulesMotor` em vez de
`rulesMotor.exam`. Como `mvn package -DskipTests` ainda **compila** os testes, o
`docker compose up --build` falhava na etapa de build da imagem — o projeto não subia.

## A migration `V20260902100000` estava fora de ordem — corrigido

Ela entrou no repositório depois que a `V20260907120000` já havia sido aplicada. Com
`validate-on-migrate=true` e out-of-order desligado, o Flyway se recusava a subir em qualquer banco
nesse estado. Resolvido com `spring.flyway.out-of-order=true`.

---

# Em aberto: o README descreve uma API que não existe

Não mexido, porque é decisão de produto e não correção de defeito:

- A seção **🔐 Segurança** descreve Spring Security, JWT e os perfis `ROLE_CITIZEN` / `ROLE_LAB` /
  `ROLE_DOCTOR`. **Nada disso existe no código**: não há `spring-boot-starter-security` no
  `pom.xml`, nenhum `SecurityFilterChain`, nenhum `@PreAuthorize`. Os 59 endpoints são abertos e
  nenhum deles pode responder 401 ou 403.
- A frase "o isolamento dos dados do cidadão é realizado a partir do usuário identificado no token"
  não se sustenta: todo endpoint por cidadão recebe o `userId` pela URL. Qualquer chamador lê os
  exames, o risco e as notificações de qualquer pessoa — inclusive a timeline clínica completa em
  `/api/v1/doctor/patients/{userId}/timeline`. A única barreira existente é o 404 de
  `/api/users/{userId}/exams/{examId}` quando o exame é de outro cidadão, e a collection cobre isso.
- A seção **📡 Principais endpoints** lista rotas que não existem: `/api/v1/auth/login`,
  `/api/v1/auth/token`, toda a família `/api/v1/users/me/*` e `/api/v1/labs/*`.

A collection reflete o código, não o README.

---

# Cobertura

Os 59 handlers dos 18 controllers são exercitados pelo menos uma vez.

| Pasta | Requests | O que cobre |
|---|---|---|
| `00 - Smoke` | 1 | Aplicação no ar |
| `01 - Catalogo` | 13 | Catálogo público, unidades, itens; resolve os ids usados adiante |
| `02 - Fixtures` | 12 | Cidadãos e perfis de saúde dos fluxos |
| `03 - Ingestao de exame e motor de risco` | 17 | `POST /api/exams` e seus 11 caminhos de erro |
| `04 - Cidadao` | 9 | Meta, histórico, detalhe e isolamento entre cidadãos |
| `05 - Notificacoes` | 13 | Histórico, varredura da meta, CRUD de avisos |
| `06 - Medico` | 12 | Timeline de marcador e comparação de exames |
| `07 - Risco` | 9 | Avaliações e evolução do risco |
| `08 - Gamificacao` | 14 | Conquistas e conquistas por cidadão |
| `09 - CRUD administrativo` | 39 | Os handlers restantes e as regressões dos bugs 1, 7 e 8 |
| `99 - SSE (manual)` | 1 | Fora da execução automatizada |

A pasta `99` fica de fora do runner porque o `SseEmitter` mantém a conexão aberta por 30 minutos
(`sanguebom.notifications.sse.timeout-ms=1800000`) e travaria o Newman.
