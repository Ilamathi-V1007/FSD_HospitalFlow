-- =========================================================
-- Patient Flow Coordination System — Week 1 Locked Schema
-- =========================================================

-- ===== AUTH MODULE =====
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN','DOCTOR','RECEPTIONIST','LAB_TECH','PHARMACIST')),
    created_at TIMESTAMP DEFAULT now()
);

-- ===== QUEUE MODULE =====
CREATE TABLE patients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    age INT,
    gender VARCHAR(10),
    contact VARCHAR(20),
    created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE queue_entries (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id),
    symptoms TEXT,
    urgency_level INT NOT NULL CHECK (urgency_level BETWEEN 1 AND 5),
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING' CHECK (status IN ('WAITING','IN_CONSULT','COMPLETED')),
    checked_in_at TIMESTAMP DEFAULT now(),
    priority_score DOUBLE PRECISION
);

-- ===== ALLOCATION MODULE =====
CREATE TABLE doctors (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    department VARCHAR(50),
    availability_status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE'
        CHECK (availability_status IN ('AVAILABLE','BUSY','ON_BREAK'))
);

CREATE TABLE beds (
    id BIGSERIAL PRIMARY KEY,
    bed_number VARCHAR(10) UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE','OCCUPIED'))
);

CREATE TABLE appointments (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id),
    doctor_id BIGINT NOT NULL REFERENCES doctors(id),
    bed_id BIGINT REFERENCES beds(id),
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS' CHECK (status IN ('IN_PROGRESS','COMPLETED')),
    started_at TIMESTAMP DEFAULT now(),
    ended_at TIMESTAMP
);

-- ===== LAB / PHARMACY MODULE =====
CREATE TABLE lab_requests (
    id BIGSERIAL PRIMARY KEY,
    appointment_id BIGINT NOT NULL REFERENCES appointments(id),
    test_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','COMPLETED')),
    result TEXT,
    requested_at TIMESTAMP DEFAULT now()
);

CREATE TABLE prescriptions (
    id BIGSERIAL PRIMARY KEY,
    appointment_id BIGINT NOT NULL REFERENCES appointments(id),
    medicine_details TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','DISPENSED')),
    dispensed_at TIMESTAMP,
    dispensed_by VARCHAR(100)
);

-- ===== PHARMACY INVENTORY (additive; Hibernate ddl-auto=update creates these on existing DBs) =====
CREATE TABLE medicines (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    strength VARCHAR(60),
    dosage_form VARCHAR(60),
    unit VARCHAR(30) NOT NULL,
    quantity INT NOT NULL,
    reorder_threshold INT NOT NULL,
    medicine_key VARCHAR(300) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_medicines_medicine_key UNIQUE (medicine_key),
    CHECK (quantity >= 0 AND reorder_threshold >= 0)
);

CREATE TABLE stock_movements (
    id BIGSERIAL PRIMARY KEY,
    medicine_id BIGINT NOT NULL REFERENCES medicines(id),
    type VARCHAR(20) NOT NULL CHECK (type IN ('INITIAL','RESTOCK','DISPENSE')),
    quantity_change INT NOT NULL,
    quantity_after INT NOT NULL,
    prescription_id BIGINT REFERENCES prescriptions(id),
    performed_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_stock_movements_prescription_medicine UNIQUE (prescription_id, medicine_id)
);

CREATE TABLE visit_summaries (
    id BIGSERIAL PRIMARY KEY,
    appointment_id BIGINT NOT NULL REFERENCES appointments(id),
    diagnosis TEXT,
    discharged_at TIMESTAMP DEFAULT now()
);

-- Helpful indexes for the queue/allocation logic
CREATE INDEX idx_queue_status ON queue_entries(status);
CREATE INDEX idx_queue_priority ON queue_entries(priority_score DESC);
CREATE INDEX idx_appointments_status ON appointments(status);
CREATE INDEX idx_beds_status ON beds(status);
