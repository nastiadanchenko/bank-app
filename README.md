# Bank App - многомодульное микросервисное приложение (Kubernetes + Helm)

## Стек технологий
### Backend
* Java 21
* Spring Boot 3.x
* Spring Web / WebFlux
* Spring Data JPA (Hibernate)
* Spring Security
* OAuth2 / OpenID Connect
### Безопасность
* Keycloak - Identity Provider
* JWT - access tokens
* OAuth2 Client / Resource Server
### Хранение данных
* PostgreSQL 16
* Liquibase - миграции схемы БД
### Инфраструктура и DevOps
* Docker
* Kubernetes
* Helm
* NGINX Ingress Controller
* Gradle (multi-module project)
* Apache Kafka - брокер сообщений взаимодействия микросервисов
### Тестирование
* JUnit 5
* Spring Boot Test
* Testcontainers (PostgreSQL, Keycloak)
* Spring Security Test
* Helm tests

---

## Общее описание

**bank-app** - микросервисное банковское приложение, развёрнутое в Kubernetes с использованием Helm.

**Особенности архитектуры:**

* каждый микросервис развёрнут как Deployment
* база данных развёрнута как StatefulSet
* Service Discovery реализован средствами Kubernetes (Service + DNS)
* централизованная конфигурация реализована через ConfigMaps
* секреты хранятся в Kubernetes Secrets
* авторизация реализована через Keycloak, развёрнутый в Kubernetes
* внешний доступ осуществляется через Ingress Controller
* Helm используется для пакетирования и развёртывания приложения

Проект является многомодульным Gradle-проектом.

---

## Асинхронное взаимодействие через Apache Kafka

В системе реализовано асинхронное взаимодействие микросервисов с использованием Apache Kafka.

Apache Kafka используется как брокер сообщений для передачи событий нотификаций между сервисами.

### Kafka Topics

Используется Kafka topic:

* `notifications` - для передачи событий нотификаций

Формат сообщения:

```json
{
"serviceName": "account-service",
"userId": "123",
"message": "Account successfully updated",
"timestamp": "2025-06-16T12:00:00"
}
```
**Producer сервисы:**

* accounts-service
* cash-service
* transfer-service

**Consumer сервис:**

* notifications-service

**Схема взаимодействия:**

```
Accounts Service ─┐
Cash Service      ├──> Kafka ───> Notifications Service
Transfer Service ─┘
```

## Структура проекта
```
bank-app/
|
├── helm/ 
│   └── bank-app/ (umbrella chart) 
│       ├── charts/ 
│       |   ├── accounts/ 
│       |   ├── cash/ 
│       |   ├── notifications/ 
│       |   ├── transfer/ 
│       |   └── front-ui/
|       ├── templates/
|       └── values.yaml
|
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

Каждый сервис - самостоятельное Spring Boot приложение.

---

## Описание сервисов

### Accounts Service (`accounts-service`)

**Назначение:**

* управление банковскими счетами

**Функциональность:**

* CRUD операций над счетами
* работа с PostgreSQL
* Liquibase для миграций
* отправка событий уведомлений в Apache Kafka
---

### Cash Service (`cash-service`)

**Назначение:**

* операции с балансом (пополнение / списание)

**Функциональность:**

* бизнес-логика денежных операций
* вызовы `accounts-service`
* отправка событий уведомлений в Apache Kafka

---

### Notifications Service (`notifications-service`)

**Назначение:**

* обработка событий уведомлений

**Функциональность:**

* чтение событий уведомлений из Apache Kafka
* логирование событий


**Kafka Consumer:**

Notifications Service подписан на Kafka topic: `notifications`

### Transfer Service (`transfer-service`)

**Назначение:**

* операции с переводом средств на счёт другого пользователя

**Функциональность:**

* бизнес-логика денежных операций
* вызовы `accounts-service`
* отправка событий уведомлений в Apache Kafka

### Front UI (front-ui)

**Назначение:**

* пользовательский интерфейс банковского приложения

**Функциональность:**

* веб-интерфейс для взаимодействия с системой
* отправка запросов в backend через api-gateway

**Особенности:**

* разворачивается как отдельный сервис
* использует API Gateway как единую точку входа

---

### Service Discovery 
Service Discovery реализован через Kubernetes DNS.

Пример обращения между сервисами:

```
http://accounts:8081
http://notifications:8086
```
Kubernetes автоматически резолвит имена сервисов.

Consul, Eureka, Spring Cloud Discovery НЕ используются.

---

## Конфигурация

##### Externalized Configuration

Конфигурация хранится в:

* ConfigMaps
* Secrets

Пример:

ConfigMap:
```
DATABASE_HOST
SERVER_PORT
KEYCLOAK_URL
```
Secret:
```
DATABASE_PASSWORD
CLIENT_SECRET
```
Spring Cloud Config НЕ используется.

---

### Helm Charts

Используется зонтичный Helm chart:
* bank-app

Включает сабчарты:
* accounts
* cash
* notifications
* transfer
* front-ui
* postgresql
* keycloak

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
./gradlew bootBuildImage 
```

## Развёртывание в Kubernetes
Требования

Установлены:
* Kubernetes (minikube / kind / rancher desktop)
* kubectl
* helm

**Создание namespace**
```
kubectl create namespace bank
```
**Установка приложения**
```
helm install bank-app ./helm/bank-app -n bank
```

**Доступ к приложению**

Ingress host:

```
http://bank.local
```

**Проверка Helm тестов**

Helm tests проверяют доступность сервисов.

Запуск:

```
helm test bank-app -n bank
```

---

## Тестирование

### Unit и Context tests

```bash
./gradlew test
```

---


