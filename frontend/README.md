# SangueBom Web

Interface Next.js + React + TypeScript, responsiva e em português. Integra dados reais da API por um proxy no servidor, incluindo SSE para notificações. Não há autenticação na API atual: o seletor de cidadão é uma ferramenta local de navegação, não um login.

## Usar tudo junto

Na raiz do repositório: `./scripts/start.sh`. Acesse http://localhost:3000. Docker Compose compila front-end e back-end e inicia o PostgreSQL com dados de exemplo.

## Desenvolver

Com a API em execução em http://localhost:8080, execute neste diretório:

```sh
npm ci
npm run dev
```

Para outra API, copie `.env.example` para `.env.local` e ajuste `BACKEND_URL`. Essa variável é usada somente no servidor Next.js, inclusive no Docker, sem exigir recompilação.

## Verificar

```sh
npm run typecheck
npm run build
npx playwright install chromium
npm run test:e2e
```

Os testes de navegador esperam a aplicação em http://localhost:3000 e uma API com os cidadãos e exames de exemplo. Use `E2E_BASE_URL` para outra porta. Os testes padrão só consultam dados; a conexão SSE pode marcar notificações pendentes como entregues, conforme comportamento da API.

O teste de gravação fica desabilitado por padrão. Ative `E2E_WRITE=1` **apenas em um banco separado para testes**, pois ele cadastra um cidadão sintético e um exame. Ele verifica também edição do perfil, persistência de fatores de risco e análise do exame.

## Funcionalidades

- Visão geral com contagens, últimos exames e meta periódica.
- Histórico paginado, busca na página, detalhe e impressão do resultado.
- Cadastro de cidadão e perfil, edição, validação de CPF e hash SHA-256 no navegador.
- Registro de exames com múltiplos marcadores do catálogo e análise pela API.
- Gráfico e tabela de evolução, filtros de período e comparação de até quatro exames.
- Conquistas, histórico de notificações paginado e eventos em tempo real.
- Estados de carregamento, erros com nova tentativa, histórico vazio e navegação no celular.

A fonte web tem fallback local; a aplicação continua funcional sem acesso ao Google Fonts. O banco persiste no volume do Docker. Dados sensíveis não são guardados em localStorage: são lembrados apenas o ID do cidadão selecionado e, por cidadão, o ID/data do último aviso visualizado.

## Endereços das telas

A navegação usa o App Router do Next.js. O endereço identifica o cidadão e, no resultado, o exame. Os links funcionam ao abrir diretamente, recarregar, abrir em outra aba e navegar com Voltar/Avançar.

| Tela | Endereço |
| --- | --- |
| Visão geral | `/cidadaos/1` |
| Histórico | `/cidadaos/1/exames` |
| Resultado | `/cidadaos/1/exames/10084` |
| Novo exame | `/cidadaos/1/exames/novo` |
| Evolução | `/cidadaos/1/evolucao` |
| Conquistas | `/cidadaos/1/conquistas` |
| Notificações | `/cidadaos/1/notificacoes` |
| Perfil | `/cidadaos/1/perfil` |
| Cadastro de cidadão | `/cidadaos/novo` |

Os números são exemplos: use os IDs dos registros disponíveis. O endereço `/` encaminha para o cidadão lembrado neste navegador ou para o primeiro disponível. Um cidadão informado explicitamente na URL sempre tem prioridade sobre a preferência local; endereços inválidos não são substituídos silenciosamente por outro cidadão.

Os filtros temporários das telas e formulários ainda não enviados não são persistidos ao recarregar. Compartilhar a URL não cria autorização de acesso: continuam valendo as limitações de autenticação da API descritas acima.

O ponto vermelho do sininho indica avisos novos desde a última abertura da lista neste navegador. Abrir pelo sininho, pelo menu ou diretamente pela URL registra a visualização. Esse marcador é local e não altera os estados de entrega `PENDING`/`SENT` da API; não sincroniza a leitura entre dispositivos.
