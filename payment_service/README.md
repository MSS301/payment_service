# Payment Service

Payment Service là một microservice quản lý thanh toán trong hệ thống MSS, tích hợp với PayOS để xử lý các giao dịch thanh toán.

## Tính năng

- **Tạo thanh toán**: Tạo link thanh toán thông qua PayOS
- **Quản lý trạng thái thanh toán**: Theo dõi và cập nhật trạng thái thanh toán
- **Webhook PayOS**: Xử lý callback từ PayOS khi thanh toán thành công/thất bại
- **Event-driven**: Publish events khi có thay đổi thanh toán
- **API Documentation**: Swagger UI để test APIs

## Tech Stack

- Java 17
- Spring Boot 3.5.6
- Spring Cloud (Eureka Client)
- Spring Data JPA
- MySQL 8.0
- Apache Kafka
- PayOS SDK
- Docker & Docker Compose

## Cấu hình

### PayOS Configuration

Cần cấu hình các thông tin PayOS trong `application.properties`:

```properties
payos.client-id=your-client-id
payos.api-key=your-api-key
payos.checksum-key=your-checksum-key
```

Hoặc sử dụng environment variables:

```bash
PAYOS_CLIENT_ID=your-client-id
PAYOS_API_KEY=your-api-key
PAYOS_CHECKSUM_KEY=your-checksum-key
```

### Database Configuration

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/payment_service_db
spring.datasource.username=root
spring.datasource.password=password
```

## Chạy ứng dụng

### 1. Chạy local

```bash
# Clone repository
git clone <repository-url>
cd payment_service

# Cấu hình database
# Tạo database: payment_service_db

# Cài đặt dependencies và chạy
./mvnw spring-boot:run
```

### 2. Chạy với Docker

```bash
# Build và chạy với docker-compose
docker-compose up -d

# Hoặc build image riêng
docker build -t payment-service .
docker run -p 8084:8084 payment-service
```

## API Endpoints

| Method | Endpoint                           | Description                              |
| ------ | ---------------------------------- | ---------------------------------------- |
| POST   | `/payment`                         | Tạo thanh toán mới                       |
| GET    | `/payment/{orderCode}`             | Lấy thông tin thanh toán theo order code |
| GET    | `/payment/user/{userId}`           | Lấy danh sách thanh toán của user        |
| GET    | `/payment/user/{userId}/paginated` | Lấy thanh toán của user có phân trang    |
| PUT    | `/payment/{orderCode}/status`      | Cập nhật trạng thái thanh toán           |
| POST   | `/payment/webhook`                 | Webhook từ PayOS                         |
| GET    | `/payment/return`                  | Return URL từ PayOS                      |
| GET    | `/payment/cancel`                  | Cancel URL từ PayOS                      |

## API Documentation

Sau khi chạy ứng dụng, truy cập:

- Swagger UI: http://localhost:8084/swagger-ui.html
- API Docs: http://localhost:8084/v3/api-docs

## Database Schema

### Table: payments

| Column                | Type      | Description           |
| --------------------- | --------- | --------------------- |
| id                    | BIGINT    | Primary key           |
| order_code            | BIGINT    | Mã đơn hàng (unique)  |
| user_id               | VARCHAR   | ID người dùng         |
| amount                | DECIMAL   | Số tiền               |
| description           | VARCHAR   | Mô tả                 |
| status                | ENUM      | Trạng thái thanh toán |
| payment_url           | VARCHAR   | Link thanh toán       |
| reference_code        | VARCHAR   | Mã tham chiếu         |
| transaction_id        | VARCHAR   | ID giao dịch          |
| wallet_transaction_id | VARCHAR   | ID giao dịch ví       |
| created_at            | TIMESTAMP | Thời gian tạo         |
| updated_at            | TIMESTAMP | Thời gian cập nhật    |
| paid_at               | TIMESTAMP | Thời gian thanh toán  |
| cancelled_at          | TIMESTAMP | Thời gian hủy         |

## Events

Service này publish các events sau:

### PaymentCreatedEvent

```json
{
  "paymentId": 1,
  "orderCode": 1234567,
  "userId": "user123",
  "amount": 100000,
  "status": "PENDING"
}
```

### PaymentStatusChangedEvent

```json
{
  "paymentId": 1,
  "orderCode": 1234567,
  "userId": "user123",
  "oldStatus": "PENDING",
  "newStatus": "PAID",
  "amount": 100000
}
```

## Monitoring & Logging

- Application logs: `/logs/payment-service.log`
- Health check: `http://localhost:8084/actuator/health`
- Metrics: `http://localhost:8084/actuator/metrics`

## Development

### Testing APIs

1. **Tạo thanh toán**:

```bash
curl -X POST http://localhost:8084/payment \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "amount": 100000,
    "description": "Thanh toán đơn hàng #123",
    "referenceCode": "REF123"
  }'
```

2. **Lấy thông tin thanh toán**:

```bash
curl http://localhost:8084/payment/1234567
```

### Webhook Testing

PayOS sẽ gửi webhook đến: `http://your-domain/payment/webhook`

Cấu trúc webhook:

```json
{
  "data": {
    "orderCode": 1234567,
    "status": "PAID",
    "transactionId": "txn_123"
  }
}
```

## Troubleshooting

### Common Issues

1. **PayOS connection error**: Kiểm tra API credentials
2. **Database connection error**: Kiểm tra database configuration
3. **Eureka registration failed**: Kiểm tra Eureka server

### Logs

```bash
# Xem logs container
docker logs payment-service

# Xem logs real-time
docker logs -f payment-service
```

## Contributing

1. Fork repository
2. Tạo feature branch
3. Commit changes
4. Push và tạo Pull Request

## License

MIT License
