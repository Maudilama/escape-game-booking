package com.demo.escape_game_booking.booking;

import com.demo.escape_game_booking.common.BusinessRuleException;
import com.demo.escape_game_booking.room.Room;
import com.demo.escape_game_booking.user.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

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
        // requis par JPA
    }

    public Booking(Room room, User user, LocalDateTime startAt, short playerCount, LocalDateTime now) {
        this.room = room;
        this.user = user;
        this.startAt = startAt;
        this.endAt = startAt.plusMinutes(room.getSessionMinutes());
        this.playerCount = playerCount;
        this.totalPrice = room.getPricePerPerson().multiply(BigDecimal.valueOf(playerCount));
        this.status = BookingStatus.CONFIRMED;
        this.createdAt = now;
    }

    /** Annulation par le client : soumise au délai minimum. */
    public void cancel(LocalDateTime now, int minHoursBeforeStart) {
        ensureNotCancelled();
        if (now.plusHours(minHoursBeforeStart).isAfter(startAt)) {
            throw new BusinessRuleException(
                    "Une réservation ne peut plus être annulée moins de "
                            + minHoursBeforeStart + " h avant la session.");
        }
        this.status = BookingStatus.CANCELLED;
    }

    /** Annulation par un administrateur : possible à tout moment. */
    public void forceCancel() {
        ensureNotCancelled();
        this.status = BookingStatus.CANCELLED;
    }

    public boolean isOwnedBy(Long userId) {
        return Objects.equals(user.getId(), userId);
    }

    public boolean overlaps(LocalDateTime start, LocalDateTime end) {
        return startAt.isBefore(end) && endAt.isAfter(start);
    }

    private void ensureNotCancelled() {
        if (status == BookingStatus.CANCELLED) {
            throw new BusinessRuleException("Cette réservation est déjà annulée.");
        }
    }

    public Long getId() { return id; }
    public Room getRoom() { return room; }
    public User getUser() { return user; }
    public LocalDateTime getStartAt() { return startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public short getPlayerCount() { return playerCount; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public BookingStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}