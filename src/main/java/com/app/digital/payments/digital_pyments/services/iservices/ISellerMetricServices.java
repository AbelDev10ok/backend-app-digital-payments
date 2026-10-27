package com.app.digital.payments.digital_pyments.services.iservices;

import com.app.digital.payments.digital_pyments.models.dtos.SellerMetricsDto;

public interface ISellerMetricServices {

    SellerMetricsDto getSellerMetrics(int year, int month);
}
