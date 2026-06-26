CREATE TABLE IF NOT EXISTS recipe (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  recipe_code VARCHAR(64) NOT NULL UNIQUE,
  recipe_name VARCHAR(128) NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS recipe_ingredient (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  recipe_code VARCHAR(64) NOT NULL,
  ingredient_code VARCHAR(64) NOT NULL,
  amount_mg INT NOT NULL,
  UNIQUE KEY uk_recipe_ingredient (recipe_code, ingredient_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS machine_inventory (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  machine_id VARCHAR(64) NOT NULL,
  ingredient_code VARCHAR(64) NOT NULL,
  available_mg INT NOT NULL,
  locked_mg INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_machine_ingredient (machine_id, ingredient_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS inventory_reservation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reservation_id VARCHAR(64) NOT NULL UNIQUE,
  order_id VARCHAR(64) NOT NULL,
  machine_id VARCHAR(64) NOT NULL,
  recipe_code VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
