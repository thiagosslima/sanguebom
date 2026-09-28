# Listar cidadãos

Retorna os registros cadastrados no sistema.

---

## Requisição

### Exemplo de requisição

```http
GET /api/appUsers
```

---

## Retorno

Em caso de sucesso, a API retorna os dados solicitados.

### Estrutura do retorno

| Campo | Tipo | Descrição |
| ----- | ---- | --------- |
| `id` | Long | Identificador do cidadão. |
| `name` | String | Nome do cidadão. |
| `birthDate` | LocalDate | Data de nascimento. |
| `email` | String | E-mail do cidadão. |
| `cpfHash` | String | Hash do CPF. |
| `status` | String | Status do cadastro. |

### Exemplo de retorno

```json
[
  {
    "id": 1,
    "name": "EXEMPLO",
    "birthDate": "2026-01-01",
    "email": "EXEMPLO",
    "cpfHash": "EXEMPLO",
    "status": "EXEMPLO"
  }
]
```