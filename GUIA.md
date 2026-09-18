# Guia de uso

Como subir o Sangue Bom e como rodar os testes de API. Tudo o que está aqui é feito por dois
scripts, e nenhum deles pede instalação além de Docker e Node.

- [O que você precisa ter](#o-que-você-precisa-ter)
- [Subir o ambiente](#subir-o-ambiente)
- [Rodar os testes de API](#rodar-os-testes-de-api)
- [Usar a collection no Postman](#usar-a-collection-no-postman)
- [Como a collection está organizada](#como-a-collection-está-organizada)
- [Testar o stream de notificações](#testar-o-stream-de-notificações)
- [Quando algo dá errado](#quando-algo-dá-errado)
- [Onde fica cada coisa](#onde-fica-cada-coisa)

---

## O que você precisa ter

| Ferramenta | Para quê | Obrigatório |
|---|---|---|
| Docker + Docker Compose | Subir o banco e a aplicação | Sim |
| Node.js | Rodar o Newman via `npx`, sem instalar nada | Só para os testes de API |
| Java 21 + Maven | Rodar `mvn test` fora do container | Não |

Um arquivo `.env` na raiz é necessário, mas você não precisa criá-lo: o `start.sh` copia do
`.env.example` se ele não existir.

---

## Subir o ambiente

```bash
./scripts/start.sh
```

Um menu aparece:

```text
  1) Subir com banco LIMPO  (apaga tudo e reaplica as migrations)
  2) Subir normalmente      (mantem os dados existentes)
  3) Parar o ambiente
  4) Sair
```

**Opção 1 — banco limpo.** Remove o volume do PostgreSQL e sobe do zero, deixando o Flyway
reconstruir o schema e todos os seeds. Pede confirmação: você digita `limpar`. Use quando quiser um
estado previsível, ou quando o banco estiver esquisito.

**Opção 2 — manter os dados.** Só sobe os containers. Migrations pendentes são aplicadas
normalmente, e nada do que já existe é apagado.

**Opção 3 — parar.** Derruba os containers. Os dados ficam preservados no volume `postgres-data`,
então a próxima opção 2 encontra tudo no lugar.

Quando termina, o script mostra:

```text
==> Ambiente pronto.
    Aplicacao : http://localhost:8080
    Banco     : localhost:5432
    Logs      : docker compose logs -f sanguebom
    Testes    : ./scripts/api-test.sh
```

### Sem menu (CI, scripts, atalhos)

```bash
./scripts/start.sh --clean --yes   # banco limpo, sem confirmação
./scripts/start.sh --keep          # sobe mantendo os dados
./scripts/start.sh --down          # para o ambiente
./scripts/start.sh --help
```

### O que o script faz por você

- Confere se o Docker está instalado e se o daemon responde.
- Cria um `.env` padrão se não existir, e avisa que criou.
- Espera o PostgreSQL ficar saudável antes de seguir.
- Espera a aplicação responder em `http://localhost:8080` — a primeira subida compila o projeto
  dentro da imagem, então pode demorar alguns minutos.
- Se a aplicação não subir, imprime os últimos 50 logs do container e sai com erro, em vez de
  dizer que deu tudo certo.

---

## Rodar os testes de API

```bash
./scripts/api-test.sh
```

Por padrão o script **recria o ambiente do zero** e só então executa a collection. Isso mantém a
execução reproduzível — o projeto não expõe nenhum endpoint `DELETE`, então a collection não tem
como limpar o que cria.

Para rodar contra o ambiente que já está de pé, sem recriar nada:

```bash
./scripts/api-test.sh --keep
```

A collection é idempotente: cada execução cria seus próprios cidadãos, com e-mail e CPF sorteados,
então rodar várias vezes seguidas com `--keep` funciona — só acumula dados de teste.

### O que esperar

```text
┌─────────────────────────┬───────────────────┬──────────────────┐
│                         │          executed │           failed │
├─────────────────────────┼───────────────────┼──────────────────┤
│                requests │               139 │                0 │
├─────────────────────────┼───────────────────┼──────────────────┤
│              assertions │               204 │                0 │
└─────────────────────────┴───────────────────┴──────────────────┘
```

**Tudo verde é o resultado esperado.** Qualquer falha é regressão. O relatório em JSON fica em
`target/newman/report.json`, útil para inspecionar uma falha específica:

```bash
python3 -c "
import json; r = json.load(open('target/newman/report.json'))
for f in r['run']['failures']:
    print(f['source']['name'], '::', f['error']['message'])"
```

---

## Usar a collection no Postman

Importe os dois arquivos:

- `postman/sanguebom.postman_collection.json`
- `postman/sanguebom.local.postman_environment.json`

Selecione o environment **Sangue Bom - local** no canto superior direito. Ele só define `baseUrl`
como `http://localhost:8080`; se a sua API estiver em outro endereço, mude ali.

**Não existe login.** O projeto não tem Spring Security nem JWT — todos os endpoints são abertos.
Não há nada para configurar em Authorization.

### Rodando dentro do Postman

Use o **Collection Runner** e execute as pastas **na ordem**, da `00` à `09`. A ordem importa: a
pasta `02` cria os cidadãos que as pastas seguintes usam, e a `03` cria os exames que alimentam as
consultas do cidadão, do médico e do risco.

Se quiser disparar um request isolado, rode antes as pastas `01`, `02` e `03` uma vez — elas
preenchem as variáveis (`userId`, `examId`, `glucoseItemId` e companhia) que os demais consomem.

---

## Como a collection está organizada

| Pasta | Requests | O que cobre |
|---|---|---|
| `00 - Smoke` | 1 | A aplicação respondeu |
| `01 - Catalogo` | 13 | Catálogo público, unidades e itens de exame; resolve os ids usados adiante |
| `02 - Fixtures` | 12 | Cria os cidadãos e perfis de saúde que os fluxos consomem |
| `03 - Ingestao de exame e motor de risco` | 17 | `POST /api/exams` e seus 11 caminhos de erro |
| `04 - Cidadao` | 9 | Meta de exames, histórico, detalhe e isolamento entre cidadãos |
| `05 - Notificacoes` | 13 | Histórico, varredura da meta e CRUD de avisos |
| `06 - Medico` | 12 | Evolução de um marcador no tempo e comparação entre exames |
| `07 - Risco` | 9 | Avaliações e evolução do risco |
| `08 - Gamificacao` | 14 | Conquistas e conquistas por cidadão |
| `09 - CRUD administrativo` | 39 | Os handlers restantes, mais as regressões dos bugs já corrigidos |
| `99 - SSE (manual)` | 1 | Fora da execução automatizada |

Os 59 endpoints dos 18 controllers são exercitados pelo menos uma vez, em caso de sucesso e de
falha. Todo teste de erro confere o corpo RFC-7807 que o `GlobalExceptionHandler` devolve — `title`,
`detail` e, nos 400 de validação, a lista em `parâmetros inválidos` — e não apenas o status.

Vários requests trazem na descrição o arquivo e a linha do comportamento que estão cobrindo. Vale a
leitura quando algum teste falhar.

---

## Testar o stream de notificações

A pasta `99 - SSE (manual)` fica fora da execução automatizada de propósito: o emissor mantém a
conexão aberta por 30 minutos (`sanguebom.notifications.sse.timeout-ms`) e travaria o Newman.

Para ver funcionando:

1. Rode a pasta `02 - Fixtures` para ter um cidadão, ou use um `userId` que já exista.
2. Abra `GET /api/users/{userId}/notifications/stream` e deixe rodando. Você recebe o evento
   `connected` na abertura e um `ping` a cada 25 segundos.
3. Em outra aba, dispare um `POST /api/exams` para o mesmo cidadão.
4. O evento `notification` chega no stream, com a notificação `EXAM_RESULT_AVAILABLE`.

Pelo terminal dá para fazer o mesmo:

```bash
curl -N http://localhost:8080/api/users/1/notifications/stream
```

---

## Quando algo dá errado

**A aplicação não sobe.**
```bash
docker compose logs -f sanguebom
```

**O Flyway reclama de migration.** Suba com o banco limpo:
```bash
./scripts/start.sh --clean
```

**Quero ver o que está no banco.**
```bash
docker compose exec postgres psql -U postgres -d sanguebom
```

**Quero conferir quais migrations foram aplicadas.**
```bash
docker compose exec postgres psql -U postgres -d sanguebom \
  -c "select version, description, success from flyway_schema_history order by installed_rank;"
```

**A porta 8080 ou 5432 já está em uso.** A do banco é configurável pelo `DB_PORT` no `.env`. A da
aplicação está fixa no `docker-compose.yml`.

**Os testes de API falharam.** Rode com o ambiente recriado para descartar sujeira de execuções
anteriores:
```bash
./scripts/api-test.sh
```
Se continuar falhando, é regressão. O nome do request na saída do Newman diz qual comportamento
quebrou, e a descrição dele no Postman aponta o arquivo e a linha.

---

## Onde fica cada coisa

| Caminho | O que é |
|---|---|
| `scripts/start.sh` | Sobe, para e recria o ambiente |
| `scripts/api-test.sh` | Executa a collection com o Newman |
| `postman/sanguebom.postman_collection.json` | A collection |
| `postman/sanguebom.local.postman_environment.json` | Environment com `baseUrl` |
| `postman/RELATORIO.md` | Os 10 bugs que a collection encontrou, com causa e correção |
| `target/newman/report.json` | Relatório da última execução |
| `.env` | Credenciais do banco, fora do versionamento |

### Outros comandos úteis

```bash
mvn test                                   # suíte JUnit: 135 testes
docker compose logs -f sanguebom           # logs da aplicação
docker compose ps                          # estado dos containers
curl http://localhost:8080/v3/api-docs     # documento OpenAPI
```

O Swagger UI fica em `http://localhost:8080/swagger-ui/index.html`.
