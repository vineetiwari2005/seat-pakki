package com.driver.bookMyShow.Models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "theaters")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Theater {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String address;

    @ManyToOne
    @JoinColumn(name = "city_id")
    @JsonIgnoreProperties({"theaters"})
    private City city;

    @Column(name = "city")
    private String cityName;

    /**
     * The Theatre Admin (THEATER_OWNER) assigned to manage this theatre.
     * Each theatre has exactly one admin. Set by Main Admin.
     */
    @ManyToOne
    @JoinColumn(name = "admin_id")
    @JsonIgnoreProperties({"ticketList", "password"})
    private User admin;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "theater", cascade = CascadeType.ALL)
    @JsonIgnoreProperties({"theater"})
    @Builder.Default
    private List<TheaterSeat> theaterSeatList = new ArrayList<>();

    @OneToMany(mappedBy = "theater", cascade = CascadeType.ALL)
    @JsonIgnoreProperties({"theater", "movie", "showSeatList", "ticketList"})
    @Builder.Default
    private List<Show> showList = new ArrayList<>();
}
