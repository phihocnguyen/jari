# Jari Backend API Documentation

> **Phiên bản:** 1.0.0  
> **Framework:** Spring Boot 3 / 4 (Java 17)  
> **Base URL:** `http://localhost:8080`  
> **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)  
> **OpenAPI JSON Spec:** [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

---

## MỤC LỤC
1. [Quy chuẩn chung (Standards & Conventions)](#1-quy-chuẩn-chung-standards--conventions)
2. [Cấu trúc Response & Error Handling](#2-cấu-trúc-response--error-handling)
3. [Xác thực & Phân quyền (Authentication & Authorization)](#3-xác-thực--phân-quyền-authentication--authorization)
4. [Dữ liệu tra cứu (Reference Data)](#4-dữ-liệu-tra-cứu-reference-data)
5. [Quản lý người dùng (Users)](#5-quản-lý-người-dùng-users)
6. [Quản lý Không gian làm việc (Workspaces)](#6-quản-lý-không-gian-làm-việc-workspaces)
7. [Quản lý Dự án (Projects)](#7-quản-lý-dự-án-projects)
8. [Quản lý Công việc (Issues & Audit History)](#8-quản-lý-công-việc-issues--audit-history)
9. [Bình luận (Comments)](#9-bình-luận-comments)
10. [Quản lý Sprint & Bảng Kanban (Sprints & Board)](#10-quản-lý-sprint--bảng-kanban-sprints--board)
11. [Thông báo thời gian thực (WebSocket / STOMP)](#11-thông-báo-thời-gian-thực-websocket--stomp)

---

## 1. Quy chuẩn chung (Standards & Conventions)

### 1.1 Headers
- Mọi request gửi JSON body phải có header:
  ```http
  Content-Type: application/json
  Accept: application/json
  ```
- Các endpoint yêu cầu xác thực phải gửi kèm access token trong header:
  ```http
  Authorization: Bearer <accessToken>
  ```

### 1.2 Kiểu dữ liệu chính
- **ID:** Chuẩn UUID v4 dạng chuỗi (ví dụ: `c0a80123-0000-0000-0000-000000000001`).
- **Thời gian (Timestamp):** Chuẩn ISO-8601 UTC (ví dụ: `2026-09-15T06:00:00Z` hoặc có offset `2026-09-15T13:00:00+07:00`).
- **Ngày (Date):** Định dạng `YYYY-MM-DD` (ví dụ: `2026-09-30`).

---

## 2. Cấu trúc Response & Error Handling

Mọi response thành công đều được bọc trong cấu trúc chuẩn `ApiResponse<T>`:

### 2.1 Success Response (`ApiResponse<T>`)
```json
{
  "data": { ... },
  "message": "Success",
  "timestamp": "2026-09-15T06:15:30.123Z"
}
```
*Đối với các thao tác không trả về body dữ liệu (như DELETE hoặc hành động chỉ trả về thông điệp), `data` sẽ là `null` hoặc không xuất hiện, `message` ghi rõ nội dung.*

### 2.2 Paginated Response (`ApiResponse<PageResponse<T>>`)
Áp dụng cho các endpoint danh sách có phân trang (như danh sách Issue):
```json
{
  "data": {
    "data": [ ... ],
    "page": 0,
    "size": 20,
    "total": 150,
    "totalPages": 8
  },
  "message": "Success",
  "timestamp": "2026-09-15T06:15:30.123Z"
}
```

### 2.3 Error Response (`ErrorResponse`)
Khi xảy ra lỗi (HTTP 4xx, 5xx), backend trả về body:
```json
{
  "error": "VALIDATION_FAILED",
  "message": "Request validation failed",
  "timestamp": "2026-09-15T06:15:30.123Z",
  "fieldErrors": {
    "email": "must be a well-formed email address",
    "password": "size must be between 6 and 100"
  }
}
```

#### Các mã lỗi thường gặp:
| HTTP Code | Error Code (`error`) | Ý nghĩa |
| :--- | :--- | :--- |
| **400** | `VALIDATION_FAILED` | Dữ liệu đầu vào không hợp lệ (kèm chi tiết trong `fieldErrors`) |
| **400** | `INVALID_STATE` | Thao tác không đúng luồng nghiệp vụ (ví dụ: sprint chưa bắt đầu) |
| **401** | `UNAUTHORIZED` | Token không hợp lệ, hết hạn, hoặc bị thu hồi (Blacklisted) |
| **403** | `FORBIDDEN` | Người dùng không có quyền thực hiện hành động |
| **404** | `NOT_FOUND` | Tài nguyên (Workspace, Project, Issue, User, ...) không tồn tại |
| **409** | `CONFLICT` | Trùng lặp dữ liệu (ví dụ: email, username, workspaceKey, projectKey đã tồn tại) |
| **500** | `INTERNAL_ERROR` | Lỗi máy chủ chưa được bắt |

---

## 3. Xác thực & Phân quyền (Authentication & Authorization)

### 3.1 Đăng ký tài khoản (Register)
- **Method & Path:** `POST /api/v1/auth/register`
- **Quyền truy cập:** Public (Không cần token)
- **Request Body:**
  ```json
  {
    "username": "phihocnguyen",
    "email": "phi@example.com",
    "password": "Password123@",
    "displayName": "Phi Nguyen"
  }
  ```
  *Ràng buộc:*
  - `username`: 3 - 50 ký tự, bắt buộc.
  - `email`: Định dạng email chuẩn, bắt buộc, duy nhất.
  - `password`: 6 - 100 ký tự, bắt buộc.
  - `displayName`: Tối đa 100 ký tự, bắt buộc.
- **Response:** `201 Created`
  ```json
  {
    "data": {
      "accessToken": "eyJhbGciOi...",
      "refreshToken": "a1b2c3d4-e5f6-...",
      "tokenType": "Bearer",
      "expiresIn": 900,
      "user": {
        "id": "7f000001-91a0-1555-8191-a0c3ba500000",
        "username": "phihocnguyen",
        "email": "phi@example.com",
        "displayName": "Phi Nguyen",
        "avatarUrl": null,
        "status": "ACTIVE"
      }
    },
    "message": "Success",
    "timestamp": "2026-09-15T06:15:30.123Z"
  }
  ```

---

### 3.2 Đăng nhập (Login)
- **Method & Path:** `POST /api/v1/auth/login`
- **Quyền truy cập:** Public
- **Request Body:**
  ```json
  {
    "email": "phi@example.com",
    "password": "Password123@"
  }
  ```
- **Response:** `200 OK` (Cấu trúc tương tự như Register)

---

### 3.3 Làm mới Access Token (Refresh Token)
- **Method & Path:** `POST /api/v1/auth/refresh`
- **Quyền truy cập:** Public
- **Request Body:**
  ```json
  {
    "refreshToken": "a1b2c3d4-e5f6-..."
  }
  ```
- **Response:** `200 OK`
  ```json
  {
    "data": {
      "accessToken": "eyJhbGciOi...",
      "refreshToken": "a1b2c3d4-e5f6-...",
      "tokenType": "Bearer",
      "expiresIn": 900,
      "user": { ... }
    },
    "message": "Success",
    "timestamp": "2026-09-15T06:15:30.123Z"
  }
  ```

---

### 3.4 Đăng xuất (Logout)
- **Method & Path:** `POST /api/v1/auth/logout`
- **Quyền truy cập:** Authenticated (`Authorization: Bearer <token>`)
- **Mô tả:** Đưa Access Token hiện tại vào Redis Blacklist và xoá Refresh Token tương ứng.
- **Response:** `200 OK`
  ```json
  {
    "data": null,
    "message": "Logged out successfully",
    "timestamp": "2026-09-15T06:15:30.123Z"
  }
  ```

---

### 3.5 Đăng nhập bằng Google (OAuth2 SSO)
1. Frontend chuyển hướng người dùng đến:
   ```
   GET http://localhost:8080/oauth2/authorize/google
   ```
2. Sau khi xác thực với Google thành công, backend tự động tạo/liên kết tài khoản và chuyển hướng về frontend:
   ```
   http://localhost:3000/oauth2/callback?accessToken=<TOKEN>&refreshToken=<TOKEN>
   ```

---

## 4. Dữ liệu tra cứu (Reference Data)

Các API này cung cấp dữ liệu danh mục tĩnh dùng chung cho hệ thống, phục vụ hiển thị dropdown trên giao diện.

### 4.1 Danh sách Loại Issue (Issue Types)
- **Method & Path:** `GET /api/v1/ref/issue-types`
- **Quyền truy cập:** Authenticated
- **Response:** `200 OK`
  ```json
  {
    "data": [
      { "id": "uuid-1", "name": "EPIC", "description": "Large body of work that can be broken down", "extra": null },
      { "id": "uuid-2", "name": "STORY", "description": "User story or feature request", "extra": null },
      { "id": "uuid-3", "name": "TASK", "description": "A task to be completed", "extra": null },
      { "id": "uuid-4", "name": "BUG", "description": "A defect or problem found in the product", "extra": null },
      { "id": "uuid-5", "name": "SUBTASK", "description": "A subtask belonging to a parent issue", "extra": null }
    ],
    "message": "Success",
    "timestamp": "2026-09-15T06:15:30.123Z"
  }
  ```

### 4.2 Danh sách Trạng thái (Statuses)
- **Method & Path:** `GET /api/v1/ref/statuses`
- **Quyền truy cập:** Authenticated
- **Response:** `200 OK` (`extra` đại diện cho status category: `TODO`, `IN_PROGRESS`, `DONE`)
  ```json
  {
    "data": [
      { "id": "uuid-10", "name": "TO DO", "description": null, "extra": "TODO" },
      { "id": "uuid-11", "name": "IN PROGRESS", "description": null, "extra": "IN_PROGRESS" },
      { "id": "uuid-12", "name": "IN REVIEW", "description": null, "extra": "IN_PROGRESS" },
      { "id": "uuid-13", "name": "DONE", "description": null, "extra": "DONE" },
      { "id": "uuid-14", "name": "CANCELLED", "description": null, "extra": "DONE" }
    ],
    "message": "Success",
    "timestamp": "2026-09-15T06:15:30.123Z"
  }
  ```

### 4.3 Danh sách Mức độ ưu tiên (Priorities)
- **Method & Path:** `GET /api/v1/ref/priorities`
- **Quyền truy cập:** Authenticated
- **Response:** `200 OK` (`extra` là thứ tự cấp độ từ 1 - cao nhất đến 5 - thấp nhất)
  ```json
  {
    "data": [
      { "id": "uuid-20", "name": "HIGHEST", "description": null, "extra": "1" },
      { "id": "uuid-21", "name": "HIGH", "description": null, "extra": "2" },
      { "id": "uuid-22", "name": "MEDIUM", "description": null, "extra": "3" },
      { "id": "uuid-23", "name": "LOW", "description": null, "extra": "4" },
      { "id": "uuid-24", "name": "LOWEST", "description": null, "extra": "5" }
    ],
    "message": "Success",
    "timestamp": "2026-09-15T06:15:30.123Z"
  }
  ```

---

## 5. Quản lý người dùng (Users)

### 5.1 Lấy thông tin cá nhân hiện tại
- **Method & Path:** `GET /api/v1/users/me`
- **Quyền truy cập:** Authenticated
- **Response:** `200 OK`
  ```json
  {
    "data": {
      "id": "7f000001-91a0-1555-8191-a0c3ba500000",
      "username": "phihocnguyen",
      "email": "phi@example.com",
      "displayName": "Phi Nguyen",
      "avatarUrl": "https://avatar.example.com/phi.png",
      "status": "ACTIVE"
    },
    "message": "Success",
    "timestamp": "2026-09-15T06:15:30.123Z"
  }
  ```

### 5.2 Cập nhật thông tin cá nhân
- **Method & Path:** `PUT /api/v1/users/me`
- **Quyền truy cập:** Authenticated
- **Request Body:**
  ```json
  {
    "displayName": "Phi Nguyen Updated",
    "avatarUrl": "https://avatar.example.com/new-avatar.png"
  }
  ```
- **Response:** `200 OK` (Trả về `UserResponse` mới)

---

## 6. Quản lý Không gian làm việc (Workspaces)

### 6.1 Tạo Workspace mới
- **Method & Path:** `POST /api/v1/workspaces`
- **Quyền truy cập:** Authenticated (Người tạo mặc định thành Owner & `WORKSPACE_ADMIN`)
- **Request Body:**
  ```json
  {
    "name": "Acme Software Corp",
    "workspaceKey": "ACME",
    "description": "Không gian làm việc cho khối kỹ thuật Acme"
  }
  ```
  *Ràng buộc:*
  - `name`: Tối đa 100 ký tự, bắt buộc.
  - `workspaceKey`: 1 - 20 ký tự, viết HOA chữ cái, số và dấu gạch dưới `_` (`^[A-Z0-9_]+$`), bắt buộc, duy nhất toàn hệ thống.
- **Response:** `201 Created`
  ```json
  {
    "data": {
      "id": "11111111-2222-3333-4444-555555555555",
      "name": "Acme Software Corp",
      "workspaceKey": "ACME",
      "description": "Không gian làm việc cho khối kỹ thuật Acme",
      "ownerId": "7f000001-91a0-1555-8191-a0c3ba500000",
      "createdAt": "2026-09-15T06:00:00Z"
    },
    "message": "Success",
    "timestamp": "2026-09-15T06:15:30.123Z"
  }
  ```

---

### 6.2 Lấy danh sách Workspace của tôi
- **Method & Path:** `GET /api/v1/workspaces`
- **Quyền truy cập:** Authenticated
- **Response:** `200 OK`
  ```json
  {
    "data": [
      {
        "id": "11111111-2222-3333-4444-555555555555",
        "name": "Acme Software Corp",
        "workspaceKey": "ACME",
        "description": "...",
        "ownerId": "7f000001-91a0-1555-8191-a0c3ba500000",
        "createdAt": "2026-09-15T06:00:00Z"
      }
    ],
    "message": "Success",
    "timestamp": "2026-09-15T06:15:30.123Z"
  }
  ```

---

### 6.3 Chi tiết một Workspace
- **Method & Path:** `GET /api/v1/workspaces/{id}`
- **Quyền truy cập:** Authenticated
- **Path Param:** `id` (UUID của workspace)
- **Response:** `200 OK`

---

### 6.4 Cập nhật Workspace
- **Method & Path:** `PUT /api/v1/workspaces/{id}`
- **Quyền truy cập:** `WORKSPACE_ADMIN`
- **Request Body:**
  ```json
  {
    "name": "Acme Enterprise Solutions",
    "description": "Mô tả mới"
  }
  ```
- **Response:** `200 OK`

---

### 6.5 Xoá Workspace
- **Method & Path:** `DELETE /api/v1/workspaces/{id}`
- **Quyền truy cập:** Chỉ `Workspace Owner`
- **Response:** `200 OK`
  ```json
  {
    "data": null,
    "message": "Workspace deleted",
    "timestamp": "2026-09-15T06:15:30.123Z"
  }
  ```

---

### 6.6 Lấy danh sách thành viên trong Workspace
- **Method & Path:** `GET /api/v1/workspaces/{id}/members`
- **Quyền truy cập:** Authenticated (Thành viên workspace)
- **Response:** `200 OK`
  ```json
  {
    "data": [
      {
        "userId": "7f000001-91a0-1555-8191-a0c3ba500000",
        "displayName": "Phi Nguyen",
        "email": "phi@example.com",
        "roleName": "WORKSPACE_ADMIN",
        "joinedAt": "2026-09-15T06:00:00Z"
      }
    ],
    "message": "Success",
    "timestamp": "2026-09-15T06:15:30.123Z"
  }
  ```

---

### 6.7 Mời thành viên vào Workspace
- **Method & Path:** `POST /api/v1/workspaces/{id}/members`
- **Quyền truy cập:** `WORKSPACE_ADMIN`
- **Request Body:**
  ```json
  {
    "userId": "99999999-8888-7777-6666-555555555555",
    "roleName": "WORKSPACE_MEMBER"
  }
  ```
  *Vai trò hợp lệ:* `WORKSPACE_ADMIN`, `WORKSPACE_MEMBER`, `WORKSPACE_VIEWER`
- **Response:** `201 Created`

---

### 6.8 Xoá thành viên khỏi Workspace
- **Method & Path:** `DELETE /api/v1/workspaces/{id}/members/{userId}`
- **Quyền truy cập:** `WORKSPACE_ADMIN`
- **Response:** `200 OK`

---

## 7. Quản lý Dự án (Projects)

### 7.1 Tạo Dự án mới trong Workspace
- **Method & Path:** `POST /api/v1/workspaces/{workspaceId}/projects`
- **Quyền truy cập:** Thành viên Workspace
- **Path Param:** `workspaceId` (UUID)
- **Request Body:**
  ```json
  {
    "name": "Jari Core Platform",
    "projectKey": "JARI",
    "description": "Dự án phát triển nền tảng Jira Clone",
    "leadId": "7f000001-91a0-1555-8191-a0c3ba500000",
    "projectType": "SOFTWARE"
  }
  ```
  *Ràng buộc:*
  - `name`: Tối đa 100 ký tự, bắt buộc.
  - `projectKey`: Viết HOA chữ cái/số/gạch dưới, tối đa 20 ký tự (ví dụ: `JARI`), dùng làm tiền tố mã Issue (`JARI-1`, `JARI-2`). Bắt buộc, duy nhất.
  - `projectType`: `SOFTWARE` (mặc định) hoặc `BUSINESS`.
- **Response:** `201 Created`
  ```json
  {
    "data": {
      "id": "22222222-3333-4444-5555-666666666666",
      "workspaceId": "11111111-2222-3333-4444-555555555555",
      "name": "Jari Core Platform",
      "projectKey": "JARI",
      "description": "Dự án phát triển nền tảng Jira Clone",
      "leadId": "7f000001-91a0-1555-8191-a0c3ba500000",
      "projectType": "SOFTWARE",
      "status": "ACTIVE",
      "createdAt": "2026-09-15T06:05:00Z"
    },
    "message": "Success",
    "timestamp": "2026-09-15T06:15:30.123Z"
  }
  ```

---

### 7.2 Lấy danh sách Dự án thuộc Workspace
- **Method & Path:** `GET /api/v1/workspaces/{workspaceId}/projects`
- **Quyền truy cập:** Thành viên Workspace
- **Response:** `200 OK` (Danh sách `ProjectResponse`)

---

### 7.3 Chi tiết Dự án
- **Method & Path:** `GET /api/v1/projects/{id}`
- **Quyền truy cập:** Authenticated
- **Response:** `200 OK`

---

### 7.4 Cập nhật Dự án
- **Method & Path:** `PUT /api/v1/projects/{id}`
- **Quyền truy cập:** `PROJECT_ADMIN`
- **Request Body:**
  ```json
  {
    "name": "Jari Platform Redesign",
    "description": "Cập nhật mô tả",
    "leadId": "7f000001-91a0-1555-8191-a0c3ba500000",
    "status": "ACTIVE"
  }
  ```
- **Response:** `200 OK`

---

### 7.5 Xoá Dự án
- **Method & Path:** `DELETE /api/v1/projects/{id}`
- **Quyền truy cập:** `PROJECT_ADMIN`
- **Response:** `200 OK`

---

### 7.6 Danh sách thành viên Dự án
- **Method & Path:** `GET /api/v1/projects/{id}/members`
- **Response:** `200 OK` (Danh sách `ProjectMemberResponse`)

---

### 7.7 Thêm thành viên vào Dự án
- **Method & Path:** `POST /api/v1/projects/{id}/members`
- **Quyền truy cập:** `PROJECT_ADMIN`
- **Request Body:**
  ```json
  {
    "userId": "99999999-8888-7777-6666-555555555555",
    "roleName": "PROJECT_MEMBER"
  }
  ```
  *Vai trò hợp lệ:* `PROJECT_ADMIN`, `PROJECT_MEMBER`, `PROJECT_VIEWER`
- **Response:** `201 Created`

---

### 7.8 Xoá thành viên khỏi Dự án
- **Method & Path:** `DELETE /api/v1/projects/{id}/members/{userId}`
- **Quyền truy cập:** `PROJECT_ADMIN`
- **Response:** `200 OK`

---

## 8. Quản lý Công việc (Issues & Audit History)

### 8.1 Tạo Issue mới
- **Method & Path:** `POST /api/v1/projects/{projectId}/issues`
- **Quyền truy cập:** Thành viên Project
- **Path Param:** `projectId` (UUID)
- **Request Body:**
  ```json
  {
    "title": "Thiết kế giao diện Kanban Board",
    "description": "Sử dụng React/Tailwind/dnd-kit để render cột và thẻ issue",
    "issueTypeId": "uuid-của-STORY",
    "statusId": "uuid-của-TO-DO",
    "priorityId": "uuid-của-HIGH",
    "assigneeId": "7f000001-91a0-1555-8191-a0c3ba500000",
    "parentId": null,
    "storyPoints": 5,
    "dueDate": "2026-09-25"
  }
  ```
  *Ràng buộc:*
  - `title`: Tối đa 255 ký tự, bắt buộc.
  - `issueTypeId`, `statusId`, `priorityId`: Bắt buộc.
  - `parentId`: UUID của Epic cha (nếu có) hoặc Issue cha của Subtask.
  - Mã Issue (ví dụ `JARI-1`) được backend sinh tự động tăng dần theo `projectKey`.
- **Response:** `201 Created`
  ```json
  {
    "data": {
      "id": "33333333-4444-5555-6666-777777777777",
      "issueKey": "JARI-1",
      "title": "Thiết kế giao diện Kanban Board",
      "description": "Sử dụng React/Tailwind/dnd-kit để render cột và thẻ issue",
      "projectId": "22222222-3333-4444-5555-666666666666",
      "issueType": "STORY",
      "status": "TO DO",
      "statusCategory": "TODO",
      "priority": "HIGH",
      "reporterId": "7f000001-91a0-1555-8191-a0c3ba500000",
      "reporterName": "Phi Nguyen",
      "assigneeId": "7f000001-91a0-1555-8191-a0c3ba500000",
      "assigneeName": "Phi Nguyen",
      "parentId": null,
      "storyPoints": 5.0,
      "dueDate": "2026-09-25",
      "createdAt": "2026-09-15T06:10:00Z",
      "updatedAt": "2026-09-15T06:10:00Z"
    },
    "message": "Success",
    "timestamp": "2026-09-15T06:15:30.123Z"
  }
  ```

---

### 8.2 Danh sách Issue có Lọc & Phân trang
- **Method & Path:** `GET /api/v1/projects/{projectId}/issues`
- **Quyền truy cập:** Thành viên Project
- **Query Parameters:**
  - `page`: Số trang, 0-indexed (mặc định: `0`).
  - `size`: Kích thước trang (mặc định: `20`).
  - `statusId`: UUID lọc theo trạng thái.
  - `assigneeId`: UUID lọc theo người được giao.
  - `issueTypeId`: UUID lọc theo loại issue.
  - `priorityId`: UUID lọc theo độ ưu tiên.
  - `sprintId`: UUID lọc theo sprint.
  - `keyword`: Tìm kiếm từ khóa theo `title` hoặc `issueKey`.
- **Response:** `200 OK` (Cấu trúc `ApiResponse<PageResponse<IssueResponse>>`)

---

### 8.3 Chi tiết một Issue
- **Method & Path:** `GET /api/v1/issues/{id}`
- **Quyền truy cập:** Authenticated
- **Response:** `200 OK`

---

### 8.4 Cập nhật Issue (Tự động ghi Log Lịch sử)
- **Method & Path:** `PUT /api/v1/issues/{id}`
- **Quyền truy cập:** Thành viên Project
- **Mô tả:** Cập nhật các trường dữ liệu của Issue. Mọi thay đổi giá trị (Title, Status, Priority, Assignee, Points, ...) đều tự động tạo một bản ghi Audit Trail và gửi thông báo qua RabbitMQ/WebSocket.
- **Request Body:**
  ```json
  {
    "title": "Thiết kế giao diện Kanban Board (Giai đoạn 1)",
    "description": "Cập nhật tài liệu thiết kế",
    "issueTypeId": "uuid-của-STORY",
    "statusId": "uuid-của-IN-PROGRESS",
    "priorityId": "uuid-của-HIGHEST",
    "assigneeId": "uuid-người-nhận-mới",
    "parentId": null,
    "storyPoints": 8,
    "dueDate": "2026-09-30"
  }
  ```
- **Response:** `200 OK` (Trả về `IssueResponse` đã cập nhật)

---

### 8.5 Xoá Issue
- **Method & Path:** `DELETE /api/v1/issues/{id}`
- **Quyền truy cập:** `PROJECT_ADMIN` hoặc Người tạo
- **Response:** `200 OK`

---

### 8.6 Lấy Lịch sử thay đổi của Issue (Audit History)
- **Method & Path:** `GET /api/v1/issues/{id}/history`
- **Quyền truy cập:** Authenticated
- **Response:** `200 OK`
  ```json
  {
    "data": [
      {
        "id": "history-uuid-1",
        "userId": "7f000001-91a0-1555-8191-a0c3ba500000",
        "userName": "Phi Nguyen",
        "field": "status",
        "oldValue": "TO DO",
        "newValue": "IN PROGRESS",
        "createdAt": "2026-09-15T06:12:00Z"
      },
      {
        "id": "history-uuid-2",
        "userId": "7f000001-91a0-1555-8191-a0c3ba500000",
        "userName": "Phi Nguyen",
        "field": "storyPoints",
        "oldValue": "5.0",
        "newValue": "8.0",
        "createdAt": "2026-09-15T06:12:00Z"
      }
    ],
    "message": "Success",
    "timestamp": "2026-09-15T06:15:30.123Z"
  }
  ```

---

### 8.7 Cập nhật nhanh Trạng thái (Quick Update Status)
- **Method & Path:** `PATCH /api/v1/issues/{id}/status`
- **Request Body:** `{"statusId": "uuid"}`
- **Response:** `200 OK` (`IssueResponse`)

---

### 8.8 Cập nhật nhanh Người được giao (Quick Update Assignee)
- **Method & Path:** `PATCH /api/v1/issues/{id}/assignee`
- **Request Body:** `{"assigneeId": "uuid"}` (hoặc `null` để huỷ gán)
- **Response:** `200 OK` (`IssueResponse`)

---

### 8.9 Cập nhật nhanh Độ ưu tiên (Quick Update Priority)
- **Method & Path:** `PATCH /api/v1/issues/{id}/priority`
- **Request Body:** `{"priorityId": "uuid"}`
- **Response:** `200 OK` (`IssueResponse`)

---

### 8.10 Cập nhật nhanh Story Points
- **Method & Path:** `PATCH /api/v1/issues/{id}/story-points`
- **Request Body:** `{"storyPoints": 5.0}`
- **Response:** `200 OK` (`IssueResponse`)

---

### 8.11 Cập nhật nhanh Ngày bắt đầu & Hạn chót (Quick Update Dates)
- **Method & Path:** `PATCH /api/v1/issues/{id}/dates`
- **Request Body:** `{"startDate": "2026-09-16", "dueDate": "2026-09-30"}`
- **Response:** `200 OK` (`IssueResponse`)

---

### 8.12 Gán nhãn cho Issue (Attach Label)
- **Method & Path:** `POST /api/v1/issues/{id}/labels`
- **Request Body:** `{"labelId": "uuid"}`
- **Response:** `200 OK` (`IssueResponse`)

---

### 8.13 Gỡ nhãn khỏi Issue (Detach Label)
- **Method & Path:** `DELETE /api/v1/issues/{id}/labels/{labelId}`
- **Response:** `200 OK` (`IssueResponse`)

---

### 8.14 Cập nhật nhanh Sprint (Move to Sprint / Backlog)
- **Method & Path:** `PATCH /api/v1/issues/{id}/sprint`
- **Request Body:** `{"sprintId": "uuid"}` (hoặc `{"sprintId": null}` để đưa issue về Backlog)
- **Response:** `200 OK` (`IssueResponse`)

---

### 8.15 Quản lý danh mục Nhãn theo Dự án (Project Labels)
- **Lấy danh sách nhãn:** `GET /api/v1/projects/{projectId}/labels`
- **Tạo nhãn mới:** `POST /api/v1/projects/{projectId}/labels`
  - Body: `{"name": "frontend", "color": "#0052CC"}`

---

## 9. Bình luận (Comments)

### 9.1 Danh sách bình luận của Issue
- **Method & Path:** `GET /api/v1/issues/{issueId}/comments`
- **Quyền truy cập:** Authenticated
- **Response:** `200 OK`
  ```json
  {
    "data": [
      {
        "id": "comment-uuid-1",
        "authorId": "7f000001-91a0-1555-8191-a0c3ba500000",
        "authorName": "Phi Nguyen",
        "content": "Đã hoàn thành mockup trên Figma, nhờ team review nhé.",
        "createdAt": "2026-09-15T06:13:00Z",
        "updatedAt": "2026-09-15T06:13:00Z"
      }
    ],
    "message": "Success",
    "timestamp": "2026-09-15T06:15:30.123Z"
  }
  ```

---

### 9.2 Thêm bình luận mới
- **Method & Path:** `POST /api/v1/issues/{issueId}/comments`
- **Quyền truy cập:** Authenticated
- **Request Body:**
  ```json
  {
    "content": "Cần thêm validation cho input số story points."
  }
  ```
- **Response:** `201 Created`

---

### 9.3 Chỉnh sửa bình luận
- **Method & Path:** `PUT /api/v1/issues/{issueId}/comments/{commentId}`
- **Quyền truy cập:** Tác giả bình luận
- **Request Body:**
  ```json
  {
    "content": "Nội dung bình luận đã được chỉnh sửa."
  }
  ```
- **Response:** `200 OK`

---

### 9.4 Xoá bình luận (Soft Delete)
- **Method & Path:** `DELETE /api/v1/issues/{issueId}/comments/{commentId}`
- **Quyền truy cập:** Tác giả bình luận
- **Response:** `200 OK`

---

## 10. Quản lý Sprint & Bảng Kanban (Sprints & Board)

### 10.1 Tạo Sprint mới trong Dự án
- **Method & Path:** `POST /api/v1/projects/{projectId}/sprints`
- **Quyền truy cập:** Thành viên Project
- **Request Body:**
  ```json
  {
    "name": "Sprint 1: MVP Core Features",
    "goal": "Hoàn thiện Auth, CRUD Issue và Kanban Board",
    "startDate": "2026-09-15T00:00:00Z",
    "endDate": "2026-09-29T23:59:59Z"
  }
  ```
- **Response:** `201 Created`
  ```json
  {
    "data": {
      "id": "sprint-uuid-1",
      "projectId": "22222222-3333-4444-5555-666666666666",
      "name": "Sprint 1: MVP Core Features",
      "goal": "Hoàn thiện Auth, CRUD Issue và Kanban Board",
      "startDate": "2026-09-15T00:00:00Z",
      "endDate": "2026-09-29T23:59:59Z",
      "status": "PLANNED",
      "createdAt": "2026-09-15T06:00:00Z"
    },
    "message": "Success",
    "timestamp": "2026-09-15T06:15:30.123Z"
  }
  ```

---

### 10.2 Danh sách Sprint của Dự án
- **Method & Path:** `GET /api/v1/projects/{projectId}/sprints`
- **Quyền truy cập:** Authenticated
- **Response:** `200 OK` (Danh sách `SprintResponse`)

---

### 10.3 Cập nhật Sprint
- **Method & Path:** `PUT /api/v1/sprints/{id}`
- **Request Body:**
  ```json
  {
    "name": "Sprint 1: MVP Core (Gia hạn)",
    "goal": "Hoàn thiện thêm tính năng Notification",
    "startDate": "2026-09-15T00:00:00Z",
    "endDate": "2026-10-05T23:59:59Z"
  }
  ```
- **Response:** `200 OK`

---

### 10.4 Bắt đầu Sprint (Start Sprint)
- **Method & Path:** `POST /api/v1/sprints/{id}/start`
- **Quyền truy cập:** `PROJECT_ADMIN`
- **Mô tả:** Chuyển trạng thái Sprint từ `PLANNED` sang `ACTIVE`. Mỗi dự án chỉ được phép có **tối đa 1 sprint ACTIVE** tại một thời điểm.
- **Response:** `200 OK` (Sprint với `status: "ACTIVE"`)

---

### 10.5 Hoàn thành Sprint (Complete Sprint)
- **Method & Path:** `POST /api/v1/sprints/{id}/complete`
- **Quyền truy cập:** `PROJECT_ADMIN`
- **Mô tả:** Chuyển trạng thái Sprint từ `ACTIVE` sang `COMPLETED`.
- **Response:** `200 OK` (Sprint với `status: "COMPLETED"`)

---

### 10.6 Thêm Issue vào Sprint
- **Method & Path:** `POST /api/v1/sprints/{id}/issues`
- **Request Body:**
  ```json
  {
    "issueId": "33333333-4444-5555-6666-777777777777"
  }
  ```
- **Response:** `201 Created`
  ```json
  {
    "data": null,
    "message": "Issue added to sprint",
    "timestamp": "2026-09-15T06:15:30.123Z"
  }
  ```

---

### 10.7 Xoá Issue khỏi Sprint (Đưa về Backlog)
- **Method & Path:** `DELETE /api/v1/sprints/{id}/issues/{issueId}`
- **Response:** `200 OK`
  ```json
  {
    "data": null,
    "message": "Issue removed from sprint",
    "timestamp": "2026-09-15T06:15:30.123Z"
  }
  ```

---

### 10.8 Lấy dữ liệu Bảng Kanban (Kanban Board View)
- **Method & Path:** `GET /api/v1/projects/{projectId}/board`
- **Quyền truy cập:** Authenticated
- **Mô tả:** Lấy toàn bộ các Issue thuộc Sprint đang `ACTIVE` của dự án, được tự động nhóm theo từng cột Status (`TO DO`, `IN PROGRESS`, `IN REVIEW`, `DONE`, v.v.).
- **Response:** `200 OK`
  ```json
  {
    "data": [
      {
        "statusId": "uuid-to-do",
        "statusName": "TO DO",
        "statusCategory": "TODO",
        "issues": [
          {
            "id": "33333333-4444-5555-6666-777777777777",
            "issueKey": "JARI-1",
            "title": "Thiết kế giao diện Kanban Board",
            "issueType": "STORY",
            "status": "TO DO",
            "priority": "HIGH",
            "assigneeName": "Phi Nguyen",
            "storyPoints": 5.0
          }
        ]
      },
      {
        "statusId": "uuid-in-progress",
        "statusName": "IN PROGRESS",
        "statusCategory": "IN_PROGRESS",
        "issues": []
      },
      {
        "statusId": "uuid-done",
        "statusName": "DONE",
        "statusCategory": "DONE",
        "issues": []
      }
    ],
    "message": "Success",
    "timestamp": "2026-09-15T06:15:30.123Z"
  }
  ```

---

### 10.9 Lấy thông tin chi tiết một Sprint
- **Method & Path:** `GET /api/v1/sprints/{id}`
- **Quyền truy cập:** Authenticated
- **Response:** `200 OK` (`SprintResponse`)

---

### 10.10 Xoá Sprint
- **Method & Path:** `DELETE /api/v1/sprints/{id}`
- **Quyền truy cập:** `PROJECT_ADMIN`
- **Mô tả:** Xoá sprint khỏi hệ thống. Toàn bộ các công việc (Issues) đang nằm trong sprint này sẽ tự động được chuyển về Backlog (`sprintId` gán về null).
- **Response:** `200 OK`
  ```json
  {
    "data": null,
    "message": "Sprint deleted successfully",
    "timestamp": "2026-09-16T08:00:00.000Z"
  }
  ```

---

## 11. Quản lý Phiên bản Phát hành (Releases / Fix Versions)

### 11.1 Danh sách Release trong Dự án
- **Method & Path:** `GET /api/v1/projects/{projectId}/releases`
- **Quyền truy cập:** Thành viên Project
- **Response:** `200 OK` (Mảng `ReleaseResponse` xếp theo thứ tự mới nhất)
  ```json
  {
    "data": [
      {
        "id": "rel-uuid-1",
        "projectId": "22222222-3333-4444-5555-666666666666",
        "name": "1.0.0",
        "description": "Bản phát hành chính thức đầu tiên",
        "status": "UNRELEASED",
        "releaseDate": "2026-09-30",
        "createdAt": "2026-09-16T08:00:00Z"
      }
    ],
    "message": "Success",
    "timestamp": "2026-09-16T08:00:00.000Z"
  }
  ```

---

### 11.2 Chi tiết một Release
- **Method & Path:** `GET /api/v1/releases/{id}`
- **Quyền truy cập:** Authenticated
- **Response:** `200 OK` (`ReleaseResponse`)

---

### 11.3 Tạo mới Release (Version)
- **Method & Path:** `POST /api/v1/projects/{projectId}/releases`
- **Quyền truy cập:** `PROJECT_ADMIN`, `PROJECT_MEMBER`
- **Request Body:**
  ```json
  {
    "name": "1.0.0",
    "description": "Initial MVP Release",
    "releaseDate": "2026-10-01"
  }
  ```
- **Response:** `201 Created` (`ReleaseResponse`)

---

### 11.4 Cập nhật thông tin Release
- **Method & Path:** `PUT /api/v1/releases/{id}`
- **Quyền truy cập:** `PROJECT_ADMIN`, `PROJECT_MEMBER`
- **Request Body:**
  ```json
  {
    "name": "1.0.0-GA",
    "description": "Release đã hoàn tất kiểm thử",
    "releaseDate": "2026-10-05",
    "status": "RELEASED"
  }
  ```
- **Response:** `200 OK` (`ReleaseResponse`)

---

### 11.5 Xoá Release
- **Method & Path:** `DELETE /api/v1/releases/{id}`
- **Quyền truy cập:** `PROJECT_ADMIN`
- **Mô tả:** Xoá release. Tất cả các Issue liên kết với release này sẽ tự động được huỷ liên kết (`release_id = null`) để đảm bảo toàn vẹn dữ liệu.
- **Response:** `200 OK`
  ```json
  {
    "data": null,
    "message": "Release deleted successfully",
    "timestamp": "2026-09-16T08:00:00.000Z"
  }
  ```

---

## 12. Thông báo thời gian thực (WebSocket / STOMP)

Hệ thống tích hợp RabbitMQ + Spring WebSocket Message Broker để đẩy thông báo trực tiếp xuống trình duyệt người dùng khi có sự kiện liên quan đến Issue hoặc Comment.

### 12.1 Kết nối STOMP
- **WebSocket URL:** `ws://localhost:8080/ws` hoặc `http://localhost:8080/ws` (hỗ trợ SockJS fallback)
- **Cấu hình Client (Ví dụ bằng `@stomp/stompjs` hoặc `sockjs-client`):**
  ```javascript
  import { Client } from '@stomp/stompjs';
  import SockJS from 'sockjs-client';

  const client = new Client({
    webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
    reconnectDelay: 5000,
  });

  client.onConnect = () => {
    // Đăng ký nhận thông báo cá nhân theo userId
    const currentUserId = "7f000001-91a0-1555-8191-a0c3ba500000";
    client.subscribe(`/topic/notifications/${currentUserId}`, (message) => {
      const payload = JSON.parse(message.body);
      console.log("Thông báo mới:", payload);
    });
  };

  client.activate();
  ```

### 12.2 Cấu trúc Notification Payload
```json
{
  "type": "ISSUE_ASSIGNED",
  "targetUserId": "7f000001-91a0-1555-8191-a0c3ba500000",
  "issueId": "33333333-4444-5555-6666-777777777777",
  "issueKey": "JARI-1",
  "message": "Bạn vừa được phân công giải quyết công việc JARI-1",
  "timestamp": "2026-09-15T06:15:00Z"
}
```

#### Các loại sự kiện (`type`):
- `ISSUE_ASSIGNED`: Khi người dùng được gán vào issue.
- `ISSUE_UPDATED`: Khi issue người dùng đang theo dõi/được gán có thay đổi.
- `COMMENT_ADDED`: Khi có người bình luận mới vào issue liên quan.

---

## 13. Bảng tham chiếu Vai trò & Phân quyền (RBAC)

| Nhóm | Vai trò (`roleName`) | Quyền hạn tiêu biểu |
| :--- | :--- | :--- |
| **Workspace** | `WORKSPACE_ADMIN` | Toàn quyền quản lý Workspace, đổi tên/mô tả, thêm/xoá thành viên Workspace, tạo Project. |
| | `WORKSPACE_MEMBER` | Xem thông tin Workspace, tham gia các Project được chỉ định. |
| | `WORKSPACE_VIEWER` | Chỉ xem (Read-only) trong phạm vi Workspace. |
| **Project** | `PROJECT_ADMIN` | Quản lý thành viên Project, cấu hình Sprint (Start/Complete), xoá Project/Issue. |
| | `PROJECT_MEMBER` | Tạo Issue, chỉnh sửa Issue, kéo thả trạng thái, bình luận, thêm Issue vào Sprint. |
| | `PROJECT_VIEWER` | Chỉ xem bảng Kanban, danh sách Issue và chi tiết Issue trong Project. |
