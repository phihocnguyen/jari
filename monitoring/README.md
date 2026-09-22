# ELK Stack — Monitoring cho Jari Backend

Bộ file này triển khai Elasticsearch + Logstash + Kibana + Filebeat để thu thập và trực quan hoá log của toàn bộ backend service.

## 1. Chạy stack

```bash
cd monitoring
docker compose up -d
```

| Service | URL | Ghi chú |
|---|---|---|
| Kibana | http://localhost:5601 | UI xem log, tạo dashboard |
| Elasticsearch | http://localhost:9200 | REST API (đã tắt security cho dev) |
| Logstash | :5044 | Nhận log từ Filebeat (beats input) |
| Filebeat | — | Tự động theo dõi log của mọi container trên máy |

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
      - /path/to/jari/app.log   # logback: đổi appender sang RollingFileAppender
```

## 4. Xem log trong Kibana

1. Mở http://localhost:5601 → **Management → Stack Management → Data Views**.
2. Create data view với pattern `jari-logs-*`, chọn `@timestamp` làm time field.
3. Vào **Discover** để xem log, filter theo `level`, `logger_name`, `service`.
4. Có thể tạo **Dashboard** (số lỗi 5xx, log ERROR theo giờ...) từ Discover → Save.

## 5. Indexer service — đồng bộ DB vào Elasticsearch

Ngoài thu log, Logstash còn chạy **indexer pipeline** (`logstash/pipeline/indexer.conf`) poll PostgreSQL và index dữ liệu sang ES để search/filter mà không phải query DB:

| Index | Nội dung | Poll |
|---|---|---|
| `jari-issues` | Issue denormalized sẵn: project, workspace, status + category, priority, type, reporter, assignee, sprint, position | 30s |
| `jari-projects` | Project + workspace, lead, member_count, issue_count | 60s |
| `jari-users` | User (không có password_hash) | 60s |

Cách hoạt động:
- Incremental sync qua `updated_at` (`:sql_last_value`), chỉ kéo bản ghi mới/thay đổi; `_id` = id bản ghi DB nên cập nhật là đè lên bản cũ, không duplicate.
- Index `jari-issues` có mapping tường minh do template `elasticsearch/templates/jari-issues.json` (service `elasticsearch-init` cài tự động khi `docker compose up`): keyword cho field filter, text cho field search, date/number cho sort.
- Logstash dùng image custom ([logstash/Dockerfile](logstash/Dockerfile)) có sẵn PostgreSQL JDBC driver.
- Kết nối DB cấu hình qua `monitoring/.env` (`JARI_JDBC_URL`, `JARI_DB_USER`, `JARI_DB_PASS`) — file này đã được gitignore, thay bằng DB của môi trường thực tế khi deploy.

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

## 7. Notes

- Hiện tại `xpack.security.enabled=false` cho môi trường dev. **Trước khi lên prod bắt buộc bật security + TLS** (password cho ES, api_key cho Logstash/Filebeat/Kibana).
- Dữ liệu index theo ngày (`jari-logs-YYYY.MM.dd`). Nên cấu hình ILM retention khi lên prod để không phình disk (Stack Management → Index Lifecycle Policies, ví dụ xoá sau 30 ngày).
- Elasticsearch chiếm ~1GB RAM với cấu hình heap 512m trong compose; chỉnh `ES_JAVA_OPTS` theo máy.
