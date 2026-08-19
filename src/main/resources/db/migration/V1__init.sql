-- Baseline schema, equivalent to what Hibernate's ddl-auto=update had been
-- generating from the JPA entities. From this point on, schema changes are
-- versioned Flyway migrations instead of auto-generated at boot.

CREATE TABLE users (
    id UUID NOT NULL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE clients (
    id UUID NOT NULL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    CONSTRAINT uk_clients_email UNIQUE (email)
);

CREATE TABLE pets (
    id UUID NOT NULL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    name VARCHAR(255) NOT NULL,
    species VARCHAR(255) NOT NULL,
    breed VARCHAR(255),
    birth_date DATE,
    weight_kg DOUBLE PRECISION,
    notes VARCHAR(255),
    client_id UUID NOT NULL,
    CONSTRAINT fk_pets_client FOREIGN KEY (client_id) REFERENCES clients (id)
);
CREATE INDEX idx_pets_client_id ON pets (client_id);

CREATE TABLE appointments (
    id UUID NOT NULL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    scheduled_at TIMESTAMP NOT NULL,
    duration_min INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    reason VARCHAR(255),
    pet_id UUID NOT NULL,
    vet_id UUID NOT NULL,
    CONSTRAINT fk_appointments_pet FOREIGN KEY (pet_id) REFERENCES pets (id),
    CONSTRAINT fk_appointments_vet FOREIGN KEY (vet_id) REFERENCES users (id)
);
CREATE INDEX idx_appointments_pet_id ON appointments (pet_id);
CREATE INDEX idx_appointments_vet_id ON appointments (vet_id);

CREATE TABLE medical_records (
    id UUID NOT NULL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    diagnosis TEXT,
    notes TEXT,
    weight_kg DOUBLE PRECISION,
    vaccines TEXT,
    prescriptions TEXT,
    appointment_id UUID NOT NULL,
    pet_id UUID NOT NULL,
    CONSTRAINT uk_medical_records_appointment UNIQUE (appointment_id),
    CONSTRAINT fk_medical_records_appointment FOREIGN KEY (appointment_id) REFERENCES appointments (id),
    CONSTRAINT fk_medical_records_pet FOREIGN KEY (pet_id) REFERENCES pets (id)
);
CREATE INDEX idx_medical_records_pet_id ON medical_records (pet_id);

CREATE TABLE services (
    id UUID NOT NULL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    price_cents INTEGER NOT NULL,
    duration_min INTEGER NOT NULL,
    active BOOLEAN NOT NULL
);
