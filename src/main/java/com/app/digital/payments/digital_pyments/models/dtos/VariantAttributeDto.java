package com.app.digital.payments.digital_pyments.models.dtos;

public class VariantAttributeDto {

    private Long attributeId;
    private String attributeName;
    private Long attributeValueId;
    private String value;

    public VariantAttributeDto() {
    }

    public VariantAttributeDto(Long attributeId, String attributeName, Long attributeValueId, String value) {
        this.attributeId = attributeId;
        this.attributeName = attributeName;
        this.attributeValueId = attributeValueId;
        this.value = value;
    }

    public Long getAttributeId() {
        return attributeId;
    }

    public void setAttributeId(Long attributeId) {
        this.attributeId = attributeId;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public Long getAttributeValueId() {
        return attributeValueId;
    }

    public void setAttributeValueId(Long attributeValueId) {
        this.attributeValueId = attributeValueId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
