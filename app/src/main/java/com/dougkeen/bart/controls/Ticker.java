package com.dougkeen.bart.controls;

import java.util.Iterator;
import java.util.WeakHashMap;

import android.content.Context;
import android.os.Handler;

public class Ticker {
    public interface TickSubscriber {
        int getTickInterval();

        void onTick(long mTickCount);
    }

    private static Ticker sInstance;

    private WeakHashMap<TickSubscriber, Object> mSubscribers;

    private WeakHashMap<Context, Object> mTickerHosts;

    private TickerEngine mEngine;

    private static class TickerEngine implements Runnable {

        private static final int TICK_INTERVAL_MILLIS = 1000;
        private Ticker publisher;
        private Handler mHandler;
        private boolean mPendingRequest = false;
        private boolean mForceStop = false;
        private long mTickCount = 0;

        public TickerEngine(Ticker publisher) {
            this.publisher = publisher;
            this.mHandler = new Handler();
        }

        @Override
        public void run() {
            mPendingRequest = false;
            if (mForceStop) {
                mForceStop = false;
                return;
            }

            long startTimeNanos = System.nanoTime();
            Iterator<TickSubscriber> iterator = publisher.mSubscribers.keySet()
                    .iterator();
            boolean stillHasListeners = false;
            while (iterator.hasNext()) {
                TickSubscriber subscriber = iterator.next();
                if (subscriber == null) {
                    continue;
                }

                stillHasListeners = true;
                if (subscriber.getTickInterval() > 0
                        && mTickCount % subscriber.getTickInterval() == 0)
                    subscriber.onTick(mTickCount);
            }
            long endTimeNanos = System.nanoTime();

            if (stillHasListeners && !mPendingRequest) {
                mHandler.postDelayed(this, TICK_INTERVAL_MILLIS
                        - ((endTimeNanos - startTimeNanos) / 1000000));
                mPendingRequest = true;
                mTickCount++;
            } else {
                mPendingRequest = false;
            }
        }

        public boolean isOn() {
            return mPendingRequest;
        }

        public void stop() {
            mForceStop = true;
        }

    }

    public synchronized static Ticker getInstance() {
        if (sInstance == null) {
            sInstance = new Ticker();
        }
        return sInstance;
    }

    public void addSubscriber(TickSubscriber subscriber, Context host) {
        if (!mSubscribers.containsKey(subscriber) && subscriber != null) {
            mSubscribers.put(subscriber, null);
            startTicking(host);
        }
    }

    private Ticker() {
        mSubscribers = new WeakHashMap<TickSubscriber, Object>();
        mTickerHosts = new WeakHashMap<Context, Object>();
        mEngine = new TickerEngine(this);
    }

    public void startTicking(Context host) {
        mTickerHosts.put(host, true);
        if (!mEngine.isOn())
            mEngine.run();
    }

    public void stopTicking(Context host) {
        mTickerHosts.remove(host);
        if (mEngine.isOn() && mTickerHosts.isEmpty())
            mEngine.stop();
    }
}
