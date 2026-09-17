create table outbox_messages (
    id uuid primary key,
    aggregate_id uuid not null references generation_requests(id) on delete cascade,
    payload jsonb not null,
    created_at timestamp with time zone not null default current_timestamp,
    published_at timestamp with time zone,
    attempts int not null default 0 check (attempts >= 0),
    next_attempt_at timestamp with time zone not null default current_timestamp,
    last_error text
);

create index idx_outbox_messages_pending
    on outbox_messages (next_attempt_at, created_at)
    where published_at is null;
