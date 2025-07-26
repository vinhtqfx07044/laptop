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
    name VARCHAR(255),
    price NUMERIC(38,2),
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
    appointment_date TIMESTAMP NOT NULL,
    description VARCHAR(1000) NOT NULL,
    status VARCHAR(50) CHECK (status IN ('APPROVE_QUOTED','CANCELLED','COMPLETED','IN_PROGRESS','QUOTED','SCHEDULED')),
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
    name VARCHAR(255),
    price NUMERIC(38,2),
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
    changes TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
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

-- =================================================================================================
-- END OF SCHEMA CREATION SCRIPT
-- =================================================================================================