CREATE TABLE IF NOT EXISTS orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id VARCHAR(64) NOT NULL UNIQUE,
  student_id VARCHAR(64) NOT NULL,
  machine_id VARCHAR(64) NOT NULL,
  recipe_code VARCHAR(64) NOT NULL,
  coupon_code VARCHAR(64),
  coupon_lock_id VARCHAR(64),
  reservation_id VARCHAR(64),
  queue_ticket_id VARCHAR(64),
  device_command_id VARCHAR(64),
  status VARCHAR(32) NOT NULL,
  estimated_wait_seconds BIGINT NOT NULL DEFAULT 0,
  failure_reason VARCHAR(255),
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS order_state_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id VARCHAR(64) NOT NULL,
  from_status VARCHAR(32),
  to_status VARCHAR(32) NOT NULL,
  reason VARCHAR(255),
  created_at DATETIME NOT NULL,
  KEY idx_order_state_history_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS payment_callback_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  callback_id VARCHAR(128) NOT NULL UNIQUE,
  order_id VARCHAR(64) NOT NULL,
  payload TEXT NOT NULL,
  handled TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  KEY idx_payment_callback_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
