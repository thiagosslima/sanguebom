# CI/CD — Sangue Bom

Quatro suítes no GitHub Actions. Cada uma é um *reusable workflow*: roda
sozinha pela aba **Actions → (workflow) → Run workflow**, ou em conjunto
pelo orquestrador `ci.yml`.

| Workflow | Arquivo | O que faz | Bloqueia? |
|---|---|---|---|
| **CI** | `ci.yml` | Orquestra as demais | — |
| **Tests** | `tests.yml` | `mvn verify` com Postgres real | ✅ sim |
| **Coverage** | `coverage.yml` | 80% nas linhas novas do PR | ✅ sim |
| **Static Analysis** | `static-analysis.yml` | CodeQL + SpotBugs | ❌ não (só alerta) |

## Quando cada uma dispara

- **Pull request** para `develop` ou `main` → Tests + Coverage + Static Analysis
- **Push** em `develop` → Tests + Static Analysis
- **Segunda-feira 06:00 UTC** → CodeQL (pega CVEs novas em código parado)
- **Manual** → `CI` com checkboxes por suíte, ou cada workflow isolado

> Para rodar **só** a cobertura, use o dispatch do workflow **Coverage**
> (ele roda o `mvn verify` por conta própria). No `ci.yml` a cobertura
> depende do job de testes, para não rodar a suíte duas vezes.

## Testes

`SanguebomApplicationTests` é um `@SpringBootTest` de verdade: sobe o contexto
e aplica as 7 migrations do Flyway. Por isso o job usa um *service container*
`postgres:18.4` em vez de excluir o teste.

Duas variáveis são obrigatórias no job:

- `DB_USERNAME` / `DB_PASSWORD` — não têm default no `application.properties`
- `SPRING_DOCKER_COMPOSE_ENABLED=false` — o `spring-boot-docker-compose` está
  no classpath e o `docker-compose.yml` fica na raiz; sem isso o teste tentaria
  subir o compose dentro do runner

Para reproduzir localmente:

```bash
docker compose up -d postgres
DB_USERNAME=postgres DB_PASSWORD=postgres SPRING_DOCKER_COMPOSE_ENABLED=false mvn -B verify
```

## Cobertura

O gate exige **80% de cobertura nas linhas adicionadas/alteradas pelo PR** —
não no projeto inteiro. A cobertura global hoje é ~57%, então um gate global
reprovaria todos os PRs sem relação com o que foi escrito neles.

A medição é feita pelo [`diff-cover`](https://github.com/Bachmann1234/diff_cover)
sobre o `target/site/jacoco/jacoco.xml`. Ele mede **linha a linha** dentro do
diff; ações do tipo "changed files" mediriam o arquivo alterado inteiro, o que
é outra coisa.

O PR recebe um comentário fixo com as duas medidas lado a lado — a do PR
(bloqueante) e a do projeto (informativa) — mais a lista de arquivos com linhas
novas descobertas e o link para o code scanning. O workflow de análise estática
publica um segundo comentário com os achados do SpotBugs por regra e por
arquivo.

Os comentários são *sticky*: `.github/scripts/sticky_comment.sh` reencontra o
comentário anterior por um marcador HTML e edita no lugar, em vez de empilhar um
novo a cada push. Usa o `gh` CLI com o `GITHUB_TOKEN` do próprio run, então
nenhuma action de terceiro recebe permissão de escrita no PR.

Para mudar o limiar sem editar o workflow, use o input `min-coverage` no
dispatch do workflow Coverage.

## Análise estática

**CodeQL** (`security-and-quality`) e **SpotBugs** publicam em
*Security → Code scanning*. Nenhum dos dois bloqueia o merge nesta primeira
versão: são 150 arquivos que nunca passaram por análise estática, e reprovar
de cara pararia todos os PRs.

Para endurecer depois de zerar o backlog:

1. No `pom.xml`, trocar `<failOnError>false</failOnError>` por `true` no
   `spotbugs-maven-plugin`.
2. Adicionar os checks às *branch protection rules* de `develop` e `main`.

O arquivo `spotbugs-exclude.xml` silencia `EI_EXPOSE_REP`/`EI_EXPOSE_REP2`
**apenas** no pacote `model`: em entidades JPA e DTOs do Lombok, devolver a
referência de um `List`/`Date` é o comportamento esperado. Nos demais pacotes
(`controller`, `service`, `config`, `rulesMotor`) os dois padrões continuam
sendo reportados. Isso reduz o ruído de 34 para 12 achados.

O SpotBugs emite caminhos relativos ao *source root*; o workflow prefixa
`src/main/java/` no SARIF para o GitHub conseguir ligar o alerta à linha.

## Deploy

**Não há workflow de deploy.** Um ambiente de DEV chegou a ser provisionado
(Neon + Render) e foi removido depois: o free tier do Render (0.1 CPU, 512 MB)
não conseguia subir a aplicação dentro da janela em que o Render procura a
porta aberta — a inicialização do contexto Spring sozinha levava mais de
60 segundos.

Se um ambiente hospedado voltar à pauta, os pontos que custaram tempo foram:

- A aplicação precisa ler a porta do host: `server.port=${PORT:8080}`.
- Provedores gerenciados de Postgres exigem TLS; a URL JDBC precisa aceitar
  parâmetros extras (`?sslmode=require`).
- O tempo de startup é o fator decisivo na escolha do plano. Vale medir antes
  de escolher o provedor.

O `Dockerfile` continua funcional e é usado pelo `docker-compose.yml`.

## Custos

Zero. O repositório é público, então os minutos de Actions e o code scanning
são ilimitados.
