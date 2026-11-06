package com.portfolio.hotel.reservation.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "RESERVATION")
public class ReservationEntity {

    @Id
    @Column(name = "ID_RES")
    private Long id;

    @Column(name = "DATE_DEB", nullable = false)
    private LocalDate checkIn;

    @Column(name = "DATE_FIN", nullable = false)
    private LocalDate checkOut;

    @Column(name = "NB_PERSONNES", nullable = false)
    private Integer guestCount;

    @Column(name = "STATUT", nullable = false, length = 20)
    private String status;

    @Column(name = "ID_CLIENT", nullable = false)
    private Long clientId;

    @Column(name = "ID_CHAMBRE", nullable = false)
    private Long roomId;

    protected ReservationEntity() {
    }
}

