create unique index ux_accreditation_pending_user on accreditation (user_id) where status = 'PENDING';
