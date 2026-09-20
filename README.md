# 🩸 Sangue Bom

Aplicação para acompanhar exames sanguíneos, pressão arterial, metas e indicadores de saúde, desenvolvida para o Hackathon FIAP Pós Tech, Arquitetura e Desenvolvimento Java, Fase 5.

O projeto reúne um frontend **Next.js + TypeScript**, uma API **Java 21 + Spring Boot 4.1**, e **PostgreSQL 18**, com migrations e dados iniciais aplicados pelo Flyway.

## Executar com um comando

Instale e inicie o **Docker com Docker Compose v2**. Na raiz do projeto:

```bash
docker compose up --build -d --wait
```

Não é necessário instalar Java, Maven ou Node.js, nem criar um `.env` para usar os padrões locais. A primeira execução baixa as imagens e compila os dois projetos, podendo levar alguns minutos.

- **Frontend:** http://localhost:3000
- **API:** http://localhost:8080
- **Swagger:** http://localhost:8080/swagger-ui/index.html

No Linux/macOS, o atalho abaixo também aguarda todos os serviços ficarem saudáveis e mostra os endereços:

```bash
./scripts/start.sh
```

Para parar preservando os dados:

```bash
docker compose down
```

Para acompanhar a inicialização ou investigar erros:

```bash
docker compose logs -f
```

## Usar o frontend

Abra http://localhost:3000. A interface usa os dados reais da API; os dados iniciais são criados automaticamente pelas migrations. Selecione um cidadão para consultar seu acompanhamento e use as áreas da interface para gerenciar os registros.

O servidor Next.js encaminha as requisições para a API pelo endereço interno `http://sanguebom:8080`. O navegador acessa tudo pela mesma origem do frontend.

A API atual não implementa autenticação ou autorização: a seleção de cidadão não representa login. O Compose publica as portas somente em `127.0.0.1`, para uso local. Autenticação e controle de acesso são necessários antes de disponibilizar dados reais na internet.

Os indicadores calculados são informativos e não substituem a avaliação de um profissional de saúde.

## Configuração opcional

Copie `.env.example` para `.env` para mudar portas ou credenciais locais:

```env
DB_PORT=5432
DB_NAME=sanguebom
DB_USERNAME=postgres
DB_PASSWORD=postgres
API_PORT=8080
FRONTEND_PORT=3000
```

Os padrões permitem começar imediatamente. Não versione credenciais reais. Alterar as credenciais no `.env` não altera usuários de um banco já inicializado.

## Desenvolvimento

Para executar apenas banco e API pelo Docker:

```bash
docker compose up --build -d postgres sanguebom
```

Se o frontend Docker já estiver ativo, pare somente ele com `docker compose stop frontend` para liberar a porta 3000. Em outro terminal, com Node.js 22 ou superior instalado:

```bash
cd frontend
npm ci
npm run dev
```

O frontend acessa `http://localhost:8080` por padrão. Para outra API, defina `BACKEND_URL` em `frontend/.env.local` e reinicie o servidor Next.js.

Para validar o frontend:

```bash
cd frontend
npm run typecheck
npm run build
```

Os testes Java podem ser executados com Java 21 e Maven (`mvn test`). Para os testes de integração, mantenha um PostgreSQL acessível e configure as variáveis de conexão exigidas pela API.

## Testes de API

A pasta `postman/` contém a collection e o ambiente para importar no Postman. Com o ambiente iniciado e Node.js instalado:

```bash
./scripts/api-test.sh --keep
```

A collection cria, modifica e remove registros de teste. A execução sem `--keep` recria o banco; consulte o [guia de uso](GUIA.md).

## Organização

```text
frontend/             Next.js, componentes e proxy para a API
src/main/java/        API Spring Boot
src/main/resources/   Configuração e migrations Flyway
src/test/             Testes Java
postman/              Testes de API
scripts/              Inicialização e testes
Dockerfile            Build e execução da API
docker-compose.yml    Frontend + API + PostgreSQL
```

Consulte [GUIA.md](GUIA.md) para comandos de operação e solução de problemas. Os endpoints efetivamente disponíveis estão documentados no Swagger da aplicação em execução.

## Projeto e licença

Hackathon FIAP, Pós Tech, Arquitetura e Desenvolvimento Java, Turma 11ADJT. Tema: inovação para otimização do atendimento no SUS. Projeto desenvolvido para fins acadêmicos.

Detalhes e testes do front-end: [frontend/README.md](frontend/README.md).

## Cenários de demonstração

A nova migration adiciona 34 cidadãos fictícios, 74 exames e cenários de metas, riscos, conquistas, notificações, dados incompletos e importados. Consulte [a matriz de cenários](docs/CENARIOS_DEMONSTRACAO.md) para escolher cada cidadão e validar a carga. Os registros anteriores são preservados.
