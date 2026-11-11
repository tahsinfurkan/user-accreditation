create table accreditation_status_history
(
    event_id         uuid primary key,
    accreditation_id uuid        not null references accreditation (id),
    previous_status  varchar(16),
    new_status       varchar(16) not null,
    origin           varchar(16) not null,
    occurred_at      timestamptz not null,
    recorded_at      timestamptz not null
);

create index ix_accreditation_status_history_accreditation
    on accreditation_status_history (accreditation_id, occurred_at);
