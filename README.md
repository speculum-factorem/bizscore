# BizScore Service

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.0-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2CA5E0?style=for-the-badge&logo=docker&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.9-02303A?style=for-the-badge&logo=apache-maven&logoColor=white)

Комплексный сервис для скоринга бизнеса в реальном времени. Система предоставляет возможность финансовым институтам и кредитным организациям оценивать кредитоспособность компаний на основе их финансовых показателей, истории и других факторов риска.

## Содержание

- [Описание проекта](#описание-проекта)
- [Архитектура](#архитектура)
- [Сущности и их взаимодействие](#сущности-и-их-взаимодействие)
- [Технологический стек](#технологический-стек)
- [Установка и запуск](#установка-и-запуск)
- [Конфигурация](#конфигурация)
- [API документация](#api-документация)
- [Безопасность](#безопасность)
- [Мониторинг](#мониторинг)
- [Разработка](#разработка)
- [Развертывание](#развертывание)

## Описание проекта

BizScore Service - это комплексный микросервис для скоринга бизнеса, который позволяет:

- Рассчитывать скоринговые баллы для компаний на основе финансовых показателей
- Интегрироваться с ML сервисом для точных прогнозов
- Применять политики риска для автоматического принятия решений
- Управлять решениями по кредитным заявкам
- Отслеживать историю скоринговых запросов
- Анализировать тенденции и статистику
- Обеспечивать аудит всех операций

### Основные возможности

**Скоринг бизнеса**

- Расчет скорингового балла на основе финансовых показателей компании
- Интеграция с ML сервисом для точных прогнозов
- Определение уровня риска (LOW, MEDIUM, HIGH)
- Валидация ИНН и других входных данных
- Кэширование результатов для повышения производительности

**Управление политиками риска**

- Создание и настройка политик риска
- Автоматическое применение политик при скоринге
- Приоритизация политик
- Условия для автоматического одобрения/отклонения
- Эскалация для ручного рассмотрения

**Управление решениями**

- Автоматическое принятие решений на основе политик
- Ручное рассмотрение заявок менеджерами
- История всех решений с причинами
- Приоритизация заявок
- Статусы решений (PENDING, APPROVED, REJECTED)

**Пакетная обработка**

- Асинхронная обработка множественных запросов
- Параллельное выполнение скоринга
- Агрегация результатов
- Обработка ошибок

**Аналитика и отчетность**

- Статистика по скоринговым запросам
- Анализ тенденций
- Сравнительный анализ компаний
- Метрики производительности

**Real-Time Мониторинг**

- Метрики и health checks
- Интеграция с Prometheus и Grafana
- SLA мониторинг
- Структурированное логирование с MDC

## Архитектура

### Общая архитектура

Система построена на основе микросервисной архитектуры с использованием Spring Boot:

```
┌─────────────────────────────────────────────────────────────────┐
│                      Client Applications                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │ Web Browser  │  │ Mobile App   │  │ External API │           │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘           │
└─────────┼─────────────────┼─────────────────┼───────────────────┘
          │                 │                 │
          └─────────────────┼─────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────────┐
│                  BizScore Service (8080)                        │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Filters: Auth (JWT), RateLimit, Logging, Metrics        │   │
│  └───────────────────────┬──────────────────────────────────┘   │
│                          │                                      │
│  ┌───────────────────────┼──────────────────────────────────┐   │
│  │  Controllers: Scoring, Advanced, Decision, Policy        │   │
│  └───────────────────────┼──────────────────────────────────┘   │
│                          │                                      │
│  ┌───────────────────────┼──────────────────────────────────┐   │
│  │  Services: Scoring, PolicyEngine, Audit, Metrics         │   │
│  └───────────────────────┼──────────────────────────────────┘   │
└──────────────────────────┼──────────────────────────────────────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
┌───────▼────────┐  ┌──────▼────-───┐ ┌───────▼────────┐
│  PostgreSQL    │  │    ML         │ │    Caffeine    │
│  Database      │  │  Service      │ │     Cache      │
└────────────────┘  └───────────────┘ └────────────────┘
        │
        │
┌───────▼───────────────────────────────────────────────────────┐
│              Infrastructure Services                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │ Prometheus   │  │   Grafana    │  │   Logging    │         │
│  │ (9090)       │  │   (3000)     │  │   (Logback)  │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
└───────────────────────────────────────────────────────────────┘
```

### Слои архитектуры

**Controller Layer (Слой контроллеров)**

- REST API endpoints для всех операций
- Валидация входных данных
- Обработка ошибок
- Swagger/OpenAPI документация

**Service Layer (Слой сервисов)**

- **ScoringService** - основная бизнес-логика скоринга
- **AdvancedScoringService** - расширенные функции (пакетная обработка, аналитика)
- **PolicyEngineService** - оценка политик риска
- **RiskPolicyService** - управление политиками
- **AuditService** - аудит операций
- **MetricsService** - сбор метрик

**Repository Layer (Слой репозиториев)**

- **ScoringRepository** - доступ к данным скоринговых запросов
- **ScoringDecisionRepository** - доступ к решениям
- **RiskPolicyRepository** - доступ к политикам
- **AuditLogRepository** - доступ к логам аудита

**Data Layer (Слой данных)**

- **PostgreSQL** - основное хранилище данных
- **Caffeine Cache** - кэширование результатов
- **ML Service** - внешний сервис для расчета скоринга

**Infrastructure Layer (Инфраструктурный слой)**

- **Prometheus** - сбор метрик
- **Grafana** - визуализация метрик
- **Logback** - структурированное логирование

### Паттерны проектирования

- **Service Layer Pattern** - разделение бизнес-логики
- **Repository Pattern** - абстракция доступа к данным
- **Strategy Pattern** - различные стратегии оценки политик
- **Retry Pattern** - повторные попытки для ML сервиса
- **Cache-Aside Pattern** - кэширование результатов
- **Aspect-Oriented Programming** - логирование и метрики

## Сущности и их взаимодействие

### Основные сущности

#### ScoringRequest (Запрос на скоринг)

Основная сущность, представляющая запрос на расчет скоринга для компании.

```java
@Entity
@Table(name = "scoring_request")
public class ScoringRequest {
    private Long id;                    // ID запроса
    private String companyName;          // Название компании
    private String inn;                 // ИНН компании
    private String businessType;        // Тип бизнеса
    private Integer yearsInBusiness;     // Лет в бизнесе
    private Double annualRevenue;        // Годовая выручка
    private Integer employeeCount;       // Количество сотрудников
    private Double requestedAmount;      // Запрашиваемая сумма
    private Boolean hasExistingLoans;    // Есть ли существующие займы
    private String industry;             // Отрасль
    private Integer creditHistory;       // Кредитная история
    private Double score;                // Рассчитанный скоринг (calculated)
    private String riskLevel;            // Уровень риска (calculated)
    private LocalDateTime createdAt;     // Дата создания
}
```

**Расчетные поля:**

- `score` - рассчитывается ML сервисом
- `riskLevel` - определяется на основе score:
  - `LOW` - score >= 70
  - `MEDIUM` - 50 <= score < 70
  - `HIGH` - score < 50

**Связи:**

- Связана с ScoringDecision (1:1) - решение по запросу
- Связана с AuditLog (1:N) - логи аудита

#### ScoringDecision (Решение по скорингу)

Решение, принятое на основе политик риска и скоринга.

```java
@Entity
@Table(name = "scoring_decisions")
public class ScoringDecision {
    private Long id;                    // ID решения
    private Long scoringRequestId;      // ID запроса на скоринг
    private String decision;            // Решение (APPROVED, REJECTED, PENDING)
    private String reason;              // Причина решения
    private String appliedPolicy;       // Примененная политика
    private String priority;             // Приоритет (LOW, MEDIUM, HIGH, URGENT)
    private String managerNotes;        // Заметки менеджера
    private String finalDecision;       // Финальное решение
    private String resolvedBy;          // Кто разрешил
    private LocalDateTime resolvedAt;   // Дата разрешения
    private LocalDateTime createdAt;    // Дата создания
}
```

**Типы решений:**

- `APPROVED` - одобрено автоматически или вручную
- `REJECTED` - отклонено автоматически или вручную
- `PENDING` - ожидает ручного рассмотрения

**Приоритеты:**

- `LOW` - низкий приоритет
- `MEDIUM` - средний приоритет
- `HIGH` - высокий приоритет
- `URGENT` - срочный

#### RiskPolicy (Политика риска)

Политика для автоматического принятия решений на основе условий.

```java
@Entity
@Table(name = "risk_policies")
public class RiskPolicy {
    private Long id;                    // ID политики
    private String name;               // Название политики
    private String description;         // Описание
    private String policyType;          // Тип политики
    private Boolean isActive;           // Активна ли политика
    private Integer priority;           // Приоритет политики
    private List<PolicyCondition> conditions; // Условия политики
    private String action;              // Действие (APPROVE, REJECT, ESCALATE)
    private String actionValue;         // Значение действия
    private LocalDateTime createdAt;    // Дата создания
    private LocalDateTime updatedAt;    // Дата обновления
}
```

**Типы политик:**

- `AUTO_APPROVE` - автоматическое одобрение
- `AUTO_REJECT` - автоматическое отклонение
- `ESCALATE` - эскалация для ручного рассмотрения

**Условия политики:**

- Проверка выручки (revenue >= threshold)
- Проверка возраста бизнеса (yearsInBusiness >= threshold)
- Проверка скоринга (score >= threshold)
- Комбинации условий через AND/OR

#### PolicyCondition (Условие политики)

Условие для оценки политики риска.

```java
@Entity
@Table(name = "policy_conditions")
public class PolicyCondition {
    private Long id;                    // ID условия
    private Long policyId;              // ID политики
    private String field;               // Поле для проверки
    private String operator;            // Оператор (>=, <=, ==, !=)
    private String value;               // Значение для сравнения
    private String logicalOperator;     // Логический оператор (AND, OR)
}
```

#### AuditLog (Лог аудита)

Лог всех операций для аудита и отслеживания.

```java
@Entity
@Table(name = "audit_logs")
public class AuditLog {
    private Long id;                    // ID лога
    private String action;              // Действие
    private String entityType;          // Тип сущности
    private Long entityId;              // ID сущности
    private String userId;              // ID пользователя
    private String details;             // Детали
    private LocalDateTime timestamp;    // Время события
}
```

#### User (Пользователь)

Пользователь системы для аутентификации и авторизации.

```java
@Entity
@Table(name = "users")
public class User {
    private Long id;                    // ID пользователя
    private String username;             // Имя пользователя
    private String password;            // Пароль (hashed)
    private String email;               // Email
    private String role;                // Роль (ADMIN, MANAGER, ANALYST)
    private Boolean enabled;             // Активен ли пользователь
}
```

### Взаимодействие сущностей

#### Сценарий 1: Расчет скоринга

```
1. Client → BizScore Service
   POST /api/score
   │
   ├─▶ JwtAuthenticationFilter.validateToken()
   │
   ├─▶ RateLimitFilter.checkRateLimit()
   │
   └─▶ ScoringController.calculateScore()
      │
      ├─▶ ScoringService.calculateScore()
      │   │
      │   ├─▶ Валидация данных (ИНН, обязательные поля)
      │   ├─▶ Преобразование DTO в Entity
      │   ├─▶ Сохранение в PostgreSQL
      │   │
      │   ├─▶ PolicyEngineService.evaluatePolicies()
      │   │   │
      │   │   ├─▶ Загрузка активных политик
      │   │   ├─▶ Оценка условий каждой политики
      │   │   ├─▶ Применение политики с наивысшим приоритетом
      │   │   ├─▶ Создание ScoringDecision
      │   │   └─▶ Сохранение решения в БД
      │   │
      │   ├─▶ MlServiceClient.calculateScore()
      │   │   │
      │   │   ├─▶ Вызов ML сервиса (POST /api/v1/score)
      │   │   ├─▶ Retry при ошибках (до 3 попыток)
      │   │   └─▶ Получение скоринга и уровня риска
      │   │
      │   ├─▶ ScoringProcessor.processScoring()
      │   │   │
      │   │   ├─▶ Объединение результатов политик и ML
      │   │   ├─▶ Обновление ScoringRequest (score, riskLevel)
      │   │   └─▶ Обновление ScoringDecision при необходимости
      │   │
      │   ├─▶ ScoringResponseEnricher.enrich()
      │   │   │
      │   │   └─▶ Добавление дополнительной информации в ответ
      │   │
      │   ├─▶ Инвалидация кэша
      │   ├─▶ AuditService.logAction()
      │   └─▶ Запись метрик
      │
      └─▶ Возврат EnhancedScoringResponse клиенту
```

#### Сценарий 2: Пакетная обработка

```
1. Client → BizScore Service
   POST /api/v2/scoring/batch
   │
   └─▶ AdvancedScoringController.calculateBatchScore()
      │
      ├─▶ AdvancedScoringService.processBatchScoring()
      │   │
      │   ├─▶ Разделение запросов на группы
      │   ├─▶ Асинхронная обработка (@Async)
      │   │   │
      │   │   ├─▶ CompletableFuture для каждого запроса
      │   │   ├─▶ Параллельный вызов ScoringService.calculateScore()
      │   │   └─▶ Сбор результатов
      │   │
      │   ├─▶ Агрегация успешных результатов
      │   ├─▶ Сбор ошибок
      │   └─▶ Формирование BatchScoringResponse
      │
      └─▶ Возврат BatchScoringResponse клиенту
```

#### Сценарий 3: Создание политики риска

```
1. Client → BizScore Service
   POST /api/policies
   │
   └─▶ RiskPolicyController.createPolicy()
      │
      ├─▶ RiskPolicyService.createPolicy()
      │   │
      │   ├─▶ Валидация данных политики
      │   ├─▶ Создание RiskPolicy
      │   ├─▶ Создание PolicyCondition для каждого условия
      │   ├─▶ Сохранение в PostgreSQL
      │   ├─▶ Инвалидация кэша политик
      │   ├─▶ AuditService.logAction()
      │   └─▶ Запись метрик
      │
      └─▶ Возврат созданной политики клиенту
```

#### Сценарий 4: Обновление решения

```
1. Client → BizScore Service
   PUT /api/decisions/{id}
   │
   └─▶ DecisionController.updateDecision()
      │
      ├─▶ ScoringService.updateDecision()
      │   │
      │   ├─▶ Поиск ScoringDecision по ID
      │   ├─▶ Обновление finalDecision, managerNotes
      │   ├─▶ Установка resolvedBy, resolvedAt
      │   ├─▶ Сохранение в PostgreSQL
      │   ├─▶ AuditService.logAction()
      │   └─▶ Запись метрик
      │
      └─▶ Возврат обновленного решения клиенту
```

### Потоки данных

**Запись данных:**

```
Client → Controller → Service → Repository → PostgreSQL
                              │
                              ├─▶ Cache Service → Caffeine
                              │
                              └─▶ ML Service Client → ML Service
```

**Чтение данных:**

```
Client → Controller → Service → Cache Service (Caffeine) → Repository (PostgreSQL)
```

**Асинхронная обработка:**

```
Service → @Async → CompletableFuture → Service → Repository → PostgreSQL
```

## Технологический стек

### Backend Framework

- **Java 21** - основной язык разработки
- **Spring Boot 3.3.0** - основной фреймворк
- **Spring Web MVC** - REST API
- **Spring Data JPA** - работа с PostgreSQL
- **Spring Security** - безопасность и аутентификация
- **Spring Validation** - валидация данных
- **Spring Boot Actuator** - мониторинг приложения
- **Spring AOP** - аспектно-ориентированное программирование
- **Spring Retry** - повторные попытки для внешних сервисов
- **Spring Cache** - кэширование

### Data Layer

- **PostgreSQL 15** - основное хранилище данных
- **Hibernate** - ORM
- **Caffeine** - кэширование в памяти
- **HikariCP** - пул соединений с БД

### Security

- **JWT (JSON Web Tokens)** - аутентификация
- **Spring Security** - защита endpoints
- **BCrypt** - хеширование паролей

### Resilience & Reliability

- **Spring Retry** - повторные попытки для ML сервиса
- **Rate Limiting** - ограничение частоты запросов
- **Circuit Breaker Pattern** - защита от каскадных отказов

### Monitoring & Observability

- **Micrometer** - сбор метрик приложения
- **Prometheus** - хранение метрик
- **Grafana** - визуализация метрик
- **Spring Boot Actuator** - health checks и метрики
- **MDC Logging** - контекстное логирование с correlation ID
- **Logback** - логирование с JSON форматом

### Development & Quality

- **JUnit 5** - unit тестирование
- **Mockito** - мокирование зависимостей
- **Testcontainers** - интеграционное тестирование
- **Lombok** - уменьшение boilerplate кода
- **Maven** - система сборки
- **JaCoCo** - покрытие кода тестами

### API & Documentation

- **OpenAPI 3** - спецификация API
- **Swagger UI** - интерактивная документация
- **RESTful Design** - REST архитектура

### Infrastructure

- **Docker** - контейнеризация
- **Docker Compose** - оркестрация контейнеров

## Установка и запуск

### Требования

- Java 21 или выше
- Maven 3.9 или выше
- PostgreSQL 15 или выше
- Docker и Docker Compose (опционально, но рекомендуется)

### Локальная установка

1. **Клонирование репозитория:**

```bash
git clone https://github.com/speculum-factorem/bizscore.git
cd bizscore
```

2. **Создание базы данных:**

```bash
# Подключитесь к PostgreSQL
psql -U postgres

# Создайте базу данных
CREATE DATABASE bizscoredb;

# Выйдите из psql
\q
```

3. **Настройка конфигурации:**

Отредактируйте `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/bizscoredb
spring.datasource.username=postgres
spring.datasource.password=your_password
ml.service.url=http://localhost:8000
```

4. **Сборка проекта:**

```bash
mvn clean install
```

5. **Запуск приложения:**

```bash
mvn spring-boot:run
```

Или используйте JAR файл:

```bash
java -jar target/bizscore-service-0.0.1-SNAPSHOT.jar
```

### Запуск с Docker Compose

1. **Запуск всех сервисов:**

```bash
docker-compose up -d
```

Это запустит:
- BizScore Service (порт 8080)
- PostgreSQL (порт 5432)
- Prometheus (порт 9090)
- Grafana (порт 3000)

2. **Проверка статуса:**

```bash
docker-compose ps
```

3. **Просмотр логов:**

```bash
# Все сервисы
docker-compose logs -f

# Конкретный сервис
docker-compose logs -f bizscore-app
```

4. **Остановка сервисов:**

```bash
docker-compose down

# С удалением volumes
docker-compose down -v
```

### Интеграция с ML сервисом

BizScore Service интегрируется с внешним ML сервисом для расчета скоринга. 

**ML Service Repository:** [ссылка на репозиторий ML сервиса](https://github.com/speculum-factorem/bizscore-pro-ml.git)

Для работы сервиса необходимо:

1. Запустить ML сервис (см. документацию в репозитории ML сервиса)
2. Настроить URL ML сервиса в `application.properties`:

```properties
ml.service.url=http://localhost:8000
ml.service.timeout.connect=5000
ml.service.timeout.read=10000
```

3. ML сервис должен предоставлять endpoint:

```
POST /api/v1/score
Content-Type: application/json

{
  "companyName": "ООО Пример",
  "inn": "1234567890",
  "yearsInBusiness": 5,
  "annualRevenue": 10000000.0,
  "employeeCount": 50,
  "requestedAmount": 5000000.0,
  "hasExistingLoans": false,
  "industry": "IT",
  "creditHistory": 3
}
```

Ответ ML сервиса:

```json
{
  "score": 75.5,
  "riskLevel": "LOW",
  "confidence": 0.92
}
```

### Проверка работоспособности

После запуска приложение доступно по адресам:

- **BizScore Service**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)

**Health Checks:**

- Health endpoint: http://localhost:8080/api/health
- Actuator health: http://localhost:8080/actuator/health
- Actuator metrics: http://localhost:8080/actuator/metrics
- Prometheus metrics: http://localhost:8080/actuator/prometheus

## Конфигурация

### Профили Spring Boot

- **default** - конфигурация по умолчанию
- **test** - для тестирования
- **prod** - для production окружения
- **docker** - для Docker контейнеров

### Основные параметры конфигурации

**application.properties:**

```properties
# Application
spring.application.name=bizscore-service
server.port=8080

# PostgreSQL Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/bizscoredb
spring.datasource.username=postgres
spring.datasource.password=password

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# ML Service
ml.service.url=http://localhost:8000
ml.service.timeout.connect=5000
ml.service.timeout.read=10000

# JWT Configuration
jwt.secret=${JWT_SECRET:your-secret-key-min-32-chars}
jwt.expiration=86400000

# Cache Configuration
spring.cache.type=caffeine
spring.cache.caffeine.spec=maximumSize=500,expireAfterWrite=300s

# Actuator
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=always
management.metrics.export.prometheus.enabled=true

# Policy Configuration
policy.evaluation.enabled=true
policy.auto-approve.enabled=true
policy.auto-reject.enabled=true
policy.escalation.enabled=true
```

### Переменные окружения

**Общие:**

- `SPRING_PROFILES_ACTIVE` - активный профиль
- `SPRING_DATASOURCE_URL` - URL базы данных
- `SPRING_DATASOURCE_USERNAME` - пользователь PostgreSQL
- `SPRING_DATASOURCE_PASSWORD` - пароль PostgreSQL
- `ML_SERVICE_URL` - URL ML сервиса
- `JWT_SECRET` - секретный ключ JWT (минимум 32 символа)

**Пример запуска с переменными окружения:**

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/bizscoredb
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=password
export ML_SERVICE_URL=http://localhost:8000
export JWT_SECRET=your-secret-key-min-32-chars

mvn spring-boot:run
```

## API документация

### Базовый URL

```
http://localhost:8080/api
```

### Аутентификация

Большинство endpoints требуют JWT токен в заголовке:

```
Authorization: Bearer <token>
```

**Получение токена:**

```
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password"
}
```

Ответ:

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "username": "admin"
}
```

### Основные endpoints

#### Scoring API

**Расчет скоринга:**

- `POST /api/score` - расчет скоринга для компании

  ```json
  {
    "companyName": "ООО Пример",
    "inn": "1234567890",
    "businessType": "LLC",
    "yearsInBusiness": 5,
    "annualRevenue": 10000000.0,
    "employeeCount": 50,
    "requestedAmount": 5000000.0,
    "hasExistingLoans": false,
    "industry": "IT",
    "creditHistory": 3
  }
  ```

- `GET /api/score/{id}` - получение результата по ID
- `GET /api/scores` - получение всех результатов (с пагинацией)
- `GET /api/scores/risk/{riskLevel}` - результаты по уровню риска
- `GET /api/scores/stats` - статистика по скорингу
- `GET /api/score/{id}/enhanced` - расширенная информация о скоринге

#### Advanced Scoring API

**Пакетная обработка:**

- `POST /api/v2/scoring/batch` - пакетный расчет скоринга

  ```json
  {
    "requests": [
      {
        "companyName": "ООО Пример 1",
        "inn": "1234567890",
        ...
      },
      {
        "companyName": "ООО Пример 2",
        "inn": "0987654321",
        ...
      }
    ]
  }
  ```

- `POST /api/v2/scoring/{id}/recalculate` - перерасчет скоринга
- `GET /api/v2/scoring/analytics/trends` - анализ тенденций
- `POST /api/v2/scoring/analytics/compare` - сравнительный анализ

#### Decision API

**Управление решениями:**

- `GET /api/decisions` - получение всех решений
- `GET /api/decisions/{id}` - получение решения по ID
- `PUT /api/decisions/{id}` - обновление решения

  ```json
  {
    "finalDecision": "APPROVED",
    "managerNotes": "Одобрено менеджером",
    "resolvedBy": "manager1"
  }
  ```

- `GET /api/decisions/pending` - ожидающие решения
- `GET /api/decisions/stats` - статистика по решениям

#### Risk Policy API

**Управление политиками:**

- `POST /api/policies` - создание политики
- `GET /api/policies` - получение всех политик
- `GET /api/policies/{id}` - получение политики по ID
- `PUT /api/policies/{id}` - обновление политики
- `DELETE /api/policies/{id}` - удаление политики
- `PUT /api/policies/{id}/activate` - активация политики
- `PUT /api/policies/{id}/deactivate` - деактивация политики

### Swagger UI

Полная интерактивная документация доступна в Swagger UI:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

### Формат ответа

Все API endpoints возвращают стандартизированный формат:

**Успешный ответ:**

```json
{
  "id": 1,
  "companyName": "ООО Пример",
  "inn": "1234567890",
  "score": 75.5,
  "riskLevel": "LOW",
  "processingStatus": "COMPLETED",
  "priority": "MEDIUM",
  "decisionReason": "Автоматически одобрено по политике",
  "createdAt": "2024-01-15T10:30:00",
  "decisionDetails": {
    "decision": "APPROVED",
    "reason": "Выручка превышает порог",
    "appliedPolicy": "Auto Approve High Revenue"
  }
}
```

**Ошибка:**

```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/score"
}
```

## Безопасность

### JWT Аутентификация

Система использует JWT токены для аутентификации. Токены содержат:

- Username
- Роли пользователя
- Время истечения

Токены валидируются через `JwtAuthenticationFilter`.

### Security Headers

Приложение автоматически добавляет следующие заголовки безопасности:

- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `Content-Security-Policy`
- `Strict-Transport-Security`

### Rate Limiting

Система ограничивает частоту запросов:

- По умолчанию: 100 запросов в минуту на пользователя
- Настраивается через конфигурацию

При превышении лимита возвращается HTTP 429 Too Many Requests.

### Роли и права доступа

- **ADMIN** - полный доступ ко всем операциям
- **MANAGER** - управление решениями, просмотр статистики
- **ANALYST** - просмотр аналитики и статистики
- **USER** - базовый доступ к API

### CORS

Настроены разрешенные origins через конфигурацию. В production рекомендуется ограничить список разрешенных доменов.

## Мониторинг

### Health Checks

Endpoint для проверки здоровья приложения:

```
GET /actuator/health
```

Доступные проверки:

- Database connectivity
- Application status
- ML Service connectivity (опционально)

### Метрики

Endpoint для получения метрик:

```
GET /actuator/metrics
```

Prometheus метрики:

```
GET /actuator/prometheus
```

**Основные метрики:**

- HTTP запросы (количество, время ответа, ошибки)
- Количество скоринговых запросов
- Количество решений
- Количество политик
- Использование памяти и CPU
- Размер пула соединений с БД
- Метрики кэша
- Метрики ML сервиса (количество вызовов, время ответа)

### Логирование

Логи структурированы с использованием MDC (Mapped Diagnostic Context):

- `requestId` - идентификатор запроса для трассировки
- `companyName` - название компании
- `inn` - ИНН компании
- `operation` - тип операции
- `scoringRequestId` - ID запроса на скоринг
- `mlServiceCall` - флаг вызова ML сервиса

Логи сохраняются в:

- Консоль (структурированный JSON формат)
- Файлы в директории `logs/`:
  - `bizscore.log`
  - `bizscore-YYYY-MM-DD.N.log` (ротация по дням)

### Grafana Dashboards

Преднастроенные дашборды в Grafana:

- Обзор системы
- Метрики API
- Метрики базы данных
- Метрики кэша
- Метрики ML сервиса
- SLA мониторинг

## Разработка

### Структура проекта

```
bizscore-service/
├── src/
│   ├── main/
│   │   ├── java/com/bizscore/
│   │   │   ├── aspect/              # AOP аспекты (логирование)
│   │   │   ├── client/              # Клиенты внешних сервисов
│   │   │   │   └── impl/            # Реализации клиентов
│   │   │   ├── config/              # Конфигурация
│   │   │   ├── controller/          # REST контроллеры
│   │   │   ├── dto/                 # Data Transfer Objects
│   │   │   │   ├── request/         # DTO запросов
│   │   │   │   └── response/        # DTO ответов
│   │   │   ├── entity/              # JPA сущности
│   │   │   ├── exception/           # Исключения
│   │   │   ├── handler/             # Обработчики исключений
│   │   │   ├── mapper/              # Мапперы
│   │   │   ├── repository/          # Репозитории
│   │   │   ├── service/             # Бизнес-логика
│   │   │   └── validation/          # Валидаторы
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-prod.yml
│   │       └── logback.xml
│   └── test/
│       ├── java/com/bizscore/
│       └── resources/
│           └── application-test.yml
├── docker-compose.yml
├── Dockerfile
├── pom.xml
└── README.md
```

### Запуск тестов

```bash
# Все тесты
mvn test

# Конкретный тест
mvn test -Dtest=ScoringServiceTest

# С покрытием кода
mvn test jacoco:report

# Просмотр отчета о покрытии
open target/site/jacoco/index.html
```

### Локальная разработка

1. Запустите PostgreSQL:

```bash
docker-compose up -d postgres
```

2. Запустите ML сервис (см. документацию ML сервиса)

3. Запустите приложение с профилем для разработки:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=default
```

4. Используйте Swagger UI для тестирования API:

```bash
open http://localhost:8080/swagger-ui.html
```

### Code Style

Проект использует стандартные Java conventions. Рекомендуется использовать:

- Java 21 features
- Lombok для уменьшения boilerplate
- Spring Boot best practices
- RESTful API design

## Развертывание

### Docker

Для развертывания с Docker:

```bash
# Сборка образа
docker build -t bizscore-service:latest .

# Запуск контейнера
docker run -d \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/bizscoredb \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=password \
  -e ML_SERVICE_URL=http://ml-service:8000 \
  -e JWT_SECRET=your-secret-key \
  bizscore-service:latest
```

### Docker Compose

Для развертывания с Docker Compose:

```bash
# Сборка и запуск всех сервисов
docker-compose up -d --build

# Проверка статуса
docker-compose ps

# Просмотр логов
docker-compose logs -f

# Остановка
docker-compose down
```

### Kubernetes

Для развертывания в Kubernetes используйте файлы в директории `k8s/`:

```bash
# Применение конфигурации
kubectl apply -f k8s/deployment.yml

# Проверка статуса
kubectl get pods -l app=bizscore-service

# Просмотр логов
kubectl logs -f deployment/bizscore-service
```