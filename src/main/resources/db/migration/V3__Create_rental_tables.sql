-- Création des tables pour la gestion locative

-- Table des contrats de location
CREATE TABLE rental_contracts (
    id BIGSERIAL PRIMARY KEY,
    property_id UUID NOT NULL REFERENCES listings(id),
    owner_id UUID NOT NULL REFERENCES users(id),
    tenant_id UUID NOT NULL REFERENCES users(id),
    contract_number VARCHAR(50) UNIQUE NOT NULL,
    monthly_rent DECIMAL(10,2) NOT NULL,
    deposit DECIMAL(10,2),
    start_date DATE NOT NULL,
    end_date DATE,
    payment_due_day INTEGER NOT NULL CHECK (payment_due_day >= 1 AND payment_due_day <= 31),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Table des paiements de loyer
CREATE TABLE rent_payments (
    id BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL REFERENCES rental_contracts(id),
    amount DECIMAL(10,2) NOT NULL,
    due_date DATE NOT NULL,
    paid_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(50),
    transaction_id VARCHAR(100),
    cinetpay_transaction_id VARCHAR(100),
    payment_reference VARCHAR(100),
    late_fee DECIMAL(10,2) DEFAULT 0,
    notes TEXT,
    receipt_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Table des transactions de paiement
CREATE TABLE payment_transactions (
    id BIGSERIAL PRIMARY KEY,
    rent_payment_id BIGINT NOT NULL REFERENCES rent_payments(id),
    stripe_payment_intent_id VARCHAR(100) UNIQUE NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    status VARCHAR(20) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    stripe_customer_id VARCHAR(100),
    payment_method_id VARCHAR(100),
    client_ip VARCHAR(45),
    user_agent TEXT,
    success_url VARCHAR(500),
    cancel_url VARCHAR(500),
    description TEXT,
    metadata TEXT,
    processed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Index pour optimiser les requêtes
CREATE INDEX idx_rental_contracts_owner ON rental_contracts(owner_id);
CREATE INDEX idx_rental_contracts_tenant ON rental_contracts(tenant_id);
CREATE INDEX idx_rental_contracts_property ON rental_contracts(property_id);
CREATE INDEX idx_rental_contracts_status ON rental_contracts(status);

CREATE INDEX idx_rent_payments_contract ON rent_payments(contract_id);
CREATE INDEX idx_rent_payments_status ON rent_payments(status);
CREATE INDEX idx_rent_payments_due_date ON rent_payments(due_date);
CREATE INDEX idx_rent_payments_overdue ON rent_payments(status, due_date) WHERE status = 'PENDING';

CREATE INDEX idx_payment_transactions_stripe ON payment_transactions(stripe_payment_intent_id);
CREATE INDEX idx_payment_transactions_rent_payment ON payment_transactions(rent_payment_id);
CREATE INDEX idx_payment_transactions_status ON payment_transactions(status);

-- Triggers pour mettre à jour updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_rental_contracts_updated_at 
    BEFORE UPDATE ON rental_contracts 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_rent_payments_updated_at 
    BEFORE UPDATE ON rent_payments 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_payment_transactions_updated_at 
    BEFORE UPDATE ON payment_transactions 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Contraintes de validation
ALTER TABLE rental_contracts 
ADD CONSTRAINT check_payment_due_day 
CHECK (payment_due_day >= 1 AND payment_due_day <= 31);

ALTER TABLE rental_contracts 
ADD CONSTRAINT check_contract_dates 
CHECK (end_date IS NULL OR end_date > start_date);

ALTER TABLE rent_payments 
ADD CONSTRAINT check_payment_amount 
CHECK (amount > 0);

ALTER TABLE rent_payments 
ADD CONSTRAINT check_payment_dates 
CHECK (paid_date IS NULL OR paid_date >= due_date);

-- Commentaires pour la documentation
COMMENT ON TABLE rental_contracts IS 'Contrats de location entre propriétaires et locataires';
COMMENT ON TABLE rent_payments IS 'Paiements de loyer mensuels';
COMMENT ON TABLE payment_transactions IS 'Transactions de paiement via Stripe';

COMMENT ON COLUMN rental_contracts.payment_due_day IS 'Jour du mois où le loyer est dû (1-31)';
COMMENT ON COLUMN rent_payments.late_fee IS 'Frais de retard calculés automatiquement';
COMMENT ON COLUMN payment_transactions.stripe_payment_intent_id IS 'ID unique de la transaction Stripe';
