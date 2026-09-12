# Atualizar conquista

Atualiza os dados de uma conquista existente.

---

## Requisição

### Estrutura da requisição

| Parâmetro | Tipo | Obrigatório | Descrição                  |
| --------- | ---- | ----------- | -------------------------- |
| `id`      | Long | Sim         | Identificador da conquista |

### Corpo da requisição

```json
{
  "code": "CONQ001",
  "name": "Primeira Conquista Atualizada",
  "description": "Descrição da primeira conquista atualizada.",
  "active": true
}
```

### Exemplo de requisição

```http
PUT /api/achievements/1
Content-Type: application/json

{
  "code": "CONQ001",
  "name": "Primeira Conquista Atualizada",
  "description": "Descrição da primeira conquista atualizada.",
  "active": true
}
```

---

## Retorno

Retorna o identificador da conquista atualizada.

### Estrutura do retorno

| Campo     | Tipo | Descrição                              |
|-----------|------|----------------------------------------|
| `id`      | Long | Identificador da conquista atualizada. |

### Exemplo de retorno

```json
1
```

## Observações

* Importante: os valores apresentados no exemplo são meramente ilustrativos.