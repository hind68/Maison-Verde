-- Execute only in a new, empty, dedicated Oracle schema.
-- This migration is intentionally non-destructive.

CREATE TABLE RoleEmploye (
  id_role NUMBER CONSTRAINT pk_role_employe PRIMARY KEY,
  libelle VARCHAR2(50 CHAR) CONSTRAINT nn_role_libelle NOT NULL,
  CONSTRAINT uq_role_libelle UNIQUE (libelle)
);

CREATE TABLE TypePersonne (
  id_type_personne NUMBER CONSTRAINT pk_type_personne PRIMARY KEY,
  libelle VARCHAR2(20 CHAR) CONSTRAINT nn_type_personne_lib NOT NULL,
  CONSTRAINT uq_type_personne_lib UNIQUE (libelle)
);

CREATE TABLE Client (
  id_client NUMBER CONSTRAINT pk_client PRIMARY KEY,
  nom VARCHAR2(100 CHAR) CONSTRAINT nn_client_nom NOT NULL,
  prenom VARCHAR2(100 CHAR) CONSTRAINT nn_client_prenom NOT NULL,
  tel VARCHAR2(20 CHAR),
  email VARCHAR2(200 CHAR),
  cin VARCHAR2(20 CHAR),
  date_naissance DATE,
  CONSTRAINT uq_client_email UNIQUE (email),
  CONSTRAINT uq_client_cin UNIQUE (cin)
);

CREATE TABLE TypeChambre (
  id_type NUMBER CONSTRAINT pk_type_chambre PRIMARY KEY,
  libelle VARCHAR2(100 CHAR) CONSTRAINT nn_type_chambre_lib NOT NULL,
  capacite NUMBER CONSTRAINT nn_type_chambre_cap NOT NULL,
  CONSTRAINT uq_type_chambre_lib UNIQUE (libelle),
  CONSTRAINT chk_type_chambre_cap CHECK (capacite > 0)
);

CREATE TABLE Chambre (
  id_chambre NUMBER CONSTRAINT pk_chambre PRIMARY KEY,
  numero VARCHAR2(10 CHAR) CONSTRAINT nn_chambre_numero NOT NULL,
  etage NUMBER CONSTRAINT nn_chambre_etage NOT NULL,
  statut VARCHAR2(20 CHAR) DEFAULT 'libre' CONSTRAINT nn_chambre_statut NOT NULL,
  id_type NUMBER CONSTRAINT nn_chambre_type NOT NULL,
  CONSTRAINT uq_chambre_numero UNIQUE (numero),
  CONSTRAINT chk_chambre_statut CHECK (statut IN ('libre', 'occupée', 'maintenance')),
  CONSTRAINT fk_chambre_type FOREIGN KEY (id_type) REFERENCES TypeChambre(id_type)
);

CREATE TABLE Employe (
  id_employe NUMBER CONSTRAINT pk_employe PRIMARY KEY,
  nom VARCHAR2(100 CHAR) CONSTRAINT nn_employe_nom NOT NULL,
  prenom VARCHAR2(100 CHAR) CONSTRAINT nn_employe_prenom NOT NULL,
  login VARCHAR2(100 CHAR),
  id_role NUMBER CONSTRAINT nn_employe_role NOT NULL,
  CONSTRAINT uq_employe_login UNIQUE (login),
  CONSTRAINT fk_employe_role FOREIGN KEY (id_role) REFERENCES RoleEmploye(id_role)
);

CREATE TABLE Service (
  id_service NUMBER CONSTRAINT pk_service PRIMARY KEY,
  nom VARCHAR2(100 CHAR) CONSTRAINT nn_service_nom NOT NULL,
  prix NUMBER(10,2) CONSTRAINT nn_service_prix NOT NULL,
  CONSTRAINT uq_service_nom UNIQUE (nom),
  CONSTRAINT chk_service_prix CHECK (prix >= 0)
);

CREATE TABLE Reservation (
  id_res NUMBER CONSTRAINT pk_reservation PRIMARY KEY,
  date_deb DATE CONSTRAINT nn_reservation_deb NOT NULL,
  date_fin DATE CONSTRAINT nn_reservation_fin NOT NULL,
  nb_personnes NUMBER CONSTRAINT nn_reservation_nb NOT NULL,
  statut VARCHAR2(20 CHAR) DEFAULT 'en_attente' CONSTRAINT nn_reservation_statut NOT NULL,
  id_client NUMBER CONSTRAINT nn_reservation_client NOT NULL,
  id_chambre NUMBER CONSTRAINT nn_reservation_chambre NOT NULL,
  CONSTRAINT chk_reservation_dates CHECK (date_fin > date_deb),
  CONSTRAINT chk_reservation_nb CHECK (nb_personnes > 0),
  CONSTRAINT chk_reservation_statut CHECK (
    statut IN ('en_attente', 'confirmée', 'annulée', 'en_cours', 'terminée')
  ),
  CONSTRAINT fk_reservation_client FOREIGN KEY (id_client) REFERENCES Client(id_client),
  CONSTRAINT fk_reservation_chambre FOREIGN KEY (id_chambre) REFERENCES Chambre(id_chambre)
);

CREATE TABLE Paiement (
  id_paiement NUMBER CONSTRAINT pk_paiement PRIMARY KEY,
  montant NUMBER(10,2) CONSTRAINT nn_paiement_montant NOT NULL,
  date_paiement DATE CONSTRAINT nn_paiement_date NOT NULL,
  mode_paiement VARCHAR2(20 CHAR) CONSTRAINT nn_paiement_mode NOT NULL,
  id_res NUMBER CONSTRAINT nn_paiement_res NOT NULL,
  id_employe NUMBER,
  CONSTRAINT chk_paiement_montant CHECK (montant > 0),
  CONSTRAINT chk_paiement_mode CHECK (mode_paiement IN ('cash', 'carte', 'virement', 'en_ligne')),
  CONSTRAINT fk_paiement_res FOREIGN KEY (id_res) REFERENCES Reservation(id_res),
  CONSTRAINT fk_paiement_employe FOREIGN KEY (id_employe) REFERENCES Employe(id_employe)
);

CREATE TABLE ConsommationService (
  id_conso NUMBER CONSTRAINT pk_conso_service PRIMARY KEY,
  date_heure TIMESTAMP DEFAULT SYSTIMESTAMP CONSTRAINT nn_conso_date NOT NULL,
  qte NUMBER CONSTRAINT nn_conso_qte NOT NULL,
  montant NUMBER(10,2),
  id_res NUMBER CONSTRAINT nn_conso_res NOT NULL,
  id_service NUMBER CONSTRAINT nn_conso_service NOT NULL,
  CONSTRAINT chk_conso_qte CHECK (qte > 0),
  CONSTRAINT chk_conso_montant CHECK (montant IS NULL OR montant >= 0),
  CONSTRAINT fk_conso_res FOREIGN KEY (id_res) REFERENCES Reservation(id_res),
  CONSTRAINT fk_conso_service FOREIGN KEY (id_service) REFERENCES Service(id_service)
);

CREATE TABLE PersonneReservation (
  id_pr NUMBER CONSTRAINT pk_personne_res PRIMARY KEY,
  id_res NUMBER CONSTRAINT nn_personne_res_res NOT NULL,
  id_type_personne NUMBER CONSTRAINT nn_personne_res_type NOT NULL,
  quantite NUMBER CONSTRAINT nn_personne_res_qte NOT NULL,
  CONSTRAINT chk_personne_res_qte CHECK (quantite > 0),
  CONSTRAINT uq_personne_res_type UNIQUE (id_res, id_type_personne),
  CONSTRAINT fk_personne_res_res FOREIGN KEY (id_res) REFERENCES Reservation(id_res),
  CONSTRAINT fk_personne_res_type FOREIGN KEY (id_type_personne) REFERENCES TypePersonne(id_type_personne)
);

CREATE SEQUENCE seq_client START WITH 1 INCREMENT BY 1 CACHE 20;
CREATE SEQUENCE seq_typechambre START WITH 1 INCREMENT BY 1 CACHE 20;
CREATE SEQUENCE seq_chambre START WITH 1 INCREMENT BY 1 CACHE 20;
CREATE SEQUENCE seq_employe START WITH 1 INCREMENT BY 1 CACHE 20;
CREATE SEQUENCE seq_service START WITH 1 INCREMENT BY 1 CACHE 20;
CREATE SEQUENCE seq_reservation START WITH 1 INCREMENT BY 1 CACHE 20;
CREATE SEQUENCE seq_paiement START WITH 1 INCREMENT BY 1 CACHE 20;
CREATE SEQUENCE seq_conso START WITH 1 INCREMENT BY 1 CACHE 20;
CREATE SEQUENCE seq_pr START WITH 1 INCREMENT BY 1 CACHE 20;

