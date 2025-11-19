INSERT INTO app_users (id, email, password_hash, name, created_at)
VALUES ('11111111-1111-1111-1111-111111111111',
        'test@example.com',
        'hash',
        'Test User',
        NOW());

INSERT INTO app_profiles (user_id, target_role, experience_years, updated_at)
VALUES ('11111111-1111-1111-1111-111111111111',
        'Java Developer',
        1,
        NOW());

INSERT INTO app_skills (user_id, skill_name, level)
VALUES ('11111111-1111-1111-1111-111111111111', 'java',   1),
       ('11111111-1111-1111-1111-111111111111', 'spring', 1),
       ('11111111-1111-1111-1111-111111111111', 'sql',    1);
