# Bank App — многомодульное микросервисное приложение

## Стек технологий
### Backend
* Java 21
* Spring Boot 3.x
* Spring Web / WebFlux
* Spring Data JPA (Hibernate)
* Spring Security
* OAuth2 / OpenID Connect
### Spring Cloud
* Spring Cloud Config Server / Client - централизованная конфигурация
* Spring Cloud Consul Discovery - Consul docker image и health-check
* Spring Cloud Gateway - API Gateway
### Безопасность
* Keycloak — Identity Provider
* JWT — access tokens
* OAuth2 Client / Resource Server
### Хранение данных
* PostgreSQL 16
* Liquibase - миграции схемы БД
### Инфраструктура и DevOps
* Docker
* docker-compose
* Gradle (multi-module project)
### Тестирование
* JUnit 5
* Spring Boot Test
* Testcontainers (PostgreSQL, Keycloak)
* Spring Security Test

---

## Общее описание

**bank-app** — микросервисный проект, реализующий банковский домен. 
Проект построен на Spring Boot и Spring Cloud:
* конфигурации микросервисов через Spring Cloud Config
* сервис-дискавери через Consul
* централизованной аутентификации и авторизации через Keycloak (OAuth2 / OIDC)
* взаимодействия сервисов по HTTP
* контейнеризации через Docker / docker-compose
* интеграционного тестирования с Testcontainers

Проект является многомодульным Gradle-проектом.

## Структура проекта

```
bank-app/
├── config-server/
├── api-gateway/
├── accounts-service/
├── cash-service/
├── notifications-service/
├── transfers-service/
├── front-ui/
├── docker-compose.yml
├── build.gradle
└── settings.gradle
```

Каждый сервис — самостоятельное Spring Boot приложение.

---

## Описание сервисов

### 1. Config Server (`config-server`)

**Назначение:**
Централизованное хранение и раздача конфигураций для всех микросервисов.

**Технологии:**

* Spring Cloud Config Server
* Native backend (classpath)

**Особенности:**

* Конфигурации хранятся внутри JAR в каталоге `config-repo/`
* Каждый сервис получает конфиг по шаблону:

  ```
  /{application-name}/{profile}
  ```

**Пример запроса:**

```
GET http://config-server:8888/cash-service/default
```

---

### 2. Service Discovery — Consul

**Назначение:**

* регистрация сервисов
* health-check
* service discovery

**Использование:**
Каждый сервис при старте:

* регистрируется в Consul
* публикует HTTP health-check

**Health-check:**

```
/actuator/health
```

---

### 3. API Gateway (`api-gateway`)

**Назначение:**

* единая точка входа в систему
* маршрутизация запросов
* интеграция с OAuth2 / Keycloak

**Функции:**

* проверка JWT токенов
* проксирование запросов во внутренние сервисы

---

### 4. Accounts Service (`accounts-service`)

**Назначение:**

* управление банковскими счетами

**Функциональность:**

* CRUD операций над счетами
* работа с PostgreSQL
* Liquibase для миграций

**Интеграции:**

* Config Server
* Consul
* Keycloak (Resource Server)

---

### 5. Cash Service (`cash-service`)

**Назначение:**

* операции с балансом (пополнение / списание)

**Функциональность:**

* бизнес-логика денежных операций
* вызовы `accounts-service`

---

### 6. Notifications Service (`notifications-service`)

**Назначение:**

* отправка уведомлений пользователям

**Функциональность:**

* REST API для приёма событий
* взаимодействие с другими сервисами

### 7. Transfer Service (`transfer-service`)

**Назначение:**

* операции с переводом средств на счёт другого пользователя

**Функциональность:**

* бизнес-логика денежных операций
* вызовы `accounts-service`

### 8. Front UI (front-ui)

**Назначение:**

* пользовательский интерфейс банковского приложения

**Функциональность:**

* веб-интерфейс для взаимодействия с системой
* отправка запросов в backend через api-gateway

**Особенности:**

* разворачивается как отдельный сервис
* не взаимодействует напрямую с Config Server и Consul
* использует API Gateway как единую точку входа


### Service Discovery — Consul
Реализация: Service Discovery реализован за счёт использования официального Docker-образа HashiCorp Consul.

**Назначение:**

* регистрация сервисов
* health-check
* service discovery

**Использование:** Каждый сервис при старте:

* регистрируется в Consul
* публикует HTTP health-check

**Health-check:**
```
 /actuator/health
```
---

---

## Конфигурация

### Config Server

Конфигурации сервисов лежат в:

```
config-server/src/main/resources/config-repo/
```

Пример:

```
cash-service-default.yml
accounts-service-default.yml
```

---

## Сборка проекта

### Сборка всех модулей

```bash
./gradlew clean build
```

### Сборка отдельного сервиса

```bash
./gradlew :cash-service:build
```

---

## Docker и запуск

### Сборка Docker-образов

```bash
docker-compose build
```

### Запуск всех сервисов

```bash
docker-compose up -d
```

Сервисы стартуют в следующем порядке:

1. Consul
2. Config Server
3. Keycloak
4. Postgres 
5. Бизнес-сервисы

---

## Тестирование

### Unit и Context tests

```bash
./gradlew test
```

### Особенности тестов

* В тестовом профиле (`test`):

    * отключён Config Server
    * отключён Consul
    * Security может быть выключена или замокана

---


