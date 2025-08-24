-- =================================================================================================
-- POSTGRESQL SCHEMA CREATION SCRIPT FOR LAPTOP REPAIR APPLICATION
-- Creates all tables for dev and prod PostgreSQL environments
-- =================================================================================================

-- -------------------------------------------------------------------------------------------------
-- Section 1: APPLICATION CORE TABLES
-- -------------------------------------------------------------------------------------------------

-- Service Item table
CREATE TABLE IF NOT EXISTS service_item (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price NUMERIC(38,2) NOT NULL,
    vat_rate NUMERIC(38,2) NOT NULL,
    warranty_days INTEGER NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- Request table
CREATE TABLE IF NOT EXISTS request (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    address VARCHAR(255),
    brand_model VARCHAR(255),
    serial_number VARCHAR(255),
    appointment_date TIMESTAMP NOT NULL,
    description VARCHAR(1000) NOT NULL,
    status VARCHAR(50) CHECK (status IN ('APPROVE_QUOTED','CANCELLED','COMPLETED','IN_PROGRESS','QUOTED','SCHEDULED','UNDER_WARRANTY')),
    completed_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- Request Items table
CREATE TABLE IF NOT EXISTS request_items (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL,
    service_item_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    price NUMERIC(38,2) NOT NULL,
    vat_rate NUMERIC(38,2) NOT NULL,
    quantity INTEGER NOT NULL,
    discount NUMERIC(38,2),
    warranty_days INTEGER NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    FOREIGN KEY (request_id) REFERENCES request(id),
    FOREIGN KEY (service_item_id) REFERENCES service_item(id)
);

-- Request History table
CREATE TABLE IF NOT EXISTS request_history (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL,
    changes TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255),
    FOREIGN KEY (request_id) REFERENCES request(id)
);

-- Request Images table
CREATE TABLE IF NOT EXISTS request_images (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL,
    images VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    FOREIGN KEY (request_id) REFERENCES request(id)
);

-- -------------------------------------------------------------------------------------------------
-- Section 2: SPRING AI CHAT MEMORY TABLE
-- -------------------------------------------------------------------------------------------------

-- Spring AI Chat Memory table for conversation history
CREATE TABLE IF NOT EXISTS SPRING_AI_CHAT_MEMORY (
    conversation_id VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL')),
    "timestamp" TIMESTAMP NOT NULL
);

-- -------------------------------------------------------------------------------------------------
-- Section 3: PERFORMANCE INDEXES
-- -------------------------------------------------------------------------------------------------

-- Service Item indexes
CREATE INDEX IF NOT EXISTS idx_service_item_active ON service_item(active);
CREATE INDEX IF NOT EXISTS idx_service_item_name ON service_item(name);
CREATE INDEX IF NOT EXISTS idx_service_item_created_at ON service_item(created_at);

-- Request indexes
CREATE INDEX IF NOT EXISTS idx_request_status ON request(status);
CREATE INDEX IF NOT EXISTS idx_request_appointment_date ON request(appointment_date);
CREATE INDEX IF NOT EXISTS idx_request_phone ON request(phone);
CREATE INDEX IF NOT EXISTS idx_request_email ON request(email);
CREATE INDEX IF NOT EXISTS idx_request_serial_number ON request(serial_number);
CREATE INDEX IF NOT EXISTS idx_request_brand_model ON request(brand_model);
CREATE INDEX IF NOT EXISTS idx_request_created_at ON request(created_at);
CREATE INDEX IF NOT EXISTS idx_request_completed_at ON request(completed_at);

-- Request Items indexes
CREATE INDEX IF NOT EXISTS idx_request_items_request_id ON request_items(request_id);
CREATE INDEX IF NOT EXISTS idx_request_items_service_item_id ON request_items(service_item_id);
CREATE INDEX IF NOT EXISTS idx_request_items_created_at ON request_items(created_at);

-- Request History indexes
CREATE INDEX IF NOT EXISTS idx_request_history_request_id ON request_history(request_id);
CREATE INDEX IF NOT EXISTS idx_request_history_created_at ON request_history(created_at);

-- Request Images indexes
CREATE INDEX IF NOT EXISTS idx_request_images_request_id ON request_images(request_id);

-- Spring AI Chat Memory indexes
CREATE INDEX IF NOT EXISTS SPRING_AI_CHAT_MEMORY_CONVERSATION_ID_TIMESTAMP_IDX
ON SPRING_AI_CHAT_MEMORY(conversation_id, "timestamp");

-- =================================================================================================
-- END OF SCHEMA CREATION SCRIPT
-- =================================================================================================