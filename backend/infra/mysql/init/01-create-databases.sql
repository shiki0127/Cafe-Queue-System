CREATE DATABASE IF NOT EXISTS cafequeue_order_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS cafequeue_inventory_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS cafequeue_queue_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS cafequeue_notification_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS cafequeue_coupon_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS cafequeue_device_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON cafequeue_order_db.* TO 'cafequeue'@'%';
GRANT ALL PRIVILEGES ON cafequeue_inventory_db.* TO 'cafequeue'@'%';
GRANT ALL PRIVILEGES ON cafequeue_queue_db.* TO 'cafequeue'@'%';
GRANT ALL PRIVILEGES ON cafequeue_notification_db.* TO 'cafequeue'@'%';
GRANT ALL PRIVILEGES ON cafequeue_coupon_db.* TO 'cafequeue'@'%';
GRANT ALL PRIVILEGES ON cafequeue_device_db.* TO 'cafequeue'@'%';
FLUSH PRIVILEGES;
