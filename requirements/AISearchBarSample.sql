/* =========================================================
   DATABASE
   ========================================================= */
CREATE DATABASE IF NOT EXISTS nl_search_demo;
USE nl_search_demo;

/* =========================================================
   COMPANIES
   ========================================================= */
CREATE TABLE companies (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  country_code CHAR(2),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_company_name (name)
);

INSERT INTO companies (name, country_code) VALUES
('Demo Company', 'HK'),
('7Eleven Kwai Chung', 'HK'),
('ABI Graphique Demo', 'FR');

/* =========================================================
   CARRIERS
   ========================================================= */
CREATE TABLE carriers (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(80) NOT NULL,
  UNIQUE KEY uk_carrier_name (name)
);

INSERT INTO carriers (name) VALUES
('DHL'),
('SF Express'),
('UPS');

/* =========================================================
   LOCATIONS
   ========================================================= */
CREATE TABLE locations (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  company_id BIGINT NOT NULL,
  name VARCHAR(160) NOT NULL,
  location_type ENUM('PUDO','LOCKER','WAREHOUSE','STORE') NOT NULL,
  city VARCHAR(80),
  is_active TINYINT(1) DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_location_name (name),
  KEY idx_location_company (company_id),
  CONSTRAINT fk_location_company
    FOREIGN KEY (company_id) REFERENCES companies(id)
);

INSERT INTO locations (company_id, name, location_type, city) VALUES
(1, 'alfred24 Office Locker', 'LOCKER', 'Hong Kong'),
(1, 'Location A', 'LOCKER', 'Hong Kong'),
(1, 'Location B', 'PUDO', 'Hong Kong'),
(2, '7Eleven Kwai Chung - PUDO', 'STORE', 'Hong Kong'),
(3, 'ABI Graphique Demo Locker Normal', 'LOCKER', 'Paris'),
(3, 'ABI Graphique Demo Locker Temp Control', 'LOCKER', 'Paris');

/* =========================================================
   ORDERS
   ========================================================= */
CREATE TABLE orders (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,

  tracking_no VARCHAR(32) NOT NULL,
  order_no VARCHAR(32) NOT NULL,

  company_id BIGINT NOT NULL,
  location_id BIGINT NOT NULL,
  carrier_id BIGINT,

  service ENUM('DELIVERY','RETURNS') NOT NULL,
  status ENUM(
    'CREATED',
    'COURIER_STORED',
    'CUSTOMER_STORED',
    'DELIVERED',
    'OPERATOR_COLLECTED',
    'EXPIRED'
  ) NOT NULL,

  collected_by_type ENUM('COURIER','CUSTOMER','OPERATOR'),
  recipient_phone VARCHAR(32),
  compartment_no INT,

  time_created DATETIME NOT NULL,
  time_stored DATETIME,
  time_collected DATETIME,
  expires_at DATETIME,

  flags VARCHAR(255),

  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

  KEY idx_tracking_no (tracking_no),
  KEY idx_order_no (order_no),
  KEY idx_status (status),
  KEY idx_service (service),
  KEY idx_created_time (time_created),
  KEY idx_stored_time (time_stored),
  KEY idx_collected_time (time_collected),

  CONSTRAINT fk_orders_company
    FOREIGN KEY (company_id) REFERENCES companies(id),
  CONSTRAINT fk_orders_location
    FOREIGN KEY (location_id) REFERENCES locations(id),
  CONSTRAINT fk_orders_carrier
    FOREIGN KEY (carrier_id) REFERENCES carriers(id)
);

/* =========================================================
   SAMPLE DATA
   ========================================================= */

-- Delivered orders
INSERT INTO orders VALUES
(NULL,'2026010806','2026010806',2,4,1,'DELIVERY','DELIVERED','CUSTOMER','85293293177',7,
 '2026-01-08 16:43:00','2026-01-08 18:12:00','2026-01-08 18:13:00','2026-01-15 00:00:00',NULL,NOW()),

(NULL,'2026010601','2026010601',1,2,1,'DELIVERY','DELIVERED','CUSTOMER','85290000005',1,
 '2026-01-06 11:00:00','2026-01-06 12:00:00','2026-01-06 12:10:00','2026-01-13 00:00:00','FRAGILE',NOW());

-- Operator collected
INSERT INTO orders VALUES
(NULL,'2026010802','2026010802',1,1,NULL,'DELIVERY','OPERATOR_COLLECTED','OPERATOR','85293293177',13,
 '2026-01-08 15:53:00','2026-01-08 17:37:00','2026-01-08 17:42:00','2026-01-12 00:00:00','VIP',NOW());

-- Customer stored (not yet collected)
INSERT INTO orders VALUES
(NULL,'2026010804','2026010804',1,1,NULL,'RETURNS','CUSTOMER_STORED',NULL,'85293293177',9,
 '2026-01-08 15:56:00','2026-01-08 17:41:00',NULL,'2026-01-20 00:00:00','FRAGILE',NOW());

-- Created only
INSERT INTO orders VALUES
(NULL,'2026010807','2026010807',1,1,NULL,'RETURNS','CREATED',NULL,'85293293177',NULL,
 '2026-01-08 17:21:00',NULL,NULL,NULL,NULL,NOW());

-- Expired parcels
INSERT INTO orders VALUES
(NULL,'2026010201','2026010201',1,2,2,'DELIVERY','EXPIRED','COURIER','85290000001',10,
 '2026-01-02 10:00:00','2026-01-02 12:00:00',NULL,'2026-01-05 00:00:00','EXPIRED',NOW()),

(NULL,'2026010301','2026010301',1,2,2,'DELIVERY','EXPIRED','CUSTOMER','85290000002',11,
 '2026-01-03 09:30:00','2026-01-03 10:00:00',NULL,'2026-01-06 00:00:00','EXPIRED,VIP',NOW());

-- Older December data
INSERT INTO orders VALUES
(NULL,'14122025','14122025',3,5,3,'DELIVERY','DELIVERED','CUSTOMER','670110034',9,
 '2025-12-03 16:24:00','2025-12-03 23:28:00','2025-12-03 23:38:00','2025-12-20 00:00:00',NULL,NOW());

/* =========================================================
   END
   ========================================================= */
