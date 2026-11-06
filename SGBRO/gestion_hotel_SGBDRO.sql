

SET SERVEROUTPUT ON;

------------------------------------------------------------
-- 0) NETTOYAGE (ordre sécurisé)
------------------------------------------------------------
BEGIN EXECUTE IMMEDIATE 'DROP PACKAGE pkg_hotel_ro'; EXCEPTION WHEN OTHERS THEN NULL; END;
/

BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER trg_res_overlap'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER trg_res_capacite'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER trg_res_verif_personnes'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER trg_conso_montant'; EXCEPTION WHEN OTHERS THEN NULL; END;
/

BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER trg_client_id'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER trg_typech_id'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER trg_chambre_id'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER trg_res_id'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER trg_service_id'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER trg_conso_id'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER trg_paiement_id'; EXCEPTION WHEN OTHERS THEN NULL; END;
/

BEGIN EXECUTE IMMEDIATE 'DROP TABLE paiement_ot PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE conso_ot PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE service_ot PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE reservation_ot PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE chambre_ot PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE typechambre_ot PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE client_ot PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/

BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_client'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_typechambre'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_chambre'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_reservation'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_service'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_conso'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_paiement'; EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- TYPES (drop du plus dépendant au moins dépendant)
BEGIN EXECUTE IMMEDIATE 'DROP TYPE paiement_t FORCE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TYPE conso_t FORCE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TYPE service_t FORCE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TYPE reservation_t FORCE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TYPE personne_ntt FORCE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TYPE personne_res_t FORCE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TYPE chambre_t FORCE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TYPE typechambre_t FORCE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TYPE client_t FORCE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TYPE tel_varray_t FORCE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TYPE adresse_t FORCE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/

------------------------------------------------------------
-- 1) TYPES OBJETS + COLLECTIONS (SGBDRO)
------------------------------------------------------------

-- 1.1 Adresse (objet)
CREATE OR REPLACE TYPE adresse_t AS OBJECT (
  rue   VARCHAR2(100),
  ville VARCHAR2(50)
);
/

-- 1.2 VARRAY (max 3 téléphones)
CREATE OR REPLACE TYPE tel_varray_t AS VARRAY(3) OF VARCHAR2(20);
/

-- 1.3 Client (objet + méthode)
CREATE OR REPLACE TYPE client_t AS OBJECT (
  id_client NUMBER,
  nom       VARCHAR2(100),
  prenom    VARCHAR2(100),
  tel_list  tel_varray_t,
  email     VARCHAR2(200),
  cin       VARCHAR2(20),
  adr       adresse_t,
  MEMBER FUNCTION full_name RETURN VARCHAR2
);
/

CREATE OR REPLACE TYPE BODY client_t AS
  MEMBER FUNCTION full_name RETURN VARCHAR2 IS
  BEGIN
    RETURN prenom || ' ' || nom;
  END;
END;
/

-- 1.4 TypeChambre (objet)
CREATE OR REPLACE TYPE typechambre_t AS OBJECT (
  id_type  NUMBER,
  libelle  VARCHAR2(100),
  capacite NUMBER
);
/

-- 1.5 Chambre (objet) avec REF vers TypeChambre
CREATE OR REPLACE TYPE chambre_t AS OBJECT (
  id_chambre NUMBER,
  numero     VARCHAR2(10),
  etage      NUMBER,
  statut     VARCHAR2(20),
  type_ref   REF typechambre_t
);
/

-- 1.6 Personnes d'une réservation (objet) + NESTED TABLE
CREATE OR REPLACE TYPE personne_res_t AS OBJECT (
  type_personne VARCHAR2(20),  -- 'adulte' / 'enfant'
  quantite      NUMBER
);
/

CREATE OR REPLACE TYPE personne_ntt AS TABLE OF personne_res_t;
/

-- 1.7 Reservation (objet) avec REFs + nested table + méthode
CREATE OR REPLACE TYPE reservation_t AS OBJECT (
  id_res       NUMBER,
  date_deb     DATE,
  date_fin     DATE,
  nb_personnes NUMBER,
  statut       VARCHAR2(20),
  client_ref   REF client_t,
  chambre_ref  REF chambre_t,
  personnes_nt personne_ntt,
  MEMBER FUNCTION nb_nuits RETURN NUMBER
);
/

CREATE OR REPLACE TYPE BODY reservation_t AS
  MEMBER FUNCTION nb_nuits RETURN NUMBER IS
  BEGIN
    RETURN (date_fin - date_deb);
  END;
END;
/

-- 1.8 Service (objet)
CREATE OR REPLACE TYPE service_t AS OBJECT (
  id_service NUMBER,
  nom        VARCHAR2(100),
  prix       NUMBER(10,2)
);
/

-- 1.9 Consommation (objet) avec REFs
CREATE OR REPLACE TYPE conso_t AS OBJECT (
  id_conso        NUMBER,
  date_heure      TIMESTAMP,
  qte             NUMBER,
  montant         NUMBER(10,2),
  reservation_ref REF reservation_t,
  service_ref     REF service_t
);
/

-- 1.10 Paiement (objet) avec REF
CREATE OR REPLACE TYPE paiement_t AS OBJECT (
  id_paiement     NUMBER,
  montant         NUMBER(10,2),
  date_paiement   DATE,
  mode_paiement   VARCHAR2(20),
  reservation_ref REF reservation_t
);
/

------------------------------------------------------------
-- 2) TABLES OBJETS (TABLE OF OBJECT) + REF SCOPE + NESTED TABLE STORE
------------------------------------------------------------

CREATE TABLE client_ot OF client_t (
  CONSTRAINT pk_client_ot PRIMARY KEY (id_client),
  CONSTRAINT uq_client_email UNIQUE (email),
  CONSTRAINT uq_client_cin   UNIQUE (cin),
  CONSTRAINT chk_client_cin_len CHECK (LENGTH(cin) <= 20)
) OBJECT IDENTIFIER IS PRIMARY KEY;

CREATE TABLE typechambre_ot OF typechambre_t (
  CONSTRAINT pk_typechambre_ot PRIMARY KEY (id_type),
  CONSTRAINT chk_type_cap CHECK (capacite > 0)
) OBJECT IDENTIFIER IS PRIMARY KEY;

CREATE TABLE chambre_ot OF chambre_t (
  CONSTRAINT pk_chambre_ot PRIMARY KEY (id_chambre),
  CONSTRAINT uq_chambre_num UNIQUE (numero),
  CONSTRAINT chk_chambre_statut CHECK (statut IN ('libre','occupée','maintenance')),
  SCOPE FOR (type_ref) IS typechambre_ot
) OBJECT IDENTIFIER IS PRIMARY KEY;

CREATE TABLE reservation_ot OF reservation_t (
  CONSTRAINT pk_reservation_ot PRIMARY KEY (id_res),
  CONSTRAINT chk_res_dates CHECK (date_fin >= date_deb),
  CONSTRAINT chk_res_statut CHECK (statut IN ('en_attente','confirmée','annulée','en_cours','terminée')),
  SCOPE FOR (client_ref)  IS client_ot,
  SCOPE FOR (chambre_ref) IS chambre_ot
) OBJECT IDENTIFIER IS PRIMARY KEY
NESTED TABLE personnes_nt STORE AS res_personnes_nt;

CREATE TABLE service_ot OF service_t (
  CONSTRAINT pk_service_ot PRIMARY KEY (id_service),
  CONSTRAINT chk_service_prix CHECK (prix >= 0)
) OBJECT IDENTIFIER IS PRIMARY KEY;

CREATE TABLE conso_ot OF conso_t (
  CONSTRAINT pk_conso_ot PRIMARY KEY (id_conso),
  CONSTRAINT chk_conso_qte CHECK (qte >= 0),
  CONSTRAINT chk_conso_montant CHECK (montant IS NULL OR montant >= 0),
  SCOPE FOR (reservation_ref) IS reservation_ot,
  SCOPE FOR (service_ref)     IS service_ot
) OBJECT IDENTIFIER IS PRIMARY KEY;

CREATE TABLE paiement_ot OF paiement_t (
  CONSTRAINT pk_paiement_ot PRIMARY KEY (id_paiement),
  CONSTRAINT chk_paiement_montant CHECK (montant >= 0),
  CONSTRAINT chk_mode CHECK (mode_paiement IN ('cash','carte','virement','en_ligne')),
  SCOPE FOR (reservation_ref) IS reservation_ot
) OBJECT IDENTIFIER IS PRIMARY KEY;

------------------------------------------------------------
-- 3) SEQUENCES (IDs)
------------------------------------------------------------
CREATE SEQUENCE seq_client      START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_typechambre START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_chambre     START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_reservation START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_service     START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_conso       START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_paiement    START WITH 1 INCREMENT BY 1;

------------------------------------------------------------
-- 3bis) TRIGGERS ID (auto)
------------------------------------------------------------
CREATE OR REPLACE TRIGGER trg_client_id
BEFORE INSERT ON client_ot
FOR EACH ROW
BEGIN
  IF :NEW.id_client IS NULL THEN
    :NEW.id_client := seq_client.NEXTVAL;
  END IF;
END;
/
SHOW ERRORS;

CREATE OR REPLACE TRIGGER trg_typech_id
BEFORE INSERT ON typechambre_ot
FOR EACH ROW
BEGIN
  IF :NEW.id_type IS NULL THEN
    :NEW.id_type := seq_typechambre.NEXTVAL;
  END IF;
END;
/
SHOW ERRORS;

CREATE OR REPLACE TRIGGER trg_chambre_id
BEFORE INSERT ON chambre_ot
FOR EACH ROW
BEGIN
  IF :NEW.id_chambre IS NULL THEN
    :NEW.id_chambre := seq_chambre.NEXTVAL;
  END IF;
END;
/
SHOW ERRORS;

CREATE OR REPLACE TRIGGER trg_res_id
BEFORE INSERT ON reservation_ot
FOR EACH ROW
BEGIN
  IF :NEW.id_res IS NULL THEN
    :NEW.id_res := seq_reservation.NEXTVAL;
  END IF;
END;
/
SHOW ERRORS;

CREATE OR REPLACE TRIGGER trg_service_id
BEFORE INSERT ON service_ot
FOR EACH ROW
BEGIN
  IF :NEW.id_service IS NULL THEN
    :NEW.id_service := seq_service.NEXTVAL;
  END IF;
END;
/
SHOW ERRORS;

CREATE OR REPLACE TRIGGER trg_conso_id
BEFORE INSERT ON conso_ot
FOR EACH ROW
BEGIN
  IF :NEW.id_conso IS NULL THEN
    :NEW.id_conso := seq_conso.NEXTVAL;
  END IF;
END;
/
SHOW ERRORS;

CREATE OR REPLACE TRIGGER trg_paiement_id
BEFORE INSERT ON paiement_ot
FOR EACH ROW
BEGIN
  IF :NEW.id_paiement IS NULL THEN
    :NEW.id_paiement := seq_paiement.NEXTVAL;
  END IF;
END;
/
SHOW ERRORS;

------------------------------------------------------------
-- 4) TRIGGERS METIER (SGBDRO)
------------------------------------------------------------

-- 4.1 Montant conso = qte * prix(service)
CREATE OR REPLACE TRIGGER trg_conso_montant
BEFORE INSERT OR UPDATE ON conso_ot
FOR EACH ROW
DECLARE
  v_prix NUMBER(10,2);
BEGIN
  SELECT s.prix
    INTO v_prix
    FROM service_ot s
   WHERE REF(s) = :NEW.service_ref;

  :NEW.montant := :NEW.qte * v_prix;

  IF :NEW.date_heure IS NULL THEN
    :NEW.date_heure := SYSTIMESTAMP;
  END IF;
END;
/
SHOW ERRORS;

-- 4.2 Capacité chambre
CREATE OR REPLACE TRIGGER trg_res_capacite
BEFORE INSERT OR UPDATE ON reservation_ot
FOR EACH ROW
DECLARE
  v_cap NUMBER;
BEGIN
  SELECT t.capacite
    INTO v_cap
    FROM chambre_ot c, typechambre_ot t
   WHERE REF(c) = :NEW.chambre_ref
     AND REF(t) = c.type_ref;

  IF :NEW.nb_personnes > v_cap THEN
    RAISE_APPLICATION_ERROR(-20002, 'Le nombre de personnes dépasse la capacité de la chambre.');
  END IF;
END;
/
SHOW ERRORS;

-- 4.3 Vérif personnes (CORRIGÉE : plus de TABLE(:NEW...) => plus de ORA-22903)
CREATE OR REPLACE TRIGGER trg_res_verif_personnes
BEFORE UPDATE OF statut, nb_personnes ON reservation_ot
FOR EACH ROW
DECLARE
  v_total NUMBER := 0;
BEGIN
  IF :NEW.statut IN ('confirmée','en_cours','terminée') THEN

    IF :NEW.personnes_nt IS NOT NULL THEN
      FOR i IN 1 .. :NEW.personnes_nt.COUNT LOOP
        v_total := v_total + NVL(:NEW.personnes_nt(i).quantite, 0);
      END LOOP;
    END IF;

    IF v_total <> :NEW.nb_personnes THEN
      RAISE_APPLICATION_ERROR(-20004,
        'Avant de confirmer/en_cours/terminer : total adultes+enfants doit égaler nb_personnes.');
    END IF;

  END IF;
END;
/
SHOW ERRORS;

-- 4.4 Chevauchement réservation (compound trigger)
CREATE OR REPLACE TRIGGER trg_res_overlap
FOR INSERT OR UPDATE OF chambre_ref, date_deb, date_fin, statut ON reservation_ot
COMPOUND TRIGGER

  TYPE t_row IS RECORD (
    id_res      NUMBER,
    chambre_ref REF chambre_t,
    date_deb    DATE,
    date_fin    DATE,
    statut      VARCHAR2(20)
  );

  TYPE t_tab IS TABLE OF t_row INDEX BY PLS_INTEGER;
  g_rows t_tab;

  BEFORE EACH ROW IS
  BEGIN
    g_rows(g_rows.COUNT + 1).id_res      := :NEW.id_res;
    g_rows(g_rows.COUNT).chambre_ref    := :NEW.chambre_ref;
    g_rows(g_rows.COUNT).date_deb       := :NEW.date_deb;
    g_rows(g_rows.COUNT).date_fin       := :NEW.date_fin;
    g_rows(g_rows.COUNT).statut         := :NEW.statut;
  END BEFORE EACH ROW;

  AFTER STATEMENT IS
    v_count NUMBER;
  BEGIN
    FOR i IN 1 .. g_rows.COUNT LOOP
      IF g_rows(i).statut IN ('en_attente','confirmée','en_cours')
         AND g_rows(i).chambre_ref IS NOT NULL THEN

        SELECT COUNT(*)
          INTO v_count
          FROM reservation_ot r
         WHERE r.chambre_ref = g_rows(i).chambre_ref
           AND r.id_res <> g_rows(i).id_res
           AND g_rows(i).date_deb < r.date_fin
           AND g_rows(i).date_fin > r.date_deb
           AND r.statut IN ('en_attente','confirmée','en_cours');

        IF v_count > 0 THEN
          RAISE_APPLICATION_ERROR(-20001, 'Cette chambre est déjà réservée sur la période donnée.');
        END IF;

      END IF;
    END LOOP;
  END AFTER STATEMENT;

END;
/
SHOW ERRORS;

------------------------------------------------------------
-- 5) PACKAGE (PL/SQL) - REFs / objets / collections
------------------------------------------------------------
CREATE OR REPLACE PACKAGE pkg_hotel_ro AS
  PROCEDURE creer_client(
    p_nom VARCHAR2, p_prenom VARCHAR2, p_email VARCHAR2, p_cin VARCHAR2,
    p_tel1 VARCHAR2, p_tel2 VARCHAR2, p_rue VARCHAR2, p_ville VARCHAR2,
    p_id_out OUT NUMBER
  );

  PROCEDURE creer_reservation(
    p_date_deb DATE, p_date_fin DATE, p_nb_personnes NUMBER,
    p_id_client NUMBER, p_id_chambre NUMBER,
    p_id_res_out OUT NUMBER
  );

  PROCEDURE set_personnes(
    p_id_res NUMBER,
    p_nb_adultes NUMBER,
    p_nb_enfants NUMBER
  );

  PROCEDURE confirmer_reservation(p_id_res NUMBER);

  PROCEDURE ajouter_consommation(
    p_id_res NUMBER,
    p_id_service NUMBER,
    p_qte NUMBER
  );

  PROCEDURE enregistrer_paiement(
    p_id_res NUMBER,
    p_montant NUMBER,
    p_mode VARCHAR2
  );

  FUNCTION total_paiements(p_id_res NUMBER) RETURN NUMBER;
  FUNCTION total_services(p_id_res NUMBER) RETURN NUMBER;
  FUNCTION nb_nuits(p_id_res NUMBER) RETURN NUMBER;

  PROCEDURE afficher_reservations_client(p_id_client NUMBER);
END pkg_hotel_ro;
/
SHOW ERRORS;

CREATE OR REPLACE PACKAGE BODY pkg_hotel_ro AS

  PROCEDURE creer_client(
    p_nom VARCHAR2, p_prenom VARCHAR2, p_email VARCHAR2, p_cin VARCHAR2,
    p_tel1 VARCHAR2, p_tel2 VARCHAR2, p_rue VARCHAR2, p_ville VARCHAR2,
    p_id_out OUT NUMBER
  ) IS
  BEGIN
    INSERT INTO client_ot VALUES (
      client_t(
        NULL, -- id auto par trigger
        p_nom,
        p_prenom,
        tel_varray_t(p_tel1, p_tel2),
        p_email,
        p_cin,
        adresse_t(p_rue, p_ville)
      )
    )
    RETURNING id_client INTO p_id_out;

    COMMIT;
  END;

  PROCEDURE creer_reservation(
    p_date_deb DATE, p_date_fin DATE, p_nb_personnes NUMBER,
    p_id_client NUMBER, p_id_chambre NUMBER,
    p_id_res_out OUT NUMBER
  ) IS
    v_client_ref  REF client_t;
    v_chambre_ref REF chambre_t;
  BEGIN
    SELECT REF(c)  INTO v_client_ref  FROM client_ot c  WHERE c.id_client = p_id_client;
    SELECT REF(ch) INTO v_chambre_ref FROM chambre_ot ch WHERE ch.id_chambre = p_id_chambre;

    INSERT INTO reservation_ot VALUES (
      reservation_t(
        NULL, -- id auto par trigger
        p_date_deb,
        p_date_fin,
        p_nb_personnes,
        'en_attente',
        v_client_ref,
        v_chambre_ref,
        personne_ntt()  -- vide au début
      )
    )
    RETURNING id_res INTO p_id_res_out;

    COMMIT;
  EXCEPTION
    WHEN NO_DATA_FOUND THEN
      ROLLBACK;
      RAISE_APPLICATION_ERROR(-20110, 'Client ou chambre introuvable (ID incorrect).');
    WHEN OTHERS THEN
      ROLLBACK;
      RAISE;
  END;

  PROCEDURE set_personnes(
    p_id_res NUMBER,
    p_nb_adultes NUMBER,
    p_nb_enfants NUMBER
  ) IS
    v_nt personne_ntt := personne_ntt();
  BEGIN
    v_nt.EXTEND(2);
    v_nt(1) := personne_res_t('adulte', p_nb_adultes);
    v_nt(2) := personne_res_t('enfant', p_nb_enfants);

    UPDATE reservation_ot r
       SET r.personnes_nt = v_nt
     WHERE r.id_res = p_id_res;

    COMMIT;
  END;

  PROCEDURE confirmer_reservation(p_id_res NUMBER) IS
  BEGIN
    UPDATE reservation_ot r
       SET r.statut = 'confirmée'
     WHERE r.id_res = p_id_res;

    COMMIT;
  END;

  PROCEDURE ajouter_consommation(
    p_id_res NUMBER,
    p_id_service NUMBER,
    p_qte NUMBER
  ) IS
    v_res_ref  REF reservation_t;
    v_serv_ref REF service_t;
  BEGIN
    SELECT REF(r) INTO v_res_ref  FROM reservation_ot r WHERE r.id_res = p_id_res;
    SELECT REF(s) INTO v_serv_ref FROM service_ot s      WHERE s.id_service = p_id_service;

    INSERT INTO conso_ot VALUES (
      conso_t(NULL, SYSTIMESTAMP, p_qte, NULL, v_res_ref, v_serv_ref)
    );

    COMMIT;
  END;

  PROCEDURE enregistrer_paiement(
    p_id_res NUMBER,
    p_montant NUMBER,
    p_mode VARCHAR2
  ) IS
    v_res_ref REF reservation_t;
  BEGIN
    SELECT REF(r) INTO v_res_ref FROM reservation_ot r WHERE r.id_res = p_id_res;

    INSERT INTO paiement_ot VALUES (
      paiement_t(NULL, p_montant, SYSDATE, p_mode, v_res_ref)
    );

    COMMIT;
  EXCEPTION
    WHEN NO_DATA_FOUND THEN
      ROLLBACK;
      RAISE_APPLICATION_ERROR(-20100, 'Réservation inexistante : paiement impossible.');
  END;

  FUNCTION total_paiements(p_id_res NUMBER) RETURN NUMBER IS
    v_total NUMBER;
    v_res_ref REF reservation_t;
  BEGIN
    SELECT REF(r) INTO v_res_ref FROM reservation_ot r WHERE r.id_res = p_id_res;

    SELECT NVL(SUM(p.montant),0)
      INTO v_total
      FROM paiement_ot p
     WHERE p.reservation_ref = v_res_ref;

    RETURN v_total;
  EXCEPTION WHEN OTHERS THEN
    RETURN 0;
  END;

  FUNCTION total_services(p_id_res NUMBER) RETURN NUMBER IS
    v_total NUMBER;
    v_res_ref REF reservation_t;
  BEGIN
    SELECT REF(r) INTO v_res_ref FROM reservation_ot r WHERE r.id_res = p_id_res;

    SELECT NVL(SUM(c.montant),0)
      INTO v_total
      FROM conso_ot c
     WHERE c.reservation_ref = v_res_ref;

    RETURN v_total;
  EXCEPTION WHEN OTHERS THEN
    RETURN 0;
  END;

  FUNCTION nb_nuits(p_id_res NUMBER) RETURN NUMBER IS
    v_n NUMBER;
  BEGIN
    SELECT r.nb_nuits()
      INTO v_n
      FROM reservation_ot r
     WHERE r.id_res = p_id_res;

    RETURN v_n;
  EXCEPTION WHEN OTHERS THEN
    RETURN 0;
  END;

  PROCEDURE afficher_reservations_client(p_id_client NUMBER) IS
    v_client_ref REF client_t;
  BEGIN
    SELECT REF(c) INTO v_client_ref FROM client_ot c WHERE c.id_client = p_id_client;

    FOR x IN (
      SELECT r.id_res, r.date_deb, r.date_fin, r.statut,
             (SELECT ch.numero FROM chambre_ot ch WHERE REF(ch)=r.chambre_ref) AS numero
        FROM reservation_ot r
       WHERE r.client_ref = v_client_ref
       ORDER BY r.id_res
    ) LOOP
      DBMS_OUTPUT.PUT_LINE('Res '||x.id_res||' | '||
        TO_CHAR(x.date_deb,'YYYY-MM-DD')||' -> '||
        TO_CHAR(x.date_fin,'YYYY-MM-DD')||' | '||
        x.statut||' | Chambre '||x.numero);
    END LOOP;
  END;

END pkg_hotel_ro;
/
SHOW ERRORS;

------------------------------------------------------------
-- 6) DONNEES DE BASE
------------------------------------------------------------
-- Identités et coordonnées entièrement fictives, réservées aux démonstrations.

-- Types chambres (id auto par trigger)
INSERT INTO typechambre_ot VALUES (typechambre_t(NULL,'Simple',1));
INSERT INTO typechambre_ot VALUES (typechambre_t(NULL,'Double',2));
INSERT INTO typechambre_ot VALUES (typechambre_t(NULL,'Suite Familiale',4));

-- Chambres (REF vers typechambre_ot)
INSERT INTO chambre_ot VALUES (
  chambre_t(NULL,'101',1,'libre',(SELECT REF(t) FROM typechambre_ot t WHERE t.libelle='Simple'))
);
INSERT INTO chambre_ot VALUES (
  chambre_t(NULL,'102',1,'libre',(SELECT REF(t) FROM typechambre_ot t WHERE t.libelle='Double'))
);
INSERT INTO chambre_ot VALUES (
  chambre_t(NULL,'301',3,'libre',(SELECT REF(t) FROM typechambre_ot t WHERE t.libelle='Suite Familiale'))
);

-- Services (id auto par trigger)
INSERT INTO service_ot VALUES (service_t(NULL,'Petit-déjeuner',50));
INSERT INTO service_ot VALUES (service_t(NULL,'Spa',300));
INSERT INTO service_ot VALUES (service_t(NULL,'Blanchisserie',100));

COMMIT;

-- Clients via package
DECLARE
  v_id1 NUMBER; v_id2 NUMBER; v_id3 NUMBER;
BEGIN
  pkg_hotel_ro.creer_client('ClientDemoA','Alice','alice.client@example.test','DEMO0001','0000000001','0000000011','1 rue Exemple','Ville Exemple',v_id1);
  pkg_hotel_ro.creer_client('ClientDemoB','Benoit','benoit.client@example.test','DEMO0002','0000000002','0000000012','2 rue Exemple','Ville Exemple',v_id2);
  pkg_hotel_ro.creer_client('ClientDemoC','Chloe','chloe.client@example.test','DEMO0003','0000000003','0000000013','3 rue Exemple','Ville Exemple',v_id3);

  DBMS_OUTPUT.PUT_LINE('Clients créés : '||v_id1||', '||v_id2||', '||v_id3);
END;
/
------------------------------------------------------------
-- 7) TESTS COMPLETS
------------------------------------------------------------

-- 7.0 vider (ordre)
BEGIN
  DELETE FROM paiement_ot;
  DELETE FROM conso_ot;
  DELETE FROM reservation_ot;
  COMMIT;
END;
/

DECLARE
  v_cli1 NUMBER;
  v_cli2 NUMBER;
  v_ch102 NUMBER;
  v_ch301 NUMBER;
  v_serv_pd NUMBER;
  v_serv_spa NUMBER;

  v_res1 NUMBER;
  v_res2 NUMBER;
  v_res3 NUMBER;

  v_montant NUMBER;
BEGIN
  -- IDs stables
  SELECT MIN(id_client) INTO v_cli1 FROM client_ot;
  SELECT MAX(id_client) INTO v_cli2 FROM client_ot;
  SELECT id_chambre INTO v_ch102 FROM chambre_ot WHERE numero='102';
  SELECT id_chambre INTO v_ch301 FROM chambre_ot WHERE numero='301';
  SELECT id_service INTO v_serv_pd  FROM service_ot WHERE nom='Petit-déjeuner';
  SELECT id_service INTO v_serv_spa FROM service_ot WHERE nom='Spa';

  DBMS_OUTPUT.PUT_LINE('IDs: cli1='||v_cli1||', cli2='||v_cli2||', ch102='||v_ch102||', ch301='||v_ch301);

  -- TEST1 : capacité (doit échouer ORA-20002)
  BEGIN
    pkg_hotel_ro.creer_reservation(DATE '2026-11-01', DATE '2026-11-03', 3, v_cli1, v_ch102, v_res1);
    DBMS_OUTPUT.PUT_LINE('TEST1 ERREUR : capacité non bloquée !');
  EXCEPTION WHEN OTHERS THEN
    DBMS_OUTPUT.PUT_LINE('TEST1 OK (capacité bloquée) : '||SQLERRM);
  END;

  -- TEST2 : chevauchement (doit échouer ORA-20001)
  pkg_hotel_ro.creer_reservation(DATE '2026-12-10', DATE '2026-12-15', 2, v_cli1, v_ch301, v_res2);
  DBMS_OUTPUT.PUT_LINE('TEST2 étape 1 OK : Res1='||v_res2);

  BEGIN
    pkg_hotel_ro.creer_reservation(DATE '2026-12-12', DATE '2026-12-14', 2, v_cli2, v_ch301, v_res3);
    DBMS_OUTPUT.PUT_LINE('TEST2 ERREUR : chevauchement non détecté !');
  EXCEPTION WHEN OTHERS THEN
    DBMS_OUTPUT.PUT_LINE('TEST2 OK (chevauchement bloqué) : '||SQLERRM);
  END;

  -- TEST3 : confirmation sans personnes (doit échouer ORA-20004)
  BEGIN
    pkg_hotel_ro.confirmer_reservation(v_res2);
    DBMS_OUTPUT.PUT_LINE('TEST3 ERREUR : confirmation acceptée sans personnes !');
  EXCEPTION WHEN OTHERS THEN
    DBMS_OUTPUT.PUT_LINE('TEST3 OK (bloqué) : '||SQLERRM);
  END;

  -- TEST4 : flow complet OK
  pkg_hotel_ro.set_personnes(v_res2, 2, 0); -- total=2
  pkg_hotel_ro.confirmer_reservation(v_res2);
  DBMS_OUTPUT.PUT_LINE('TEST4 OK : réservation confirmée après personnes');

  -- TEST5 : consommations (montant auto)
  pkg_hotel_ro.ajouter_consommation(v_res2, v_serv_pd, 2);  -- 2*50=100
  pkg_hotel_ro.ajouter_consommation(v_res2, v_serv_spa, 1); -- 1*300=300

  SELECT c.montant INTO v_montant
    FROM conso_ot c
   ORDER BY c.id_conso DESC
   FETCH FIRST 1 ROW ONLY;

  DBMS_OUTPUT.PUT_LINE('TEST5 OK : montant dernière conso='||v_montant||' (attendu 300)');

  -- TEST6 : paiements + totaux
  pkg_hotel_ro.enregistrer_paiement(v_res2, 500, 'cash');
  pkg_hotel_ro.enregistrer_paiement(v_res2, 2000, 'carte');

  DBMS_OUTPUT.PUT_LINE('TEST6 OK : total paiements='||pkg_hotel_ro.total_paiements(v_res2));
  DBMS_OUTPUT.PUT_LINE('TEST6 OK : total services='||pkg_hotel_ro.total_services(v_res2));
  DBMS_OUTPUT.PUT_LINE('TEST6 OK : nb nuits='||pkg_hotel_ro.nb_nuits(v_res2));

  -- TEST7 : afficher réservations client
  DBMS_OUTPUT.PUT_LINE('TEST7 : réservations du client '||v_cli1);
  pkg_hotel_ro.afficher_reservations_client(v_cli1);

END;
/
