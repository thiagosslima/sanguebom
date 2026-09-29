# Atualizar Usuário

Atualiza os dados de um usuário existente.

---

## Requisição

### Estrutura da requisição

| Parâmetro | Tipo | Obrigatório | Descrição                |
|-----------|------|-------------|--------------------------|
| `id`      | Long | Sim         | Identificador do usuário |

### Corpo da requisição

```json
{
  "name": "QA Cidadao Principal (atualizado)",
  "birthDate": "1990-05-15",
  "email": "example-guid@sanguebom.test",
  "cpfHash": "hash_example-guid",
  "status": "ACTIVE"
}
```

### Exemplo de requisição

```http
PUT /api/appUsers/1
Content-Type: application/json

{
  "name": "QA Cidadao Principal (atualizado)",
  "birthDate": "1990-05-15",
  "email": "example-guid@sanguebom.test",
  "cpfHash": "hash_example-guid",
  "status": "ACTIVE"
}
```

---

## Retorno

Em caso de sucesso, a API retorna os dados solicitados.

Retorna o identificador do registro criado ou atualizado.

### Estrutura do retorno

| Campo | Tipo | Descrição                             |
|-------|------|---------------------------------------|
| `id`  | Long | Identificador do registro atualizado. |

### Exemplo de retorno

```json
1
```

## Observações

* Os campos identificadores gerados pela aplicação não devem ser informados quando não fizerem parte do corpo da
  requisição.
* Importante: os valores apresentados no exemplo são meramente ilustrativos.