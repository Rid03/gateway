API Gateway (Spring Cloud, WebFlux, Redis, Keycloak)

Описание

Это реактивный API Gateway на базе Spring Cloud Gateway для работы с микросервисами.
Он включает базовую конфигурацию:
	•	JWT-аутентификация через Keycloak, с извлечением ролей (realm_access.roles, resource_access.*.roles)
	•	Открытые и защищённые эндпоинты (например, /actuator/**, /swagger-ui/**)
	•	Rate Limiting на основе Redis (токен-бакет)
	•	Кэширование ответов GET через Redis

Некоторые функции (например, продвинутый кэш, интеграция с конкретными сервисами) будут реализованы в следующих шагах.

⸻

Требования
	•	Java 17+
	•	Gradle 8+
	•	Redis
	•	Keycloak
	•	Eureka Server (отдельный проект)

⸻

Настройка Eureka Server

Для работы с Discovery/LoadBalancer нужно поднять отдельный проект Eureka Server:

https://github.com/Rid03/eurekaServer
	1.	Склонировать репозиторий.
	2.	Запустить Spring Boot приложение (./gradlew bootRun или через IDE).
	3.	Порт по умолчанию: 8761

После запуска Eureka, наш Gateway будет подключаться к нему как клиент.

⸻

Конфигурация проекта

application.yml (пример)
``` yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway

  cloud:
    gateway:
      default-filters:
        - name: AddResponseHeader
          args:
            name: X-Gateway
            value: spring-cloud-gateway

      routes:
        # Пример маршрута (подключение сервисов будет позже)
        - id: example-service
          uri: lb://example-service
          predicates:
            - Path=/example/**
          filters:
            - name: RequestRateLimiter
              args:
                key-resolver: "#{@principalOrIpKeyResolver}"
                redis-rate-limiter.replenishRate: 5
                redis-rate-limiter.burstCapacity: 10

    loadbalancer:
      cache:
        enabled: true

  redis:
    host: localhost
    port: 6379

  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8089/realms/demo
```
Основные функции, уже реализованные
	1.	Security
	•	JWT-аутентификация через Keycloak
	•	Роли пользователей из realm_access.roles и resource_access.*.roles
	•	Открытые эндпоинты: /actuator/**, /swagger-ui/**, /v3/api-docs/**, /eureka/**
	•	Защищённые эндпоинты: anyExchange().authenticated()
	2.	Rate Limiting
	•	Реализовано через RedisRateLimiter
	•	Ключ берётся из JWT sub или IP клиента
	3.	Кэширование GET-запросов
	•	Реализовано через CacheResponseFilter
	•	Ответы GET сохраняются в Redis с TTL (по умолчанию 30 секунд)
	•	Заголовки X-Cache: HIT/MISS показывают попадание в кэш

⸻

Как запустить
	1.	Запустить Redis (порт по умолчанию 6379).
	2.	Запустить Eureka Server (инструкция выше).
	3.	Настроить Keycloak и realm (пример http://localhost:8089/realms/demo).
	4.	Собрать и запустить Gateway:
```./gradlew bootRun```
Gateway будет слушать на 8080.


