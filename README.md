# 🏪 couponmoa-store

## 📌 개요

이 서버는 Couponmoa 프로젝트의 **가맹점(매장) 관리 및 구독 기능을 전담**하는 서비스입니다.  
관리자는 매장을 생성/수정/삭제할 수 있고, 사용자는 매장을 구독하여 **새 쿠폰 발행 시 알림을 받을 수 있습니다.** (알림 서버로 요청)

---

## 🧩 주요 기능

- 매장 등록, 수정, 삭제
- 매장 키워드 검색 (커서 기반 페이징 지원)
- 사용자의 매장 구독 / 구독 취소 / 구독 목록 조회
- 구독자에게 **SQS 기반 이메일 알림 발송 요청**

---

## 🔗 주요 API 목록

### 📁 매장 관리 (StoreControllerV2)

| 메서드 | URI | 설명 |
|--------|-----|------|
| `POST` | `/api/v2/stores` | 매장 생성 |
| `GET` | `/api/v2/stores` | 키워드 기반 매장 목록 조회 (커서 기반) |
| `GET` | `/api/v2/stores/my` | 내가 등록한 매장 목록 조회 |
| `GET` | `/api/v2/stores/my/simple` | 내가 등록한 매장 목록 (간단히) |
| `GET` | `/api/v2/stores/{storeId}` | 매장 상세 조회 |
| `PUT` | `/api/v2/stores/{storeId}` | 매장 수정 |
| `DELETE` | `/api/v2/stores/{storeId}` | 매장 삭제 |

---

### 👥 매장 구독 (UserStoreSubscribeController)

| 메서드 | URI | 설명 |
|--------|-----|------|
| `POST` | `/api/v1/stores/{storeId}/subscriptions` | 매장 구독 |
| `POST` | `/api/v1/stores/{storeId}/unsubscriptions` | 매장 구독 취소 |
| `GET` | `/api/v1/stores/subscriptions` | 내가 구독한 매장 목록 |

---

## ⚙️ 기술 스택

- Java 17
- Spring Boot 3.x
- Spring Data JPA + MySQL
- Amazon SQS (알림 전송 연계)
- Gradle
