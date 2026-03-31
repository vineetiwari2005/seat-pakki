package com.driver.bookMyShow.Models;

import com.driver.bookMyShow.Enums.SeatType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "theater_seats")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TheaterSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "seat_no", nullable = false)
    private String seatNo;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "seat_type", nullable = false)
    private SeatType seatType;

    @ManyToOne
    @JoinColumn(name = "theater_id", nullable = false)
    @JsonIgnoreProperties({"theaterSeatList", "showList", "createdAt"})
    private Theater theater;
}









