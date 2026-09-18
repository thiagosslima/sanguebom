# 🩸 Sangue Bom

Plataforma de acompanhamento longitudinal de índices sanguíneos para cidadãos atendidos pelo SUS.

O **Sangue Bom** tem como objetivo centralizar o histórico de exames, acompanhar a evolução dos indicadores de saúde, auxiliar na identificação de possíveis situações de risco e facilitar o acesso do profissional de saúde à evolução dos dados do cidadão.

O projeto foi desenvolvido como MVP para o **Hackathon FIAP Pós Tech, Arquitetura e Desenvolvimento Java, Fase 5**, com foco em inovação para otimização do atendimento no SUS.

## 🚀 Stack

* Java 21
* Spring Boot 3.3.x
* Spring Web MVC
* Spring Data JPA
* Hibernate
* PostgreSQL
* Flyway
* Spring Security
* JWT
* Bean Validation
* Springdoc OpenAPI
* JUnit 5
* Mockito
* MockMvc
* Docker
* Docker Compose

A arquitetura utiliza Spring MVC em camadas, organizada por funcionalidades, separando Controller, Service, Repository e Entity.

## 📋 Pré requisitos

Para executar o projeto localmente, é necessário ter instalado:

* Docker
* Docker Compose

O Docker Compose é responsável por executar os dois serviços necessários para o ambiente:

* `postgres`, banco de dados PostgreSQL
* `sanguebom`, aplicação Spring Boot

## ⚙️ Configuração

Antes de iniciar o projeto, crie um arquivo `.env` na raiz do projeto.

Exemplo:

```env
DB_HOST=localhost
DB_PORT=5432
DB_USERNAME=postgres
DB_PASSWORD=postgres
DB_NAME=sanguebom
```

> **Importante:** o arquivo `.env` não deve ser versionado no repositório caso contenha credenciais reais.

Recomenda se utilizar um `.env.example` para disponibilizar apenas a estrutura das variáveis necessárias.

Exemplo:

```env
DB_HOST=localhost
DB_PORT=5432
DB_USERNAME=postgres
DB_PASSWORD=
DB_NAME=sanguebom
```

## ⚡ Subindo com um comando

> Passo a passo completo, incluindo os testes de API e o que fazer quando algo dá errado:
> **[GUIA.md](GUIA.md)**.

O jeito mais rápido de levantar o ambiente é usar o script de inicialização:

```bash
./scripts/start.sh
```

Ele abre um menu com três opções:

```text
1) Subir com banco LIMPO  (apaga tudo e reaplica as migrations)
2) Subir normalmente      (mantem os dados existentes)
3) Parar o ambiente
```

A opção **1** remove o volume do PostgreSQL e sobe tudo do zero, deixando o Flyway reconstruir
schema e seeds. A opção **2** mantém os dados que já estão lá. A opção **3** para os containers
sem apagar nada.

Para uso não interativo (CI, scripts):

```bash
./scripts/start.sh --clean --yes   # banco limpo, sem confirmação
./scripts/start.sh --keep          # mantém os dados
./scripts/start.sh --down          # para o ambiente
```

O script verifica o Docker, cria um `.env` padrão se não existir, espera o PostgreSQL ficar
saudável e a aplicação responder, e mostra os logs do container se algo falhar na subida.

## 🧪 Testes de API (Postman + Newman)

A pasta `postman/` traz uma collection que cobre os 59 endpoints do projeto, em casos de sucesso
e de falha. Para executar:

```bash
./scripts/api-test.sh
```

Por padrão o script **recria o ambiente do zero** antes de rodar, o que torna a execução
reproduzível. Para rodar contra o ambiente que já estiver de pé:

```bash
./scripts/api-test.sh --keep
```

A collection também pode ser importada direto no Postman:

* `postman/sanguebom.postman_collection.json`
* `postman/sanguebom.local.postman_environment.json`

### Lendo o resultado

São **139 requests e 204 asserções**, e o esperado é **tudo verde**. Qualquer falha é regressão.

A collection foi escrita antes das correções e encontrou 10 bugs, todos já corrigidos; as
asserções que os expuseram continuam ali como regressão. O histórico, com causa e correção de
cada um, está em [`postman/RELATORIO.md`](postman/RELATORIO.md).

A pasta `99 - SSE (manual)` fica fora da execução automatizada, porque o stream de notificações
mantém a conexão aberta por 30 minutos.

> **Atenção:** a API não possui autenticação. Não há Spring Security, JWT nem verificação de
> perfil no código — os 59 endpoints são abertos. A seção [🔐 Segurança](#-segurança) descreve um
> design que ainda não foi implementado.

## 🐳 Subindo o projeto com Docker Compose

Com o Docker em execução, na raiz do projeto execute:

```bash
docker compose up --build
```

O comando irá:

1. Criar o container do PostgreSQL.
2. Aguardar o PostgreSQL estar disponível.
3. Criar o container da aplicação Sangue Bom.
4. Disponibilizar a aplicação na porta `8080`.
5. Disponibilizar o PostgreSQL na porta definida em `DB_PORT`.
6. Executar as migrations do Flyway durante a inicialização da aplicação.

Para executar os containers em segundo plano:

```bash
docker compose up --build -d
```

Para acompanhar os logs:

```bash
docker compose logs -f
```

Para acompanhar somente os logs da aplicação:

```bash
docker compose logs -f sanguebom
```

Para acompanhar somente os logs do banco:

```bash
docker compose logs -f postgres
```

## 🛑 Parando o ambiente

Para parar os containers:

```bash
docker compose down
```

Para parar os containers e remover também os recursos criados pelo Compose:

```bash
docker compose down --remove-orphans
```

## 🔄 Recriando o ambiente

Caso seja necessário reconstruir a imagem da aplicação após alterações no código:

```bash
docker compose up --build
```

Caso seja necessário reconstruir as imagens sem utilizar o cache:

```bash
docker compose build --no-cache
docker compose up
```

## 🗄️ Banco de dados

O projeto utiliza PostgreSQL como banco de dados principal.

Dentro do Docker Compose, a aplicação não acessa o banco utilizando `localhost`. O hostname utilizado entre os containers é:

```text
postgres
```

Isso ocorre porque os dois serviços estão conectados à mesma rede Docker:

```yaml
networks:
  sanguebom-network:
    driver: bridge
```

A aplicação utiliza as seguintes configurações dentro do container:

```text
DB_HOST=postgres
DB_PORT=5432
DB_USERNAME=${DB_USERNAME}
DB_PASSWORD=${DB_PASSWORD}
DB_NAME=${DB_NAME}
```

O PostgreSQL, por sua vez, fica disponível para acesso externo através da porta definida em `DB_PORT`.

Por exemplo, utilizando:

```env
DB_PORT=5432
```

o banco poderá ser acessado localmente através de:

```text
localhost:5432
```

## 🧬 Migrations

O controle da estrutura do banco de dados é realizado utilizando **Flyway**.

As migrations devem ser executadas automaticamente durante a inicialização da aplicação.

A documentação do projeto prevê que o schema e os dados iniciais sejam versionados através do Flyway, incluindo o schema principal e os seeds necessários para catálogo, regras, conquistas e dados de demonstração.

Estrutura esperada:

```text
src/
└── main/
    └── resources/
        └── db/
            └── migration/
                ├── V1__schema.sql
                ├── V2__seed_catalog.sql
                ├── V3__seed_reference_rules.sql
                ├── V4__seed_achievements.sql
                └── V5__seed_demo.sql
```

O banco deve ser inicializado automaticamente ao subir a aplicação.

## 🔎 Verificando a aplicação

Após executar:

```bash
docker compose up --build
```

a aplicação estará disponível em:

```text
http://localhost:8080
```

### Swagger

A documentação da API pode ser acessada através do Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

A documentação do projeto prevê o uso do Springdoc OpenAPI e Swagger UI para documentação e demonstração da API.

## ❤️ Fluxo principal do MVP

O fluxo principal da aplicação é:

```text
Cidadão
   │
   ▼
Cadastro
   │
   ▼
Perfil de saúde
   │
   ▼
Meta de exames
   │
   ▼
Exame realizado
   │
   ▼
Laboratório publica resultado
   │
   ▼
Motor de pré diagnóstico
   │
   ├── Flags por exame
   ├── Score de risco
   └── Nível de risco
   │
   ▼
Notificação
   │
   ▼
Gamificação
   │
   ▼
Histórico do cidadão
   │
   ▼
Timeline para o médico
```

O MVP contempla cadastro e perfil de saúde, metas de exames, publicação de resultados pelo laboratório, registro de pressão arterial, motor de pré diagnóstico, histórico, gamificação, notificações e acesso médico à evolução temporal dos indicadores.

## 🔐 Segurança

A aplicação utiliza Spring Security e JWT.

Existem três perfis principais:

| Perfil         | Responsabilidade                   |
| -------------- | ---------------------------------- |
| `ROLE_CITIZEN` | Acesso aos próprios dados          |
| `ROLE_LAB`     | Publicação e liberação de exames   |
| `ROLE_DOCTOR`  | Consulta da evolução dos pacientes |

O isolamento dos dados do cidadão é realizado a partir do usuário identificado no token, evitando que um cidadão consulte os dados de outro usuário.

O CPF não deve ser armazenado em texto puro, sendo utilizado `cpf_hash` para sua persistência.

## 🩺 Disclaimer médico

O pré diagnóstico apresentado pelo Sangue Bom possui caráter exclusivamente informativo.

> Este é um resultado informativo gerado automaticamente a partir de valores de referência da literatura médica. Não constitui diagnóstico e não substitui a avaliação de um profissional de saúde.

Esse disclaimer faz parte da especificação do MVP e deve ser apresentado nas respostas que contenham `risk_assessment`.

## 📡 Principais endpoints

### Autenticação

```text
POST /api/v1/auth/login
POST /api/v1/auth/token
```

### Cidadão

```text
POST /api/v1/users

GET  /api/v1/users/me
PUT  /api/v1/users/me

GET  /api/v1/users/me/health-profile
PUT  /api/v1/users/me/health-profile

GET  /api/v1/users/me/exam-goal
GET  /api/v1/users/me/exams
GET  /api/v1/users/me/exams/{id}

GET  /api/v1/users/me/risk

POST /api/v1/users/me/blood-pressures
GET  /api/v1/users/me/blood-pressures

GET /api/v1/users/me/achievements
GET /api/v1/users/me/notifications
```

### Laboratório

```text
POST  /api/v1/labs/exams
PATCH /api/v1/labs/exams/{id}/release
GET   /api/v1/labs/exams
```

### Médico

```text
GET /api/v1/doctor/patients/{userId}/timeline
GET /api/v1/doctor/patients/{userId}/exams/compare
GET /api/v1/doctor/patients/{userId}/summary
```

### Catálogo

```text
GET /api/v1/exam-items
GET /api/v1/exam-items/{code}/reference-ranges
GET /api/v1/health-units
```

A especificação completa dos endpoints, perfis e responsabilidades está documentada no projeto.

## 🧪 Testes

Para executar os testes:

```bash
./mvnw test
```

Ou, caso o Maven esteja instalado localmente:

```bash
mvn test
```

O projeto prevê testes unitários e testes da camada web utilizando JUnit 5, Mockito e MockMvc, com atenção especial ao motor de regras e ao isolamento dos dados entre cidadãos.

## 📦 Estrutura do projeto

```text
src/
└── main/
    └── java/
        └── br/
            └── com/
                └── fiap/
                    └── sanguebom/
                        ├── config/
                        ├── controller/
                        ├── exception/
                        └── model/
                            └── dtos/
                            └── entities/
                        ├── repository/
                        └── service/
```

A organização é feita por feature, mantendo dentro de cada funcionalidade suas respectivas camadas de Controller, Service, Repository, Entity e DTO.

## 🏗️ Arquitetura

A aplicação segue uma arquitetura Spring MVC em camadas:

```text
                    ┌───────────────────┐
                    │      Cliente      │
                    │ Postman / Swagger │
                    └─────────┬─────────┘
                              │
                              ▼
                    ┌───────────────────┐
                    │   Spring Security │
                    │     JWT Filter    │
                    └─────────┬─────────┘
                              │
                              ▼
                    ┌───────────────────┐
                    │    Controller     │
                    │ REST + DTO +      │
                    │ Bean Validation   │
                    └─────────┬─────────┘
                              │
                              ▼
                    ┌───────────────────┐
                    │      Service      │
                    │ Regras de negócio │
                    │   + transação     │
                    └─────────┬─────────┘
                              │
                              ▼
                    ┌───────────────────┐
                    │    Repository     │
                    │   Spring Data JPA │
                    └─────────┬─────────┘
                              │
                              ▼
                    ┌───────────────────┐
                    │    PostgreSQL     │
                    └───────────────────┘
```

As regras arquiteturais definidas para o projeto estabelecem que Controllers não devem conter regras de negócio, Entities JPA não devem ser expostas pelos Controllers e Services concentram as regras e fronteiras transacionais.

## 🐘 Serviços Docker

O ambiente Docker é composto por:

### PostgreSQL

```yaml
postgres:
  image: postgres:18.4
```

Responsável pela persistência dos dados da aplicação.

### Sangue Bom

```yaml
sanguebom:
  build:
    context: .
```

Responsável pela execução da aplicação Spring Boot.

Os dois serviços utilizam a rede:

```text
sanguebom-network
```

A aplicação utiliza `postgres` como hostname do banco dentro da rede Docker.

## 🧹 Limpeza completa do ambiente

Caso seja necessário remover os containers, redes e volumes associados ao projeto:

```bash
docker compose down -v
```

> **Atenção:** o parâmetro `-v` remove os volumes associados ao Compose e, consequentemente, os dados persistidos do PostgreSQL.

Depois disso, para iniciar novamente o ambiente:

```bash
docker compose up --build
```

## 📚 Documentação adicional

A documentação completa do projeto contempla:

* Plano estruturado da solução
* Arquitetura
* Modelo de dados
* Segurança e LGPD
* Catálogo de endpoints
* Motor de pré diagnóstico
* Gamificação
* Notificações
* Checklist de desenvolvimento
* Critérios de avaliação
* Roteiro da demonstração do MVP
* Próximos passos

O fluxo recomendado para a demonstração começa pela subida do ambiente com Docker Compose, seguida de cadastro, configuração do perfil de saúde, criação da meta, publicação do exame pelo laboratório, execução do pré diagnóstico, consulta das notificações e conquistas e, por fim, consulta da evolução pelo médico.

## 👥 Projeto

**Sangue Bom**

Hackathon FIAP, Pós Tech, Arquitetura e Desenvolvimento Java, Turma 11ADJT.

**Tema:** Inovação para otimização de atendimento no SUS.

---

## 📄 Licença

Projeto desenvolvido para fins acadêmicos no Hackathon FIAP.
