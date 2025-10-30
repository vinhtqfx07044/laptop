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
    name VARCHAR(100) NOT NULL CHECK (LENGTH(name) >= 3),
    phone VARCHAR(10) NOT NULL CHECK (phone ~ '^0[0-9]{9}$'),
    email VARCHAR(255),
    address VARCHAR(255),
    brand_model VARCHAR(255),
    serial_number VARCHAR(255),
    appointment_date TIMESTAMP NOT NULL,
    description VARCHAR(1000) NOT NULL CHECK (LENGTH(description) >= 10),
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

-- Spring AI Chat Memory table for conversation history
CREATE TABLE IF NOT EXISTS SPRING_AI_CHAT_MEMORY (
    conversation_id VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL')),
    "timestamp" TIMESTAMP NOT NULL
);

-- Spring AI Vector Store table for document embeddings
CREATE TABLE IF NOT EXISTS vector_store (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    content TEXT NOT NULL,
    metadata JSON,
    embedding vector(1536)
);

-- Document metadata table for PDF management
CREATE TABLE IF NOT EXISTS document (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(500) NOT NULL,
    description TEXT,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    file_extension VARCHAR(10) NOT NULL DEFAULT 'pdf',
    processing_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    processing_error TEXT,
    embedding_chunk_count INTEGER,
    embedding_model VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(255) DEFAULT 'system',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(255)
);

-- Create essential indexes
CREATE INDEX IF NOT EXISTS idx_request_status ON request(status);
CREATE INDEX IF NOT EXISTS idx_request_appointment_date ON request(appointment_date);
CREATE INDEX IF NOT EXISTS idx_service_item_active ON service_item(active);
CREATE INDEX IF NOT EXISTS idx_request_items_request_id ON request_items(request_id);
CREATE INDEX IF NOT EXISTS idx_request_items_service_item_id ON request_items(service_item_id);
CREATE INDEX IF NOT EXISTS idx_request_history_request_id ON request_history(request_id);
CREATE INDEX IF NOT EXISTS idx_request_images_request_id ON request_images(request_id);
CREATE INDEX IF NOT EXISTS SPRING_AI_CHAT_MEMORY_CONVERSATION_ID_TIMESTAMP_IDX
ON SPRING_AI_CHAT_MEMORY(conversation_id, "timestamp");
CREATE INDEX IF NOT EXISTS vector_store_embedding_idx
ON vector_store USING hnsw (embedding vector_cosine_ops);
