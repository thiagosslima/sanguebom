# --- 1. Etapa de Build ---
# Usa uma imagem com Maven ja instalado para compilar o projeto
FROM maven:3.9.9-amazoncorretto-21-alpine as build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn dependency:go-offline

# Compila o projeto e cria o JAR. Os testes são pulados, pois não temos um banco de dados aqui.
RUN mvn package -DskipTests

# --- 2. Etapa Final ---
# Usa uma imagem JRE (Java Runtime Environment) muito menor, apenas para executar a aplicação
# Usando a mesma versão do Java da etapa de build para garantir compatibilidade
FROM amazoncorretto:21.0.3-alpine3.19

# Cria um grupo e um usuário de sistema dedicados para rodar a aplicação
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Define o usuário que executará os próximos comandos e a aplicação
USER appuser

# Define o diretório de trabalho dentro do container
WORKDIR /app

# Copia o JAR da etapa de build para a imagem final, já com o usuário e grupo corretos
COPY --from=build --chown=appuser:appgroup /app/target/*.jar app.jar

# Comando para executar a aplicação quando o container iniciar
ENTRYPOINT ["java", "-jar", "app.jar"]
