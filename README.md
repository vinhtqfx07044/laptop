# Laptop Repair Application

Ứng dụng quản lý sửa chữa laptop với Spring Boot và PostgreSQL.

## Yêu cầu hệ thống

- Java 21+
- Maven 3.6+
- PostgreSQL 12+ (cho production)
- WSL2 (nếu chạy trên Windows)

## Cài đặt PostgreSQL trên WSL

### 1. Cài đặt PostgreSQL

```bash
# Cập nhật package list
sudo apt update

# Cài đặt PostgreSQL
sudo apt install postgresql postgresql-contrib

# Kiểm tra version
sudo -u postgres psql -c "SELECT version();"
```

### 2. Khởi động dịch vụ PostgreSQL

```bash
# Khởi động PostgreSQL service
sudo service postgresql start

# Kiểm tra trạng thái
sudo service postgresql status

# Tự động khởi động khi boot (optional)
sudo systemctl enable postgresql
```

### 3. Thiết lập mật khẩu cho user postgres

```bash
# Đăng nhập PostgreSQL với user postgres
sudo -u postgres psql

# Đặt mật khẩu cho user postgres
ALTER USER postgres PASSWORD 'postgres';

# Thoát
\q
```

### 4. Tạo database production

```bash
# Đăng nhập với user postgres
sudo -u postgres psql

# Tạo database
CREATE DATABASE laptop_repair_prod;

# Kiểm tra database đã tạo
\l

# Thoát
\q
```

### 5. Xóa database dev cũ (nếu có)

```bash
# Đăng nhập PostgreSQL
sudo -u postgres psql

# Xóa database dev (nếu tồn tại)
DROP DATABASE IF EXISTS laptop_repair_dev;

# Thoát
\q
```

## Cấu hình ứng dụng

### 1. File .env (đã có sẵn)

File `.env` đã được cấu hình sử dụng user `postgres` thay vì tạo user riêng:

```properties
# PostgreSQL Production Database
POSTGRES_PROD_HOST=localhost
POSTGRES_PROD_PORT=5432
POSTGRES_PROD_DB=laptop_repair_prod
POSTGRES_PROD_USER=postgres
POSTGRES_PROD_PASSWORD=postgres
```

### 2. Application Properties

- **Dev profile**: `application-dev.properties` - sử dụng H2 in-memory database
- **Prod profile**: `application-prod.properties` - sử dụng PostgreSQL từ biến môi trường

## Chạy ứng dụng

### 1. Profile Development (H2 Database)

```bash
mvn spring-boot:run
# hoặc
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 2. Profile Production (PostgreSQL)

```bash
# Đảm bảo PostgreSQL đang chạy
sudo service postgresql start

# Chạy với production profile
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## Khởi tạo dữ liệu

Khi chạy lần đầu với profile production, ứng dụng sẽ tự động:

1. Tạo các bảng từ file `schema.sql`
2. Thêm dữ liệu mẫu từ file `data.sql`

### Cấu trúc database

Ứng dụng bao gồm các bảng chính:

- `service_item` - Danh mục dịch vụ sửa chữa
- `request` - Yêu cầu sửa chữa từ khách hàng
- `request_items` - Chi tiết dịch vụ trong mỗi yêu cầu
- `request_history` - Lịch sử thay đổi trạng thái
- `request_images` - Hình ảnh đính kèm
- `spring_ai_chat_memory` - Lưu trữ lịch sử chat AI

## Truy cập ứng dụng

- **URL**: http://localhost:8080
- **Login trang staff**: http://localhost:8080/login
  - Username: `staff`
  - Password: `staff123`

## Kiểm tra database

```bash
# Đăng nhập PostgreSQL
sudo -u postgres psql -d laptop_repair_prod

# Xem danh sách bảng
\dt

# Xem cấu trúc bảng
\d request

# Xem dữ liệu
SELECT COUNT(*) FROM service_item;
SELECT COUNT(*) FROM request;

# Thoát
\q
```

## Xử lý sự cố

### 1. PostgreSQL không khởi động được

```bash
# Kiểm tra log
sudo journalctl -u postgresql

# Restart service
sudo service postgresql restart
```

### 2. Lỗi kết nối database

- Kiểm tra PostgreSQL đang chạy: `sudo service postgresql status`
- Kiểm tra port 5432: `netstat -an | grep 5432`
- Kiểm tra file `.env` có đúng thông tin kết nối

### 3. Port 8080 đã được sử dụng

```bash
# Tìm process đang sử dụng port 8080
lsof -i :8080

# Kill process (thay PID bằng process ID thực tế)
kill -9 PID
```

### 4. Reset toàn bộ database

```bash
# Xóa và tạo lại database
sudo -u postgres psql -c "DROP DATABASE IF EXISTS laptop_repair_prod;"
sudo -u postgres psql -c "CREATE DATABASE laptop_repair_prod;"

# Restart ứng dụng để tự động tạo lại schema và data
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## Lưu ý quan trọng

1. **User postgres**: Sử dụng user `postgres` mặc định thay vì tạo user riêng để đơn giản hóa setup
2. **Schema validation**: Production profile sử dụng `validate` mode, không tự động tạo/sửa bảng
3. **Data initialization**: Dữ liệu chỉ được khởi tạo khi database trống
4. **Security**: Trong môi trường thực tế, nên tạo user riêng với quyền hạn chế thay vì dùng postgres superuser

## Scripts hữu ích

### Backup database

```bash
sudo -u postgres pg_dump laptop_repair_prod > backup.sql
```

### Restore database

```bash
sudo -u postgres psql laptop_repair_prod < backup.sql
```

### Xem thống kê database

```bash
sudo -u postgres psql -d laptop_repair_prod -c "
SELECT 
    schemaname,
    tablename,
    attname,
    n_distinct,
    correlation 
FROM pg_stats 
WHERE schemaname = 'public';"
```