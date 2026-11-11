create table accreditation_document
(
    id         uuid primary key,
    name       varchar(255) not null,
    mime_type  varchar(100) not null,
    size_bytes bigint       not null,
    sha256     char(64)     not null,
    content    bytea        not null,
    created_at timestamptz  not null
);

create table accreditation
(
    id                 uuid primary key,
    user_id            varchar(32) not null,
    accreditation_type varchar(16) not null,
    status             varchar(16) not null,
    document_id        uuid        not null,
    created_at         timestamptz not null,
    updated_at         timestamptz not null,
    version            bigint      not null
);

create index ix_accreditation_user_id on accreditation (user_id);
