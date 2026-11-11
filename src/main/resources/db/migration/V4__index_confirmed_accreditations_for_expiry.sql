create index ix_accreditation_confirmed_updated_at on accreditation (updated_at, id) where status = 'CONFIRMED';
