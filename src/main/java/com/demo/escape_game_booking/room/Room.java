package com.demo.escape_game_booking.room;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String theme;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private short difficulty;

    @Column(nullable = false)
    private short minPlayers;

    @Column(nullable = false)
    private short maxPlayers;

    @Column(nullable = false)
    private int sessionMinutes;

    @Column(nullable = false)
    private int resetMinutes;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal pricePerPerson;

    @Column(nullable = false)
    private boolean active;

    protected Room() {
        // requis par JPA
    }

    public Room(String name, String theme, String description, short difficulty,
                short minPlayers, short maxPlayers, int sessionMinutes,
                int resetMinutes, BigDecimal pricePerPerson) {
        this.name = name;
        this.theme = theme;
        this.description = description;
        this.difficulty = difficulty;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.sessionMinutes = sessionMinutes;
        this.resetMinutes = resetMinutes;
        this.pricePerPerson = pricePerPerson;
        this.active = true;
    }

    public boolean acceptsPlayerCount(int playerCount) {
        return playerCount >= minPlayers && playerCount <= maxPlayers;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getTheme() { return theme; }
    public String getDescription() { return description; }
    public short getDifficulty() { return difficulty; }
    public short getMinPlayers() { return minPlayers; }
    public short getMaxPlayers() { return maxPlayers; }
    public int getSessionMinutes() { return sessionMinutes; }
    public int getResetMinutes() { return resetMinutes; }
    public BigDecimal getPricePerPerson() { return pricePerPerson; }
    public boolean isActive() { return active; }
}