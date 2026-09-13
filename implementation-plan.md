Jari — Jira Clone: Implementation Plan
Background
Xây dựng backend RESTful API cho một Jira clone (Jari) bằng Spring Boot 4.1.1 + Java 17, tổ chức theo package-by-feature, sử dụng Flyway để quản lý schema migration PostgreSQL. Stack đã chọn trong pom.xml: JPA/Hibernate, Spring Security + OAuth2, Spring AMQP (RabbitMQ), Redis, WebSocket, Actuator, SpringDoc OpenAPI.

Open Questions
IMPORTANT

Q1 — JWT vs Session: Xác thực stateless bằng JWT (access + refresh token lưu Redis) hay stateful session? → Đề xuất: JWT vì phù hợp với SPA/mobile client.

Q2 — OAuth2 Providers: Chỉ Google, hay thêm GitHub/Microsoft?

Q3 — Notification delivery: Email notification (JavaMailSender) ngay từ đầu hay chỉ WebSocket in-app?

Q4 — Scope MVP: Có cần Board Drag-and-Drop API (kanban column ordering) ngay Phase 1 không?

Q5 — MapStruct hay thủ công: Dùng MapStruct để generate DTO mapper hay tự viết? → Đề xuất: MapStruct.

Proposed Changes
Phase 1 — Foundation & Infrastructure
[MODIFY] 
pom.xml
Thêm các dependency còn thiếu:

io.jsonwebtoken:jjwt-api/impl/jackson — JWT
org.mapstruct:mapstruct + processor — DTO mapping
com.github.f4b6a3:ulid-creator — ULID generation (thay thế random UUID cho issue_key)
org.testcontainers:postgresql + junit-jupiter — Integration test với real DB
Loại bỏ các artifact không tồn tại trong Spring Boot 4.x:

spring-boot-starter-actuator-test, spring-boot-starter-amqp-test, v.v. (Spring Boot không có -test variant cho những module này — chỉ spring-boot-starter-test là đủ)
[NEW] application.yaml (đầy đủ)
yaml

spring:
  application:
    name: jari
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/jari}
    username: ${DB_USER:jari}
    password: ${DB_PASS:jari}
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: false
  jpa:
    hibernate:
      ddl-auto: validate        # Flyway owns schema; Hibernate chỉ validate
    show-sql: false
    open-in-view: false
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USER:guest}
    password: ${RABBITMQ_PASS:guest}
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: openid,profile,email
app:
  jwt:
    secret: ${JWT_SECRET}
    access-token-expiry: 900        # 15 phút
    refresh-token-expiry: 604800    # 7 ngày
  cors:
    allowed-origins: ${CORS_ORIGINS:http://localhost:3000}
[NEW] Flyway Migration Scripts
Đặt tại src/main/resources/db/migration/

File	Nội dung
V1__create_schema.sql	Toàn bộ DDL từ schema đã cung cấp (17 tables)
V2__seed_reference_data.sql	Insert dữ liệu seed: roles (ADMIN,MEMBER,VIEWER), permissions, issue_types (EPIC,STORY,TASK,BUG,SUBTASK), statuses (TODO,IN_PROGRESS,IN_REVIEW,DONE), priorities (HIGHEST→LOWEST)
NOTE

Convention đặt tên: V{version}__{description}.sql — Flyway checksum sẽ fail nếu file đã migrate bị sửa; tạo file mới V3__... cho mọi thay đổi.

Phase 2 — Package-by-Feature Structure
Cấu trúc thư mục tổng thể:


src/main/java/com/example/jari/
├── JariApplication.java
│
├── shared/                          # Cross-cutting concerns
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── JwtConfig.java
│   │   ├── CorsConfig.java
│   │   ├── RabbitMQConfig.java
│   │   ├── RedisConfig.java
│   │   ├── WebSocketConfig.java
│   │   └── OpenApiConfig.java
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java   # @RestControllerAdvice
│   │   ├── ApiException.java
│   │   ├── ResourceNotFoundException.java
│   │   ├── ForbiddenException.java
│   │   └── ConflictException.java
│   ├── response/
│   │   ├── ApiResponse.java              # Generic wrapper {data, message, timestamp}
│   │   └── PageResponse.java
│   ├── security/
│   │   ├── JwtTokenProvider.java
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── CustomUserDetails.java
│   │   └── CustomUserDetailsService.java
│   └── util/
│       ├── SlugUtil.java                 # tạo project_key / issue_key
│       └── PaginationUtil.java
│
├── user/                            # Feature: User & Auth
│   ├── controller/
│   │   ├── AuthController.java          # POST /api/v1/auth/register, /login, /refresh, /logout
│   │   └── UserController.java          # GET /api/v1/users/me, PUT profile, avatar
│   ├── dto/
│   │   ├── RegisterRequest.java
│   │   ├── LoginRequest.java
│   │   ├── AuthResponse.java            # {accessToken, refreshToken, user}
│   │   ├── UserResponse.java
│   │   └── UpdateProfileRequest.java
│   ├── entity/
│   │   ├── User.java
│   │   └── OAuthAccount.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   └── OAuthAccountRepository.java
│   ├── service/
│   │   ├── AuthService.java
│   │   ├── UserService.java
│   │   └── TokenService.java            # Redis: lưu refresh token & blacklist access token
│   ├── mapper/
│   │   └── UserMapper.java              # MapStruct
│   └── oauth2/
│       ├── OAuth2AuthenticationSuccessHandler.java
│       ├── OAuth2AuthenticationFailureHandler.java
│       └── OAuth2UserInfoFactory.java
│
├── rbac/                            # Feature: Roles & Permissions
│   ├── entity/
│   │   ├── Role.java
│   │   ├── Permission.java
│   │   └── RolePermission.java
│   ├── repository/
│   │   ├── RoleRepository.java
│   │   └── PermissionRepository.java
│   └── service/
│       └── RbacService.java             # hasPermission check
│
├── workspace/                       # Feature: Workspace
│   ├── controller/
│   │   ├── WorkspaceController.java     # CRUD /api/v1/workspaces
│   │   └── WorkspaceMemberController.java  # /api/v1/workspaces/{id}/members
│   ├── dto/
│   │   ├── CreateWorkspaceRequest.java
│   │   ├── UpdateWorkspaceRequest.java
│   │   ├── WorkspaceResponse.java
│   │   ├── InviteMemberRequest.java
│   │   └── WorkspaceMemberResponse.java
│   ├── entity/
│   │   ├── Workspace.java
│   │   └── WorkspaceMember.java
│   ├── repository/
│   │   ├── WorkspaceRepository.java
│   │   └── WorkspaceMemberRepository.java
│   ├── service/
│   │   └── WorkspaceService.java
│   └── mapper/
│       └── WorkspaceMapper.java
│
├── project/                         # Feature: Project
│   ├── controller/
│   │   ├── ProjectController.java       # CRUD /api/v1/workspaces/{wid}/projects
│   │   └── ProjectMemberController.java
│   ├── dto/
│   │   ├── CreateProjectRequest.java
│   │   ├── UpdateProjectRequest.java
│   │   ├── ProjectResponse.java
│   │   └── ProjectMemberResponse.java
│   ├── entity/
│   │   ├── Project.java
│   │   └── ProjectMember.java
│   ├── repository/
│   │   ├── ProjectRepository.java
│   │   └── ProjectMemberRepository.java
│   ├── service/
│   │   └── ProjectService.java
│   └── mapper/
│       └── ProjectMapper.java
│
├── issue/                           # Feature: Issue (core)
│   ├── controller/
│   │   ├── IssueController.java         # CRUD /api/v1/projects/{pid}/issues
│   │   ├── IssueHistoryController.java  # GET /api/v1/issues/{id}/history
│   │   └── CommentController.java       # CRUD /api/v1/issues/{id}/comments
│   ├── dto/
│   │   ├── CreateIssueRequest.java
│   │   ├── UpdateIssueRequest.java
│   │   ├── IssueResponse.java
│   │   ├── IssueDetailResponse.java
│   │   ├── IssueFilterRequest.java      # Filter by status, assignee, type, sprint…
│   │   ├── CommentRequest.java
│   │   ├── CommentResponse.java
│   │   └── IssueHistoryResponse.java
│   ├── entity/
│   │   ├── Issue.java
│   │   ├── Comment.java
│   │   ├── IssueHistory.java
│   │   ├── IssueType.java
│   │   ├── Status.java
│   │   └── Priority.java
│   ├── repository/
│   │   ├── IssueRepository.java         # JpaSpecificationExecutor cho dynamic filter
│   │   ├── CommentRepository.java
│   │   ├── IssueHistoryRepository.java
│   │   ├── IssueTypeRepository.java
│   │   ├── StatusRepository.java
│   │   └── PriorityRepository.java
│   ├── service/
│   │   ├── IssueService.java
│   │   ├── CommentService.java
│   │   └── IssueHistoryService.java     # Ghi audit log khi field thay đổi
│   ├── mapper/
│   │   └── IssueMapper.java
│   └── spec/
│       └── IssueSpecification.java      # Spring Data Specifications for dynamic query
│
├── sprint/                          # Feature: Sprint & Board
│   ├── controller/
│   │   ├── SprintController.java        # CRUD + start/complete /api/v1/projects/{pid}/sprints
│   │   └── BoardController.java         # GET /api/v1/projects/{pid}/board (kanban view)
│   ├── dto/
│   │   ├── CreateSprintRequest.java
│   │   ├── UpdateSprintRequest.java
│   │   ├── SprintResponse.java
│   │   ├── BoardResponse.java           # {columns: [{status, issues}]}
│   │   └── MoveIssueRequest.java        # Move issue between sprints / reorder
│   ├── entity/
│   │   ├── Sprint.java
│   │   └── SprintIssue.java
│   ├── repository/
│   │   ├── SprintRepository.java
│   │   └── SprintIssueRepository.java
│   ├── service/
│   │   └── SprintService.java
│   └── mapper/
│       └── SprintMapper.java
│
├── notification/                    # Feature: Notification (RabbitMQ + WebSocket)
│   ├── controller/
│   │   └── NotificationController.java  # GET /api/v1/notifications (user's inbox)
│   ├── dto/
│   │   └── NotificationPayload.java
│   ├── messaging/
│   │   ├── NotificationProducer.java    # Publish to RabbitMQ exchange
│   │   └── NotificationConsumer.java    # Consume & push via WebSocket STOMP
│   └── websocket/
│       └── WebSocketEventListener.java
│
└── reference/                       # Feature: Reference data (read-only lookup)
    ├── controller/
    │   └── ReferenceController.java     # GET /api/v1/ref/issue-types, statuses, priorities
    ├── dto/
    │   └── ReferenceItemResponse.java
    └── service/
        └── ReferenceService.java        # Cached in Redis (TTL 1h)
Phase 3 — Core Feature Implementation (theo thứ tự dependency)
Step 1 — Shared Infrastructure
ApiResponse<T> wrapper chuẩn
GlobalExceptionHandler — mapping exception → HTTP status + error body
JwtTokenProvider (JJWT) — sign/verify/extract claims
JwtAuthenticationFilter — đọc Bearer token từ header, set SecurityContext
SecurityConfig — permit /api/v1/auth/**, /oauth2/**, /swagger-ui/**; require auth cho còn lại
RedisConfig — Jackson serializer
RabbitMQConfig — declare exchange, queue, binding
WebSocketConfig — STOMP endpoint /ws, message broker prefix /topic
Step 2 — User & Auth Feature
Endpoints:

Method	Path	Mô tả
POST	/api/v1/auth/register	Đăng ký tài khoản
POST	/api/v1/auth/login	Đăng nhập, trả JWT
POST	/api/v1/auth/refresh	Refresh access token
POST	/api/v1/auth/logout	Blacklist token
GET	/api/v1/users/me	Lấy profile
PUT	/api/v1/users/me	Cập nhật profile
GET	/oauth2/authorize/google	OAuth2 redirect
Logic quan trọng:

AuthService.register() → hash password bằng BCrypt, lưu User, tạo JWT pair
TokenService → lưu refresh:{userId} vào Redis với TTL; blacklist access token khi logout
OAuth2AuthenticationSuccessHandler → sau Google callback, upsert User + OAuthAccount, redirect kèm JWT
Step 3 — RBAC Feature
Seed data (V2 migration) tạo sẵn roles: WORKSPACE_ADMIN, WORKSPACE_MEMBER, WORKSPACE_VIEWER; và roles tương tự cho project
RbacService.hasWorkspacePermission(userId, workspaceId, permission) — dùng để authorize trong service layer
Step 4 — Workspace Feature
Endpoints:

Method	Path	Mô tả
POST	/api/v1/workspaces	Tạo workspace
GET	/api/v1/workspaces	Danh sách workspace của tôi
GET	/api/v1/workspaces/{id}	Chi tiết
PUT	/api/v1/workspaces/{id}	Cập nhật
DELETE	/api/v1/workspaces/{id}	Xóa (owner only)
GET	/api/v1/workspaces/{id}/members	Danh sách member
POST	/api/v1/workspaces/{id}/members	Invite member
PUT	/api/v1/workspaces/{id}/members/{uid}/role	Đổi role
DELETE	/api/v1/workspaces/{id}/members/{uid}	Kick member
Step 5 — Project Feature
Endpoints:

Method	Path	Mô tả
POST	/api/v1/workspaces/{wid}/projects	Tạo project
GET	/api/v1/workspaces/{wid}/projects	Danh sách projects
GET	/api/v1/projects/{pid}	Chi tiết
PUT	/api/v1/projects/{pid}	Cập nhật
DELETE	/api/v1/projects/{pid}	Archive/delete
GET/POST/PUT/DELETE	/api/v1/projects/{pid}/members	Quản lý project members
Logic: project_key tự động sinh từ tên project (e.g. "My Project" → "MP"), đảm bảo unique trong workspace.

Step 6 — Issue Feature (core)
Endpoints:

Method	Path	Mô tả
POST	/api/v1/projects/{pid}/issues	Tạo issue
GET	/api/v1/projects/{pid}/issues	List + filter (status, type, assignee, sprint, keyword) + pagination
GET	/api/v1/issues/{id}	Chi tiết issue
PUT	/api/v1/issues/{id}	Cập nhật
DELETE	/api/v1/issues/{id}	Xóa
PATCH	/api/v1/issues/{id}/status	Chuyển status
PATCH	/api/v1/issues/{id}/assignee	Assign
GET	/api/v1/issues/{id}/history	Audit log
GET/POST/PUT/DELETE	/api/v1/issues/{id}/comments	Comments
Logic quan trọng:

issue_key = {project_key}-{sequential_number} (e.g. MP-42) — dùng DB sequence hoặc SELECT MAX trong transaction
IssueHistoryService — tự động ghi history khi status, assignee, priority, title... thay đổi (so sánh trước/sau update)
IssueSpecification — dynamic query với Specification<Issue> cho filter phức tạp
Sau khi cập nhật issue → publish event lên RabbitMQ → NotificationConsumer push WebSocket tới assignee
Step 7 — Sprint Feature
Endpoints:

Method	Path	Mô tả
POST	/api/v1/projects/{pid}/sprints	Tạo sprint
GET	/api/v1/projects/{pid}/sprints	Danh sách sprints
PUT	/api/v1/sprints/{sid}	Cập nhật sprint
POST	/api/v1/sprints/{sid}/start	Start sprint (→ status=ACTIVE)
POST	/api/v1/sprints/{sid}/complete	Complete sprint
POST	/api/v1/sprints/{sid}/issues	Thêm issue vào sprint
DELETE	/api/v1/sprints/{sid}/issues/{iid}	Xóa issue khỏi sprint
GET	/api/v1/projects/{pid}/board	Kanban board (active sprint + issue grouped by status)
PATCH	/api/v1/sprints/{sid}/issues/{iid}/position	Reorder issue (lexicographic position)
Logic:

Chỉ 1 sprint có thể ACTIVE tại một thời điểm trong project
Khi complete, unresolved issues có thể move sang sprint tiếp theo hoặc backlog
Step 8 — Notification Feature
NotificationProducer.send(event) → gửi NotificationPayload (type, targetUserId, issueId, message)
NotificationConsumer @RabbitListener → push qua SimpMessagingTemplate tới /topic/notifications/{userId}
Client subscribe /topic/notifications/{userId} qua STOMP WebSocket
Step 9 — Reference Data API
Cached in Redis với key ref:issue-types, ref:statuses, ref:priorities, TTL 1 giờ
GET /api/v1/ref/issue-types, /api/v1/ref/statuses, /api/v1/ref/priorities
Phase 4 — Cross-Cutting Implementation Details
Entity Design Conventions
java

// Shared base entity
@MappedSuperclass
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;
    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
API Response Convention
json

// Success
{ "data": { ... }, "message": "Success", "timestamp": "2026-09-13T16:20:00Z" }
// Error
{ "error": "RESOURCE_NOT_FOUND", "message": "Issue not found", "timestamp": "..." }
// Paginated
{ "data": [...], "page": 0, "size": 20, "total": 150, "totalPages": 8 }
Security Flow

Client → [Bearer JWT] → JwtAuthenticationFilter → SecurityContext
                                                 → Controller → Service
                                                                → RbacService.check()
Flyway Migration Rules
Không bao giờ sửa file migration đã commit
Tạo V{n+1}__...sql cho mọi schema change
Mỗi migration là idempotent và có transaction
Naming: V1__create_schema.sql, V2__seed_reference_data.sql, V3__add_column_xyz.sql
Verification Plan
Automated Tests
bash

# Unit tests
./mvnw test
# Integration tests (Testcontainers)
./mvnw verify -P integration-test
Manual Verification
Chạy docker-compose up -d (postgres, redis, rabbitmq)
./mvnw spring-boot:run → Flyway tự migrate
Mở http://localhost:8080/swagger-ui.html — kiểm tra tất cả endpoint visible
Test auth flow: register → login → lấy JWT → access protected endpoint
Test OAuth2: redirect Google → callback → JWT trả về
Test WebSocket: subscribe /topic/notifications/{userId} → update issue → nhận notification
Implementation Order Summary

Phase 1: pom.xml + application.yaml + Flyway migrations (V1 + V2)
Phase 2: Shared (ApiResponse, Exception, JWT, Security, Configs)
Phase 3: User + Auth feature (register/login/OAuth2)
Phase 4: RBAC entities + seed
Phase 5: Workspace feature
Phase 6: Project feature
Phase 7: Issue feature (core — largest)
Phase 8: Sprint + Board feature
Phase 9: Notification (RabbitMQ + WebSocket)
Phase 10: Reference data API + Redis cache
Phase 11: OpenAPI annotations + final polish
