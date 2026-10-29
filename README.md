# Binance Price Service

Сервис по расписанию забирает среднюю цену торговой пары (по умолчанию `BNBUSDT`) из
[Binance `GET /api/v3/avgPrice`](https://developers.binance.com/en/docs/catalog/core-trading-spot-trading/api/rest-api/market#avg-price),
сохраняет её в PostgreSQL и отдаёт через REST: последнюю цену, историю с пагинацией, среднюю цену и тренд за интервал.

**Стек:** Java 21, Spring Boot 3.5, PostgreSQL 17, Flyway, JUnit 5, Mockito, Testcontainers, WireMock, Docker Compose.

## Быстрый старт

Нужен только Docker.

```bash
docker compose up --build
```

Поднимутся PostgreSQL и приложение на порту `8080`. Первый опрос Binance выполняется сразу после старта, дальше — каждые 30 секунд.

```bash
curl http://localhost:8080/api/v1/prices/latest
```

Остановить: `docker compose down`. Данные хранятся в томе `postgres-data`; удалить их вместе с контейнерами: `docker compose down -v`.

## Запуск из IDE

1. Поднять только базу:
   ```bash
   docker compose up -d postgres
   ```
2. Задать переменные окружения в Run Configuration (Environment variables):
   ```
   DB_USERNAME=binance;DB_PASSWORD=binance
   ```
3. Запустить `BinancePriceServiceApplication`.

Или из терминала (bash):

```bash
DB_USERNAME=binance DB_PASSWORD=binance ./mvnw spring-boot:run
```

## Тесты

```bash
./mvnw test
```

Для интеграционного теста нужен **запущенный Docker**: Testcontainers поднимает в нём PostgreSQL.

- **Unit-тесты** (без Spring и Docker): расчёт тренда, проверка интервала, сохранение цены, поведение шедулера при сбоях и rate limit.
- **Интеграционный тест** `PriceServiceIntegrationTest`: всё приложение с настоящим PostgreSQL (Testcontainers) и поддельным Binance (WireMock) — опрос → БД → REST, отсутствие дублей, работа после ошибки 500, пауза после 429.

## API

Все эндпоинты возвращают данные для символа, который опрашивает сервис (`BINANCE_SYMBOL`).

**Формат `from` / `to`:** ISO-8601 момент времени, например `2026-09-01T00:00:00Z`. Обе границы включаются: `[from, to]`.
Фильтрация идёт по `fetchedAt`. Можно указать смещение, но `+` в URL нужно кодировать: `2026-09-01T03:00:00%2B03:00`.

### 1. Последняя цена

```bash
curl http://localhost:8080/api/v1/prices/latest
```

```json
{"symbol": "BNBUSDT", "price": "748.07374220", "fetchedAt": "2026-10-29T17:12:42Z"}
```

`fetchedAt` — когда цена получена от Binance, в UTC с точностью до секунд (`YYYY-MM-DDTHH:mm:ssZ`).

### 2. Средняя цена за интервал

```bash
curl "http://localhost:8080/api/v1/prices/average?from=2026-10-29T00:00:00Z&to=2026-10-30T00:00:00Z"
```

```json
{"averagePrice": 756.44834863}
```

### 3. История с пагинацией

```bash
curl "http://localhost:8080/api/v1/prices?from=2026-10-29T00:00:00Z&to=2026-10-30T00:00:00Z&page=0&size=20"
```

```json
{
  "items": [
    {"symbol": "BNBUSDT", "price": "756.95984073", "fetchedAt": "2026-10-29T10:18:59Z"},
    {"symbol": "BNBUSDT", "price": "757.05053070", "fetchedAt": "2026-10-29T10:19:29Z"}
  ],
  "page": 0,
  "size": 20,
  "totalElements": 135,
  "hasNext": true
}
```

Записи отсортированы по `fetchedAt` по возрастанию. `page` — от 0 (по умолчанию 0), `size` — от 1 до 100 (по умолчанию 20).

### 4. Тренд за интервал

```bash
curl "http://localhost:8080/api/v1/prices/trend?from=2026-10-29T00:00:00Z&to=2026-10-30T00:00:00Z"
```

```json
{"symbol": "BNBUSDT", "start_date": "2026-10-29T00:00:00Z", "end_date": "2026-10-30T00:00:00Z", "trend": "UPWARD"}
```

Значения `trend`: `UPWARD`, `DOWNWARD`, `FLAT` (алгоритм — в разделе «Принятые решения»).

### Ошибки

Ошибки возвращаются в формате [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457) (`application/problem+json`):

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "page: must be greater than or equal to 0; size: must be less than or equal to 100",
  "instance": "/api/v1/prices"
}
```

| Код | Когда |
|-----|-------|
| 400 | Нет обязательного параметра, неверный формат даты, `from` позже `to`, `page`/`size` вне допустимых значений |
| 404 | Нет ни одной цены (для `latest`) или нет цен в интервале (для `average` и `trend`); для истории — пустая страница |
| 500 | Внутренняя ошибка; подробности только в логе сервиса |

## Переменные окружения

| Переменная | По умолчанию | Описание |
|---|---|---|
| `DB_HOST` | `localhost` | Хост PostgreSQL |
| `DB_PORT` | `5432` | Порт PostgreSQL. В docker compose — порт, на котором база доступна с хоста |
| `DB_NAME` | `binance_prices` | Имя базы |
| `DB_USERNAME` | — (**обязательна**) | Пользователь базы |
| `DB_PASSWORD` | — (**обязательна**) | Пароль базы |
| `BINANCE_BASE_URL` | `https://api.binance.com` | Адрес REST API Binance |
| `BINANCE_SYMBOL` | `BNBUSDT` | Торговая пара; формат Binance: `^[A-Z0-9_.-]{1,20}$` |
| `BINANCE_POLL_INTERVAL` | `PT30S` | Пауза между опросами, ISO-8601 duration |
| `BINANCE_CONNECT_TIMEOUT` | `PT5S` | Таймаут соединения с Binance |
| `BINANCE_READ_TIMEOUT` | `PT10S` | Таймаут ожидания ответа Binance |
| `BINANCE_POLLING_ENABLED` | `true` | `false` выключает опрос по расписанию |
| `TREND_FLAT_THRESHOLD_PERCENT` | `0.5` | Изменение цены (в % от первой цены интервала), которое ещё считается `FLAT` |
| `APP_PORT` | `8080` | Только для docker compose: порт приложения на хосте |

Значения проверяются при старте: если, например, `BINANCE_POLL_INTERVAL=abc` или порог отрицательный, приложение не запустится и укажет неверное свойство.

У `DB_USERNAME` и `DB_PASSWORD` нет значений по умолчанию в приложении, чтобы оно никогда не подключилось молча с «паролем по умолчанию».
В `docker-compose.yml` для них заданы значения `binance` / `binance` — **только для локального запуска**.
Postgres применяет пароль только при первом создании тома: после его смены нужен `docker compose down -v`.

## Принятые решения

- **Без дублей.** В таблице ограничение `UNIQUE (symbol, close_time)`, где `close_time` — время последней сделки из ответа Binance.
  Вставка идёт через `INSERT ... ON CONFLICT DO NOTHING`: если между опросами не было сделок и Binance вернул тот же `closeTime`, запись пропускается.
- **Сбой Binance не останавливает расписание.** Шедулер перехватывает любые ошибки и пишет их в лог; следующий опрос идёт по расписанию.
  `fixedDelay` гарантирует, что опросы не накладываются друг на друга. Запросы к Binance ограничены таймаутами.
- **Rate limits.** Один запрос `avgPrice` весит 2; при опросе раз в 30 секунд это далеко от лимитов Binance.
  Если Binance всё же ответил 429 или 418, опросы приостанавливаются на время из заголовка `Retry-After`, чтобы не получить бан IP.
- **Потеря данных.** `avgPrice` отдаёт только текущее значение, поэтому точку, пропущенную из-за сбоя, восстановить нельзя — сервис её не подделывает.
  Каждая цена сохраняется одним атомарным `INSERT`, вызов Binance выполняется вне транзакции.
- **Деньги без `double`.** В Java — `BigDecimal`, в БД — `NUMERIC` без ограничения точности, цена хранится ровно так, как её прислал Binance.
  Среднее считается в SQL (`ROUND(AVG(price), MAX(SCALE(price)))`), потому что `AVG` в JPQL возвращает `Double`.
  `price` отдаётся строкой, как это делает Binance, а `averagePrice` — числом, как в примере задания.
- **Тренд.** Сравниваются первая и последняя цена интервала: `FLAT`, если `|последняя − первая| ≤ первая × порог / 100`,
  иначе `UPWARD` или `DOWNWARD` по знаку изменения. Порог 0.5% по умолчанию — выбор, а не стандарт; он настраивается.
- **`start_date` / `end_date` в тренде** — это запрошенные `from` / `to` в том же формате, что и `fetchedAt`,
  а не только дата, как в примере задания: так ответ однозначно соответствует запросу.
- **`fetchedAt` и `closeTime`.** `fetchedAt` — время сервиса, `closeTime` — время Binance; из-за расхождения часов они могут отличаться на доли секунды.
  Все эндпоинты работают по `fetchedAt`.
- **Схема БД** создаётся только миграциями Flyway; Hibernate лишь проверяет соответствие (`ddl-auto: validate`).

## Структура проекта

```
src/main/java/org/limir/binancepriceservice
├── config       настройки (BinanceProperties, TrendProperties), RestClient, Clock, включение шедулера
├── client       клиент Binance и его исключения
├── scheduler    опрос Binance по расписанию, пауза при rate limit
├── service      сохранение цен, запросы к истории, расчёт тренда
├── repository   Spring Data JPA репозиторий и SQL-запросы
├── entity       сущность PriceSnapshot
├── dto          ответы API и интервал TimeRange
├── controller   REST-контроллер и обработчик ошибок
└── exception    исключения для ответов 400 и 404
src/main/resources
├── application.yaml
└── db/migration  миграции Flyway
```
