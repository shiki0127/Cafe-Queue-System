CREATE TABLE IF NOT EXISTS coupon_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  template_code VARCHAR(64) NOT NULL UNIQUE,
  template_name VARCHAR(128) NOT NULL,
  discount_amount DECIMAL(10,2) NOT NULL,
  threshold_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  total_quantity INT NOT NULL,
  issued_quantity INT NOT NULL DEFAULT 0,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_coupon (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  coupon_code VARCHAR(64) NOT NULL UNIQUE,
  template_code VARCHAR(64) NOT NULL,
  student_id VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  locked_order_id VARCHAR(64),
  created_at DATETIME NOT NULL,
  used_at DATETIME,
  KEY idx_user_coupon_student_status (student_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS coupon_redeem_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  coupon_code VARCHAR(64) NOT NULL,
  order_id VARCHAR(64) NOT NULL,
  idempotency_key VARCHAR(128) NOT NULL UNIQUE,
  created_at DATETIME NOT NULL,
  KEY idx_coupon_redeem_coupon_code (coupon_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
