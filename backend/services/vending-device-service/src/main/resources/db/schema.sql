CREATE TABLE IF NOT EXISTS vending_device (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  device_id VARCHAR(64) NOT NULL UNIQUE,
  location VARCHAR(128) NOT NULL,
  online TINYINT(1) NOT NULL DEFAULT 0,
  water_temperature INT NOT NULL DEFAULT 0,
  cleaning_required TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS device_command (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  command_id VARCHAR(64) NOT NULL UNIQUE,
  device_id VARCHAR(64) NOT NULL,
  order_id VARCHAR(64) NOT NULL,
  recipe_code VARCHAR(64) NOT NULL,
  command_type VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL,
  completed_at DATETIME,
  KEY idx_device_command_device_status (device_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS device_telemetry_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  device_id VARCHAR(64) NOT NULL,
  water_temperature INT NOT NULL,
  raw_payload TEXT,
  created_at DATETIME NOT NULL,
  KEY idx_device_telemetry_device_time (device_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
