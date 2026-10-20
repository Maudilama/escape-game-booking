package com.demo.escape_game_booking.booking;

import com.demo.escape_game_booking.common.BusinessRuleException;
import com.demo.escape_game_booking.room.Room;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false, length = 100)
    private String customerName;

    @Column(nullable = false)
    private String customerEmail;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @Column(nullable = false)
    private short playerCount;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Version
    private long version;

    protected Booking() {
    }

    public Booking(Room room, String customerName, String customerEmail,
                   LocalDateTime startAt, short playerCount, LocalDateTime now) {
        this.room = room;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.startAt = startAt;
        this.endAt = startAt.plusMinutes(room.getSessionMinutes());
        this.playerCount = playerCount;
        this.totalPrice = room.getPricePerPerson().multiply(BigDecimal.valueOf(playerCount));
        this.status = BookingStatus.CONFIRMED;
        this.createdAt = now;
    }

    public void cancel(LocalDateTime now, int minHoursBeforeStart) {
        if (status == BookingStatus.CANCELLED) {
            throw new BusinessRuleException("Cette réservation est déjà annulée.");
        }
        if (now.plusHours(minHoursBeforeStart).isAfter(startAt)) {
            throw new BusinessRuleException(
                    "Une réservation ne peut plus être annulée moins de "
                            + minHoursBeforeStart + " h avant la session.");
        }
        this.status = BookingStatus.CANCELLED;
    }

    public boolean overlaps(LocalDateTime start, LocalDateTime end) {
        return startAt.isBefore(end) && endAt.isAfter(start);
    }

    public Long getId() { return id; }
    public Room getRoom() { return room; }
    public String getCustomerName() { return customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public LocalDateTime getStartAt() { return startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public short getPlayerCount() { return playerCount; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public BookingStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}