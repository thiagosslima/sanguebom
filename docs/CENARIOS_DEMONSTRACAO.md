# Cidadãos e cenários de demonstração

A migration `V20260919230000__CENARIOS_DEMONSTRACAO_CIDADAOS.sql` adiciona **34 cidadãos fictícios**, sem modificar os registros anteriores, as faixas de referência ou as regras clínicas. Todos têm `(Demo)` no nome e e-mail no domínio reservado `demo.sanguebom.example`. Os hashes usam identificadores sintéticos, sem CPFs de pessoas reais.

A carga cobre os estados de domínio e situações de interface disponíveis hoje. Não pretende representar todas as combinações clínicas possíveis. Valores clínicos e pontuações seguem exclusivamente as regras já cadastradas no projeto; os exemplos não constituem diagnóstico.

## Como usar

```bash
docker compose up --build -d --wait
```

O Flyway aplica a nova migration uma única vez. Não é necessário limpar o banco. Abra http://localhost:3000 e selecione um cidadão com `(Demo)` no nome. Para localizar o ID pela API, use o prefixo de e-mail da tabela abaixo.

As datas são relativas à **execução da migration**, não ao reinício do container. Metas de prazo mudam naturalmente conforme os dias passam. Reaplicar ou editar uma migration já aplicada não é uma forma de renovar esses cenários.

## Matriz de cobertura

| Prefixo do e-mail | Cidadão | Cenário na data da carga |
| --- | --- | --- |
| sem_historico | Alice Ferreira | Sem exames, conquistas ou notificações; meta `NO_HISTORY` |
| baixo | Bruno Almeida | Painel completo com 11 marcadores normais, risco `LOW`, duas conquistas |
| moderado | Camila Santos | Glicose abaixo da faixa; resultado `LOW`, pontuação 4, risco `MODERATE` |
| alto | Diego Oliveira | Resultado `ATTENTION`, pontuação 6, risco `HIGH` |
| muito_alto | Elisa Costa | Resultados `HIGH` e `VERY_HIGH`, pontuação 10, risco `VERY_HIGH` |
| vence_15 | Fabio Rodrigues | Meta vence em 15 dias, aviso `DUE_SOON` |
| vence_hoje | Gabriela Lima | Meta vence hoje, ainda `DUE_SOON` |
| vence_30 | Henrique Barbosa | Limite inclusivo de 30 dias para `DUE_SOON` |
| vence_31 | Isabela Rocha | Um dia além do limite: `UP_TO_DATE` |
| venceu_ontem | Joao Pereira | Meta venceu há um dia, `OVERDUE` |
| atrasado | Karina Martins | Meta venceu há 60 dias, alerta pendente |
| coletado | Lucas Ribeiro | Exame `COLLECTED`, sem resultados, liberação ou avaliação |
| em_analise | Mariana Araujo | Exame e resultado `IN_ANALYSIS`, sem avaliação |
| melhora | Nicolas Fernandes | Três coletas: risco muito alto → alto → baixo; três conquistas |
| piora | Olivia Gomes | Três coletas: risco baixo → alto → muito alto; conquistas anteriores mantidas |
| estavel | Pedro Carvalho | Três painéis iguais; comparação com variação zero |
| historico_extenso | Rafaela Nascimento | 25 exames e avisos, paginação do histórico/notificações/timeline, valores oscilantes |
| itens_diferentes | Samuel Teixeira | Itens presentes somente em uma das coletas: comparação “Não medido” |
| valor_zero | Tatiana Moreira | Glicose 0 → 70; variação absoluta válida e percentual nulo quando a base é zero |
| limites_marcadores | Vinicius Cardoso | 12 exames nas fronteiras inclusivas/exclusivas da glicose e dos triglicerídeos |
| score_zero | Yasmin Freitas | HDL em faixa protetora; pontuação zero, risco `LOW` |
| score_tres | Andre Batista | Média exata 3: risco `MODERATE` e conquista Campeão da Saúde (`score <= 3`) |
| score_oito | Beatriz Duarte | Resultado `HIGH`, pontuação exata 8, risco agregado `VERY_HIGH` |
| sem_perfil | Caio Mendes | Cadastro incompleto; meta retorna 404 e análise exige perfil; formulário permite completar |
| sem_sexo | Daniela Pinto | Perfil sem sexo; precisa completar antes de analisar exame |
| sem_nascimento | Eduardo Vieira | Cadastro sem nascimento; precisa completar antes de analisar exame |
| menor_faixa | Fernanda Campos | 18 anos, sem histórico; GLUCOSE não tem faixa aplicável (catálogo atual: 21–59 anos) |
| maior_faixa | Gustavo Lopes | 70 anos, sem histórico; mesma ausência de faixa para GLUCOSE |
| inativo | Helena Machado | Cadastro `INACTIVE`, meta vencida; rotina de avisos de meta ignora o cidadão |
| resultado_textual | Igor Azevedo | Dois exames em análise: um resultado textual, outro numérico; comparação e timeline sem avaliação |
| sem_avaliacao | Julia Monteiro | Exame liberado com resultados, mas sem avaliação de risco importada |
| legado | Leandro Correia | `COMPLETED`, resultados `ALERTA`/`NORMAL`, riscos `ALERTA`/`NORMAL` e avisos `READ`/`UNREAD` |
| otimo_importado | Luana Dias | Resultado importado `OPTIMAL`, sem risco calculado localmente |
| pontual_alterado | Marcelo Reis | Dois exames pontuais alterados; Sangue Bom + Pontual, sem Campeão da Saúde |

## Consistência com a API

- Sexo `M`/`F`, três periodicidades, fatores de risco JSON e múltiplas unidades existentes.
- Pontuações são médias arredondadas a duas casas; risco usa os limites 3, 6 e 8 do motor atual.
- Nenhuma avaliação é criada para exames coletados, em análise ou com resultado textual.
- Conquistas respeitam exames `RELEASED`, pontuação <= 3 e intervalo menor que a periodicidade + 30 dias. Uma conquista adquirida não desaparece se a saúde piorar depois.
- Avisos de resultado usam `EXAM:<id>`; avisos de meta usam a data de vencimento, como a rotina diária. Abrir o SSE pode mudar `PENDING` para `SENT`.
- `OPTIMAL`, `ALERTA`, `COMPLETED`, `READ` e `UNREAD` ficam isolados como **dados importados/legados**. Não se afirma que sejam produzidos pelo motor atual. O HDL em faixa protetora, por exemplo, recebe `NORMAL` no motor atual.
- Faixas históricas são consultadas conforme sua vigência real. Coletas anteriores a `valid_from` podem ter resultado e risco sem snapshot de faixa na timeline; a carga não retroage a validade das regras existentes.
- Os limites de prazo são exatos mesmo no fim de mês: se necessário, a carga escolhe outra das três periodicidades para preservar a data pretendida.
- IDs são obtidos da `primary_sequence`. Se houver conflito de e-mail reservado ou regra ambígua/ausente, a migration aborta em vez de sobrescrever registros existentes.

Erros de requisição (ID inexistente, exame de outro cidadão, datas invertidas, CPF/e-mail duplicado, valores negativos ou itens repetidos) devem ser exercitados pelos testes da API. A carga não grava registros inválidos para simular essas rejeições.

## Validar a carga

Com a API ativa, execute:

```bash
python3 scripts/validate-demo-seed.py
# Outra instância:
python3 scripts/validate-demo-seed.py --url http://localhost:18081
```

O validador é somente leitura e não abre SSE. Ele verifica cobertura, pontuações, estados de meta, paginação, comparações, dados importados e as combinações de conquistas. As asserções de prazo consideram a data da carga registrada no `updatedAt` dos cidadãos. Execute antes de editar os registros de demonstração: mudanças manuais podem alterar os resultados esperados.
