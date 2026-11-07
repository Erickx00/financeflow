# ADR 0001 — Stack e versões fixadas

**Status:** aceito (Dia 1)

## Contexto
A especificação pede Java 25, Spring Boot 4.x, PostgreSQL 17, Redis 7.4+, Maven e Flyway. O ambiente não tem acesso ao Maven Central; o cache local `~/.m2` contém Spring Boot 4.1.1/4.1.2 e starters correspondentes.

## Decisão
- Spring Boot **4.1.1** como parent/BOM (todas as dependências usadas existem nessa versão no cache local).
- Java **25** (`<java.version>25</java.version>`).
- PostgreSQL **17**, Redis **7.4-alpine** no Docker Compose.
- Flyway **12.x** gerenciado pelo BOM **mais** `flyway-database-postgresql` (obrigatório desde o Flyway 10).
- springdoc-openapi **3.2.0**, versão compatível com Boot 4 (a linha 2.x é para Boot 3).
- JWT via `spring-boot-starter-oauth2-resource-server` (Nimbus JOSE), **não** `jjwt`: evita o módulo `jjwt-jackson`, que depende de Jackson 2, enquanto o Boot 4 usa Jackson 3.
- Testcontainers apenas no perfil `it` (exige Docker e rede).

## Consequências
- Versões de bibliotecas ficam sob o BOM; só o springdoc é pinado.
- Atualização de versão só com suíte verde.
- O build local offline funciona a partir do cache; a CI usa Maven Central.
