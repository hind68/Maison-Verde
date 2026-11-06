SET SERVEROUTPUT ON
SET DEFINE OFF
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

DECLARE
  v_client NUMBER;
  v_room NUMBER;
  v_res NUMBER;
BEGIN
  SELECT id_client INTO v_client FROM Client WHERE email = 'amine.demo@example.test';
  SELECT id_chambre INTO v_room FROM Chambre WHERE numero = '301';
  pkg_reservation_app.creer_reservation_confirmee(
    DATE '2033-06-10', DATE '2033-06-15', 2, 0, v_client, v_room, v_res
  );
  DBMS_SESSION.SLEEP(5);
  COMMIT;
  DBMS_OUTPUT.PUT_LINE('CONCURRENCY_A_SUCCESS=' || v_res);
END;
/

