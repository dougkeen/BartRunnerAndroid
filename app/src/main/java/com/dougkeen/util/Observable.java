package com.dougkeen.util;

import java.util.WeakHashMap;

import org.apache.commons.lang3.ObjectUtils;

public class Observable<T> {
    private T value;
    private WeakHashMap<Observer<T>, Boolean> listeners = new WeakHashMap<Observer<T>, Boolean>();

    public Observable() {
        super();
    }

    public Observable(T value) {
        super();
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        if (!ObjectUtils.equals(this.value, value)) {
            this.value = value;
            notifyOfChange(value);
        }
    }

    public void registerObserver(Observer<T> observer) {
        listeners.put(observer, true);
    }

    public void unregisterObserver(Observer<T> observer) {
        listeners.remove(observer);
    }

    public void unregisterAllObservers() {
        listeners.clear();
    }

    protected void notifyOfChange(T value) {
        for (Observer<T> listener : listeners.keySet()) {
            if (listener != null) {
                listener.onUpdate(value);
            }
        }
    }
}
