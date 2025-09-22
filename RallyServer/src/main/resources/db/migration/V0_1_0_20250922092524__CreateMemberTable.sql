CREATE SEQUENCE member_id_seq;

CREATE TABLE IF NOT EXISTS member (
    id INTEGER PRIMARY KEY DEFAULT nextval('member_id_seq'),
    email text,
    password text
 );
