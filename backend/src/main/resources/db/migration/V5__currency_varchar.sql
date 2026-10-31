-- char(3) vira bpchar no Postgres, e o Hibernate com ddl-auto=validate recusa
-- subir porque a entidade mapeia String (varchar). O validate esta certo: se o
-- banco e a entidade discordam, melhor nao subir do que gravar torto.
alter table account alter column currency type varchar(3);
