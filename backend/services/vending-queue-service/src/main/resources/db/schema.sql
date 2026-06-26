CREATE TABLE IF NOT EXISTS queue_ticket (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  ticket_id VARCHAR(64) NOT NULL UNIQUE,
  order_id VARCHAR(64) NOT NULL,
  machine_id VARCHAR(64) NOT NULL,
  recipe_code VARCHAR(64) NOT NULL,
  queue_position INT NOT NULL,
  estimated_wait_seconds BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  KEY idx_queue_ticket_machine_status (machine_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS dispatch_rule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  rule_code VARCHAR(64) NOT NULL UNIQUE,
  rule_name VARCHAR(128) NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  weight INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
