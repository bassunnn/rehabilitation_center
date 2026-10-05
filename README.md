# Реабилитационный центр

Учебное консольное приложение по курсу «Программирование корпоративных систем».

## Предметная область

В приложении связаны четыре сущности:

- `Patient` — пациент, `PatientStatus`;
- `Therapist` — специалист центра, `TherapistSpecialization`;
- `Procedure` — процедура пациента, `ProcedureStatus`;
- `RehabilitationPlan` — план реабилитации, `PlanStatus`.

Связи `procedures` и `rehabilitation_plans` ссылаются на пациента и специалиста через внешние ключи.

## Запуск

1. Создайте базу PostgreSQL `rehabilitation_center`.
2. Выполните `database.sql` пользователем с правом создания таблиц.
3. При необходимости задайте параметры подключения свойствами JVM:
   `-Ddb.url=... -Ddb.user=... -Ddb.password=...`.
4. Запустите Maven-проект. Главный класс — `ru.mirea.project.Main`.

Меню содержит CRUD, сортировку ASC/DESC, фильтры, CSV-экспорт и сводную статистику.

## Требования

- JDK 21 или новее (класс собирается с `--release 21`). Запуск на JRE 8 приведёт к ошибке
  `No compiler is provided in this environment`.
- Maven 3.9+.
- PostgreSQL 14+.

## Схема базы данных

Схема в `database.sql` должна полностью соответствовать Java-моделям. Существуют четыре таблицы,
связанные внешними ключами:

| Таблица               | Столбцы                                                                 |
| --------------------- | ----------------------------------------------------------------------- |
| `patients`            | `id`, `first_name`, `last_name`, `phone`, `email`, `birth_date`, `status` |
| `therapists`          | `id`, `full_name`, `specialization`, `phone`, `email`, `hire_date`        |
| `procedures`          | `id`, `patient_id`, `therapist_id`, `name`, `description`, `procedure_date`, `status` |
| `rehabilitation_plans`| `id`, `patient_id`, `therapist_id`, `title`, `goal`, `start_date`, `end_date`, `status` |

Если приложение падает с ошибками вида `столбец "status" не существует`,
`столбец "therapist_id" не существует` или `отношение "therapists" не существует`,
значит в базе лежит устаревшая схема. Приведите базу в соответствие:

```powershell
$env:PGPASSWORD = "<пароль>"
& "C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -h localhost -d rehabilitation_center -v ON_ERROR_STOP=1 -f database.sql
```

Скрипт идемпотентен: он пересоздаёт таблицы и заново загружает тестовые данные.

## Диагностика проблем

- `No compiler is provided in this environment` — запущен JRE вместо JDK. Укажите JDK 21+,
  например задав `JAVA_HOME` перед сборкой.
- Ошибки подключения — проверьте параметры `-Ddb.url`, `-Ddb.user`, `-Ddb.password`.
- Параметры подключения по умолчанию: `jdbc:postgresql://localhost:5432/rehabilitation_center`,
  пользователь `postgres`.
