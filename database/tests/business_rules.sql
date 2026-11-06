SET SERVEROUTPUT ON
SET DEFINE OFF
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

DECLARE
  v_client NUMBER;
  v_room_double NUMBER;
  v_room_family NUMBER;
  v_adulte NUMBER;
  v_res NUMBER;
  v_res_base NUMBER;
  v_count NUMBER;

  PROCEDURE assert_true(p_condition BOOLEAN, p_message VARCHAR2) IS
  BEGIN
    IF NOT p_condition THEN
      RAISE_APPLICATION_ERROR(-20990, 'ECHEC: ' || p_message);
    END IF;
    DBMS_OUTPUT.PUT_LINE('OK: ' || p_message);
  END;

  PROCEDURE expect_error(p_expected NUMBER, p_actual NUMBER, p_message VARCHAR2) IS
  BEGIN
    assert_true(p_actual = p_expected, p_message || ' (code ' || p_actual || ')');
  END;
BEGIN
  SELECT id_client INTO v_client FROM Client WHERE email = 'amine.demo@example.test';
  SELECT id_chambre INTO v_room_double FROM Chambre WHERE numero = '102';
  SELECT id_chambre INTO v_room_family FROM Chambre WHERE numero = '301';
  SELECT id_type_personne INTO v_adulte FROM TypePersonne WHERE libelle = 'adulte';

  SAVEPOINT test_start;

  BEGIN
    pkg_reservation_app.creer_reservation_confirmee(
      DATE '2032-01-10', DATE '2032-01-10', 1, 0, v_client, v_room_double, v_res
    );
    RAISE_APPLICATION_ERROR(-20991, 'Le séjour nul aurait dû échouer.');
  EXCEPTION
    WHEN OTHERS THEN expect_error(-20003, SQLCODE, 'séjour de zéro nuit refusé');
  END;

  BEGIN
    pkg_reservation_app.creer_reservation_confirmee(
      DATE '2032-01-12', DATE '2032-01-11', 1, 0, v_client, v_room_double, v_res
    );
    RAISE_APPLICATION_ERROR(-20991, 'Les dates inversées auraient dû échouer.');
  EXCEPTION
    WHEN OTHERS THEN expect_error(-20003, SQLCODE, 'dates inversées refusées');
  END;

  BEGIN
    pkg_reservation_app.creer_reservation_confirmee(
      DATE '2032-02-01', DATE '2032-02-03', 3, 0, v_client, v_room_double, v_res
    );
    RAISE_APPLICATION_ERROR(-20991, 'La capacité aurait dû être refusée.');
  EXCEPTION
    WHEN OTHERS THEN expect_error(-20002, SQLCODE, 'capacité de chambre respectée');
  END;

  v_res := seq_reservation.NEXTVAL;
  INSERT INTO Reservation(id_res, date_deb, date_fin, nb_personnes, statut, id_client, id_chambre)
  VALUES (v_res, DATE '2032-03-01', DATE '2032-03-03', 2, 'en_attente', v_client, v_room_family);
  INSERT INTO PersonneReservation(id_res, id_type_personne, quantite)
  VALUES (v_res, v_adulte, 1);
  BEGIN
    UPDATE Reservation SET statut = 'confirmée' WHERE id_res = v_res;
    RAISE_APPLICATION_ERROR(-20991, 'La cohérence des occupants aurait dû échouer.');
  EXCEPTION
    WHEN OTHERS THEN expect_error(-20004, SQLCODE, 'occupants incohérents refusés');
  END;
  ROLLBACK TO test_start;

  pkg_reservation_app.creer_reservation_confirmee(
    DATE '2032-04-10', DATE '2032-04-15', 2, 1, v_client, v_room_family, v_res_base
  );
  DBMS_OUTPUT.PUT_LINE('OK: création complète de réservation');

  BEGIN
    pkg_reservation_app.creer_reservation_confirmee(
      DATE '2032-04-12', DATE '2032-04-14', 1, 0, v_client, v_room_family, v_res
    );
    RAISE_APPLICATION_ERROR(-20991, 'Le chevauchement aurait dû échouer.');
  EXCEPTION
    WHEN OTHERS THEN expect_error(-20001, SQLCODE, 'chevauchement refusé');
  END;

  pkg_reservation_app.creer_reservation_confirmee(
    DATE '2032-04-15', DATE '2032-04-17', 1, 0, v_client, v_room_family, v_res
  );
  DBMS_OUTPUT.PUT_LINE('OK: séjour adjacent accepté');
  ROLLBACK TO test_start;

  SAVEPOINT rollback_test;
  BEGIN
    pkg_reservation_app.creer_reservation_confirmee(
      DATE '2032-05-01', DATE '2032-05-03', 1, 0, v_client, v_room_double, v_res
    );
    INSERT INTO PersonneReservation(id_res, id_type_personne, quantite)
    VALUES (v_res, v_adulte, 1);
  EXCEPTION
    WHEN OTHERS THEN
      ROLLBACK TO rollback_test;
  END;

  SELECT COUNT(*) INTO v_count FROM Reservation WHERE id_res = v_res;
  assert_true(v_count = 0, 'rollback intégral après échec intermédiaire');

  ROLLBACK TO test_start;
  DBMS_OUTPUT.PUT_LINE('TOUS_LES_TESTS_SEQUENTIELS_OK');
END;
/

