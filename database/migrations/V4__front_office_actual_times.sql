-- Additive Front Office migration. Safe for the existing reservation history.

ALTER TABLE Reservation ADD (
  date_arrivee_reelle TIMESTAMP,
  date_depart_reelle TIMESTAMP
);

ALTER TABLE Reservation ADD CONSTRAINT chk_reservation_actual_times CHECK (
  date_depart_reelle IS NULL
  OR (date_arrivee_reelle IS NOT NULL AND date_depart_reelle >= date_arrivee_reelle)
);

