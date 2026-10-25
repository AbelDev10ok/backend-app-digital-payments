package com.app.digital.payments.digital_pyments.models.dtos;

import java.util.ArrayList;
import java.util.List;

public class AttributeDto {

    private Long id;
    private String name;
    private List<AttributeValueDto> values = new ArrayList<>();

    public AttributeDto() {
    }

    public AttributeDto(Long id, String name, List<AttributeValueDto> values) {
        this.id = id;
        this.name = name;
        this.values = values != null ? values : new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<AttributeValueDto> getValues() {
        return values;
    }

    public void setValues(List<AttributeValueDto> values) {
        this.values = values;
    }
}
