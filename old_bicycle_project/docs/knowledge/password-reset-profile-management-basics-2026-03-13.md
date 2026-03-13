# Password Reset Va Profile Management Cho Nguoi Moi Hoc

Ngay cap nhat: 2026-03-13  
Pham vi: Cac chuc nang account management moi duoc them vao backend

## 1. Boc canh

Trong mot he thong co dang nhap, chi co `register` va `login` la chua du.

Nguoi dung thuc te se gap cac tinh huong nhu:

- quen mat khau
- muon doi mat khau
- muon cap nhat so dien thoai
- muon doi avatar
- muon cap nhat dia chi mac dinh

Neu backend khong co cac chuc nang nay, thi tai khoan tuy dang nhap duoc nhung van chua duoc xem la hoan chinh.

## 2. Dinh nghia can biet

### Password reset la gi?

`Password reset` la luong dat lai mat khau khi nguoi dung khong con nho mat khau cu.

Day khac voi `change password`.

- `change password`: user dang dang nhap va biet mat khau cu
- `reset password`: user quen mat khau va can mot cach an toan de dat lai

### Profile management la gi?

`Profile management` la nhom chuc nang cho phep user cap nhat thong tin ca nhan.

Trong project nay, nhung thong tin do la:

- ho
- ten
- so dien thoai
- avatar
- dia chi mac dinh

### Reset token la gi?

`Reset token` la mot ma tam thoi do backend tao ra de xac nhan rang yeu cau dat lai mat khau la hop le.

Co the hieu don gian:

- user bao "toi quen mat khau"
- backend tao mot ma tam thoi
- backend gui ma nay qua email
- khi user bam vao link reset, backend kiem tra ma do con hop le khong

## 3. Vi sao khong dat lai mat khau ngay khi nguoi dung nhap email?

Vi neu chi can biet email la doi duoc mat khau, thi bat ky ai biet email cua ban deu co the chiem tai khoan.

Cho nen backend phai co them mot bang chung nua.  
Bang chung o phase nay la: `password reset token`.

Flow dung la:

1. User nhap email.
2. Backend tao token tam thoi.
3. Backend gui token do qua email.
4. User mo link reset.
5. Backend kiem tra token.
6. Neu token hop le thi moi cho doi mat khau.

## 4. Tai sao can bang `password_reset_tokens` rieng?

Day la mot diem rat quan trong.

Project da co:

- `email_verifications`
- `refresh_tokens`

Nhung khong nen dung chung chung mot bang cho tat ca.

Tai sao?

Vi moi loai token co y nghia khac nhau:

- `email_verification`: xac thuc email
- `refresh_token`: gia han dang nhap
- `password_reset_token`: dat lai mat khau

Neu gom chung lai, code se rat de roi:

- token nay dung cho viec gi
- luc nao het han
- xoa theo rule nao

Tach rieng bang se de hieu va de bao tri hon.

## 5. Password policy la gi?

`Password policy` la tap cac quy tac bat buoc cua mat khau.

Trong phase nay, backend enforce:

- toi thieu 8 ky tu
- co it nhat 1 chu hoa
- co it nhat 1 chu so

Vi du:

- `abc12345` -> sai, vi khong co chu hoa
- `Abcdefgh` -> sai, vi khong co so
- `Abcd1234` -> dung

Day la mot quy tac don gian, nhung tot hon rat nhieu so voi chi kiem tra do dai.

## 6. Tai sao cung mot password policy phai dung o nhieu noi?

Nguoi moi hoc hay mac loi nay:

- register co mot rule
- reset password lai mot rule khac
- change password lai mot rule khac nua

Ket qua la he thong khong dong nhat.

Dung hon la:

- register phai dung cung policy
- reset password phai dung cung policy
- change password phai dung cung policy

Noi cach khac, da goi la "chuan mat khau" thi phai dung lai o moi diem thay doi mat khau.

## 7. Phase nay da ap dung nhu the nao?

### A. Them forgot password

Backend them endpoint:

- `POST /api/auth/forgot-password`

Nhiem vu cua endpoint nay:

- nhan email
- neu user ton tai thi tao reset token
- gui email reset
- van tra thong bao chung chung

Thong bao chung chung rat quan trong.

Tai sao?

Neu backend tra:

- "email ton tai"
- "email khong ton tai"

thi nguoi xau co the dung endpoint nay de doan xem email nao co tai khoan trong he thong.

Cho nen backend tra mot cau chung:

- `Neu email ton tai, he thong da gui huong dan dat lai mat khau.`

Day la cach giam `user enumeration`.

### B. Them reset password

Backend them endpoint:

- `POST /api/auth/reset-password`

Endpoint nay:

- nhan `token`
- nhan `newPassword`
- kiem tra token co ton tai khong
- kiem tra token con han khong
- kiem tra password policy
- doi mat khau moi

### C. Thu hoi refresh token sau khi doi/reset mat khau

Day la mot rule bao mat tot va rat nen co.

Vi du:

1. User dang login tren 3 thiet bi.
2. User reset password vi nghi tai khoan co van de.
3. Neu refresh token cu van con song, cac session cu van co the tiep tuc hoat dong.

Cho nen phase nay backend da:

- xoa refresh token sau khi `reset password`
- xoa refresh token sau khi `change password`

Dieu nay buoc user dang nhap lai bang mat khau moi.

## 8. Change password khac reset password nhu the nao?

### Change password

Ap dung khi:

- user dang dang nhap
- user biet mat khau hien tai

Flow:

1. nhap current password
2. nhap new password
3. backend so khop current password
4. neu dung moi cho doi

### Reset password

Ap dung khi:

- user quen mat khau
- user khong dang dang nhap hoac khong dung duoc mat khau cu

Flow:

1. xin reset bang email
2. nhan token
3. dung token de dat lai mat khau

## 9. Profile update trong phase nay lam gi?

Backend them endpoint:

- `PATCH /api/auth/profile`

Endpoint nay cho phep cap nhat:

- `firstName`
- `lastName`
- `phone`
- `avatarUrl`
- `defaultAddress`

Va `GET /api/auth/me` cung duoc mo rong de tra ve cac field nay, de frontend co the hien thi profile day du.

## 10. Vi du cu the

### Vi du 1: Quen mat khau

1. User nhap `lan@example.com`.
2. Backend tao `reset-token-123`.
3. Backend gui email co link:

```text
http://frontend/reset-password?token=reset-token-123
```

4. User mo link, nhap mat khau moi `StrongPass1`.
5. Backend kiem tra token va doi mat khau.

### Vi du 2: Doi mat khau khi dang dang nhap

1. User dang dang nhap.
2. Goi `PATCH /api/auth/change-password`.
3. Gui:

```json
{
  "currentPassword": "OldPass1",
  "newPassword": "FreshPass2"
}
```

4. Backend kiem tra `OldPass1` co dung khong.
5. Neu dung, backend luu mat khau moi va thu hoi refresh token cu.

### Vi du 3: Cap nhat profile

```json
{
  "firstName": "Mai",
  "lastName": "Nguyen",
  "phone": "0988111222",
  "avatarUrl": "https://cdn.example/avatar.png",
  "defaultAddress": "123 Nguyen Trai"
}
```

Backend se cap nhat cac truong nay cho user dang dang nhap.

## 11. Nhung loi nguoi moi hoc hay gap

### Loi 1: Nghi reset password va change password la mot

Khong dung.  
Mot cai can current password.  
Mot cai can reset token.

### Loi 2: Dung chung token xac thuc email cho reset password

Khong nen.  
Moi token phuc vu mot muc dich khac nhau.

### Loi 3: Sau khi doi mat khau ma van de refresh token cu song

Day la lo hong bao mat.

Neu password da doi ma session cu van song, thi tai khoan van co nguy co bi dung tiep.

### Loi 4: Password policy chi check o register

Sai.  
Neu reset password khong check cung policy, user van co the quay lai dung mat khau yeu.

### Loi 5: Endpoint forgot-password de lo email nao ton tai

Neu backend tra ve thong bao khac nhau cho email co/khong ton tai, thi nguoi xau co the do danh sach tai khoan.

## 12. Kien thuc nay vua duoc ap dung vao file nao?

Neu muon doi chieu ly thuyet voi code, xem:

- `src/main/java/com/backend/old_bicycle_project/controller/AuthController.java`
- `src/main/java/com/backend/old_bicycle_project/service/AuthService.java`
- `src/main/java/com/backend/old_bicycle_project/service/EmailService.java`
- `src/main/java/com/backend/old_bicycle_project/entity/PasswordResetToken.java`
- `src/main/java/com/backend/old_bicycle_project/repository/PasswordResetTokenRepository.java`
- `src/main/resources/db/migration/V6__password_reset_tokens.sql`

## 13. Chot lai cho de nho

Neu giai thich ngan gon cho sinh vien nam nhat:

- `forgot password` la xin mot quyen duoc dat lai mat khau
- `reset token` la bang chung tam thoi cho quyen do
- `reset password` la thuc su doi mat khau bang token
- `change password` la doi mat khau khi van biet mat khau cu
- `profile management` la cap nhat thong tin ca nhan

Va cau quan trong nhat cua phase nay la:

**Account management dung nghia khong chi la dang nhap duoc, ma la phai tu phuc hoi va cap nhat tai khoan duoc mot cach an toan.**
