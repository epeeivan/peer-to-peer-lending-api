CREATE TABLE users (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(150) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    is_system  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE wallets (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT        NOT NULL UNIQUE REFERENCES users(id),
    balance    NUMERIC(19,2) NOT NULL DEFAULT 0,
    version    BIGINT        NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT chk_wallet_balance_non_negative CHECK (balance >= 0)
);

CREATE TABLE loans (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    borrower_id          BIGINT        NOT NULL REFERENCES users(id),
    principal            NUMERIC(19,2) NOT NULL CHECK (principal > 0),
    annual_interest_rate NUMERIC(9,6)  NOT NULL CHECK (annual_interest_rate >= 0),
    term_months          INT           NOT NULL CHECK (term_months > 0),
    status               VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    funded_amount        NUMERIC(19,2) NOT NULL DEFAULT 0,
    funding_deadline     TIMESTAMPTZ   NOT NULL,
    monthly_payment      NUMERIC(19,2),
    remaining_principal  NUMERIC(19,2),
    installments_paid    INT           NOT NULL DEFAULT 0,
    version              BIGINT        NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    funded_at            TIMESTAMPTZ,
    CONSTRAINT chk_loan_funded_le_principal CHECK (funded_amount <= principal),
    CONSTRAINT chk_loan_status CHECK (status IN ('PENDING', 'FUNDED', 'REPAID', 'CANCELLED'))
);

CREATE INDEX idx_loans_borrower ON loans(borrower_id);
CREATE INDEX idx_loans_pending_deadline ON loans(status, funding_deadline);

CREATE TABLE loan_investments (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    loan_id     BIGINT        NOT NULL REFERENCES loans(id),
    investor_id BIGINT        NOT NULL REFERENCES users(id),
    amount      NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_loan_investments_loan ON loan_investments(loan_id);
CREATE INDEX idx_loan_investments_investor ON loan_investments(investor_id);

CREATE TABLE ledger_entries (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    wallet_id      BIGINT        NOT NULL REFERENCES wallets(id),
    loan_id        BIGINT        REFERENCES loans(id),
    type           VARCHAR(30)   NOT NULL,
    amount         NUMERIC(19,2) NOT NULL,
    balance_after  NUMERIC(19,2) NOT NULL,
    correlation_id UUID          NOT NULL,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT chk_ledger_amount_non_zero CHECK (amount <> 0),
    CONSTRAINT chk_ledger_type CHECK (type IN
        ('DEPOSIT', 'WITHDRAWAL', 'LOAN_FUNDING', 'LOAN_DISBURSEMENT',
         'REPAYMENT_DEBIT', 'REPAYMENT_CREDIT', 'PLATFORM_FEE', 'FUNDING_REFUND'))
);

CREATE INDEX idx_ledger_wallet ON ledger_entries(wallet_id);
CREATE INDEX idx_ledger_loan ON ledger_entries(loan_id);
CREATE INDEX idx_ledger_correlation ON ledger_entries(correlation_id);
