# Hướng dẫn xử lý sự cố (Troubleshooting Guide)

## 1. Lỗi phiên bản Java / `JAVA_HOME` / Maven dùng sai JDK

**Triệu chứng:**
- `Detected JDK version ... is not in the allowed range [25,)` (lỗi từ `maven-enforcer-plugin`).
- `invalid target release: 25` hoặc `Unsupported class file major version`.

**Cách khắc phục:**
1. Kiểm tra phiên bản Java đang dùng trong terminal:
   ```bash
   java --version
   javac --version
   ./mvnw --version
   ```
2. Đảm bảo biến môi trường `JAVA_HOME` trỏ đúng tới thư mục cài đặt JDK 25+:
   - **macOS:**
     ```bash
     export JAVA_HOME=$(/usr/libexec/java_home -v 25)
     ```
   - **Windows PowerShell:**
     ```powershell
     $env:JAVA_HOME = "C:\Program Files\Java\jdk-25"
     ```
   - **Linux:** Kiểm tra `echo $JAVA_HOME` và cập nhật trỏ về thư mục JDK 25+.

---

## 2. Lỗi Docker daemon / Quyền Docker / Testcontainers không tìm thấy Docker

**Triệu chứng:**
- `IllegalStateException: Could not find a valid Docker environment.`
- `Cannot connect to the Docker daemon.`

**Cách khắc phục:**
1. Đảm bảo **Docker Desktop** (macOS/Windows) đã được mở và đang ở trạng thái *Running*.
2. Kiểm tra từ terminal bằng lệnh:
   ```bash
   docker info
   ```
3. Trên **Linux**, nếu `docker info` báo lỗi `permission denied`, thêm user vào nhóm `docker` rồi đăng nhập lại:
   ```bash
   sudo usermod -aG docker $USER
   ```
4. Nếu bạn dùng Docker Engine phiên bản mới (Docker 29+) với cấu hình tùy chỉnh, đảm bảo Testcontainers không bị chặn bởi biến môi trường `DOCKER_HOST` sai lệch.

---

## 3. Lỗi quyền thực thi Maven Wrapper (`Permission denied`)

**Triệu chứng:**
- `zsh: permission denied: ./mvnw` hoặc `bash: ./mvnw: Permission denied`.

**Cách khắc phục (macOS / Linux):**
```bash
chmod +x mvnw
```
*(Trên Windows PowerShell, luôn dùng `.\mvnw.cmd`.)*

---

## 4. Lỗi tải Maven, dependencies hoặc Docker image

**Triệu chứng:**
- Quá trình `./mvnw` bị timeout hoặc báo `Could not transfer artifact` / `Error pulling image`.

**Cách khắc phục:**
1. Kiểm tra kết nối Internet (lần chạy đầu tiên bắt buộc cần Internet để tải Maven, thư viện và image `postgres:17-alpine`).
2. Nếu mạng trường/công ty có proxy hoặc firewall chặn Docker Hub / Maven Central, thử đổi sang mạng ổn định hơn và chạy lại `./mvnw -Dtest=SmokeTest test`.

---

## 5. Lỗi Flyway migration và JDBC

**Triệu chứng:**
- `FlywayValidateException` hoặc `PSQLException: ERROR: syntax error at or near ...` khi sửa file SQL.

**Cách khắc phục:**
1. Kiểm tra lại cú pháp PostgreSQL trong `V1__create_users_table.sql` (dấu phẩy giữa các cột, dấu ngoặc đơn của `CHECK (...)`, dấu `;` cuối lệnh).
2. Vì mỗi lần chạy `./mvnw test` đều tạo container mới, bạn chỉ cần sửa lại `V1__create_users_table.sql` cho đúng cú pháp rồi chạy lại `./mvnw test`.

---

## 6. Lỗi DBUnit assertion failures

**Triệu chứng:**
- `AssertionFailedError` từ `org.dbunit.Assertion` khi so sánh bảng `users`.

**Cách khắc phục:**
1. Kiểm tra xem dữ liệu bạn sinh ra trong `DataGenerationTest` có khớp với kiểu dữ liệu và độ dài cột trong bảng `users` hay không (`username` 3–20 ký tự, `email` <= 100 ký tự và không trùng lặp).
2. Nếu bạn thay đổi logic tạo `User` trong `DataGenerationTest`, đảm bảo đối tượng truyền vào `userRepository.insert(...)` chính là đối tượng được đưa vào `generatedUsers` để dựng bảng kỳ vọng.

---

## 7. Phân biệt 3 loại lỗi khi chạy `./mvnw test`

| Loại lỗi | Dấu hiệu nhận biết | Ý nghĩa & Hành động |
|---|---|---|
| **1. Chưa làm TODO** | Thông báo có chữ `TODO 1.1`, `TODO 1.2`, hoặc `TODO 2.1 / 2.2` | Bạn chưa điền code/giá trị vào vị trí TODO tương ứng. Hãy hoàn thành TODO rồi chạy lại. |
| **2. Lỗi chủ đích của bài tập (Task 2 & 3)** | `verifyRequiredBoundaryTestCasesPresent` **PASS**, chỉ có **1** test case trong `UserConstraintTest` bị `FAIL` vì database không ném `SQLException` | Đây là lỗi thực tế từ schema bị thiếu/sai ràng buộc! Hãy chụp `before_fix.png` rồi chuyển sang Task 3 & 4. |
| **3. Lỗi hạ tầng (Infrastructure error)** | `SmokeTest` bị `ERROR`, `ExceptionInInitializerError`, không kết nối được Docker hoặc sai JDK | Kiểm tra lại mục 1–4 ở trên để sửa môi trường trước khi làm bài. |
