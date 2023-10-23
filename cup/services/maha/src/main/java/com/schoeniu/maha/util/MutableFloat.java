package com.schoeniu.maha.util;

import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Mutable float class to be used as binding object for gauge metrics
 */
@ToString
@NoArgsConstructor
public class MutableFloat extends Number {

    private float value;

    public static MutableFloat of(final Number value) {
        MutableFloat mutableFloat = new MutableFloat();
        mutableFloat.setValue(value);
        return mutableFloat;
    }

    @Override
    public int intValue() {
        return Math.round(value);
    }

    @Override
    public long longValue() {
        return Math.round(value);
    }

    @Override
    public float floatValue() {
        return value;
    }

    @Override
    public double doubleValue() {
        return value;
    }

    public void setValue(final Number value) {
        this.value = value.floatValue();
    }
}
