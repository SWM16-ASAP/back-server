# MySQL schema migrations

Add versioned SQL migrations here when introducing each domain table.
No domain tables are created by the infrastructure-only change.
Flyway owns DDL; Hibernate validates the schema and must not update it.
Existing production data conversion is outside this redesign's scope.
