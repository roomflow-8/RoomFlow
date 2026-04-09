CREATE DATABASE `roomflow` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci */;

-- roomflow.equipment definition

CREATE TABLE `equipment` (
                             `equipment_id` bigint(20) NOT NULL AUTO_INCREMENT,
                             `equipment_name` varchar(100) NOT NULL,
                             `total_stock` int(11) NOT NULL DEFAULT 0,
                             `description` text DEFAULT NULL,
                             `maintenance_limit` int(11) NOT NULL DEFAULT 0,
                             `price` decimal(10,0) NOT NULL DEFAULT 0,
                             `status` enum('AVAILABLE','MAINTENANCE','INACTIVE') NOT NULL DEFAULT 'AVAILABLE',
                             `total_reservations` int(11) NOT NULL DEFAULT 0,
                             `image_url` varchar(1000) DEFAULT NULL,
                             `created_at` datetime NOT NULL DEFAULT current_timestamp(),
                             `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
                             PRIMARY KEY (`equipment_id`),
                             CONSTRAINT `chk_equipment_stock` CHECK (`total_stock` >= 0)
) ENGINE=InnoDB AUTO_INCREMENT=99 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;


-- roomflow.holiday definition

CREATE TABLE `holiday` (
                           `holiday_id` bigint(20) NOT NULL AUTO_INCREMENT,
                           `title` varchar(100) NOT NULL,
                           `description` varchar(500) DEFAULT NULL,
                           `holiday_date` date NOT NULL,
                           `is_active` tinyint(1) NOT NULL DEFAULT 1,
                           `created_at` datetime NOT NULL DEFAULT current_timestamp(),
                           `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
                           PRIMARY KEY (`holiday_id`),
                           UNIQUE KEY `uk_holiday_date` (`holiday_date`),
                           KEY `idx_holiday_active_date` (`is_active`,`holiday_date`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;


-- roomflow.meeting_rooms definition

CREATE TABLE `meeting_rooms` (
                                 `room_id` bigint(20) NOT NULL AUTO_INCREMENT,
                                 `room_name` varchar(50) NOT NULL,
                                 `capacity` int(11) NOT NULL,
                                 `description` text DEFAULT NULL,
                                 `hourly_price` decimal(10,0) NOT NULL DEFAULT 0,
                                 `status` enum('AVAILABLE','MAINTENANCE','INACTIVE') NOT NULL DEFAULT 'AVAILABLE',
                                 `total_reservations` int(11) NOT NULL DEFAULT 0,
                                 `image_url` varchar(1000) DEFAULT NULL,
                                 `created_at` datetime NOT NULL DEFAULT current_timestamp(),
                                 `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
                                 PRIMARY KEY (`room_id`)
) ENGINE=InnoDB AUTO_INCREMENT=54 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;


-- roomflow.reservation_policy definition

CREATE TABLE `reservation_policy` (
                                      `policy_id` bigint(20) NOT NULL AUTO_INCREMENT,
                                      `policy_key` varchar(50) NOT NULL,
                                      `policy_value` varchar(100) DEFAULT NULL,
                                      `description` varchar(255) DEFAULT NULL,
                                      `created_at` datetime NOT NULL DEFAULT current_timestamp(),
                                      `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
                                      PRIMARY KEY (`policy_id`),
                                      UNIQUE KEY `policy_key` (`policy_key`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;


-- roomflow.users definition

CREATE TABLE `users` (
                         `user_id` bigint(20) NOT NULL AUTO_INCREMENT,
                         `name` varchar(50) NOT NULL,
                         `email` varchar(255) NOT NULL,
                         `password` varchar(255) NOT NULL,
                         `role` enum('USER','ADMIN') NOT NULL DEFAULT 'USER',
                         `created_at` datetime NOT NULL DEFAULT current_timestamp(),
                         `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
                         `deleted_at` datetime DEFAULT NULL,
                         PRIMARY KEY (`user_id`),
                         UNIQUE KEY `email` (`email`),
                         KEY `idx_users_deleted_at` (`deleted_at`)
) ENGINE=InnoDB AUTO_INCREMENT=85 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;


-- roomflow.notices definition

CREATE TABLE `notices` (
                           `notice_id` bigint(20) NOT NULL AUTO_INCREMENT,
                           `title` varchar(200) NOT NULL,
                           `content` text NOT NULL,
                           `is_pinned` tinyint(1) DEFAULT 0,
                           `is_visibled` tinyint(1) DEFAULT 1,
                           `view_count` int(11) DEFAULT 0,
                           `created_by` bigint(20) DEFAULT NULL,
                           `updated_by` bigint(20) DEFAULT NULL,
                           `created_at` timestamp NULL DEFAULT current_timestamp(),
                           `updated_at` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
                           PRIMARY KEY (`notice_id`),
                           KEY `fk_notice_created_user` (`created_by`),
                           KEY `fk_notice_updated_user` (`updated_by`),
                           CONSTRAINT `fk_notice_created_user` FOREIGN KEY (`created_by`) REFERENCES `users` (`user_id`) ON DELETE SET NULL,
                           CONSTRAINT `fk_notice_updated_user` FOREIGN KEY (`updated_by`) REFERENCES `users` (`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;


-- roomflow.reservations definition

CREATE TABLE `reservations` (
                                `reservation_id` bigint(20) NOT NULL AUTO_INCREMENT,
                                `user_id` bigint(20) DEFAULT NULL,
                                `room_id` bigint(20) NOT NULL,
                                `idempotency_key` varchar(100) NOT NULL,
                                `status` enum('PENDING','CONFIRMED','CANCELLED','EXPIRED') NOT NULL DEFAULT 'PENDING',
                                `total_amount` decimal(10,0) NOT NULL DEFAULT 0,
                                `memo` text DEFAULT NULL,
                                `cancelled_at` datetime DEFAULT NULL,
                                `cancel_reason` varchar(255) DEFAULT NULL,
                                `created_at` datetime NOT NULL DEFAULT current_timestamp(),
                                `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
                                PRIMARY KEY (`reservation_id`),
                                UNIQUE KEY `uk_reservations_idempotency` (`idempotency_key`),
                                KEY `fk_reservations_room` (`room_id`),
                                KEY `idx_reservations_user_created` (`user_id`,`created_at`),
                                KEY `idx_reservations_status_created` (`status`,`created_at`),
                                CONSTRAINT `fk_reservations_room` FOREIGN KEY (`room_id`) REFERENCES `meeting_rooms` (`room_id`),
                                CONSTRAINT `fk_reservations_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=724 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;


-- roomflow.room_slots definition

CREATE TABLE `room_slots` (
                              `room_slot_id` bigint(20) NOT NULL AUTO_INCREMENT,
                              `room_id` bigint(20) NOT NULL,
                              `slot_start_at` datetime NOT NULL,
                              `slot_end_at` datetime NOT NULL,
                              `is_active` tinyint(1) NOT NULL DEFAULT 1,
                              `created_at` datetime NOT NULL DEFAULT current_timestamp(),
                              `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
                              PRIMARY KEY (`room_slot_id`),
                              UNIQUE KEY `uk_room_slot` (`room_id`,`slot_start_at`,`slot_end_at`),
                              KEY `idx_room_slot_start` (`slot_start_at`),
                              CONSTRAINT `fk_room_slots_room` FOREIGN KEY (`room_id`) REFERENCES `meeting_rooms` (`room_id`),
                              CONSTRAINT `chk_room_slots_time` CHECK (`slot_start_at` < `slot_end_at`)
) ENGINE=InnoDB AUTO_INCREMENT=3878 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;


-- roomflow.social_accounts definition

CREATE TABLE `social_accounts` (
                                   `social_account_id` bigint(20) NOT NULL AUTO_INCREMENT,
                                   `user_id` bigint(20) NOT NULL,
                                   `provider` varchar(30) NOT NULL,
                                   `provider_user_id` varchar(100) NOT NULL,
                                   `created_at` datetime NOT NULL DEFAULT current_timestamp(),
                                   `refresh_token` varchar(500) DEFAULT NULL,
                                   PRIMARY KEY (`social_account_id`),
                                   UNIQUE KEY `uk_social_accounts_provider_user` (`provider`,`provider_user_id`),
                                   KEY `fk_social_accounts_user` (`user_id`),
                                   CONSTRAINT `fk_social_accounts_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;


-- roomflow.payments definition

CREATE TABLE `payments` (
                            `payment_id` bigint(20) NOT NULL AUTO_INCREMENT,
                            `user_id` bigint(20) NOT NULL,
                            `reservation_id` bigint(20) NOT NULL,
                            `order_id` varchar(64) NOT NULL,
                            `order_name` varchar(100) NOT NULL,
                            `status` enum('READY','IN_PROGRESS','DONE','CANCELED','PARTIAL_CANCELED','ABORTED','EXPIRED') NOT NULL DEFAULT 'READY',
                            `payment_key` varchar(200) DEFAULT NULL,
                            `requested_at` datetime DEFAULT NULL,
                            `approved_at` datetime DEFAULT NULL,
                            `room_amount` decimal(10,0) NOT NULL DEFAULT 0,
                            `equipment_amount` decimal(10,0) NOT NULL DEFAULT 0,
                            `total_amount` decimal(10,0) NOT NULL,
                            `balance_amount` decimal(10,0) DEFAULT NULL,
                            `method` varchar(20) DEFAULT NULL,
                            `receipt_url` varchar(500) DEFAULT NULL,
                            `created_at` datetime NOT NULL DEFAULT current_timestamp(),
                            `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
                            PRIMARY KEY (`payment_id`),
                            UNIQUE KEY `order_id` (`order_id`),
                            UNIQUE KEY `payment_key` (`payment_key`),
                            KEY `idx_payments_user_id` (`user_id`),
                            KEY `idx_payments_reservation_id` (`reservation_id`),
                            KEY `idx_payments_order_id` (`order_id`),
                            KEY `idx_payments_status` (`status`),
                            CONSTRAINT `fk_payments_reservation` FOREIGN KEY (`reservation_id`) REFERENCES `reservations` (`reservation_id`),
                            CONSTRAINT `fk_payments_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;


-- roomflow.reservation_equipments definition

CREATE TABLE `reservation_equipments` (
                                          `reservation_equipment_id` bigint(20) NOT NULL AUTO_INCREMENT,
                                          `reservation_id` bigint(20) NOT NULL,
                                          `equipment_id` bigint(20) NOT NULL,
                                          `quantity` int(11) NOT NULL DEFAULT 1,
                                          `status` enum('PENDING','CONFIRMED','CANCELLED','EXPIRED') NOT NULL DEFAULT 'PENDING',
                                          `unit_price` decimal(10,0) NOT NULL DEFAULT 0,
                                          `total_amount` decimal(10,0) NOT NULL DEFAULT 0,
                                          `cancelled_at` datetime DEFAULT NULL,
                                          `cancel_reason` varchar(255) DEFAULT NULL,
                                          `created_at` datetime NOT NULL DEFAULT current_timestamp(),
                                          PRIMARY KEY (`reservation_equipment_id`),
                                          KEY `fk_reservation_equipments_reservation` (`reservation_id`),
                                          KEY `fk_reservation_equipments_equipment` (`equipment_id`),
                                          KEY `idx_reservation_equipments_status_created` (`status`,`created_at`),
                                          CONSTRAINT `fk_reservation_equipments_equipment` FOREIGN KEY (`equipment_id`) REFERENCES `equipment` (`equipment_id`),
                                          CONSTRAINT `fk_reservation_equipments_reservation` FOREIGN KEY (`reservation_id`) REFERENCES `reservations` (`reservation_id`),
                                          CONSTRAINT `chk_reservation_equipment_quantity` CHECK (`quantity` > 0)
) ENGINE=InnoDB AUTO_INCREMENT=824 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;


-- roomflow.reservation_history definition

CREATE TABLE `reservation_history` (
                                       `history_id` bigint(20) NOT NULL AUTO_INCREMENT,
                                       `reservation_id` bigint(20) NOT NULL,
                                       `target_type` enum('RESERVATION','EQUIPMENT') NOT NULL,
                                       `target_id` bigint(20) NOT NULL,
                                       `from_status` enum('PENDING','CONFIRMED','CANCELLED','EXPIRED','NONE') NOT NULL,
                                       `to_status` enum('PENDING','CONFIRMED','CANCELLED','EXPIRED') NOT NULL,
                                       `changed_by` bigint(20) DEFAULT NULL,
                                       `reason` varchar(255) DEFAULT NULL,
                                       `created_at` datetime NOT NULL DEFAULT current_timestamp(),
                                       PRIMARY KEY (`history_id`),
                                       KEY `fk_reservation_history_reservation` (`reservation_id`),
                                       KEY `fk_reservation_history_user` (`changed_by`),
                                       CONSTRAINT `fk_reservation_history_reservation` FOREIGN KEY (`reservation_id`) REFERENCES `reservations` (`reservation_id`),
                                       CONSTRAINT `fk_reservation_history_user` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=2353 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;


-- roomflow.reservation_rooms definition

CREATE TABLE `reservation_rooms` (
                                     `reservation_room_id` bigint(20) NOT NULL AUTO_INCREMENT,
                                     `reservation_id` bigint(20) NOT NULL,
                                     `room_id` bigint(20) NOT NULL,
                                     `room_slot_id` bigint(20) NOT NULL,
                                     `amount` decimal(10,0) NOT NULL DEFAULT 0,
                                     `created_at` datetime NOT NULL DEFAULT current_timestamp(),
                                     PRIMARY KEY (`reservation_room_id`),
                                     UNIQUE KEY `uk_reservation_room_slot` (`reservation_id`,`room_slot_id`),
                                     KEY `fk_reservation_rooms_room` (`room_id`),
                                     KEY `idx_reservation_room_room_slot_id` (`room_slot_id`),
                                     KEY `idx_reservation_room_reservation_id_id` (`reservation_id`),
                                     CONSTRAINT `fk_reservation_rooms_reservation` FOREIGN KEY (`reservation_id`) REFERENCES `reservations` (`reservation_id`),
                                     CONSTRAINT `fk_reservation_rooms_room` FOREIGN KEY (`room_id`) REFERENCES `meeting_rooms` (`room_id`),
                                     CONSTRAINT `fk_reservation_rooms_room_slot` FOREIGN KEY (`room_slot_id`) REFERENCES `room_slots` (`room_slot_id`)
) ENGINE=InnoDB AUTO_INCREMENT=908 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;


-- roomflow.payment_cancels definition

CREATE TABLE `payment_cancels` (
                                   `cancel_id` bigint(20) NOT NULL AUTO_INCREMENT,
                                   `payment_id` bigint(20) NOT NULL,
                                   `reservation_equipment_id` bigint(20) DEFAULT NULL,
                                   `transaction_key` varchar(100) DEFAULT NULL,
                                   `receipt_key` varchar(100) DEFAULT NULL,
                                   `cancel_amount` decimal(10,0) NOT NULL,
                                   `tax_free_amount` decimal(10,0) DEFAULT 0,
                                   `refundable_amount` decimal(10,0) DEFAULT 0,
                                   `cancel_status` enum('PENDING','DONE','FAILED') NOT NULL DEFAULT 'PENDING',
                                   `cancel_reason` varchar(255) NOT NULL,
                                   `canceled_at` datetime DEFAULT NULL,
                                   `created_at` datetime NOT NULL DEFAULT current_timestamp(),
                                   `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
                                   PRIMARY KEY (`cancel_id`),
                                   KEY `fk_payment_cancels_equipment` (`reservation_equipment_id`),
                                   KEY `idx_payment_cancels_payment_id` (`payment_id`),
                                   CONSTRAINT `fk_payment_cancels_equipment` FOREIGN KEY (`reservation_equipment_id`) REFERENCES `reservation_equipments` (`reservation_equipment_id`),
                                   CONSTRAINT `fk_payment_cancels_payment` FOREIGN KEY (`payment_id`) REFERENCES `payments` (`payment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;