# CafeQueue

校园智能饮品多维调度与物联网控制平台后端工程。

## Modules

- `gateway-service`: unified gateway, JWT filter, dynamic route and rate limiting entry.
- `order-service`: order lifecycle and payment callback.
- `recipe-inventory-service`: recipe and per-machine inventory reservation.
- `vending-queue-service`: queue dispatch and wait-time estimation.
- `notification-service`: SSE notification center.
- `marketing-coupon-service`: coupon issue, lock and redeem.
- `vending-device-service`: vending device status and command dispatch.

## Local Run

```bash
mvn -DskipTests package
docker compose up --build
```
