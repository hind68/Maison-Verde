-- Housekeeping and operational state are deliberately independent from occupancy.
-- Existing rooms start ready; this initializes current state without inventing history.

ALTER TABLE Chambre ADD (
  etat_operationnel VARCHAR2(20 CHAR) DEFAULT 'en_service' NOT NULL,
  etat_proprete VARCHAR2(20 CHAR) DEFAULT 'prete' NOT NULL
);

UPDATE Chambre
   SET etat_operationnel = CASE
     WHEN statut = 'maintenance' THEN 'hors_service'
     ELSE 'en_service'
   END,
       etat_proprete = 'prete';

ALTER TABLE Chambre ADD CONSTRAINT chk_chambre_operationnel
  CHECK (etat_operationnel IN ('en_service', 'hors_service'));

ALTER TABLE Chambre ADD CONSTRAINT chk_chambre_proprete
  CHECK (etat_proprete IN ('a_nettoyer', 'nettoyee', 'prete'));

CREATE INDEX idx_chambre_etats
  ON Chambre(etat_operationnel, etat_proprete);

COMMIT;
