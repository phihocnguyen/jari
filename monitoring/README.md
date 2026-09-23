# ELK Stack — Monitoring cho Jari Backend

Bộ file này triển khai Elasticsearch + Logstash + Kibana + Filebeat để thu thập và trực quan hoá log của toàn bộ backend service.

## 1. Chạy stack

```bash
cd monitoring
docker compose up -d
```

| Service       | URL                   | Ghi chú                                              |
| ------------- | --------------------- | ---------------------------------------------------- |
| **Grafana**   | http://localhost:3001 | Metrics API (request count, latency) — **giờ +7**    |
| **Prometheus**| http://localhost:9090 | Engine metrics, PromQL — **luôn UTC (+0)**           |
| **Kibana**    | http://localhost:5601 | **Error logs**, request logs — giờ theo browser/Kibana |
| Elasticsearch | http://localhost:9200 | REST API (dev: security tắt)                         |
| Logstash      | :5044                 | Nhận log từ Filebeat                                 |
| Filebeat      | —                     | Thu log container trên máy                           |

Đổi phiên bản qua file `.env` hoặc biến môi trường, ví dụ `ELK_VERSION=9.1.2`.

## 2. Bật log JSON cho backend (bắt buộc để log vào ELK gọn gàng)

Thêm dependency vào `pom.xml`:

```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>8.1</version>
</dependency>
```

Tạo `src/main/resources/logback-spring.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProfile name="local">
        <!-- Định dạng console bình thường khi chạy local -->
        <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
        <include resource="org/springframework/boot/logging/logback/console-appender.xml"/>
        <root level="INFO"><appender-ref ref="CONSOLE"/></root>
    </springProfile>

    <springProfile name="docker,prod">
        <!-- JSON ra stdout, Filebeat sẽ parse -->
        <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LogstashEncoder">
                <customFields>{"service_name":"jari"}</customFields>
                <fieldNames>
                    <timestamp>@timestamp</timestamp>
                    <version>[ignore]</version>
                </fieldNames>
            </encoder>
        </appender>
        <root level="INFO"><appender-ref ref="JSON_CONSOLE"/></root>
    </springProfile>
</configuration>
```

## 3. Cho Filebeat đọc log container backend

Trong `docker-compose.yml` (hoặc file deploy) của **backend**, thêm label để Filebeat parse JSON:

```yaml
services:
  jari-backend:
    # ... image, ports, ...
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    labels:
      co.elastic.logs/json.keys_under_root: "true"
      co.elastic.logs/json.add_error_key: "true"
      co.elastic.logs/json.message_key: "message"
```

Nếu backend chạy **trực tiếp trên host** (không qua Docker), thêm input đọc file log vào `monitoring/filebeat/filebeat.yml`:

```yaml
filebeat.inputs:
  - type: filestream
    id: jari-app
    paths:
      - /path/to/jari/app.log # logback: đổi appender sang RollingFileAppender
```

## 4. Xem error logs trong Kibana

### 4.1 Chuẩn bị

1. ELK stack đang chạy: `docker compose up -d elasticsearch logstash kibana filebeat`
2. Backend log **JSON** ra stdout (Filebeat đọc qua Docker):

```bash
# Từ thư mục jari/
SPRING_PROFILES_ACTIVE=docker ./mvnw spring-boot:run
```

3. Đợi ~1 phút để log đi qua Filebeat → Logstash → index `jari-logs-YYYY.MM.dd`

### 4.2 Tạo data view (lần đầu)

1. Mở http://localhost:5601
2. **Management → Stack Management → Data Views → Create data view**
3. Name: `jari-logs`, Index pattern: `jari-logs-*`, Timestamp: `@timestamp` → Save

### 4.3 Xem error logs

1. **Analytics → Discover** → chọn data view `jari-logs`
2. Góc phải chọn **time range** (vd. Last 15 minutes, hoặc custom lúc chạy k6/benchmark)
3. Filter KQL:

```
level: "ERROR"
```

Chỉ exception từ `GlobalExceptionHandler`:

```
level: "ERROR" and message: "Unhandled exception"
```

Lỗi 5xx trên API cụ thể (log structured `http_request`):

```
message: "http_request" and status: 500
```

Hoặc:

```
message: *http_request* and message: *status=500*
```

4. Bấm vào dòng log → xem full `message`, `stack_trace` (nếu có), `logger_name`

### 4.4 Request logs (không phải error)

Mọi API request ghi dạng:

```
http_request method=GET uri=/api/v1/... status=200 duration_ms=42
```

Filter:

```
message: "http_request"
```

Filter theo endpoint:

```
message: *uri=/api/v1/projects**
```

---

## 7. Metrics benchmark — Grafana (+7) & Prometheus (UTC)

```
Backend (/actuator/prometheus) → Prometheus (lưu + PromQL) → Grafana (chart, +7)
```

### 7.1 Chạy stack metrics

```bash
cd monitoring
docker compose up -d prometheus grafana
```

Backend phải chạy trên host port **8080**. Kiểm tra target: http://localhost:9090/targets → `jari-backend` = **UP**.

### 7.2 Grafana — request count theo API, khung thời gian +7

1. Mở http://localhost:3001 — login **admin** / **admin**
2. **Dashboards → Jari → Jari API Requests**
3. Góc phải chọn **time range** trùng lúc chạy k6 (vd. `2026-09-23 15:00` → `15:10`) — hiển thị **giờ Việt Nam (+7)**
4. Các panel:

| Panel | Ý nghĩa |
| ----- | ------- |
| Request rate theo API | req/s theo từng endpoint (line chart) |
| Tổng request API | Tổng số request trong khung thời gian đã chọn |
| Số request / phút theo API | Histogram theo phút |
| **Bảng URI / Method / Status / Count** | **Số lần gọi từng API** — panel chính để đếm |
| p95 latency | Latency p95 theo API |

5. Refresh: dashboard auto 10s, hoặc bấm refresh sau khi chạy benchmark

> **Prometheus UI (9090) luôn UTC** — không đổi timezone. Chỉ dùng khi debug PromQL.

### 7.3 PromQL tham khảo (Prometheus UI, UTC)

**Tổng request theo API trong 15 phút:**

```promql
sum by (uri, method, status) (
  increase(http_server_requests_seconds_count{job="jari-backend", uri=~"/api/v1/.*"}[15m])
)
```

**Request/s theo thời gian:**

```promql
sum by (uri, method) (
  rate(http_server_requests_seconds_count{job="jari-backend", uri=~"/api/v1/.*"}[1m])
)
```

**Số lỗi 5xx (metrics, không phải log text):**

```promql
sum by (uri, method) (
  increase(http_server_requests_seconds_count{job="jari-backend", status=~"5..", uri=~"/api/v1/.*"}[15m])
)
```

Recording rules (`prometheus/recording_rules.yml`):

| Metric | Ý nghĩa |
| ------ | ------- |
| `jari:http_request_duration_seconds:p95` | p95 latency theo uri/method |
| `jari:http_request_duration_seconds:avg` | avg latency theo uri/method |
| `jari:http_requests:rate5m` | req/s theo uri/method/status |
| `jari:http_requests:increase5m` | request tăng thêm trong 5 phút |
| `jari:cache_hit_rate_overall:5m` | **Redis cache hit rate tổng** (0–1, ×100 = %) |
| `jari:cache_hit_rate:5m` | hit rate theo từng cache (`ref:*`, `issue:*`, …) |
| `jari:cache_gets:rate5m` | cache gets/s theo `result=hit|miss` |

**Redis cache hit rate (tổng, %):**

```promql
100 * jari:cache_hit_rate_overall:5m
```

**Hit rate theo cache name:**

```promql
100 * jari:cache_hit_rate:5m
```

**Hit vs miss / s:**

```promql
sum by (cache, result) (jari:cache_gets:rate5m)
```

> Cần `spring.cache.redis.enable-statistics: true` + `RedisCacheManager.enableStatistics()` (đã cấu hình trong backend). Sau restart app, panel **Redis cache hit rate** trên Grafana dashboard sẽ có dữ liệu khi có traffic.

### 7.4 Workflow sau k6 benchmark

```bash
# Terminal 1 — backend
SPRING_PROFILES_ACTIVE=docker ./mvnw spring-boot:run

# Terminal 2 — metrics
cd monitoring && docker compose up -d prometheus grafana

# Terminal 3 — benchmark (ghi nhớ giờ bắt đầu/kết thúc)
cd k6 && ./run.sh load.js
```

Sau benchmark:

- **Grafana** http://localhost:3001 → set time range → xem bảng **Count theo API** + latency + **Redis cache hit rate**
- **Kibana** http://localhost:5601 → `level: ERROR` hoặc `status: 500` → xem stack trace

---

## 5. Indexer service — đồng bộ DB vào Elasticsearch

Ngoài thu log, Logstash còn chạy **indexer pipeline** (`logstash/pipeline/indexer.conf`) poll PostgreSQL và index dữ liệu sang ES để search/filter mà không phải query DB:

| Index           | Nội dung                                                                                                            | Poll |
| --------------- | ------------------------------------------------------------------------------------------------------------------- | ---- |
| `jari-issues`   | Issue denormalized sẵn: project, workspace, status + category, priority, type, reporter, assignee, sprint, position | 30s  |
| `jari-projects` | Project + workspace, lead, member_count, issue_count                                                                | 60s  |
| `jari-users`    | User (không có password_hash)                                                                                       | 60s  |

Cách hoạt động:

- Incremental sync qua `updated_at` (`:sql_last_value`), chỉ kéo bản ghi mới/thay đổi; `_id` = id bản ghi DB nên cập nhật là đè lên bản cũ, không duplicate.
- Index `jari-issues` có mapping tường minh do template `elasticsearch/templates/jari-issues.json` (service `elasticsearch-init` cài tự động khi `docker compose up`): keyword cho field filter, text cho field search, date/number cho sort.
- Logstash dùng image custom ([logstash/Dockerfile](logstash/Dockerfile)) có sẵn PostgreSQL JDBC driver.
- Kết nối DB cấu hình qua `monitoring/.env` (`JARI_JDBC_URL`, `JARI_DB_USER`, `JARI_DB_PASS`) — copy từ `monitoring/.env.example` (Postgres local qua Docker).

Lưu ý:

- Pipeline **không xử lý delete**: bản ghi xoá cứng trong DB sẽ còn sót trong ES (issue xoá rồi vẫn có thể còn hiện trong search tối đa cho đến khi index lại). Nếu cần đồng bộ delete, thêm script chạy `DELETE BY QUERY` định kỳ.

## 6. Backend query từ Elasticsearch (write-through)

Backend truy vấn ES làm đường chính cho list issue, và **ghi vào ES ngay khi dữ liệu thay đổi** (write-through):

- `IssueIndexService` — được `IndexerConsumer` gọi sau khi nhận message từ RabbitMQ. Search thấy kết quả gần như tức thời, không phải đợi poll.
- Issue create/update/delete/reorder/sprint đổi (IssueService + AutomationService) → bắn `IssueIndexEvent`.
- **Đổi tên project** → `ProjectIndexChangedEvent` → reindex toàn bộ issue của project.
- **User đổi displayName** → `UserIndexChangedEvent` → reindex các issue mà user là assignee/reporter.
- Logstash indexer poll 30s vẫn giữ nguyên làm **safety net**: hàn gắn các bản ghi miss khi ES down lúc commit hoặc bulk update ngoài luồng event.
- Lỗi khi index vào ES **không làm lỗi API** — chỉ log warn `Failed to index issue...`, dữ liệu sẽ được poll bắt lại sau.
- Đường đọc: `IssueSearchService` filter trên ES → ES trả ID → DB hydrate entity theo ID (PK lookup) cho response đầy đủ. ES lỗi → fallback JPA Specification tự động.
- Kết nối: `spring.elasticsearch.uris` trong `application.yaml` (default `http://localhost:9200`; backend chạy trong Docker cùng network `jari-monitoring` thì set `ELASTICSEARCH_URIS=http://elasticsearch:9200`).
- CRUD vẫn ghi vào Postgres như trước — Postgres vẫn là source of truth.

## 9. Notes

- Hiện tại `xpack.security.enabled=false` cho môi trường dev. **Trước khi lên prod bắt buộc bật security + TLS** (password cho ES, api_key cho Logstash/Filebeat/Kibana).
- Dữ liệu index theo ngày (`jari-logs-YYYY.MM.dd`). Nên cấu hình ILM retention khi lên prod để không phình disk (Stack Management → Index Lifecycle Policies, ví dụ xoá sau 30 ngày).
- Elasticsearch chiếm ~1GB RAM với cấu hình heap 512m trong compose; chỉnh `ES_JAVA_OPTS` theo máy.
- Logstash indexer: set `JARI_JDBC_URL=jdbc:postgresql://host.docker.internal:5432/jari` trong `monitoring/.env`.
