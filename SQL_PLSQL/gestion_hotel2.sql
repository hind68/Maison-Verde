SET SERVEROUTPUT ON;

------------------------------------------------------------
-- 1) TABLES DE RÉFÉRENCE
------------------------------------------------------------
CREATE TABLE RoleEmploye (
  id_role NUMBER PRIMARY KEY,
  libelle VARCHAR2(50) UNIQUE NOT NULL
);

CREATE TABLE TypePersonne (
  id_type_personne NUMBER PRIMARY KEY,
  libelle          VARCHAR2(20) UNIQUE NOT NULL
);

------------------------------------------------------------
-- 2) TABLES PRINCIPALES
------------------------------------------------------------
CREATE TABLE Client (
  id_client      NUMBER PRIMARY KEY,
  nom            VARCHAR2(100) NOT NULL,
  prenom         VARCHAR2(100) NOT NULL,
  tel            VARCHAR2(20),
  email          VARCHAR2(200) UNIQUE,
  cin            VARCHAR2(20) UNIQUE,
  date_naissance DATE
);

CREATE TABLE TypeChambre (
  id_type   NUMBER PRIMARY KEY,
  libelle   VARCHAR2(100) NOT NULL,
  capacite  NUMBER NOT NULL CHECK (capacite > 0)
);

CREATE TABLE Chambre (
  id_chambre NUMBER PRIMARY KEY,
  numero     VARCHAR2(10) UNIQUE NOT NULL,
  etage      NUMBER NOT NULL,
  statut     VARCHAR2(20) DEFAULT 'libre'
             CHECK (statut IN ('libre','occupée','maintenance')),
  id_type    NUMBER NOT NULL,
  CONSTRAINT fk_chambre_type
    FOREIGN KEY (id_type) REFERENCES TypeChambre(id_type)
);

CREATE TABLE Employe (
  id_employe NUMBER PRIMARY KEY,
  nom        VARCHAR2(100) NOT NULL,
  prenom     VARCHAR2(100) NOT NULL,
  login      VARCHAR2(100) UNIQUE,
  id_role    NUMBER NOT NULL,
  CONSTRAINT fk_emp_role
    FOREIGN KEY (id_role) REFERENCES RoleEmploye(id_role)
);

CREATE TABLE Service (
  id_service NUMBER PRIMARY KEY,
  nom        VARCHAR2(100) NOT NULL,
  prix       NUMBER(10,2) NOT NULL CHECK (prix >= 0)
);

CREATE TABLE Reservation (
  id_res       NUMBER PRIMARY KEY,
  date_deb     DATE NOT NULL,
  date_fin     DATE NOT NULL,
  nb_personnes NUMBER NOT NULL CHECK (nb_personnes > 0),
  statut       VARCHAR2(20) DEFAULT 'en_attente'
               CHECK (statut IN ('en_attente','confirmée','annulée','en_cours','terminée')),
  id_client    NUMBER NOT NULL,
  id_chambre   NUMBER NOT NULL,
  CONSTRAINT fk_res_client
    FOREIGN KEY (id_client) REFERENCES Client(id_client),
  CONSTRAINT fk_res_chambre
    FOREIGN KEY (id_chambre) REFERENCES Chambre(id_chambre),
  CONSTRAINT chk_reservation_dates
    CHECK (date_fin >= date_deb)
);

CREATE TABLE Paiement (
  id_paiement   NUMBER PRIMARY KEY,
  montant       NUMBER(10,2) NOT NULL CHECK (montant >= 0),
  date_paiement DATE NOT NULL,
  mode_paiement VARCHAR2(20)
               CHECK (mode_paiement IN ('cash','carte','virement','en_ligne')),
  id_res        NUMBER NOT NULL,
  id_employe    NUMBER,
  CONSTRAINT fk_paiement_res
    FOREIGN KEY (id_res) REFERENCES Reservation(id_res),
  CONSTRAINT fk_paiement_employe
    FOREIGN KEY (id_employe) REFERENCES Employe(id_employe)
);

CREATE TABLE ConsommationService (
  id_conso   NUMBER PRIMARY KEY,
  date_heure TIMESTAMP DEFAULT SYSTIMESTAMP,
  qte        NUMBER NOT NULL CHECK (qte >= 0),
  montant    NUMBER(10,2),
  id_res     NUMBER NOT NULL,
  id_service NUMBER NOT NULL,
  CONSTRAINT fk_conso_res
    FOREIGN KEY (id_res) REFERENCES Reservation(id_res),
  CONSTRAINT fk_conso_service
    FOREIGN KEY (id_service) REFERENCES Service(id_service),
  CONSTRAINT chk_conso_montant_pos CHECK (montant IS NULL OR montant >= 0)
);

CREATE TABLE PersonneReservation (
  id_pr            NUMBER PRIMARY KEY,
  id_res           NUMBER NOT NULL,
  id_type_personne NUMBER NOT NULL,
  quantite         NUMBER NOT NULL CHECK (quantite >= 0),
  CONSTRAINT fk_pr_res
    FOREIGN KEY (id_res) REFERENCES Reservation(id_res),
  CONSTRAINT fk_pr_type
    FOREIGN KEY (id_type_personne) REFERENCES TypePersonne(id_type_personne)
);

------------------------------------------------------------
-- 3) SÉQUENCES + TRIGGERS AUTO-INCREMENT
------------------------------------------------------------
CREATE SEQUENCE seq_client      START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_typechambre START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_chambre     START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_employe     START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_service     START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_reservation START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_paiement    START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_conso       START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_pr          START WITH 1 INCREMENT BY 1;

CREATE OR REPLACE TRIGGER trg_client_bi
BEFORE INSERT ON Client FOR EACH ROW
BEGIN
  IF :NEW.id_client IS NULL THEN
    SELECT seq_client.NEXTVAL INTO :NEW.id_client FROM dual;
  END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_typechambre_bi
BEFORE INSERT ON TypeChambre FOR EACH ROW
BEGIN
  IF :NEW.id_type IS NULL THEN
    SELECT seq_typechambre.NEXTVAL INTO :NEW.id_type FROM dual;
  END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_chambre_bi
BEFORE INSERT ON Chambre FOR EACH ROW
BEGIN
  IF :NEW.id_chambre IS NULL THEN
    SELECT seq_chambre.NEXTVAL INTO :NEW.id_chambre FROM dual;
  END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_employe_bi
BEFORE INSERT ON Employe FOR EACH ROW
BEGIN
  IF :NEW.id_employe IS NULL THEN
    SELECT seq_employe.NEXTVAL INTO :NEW.id_employe FROM dual;
  END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_service_bi
BEFORE INSERT ON Service FOR EACH ROW
BEGIN
  IF :NEW.id_service IS NULL THEN
    SELECT seq_service.NEXTVAL INTO :NEW.id_service FROM dual;
  END IF;
END;
/


CREATE OR REPLACE TRIGGER trg_paiement_bi
BEFORE INSERT ON Paiement FOR EACH ROW
BEGIN
  IF :NEW.id_paiement IS NULL THEN
    SELECT seq_paiement.NEXTVAL INTO :NEW.id_paiement FROM dual;
  END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_conso_bi
BEFORE INSERT ON ConsommationService FOR EACH ROW
BEGIN
  IF :NEW.id_conso IS NULL THEN
    SELECT seq_conso.NEXTVAL INTO :NEW.id_conso FROM dual;
  END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_pr_bi
BEFORE INSERT ON PersonneReservation FOR EACH ROW
BEGIN
  IF :NEW.id_pr IS NULL THEN
    SELECT seq_pr.NEXTVAL INTO :NEW.id_pr FROM dual;
  END IF;
END;
/

------------------------------------------------------------
-- 4) TRIGGERS MÉTIER
------------------------------------------------------------

-- 4.1) Chevauchement
CREATE OR REPLACE TRIGGER trg_reservation_pas_chevauchement
FOR INSERT OR UPDATE OF id_chambre, date_deb, date_fin, statut ON Reservation
COMPOUND TRIGGER

  TYPE t_res IS RECORD (
    id_res     Reservation.id_res%TYPE,
    id_chambre Reservation.id_chambre%TYPE,
    date_deb   Reservation.date_deb%TYPE,
    date_fin   Reservation.date_fin%TYPE,
    statut     Reservation.statut%TYPE
  );
  TYPE t_tab IS TABLE OF t_res INDEX BY PLS_INTEGER;
  g_rows t_tab;

  BEFORE EACH ROW IS
  BEGIN
    g_rows(g_rows.COUNT + 1).id_res     := :NEW.id_res;
    g_rows(g_rows.COUNT).id_chambre     := :NEW.id_chambre;
    g_rows(g_rows.COUNT).date_deb       := :NEW.date_deb;
    g_rows(g_rows.COUNT).date_fin       := :NEW.date_fin;
    g_rows(g_rows.COUNT).statut         := :NEW.statut;
  END BEFORE EACH ROW;

  AFTER STATEMENT IS
    v_count NUMBER;
  BEGIN
    FOR i IN 1 .. g_rows.COUNT LOOP

      IF g_rows(i).statut IN ('en_attente','confirmée','en_cours')
         AND g_rows(i).id_chambre IS NOT NULL
         AND g_rows(i).date_deb IS NOT NULL
         AND g_rows(i).date_fin IS NOT NULL THEN

        SELECT COUNT(*)
          INTO v_count
          FROM Reservation r
         WHERE r.id_chambre = g_rows(i).id_chambre
           AND r.id_res <> g_rows(i).id_res
           AND g_rows(i).date_deb < r.date_fin
           AND g_rows(i).date_fin > r.date_deb
           AND r.statut IN ('en_attente','confirmée','en_cours');

        IF v_count > 0 THEN
          RAISE_APPLICATION_ERROR(-20001,
            'Cette chambre est déjà réservée sur la période donnée.');
        END IF;

      END IF;
    END LOOP;
  END AFTER STATEMENT;

END trg_reservation_pas_chevauchement;
/
SHOW ERRORS;

-- 4.2) Capacité chambre
CREATE OR REPLACE TRIGGER trg_reservation_capacite
BEFORE INSERT OR UPDATE ON Reservation
FOR EACH ROW
DECLARE
  v_cap TypeChambre.capacite%TYPE;
BEGIN
  SELECT tc.capacite
    INTO v_cap
    FROM Chambre c JOIN TypeChambre tc ON tc.id_type = c.id_type
   WHERE c.id_chambre = :NEW.id_chambre;

  IF :NEW.nb_personnes > v_cap THEN
    RAISE_APPLICATION_ERROR(-20002,
      'Le nombre de personnes dépasse la capacité de la chambre.');
  END IF;
END;
/

-- 4.3) Calcul montant consommation = qte * Service.prix
CREATE OR REPLACE TRIGGER trg_conso_calcul_montant
BEFORE INSERT OR UPDATE ON ConsommationService
FOR EACH ROW
DECLARE
  v_prix Service.prix%TYPE;
BEGIN
  SELECT prix INTO v_prix FROM Service WHERE id_service = :NEW.id_service;
  :NEW.montant := :NEW.qte * v_prix;
END;
/

-- 4.4) Vérif personnes uniquement au moment d'activation (confirmée/en_cours/terminée)
CREATE OR REPLACE TRIGGER trg_reservation_verif_personnes
BEFORE UPDATE OF statut, nb_personnes ON Reservation
FOR EACH ROW
DECLARE
  v_total NUMBER;
BEGIN
  IF :NEW.statut IN ('confirmée','en_cours','terminée') THEN
    SELECT NVL(SUM(quantite), 0)
      INTO v_total
      FROM PersonneReservation
     WHERE id_res = :OLD.id_res;

    IF v_total <> :NEW.nb_personnes THEN
      RAISE_APPLICATION_ERROR(-20004,
        'Avant de confirmer/en_cours/terminer : total adultes+enfants doit égaler nb_personnes.');
    END IF;
  END IF;
END;
/

------------------------------------------------------------
-- 5) PACKAGE PKG_HOTEL (fonctions, procédures, exceptions, curseur)
------------------------------------------------------------
CREATE OR REPLACE PACKAGE pkg_hotel AS
  FUNCTION total_paiement_reservation(p_id_res NUMBER) RETURN NUMBER;
  FUNCTION total_services_reservation(p_id_res NUMBER) RETURN NUMBER;
  FUNCTION nombre_nuits(p_id_res NUMBER) RETURN NUMBER;

  PROCEDURE creer_reservation(
    p_date_deb     DATE,
    p_date_fin     DATE,
    p_nb_personnes NUMBER,
    p_id_client    NUMBER,
    p_id_chambre   NUMBER,
    p_id_res_out   OUT NUMBER
  );

  PROCEDURE ajouter_personnes(
    p_id_res NUMBER,
    p_id_type_personne NUMBER,
    p_quantite NUMBER
  );

  PROCEDURE confirmer_reservation(p_id_res NUMBER);

  PROCEDURE enregistrer_paiement(
    p_id_res NUMBER,
    p_montant NUMBER,
    p_mode VARCHAR2,
    p_id_employe NUMBER DEFAULT NULL
  );

  PROCEDURE ajouter_consommation(
    p_id_res NUMBER,
    p_id_service NUMBER,
    p_qte NUMBER,
    p_date_heure TIMESTAMP DEFAULT SYSTIMESTAMP
  );

  PROCEDURE afficher_reservations_client(p_id_client NUMBER);
END pkg_hotel;
/
SHOW ERRORS;

CREATE OR REPLACE PACKAGE BODY pkg_hotel AS

  FUNCTION total_paiement_reservation(p_id_res NUMBER) RETURN NUMBER IS
    v_total NUMBER;
  BEGIN
    SELECT NVL(SUM(montant), 0) INTO v_total FROM Paiement WHERE id_res = p_id_res;
    RETURN v_total;
  END;

  FUNCTION total_services_reservation(p_id_res NUMBER) RETURN NUMBER IS
    v_total NUMBER;
  BEGIN
    SELECT NVL(SUM(montant), 0) INTO v_total FROM ConsommationService WHERE id_res = p_id_res;
    RETURN v_total;
  END;

  FUNCTION nombre_nuits(p_id_res NUMBER) RETURN NUMBER IS
    v_n NUMBER;
  BEGIN
    SELECT (date_fin - date_deb) INTO v_n FROM Reservation WHERE id_res = p_id_res;
    RETURN v_n;
  EXCEPTION WHEN NO_DATA_FOUND THEN
    RETURN 0;
  END;

  PROCEDURE creer_reservation(
    p_date_deb     DATE,
    p_date_fin     DATE,
    p_nb_personnes NUMBER,
    p_id_client    NUMBER,
    p_id_chambre   NUMBER,
    p_id_res_out   OUT NUMBER
  ) AS
  BEGIN
    -- Génération sûre de l’ID (pour que le trigger chevauchement fonctionne)
    p_id_res_out := seq_reservation.NEXTVAL;

    INSERT INTO Reservation(id_res, date_deb, date_fin, nb_personnes, statut, id_client, id_chambre)
    VALUES (p_id_res_out, p_date_deb, p_date_fin, p_nb_personnes, 'en_attente', p_id_client, p_id_chambre);

    COMMIT;
  EXCEPTION WHEN OTHERS THEN
    ROLLBACK;
    RAISE;
  END;

  PROCEDURE ajouter_personnes(
    p_id_res NUMBER,
    p_id_type_personne NUMBER,
    p_quantite NUMBER
  ) AS
  BEGIN
    INSERT INTO PersonneReservation(id_res, id_type_personne, quantite)
    VALUES (p_id_res, p_id_type_personne, p_quantite);
    COMMIT;
  EXCEPTION WHEN OTHERS THEN
    ROLLBACK;
    RAISE;
  END;

  PROCEDURE confirmer_reservation(p_id_res NUMBER) AS
  BEGIN
    UPDATE Reservation SET statut = 'confirmée' WHERE id_res = p_id_res;
    COMMIT;
  EXCEPTION WHEN OTHERS THEN
    ROLLBACK;
    RAISE;
  END;

  PROCEDURE enregistrer_paiement(
    p_id_res NUMBER,
    p_montant NUMBER,
    p_mode VARCHAR2,
    p_id_employe NUMBER DEFAULT NULL
  ) AS
    v_dummy NUMBER;
  BEGIN
    SELECT 1 INTO v_dummy FROM Reservation WHERE id_res = p_id_res;

    INSERT INTO Paiement(montant, date_paiement, mode_paiement, id_res, id_employe)
    VALUES (p_montant, SYSDATE, p_mode, p_id_res, p_id_employe);

    COMMIT;
  EXCEPTION
    WHEN NO_DATA_FOUND THEN
      ROLLBACK;
      RAISE_APPLICATION_ERROR(-20100, 'Réservation inexistante : paiement impossible.');
    WHEN OTHERS THEN
      ROLLBACK;
      RAISE;
  END;

  PROCEDURE ajouter_consommation(
    p_id_res NUMBER,
    p_id_service NUMBER,
    p_qte NUMBER,
    p_date_heure TIMESTAMP DEFAULT SYSTIMESTAMP
  ) AS
  BEGIN
    INSERT INTO ConsommationService(date_heure, qte, id_res, id_service)
    VALUES (p_date_heure, p_qte, p_id_res, p_id_service);
    COMMIT;
  EXCEPTION WHEN OTHERS THEN
    ROLLBACK;
    RAISE;
  END;

  PROCEDURE afficher_reservations_client(p_id_client NUMBER) AS
    CURSOR c IS
      SELECT r.id_res, r.date_deb, r.date_fin, r.statut, ch.numero
        FROM Reservation r JOIN Chambre ch ON ch.id_chambre = r.id_chambre
       WHERE r.id_client = p_id_client
       ORDER BY r.id_res;
    v c%ROWTYPE;
  BEGIN
    OPEN c;
    LOOP
      FETCH c INTO v;
      EXIT WHEN c%NOTFOUND;
      DBMS_OUTPUT.PUT_LINE('Res '||v.id_res||' | '||
        TO_CHAR(v.date_deb,'YYYY-MM-DD')||' -> '||
        TO_CHAR(v.date_fin,'YYYY-MM-DD')||' | '||
        v.statut||' | Chambre '||v.numero);
    END LOOP;
    CLOSE c;
  END;

END pkg_hotel;
/
SHOW ERRORS;

------------------------------------------------------------
-- 6) DONNÉES DE BASE
------------------------------------------------------------
-- Identités et coordonnées entièrement fictives, réservées aux démonstrations.
INSERT INTO RoleEmploye(id_role, libelle) VALUES (1, 'reception');
INSERT INTO RoleEmploye(id_role, libelle) VALUES (2, 'direction');
INSERT INTO RoleEmploye(id_role, libelle) VALUES (3, 'housekeeping');
INSERT INTO RoleEmploye(id_role, libelle) VALUES (4, 'compta');

INSERT INTO TypePersonne(id_type_personne, libelle) VALUES (1, 'adulte');
INSERT INTO TypePersonne(id_type_personne, libelle) VALUES (2, 'enfant');

INSERT INTO Client(nom, prenom, tel, email, cin, date_naissance)
VALUES ('ClientDemoA', 'Alice', '0000000001', 'alice.client@example.test', 'DEMO0001', DATE '1995-04-12');
INSERT INTO Client(nom, prenom, tel, email, cin, date_naissance)
VALUES ('ClientDemoB', 'Benoit', '0000000002', 'benoit.client@example.test', 'DEMO0002', DATE '1998-09-25');
INSERT INTO Client(nom, prenom, tel, email, cin, date_naissance)
VALUES ('ClientDemoC', 'Chloe', '0000000003', 'chloe.client@example.test', 'DEMO0003', DATE '1990-01-14');


INSERT INTO TypeChambre(libelle, capacite) VALUES ('Simple', 1);
INSERT INTO TypeChambre(libelle, capacite) VALUES ('Double', 2);
INSERT INTO TypeChambre(libelle, capacite) VALUES ('Suite Familiale', 4);

-- ID chambre : 101=1, 102=2, 301=3
INSERT INTO Chambre(numero, etage, statut, id_type) VALUES ('101', 1, 'libre', 1);
INSERT INTO Chambre(numero, etage, statut, id_type) VALUES ('102', 1, 'libre', 2);
INSERT INTO Chambre(numero, etage, statut, id_type) VALUES ('301', 3, 'libre', 3);

INSERT INTO Employe(nom, prenom, login, id_role) VALUES ('EmployeDemoA', 'Alex', 'demo_reception', 1);
INSERT INTO Employe(nom, prenom, login, id_role) VALUES ('EmployeDemoB', 'Lou', 'demo_direction', 2);

INSERT INTO Service(nom, prix) VALUES ('Petit-déjeuner', 50);
INSERT INTO Service(nom, prix) VALUES ('Spa', 300);
INSERT INTO Service(nom, prix) VALUES ('Blanchisserie', 100);

COMMIT;

------------------------------------------------------------
-- 7) TESTS COMPLETS (TOUTES LES FONCTIONNALITÉS)
------------------------------------------------------------

-- 7.0 Vider les tables métier (tests reproductibles)
BEGIN
  DELETE FROM Paiement;
  DELETE FROM ConsommationService;
  DELETE FROM PersonneReservation;
  DELETE FROM Reservation;
  COMMIT;
END;
/

-- 7.1 Auto-increment Client
DECLARE
  v_id NUMBER;
BEGIN
  INSERT INTO Client(nom, prenom, tel, email, cin, date_naissance)
  VALUES ('ClientTest', 'Test', '0000000000', 'client.test@example.test', 'DEMO9999', DATE '2001-01-01')
  RETURNING id_client INTO v_id;

  COMMIT;
  DBMS_OUTPUT.PUT_LINE('TEST1 OK : client inséré, id_client='||v_id);
END;
/

-- 7.2 Tests triggers + package
DECLARE
  v_ch_102 NUMBER;
  v_ch_301 NUMBER;

  v_pd  NUMBER;
  v_spa NUMBER;

  v_cli1 NUMBER;
  v_cli2 NUMBER;
  v_emp1 NUMBER;

  v_res1 NUMBER;
  v_res2 NUMBER;
  v_res3 NUMBER;
  v_res_ok NUMBER;

  v_last_montant NUMBER;
BEGIN
  SELECT id_chambre INTO v_ch_102 FROM Chambre WHERE numero='102';
  SELECT id_chambre INTO v_ch_301 FROM Chambre WHERE numero='301';

  SELECT id_service INTO v_pd  FROM Service WHERE nom='Petit-déjeuner';
  SELECT id_service INTO v_spa FROM Service WHERE nom='Spa';

  SELECT id_client INTO v_cli1 FROM Client WHERE id_client = 1;
  SELECT id_client INTO v_cli2 FROM Client WHERE id_client = 2;
  SELECT id_employe INTO v_emp1 FROM Employe WHERE id_employe = 1;

  DBMS_OUTPUT.PUT_LINE('IDs chargés : ch102='||v_ch_102||', ch301='||v_ch_301||', cli1='||v_cli1);

  ------------------------------------------------------------
  -- TEST2 : capacité (doit échouer)
  ------------------------------------------------------------
  BEGIN
    pkg_hotel.creer_reservation(
      p_date_deb => DATE '2026-11-01',
      p_date_fin => DATE '2026-11-03',
      p_nb_personnes => 3,              -- chambre Double cap=2
      p_id_client => v_cli1,
      p_id_chambre => v_ch_102,
      p_id_res_out => v_res1
    );
    DBMS_OUTPUT.PUT_LINE('TEST2 ERREUR : la capacité n’a pas été bloquée !');
  EXCEPTION
    WHEN OTHERS THEN
      DBMS_OUTPUT.PUT_LINE('TEST2 OK (capacité bloquée) : '||SQLERRM);
  END;

  ------------------------------------------------------------
  -- TEST3 : chevauchement (doit échouer)
  ------------------------------------------------------------
  pkg_hotel.creer_reservation(
    p_date_deb => DATE '2026-12-10',
    p_date_fin => DATE '2026-12-15',
    p_nb_personnes => 2,
    p_id_client => v_cli1,
    p_id_chambre => v_ch_301,
    p_id_res_out => v_res2
  );
  DBMS_OUTPUT.PUT_LINE('TEST3 étape 1 OK : Res1 id='||v_res2);

  BEGIN
    pkg_hotel.creer_reservation(
      p_date_deb => DATE '2026-12-12',
      p_date_fin => DATE '2026-12-14',
      p_nb_personnes => 2,
      p_id_client => v_cli2,
      p_id_chambre => v_ch_301,
      p_id_res_out => v_res3
    );
    DBMS_OUTPUT.PUT_LINE('TEST3 ERREUR : chevauchement non détecté !');
  EXCEPTION
    WHEN OTHERS THEN
      DBMS_OUTPUT.PUT_LINE('TEST3 OK (chevauchement bloqué) : '||SQLERRM);
  END;

  ------------------------------------------------------------
  -- TEST4 : confirmation sans personnes (doit échouer)
  ------------------------------------------------------------
  pkg_hotel.creer_reservation(
    p_date_deb => DATE '2027-01-05',
    p_date_fin => DATE '2027-01-07',
    p_nb_personnes => 3,
    p_id_client => v_cli1,
    p_id_chambre => v_ch_301,
    p_id_res_out => v_res_ok
  );
  DBMS_OUTPUT.PUT_LINE('TEST4 étape 1 OK : Res créée id='||v_res_ok);

  BEGIN
    pkg_hotel.confirmer_reservation(v_res_ok);
    DBMS_OUTPUT.PUT_LINE('TEST4 ERREUR : confirmation acceptée sans personnes !');
  EXCEPTION
    WHEN OTHERS THEN
      DBMS_OUTPUT.PUT_LINE('TEST4 OK (confirmation bloquée) : '||SQLERRM);
  END;

  ------------------------------------------------------------
  -- TEST5 : flow complet OK (personnes -> confirmation)
  ------------------------------------------------------------
  pkg_hotel.ajouter_personnes(v_res_ok, 1, 2);
  pkg_hotel.ajouter_personnes(v_res_ok, 2, 1);
  DBMS_OUTPUT.PUT_LINE('TEST5 étape 1 OK : personnes ajoutées');

  pkg_hotel.confirmer_reservation(v_res_ok);
  DBMS_OUTPUT.PUT_LINE('TEST5 étape 2 OK : réservation confirmée');

  ------------------------------------------------------------
  -- TEST6 : consommation + calcul montant
  ------------------------------------------------------------
  pkg_hotel.ajouter_consommation(v_res_ok, v_pd, 2);
  pkg_hotel.ajouter_consommation(v_res_ok, v_spa, 1);

  SELECT montant
    INTO v_last_montant
    FROM ConsommationService
   WHERE id_res = v_res_ok
   ORDER BY id_conso DESC
   FETCH FIRST 1 ROW ONLY;

  DBMS_OUTPUT.PUT_LINE('TEST6 OK : montant dernière conso='||v_last_montant||' (Spa=300 attendu)');

  ------------------------------------------------------------
  -- TEST7 : paiements + fonctions
  ------------------------------------------------------------
  pkg_hotel.enregistrer_paiement(v_res_ok, 500,  'cash',  v_emp1);
  pkg_hotel.enregistrer_paiement(v_res_ok, 2000, 'carte', v_emp1);

  DBMS_OUTPUT.PUT_LINE('TEST7 OK : Total paiements='||pkg_hotel.total_paiement_reservation(v_res_ok));
  DBMS_OUTPUT.PUT_LINE('TEST7 OK : Total services='||pkg_hotel.total_services_reservation(v_res_ok));
  DBMS_OUTPUT.PUT_LINE('TEST7 OK : Nuits='||pkg_hotel.nombre_nuits(v_res_ok));

  ------------------------------------------------------------
  -- TEST8 : exception paiement sur réservation inexistante
  ------------------------------------------------------------
  BEGIN
    pkg_hotel.enregistrer_paiement(999999, 100, 'cash', v_emp1);
    DBMS_OUTPUT.PUT_LINE('TEST8 ERREUR : paiement accepté sur réservation inexistante !');
  EXCEPTION
    WHEN OTHERS THEN
      DBMS_OUTPUT.PUT_LINE('TEST8 OK (exception) : '||SQLERRM);
  END;

  ------------------------------------------------------------
  -- TEST9 : curseur affichage réservations client
  ------------------------------------------------------------
  DBMS_OUTPUT.PUT_LINE('TEST9 : Affichage réservations client 1');
  pkg_hotel.afficher_reservations_client(v_cli1);

END;
/
