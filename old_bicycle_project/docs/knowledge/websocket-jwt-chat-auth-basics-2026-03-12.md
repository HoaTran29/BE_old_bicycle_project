# WebSocket JWT Chat Auth Cho Nguoi Moi Hoc

Ngay cap nhat: 2026-03-12  
Pham vi: Cach backend rang buoc danh tinh nguoi gui tin nhan trong chat real-time

## 1. Boc canh

Project nay co chat real-time dung WebSocket va STOMP.

Luc dau, payload chat co field `senderId`, va backend doc field nay de biet ai la nguoi gui tin nhan. Cach do nghe co ve don gian, nhung no co mot lo hong lon:

- client co the tu sua `senderId`
- nguoi dung A co the gia mao thanh nguoi dung B

Noi ngan gon: neu backend tin vao `senderId` do client tu khai, thi danh tinh nguoi gui khong con dang tin cay.

## 2. Dinh nghia can biet truoc

### WebSocket la gi?

`WebSocket` la ket noi giu cho client va server noi chuyen lien tuc theo thoi gian thuc.

No khac voi HTTP thong thuong o cho:

- HTTP: goi xong roi dong
- WebSocket: mo ket noi, roi gui/nhan nhieu lan tren cung mot kenh

Chat real-time rat hay dung WebSocket.

### STOMP la gi?

`STOMP` la mot giao thuc nhan tin chay tren WebSocket.

Ban co the hieu no la "cach dong goi tin nhan co cau truc" de server va client hieu nhau ro hon.

Vi du:

- `CONNECT`: ket noi vao he thong chat
- `SEND`: gui mot tin nhan
- `SUBSCRIBE`: dang ky nghe mot kenh tin nhan

### JWT la gi?

`JWT` la token xac thuc.

Sau khi dang nhap, backend cap token cho user.  
Khi user goi API hoac ket noi STOMP, token nay duoc gui len de backend xac minh:

- day co dung la user da dang nhap khong
- token con han khong

### Principal la gi?

`Principal` la danh tinh ma server dang gan cho mot session hoac mot request.

Hieu don gian:

- neu server da xac thuc ban la user A
- thi `Principal` la "day la user A"

Trong phase nay, WebSocket session sau khi xac thuc se duoc gan mot `Principal`.

## 3. Van de cu the cua chat truoc khi sua

Truoc day, luong chat co the hieu la:

1. Client gui payload:

```json
{
  "conversationId": "abc",
  "senderId": "user-b",
  "content": "xin chao"
}
```

2. Backend doc `senderId`.
3. Backend tin rang day la nguoi gui that.

Van de o day la gi?

Neu dang dang nhap bang user A, client van co the tu sua payload thanh:

- `senderId = user-b`

Khi do backend co nguy co luu tin nhan nhu the user B vua gui.

Day la mot loai loi rat nguy hiem: **tin vao du lieu nhay cam do client tu khai**.

## 4. Nguyen tac dung la gi?

Nguyen tac dung la:

**Danh tinh phai den tu session da duoc xac thuc, khong phai tu payload.**

Hay nho:

- `payload` la noi dung user muon gui
- `auth context` moi la noi cho biet user do la ai

Trong code backend tot, 2 thu nay phai tach nhau ra.

## 5. Phase nay da sua theo huong nao?

Backend da doi sang flow sau:

1. Client `CONNECT` vao STOMP.
2. Client gui `Authorization: Bearer <token>`.
3. Interceptor cua backend doc token nay.
4. Backend validate JWT.
5. Neu hop le, backend tim user that.
6. Backend tao `Principal` cho session WebSocket.
7. Khi client gui `SEND`, backend lay `senderId` tu `Principal`.
8. Field `senderId` trong payload khong con la nguon su that nua.

## 6. Interceptor trong bai toan nay la gi?

`Interceptor` la mot lop dung de chan ngang du lieu truoc khi no di tiep vao he thong.

Co the hieu no giong nhu mot chot kiem tra.

Trong phase nay, `WebSocketAuthChannelInterceptor` lam viec nhu sau:

- neu la frame `CONNECT`
  - kiem tra co bearer token khong
  - validate JWT
  - tim user trong database
  - gan `Principal` vao session
- neu la frame `SEND` hoac `SUBSCRIBE`
  - neu session chua co user xac thuc thi chan lai

Nghia la:

- khong co token -> khong cho ket noi chat dung cach
- co token sai -> khong cho gui tin nhan

## 7. Tai sao `Principal.name` lai la `userId` ma khong phai email?

Day la mot diem rat thuc te trong project nay.

Chat va notification dang dung:

```java
convertAndSendToUser(userId.toString(), "/queue/messages", ...)
```

Va

```java
convertAndSendToUser(userId.toString(), "/queue/notifications", ...)
```

Dieu nay co nghia la he thong private queue dang route theo `userId`.

Neu `Principal.name` la email thi se co lech:

- server gui theo `userId`
- session lai dang ky theo `email`

Ket qua la tin nhan private queue co the khong den dung nguoi.

Cho nen phase nay backend tao `StompUserPrincipal` voi:

- `getName()` tra ve `userId`

Day la cach de he thong chat va notification noi chuyen dung "ngon ngu" voi nhau.

## 8. Vi du de de hieu

### Truoc khi sua

User A dang nhap, nhung client gui:

```json
{
  "conversationId": "c1",
  "senderId": "user-b",
  "content": "Toi la B day"
}
```

Neu backend tin vao payload, he thong co the luu sai la user B vua gui tin nhan.

### Sau khi sua

User A dang nhap va ket noi STOMP bang token cua A.

Khi gui message:

```json
{
  "conversationId": "c1",
  "senderId": "user-b",
  "content": "Toi la B day"
}
```

Backend bo qua `senderId` trong payload.

Backend lay danh tinh that tu session:

- `principal.getName() = user-a-id`

Nen tin nhan van duoc luu la cua user A, khong phai user B.

## 9. Tai sao day la mot cai tien quan trong?

Vi no giai quyet mot quy tac backend rat can ban:

**Du lieu lien quan den quyen han va danh tinh phai do server kiem soat.**

Neu khong, bat ky chuc nang nao cung co the bi gia mao:

- chat
- notification
- order action
- review
- report

Chat la mot vi du rat de thay, nhung bai hoc nay dung duoc cho rat nhieu module khac.

## 10. Nhung loi nguoi moi hoc hay gap

### Loi 1: Nghi rang da login roi thi payload nao gui len cung an toan

Sai.  
Login chi noi rang user da co token.  
No khong co nghia la moi field client gui len deu dang tin.

### Loi 2: Nghi rang `senderId` la bat buoc phai co trong payload

Khong nhat thiet.  
Neu server da biet nguoi gui la ai tu session, thi `senderId` trong payload co the la du thua hoac chi dung tam de backward compatibility.

### Loi 3: Khong dong bo ten user trong private queue

Neu session dang theo email ma `convertAndSendToUser` lai gui theo `userId`, tin nhan co the khong route dung.

### Loi 4: Chi khoa `SEND` ma quen `CONNECT` va `SUBSCRIBE`

Neu chi chan luc gui tin nhan ma khong chan luc ket noi hoac subscribe, thi van co the phat sinh hanh vi khong mong muon.

## 11. Kien thuc nay vua duoc ap dung vao file nao?

Neu muon doi chieu ly thuyet voi code, xem:

- `src/main/java/com/backend/old_bicycle_project/security/WebSocketAuthChannelInterceptor.java`
- `src/main/java/com/backend/old_bicycle_project/security/StompUserPrincipal.java`
- `src/main/java/com/backend/old_bicycle_project/config/WebSocketConfig.java`
- `src/main/java/com/backend/old_bicycle_project/controller/ChatController.java`
- `src/main/java/com/backend/old_bicycle_project/service/MessageService.java`
- `src/main/java/com/backend/old_bicycle_project/service/impl/MessageServiceImpl.java`

## 12. Chot lai cho de nho

Neu giai thich ngan gon cho sinh vien nam nhat:

- `payload` la du lieu user muon gui
- `JWT` la cach chung minh user da dang nhap
- `Principal` la danh tinh ma server cong nhan
- backend dung phai lay danh tinh tu `Principal`, khong lay tu payload

Va cau quan trong nhat cua phase nay la:

**Nguoi gui tin nhan phai do server xac dinh, khong phai do client tu khai.**
