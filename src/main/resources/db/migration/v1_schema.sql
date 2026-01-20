CREATE TABLE accounts (
                          id BIGSERIAL PRIMARY KEY,
                          merchant_id VARCHAR(100) NOT NULL,
                          balance NUMERIC(19,4) NOT NULL CHECK (balance >= 0),
                          currency VARCHAR(3) NOT NULL,
                          status VARCHAR(20) NOT NULL,
                          created_at TIMESTAMP NOT NULL
);

CREATE TABLE transactions (
                              id BIGSERIAL PRIMARY KEY,
                              idempotency_key VARCHAR(255) UNIQUE NOT NULL,
                              type VARCHAR(20) NOT NULL,
                              from_account_id BIGINT,
                              to_account_id BIGINT,
                              amount NUMERIC(19,4) NOT NULL,
                              currency VARCHAR(3) NOT NULL,
                              status VARCHAR(20) NOT NULL,
                              from_balance_before NUMERIC(19,4),
                              from_balance_after NUMERIC(19,4),
                              to_balance_before NUMERIC(19,4),
                              to_balance_after NUMERIC(19,4),
                              created_at TIMESTAMP NOT NULL
);
