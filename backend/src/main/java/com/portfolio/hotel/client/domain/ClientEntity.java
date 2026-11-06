package com.portfolio.hotel.client.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "CLIENT")
public class ClientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "client_sequence")
    @SequenceGenerator(name = "client_sequence", sequenceName = "SEQ_CLIENT", allocationSize = 1)
    @Column(name = "ID_CLIENT")
    private Long id;

    @Column(name = "NOM", nullable = false, length = 100)
    private String lastName;

    @Column(name = "PRENOM", nullable = false, length = 100)
    private String firstName;

    @Column(name = "TEL", length = 20)
    private String phone;

    @Column(name = "EMAIL", length = 200)
    private String email;

    @Column(name = "CIN", length = 20)
    private String nationalId;

    @Column(name = "DATE_NAISSANCE")
    private LocalDate birthDate;

    protected ClientEntity() {
    }

    public ClientEntity(String lastName, String firstName, String phone, String email) {
        this.lastName = lastName;
        this.firstName = firstName;
        this.phone = phone;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }
}
