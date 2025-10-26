-- Création des tables pour la gestion locative

-- Table des contrats de location
CREATE TABLE IF NOT EXISTS rental_contracts (
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
CREATE TABLE IF NOT EXISTS rent_payments (
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
CREATE TABLE IF NOT EXISTS payment_transactions (
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

-- Table des paiements fractionnés
CREATE TABLE IF NOT EXISTS split_payments (
    id BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL REFERENCES rental_contracts(id),
    total_amount DECIMAL(10,2) NOT NULL,
    deposit_percentage INTEGER NOT NULL DEFAULT 30,
    deposit_amount DECIMAL(10,2) NOT NULL,
    balance_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Table des éléments de paiement fractionné
CREATE TABLE IF NOT EXISTS split_payment_items (
    id BIGSERIAL PRIMARY KEY,
    split_payment_id BIGINT NOT NULL REFERENCES split_payments(id),
    payment_type VARCHAR(20) NOT NULL, -- DEPOSIT ou BALANCE
    amount DECIMAL(10,2) NOT NULL,
    due_date DATE NOT NULL,
    paid_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(50),
    transaction_id VARCHAR(100),
    stripe_payment_intent_id VARCHAR(100),
    receipt_url VARCHAR(500),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Index pour optimiser les requêtes
CREATE INDEX IF NOT EXISTS idx_rental_contracts_owner ON rental_contracts(owner_id);
CREATE INDEX IF NOT EXISTS idx_rental_contracts_tenant ON rental_contracts(tenant_id);
CREATE INDEX IF NOT EXISTS idx_rental_contracts_property ON rental_contracts(property_id);
CREATE INDEX IF NOT EXISTS idx_rental_contracts_status ON rental_contracts(status);

CREATE INDEX IF NOT EXISTS idx_rent_payments_contract ON rent_payments(contract_id);
CREATE INDEX IF NOT EXISTS idx_rent_payments_status ON rent_payments(status);
CREATE INDEX IF NOT EXISTS idx_rent_payments_due_date ON rent_payments(due_date);
CREATE INDEX IF NOT EXISTS idx_rent_payments_overdue ON rent_payments(status, due_date) WHERE status = 'PENDING';

CREATE INDEX IF NOT EXISTS idx_payment_transactions_stripe ON payment_transactions(stripe_payment_intent_id);
CREATE INDEX IF NOT EXISTS idx_payment_transactions_rent_payment ON payment_transactions(rent_payment_id);
CREATE INDEX IF NOT EXISTS idx_payment_transactions_status ON payment_transactions(status);

CREATE INDEX IF NOT EXISTS idx_split_payments_contract ON split_payments(contract_id);
CREATE INDEX IF NOT EXISTS idx_split_payments_status ON split_payments(status);
CREATE INDEX IF NOT EXISTS idx_split_payment_items_split_payment ON split_payment_items(split_payment_id);
CREATE INDEX IF NOT EXISTS idx_split_payment_items_status ON split_payment_items(status);
CREATE INDEX IF NOT EXISTS idx_split_payment_items_type ON split_payment_items(payment_type);
CREATE INDEX IF NOT EXISTS idx_split_payment_items_stripe ON split_payment_items(stripe_payment_intent_id);

-- Triggers pour mettre à jour updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Créer les triggers seulement s'ils n'existent pas déjà
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'update_rental_contracts_updated_at') THEN
        CREATE TRIGGER update_rental_contracts_updated_at 
            BEFORE UPDATE ON rental_contracts 
            FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'update_rent_payments_updated_at') THEN
        CREATE TRIGGER update_rent_payments_updated_at 
            BEFORE UPDATE ON rent_payments 
            FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'update_payment_transactions_updated_at') THEN
        CREATE TRIGGER update_payment_transactions_updated_at 
            BEFORE UPDATE ON payment_transactions 
            FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'update_split_payments_updated_at') THEN
        CREATE TRIGGER update_split_payments_updated_at 
            BEFORE UPDATE ON split_payments 
            FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'update_split_payment_items_updated_at') THEN
        CREATE TRIGGER update_split_payment_items_updated_at 
            BEFORE UPDATE ON split_payment_items 
            FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
    END IF;
END $$;

-- Contraintes de validation (ajoutées seulement si elles n'existent pas)
DO $$
BEGIN
    -- Contraintes pour rental_contracts
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'check_payment_due_day') THEN
        ALTER TABLE rental_contracts 
        ADD CONSTRAINT check_payment_due_day 
        CHECK (payment_due_day >= 1 AND payment_due_day <= 31);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'check_contract_dates') THEN
        ALTER TABLE rental_contracts 
        ADD CONSTRAINT check_contract_dates 
        CHECK (end_date IS NULL OR end_date > start_date);
    END IF;
    
    -- Contraintes pour rent_payments
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'check_payment_amount') THEN
        ALTER TABLE rent_payments 
        ADD CONSTRAINT check_payment_amount 
        CHECK (amount > 0);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'check_payment_dates') THEN
        ALTER TABLE rent_payments 
        ADD CONSTRAINT check_payment_dates 
        CHECK (paid_date IS NULL OR paid_date >= due_date);
    END IF;
    
    -- Contraintes pour split_payments
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'check_split_payment_amount') THEN
        ALTER TABLE split_payments 
        ADD CONSTRAINT check_split_payment_amount 
        CHECK (total_amount > 0 AND deposit_amount > 0 AND balance_amount > 0);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'check_deposit_percentage') THEN
        ALTER TABLE split_payments 
        ADD CONSTRAINT check_deposit_percentage 
        CHECK (deposit_percentage > 0 AND deposit_percentage <= 100);
    END IF;
    
    -- Contraintes pour split_payment_items
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'check_split_item_amount') THEN
        ALTER TABLE split_payment_items 
        ADD CONSTRAINT check_split_item_amount 
        CHECK (amount > 0);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'check_split_item_dates') THEN
        ALTER TABLE split_payment_items 
        ADD CONSTRAINT check_split_item_dates 
        CHECK (paid_date IS NULL OR paid_date >= due_date);
    END IF;
END $$;

-- Commentaires pour la documentation
COMMENT ON TABLE rental_contracts IS 'Contrats de location entre propriétaires et locataires';
COMMENT ON TABLE rent_payments IS 'Paiements de loyer mensuels';
COMMENT ON TABLE payment_transactions IS 'Transactions de paiement via Stripe';
COMMENT ON TABLE split_payments IS 'Paiements fractionnés (acompte + solde)';
COMMENT ON TABLE split_payment_items IS 'Éléments individuels des paiements fractionnés';

COMMENT ON COLUMN rental_contracts.payment_due_day IS 'Jour du mois où le loyer est dû (1-31)';
COMMENT ON COLUMN rent_payments.late_fee IS 'Frais de retard calculés automatiquement';
COMMENT ON COLUMN payment_transactions.stripe_payment_intent_id IS 'ID unique de la transaction Stripe';
COMMENT ON COLUMN split_payments.deposit_percentage IS 'Pourcentage de l''acompte (1-100)';
COMMENT ON COLUMN split_payment_items.payment_type IS 'Type de paiement: DEPOSIT ou BALANCE';