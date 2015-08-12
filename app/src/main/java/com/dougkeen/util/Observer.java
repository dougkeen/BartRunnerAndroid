package com.dougkeen.util;


public interface Observer<T> {
    void onUpdate(final T newValue);
}
