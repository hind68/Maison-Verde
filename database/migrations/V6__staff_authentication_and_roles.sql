-- Security attributes extend the academic employee model without creating credentials.

MERGE INTO RoleEmploye target
USING (
  SELECT 101 id_role, 'ADMIN' libelle FROM dual UNION ALL
  SELECT 102, 'MANAGER' FROM dual UNION ALL
  SELECT 103, 'RECEPTION' FROM dual UNION ALL
  SELECT 104, 'HOUSEKEEPING' FROM dual
) source
ON (target.libelle = source.libelle)
WHEN NOT MATCHED THEN
  INSERT (id_role, libelle) VALUES (source.id_role, source.libelle);

ALTER TABLE Employe ADD (
  email VARCHAR2(200 CHAR),
  password_hash VARCHAR2(100 CHAR),
  actif NUMBER(1) DEFAULT 0 NOT NULL,
  changement_mdp_requis NUMBER(1) DEFAULT 1 NOT NULL,
  tentatives_connexion NUMBER DEFAULT 0 NOT NULL,
  verrouille_jusqua TIMESTAMP,
  cree_le TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  modifie_le TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
);

UPDATE Employe
   SET login = LOWER(TRIM(login)),
       email = LOWER(TRIM(email));

ALTER TABLE Employe ADD CONSTRAINT chk_employe_actif
  CHECK (actif IN (0, 1));

ALTER TABLE Employe ADD CONSTRAINT chk_employe_changement_mdp
  CHECK (changement_mdp_requis IN (0, 1));

ALTER TABLE Employe ADD CONSTRAINT chk_employe_tentatives
  CHECK (tentatives_connexion >= 0);

CREATE UNIQUE INDEX uq_employe_login_normalise
  ON Employe(LOWER(TRIM(login)));

CREATE UNIQUE INDEX uq_employe_email_normalise
  ON Employe(LOWER(TRIM(email)));

CREATE INDEX idx_employe_securite
  ON Employe(actif, id_role, verrouille_jusqua);

COMMIT;
