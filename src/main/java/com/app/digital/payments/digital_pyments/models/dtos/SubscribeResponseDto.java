package com.app.digital.payments.digital_pyments.models.dtos;

public class SubscribeResponseDto {

    private String initPoint;
    private String preapprovalId;
    private String status;

    public SubscribeResponseDto() {
    }

    public SubscribeResponseDto(String initPoint, String preapprovalId, String status) {
        this.initPoint = initPoint;
        this.preapprovalId = preapprovalId;
        this.status = status;
    }

    public String getInitPoint() { return initPoint; }
    public void setInitPoint(String initPoint) { this.initPoint = initPoint; }
    public String getPreapprovalId() { return preapprovalId; }
    public void setPreapprovalId(String preapprovalId) { this.preapprovalId = preapprovalId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}