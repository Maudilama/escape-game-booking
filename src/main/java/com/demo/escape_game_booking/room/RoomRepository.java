package com.demo.escape_game_booking.room;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findAllByActiveTrueOrderByIdAsc();

    Optional<Room> findByIdAndActiveTrue(Long id);

    @EntityGraph(attributePaths = "translations")
    List<Room> findAllWithTranslationsByActiveTrueOrderByIdAsc();

    @EntityGraph(attributePaths = "translations")
    Optional<Room> findWithTranslationsByIdAndActiveTrue(Long id);
}