# Consultar conquista

Retorna os dados de uma conquista específica a partir do seu identificador.

---

## Requisição

### Estrutura da requisição

| Parâmetro | Tipo | Obrigatório | Descrição                  |
| --------- | ---- | ----------- | -------------------------- |
| `id`      | Long | Sim         | Identificador da conquista |

### Exemplo de requisição

```http
GET /api/achievements/{id}
```

## Retorno

Em caso de sucesso, a API retorna a busca da conquista com o identificador informado.

### Estrutura do retorno

| Campo         | Tipo           | Descrição                           |
| ------------- | -------------- | ----------------------------------- |
| `id`          | Long           | Identificador da conquista          |
| `code`        | String         | Código da conquista                 |
| `name`        | String         | Nome da conquista                   |
| `description` | String         | Descrição da conquista              |
| `active`      | Boolean        | Indica se a conquista está ativa    |
| `createdAt`   | OffsetDateTime | Data e hora de criação da conquista |

### Exemplo de retorno

```json
{
  "id": 1,
  "code": "CONQ001",
  "name": "Primeira Conquista",
  "description": "Descrição da primeira conquista.",
  "active": true,
  "createdAt": "2023-01-01T00:00:00"
}
```