SET PAGESIZE 50
SET FEEDBACK OFF
SET HEADING OFF

SELECT 'CONCURRENCY_RESERVATIONS=' || COUNT(*)
  FROM Reservation r
  JOIN Chambre c ON c.id_chambre = r.id_chambre
 WHERE c.numero = '301'
   AND r.date_deb < DATE '2033-06-15'
   AND r.date_fin > DATE '2033-06-10'
   AND r.statut IN ('en_attente', 'confirmée', 'en_cours');

EXIT SUCCESS
