# Listar conquistas

Retorna todas as conquistas cadastradas no sistema.

---

## Requisição

### Exemplo de requisição

```http
GET /api/achievements
```

---

## Retorno

Em caso de sucesso, a API retorna a lista de conquistas cadastradas no sistema.

### Estrutura do retorno


| Campo         | Tipo             | Descrição                                    |
|---------------|------------------| -------------------------------------------- |
| `id`          | `Long`           | Identificador da conquista.                  |
| `code`        | `String`         | Código da conquista.                         |
| `name`        | `String`         | Nome da conquista.                           |
| `description` | `String`         | Descrição da conquista.                      |
| `active`      | `Boolean`        | Indica se a conquista está ativa.            |
| `createdAt`   | `OffsetDateTime` | Data e hora da criação do registro.          |

### Exemplo de retorno

```json
[
  {
    "id": 1,
    "code": "CONQ001",
    "name": "Primeira Conquista",
    "description": "Descrição da primeira conquista.",
    "active": true,
    "createdAt": "2023-01-01T00:00:00"
  },
  {
    "id": 2,
    "code": "CONQ002",
    "name": "Segunda Conquista",
    "description": "Descrição da segunda conquista.",
    "active": true,
    "createdAt": "2023-01-02T00:00:00"
  }
]
```