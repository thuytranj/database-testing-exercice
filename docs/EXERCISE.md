# Hướng dẫn chi tiết bài thực hành (Exercise Guide)

**Tiêu đề:** Generate, Test & Fix – Database Testing Challenge  
**Thời gian dự kiến:** 15–20 phút (không tính cài đặt môi trường và nộp bài)

---

## Yêu cầu nghiệp vụ hệ thống

Bảng `users` lưu trữ thông tin đăng ký người dùng với các ràng buộc sau:

| Cột | Kiểu dữ liệu | Yêu cầu nghiệp vụ |
|---|---|---|
| `id` | Integer | Khóa chính tự động tăng |
| `username` | `VARCHAR(20)` | Bắt buộc (`NOT NULL`), độ dài từ 3 đến 20 ký tự |
| `email` | `VARCHAR(100)` | Bắt buộc (`NOT NULL`), không được trùng lặp (`UNIQUE`) |
| `age` | Integer | Bắt buộc (`NOT NULL`), giá trị hợp lệ từ 18 đến 60 (bao gồm cả 18 và 60) |

---

## Lộ trình học tập & Phân biệt thủ công vs. tự động

Bài thực hành gồm 4 nhiệm vụ nối tiếp nhau:
1. **Task 1 (DBUnit XML Setup):** Bạn **tự chuẩn bị thủ công** dữ liệu khởi tạo dưới dạng Flat XML (`initial-users.xml`). Bộ test tự động nạp XML vào PostgreSQL bằng `DatabaseOperation.CLEAN_INSERT` và kiểm tra độc lập từng bản ghi.
2. **Task 2 (DBUnit State Verification):** Bạn đọc các thao tác CRUD đã viết sẵn trong `DBUnitExpectedStateTest.java`, **tự dự đoán** trạng thái bảng sau khi chạy CRUD và ghi vào `expected-users.xml`. Bộ test tự động nạp lại `initial-users.xml`, chạy CRUD và dùng `Assertion.assertEquals` so sánh bảng kỳ vọng với bảng thực tế.
3. **Task 3 (Automated Data Generation):** Bạn hoàn thành logic sinh `email` và `age` bằng **Datafaker** (seed cố định `42L`) để sinh tự động 10 user hợp lệ và đối chiếu với bảng DBUnit dựng trong bộ nhớ (`DefaultTable`).
4. **Task 4 (Boundary Testing, Bug Hunting & Fix):** Bạn thiết kế các ca kiểm thử biên (**BVA**), chạy toàn bộ bộ test để phát hiện **1 lỗi schema chủ đích**, sửa file migration SQL và chạy lại kiểm thử hồi quy.

---

## Hạ tầng đã được chuẩn bị sẵn

Bạn **không cần** chỉnh sửa các phần hạ tầng sau:
- [BaseDatabaseTest.java](../src/test/java/com/example/dbtesting/support/BaseDatabaseTest.java): Tự động khởi chạy PostgreSQL qua Testcontainers, chạy Flyway migration, dọn sạch dữ liệu (`TRUNCATE ... RESTART IDENTITY`) trước mỗi test case để đảm bảo **test isolation**, đồng bộ sequence (`syncUserIdSequence`), và cung cấp các hàm tiện ích DBUnit.
- [User.java](../src/main/java/com/example/dbtesting/model/User.java) & [UserRepository.java](../src/main/java/com/example/dbtesting/repository/UserRepository.java): Tầng model và JDBC repository sử dụng `PreparedStatement` (hỗ trợ `insert`, `findByEmail`, `findAll`, `updateAgeByEmail`, `deleteByEmail`).
- [SmokeTest.java](../src/test/java/com/example/dbtesting/SmokeTest.java): Kiểm tra kết nối Docker, PostgreSQL và Flyway.
- [DBUnitSeedTest.java](../src/test/java/com/example/dbtesting/DBUnitSeedTest.java) & [DBUnitExpectedStateTest.java](../src/test/java/com/example/dbtesting/DBUnitExpectedStateTest.java): Đã cài đặt sẵn toàn bộ logic nạp XML, chạy CRUD và đối chiếu DBUnit cho Task 1 và Task 2.
- [existing-users.xml](../src/test/resources/datasets/existing-users.xml): Dữ liệu nền được nạp trước mỗi test case trong `UserConstraintTest`.

---

## Làm quen nhanh với DBUnit Flat XML

Trong DBUnit **Flat XML Dataset**, thẻ gốc là `<dataset>`, mỗi thẻ con đại diện cho **1 dòng** trong bảng có tên trùng với tên thẻ, và mỗi thuộc tính là **1 cột**.

Ví dụ minh họa (cho một bảng `products` không liên quan đến bài tập):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<dataset>
    <products id="1" name="Laptop" price="1500"/>
    <products id="2" name="Mouse" price="25"/>
</dataset>
```

---

## Task 1 – DBUnit Data Setup (3 phút)

### Mục tiêu
Thực hành chuẩn bị dữ liệu kiểm thử thủ công bằng DBUnit Flat XML Dataset.

### File cần chỉnh sửa
[initial-users.xml](../src/test/resources/datasets/initial-users.xml) *(không cần sửa file Java)*

### Yêu cầu dữ liệu cần khai báo (`TODO 1.1`)
Khai báo đúng 3 bản ghi trong bảng `users` với dữ liệu sau:

| `id` | `username` | `email` | `age` |
|---|---|---|---|
| `1` | `alice` | `alice@example.com` | `20` |
| `2` | `bob` | `bob@example.com` | `25` |
| `3` | `charlie` | `charlie@example.com` | `30` |

### Những gì đã có sẵn vs. Bạn cần làm
- **Đã có sẵn:** `DBUnitSeedTest.java` đã cài đặt sẵn việc đọc XML bằng `FlatXmlDataSetBuilder`, nạp vào PostgreSQL qua `DatabaseOperation.CLEAN_INSERT`, và kiểm tra độc lập số dòng cũng như từng cột của cả 3 bản ghi.
- **Bạn cần làm:** Điền 3 thẻ `<users .../>` hợp lệ vào `initial-users.xml`.

<details>
<summary>Gợi ý từng bước (nhấn để xem nếu cần)</summary>

1. Giữ nguyên dòng khai báo `<?xml version="1.0" encoding="UTF-8"?>` và thẻ bao `<dataset>...</dataset>`.
2. Mỗi dòng dùng một thẻ tự đóng `<users id="..." username="..." email="..." age="..."/>`.
3. Kiểm tra chính xác chính tả `username`, `email` và giá trị số của `id`, `age`.

</details>

### Lệnh chạy & Kết quả mong đợi
```bash
./mvnw -Dtest=DBUnitSeedTest test
```
- **Kết quả mong đợi:** `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0` (`BUILD SUCCESS`).

---

## Task 2 – DBUnit State Verification (4 phút)

### Mục tiêu
Đọc hiểu các thao tác CRUD trên dữ liệu nền, dự đoán trạng thái cuối cùng của cơ sở dữ liệu và viết file Expected XML để DBUnit đối chiếu tự động.

### File cần chỉnh sửa
[expected-users.xml](../src/test/resources/datasets/expected-users.xml) *(không cần sửa file Java)*

### Những gì đã có sẵn vs. Bạn cần làm
- **Đã có sẵn:** `DBUnitExpectedStateTest.java` tự động nạp lại `initial-users.xml` (độc lập với Task 1), đồng bộ PostgreSQL sequence, thực thi 3 thao tác CRUD theo thứ tự:
  1. Xóa **Bob** (`bob@example.com`).
  2. Cập nhật tuổi của **Alice** (`alice@example.com`) thành `22`.
  3. Thêm mới **David** (`username="david"`, `email="david@example.com"`, `age=28`).
  Sau đó test nạp `expected-users.xml`, lấy bảng `users` thực tế từ PostgreSQL, loại trừ cột `id`, sắp xếp cả hai bảng theo cột `email` và gọi `Assertion.assertEquals`.
- **Bạn cần làm (`TODO 2.1`):** Mở [DBUnitExpectedStateTest.java](../src/test/java/com/example/dbtesting/DBUnitExpectedStateTest.java) để xem chính xác các thao tác CRUD, tự suy luận các dòng còn lại trong bảng `users` sau khi thực thi, và khai báo chúng vào `expected-users.xml`.

<details>
<summary>Gợi ý từng bước (nhấn để xem nếu cần)</summary>

1. Bắt đầu từ 3 dòng của Task 1 (`alice`, `bob`, `charlie`), lần lượt áp dụng từng thao tác: xóa, cập nhật, thêm mới.
2. Trong `expected-users.xml`, bạn có thể bỏ qua thuộc tính `id` (hoặc giữ `id` bất kỳ) vì test đã loại trừ cột `id` trước khi so sánh.
3. Thứ tự các thẻ `<users .../>` trong XML không quan trọng vì test đã sắp xếp theo `email`.

</details>

### Lệnh chạy & Kết quả mong đợi
```bash
./mvnw -Dtest=DBUnitExpectedStateTest test
```
- **Kết quả mong đợi:** `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0` (`BUILD SUCCESS`).

---

## Task 3 – Data Generation (4 phút)

### Mục tiêu
Sinh 10 người dùng hợp lệ, có tính tái lập (reproducible) bằng **Datafaker**, lưu vào PostgreSQL qua JDBC và đối chiếu toàn bộ dữ liệu thực tế với dữ liệu kỳ vọng bằng **DBUnit** (bảng kỳ vọng dựng trong bộ nhớ).

### File cần chỉnh sửa
[DataGenerationTest.java](../src/test/java/com/example/dbtesting/DataGenerationTest.java)

### Những gì đã có sẵn vs. Bạn cần làm
- **Đã có sẵn:** Khởi tạo `Faker` với fixed seed `42L`, hàm sinh `username` hợp lệ (3–20 ký tự), hàm kiểm tra độc lập (independent validation), lưu 10 user qua `userRepository.insert(...)`, dựng bảng kỳ vọng trong bộ nhớ (`DefaultTable`) và so sánh bằng DBUnit (`assertUsersTableEquals` loại trừ cột `id` và sắp xếp theo `email`).
- **Bạn cần làm:**
  - `TODO 3.1`: Sinh địa chỉ `email` **duy nhất** (unique) bằng Datafaker.
  - `TODO 3.2`: Sinh `age` nằm trong khoảng hợp lệ theo yêu cầu nghiệp vụ bằng Datafaker.

### Kiến thức & API tham khảo
- **Fixed seed (`new Random(42L)`):** Giúp Datafaker luôn sinh ra cùng một chuỗi dữ liệu qua mỗi lần chạy, đảm bảo kết quả kiểm thử có thể tái lập và dễ debug.
- **Uniqueness:** Khi sinh dữ liệu ngẫu nhiên lặp lại nhiều lần, hai lần gọi `faker.internet().emailAddress()` vẫn có xác suất trùng nhau hoặc quá dài. Kết hợp thêm biến đếm `i` (ví dụ tiền tố `i + "_"`) sẽ đảm bảo tính duy nhất tuyệt đối.
- **API Datafaker:**
  - `faker.internet().emailAddress()` → trả về một chuỗi email ngẫu nhiên.
  - `faker.number().numberBetween(int minInclusive, int maxExclusive)` → trả về số nguyên trong khoảng `[minInclusive, maxExclusive)` (lưu ý cận trên là **exclusive**).

<details>
<summary>Gợi ý từng bước (nhấn để xem nếu cần)</summary>

1. Cho `TODO 3.1`: Bạn có thể ghép chỉ số vòng lặp `i` với `faker.internet().emailAddress()` (ví dụ `i + "_" + ...`) để đảm bảo không bao giờ trùng lặp.
2. Cho `TODO 3.2`: Vì `numberBetween(min, max)` không bao gồm `max`, hãy chọn `min` và `max` sao cho giá trị trả về nằm gọn trong khoảng tuổi hợp lệ của nghiệp vụ.

</details>

### Lệnh chạy & Kết quả mong đợi
```bash
./mvnw -Dtest=DataGenerationTest test
```
- **Kết quả mong đợi:** `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0` (`BUILD SUCCESS`).

---

## Task 4 – Boundary Testing, Bug Hunting & Fix (6 phút)

### Mục tiêu
Áp dụng kỹ thuật **Boundary Value Analysis (BVA)** cho trường `age` cùng JUnit 5 `@ParameterizedTest`, phát hiện lỗi schema chủ đích, sửa file migration và chạy kiểm thử hồi quy toàn bộ hệ thống.

### Các file cần chỉnh sửa
1. [UserConstraintTest.java](../src/test/java/com/example/dbtesting/UserConstraintTest.java)
2. [V1__create_users_table.sql](../src/main/resources/db/migration/V1__create_users_table.sql)

### Phần A – Hoàn thành BVA & Săn lỗi (`UserConstraintTest.java`)
- **Đã có sẵn:**
  - Nạp dữ liệu nền từ `existing-users.xml` trước mỗi lượt chạy (`@BeforeEach`).
  - Các test case mẫu `TC01` (tuổi hợp lệ), `TC06` (trùng email), `TC07` (email `NULL`).
  - Toàn bộ logic thực thi `userRepository.insert(...)`, kiểm tra bản ghi được lưu khi `ACCEPT`, kiểm tra `SQLException` + PostgreSQL `SQLSTATE` (`23514` cho CHECK, `23505` cho UNIQUE, `23502` cho NOT NULL) và kiểm tra bảo toàn dữ liệu cũ bằng DBUnit khi `REJECT`.
  - Test kiểm tra tính đầy đủ (`verifyRequiredBoundaryTestCasesPresent`) để đảm bảo bạn không bỏ sót hoặc ghi sai các giá trị biên.
- **Bạn cần làm:**
  - `TODO 4.1`: Xác định 4 giá trị biên (cận dưới không hợp lệ, cận dưới hợp lệ, cận trên hợp lệ, cận trên không hợp lệ) thay cho `-1` trong `@CsvSource` cho `TC02`–`TC05`.
  - `TODO 4.2`: Xác định kết quả mong đợi (`ACCEPT` hoặc `REJECT`) thay cho `TODO` tương ứng với `TC02`–`TC05`.

<details>
<summary>Gợi ý từng bước cho BVA (nhấn để xem nếu cần)</summary>

1. Với miền giá trị hợp lệ $[A, B]$, kỹ thuật BVA chuẩn kiểm tra 4 điểm biên: $A - 1$, $A$, $B$, $B + 1$.
2. Trong `@CsvSource`, mỗi dòng có định dạng: `"TCxx, <tuổi>, <email>, <ACCEPT hoặc REJECT>"`.

</details>

1. Chạy toàn bộ test suite:
   ```bash
   ./mvnw test
   ```
2. Quan sát kết quả: `verifyRequiredBoundaryTestCasesPresent` sẽ `PASS`, và sẽ có **đúng 1 test case bị `FAIL`** trong `UserConstraintTest` (`Tests run: 13, Failures: 1, Errors: 0, Skipped: 0`).
3. **Chụp ảnh màn hình** kết quả thất bại (bao gồm tên test case bị FAIL và dòng tổng kết) và lưu thành `before_fix.png`.

### Phần B – Sửa lỗi Schema & Kiểm thử hồi quy (`V1__create_users_table.sql`)
1. Mở [V1__create_users_table.sql](../src/main/resources/db/migration/V1__create_users_table.sql), đối chiếu định nghĩa bảng với yêu cầu nghiệp vụ ở đầu bài để tìm ràng buộc bị thiếu/sai.
2. Chỉnh sửa `V1__create_users_table.sql` sao cho khớp hoàn toàn với yêu cầu nghiệp vụ.
3. Chạy lại toàn bộ test suite:
   ```bash
   ./mvnw test
   ```
4. Kiểm tra kết quả: tất cả **13** test đều `PASS` (`Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`).
5. **Chụp ảnh màn hình** kết quả thành công và lưu thành `after_fix.png`.

> **Tại sao trong bài tập này chúng ta sửa trực tiếp file `V1`?**
> Trong môi trường test tự động với Testcontainers, mỗi lần chạy `./mvnw test` sẽ khởi tạo một container PostgreSQL hoàn toàn mới và chạy lại Flyway từ đầu, nên việc sửa trực tiếp `V1` hoạt động sạch sẽ. Tuy nhiên, **trong hệ thống thực tế đã triển khai (production)**, bạn tuyệt đối không sửa file migration đã chạy mà phải tạo một file migration mới (ví dụ `V2__...sql`) để `ALTER TABLE`.

---

## Quy trình nộp bài & Checklist

1. Tạo bản sao thư mục `submit/MSSV/` và đổi tên `MSSV` thành mã số sinh viên của bạn.
2. Copy **5 file bài làm** vào thư mục `<MSSV>/`:
   - `initial-users.xml`
   - `expected-users.xml`
   - `DataGenerationTest.java`
   - `UserConstraintTest.java`
   - `V1__create_users_table.sql`
3. Đặt **2 ảnh chụp màn hình** vào `<MSSV>/`:
   - `before_fix.png`
   - `after_fix.png`
4. Điền đầy đủ nội dung vào `REPORT.md` trong `<MSSV>/`.
5. Kiểm tra lại thư mục `<MSSV>/` có **đúng 8 file**, sau đó nén thành `<MSSV>.zip`.

**Checklist trước khi nộp:**
- [ ] `DBUnitSeedTest` chạy `PASS` độc lập (Task 1).
- [ ] `DBUnitExpectedStateTest` chạy `PASS` độc lập (Task 2).
- [ ] `DataGenerationTest` chạy `PASS` độc lập (Task 3).
- [ ] `before_fix.png` thể hiện rõ 1 test case `FAIL` trước khi sửa schema (Task 4).
- [ ] `after_fix.png` thể hiện rõ `Tests run: 13, Failures: 0, Errors: 0, Skipped: 0` (Task 4).
- [ ] `REPORT.md` đã điền đầy đủ MSSV, họ tên, phân tích lỗi và kết quả.
- [ ] File `<MSSV>.zip` chỉ chứa 8 file theo đúng cấu trúc yêu cầu.
