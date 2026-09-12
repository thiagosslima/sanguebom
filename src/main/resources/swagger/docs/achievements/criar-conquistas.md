# Criar conquista

Cria uma nova conquista no sistema.

---

## Requisição

### Estrutura da requisição

| Campo         | Tipo    | Obrigatório | Tamanho máximo | Descrição                        |
| ------------- | ------- | ----------- | -------------: | -------------------------------- |
| `code`        | String  | Não         |             50 | Código da conquista              |
| `name`        | String  | Não         |            100 | Nome da conquista                |
| `description` | String  | Não         |            500 | Descrição da conquista           |
| `active`      | Boolean | Não         |                | Indica se a conquista está ativa |

### Exemplo de requisição

```http
POST /api/achievements
Content-Type: application/json

{
  "code": "CONQ001",
  "name": "Primeira Conquista",
  "description": "Descrição da primeira conquista.",
  "active": true
}
```

---

## Retorno

Retorna o identificador da conquista criada.

### Estrutura do retorno

| Campo     | Tipo | Descrição                           |
|-----------|------|-------------------------------------|
| `id`      | Long | Identificador da conquista criada.  |

### Exemplo de retorno

```json
1
```

## Observações

* Os campos `id` e `createdAt` são gerenciados pela aplicação e não devem ser informados na criação.
* Importante: os valores apresentados no exemplo são meramente ilustrativos.