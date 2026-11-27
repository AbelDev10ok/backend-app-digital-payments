package com.app.digital.payments.digital_pyments.models.dtos;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Ronda de cobro del dia: el plan ordenado por riesgo. Los montos vienen por
 * item y por moneda; el cliente de la API nunca suma monedas distintas.
 */
public class CollectionRoundDto {

    private Long id;
    private LocalDate fecha;
    private LocalDateTime createdAt;
    private List<CollectionRoundItemDto> items = new ArrayList<>();

    public CollectionRoundDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<CollectionRoundItemDto> getItems() {
        return items;
    }

    public void setItems(List<CollectionRoundItemDto> items) {
        this.items = items;
    }
}
