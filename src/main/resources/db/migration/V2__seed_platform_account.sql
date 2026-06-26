INSERT INTO users (name, email, system_role) VALUES
    ('Platform Account', 'platform@system.local', 'PLATFORM'),
    ('Escrow Account',   'escrow@system.local',   'ESCROW');

INSERT INTO wallets (user_id)
SELECT id FROM users WHERE system_role IS NOT NULL;
