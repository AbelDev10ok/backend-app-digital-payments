package com.app.digital.payments.digital_pyments.utils;


import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValueOfEnumValidator implements ConstraintValidator<IValueOfEnum , String>  {

    private String[] acceptedValues;

    @Override
    public void initialize(IValueOfEnum annotation) {
        acceptedValues = new String[annotation.enumClass().getEnumConstants().length];
        Enum<?>[] enums = annotation.enumClass().getEnumConstants();
        for (int i = 0; i < enums.length; i++) {
            acceptedValues[i] = enums[i].name();
        }
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;
        for (String v : acceptedValues) {
            if (v.equals(value)) return true;
        }
        return false;
    }

}
