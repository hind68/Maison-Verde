-- Business rules and application-facing PL/SQL.
-- No procedure controls the transaction: the Java caller must commit or roll back.

CREATE INDEX idx_chambre_type ON Chambre(id_type);
CREATE INDEX idx_employe_role ON Employe(id_role);
CREATE INDEX idx_reservation_client ON Reservation(id_client);
CREATE INDEX idx_reservation_period ON Reservation(id_chambre, statut, date_deb, date_fin);
CREATE INDEX idx_paiement_res ON Paiement(id_res);
CREATE INDEX idx_paiement_employe ON Paiement(id_employe);
CREATE INDEX idx_conso_res ON ConsommationService(id_res);
CREATE INDEX idx_conso_service ON ConsommationService(id_service);
CREATE INDEX idx_personne_res_type ON PersonneReservation(id_type_personne);

CREATE OR REPLACE TRIGGER trg_client_bi
BEFORE INSERT ON Client FOR EACH ROW
BEGIN
  IF :NEW.id_client IS NULL THEN :NEW.id_client := seq_client.NEXTVAL; END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_typechambre_bi
BEFORE INSERT ON TypeChambre FOR EACH ROW
BEGIN
  IF :NEW.id_type IS NULL THEN :NEW.id_type := seq_typechambre.NEXTVAL; END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_chambre_bi
BEFORE INSERT ON Chambre FOR EACH ROW
BEGIN
  IF :NEW.id_chambre IS NULL THEN :NEW.id_chambre := seq_chambre.NEXTVAL; END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_employe_bi
BEFORE INSERT ON Employe FOR EACH ROW
BEGIN
  IF :NEW.id_employe IS NULL THEN :NEW.id_employe := seq_employe.NEXTVAL; END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_service_bi
BEFORE INSERT ON Service FOR EACH ROW
BEGIN
  IF :NEW.id_service IS NULL THEN :NEW.id_service := seq_service.NEXTVAL; END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_reservation_bi
BEFORE INSERT ON Reservation FOR EACH ROW
BEGIN
  IF :NEW.id_res IS NULL THEN :NEW.id_res := seq_reservation.NEXTVAL; END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_paiement_bi
BEFORE INSERT ON Paiement FOR EACH ROW
BEGIN
  IF :NEW.id_paiement IS NULL THEN :NEW.id_paiement := seq_paiement.NEXTVAL; END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_conso_bi
BEFORE INSERT ON ConsommationService FOR EACH ROW
BEGIN
  IF :NEW.id_conso IS NULL THEN :NEW.id_conso := seq_conso.NEXTVAL; END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_personne_res_bi
BEFORE INSERT ON PersonneReservation FOR EACH ROW
BEGIN
  IF :NEW.id_pr IS NULL THEN :NEW.id_pr := seq_pr.NEXTVAL; END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_reservation_capacite
BEFORE INSERT OR UPDATE OF nb_personnes, id_chambre ON Reservation
FOR EACH ROW
DECLARE
  v_capacite TypeChambre.capacite%TYPE;
BEGIN
  SELECT tc.capacite
    INTO v_capacite
    FROM Chambre c
    JOIN TypeChambre tc ON tc.id_type = c.id_type
   WHERE c.id_chambre = :NEW.id_chambre;

  IF :NEW.nb_personnes > v_capacite THEN
    RAISE_APPLICATION_ERROR(-20002, 'Le nombre de personnes dépasse la capacité de la chambre.');
  END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_reservation_overlap
FOR INSERT OR UPDATE OF id_chambre, date_deb, date_fin, statut ON Reservation
COMPOUND TRIGGER
  TYPE t_reservation IS RECORD (
    id_res Reservation.id_res%TYPE,
    id_chambre Reservation.id_chambre%TYPE,
    date_deb Reservation.date_deb%TYPE,
    date_fin Reservation.date_fin%TYPE,
    statut Reservation.statut%TYPE
  );
  TYPE t_reservations IS TABLE OF t_reservation INDEX BY PLS_INTEGER;
  g_rows t_reservations;

  BEFORE EACH ROW IS
    v_index PLS_INTEGER;
  BEGIN
    v_index := g_rows.COUNT + 1;
    g_rows(v_index).id_res := :NEW.id_res;
    g_rows(v_index).id_chambre := :NEW.id_chambre;
    g_rows(v_index).date_deb := :NEW.date_deb;
    g_rows(v_index).date_fin := :NEW.date_fin;
    g_rows(v_index).statut := :NEW.statut;
  END BEFORE EACH ROW;

  AFTER STATEMENT IS
    v_count NUMBER;
  BEGIN
    IF g_rows.COUNT > 0 THEN
      FOR i IN 1 .. g_rows.COUNT LOOP
        IF g_rows(i).statut IN ('en_attente', 'confirmée', 'en_cours') THEN
          SELECT COUNT(*)
            INTO v_count
            FROM Reservation r
           WHERE r.id_chambre = g_rows(i).id_chambre
             AND r.id_res <> g_rows(i).id_res
             AND g_rows(i).date_deb < r.date_fin
             AND g_rows(i).date_fin > r.date_deb
             AND r.statut IN ('en_attente', 'confirmée', 'en_cours');

          IF v_count > 0 THEN
            RAISE_APPLICATION_ERROR(-20001, 'Cette chambre est déjà réservée sur la période donnée.');
          END IF;
        END IF;
      END LOOP;
    END IF;
  END AFTER STATEMENT;
END;
/

CREATE OR REPLACE TRIGGER trg_reservation_occupants
BEFORE UPDATE OF statut, nb_personnes ON Reservation
FOR EACH ROW
DECLARE
  v_total NUMBER;
BEGIN
  IF :NEW.statut IN ('confirmée', 'en_cours', 'terminée') THEN
    SELECT NVL(SUM(quantite), 0)
      INTO v_total
      FROM PersonneReservation
     WHERE id_res = :OLD.id_res;

    IF v_total <> :NEW.nb_personnes THEN
      RAISE_APPLICATION_ERROR(-20004, 'Le total des occupants doit correspondre à la réservation.');
    END IF;
  END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_personne_res_coherence
FOR INSERT OR UPDATE OR DELETE ON PersonneReservation
COMPOUND TRIGGER
  TYPE t_ids IS TABLE OF BOOLEAN INDEX BY VARCHAR2(40);
  g_ids t_ids;

  PROCEDURE memoriser(p_id_res NUMBER) IS
  BEGIN
    IF p_id_res IS NOT NULL THEN
      g_ids(TO_CHAR(p_id_res)) := TRUE;
    END IF;
  END;

  AFTER EACH ROW IS
  BEGIN
    IF INSERTING OR UPDATING THEN memoriser(:NEW.id_res); END IF;
    IF DELETING OR UPDATING THEN memoriser(:OLD.id_res); END IF;
  END AFTER EACH ROW;

  AFTER STATEMENT IS
    v_key VARCHAR2(40);
    v_statut Reservation.statut%TYPE;
    v_attendu Reservation.nb_personnes%TYPE;
    v_total NUMBER;
  BEGIN
    v_key := g_ids.FIRST;
    WHILE v_key IS NOT NULL LOOP
      BEGIN
        SELECT statut, nb_personnes
          INTO v_statut, v_attendu
          FROM Reservation
         WHERE id_res = TO_NUMBER(v_key);

        IF v_statut IN ('confirmée', 'en_cours', 'terminée') THEN
          SELECT NVL(SUM(quantite), 0)
            INTO v_total
            FROM PersonneReservation
           WHERE id_res = TO_NUMBER(v_key);

          IF v_total <> v_attendu THEN
            RAISE_APPLICATION_ERROR(-20004, 'Le total des occupants doit correspondre à la réservation.');
          END IF;
        END IF;
      EXCEPTION
        WHEN NO_DATA_FOUND THEN NULL;
      END;
      v_key := g_ids.NEXT(v_key);
    END LOOP;
  END AFTER STATEMENT;
END;
/

CREATE OR REPLACE TRIGGER trg_conso_calcul_montant
BEFORE INSERT OR UPDATE OF qte, id_service ON ConsommationService
FOR EACH ROW
DECLARE
  v_prix Service.prix%TYPE;
BEGIN
  SELECT prix INTO v_prix FROM Service WHERE id_service = :NEW.id_service;
  :NEW.montant := :NEW.qte * v_prix;
END;
/

CREATE OR REPLACE PACKAGE pkg_reservation_app AUTHID DEFINER AS
  PROCEDURE creer_reservation_confirmee(
    p_date_deb DATE,
    p_date_fin DATE,
    p_nb_adultes NUMBER,
    p_nb_enfants NUMBER,
    p_id_client NUMBER,
    p_id_chambre NUMBER,
    p_id_res_out OUT NUMBER
  );
END pkg_reservation_app;
/

CREATE OR REPLACE PACKAGE BODY pkg_reservation_app AS
  PROCEDURE creer_reservation_confirmee(
    p_date_deb DATE,
    p_date_fin DATE,
    p_nb_adultes NUMBER,
    p_nb_enfants NUMBER,
    p_id_client NUMBER,
    p_id_chambre NUMBER,
    p_id_res_out OUT NUMBER
  ) IS
    v_capacite TypeChambre.capacite%TYPE;
    v_statut_chambre Chambre.statut%TYPE;
    v_nb_personnes NUMBER;
    v_count NUMBER;
    v_dummy NUMBER;
    v_type_adulte TypePersonne.id_type_personne%TYPE;
    v_type_enfant TypePersonne.id_type_personne%TYPE;
  BEGIN
    IF p_date_deb IS NULL OR p_date_fin IS NULL OR p_date_fin <= p_date_deb THEN
      RAISE_APPLICATION_ERROR(-20003, 'La date de départ doit être postérieure à la date d''arrivée.');
    END IF;

    IF p_nb_adultes IS NULL OR p_nb_adultes < 1 OR NVL(p_nb_enfants, 0) < 0 THEN
      RAISE_APPLICATION_ERROR(-20004, 'Une réservation exige au moins un adulte et des quantités valides.');
    END IF;

    v_nb_personnes := p_nb_adultes + NVL(p_nb_enfants, 0);

    SELECT 1 INTO v_dummy FROM Client WHERE id_client = p_id_client;

    SELECT c.statut, tc.capacite
      INTO v_statut_chambre, v_capacite
      FROM Chambre c
      JOIN TypeChambre tc ON tc.id_type = c.id_type
     WHERE c.id_chambre = p_id_chambre
       FOR UPDATE OF c.statut WAIT 10;

    IF v_statut_chambre = 'maintenance' THEN
      RAISE_APPLICATION_ERROR(-20005, 'La chambre est indisponible pour maintenance.');
    END IF;

    IF v_nb_personnes > v_capacite THEN
      RAISE_APPLICATION_ERROR(-20002, 'Le nombre de personnes dépasse la capacité de la chambre.');
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM Reservation r
     WHERE r.id_chambre = p_id_chambre
       AND p_date_deb < r.date_fin
       AND p_date_fin > r.date_deb
       AND r.statut IN ('en_attente', 'confirmée', 'en_cours');

    IF v_count > 0 THEN
      RAISE_APPLICATION_ERROR(-20001, 'Cette chambre est déjà réservée sur la période donnée.');
    END IF;

    SELECT id_type_personne INTO v_type_adulte FROM TypePersonne WHERE libelle = 'adulte';
    SELECT id_type_personne INTO v_type_enfant FROM TypePersonne WHERE libelle = 'enfant';

    p_id_res_out := seq_reservation.NEXTVAL;
    INSERT INTO Reservation(id_res, date_deb, date_fin, nb_personnes, statut, id_client, id_chambre)
    VALUES (p_id_res_out, p_date_deb, p_date_fin, v_nb_personnes, 'en_attente', p_id_client, p_id_chambre);

    INSERT INTO PersonneReservation(id_res, id_type_personne, quantite)
    VALUES (p_id_res_out, v_type_adulte, p_nb_adultes);

    IF NVL(p_nb_enfants, 0) > 0 THEN
      INSERT INTO PersonneReservation(id_res, id_type_personne, quantite)
      VALUES (p_id_res_out, v_type_enfant, p_nb_enfants);
    END IF;

    UPDATE Reservation SET statut = 'confirmée' WHERE id_res = p_id_res_out;
  END creer_reservation_confirmee;
END pkg_reservation_app;
/
