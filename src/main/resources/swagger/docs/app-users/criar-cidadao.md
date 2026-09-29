# Criar cidadão

Cria um novo cidadão no sistema.

---

## Requisição

### Estrutura da requisição

| Campo       | Tipo      | Obrigatório | Descrição           |
|-------------|-----------|-------------|---------------------|
| `name`      | String    | Não         | Nome do cidadão.    |
| `birthDate` | LocalDate | Não         | Data de nascimento. |
| `email`     | String    | Não         | E-mail do cidadão.  |
| `cpfHash`   | String    | Não         | Hash do CPF.        |
| `status`    | String    | Não         | Status do cadastro. |

### Exemplo de requisição

```http
POST /api/appUsers
Content-Type: application/json

{
  "name": "QA Cidadao Principal",
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

| Campo | Tipo | Descrição                          |
|-------|------|------------------------------------|
| `id`  | Long | Identificador do registro criado. |

### Exemplo de retorno

```json
1
```

## Observações

* Os campos identificadores gerados pela aplicação não devem ser informados quando não fizerem parte do corpo da requisição.
* Importante: os valores apresentados no exemplo são meramente ilustrativos.

---