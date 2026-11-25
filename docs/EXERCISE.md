# Hướng dẫn chi tiết bài thực hành (Exercise Guide)

**Tiêu đề:** Generate, Test & Fix – Database Testing Challenge  
**Thời gian dự kiến:** 10–15 phút

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

## Hạ tầng đã được chuẩn bị sẵn

Bạn **không cần** chỉnh sửa các phần hạ tầng sau:
- [BaseDatabaseTest.java](../src/test/java/com/example/dbtesting/support/BaseDatabaseTest.java): Tự động khởi chạy PostgreSQL qua Testcontainers, chạy Flyway migration, dọn sạch dữ liệu (`TRUNCATE ... RESTART IDENTITY`) trước mỗi test case để đảm bảo **test isolation**, và cung cấp các hàm tiện ích cho DBUnit.
- [User.java](../src/main/java/com/example/dbtesting/model/User.java) & [UserRepository.java](../src/main/java/com/example/dbtesting/repository/UserRepository.java): Tầng model và JDBC repository sử dụng `PreparedStatement`.
- [SmokeTest.java](../src/test/java/com/example/dbtesting/SmokeTest.java): Kiểm tra kết nối Docker, PostgreSQL và Flyway.
- [existing-users.xml](../src/test/resources/datasets/existing-users.xml): Dữ liệu nền được nạp trước mỗi test case trong `UserConstraintTest`.

---

## Task 1 – Data Generation (3 phút)

### Mục tiêu
Sinh 10 người dùng hợp lệ, có tính tái lập (reproducible) bằng **Datafaker**, lưu vào PostgreSQL qua JDBC và đối chiếu toàn bộ dữ liệu thực tế với dữ liệu kỳ vọng bằng **DBUnit**.

### File cần chỉnh sửa
[DataGenerationTest.java](../src/test/java/com/example/dbtesting/DataGenerationTest.java)

### Những gì đã có sẵn vs. Bạn cần làm
- **Đã có sẵn:** Khởi tạo `Faker` với fixed seed `42L`, hàm sinh `username` hợp lệ (3–20 ký tự), hàm kiểm tra độc lập (independent validation), lưu 10 user qua `userRepository.insert(...)`, dựng bảng kỳ vọng trong bộ nhớ (`DefaultTable`) và so sánh bằng DBUnit (`assertUsersTableEquals` loại trừ cột `id` và sắp xếp theo `email`).
- **Bạn cần làm:**
  - `TODO 1.1`: Sinh địa chỉ `email` **duy nhất** (unique) bằng Datafaker.
  - `TODO 1.2`: Sinh `age` nằm trong khoảng hợp lệ theo yêu cầu nghiệp vụ bằng Datafaker.

### Kiến thức & API tham khảo
- **Fixed seed (`new Random(42L)`):** Giúp Datafaker luôn sinh ra cùng một chuỗi dữ liệu qua mỗi lần chạy, đảm bảo kết quả kiểm thử có thể tái lập và dễ debug.
- **Uniqueness:** Khi sinh dữ liệu ngẫu nhiên lặp lại nhiều lần, hai lần gọi `faker.internet().emailAddress()` vẫn có xác suất trùng nhau hoặc quá dài. Kết hợp thêm biến đếm `i` (ví dụ tiền tố `i + "_"`) sẽ đảm bảo tính duy nhất tuyệt đối.
- **API Datafaker:**
  - `faker.internet().emailAddress()` → trả về một chuỗi email ngẫu nhiên.
  - `faker.number().numberBetween(int minInclusive, int maxExclusive)` → trả về số nguyên trong khoảng `[minInclusive, maxExclusive)` (lưu ý cận trên là **exclusive**).

<details>
<summary>Gợi ý từng bước (nhấn để xem nếu cần)</summary>

1. Cho `TODO 1.1`: Bạn có thể ghép chỉ số vòng lặp `i` với `faker.internet().emailAddress()` (ví dụ `i + "_" + ...`) để đảm bảo không bao giờ trùng lặp.
2. Cho `TODO 1.2`: Vì `numberBetween(min, max)` không bao gồm `max`, hãy chọn `min` và `max` sao cho giá trị trả về nằm gọn trong khoảng tuổi hợp lệ của nghiệp vụ.

</details>

### Lệnh chạy & Kết quả mong đợi
```bash
./mvnw -Dtest=DataGenerationTest test
```
- **Kết quả mong đợi:** `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0` (`BUILD SUCCESS`).

---

## Task 2 – Database Testing với BVA (4 phút)

### Mục tiêu
Áp dụng kỹ thuật **Boundary Value Analysis (BVA)** cho trường `age` cùng JUnit 5 `@ParameterizedTest` để kiểm tra xem cơ sở dữ liệu có thực thi đúng các ràng buộc (`CHECK`, `UNIQUE`, `NOT NULL`) hay không.

### File cần chỉnh sửa
[UserConstraintTest.java](../src/test/java/com/example/dbtesting/UserConstraintTest.java)

### Những gì đã có sẵn vs. Bạn cần làm
- **Đã có sẵn:**
  - Nạp dữ liệu nền từ `existing-users.xml` trước mỗi lượt chạy (`@BeforeEach`).
  - Các test case mẫu `TC01` (tuổi hợp lệ), `TC06` (trùng email), `TC07` (email `NULL`).
  - Toàn bộ logic thực thi `userRepository.insert(...)`, kiểm tra bản ghi được lưu khi `ACCEPT`, kiểm tra `SQLException` + PostgreSQL `SQLSTATE` (`23514` cho CHECK, `23505` cho UNIQUE, `23502` cho NOT NULL) và kiểm tra bảo toàn dữ liệu cũ bằng DBUnit khi `REJECT`.
  - Test kiểm tra tính đầy đủ (`verifyRequiredBoundaryTestCasesPresent`) để đảm bảo bạn không bỏ sót hoặc ghi sai các giá trị biên.
- **Bạn cần làm:**
  - `TODO 2.1`: Xác định 4 giá trị biên (cận dưới không hợp lệ, cận dưới hợp lệ, cận trên hợp lệ, cận trên không hợp lệ) thay cho `-1` trong `@CsvSource` cho `TC02`–`TC05`.
  - `TODO 2.2`: Xác định kết quả mong đợi (`ACCEPT` hoặc `REJECT`) thay cho `TODO` tương ứng với `TC02`–`TC05`.

<details>
<summary>Gợi ý từng bước (nhấn để xem nếu cần)</summary>

1. Với miền giá trị hợp lệ $[A, B]$, kỹ thuật BVA chuẩn kiểm tra 4 điểm biên: $A - 1$, $A$, $B$, $B + 1$.
2. Trong `@CsvSource`, mỗi dòng có định dạng: `"TCxx, <tuổi>, <email>, <ACCEPT hoặc REJECT>"`.

</details>

### Lệnh chạy & Kết quả mong đợi
```bash
./mvnw -Dtest=UserConstraintTest test
```
- **Kết quả mong đợi:** Test `verifyRequiredBoundaryTestCasesPresent` sẽ `PASS`, và trong `shouldEnforceUserConstraints` sẽ có **đúng 1 test case bị `FAIL`** do lỗi chủ đích đang tồn tại trong schema cơ sở dữ liệu.

---

## Task 3 – Bug Hunting (2 phút)

### Mục tiêu
Chạy toàn bộ bộ kiểm thử, quan sát sự khác biệt giữa **Expected** và **Actual**, truy vết ngược lại file migration để tìm nguyên nhân gốc rễ (root cause).

### Các bước thực hiện
1. Chạy toàn bộ test suite:
   ```bash
   ./mvnw test
   ```
2. Đọc kỹ thông báo lỗi ở terminal:
   - Test case nào bị `FAIL`?
   - **Expected Result** là gì? **Actual Result** là gì?
3. **Chụp ảnh màn hình** kết quả thất bại (bao gồm tên test case bị FAIL và dòng tổng kết `Tests run: ..., Failures: 1, ...`) và lưu thành `before_fix.png`.
4. Mở file [V1__create_users_table.sql](../src/main/resources/db/migration/V1__create_users_table.sql), đối chiếu định nghĩa bảng với yêu cầu nghiệp vụ ở đầu bài để xác định nguyên nhân gây lỗi.

---

## Task 4 – Fix & Retest (3 phút)

### Mục tiêu
Sửa lỗi ràng buộc trong schema và chạy kiểm thử hồi quy (regression testing) để đảm bảo toàn bộ hệ thống hoạt động đúng.

### File cần chỉnh sửa
[V1__create_users_table.sql](../src/main/resources/db/migration/V1__create_users_table.sql)

### Các bước thực hiện
1. Chỉnh sửa ràng buộc bị thiếu/sai trong `V1__create_users_table.sql` sao cho khớp hoàn toàn với yêu cầu nghiệp vụ.
2. Chạy lại toàn bộ test suite:
   ```bash
   ./mvnw test
   ```
3. Kiểm tra kết quả: tất cả test đều `PASS` (`Failures: 0, Errors: 0, Skipped: 0`).
4. **Chụp ảnh màn hình** kết quả thành công và lưu thành `after_fix.png`.

> **Tại sao trong bài tập này chúng ta sửa trực tiếp file `V1`?**
> Trong môi trường test tự động với Testcontainers, mỗi lần chạy `./mvnw test` sẽ khởi tạo một container PostgreSQL hoàn toàn mới và chạy lại Flyway từ đầu, nên việc sửa trực tiếp `V1` hoạt động sạch sẽ. Tuy nhiên, **trong hệ thống thực tế đã triển khai (production)**, bạn tuyệt đối không sửa file migration đã chạy mà phải tạo một file migration mới (ví dụ `V2__...sql`) để `ALTER TABLE`.

---

## Quy trình nộp bài & Checklist

1. Tạo bản sao thư mục `submit/MSSV/` và đổi tên `MSSV` thành mã số sinh viên của bạn.
2. Copy 3 file đã chỉnh sửa vào thư mục `<MSSV>/`:
   - `DataGenerationTest.java`
   - `UserConstraintTest.java`
   - `V1__create_users_table.sql`
3. Đặt 2 ảnh chụp màn hình vào `<MSSV>/`:
   - `before_fix.png`
   - `after_fix.png`
4. Điền đầy đủ nội dung vào `REPORT.md` trong `<MSSV>/`.
5. Kiểm tra lại thư mục `<MSSV>/` có **đúng 6 file**, sau đó nén thành `<MSSV>.zip`.

**Checklist trước khi nộp:**
- [ ] `DataGenerationTest` chạy `PASS` độc lập.
- [ ] `before_fix.png` thể hiện rõ 1 test case `FAIL` trước khi sửa schema.
- [ ] `after_fix.png` thể hiện rõ `Failures: 0, Errors: 0, Skipped: 0`.
- [ ] `REPORT.md` đã điền đầy đủ MSSV, họ tên, phân tích lỗi và kết quả.
- [ ] File `<MSSV>.zip` chỉ chứa 6 file theo đúng cấu trúc yêu cầu.

---
