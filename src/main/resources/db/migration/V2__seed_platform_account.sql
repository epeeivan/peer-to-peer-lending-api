INSERT INTO users (name, email, is_system) VALUES
    ('Platform Account', 'platform@system.local', TRUE),
    ('Escrow Account',   'escrow@system.local',   TRUE);

INSERT INTO wallets (user_id)
SELECT id FROM users WHERE email IN ('platform@system.local', 'escrow@system.local');
