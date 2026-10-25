package com.app.digital.payments.digital_pyments.models.dtos;

public class AttributeValueDto {

    private Long id;
    private String value;

    public AttributeValueDto() {
    }

    public AttributeValueDto(Long id, String value) {
        this.id = id;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
