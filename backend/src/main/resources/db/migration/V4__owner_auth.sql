-- Owner auth: account status + MFA columns on users, and a business_profiles table.
-- All statements are idempotent so the migration is safe to re-apply.

-- Add status and MFA columns to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'APPROVED';
ALTER TABLE users ADD COLUMN IF NOT EXISTS mfa_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS mfa_secret VARCHAR(100) DEFAULT NULL;

-- Set existing customer/admin accounts to APPROVED (keeps current logins working)
UPDATE users SET status = 'APPROVED' WHERE status IS NULL;

-- Create business_profiles table
CREATE TABLE IF NOT EXISTS business_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL,
    company_name VARCHAR(150) NOT NULL,
    tax_id VARCHAR(50) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    bank_account_number VARCHAR(50) NOT NULL,
    bank_ifsc_code VARCHAR(30) NOT NULL,
    registration_doc_path VARCHAR(255),
    electricity_doc_path VARCHAR(255),
    bank_doc_path VARCHAR(255),
    CONSTRAINT fk_business_profile_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
