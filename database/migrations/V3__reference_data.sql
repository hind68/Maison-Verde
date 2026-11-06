-- Stable reference values required by the reservation flow.
-- Transaction ownership belongs to the migration runner.

INSERT INTO RoleEmploye(id_role, libelle) VALUES (1, 'reception');
INSERT INTO RoleEmploye(id_role, libelle) VALUES (2, 'direction');
INSERT INTO RoleEmploye(id_role, libelle) VALUES (3, 'housekeeping');
INSERT INTO RoleEmploye(id_role, libelle) VALUES (4, 'compta');

INSERT INTO TypePersonne(id_type_personne, libelle) VALUES (1, 'adulte');
INSERT INTO TypePersonne(id_type_personne, libelle) VALUES (2, 'enfant');

