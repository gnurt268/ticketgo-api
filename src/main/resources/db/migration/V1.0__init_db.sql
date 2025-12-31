-- ============================================
-- TicketGo Database Initialization Script
-- Version: 1.0
-- Author: Trung Nguyen
-- Description: Complete database schema for TicketGo event management system
-- ============================================

-- Drop tables if exists (for clean install)
DROP TABLE IF EXISTS ai_recommendations;
DROP TABLE IF EXISTS user_event_interactions;
DROP TABLE IF EXISTS check_ins;
DROP TABLE IF EXISTS face_embeddings;
DROP TABLE IF EXISTS reviews;
DROP TABLE IF EXISTS tickets;
DROP TABLE IF EXISTS seats;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS ticket_zones;
DROP TABLE IF EXISTS events;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS users;

-- ============================================
-- TABLE: users
-- Description: User accounts (USER, ORGANIZER, ADMIN)
-- ============================================
CREATE TABLE users (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       full_name VARCHAR(255) NOT NULL,
                       phone VARCHAR(20),
                       avatar_url VARCHAR(500),
                       role VARCHAR(20) NOT NULL DEFAULT 'USER',
                       is_active BOOLEAN NOT NULL DEFAULT TRUE,
                       email_verified BOOLEAN NOT NULL DEFAULT FALSE,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                       INDEX idx_email (email),
                       INDEX idx_role (role),
                       INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- TABLE: categories
-- Description: Event categories (Concert, Workshop, Sports, etc.)
-- ============================================
CREATE TABLE categories (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            name VARCHAR(100) NOT NULL UNIQUE,
                            slug VARCHAR(100) NOT NULL UNIQUE,
                            description TEXT,
                            icon_url VARCHAR(500),
                            display_order INT NOT NULL DEFAULT 0,
                            is_active BOOLEAN NOT NULL DEFAULT TRUE,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                            INDEX idx_slug (slug),
                            INDEX idx_display_order (display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- TABLE: events
-- Description: Main events table
-- ============================================
CREATE TABLE events (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        organizer_id BIGINT NOT NULL,
                        category_id BIGINT NOT NULL,
                        title VARCHAR(255) NOT NULL,
                        slug VARCHAR(255) NOT NULL UNIQUE,
                        description TEXT,
                        poster_url VARCHAR(500),
                        banner_url VARCHAR(500),
                        location VARCHAR(255) NOT NULL,
                        venue VARCHAR(255) NOT NULL,
                        address TEXT,
                        city VARCHAR(100),
                        start_date TIMESTAMP NOT NULL,
                        end_date TIMESTAMP NOT NULL,
                        status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
                        event_type VARCHAR(20) DEFAULT 'OUTDOOR',
                        is_featured BOOLEAN NOT NULL DEFAULT FALSE,
                        max_tickets_per_order INT NOT NULL DEFAULT 10,
                        enable_seat_selection BOOLEAN NOT NULL DEFAULT FALSE,
                        seat_map_image_url VARCHAR(500),
                        enable_face_recognition BOOLEAN NOT NULL DEFAULT TRUE,
                        face_recognition_threshold DECIMAL(3,2) DEFAULT 0.70,
                        require_face_upload BOOLEAN NOT NULL DEFAULT TRUE,
                        view_count INT NOT NULL DEFAULT 0,
                        total_tickets_sold INT NOT NULL DEFAULT 0,
                        total_revenue DECIMAL(15,2) DEFAULT 0.00,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                        FOREIGN KEY (organizer_id) REFERENCES users(id) ON DELETE CASCADE,
                        FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT,

                        INDEX idx_organizer (organizer_id),
                        INDEX idx_category (category_id),
                        INDEX idx_slug (slug),
                        INDEX idx_status (status),
                        INDEX idx_start_date (start_date),
                        INDEX idx_city (city),
                        INDEX idx_featured (is_featured),
                        INDEX idx_event_type (event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- TABLE: ticket_zones
-- Description: Ticket zones/sections for events (VIP, Standard, etc.)
-- ============================================
CREATE TABLE ticket_zones (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              event_id BIGINT NOT NULL,
                              zone_name VARCHAR(100) NOT NULL,
                              zone_code VARCHAR(50) NOT NULL,
                              description TEXT,
                              color_code VARCHAR(7) DEFAULT '#3B82F6',
                              price DECIMAL(15,2) NOT NULL,
                              currency VARCHAR(3) DEFAULT 'VND',
                              total_capacity INT NOT NULL,
                              available_capacity INT NOT NULL,
                              reserved_capacity INT NOT NULL DEFAULT 0,
                              zone_type VARCHAR(20) DEFAULT 'STANDARD',
                              is_active BOOLEAN NOT NULL DEFAULT TRUE,
                              display_order INT NOT NULL DEFAULT 0,
                              sale_start_date TIMESTAMP NULL,
                              sale_end_date TIMESTAMP NULL,
                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                              FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,

                              UNIQUE KEY unique_event_zone (event_id, zone_code),
                              INDEX idx_event (event_id),
                              INDEX idx_zone_code (zone_code),
                              INDEX idx_price (price)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- TABLE: seats
-- Description: Individual seats for INDOOR events
-- ============================================
CREATE TABLE seats (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       ticket_zone_id BIGINT NOT NULL,
                       row_label VARCHAR(10) NOT NULL,
                       seat_number INT NOT NULL,
                       seat_code VARCHAR(20) NOT NULL,
                       position_x INT NOT NULL,
                       position_y INT NOT NULL,
                       status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
                       price DECIMAL(15,2) NOT NULL,
                       seat_type VARCHAR(20) DEFAULT 'STANDARD',
                       reserved_by BIGINT NULL,
                       reserved_until TIMESTAMP NULL,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                       FOREIGN KEY (ticket_zone_id) REFERENCES ticket_zones(id) ON DELETE CASCADE,
                       FOREIGN KEY (reserved_by) REFERENCES users(id) ON DELETE SET NULL,

                       UNIQUE KEY unique_seat (ticket_zone_id, seat_code),
                       INDEX idx_zone (ticket_zone_id),
                       INDEX idx_status (status),
                       INDEX idx_row (row_label),
                       INDEX idx_reservation (reserved_by, reserved_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- TABLE: orders
-- Description: Purchase orders
-- ============================================
CREATE TABLE orders (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        order_code VARCHAR(50) NOT NULL UNIQUE,
                        user_id BIGINT NOT NULL,
                        event_id BIGINT NOT NULL,
                        total_amount DECIMAL(15,2) NOT NULL,
                        currency VARCHAR(3) DEFAULT 'VND',
                        quantity INT NOT NULL,
                        payment_method VARCHAR(20) DEFAULT 'VNPAY',
                        payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                        payment_transaction_id VARCHAR(255),
                        paid_at TIMESTAMP NULL,
                        buyer_name VARCHAR(255) NOT NULL,
                        buyer_email VARCHAR(255) NOT NULL,
                        buyer_phone VARCHAR(20) NOT NULL,
                        notes TEXT,
                        ip_address VARCHAR(45),
                        user_agent TEXT,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                        FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,

                        INDEX idx_user (user_id),
                        INDEX idx_event (event_id),
                        INDEX idx_order_code (order_code),
                        INDEX idx_payment_status (payment_status),
                        INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- TABLE: tickets
-- Description: Individual tickets (1 ticket = 1 person)
-- ============================================
CREATE TABLE tickets (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         ticket_code VARCHAR(50) NOT NULL UNIQUE,
                         order_id BIGINT NOT NULL,
                         event_id BIGINT NOT NULL,
                         ticket_zone_id BIGINT NOT NULL,
                         seat_id BIGINT NULL,
                         holder_name VARCHAR(255) NOT NULL,
                         holder_email VARCHAR(255) NOT NULL,
                         holder_phone VARCHAR(20) NOT NULL,
                         holder_id_number VARCHAR(50),
                         seat_number VARCHAR(20),
                         `row_number` VARCHAR(10),
                         qr_code VARCHAR(500) NOT NULL UNIQUE,
                         face_image_url VARCHAR(500),
                         face_uploaded_at TIMESTAMP NULL,
                         face_embedding_id BIGINT NULL,
                         status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                         is_checked_in BOOLEAN NOT NULL DEFAULT FALSE,
                         checked_in_at TIMESTAMP NULL,
                         checked_in_by VARCHAR(20),
                         checked_in_confidence DECIMAL(4,2),
                         transferred_from_email VARCHAR(255),
                         transferred_at TIMESTAMP NULL,
                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                         FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
                         FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
                         FOREIGN KEY (ticket_zone_id) REFERENCES ticket_zones(id) ON DELETE RESTRICT,
                         FOREIGN KEY (seat_id) REFERENCES seats(id) ON DELETE SET NULL,

                         INDEX idx_order (order_id),
                         INDEX idx_event (event_id),
                         INDEX idx_zone (ticket_zone_id),
                         INDEX idx_seat (seat_id),
                         INDEX idx_ticket_code (ticket_code),
                         INDEX idx_qr_code (qr_code),
                         INDEX idx_holder_email (holder_email),
                         INDEX idx_status (status),
                         INDEX idx_checked_in (is_checked_in)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- TABLE: reviews
-- Description: Event reviews by users
-- ============================================
CREATE TABLE reviews (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         user_id BIGINT NOT NULL,
                         event_id BIGINT NOT NULL,
                         order_id BIGINT NULL,
                         rating INT NOT NULL,
                         title VARCHAR(255),
                         comment TEXT,
                         sentiment_score DECIMAL(4,2),
                         sentiment_label VARCHAR(20),
                         is_approved BOOLEAN NOT NULL DEFAULT FALSE,
                         is_reported BOOLEAN NOT NULL DEFAULT FALSE,
                         moderation_notes TEXT,
                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                         FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                         FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
                         FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL,

                         UNIQUE KEY unique_user_event_review (user_id, event_id),
                         INDEX idx_user (user_id),
                         INDEX idx_event (event_id),
                         INDEX idx_rating (rating),
                         INDEX idx_sentiment (sentiment_label)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- TABLE: face_embeddings
-- Description: AI face recognition data (512-dim vectors)
-- ============================================
CREATE TABLE face_embeddings (
                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 ticket_id BIGINT NOT NULL UNIQUE,
                                 embedding JSON NOT NULL,
                                 image_url VARCHAR(500) NOT NULL,
                                 image_type VARCHAR(20) DEFAULT 'NORMAL',
                                 face_quality_score DECIMAL(4,2),
                                 face_detected BOOLEAN NOT NULL DEFAULT TRUE,
                                 face_alignment_score DECIMAL(4,2),
                                 model_name VARCHAR(100) DEFAULT 'InsightFace-ArcFace',
                                 model_version VARCHAR(50) DEFAULT '1.0',
                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                 FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,

                                 INDEX idx_ticket (ticket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- TABLE: check_ins
-- Description: Check-in audit log
-- ============================================
CREATE TABLE check_ins (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           ticket_id BIGINT NOT NULL,
                           event_id BIGINT NOT NULL,
                           check_in_method VARCHAR(20) NOT NULL,
                           check_in_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           face_image_url VARCHAR(500),
                           face_match_confidence DECIMAL(4,2),
                           face_match_ticket_id BIGINT,
                           check_in_location VARCHAR(255),
                           device_info TEXT,
                           ip_address VARCHAR(45),
                           staff_user_id BIGINT NULL,
                           staff_notes TEXT,
                           is_successful BOOLEAN NOT NULL DEFAULT TRUE,
                           failure_reason TEXT,
                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
                           FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
                           FOREIGN KEY (staff_user_id) REFERENCES users(id) ON DELETE SET NULL,

                           INDEX idx_ticket (ticket_id),
                           INDEX idx_event (event_id),
                           INDEX idx_check_in_at (check_in_at),
                           INDEX idx_method (check_in_method)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- TABLE: user_event_interactions
-- Description: User behavior tracking for AI recommendations
-- ============================================
CREATE TABLE user_event_interactions (
                                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                         user_id BIGINT NOT NULL,
                                         event_id BIGINT NOT NULL,
                                         interaction_type VARCHAR(20) NOT NULL,
                                         session_id VARCHAR(255),
                                         referrer_url TEXT,
                                         device_type VARCHAR(20),
                                         duration_seconds INT,
                                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                         FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                         FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,

                                         INDEX idx_user (user_id),
                                         INDEX idx_event (event_id),
                                         INDEX idx_interaction_type (interaction_type),
                                         INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- TABLE: ai_recommendations
-- Description: Cached AI recommendation results
-- ============================================
CREATE TABLE ai_recommendations (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    user_id BIGINT NOT NULL,
                                    event_id BIGINT NOT NULL,
                                    score DECIMAL(5,4) NOT NULL,
                                    algorithm VARCHAR(30) NOT NULL,
                                    reason TEXT,
                                    model_version VARCHAR(50),
                                    is_clicked BOOLEAN NOT NULL DEFAULT FALSE,
                                    clicked_at TIMESTAMP NULL,
                                    expires_at TIMESTAMP NULL,
                                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,

                                    UNIQUE KEY unique_user_event_rec (user_id, event_id, algorithm),
                                    INDEX idx_user (user_id),
                                    INDEX idx_event (event_id),
                                    INDEX idx_score (score),
                                    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- SAMPLE DATA
-- ============================================

-- Insert admin user (password: admin123)
INSERT INTO users (email, password, full_name, phone, role, is_active, email_verified) VALUES
    ('admin@ticketgo.com', '$2a$10$xN3GXyYWPvCfHMZvvBDPWO7H1l2EQYdLFPDjN4l1nXCmJBpVkJLV2', 'Admin TicketGO', '0987654321', 'ADMIN', TRUE, TRUE);

-- Insert sample categories
INSERT INTO categories (name, slug, description, display_order, is_active) VALUES
                                                                               ('Concert', 'concert', 'Live music performances and concerts', 1, TRUE),
                                                                               ('Workshop', 'workshop', 'Educational workshops and training sessions', 2, TRUE),
                                                                               ('Festival', 'festival', 'Cultural festivals and celebrations', 3, TRUE),
                                                                               ('Sports', 'sports', 'Sports events and competitions', 4, TRUE),
                                                                               ('Conference', 'conference', 'Business conferences and seminars', 5, TRUE),
                                                                               ('Exhibition', 'exhibition', 'Art exhibitions and galleries', 6, TRUE),
                                                                               ('Theater', 'theater', 'Theater performances and plays', 7, TRUE);

-- ============================================
-- STORED PROCEDURES
-- ============================================

-- Procedure to generate seats for INDOOR events
DELIMITER //

CREATE PROCEDURE sp_generate_seats(
    IN p_ticket_zone_id BIGINT,
    IN p_rows INT,
    IN p_seats_per_row INT,
    IN p_base_price DECIMAL(15,2)
)
BEGIN
    DECLARE v_row INT DEFAULT 1;
    DECLARE v_seat INT;
    DECLARE v_row_label VARCHAR(10);
    DECLARE v_seat_code VARCHAR(20);

    WHILE v_row <= p_rows DO
        -- Generate row label (A, B, C, ..., Z, AA, AB, ...)
        IF v_row <= 26 THEN
            SET v_row_label = CHAR(64 + v_row);
ELSE
            SET v_row_label = CONCAT(CHAR(64 + FLOOR((v_row - 1) / 26)), CHAR(64 + ((v_row - 1) % 26) + 1));
END IF;

        SET v_seat = 1;
        WHILE v_seat <= p_seats_per_row DO
            SET v_seat_code = CONCAT(v_row_label, v_seat);

INSERT INTO seats (
    ticket_zone_id,
    row_label,
    seat_number,
    seat_code,
    position_x,
    position_y,
    status,
    price,
    seat_type
) VALUES (
             p_ticket_zone_id,
             v_row_label,
             v_seat,
             v_seat_code,
             v_seat - 1,
             v_row - 1,
             'AVAILABLE',
             p_base_price,
             'STANDARD'
         );

SET v_seat = v_seat + 1;
END WHILE;

        SET v_row = v_row + 1;
END WHILE;
END //

DELIMITER ;

-- ============================================
-- END OF SCRIPT
-- ============================================