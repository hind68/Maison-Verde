-- Optional fictitious data for a new development schema only.
-- No COMMIT is issued; the caller owns the transaction.

INSERT INTO TypeChambre(libelle, capacite) VALUES ('Simple', 1);
INSERT INTO TypeChambre(libelle, capacite) VALUES ('Double', 2);
INSERT INTO TypeChambre(libelle, capacite) VALUES ('Suite Familiale', 4);

INSERT INTO Chambre(numero, etage, statut, id_type)
SELECT '101', 1, 'libre', id_type FROM TypeChambre WHERE libelle = 'Simple';
INSERT INTO Chambre(numero, etage, statut, id_type)
SELECT '102', 1, 'libre', id_type FROM TypeChambre WHERE libelle = 'Double';
INSERT INTO Chambre(numero, etage, statut, id_type)
SELECT '301', 3, 'libre', id_type FROM TypeChambre WHERE libelle = 'Suite Familiale';

INSERT INTO Client(nom, prenom, tel, email, cin, date_naissance)
VALUES ('Demo', 'Amine', '0600000001', 'amine.demo@example.test', 'DEMO-CIN-001', DATE '1990-01-01');
INSERT INTO Client(nom, prenom, tel, email, cin, date_naissance)
VALUES ('Demo', 'Sara', '0600000002', 'sara.demo@example.test', 'DEMO-CIN-002', DATE '1992-02-02');

