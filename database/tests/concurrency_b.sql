SET SERVEROUTPUT ON
SET DEFINE OFF
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

DECLARE
  v_client NUMBER;
  v_room NUMBER;
  v_res NUMBER;
BEGIN
  SELECT id_client INTO v_client FROM Client WHERE email = 'sara.demo@example.test';
  SELECT id_chambre INTO v_room FROM Chambre WHERE numero = '301';
  pkg_reservation_app.creer_reservation_confirmee(
    DATE '2033-06-12', DATE '2033-06-14', 1, 0, v_client, v_room, v_res
  );
  COMMIT;
  DBMS_OUTPUT.PUT_LINE('CONCURRENCY_B_SUCCESS=' || v_res);
END;
/

