/**
 * Definicion global del filtro de tenancy de Hibernate. Cada entidad del
 * dominio se anota con @Filter(name = "tenantFilter") y el filtro se activa
 * por request con el businessId del token (ver TenantContext).
 */
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "businessId", type = Long.class))
package com.app.digital.payments.digital_pyments.models;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
