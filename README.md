# Информационная система Реабилитационного центра

> **Контрольная работа №3.** Разработка backend и frontend информационной системы.  
> Проект выполнен на базе архитектурного эталона *Student Hub* с полной адаптацией под предметную область **«Реабилитационный центр»**.

---

## 🏥 Предметная область и перенос концепций

В рамках системы автоматизируются процессы стационарного и амбулаторного учета пациентов реабилитационного центра, распределения по клиническим отделениям, динамической оценки показателей реабилитационного потенциала, фиксации назначений и ведения аудита действий персонала.

| Концепция Student Hub | Реализация в Реабилитационном центре | Назначение |
|---|---|---|
| `Student` | `Patient` (Пациент) | Основная сущность: медкарта, ФИО, контакты, дата рождения, статус, показатели, теги, версия. |
| `StudyGroup` | `Department` (Отделение) | Справочная сущность: код отделения, наименование, вместимость (коек), статус активности. |
| `StudentStatus` | `PatientStatus` | Статусы: `UNDER_OBSERVATION` (на наблюдении), `IN_REHABILITATION` (в процессе), `COMPLETED` (курс завершен), `DISCHARGED` (выписан). |
| `RiskLevel` | `RiskLevel` | Расчетный клинический риск ухода: `NOT_APPLICABLE`, `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` (вычисляется сервисом `RiskCalculator` на базе шкалы реабилитации и признака потребности в уходе). |
| `averageGrade` | `rehabilitationScore` | Числовой показатель: шкала реабилитационного потенциала (0.00 – 100.00%). |
| `hasDebt` | `requiresAssistance` | Логический признак: нуждается в постороннем уходе / сопровождении. |
| `student_tag` | `patient_tag` | Теги клинического профиля («ЛФК», «Кардио», «Неврология», «Инсульт», «Травма»). |
| `StudentNote` | `PatientNote` | Медицинские заметки, анамнез и назначения в карточке пациента с возможностью закрепления (`pinned`). |
| Роли доступа | `ROLE_VIEWER`, `ROLE_DOCTOR`, `ROLE_ADMIN` | Уровни доступа: Наблюдатель (только чтение), Врач (CRUD пациентов и заметок), Администратор (полный доступ, справочники, пользователи, аудит). |
| `UserAccount` | `UserAccount` | Учетные записи медицинского персонала с политикой стойких паролей BCrypt и флагом обязательной смены пароля. |
| `RefreshSession` | `RefreshSession` | Серверная сессия обновления JWT, привязанная к хэшу refresh-токена в HttpOnly Cookie. |
| `AuditEvent` | `AuditEvent` | Журнал аудита с фиксацией IP, User-Agent, типа события и сквозного `Correlation ID`. |
| `AnalyticsResponse` | `AnalyticsResponse` | Сводные KPI и агрегированные распределения для аналитической панели. |

---

## 🛠 Технологический стек

- **Backend:** Java 21, Spring Boot 3.5.5 (Spring MVC, Spring Data JPA, Spring Security, Validation)
- **База данных:** PostgreSQL 17 (драйвер JDBC 42.7.x, миграции Flyway)
- **Безопасность:** Spring Security 6, JJWT 0.12.6, BCrypt, HttpOnly/SameSite Cookie, защита от подбора паролей (`LoginAttemptService`)
- **Документация API:** Springdoc OpenAPI 2.8.9, Swagger UI
- **CSV & Экспорт:** Apache Commons CSV 1.14.1 с защитой от Formula Injection (`=`, `+`, `-`, `@`, `\t`, `\r`)
- **Frontend:** Современный компонентный Vanilla JS (SPA), HTML5 Dialog API, CSS Grid/Flexbox
- **Тестирование:** JUnit 5, Spring Boot Test, H2 Database (in-memory для изолированных тестов)
- **Контейнеризация:** Docker Compose, Dockerfile / официальный образ Maven + Temurin 21

---

## 📁 Структура проекта

```text
rehabilitation_center/
├── pom.xml                     # Корневой Maven Aggregator POM (backend + javafx-client)
├── .env                        # Переменные окружения для локального запуска и Docker
├── .env.example                # Шаблон конфигурации окружения
├── docker-compose.yml          # Запуск всей системы через Docker Compose
├── start-compose.ps1           # Скрипт быстрого запуска системы
│
├── backend/                    # Spring Boot REST API & Web UI (КР №3)
│   ├── pom.xml                 # Спецификация сборки backend
│   ├── docker-compose.yml      # Контейнеризация PostgreSQL 17 + Spring Boot
│   ├── start-compose.ps1       # Скрипт запуска backend с контролем готовности
│   ├── stop-compose.ps1        # Остановка контейнеров backend
│   ├── verify-compose.ps1      # Верификация конфигурации compose
│   ├── init-env.ps1            # Генератор криптографических секретов и .env
│   └── src/
│       ├── main/
│       │   ├── java/ru/mirea/rehab/
│       │   │   ├── config/     # Spring Security, OpenAPI, AppProperties, BootstrapAdmin
│       │   │   ├── controller/ # REST контроллеры (/api/patients, /api/auth, /api/departments и др.)
│       │   │   ├── domain/     # JPA сущности и перечисления (Patient, Department, UserAccount...)
│       │   │   ├── dto/        # DTO входных и выходных данных, валидация
│       │   │   ├── error/      # GlobalExceptionHandler, ProblemDetails, AppErrorController
│       │   │   ├── filter/     # SmartFilterParser, PatientSpecifications (JPA Criteria)
│       │   │   ├── repository/ # Spring Data JPA репозитории
│       │   │   ├── security/   # JwtService, JwtFilter, PasswordPolicy, CurrentUserService
│       │   │   ├── service/    # Бизнес-логика, транзакции, аудит, расчет рисков, CSV
│       │   │   ├── web/        # CorrelationIdFilter, PageableFactory
│       │   │   └── RehabilitationCenterApplication.java
│       │   └── resources/
│       │       ├── application.yml         # Настройки приложения
│       │       ├── application-test.yml    # Настройки для unit-тестов
│       │       ├── db/migration/           # SQL миграции Flyway (V1__initial_schema.sql)
│       │       └── static/                 # Веб-клиент (index.html, app.js, styles.css)
│       └── test/java/ru/mirea/rehab/       # Автоматические тесты (10 тестов)
│
└── javafx-client/              # Настольный JavaFX-клиент (КР №2)
    ├── pom.xml
    ├── src/
    └── run.ps1
```

---

## 🚀 Быстрый запуск готового проекта

### 1. Запуск через Docker Compose (рекомендуемый способ)

1. Запустите **Docker Desktop**.
2. Откройте PowerShell в корне проекта:
```powershell
powershell -ExecutionPolicy Bypass -File .\backend\start-compose.ps1
```
Либо прямой командой:
```powershell
docker compose --env-file .env -f backend/docker-compose.yml up -d --wait
```

### 2. Запуск backend без Docker (локально через Maven)

1. Убедитесь, что запущен PostgreSQL на порту `5432` с базой `rehabilitation_center` (или используйте профиль с PostgreSQL).
2. Выполните сборку и запуск:
```powershell
cd backend
mvn spring-boot:run
```

---

## 🌐 Сетевые адреса системы

| Назначение | Адрес |
|---|---|
| **Web-интерфейс** | [http://localhost:8080/](http://localhost:8080/) |
| **Swagger UI** | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) |
| **REST API** | `http://localhost:8080/api` |
| **OpenAPI спецификация (JSON)** | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) |
| **Health Check (Actuator)** | [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health) |
| **PostgreSQL JDBC** | `jdbc:postgresql://localhost:55432/rehabilitation_center` |

---

## 🔑 Учетные данные по умолчанию

| Параметр | Значение |
|---|---|
| **Логин администратора** | `admin@example.org` |
| **Временный пароль администратора** | `Adm1!750DF9E52689A0B47E4DE85BC5C8AAC0` |
| **База данных** | `rehabilitation_center` |
| **Пользователь БД** | `rehab_app` |
| **Пароль БД** | `Db1!D90F176A0758B82E72A92FC9BE3F8652` |

> ⚠️ **Важно:** При первом входе система в соответствии с регламентом безопасности потребует сменить временный пароль администратора на постоянный (не менее 12 символов, содержащий заглавные и строчные буквы, цифру и специальный символ).

---

## 🧪 Запуск автоматических тестов

Для запуска всех тестов обоих модулей выполните:
```powershell
mvn test
```
- **Backend:** 10 тестов (контекст Spring, расчет клинических рисков, политика паролей, умный парсер фильтров без SQL-конкатенации).
- **JavaFX Client:** 11 тестов (валидация, экспорт, бизнес-правила).
- **Итого:** 21 тест — все успешно проходят (`BUILD SUCCESS`).

---

## 🛡 Чек-лист защиты проекта (16 пунктов)

Ниже приведено подробное руководство для ответов на вопросы и демонстрации кода преподавателю:

### 1. Авторизация и JWT
- **Где в коде:** [JwtService.java](file:///c:/Users/lobin/OneDrive/Рабочий%20стол/Repositories/rehabilitation_center/backend/src/main/java/ru/mirea/rehab/security/JwtService.java), [AuthController.java](file:///c:/Users/lobin/OneDrive/Рабочий%20стол/Repositories/rehabilitation_center/backend/src/main/java/ru/mirea/rehab/controller/AuthController.java), [AuthService.java](file:///c:/Users/lobin/OneDrive/Рабочий%20стол/Repositories/rehabilitation_center/backend/src/main/java/ru/mirea/rehab/service/AuthService.java).
- **Как устроено:** Короткоживущий Access JWT (15 минут) передается в теле ответа и хранится только в оперативной памяти вкладки. Refresh-токен хранится в защищенной `HttpOnly`, `SameSite=Lax` Cookie. На сервере сессия отслеживается в таблице `refresh_session` по SHA-256 хэшу токена. Ротация сессии происходит при каждом обновлении токена.

### 2. Разграничение доступа (RBAC)
- **Где в коде:** [Role.java](file:///c:/Users/lobin/OneDrive/Рабочий%20стол/Repositories/rehabilitation_center/backend/src/main/java/ru/mirea/rehab/domain/Role.java), [SecurityConfig.java](file:///c:/Users/lobin/OneDrive/Рабочий%20стол/Repositories/rehabilitation_center/backend/src/main/java/ru/mirea/rehab/config/SecurityConfig.java), контроллеры с `@PreAuthorize`.
- **Роли:**
  - `ROLE_VIEWER`: просмотр карточек пациентов, отделений и дашборда.
  - `ROLE_DOCTOR`: создание, редактирование пациентов, добавление заметок, экспорт/импорт CSV.
  - `ROLE_ADMIN`: управление отделениями, пользователями, сброс паролей, просмотр журнала аудита.

### 3. Защита от брутфорса и политика паролей
- **Где в коде:** [LoginAttemptService.java](file:///c:/Users/lobin/OneDrive/Рабочий%20стол/Repositories/rehabilitation_center/backend/src/main/java/ru/mirea/rehab/service/LoginAttemptService.java), [PasswordPolicy.java](file:///c:/Users/lobin/OneDrive/Рабочий%20стол/Repositories/rehabilitation_center/backend/src/main/java/ru/mirea/rehab/security/PasswordPolicy.java).
- **Как устроено:** Хэширование BCrypt (сила 12). Ограничение на 5 неудачных попыток входа с временной блокировкой на 15 минут. Пароли проверяются регулярным выражением на длину >= 12, наличие цифр, разных регистров и спецсимволов. Флаг `passwordChangeRequired` блокирует любые операции до смены временного пароля.

### 4. Основная сущность (Пациент) и валидация
- **Где в коде:** [Patient.java](file:///c:/Users/lobin/OneDrive/Рабочий%20стол/Repositories/rehabilitation_center/backend/src/main/java/ru/mirea/rehab/domain/Patient.java), [PatientDtos.java](file:///c:/Users/lobin/OneDrive/Рабочий%20стол/Repositories/rehabilitation_center/backend/src/main/java/ru/mirea/rehab/dto/PatientDtos.java).
- **Поля:** UUID id, внешний код (номер медкарты), ФИО, email, телефон (+7-999-999-99-99), дата рождения, отделение, статус, шкала реабилитации (0-100), признак ухода, расчетный риск, теги, version, archived.
- **Валидация:** Bean Validation (`@NotBlank`, `@Pattern`, `@DecimalMin`, `@DecimalMax`, `@Past`).

### 5. Справочная сущность (Отделение)
- **Где в коде:** [Department.java](file:///c:/Users/lobin/OneDrive/Рабочий%20стол/Repositories/rehabilitation_center/backend/src/main/java/ru/mirea/rehab/domain/Department.java), [DepartmentService.java](file:///c:/Users/lobin/OneDrive/Рабочий%20стол/Repositories/rehabilitation_center/backend/src/main/java/ru/mirea/rehab/service/DepartmentService.java).
- **Особенности:** Код отделения, наименование, коечный фонд, признак `active`. Реализовано мягкое включение/отключение без удаления связанных пациентов.

### 6. Умный поиск и фильтрация без конкатенации SQL
- **Где в коде:** [SmartFilterParser.java](file:///c:/Users/lobin/OneDrive/Рабочий%20стол/Repositories/rehabilitation_center/backend/src/main/java/ru/mirea/rehab/filter/SmartFilterParser.java), [PatientSpecifications.java](file:///c:/Users/lobin/OneDrive/Рабочий%20стол/Repositories/rehabilitation_center/backend/src/main/java/ru/mirea/rehab/filter/PatientSpecifications.java).
- **Безопасность:** Запросы собираются строго через JPA Criteria API (`CriteriaBuilder`, `Predicate`). Никаких SQL/JPQL конкатенаций пользовательских строк. Поддерживается умный синтаксис: `отделение:НЕВРО статус:реабилитация балл>=70 помощь:да риск:высокий тег:инсульт архив:все`.

### 7. Сортировка и серверная пагинация
- **Где в коде:** [PageableFactory.java](file:///c:/Users/lobin/OneDrive/Рабочий%20стол/Repositories/rehabilitation_center/backend/src/main/java/ru/mirea/rehab/web/PageableFactory.java), [PatientController.java](file:///c:/Users/lobin/OneDrive/Рабочий%20стол/Repositories/rehabilitation_center/backend/src/main/java/ru/mirea/rehab/controller/PatientController.java).
- **Особенности:** Разрешенный whitelist полей сортировки (`name`, `score`, `status`, `created`, `risk`). Ограничение максимального размера страницы (`maxPageSize = 100`).

### 8. Мягкое удаление (Архив) и восстановление
- **Где в коде:** [PatientService.java](file:///c:/Users/lobin/OneDrive/Рабочий%20стол/Repositories/rehabilitation_center/backend/src/main/java/ru/mirea/rehab/service/PatientService.java).
- **Поведение:** По умолчанию удаление переводит пациента в архив (`archived = true`, `archivedAt = now`). Доступен метод восстановления (`/restore`). Физическое удаление (`/permanent`) разрешено только администратору при явном включении опции `PERMANENT_DELETE_ENABLED=true`.

### 9. Контроль конкурентного изменения (Optimistic Locking)
- **Где в коде:** Поле `@Version private Long version;` в [AuditedVersionedEntity.java](file:///c:/Users/lobin/OneDrive/Рабочий%20стол/Repositories/rehabilitation_center/backend/src/main/java/ru/mirea/rehab/domain/AuditedVersionedEntity.java).
- **Поведение:** При обновлении сущности проверяется переданная клиентом версия. Если запись изменена другим пользователем, выбрасывается `ConflictException` (HTTP 409) с пояснением.

### 10. Дочерние медицинские заметки
- **Где в коде:** [PatientNote.java](file:///c:/Users/lobin/OneDrive/Рабочий%20стол/Repositories/rehabilitation_center/backend/src/main/java/ru/mirea/rehab/domain/PatientNote.java), [NoteController.java](file:///c:/Users/lobin/OneDrive/Рабочий%20стол/Repositories/rehabilitation_center/backend/src/main/java/ru/mirea/rehab/controller/NoteController.java), [NoteService.java](file:///c:/Users/lobin/OneDrive/Рабочий%20стол/Repositories/rehabilitation_center/backend/src/main/java/ru/mirea/rehab/service/NoteService.java).
- **Особенности:** Привязка к пациенту по FK, имя автора из JWT-токена, отметка времени, флаг закрепления (`pinned`).

### 11. Массовые операции (Bulk Actions)
- **Где в коде:** [PatientController.java](file:///c:/Users/lobin/OneDrive/Рабочий%20стол/Repositories/rehabilitation_center/backend/src/main/java/ru/mirea/rehab/controller/PatientController.java), [PatientService.java](file:///c:/Users/lobin/OneDrive/Рабочий%20стол/Repositories/rehabilitation_center/backend/src/main/java/ru/mirea/rehab/service/PatientService.java).
- **Действия:** `ARCHIVE`, `RESTORE`, `SET_STATUS`, `ADD_TAG`, `REMOVE_TAG` для списка выбранных UUID пациентов с возвратом отчета об успешных и ошибочных строках.

### 12. CSV импорт и экспорт
- **Где в коде:** [CsvPatientService.java](file:///c:/Users/lobin/OneDrive/Рабочий%20стол/Repositories/rehabilitation_center/backend/src/main/java/ru/mirea/rehab/service/CsvPatientService.java).
- **Безопасность:** Санитизация опасных префиксов Excel-формул (`=`, `+`, `-`, `@`, табуляция). Предварительный просмотр (`/import/preview`) с валидацией без сохранения в БД. Режимы `CREATE_ONLY` и `UPSERT_BY_EXTERNAL_ID`.

### 13. Аналитический дашборд
- **Где в коде:** [AnalyticsController.java](file:///c:/Users/lobin/OneDrive/Рабочий%20стол/Repositories/rehabilitation_center/backend/src/main/java/ru/mirea/rehab/controller/AnalyticsController.java), [AnalyticsService.java](file:///c:/Users/lobin/OneDrive/Рабочий%20стол/Repositories/rehabilitation_center/backend/src/main/java/ru/mirea/rehab/service/AnalyticsService.java).
- **Показатели:** Всего пациентов, активных, архивных, нуждающихся в уходе, средний балл реабилитации, группировки по отделениям, статусам и рискам.

### 14. Журнал аудита и Correlation ID
- **Где в коде:** [CorrelationIdFilter.java](file:///c:/Users/lobin/OneDrive/Рабочий%20стол/Repositories/rehabilitation_center/backend/src/main/java/ru/mirea/rehab/web/CorrelationIdFilter.java), [AuditService.java](file:///c:/Users/lobin/OneDrive/Рабочий%20стол/Repositories/rehabilitation_center/backend/src/main/java/ru/mirea/rehab/service/AuditService.java), [AuditController.java](file:///c:/Users/lobin/OneDrive/Рабочий%20стол/Repositories/rehabilitation_center/backend/src/main/java/ru/mirea/rehab/controller/AuditController.java).
- **Особенности:** Каждому HTTP-запросу присваивается сквозной `X-Correlation-ID`. Критические действия (вход, создание/редактирование пациента, смена пароля, создание пользователя) логируются с фиксацией автора, IP, User-Agent и correlation ID.

### 15. Документация OpenAPI / Swagger
- **Где в коде:** [OpenApiConfig.java](file:///c:/Users/lobin/OneDrive/Рабочий%20стол/Repositories/rehabilitation_center/backend/src/main/java/ru/mirea/rehab/config/OpenApiConfig.java).
- **Доступность:** Swagger UI по адресу `/swagger-ui.html` защищен и доступен авторизованным пользователям с ролью `ROLE_ADMIN`.

### 16. Контейнеризация и Health Check
- **Где в коде:** [docker-compose.yml](file:///c:/Users/lobin/OneDrive/Рабочий%20стол/Repositories/rehabilitation_center/backend/docker-compose.yml), [start-compose.ps1](file:///c:/Users/lobin/OneDrive/Рабочий%20стол/Repositories/rehabilitation_center/backend/start-compose.ps1).
- **Особенности:** Проверка готовности БД (`pg_isready`), проверка старта бэкенда по сокету (`echo > /dev/tcp/127.0.0.1/8080`), сохранение данных в named volume `rehab_postgres`.
