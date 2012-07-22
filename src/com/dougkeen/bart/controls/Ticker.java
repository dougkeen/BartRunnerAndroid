package com.dougkeen.bart.controls;

import java.util.Iterator;
import java.util.WeakHashMap;

import android.os.Handler;
import android.util.Log;

public class Ticker {
	public static interface TickSubscriber {
		int getTickInterval();

		void onTick();
	}

	private static Ticker sInstance;

	private WeakHashMap<TickSubscriber, Object> mSubscribers;

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
					subscriber.onTick();
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

	};

	public synchronized static Ticker getInstance() {
		if (sInstance == null) {
			sInstance = new Ticker();
		}
		return sInstance;
	}

	public void addSubscriber(TickSubscriber subscriber) {
		if (!mSubscribers.containsKey(subscriber) && subscriber != null) {
			mSubscribers.put(subscriber, null);
			startTicking();
		}
	}

	private Ticker() {
		mSubscribers = new WeakHashMap<TickSubscriber, Object>();
		mEngine = new TickerEngine(this);
	}

	public void startTicking() {
		if (!mEngine.isOn())
			mEngine.run();
	}

	public void stopTicking() {
		if (mEngine.isOn())
			mEngine.stop();
	}

}
