CREATE TABLE IF NOT EXISTS notification_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  notification_id VARCHAR(64) NOT NULL UNIQUE,
  student_id VARCHAR(64) NOT NULL,
  title VARCHAR(128) NOT NULL,
  content VARCHAR(512) NOT NULL,
  business_id VARCHAR(64),
  status VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL,
  sent_at DATETIME,
  KEY idx_notification_student (student_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notification_subscription (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_id VARCHAR(64) NOT NULL,
  channel VARCHAR(32) NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL,
  UNIQUE KEY uk_notification_subscription (student_id, channel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
