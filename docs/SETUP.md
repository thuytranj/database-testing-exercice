# Hướng dẫn cài đặt môi trường (Setup Guide)

## 1. Bạn cần tự cài đặt những gì?

Bạn **chỉ** cần cài đặt thủ công 3 thành phần sau:
1. **JDK 25+** (khuyến nghị OpenJDK 25).
2. **Docker Desktop** (trên macOS/Windows) hoặc Docker Engine + Docker CLI (trên Linux).
3. **IDE / Code Editor** (IntelliJ IDEA, VS Code, hoặc Eclipse).
4. *(Tùy chọn)* **Git** nếu bạn clone repository thay vì tải mã nguồn trực tiếp.

> **Lưu ý quan trọng:** Bạn **không** cần cài đặt thủ công Maven, PostgreSQL, pgAdmin, Docker Compose, JUnit, DBUnit, Datafaker, Testcontainers hay Flyway.
> - **Maven Wrapper (`mvnw` / `mvnw.cmd`)** sẽ tự động tải Maven đúng phiên bản.
> - **Maven** sẽ tự động tải toàn bộ thư viện kiểm thử.
> - **Testcontainers** sẽ tự động tải image PostgreSQL và khởi chạy container khi test chạy.
> - **Flyway** sẽ tự động tạo bảng và cấu trúc cơ sở dữ liệu.

## 2. Yêu cầu kết nối Internet lần đầu

Trong lần chạy đầu tiên, hệ thống cần kết nối Internet để:
- Tải Maven distribution (nếu chưa có trong `~/.m2/wrapper`).
- Tải các dependency trong `pom.xml`.
- Tải Docker image `postgres:17-alpine` và image hỗ trợ của Testcontainers.

> **Khuyến nghị:** Hãy hoàn thành bước cài đặt và chạy `SmokeTest` thành công **trước khi vào lớp** để tránh mất thời gian tải về trong giờ thực hành.

## 3. Hướng dẫn theo hệ điều hành

### macOS
1. Kiểm tra JDK 25+:
   ```bash
   java --version
   javac --version
   ```
   Nếu chưa có JDK 25+, cài đặt qua Homebrew (`brew install openjdk@25`) hoặc tải từ Adoptium / Oracle và cấu hình `JAVA_HOME`.
2. Mở **Docker Desktop** và đợi trạng thái chuyển sang *Running*.
3. Nếu gặp lỗi quyền thực thi đối với `mvnw`, cấp quyền:
   ```bash
   chmod +x mvnw
   ```

### Windows (PowerShell)
1. Kiểm tra JDK 25+:
   ```powershell
   java --version
   javac --version
   ```
   Nếu chưa có, cài đặt JDK 25+ và thêm vào biến môi trường `JAVA_HOME` và `Path`.
2. Mở **Docker Desktop** (đảm bảo WSL 2 backend đang hoạt động) và đợi trạng thái *Engine running*.
3. Sử dụng `.\mvnw.cmd` thay cho `./mvnw` trong PowerShell.

### Linux
1. Kiểm tra JDK 25+:
   ```bash
   java --version
   javac --version
   ```
2. Đảm bảo Docker daemon đang chạy (`sudo systemctl start docker`) và user hiện tại có quyền chạy `docker` không cần `sudo` (thuộc nhóm `docker`).
3. Cấp quyền thực thi cho Maven Wrapper nếu cần:
   ```bash
   chmod +x mvnw
   ```

## 4. Kiểm tra môi trường (Verification)

Chạy lần lượt các lệnh sau tại thư mục gốc của project:

**macOS / Linux:**
```bash
java --version
javac --version
docker --version
docker info
./mvnw --version
./mvnw -Dtest=SmokeTest test
```

**Windows (PowerShell):**
```powershell
java --version
javac --version
docker --version
docker info
.\mvnw.cmd --version
.\mvnw.cmd -Dtest=SmokeTest test
```

Khi `./mvnw -Dtest=SmokeTest test` kết thúc với `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0` và `BUILD SUCCESS`, môi trường của bạn đã hoàn toàn sẵn sàng.

> **Lưu ý:** Nếu bạn chạy `./mvnw test` ngay lúc này khi chưa làm bài, các bài test có `TODO` sẽ báo lỗi nhắc bạn hoàn thành nhiệm vụ. Đây là hành vi bình thường của bộ starter.
