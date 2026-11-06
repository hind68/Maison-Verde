package com.portfolio.hotel.room.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "CHAMBRE")
public class RoomEntity {

    @Id
    @Column(name = "ID_CHAMBRE")
    private Long id;

    @Column(name = "NUMERO", nullable = false, length = 10)
    private String number;

    @Column(name = "ETAGE", nullable = false)
    private Integer floor;

    @Column(name = "STATUT", nullable = false, length = 20)
    private String status;

    @Column(name = "ETAT_OPERATIONNEL", nullable = false, length = 20)
    private String operationalStatus;

    @Column(name = "ETAT_PROPRETE", nullable = false, length = 20)
    private String housekeepingStatus;

    @Column(name = "ID_TYPE", nullable = false)
    private Long roomTypeId;

    protected RoomEntity() {
    }
}
