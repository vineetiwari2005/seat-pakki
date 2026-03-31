package com.driver.bookMyShow.Models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shows")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "show_id")
    private Integer id;

    @Column(nullable = false)
    private Time time;

    @Column(nullable = false)
    private Date date;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "movie_id", nullable = false)
    @JsonIgnoreProperties({"shows", "createdAt", "updatedAt"})
    private Movie movie;

    @ManyToOne
    @JoinColumn(name = "theater_id", nullable = false)
    @JsonIgnoreProperties({"shows", "theaterSeats", "createdAt"})
    private Theater theater;

    @OneToMany(mappedBy = "show", cascade = CascadeType.ALL)
    @JsonIgnoreProperties({"show"})
    @Builder.Default
    private List<ShowSeat> showSeatList = new ArrayList<>();

    @OneToMany(mappedBy = "show", cascade = CascadeType.ALL)
    @JsonIgnoreProperties({"show"})
    @Builder.Default
    private List<Ticket> ticketList = new ArrayList<>();
}
