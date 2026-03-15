# Payment Flow va Refund Flow Co Ban Cho Nguoi Moi Hoc

Ngay cap nhat: 2026-03-12  
Pham vi: Backend transaction layer cua du an `old_bicycle_project`

## 1. Boc canh cua bai toan

Du an nay la san mua ban xe dap cu. Khi nguoi mua muon chot xe, he thong can co mot quy trinh de:

- tao `order`
- xac dinh nguoi mua can tra truoc bao nhieu tien
- ghi nhan viec da thanh toan
- giu trang thai don hang sau khi da thanh toan
- xu ly hoan tien neu co tranh chap hoac gian lan

Nghe co ve nhieu, nhung neu tach ra thi day chi la bai toan: `ai tra tien`, `tra bao nhieu`, `tra luc nao`, `he thong biet giao dich da thanh cong bang cach nao`, va `neu can hoan tien thi xu ly ra sao`.

## 2. Dinh nghia tung khai niem co ban

### Payment flow la gi?

`Payment flow` la luong xu ly thanh toan.

No tra loi cac cau hoi:

- nguoi dung bat dau thanh toan tu dau
- he thong tao thong tin thanh toan nhu the nao
- khi nao xem la da thanh toan thanh cong
- sau khi thanh cong thi cap nhat database ra sao

Vi du don gian:

1. Ban dat mua mot chiec xe.
2. He thong tao ma thanh toan.
3. Ban chuyen khoan dung so tien va dung noi dung.
4. He thong nhan duoc thong bao giao dich thanh cong.
5. Don hang duoc chuyen sang trang thai da dat coc.

### Refund flow la gi?

`Refund flow` la luong hoan tien.

No tra loi cac cau hoi:

- ai duoc phep yeu cau hoan tien
- trong truong hop nao duoc hoan tien
- ai duyet yeu cau hoan tien
- khi hoan tien xong thi cap nhat order va payment nhu the nao

Vi du:

1. Nguoi mua da dat coc.
2. Phat hien nguoi ban gian lan.
3. Nguoi mua tao `refund request`.
4. Admin xem xet va duyet.
5. Sau khi hoan tien xong, he thong danh dau giao dich la `refunded`.

### Upfront payment la gi?

`Upfront payment` la so tien phai tra ngay luc nay.

Day la khai niem rat quan trong trong phase nay.

Khong nen nghi cung mot kieu la "luc nao cung la dat coc". Vi sau nay co the co 2 lua chon:

- tra truoc mot phan
- tra truoc toan bo

Neu he thong chi nghi theo kieu `depositAmount`, sau nay muon mo rong se kho hon.  
Neu he thong nghi theo kieu `requiredUpfrontAmount`, thi:

- hom nay co the la 2 trieu dat coc
- ngay mai co the la 20 trieu tra full

Code van dung duoc.

### Webhook la gi?

`Webhook` la mot cach de he thong khac chu dong goi ve backend cua minh de bao: "Co su kien vua xay ra".

Trong bai toan nay, su kien do la: `co giao dich thanh toan thanh cong`.

Hieu don gian:

- client khong can ngoi canh canh de hoi "da thanh toan chua?"
- gateway hoac he thong trung gian se tu bao cho backend

Vi du:

1. Buyer chuyen khoan.
2. SePay ghi nhan giao dich.
3. SePay goi vao endpoint webhook cua backend.
4. Backend tim payment record theo `gatewayOrderCode`.
5. Neu hop le thi cap nhat `payment.status = success`.

### Hold funds la gi?

`Hold funds` co the hieu don gian la "tien dang duoc giu lai, chua xem la da giai phong xong cho giao dich".

Trong project nay, sau khi nhan duoc khoan thanh toan upfront:

- order chuyen sang `deposited`
- `fundingStatus` chuyen sang `held`

Dieu nay cho biet:

- tien da vao he thong
- nhung giao dich chua chot xong

Neu sau do co van de, minh con duong di cho refund.

## 3. Tai sao phai tach `Order`, `Payment`, `RefundRequest`?

Day la mot diem nguoi moi hoc rat hay nham.

### `Order`

`Order` la ban ghi nghiep vu mua ban.

No tra loi:

- buyer la ai
- seller la ai
- mua san pham nao
- tong so tien cua don hang la bao nhieu
- don hang dang o trang thai nao

### `Payment`

`Payment` la ban ghi mot lan thanh toan.

No tra loi:

- lan thanh toan nay thuoc order nao
- da tra bao nhieu
- thanh toan theo cong nao
- trang thai giao dich la gi

Mot order co the co nhieu payment.

Vi du:

- lan 1: buyer dat coc 2 trieu
- lan 2: buyer thanh toan phan con lai 18 trieu

Neu gom het vao `Order` thi sau nay rat kho mo rong.

### `RefundRequest`

`RefundRequest` la ban ghi yeu cau hoan tien.

No tra loi:

- ai tao yeu cau hoan tien
- hoan bao nhieu
- ly do la gi
- admin da duyet hay chua
- da hoan tien xong hay chua

Neu khong tach rieng, ban se de bi roi vao tinh huong:

- khong biet ai yeu cau refund
- khong biet lich su xet duyet
- khong biet refund da hoan tat hay moi chi dang cho admin

## 4. Vi sao phase nay tach ro `cash` voi `transfer/online`?

Trong code moi, luong duoc tach nhu sau:

- `cash`: xac nhan thu cong
- `transfer` hoac `online`: tao payment request va cho webhook xac nhan

Tai sao phai lam vay?

Vi neu khong tach, he thong se bi chong cheo:

- vua co the bam tay `confirm deposit`
- vua co the cho webhook auto confirm

Khi do rat de bi:

- double update
- doi trang thai sai
- khong biet lan thanh toan nao la that, lan nao la thu cong

Noi ngan gon: moi cach thanh toan phai co mot "cua vao" ro rang.

## 5. Phase nay da ap dung vao project nhu the nao?

### A. Order duoc nang cap de ho tro partial va full

Trong project, `Order` da co them cac field de dien dat tien trang thai giao dich:

- `requiredUpfrontAmount`
- `paidAmount`
- `remainingAmount`
- `paymentOption`
- `fundingStatus`
- `acceptedAt`
- `paymentDeadline`

Y nghia don gian:

- `requiredUpfrontAmount`: bay gio can thu bao nhieu
- `paidAmount`: hien tai da ghi nhan bao nhieu
- `remainingAmount`: con lai bao nhieu
- `paymentOption`: buyer chon `partial` hay `full`
- `fundingStatus`: tien dang `unpaid`, `awaiting_payment`, `held`, `refunded`...

### B. Payment request duoc tao rieng

Backend da co service tao `payment request`.

No se tra ve:

- so tien can thanh toan
- ma giao dich
- noi dung chuyen khoan
- thong tin ngan hang
- QR code de buyer quet

Day la mot diem thuc te va rat quan trong: frontend khong tu nghi ra so tien va noi dung chuyen khoan. Backend phai tao ra de tranh sai lech.

### C. Webhook se xac nhan giao dich

Khi webhook hop le:

- payment chuyen sang `success`
- order cap nhat `paidAmount`
- neu da du upfront thi order chuyen sang `deposited`
- `fundingStatus` chuyen sang `held`

Noi cach khac, webhook la "bang chung ky thuat" de he thong biet tien da vao.

### D. Refund phase 1 duoc gioi han cho de dung

Phase nay chu y mot quyet dinh thiet ke:

- refund chi xu ly theo huong `full refund`

Tai sao?

Vi partial refund se can nhieu rule hon:

- hoan mot phan nao
- order co tiep tuc duoc hay khong
- `remainingAmount` tinh lai ra sao

De tranh logic nua voi nua, phase 1 chon cach don gian va chac chan hon:

- neu refund, xem nhu huy giao dich dang giu tien
- sau khi admin hoan tat refund:
  - `payment.status = refunded`
  - `order.status = cancelled`
  - `fundingStatus = refunded`

## 6. Vi du cu the de de hinh dung

### Vi du 1: Buyer dat coc mot phan

Xe co gia: `10,000,000`

Buyer chon:

- `paymentOption = partial`
- `requiredUpfrontAmount = 2,000,000`

Ket qua:

1. Seller chap nhan order.
2. He thong tao payment request 2 trieu.
3. Buyer chuyen khoan 2 trieu.
4. Webhook ve thanh cong.
5. Order chuyen sang `deposited`, `fundingStatus = held`.

### Vi du 2: Buyer tra full ngay tu dau

Xe co gia: `10,000,000`

Buyer chon:

- `paymentOption = full`
- `requiredUpfrontAmount = 10,000,000`

Ket qua:

Flow khong doi. Chi khac so tien can thu luc dau la 10 trieu.

Day chinh la ly do minh dung khai niem `upfront amount` thay vi nghi cung mot kieu la dat coc.

### Vi du 3: Refund vi seller gian lan

Buyer da thanh toan upfront thanh cong.

Sau do:

1. Buyer phat hien mo ta xe khong dung thuc te.
2. Buyer tao `refund request`.
3. Admin xem xet.
4. Admin danh dau `approved`.
5. Sau khi chuyen tien hoan lai xong, admin danh dau `completed`.

Ket qua:

- payment -> `refunded`
- order -> `cancelled`
- funding -> `refunded`

## 7. Nhung loi nguoi moi hoc rat hay gap

### Loi 1: Nghi rang `Order` la du

Sai vi `Order` chi mo ta giao dich mua ban.  
No khong du tot de luu lich su thanh toan va lich su hoan tien.

### Loi 2: Nghi rang payment thanh cong la chi can frontend bao lai

Sai. Frontend co the bi gia mao hoac goi sai.  
Backend can co mot nguon xac nhan dang tin cay hon, va webhook la mot cach pho bien.

### Loi 3: Trộn manual flow va online flow

Neu khong tach ro:

- cash van co the auto webhook
- transfer van co the bam tay confirm

Thi business rule se rat roi.

### Loi 4: Co amount refund la nghi co partial refund ngay

Khong dung.  
Cho phep nhap amount khong co nghia la logic da san sang cho moi kieu refund.

Trong phase 1, project nay chon cach an toan hon:

- chi di theo full refund
- de partial refund cho phase sau

## 8. Kien thuc nay vua duoc ap dung vao file nao?

Neu muon doc code de doi chieu voi ly thuyet, co the xem:

- `src/main/java/com/backend/old_bicycle_project/entity/Order.java`
- `src/main/java/com/backend/old_bicycle_project/entity/Payment.java`
- `src/main/java/com/backend/old_bicycle_project/entity/RefundRequest.java`
- `src/main/java/com/backend/old_bicycle_project/service/impl/OrderServiceImpl.java`
- `src/main/java/com/backend/old_bicycle_project/service/impl/PaymentServiceImpl.java`
- `src/main/java/com/backend/old_bicycle_project/service/impl/RefundServiceImpl.java`
- `src/main/resources/db/migration/V5__payment_refund_upgrade.sql`

## 9. Chot lai bang ngon ngu that de nho

Neu giai thich cho mot ban nam nhat:

- `Order` la don mua ban
- `Payment` la mot lan dong tien
- `RefundRequest` la mot yeu cau xin hoan tien
- `Webhook` la cach he thong ben ngoai tu bao ve cho backend
- `Upfront amount` la so tien phai tra luc nay, co the la mot phan hoac toan bo

Va bai hoc quan trong nhat trong phase nay la:

**Dung thiet ke theo y nghia nghiep vu rong hon mot chut o hien tai, thi sau nay se de mo rong hon rat nhieu.**
