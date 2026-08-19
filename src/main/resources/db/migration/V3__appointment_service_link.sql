-- Permite (opcionalmente) associar uma consulta a um item do catálogo de
-- serviços, usado no relatório de faturamento por serviço.
ALTER TABLE appointments ADD COLUMN service_id UUID NULL;
ALTER TABLE appointments ADD CONSTRAINT fk_appointments_service FOREIGN KEY (service_id) REFERENCES services (id);
CREATE INDEX idx_appointments_service_id ON appointments (service_id);
