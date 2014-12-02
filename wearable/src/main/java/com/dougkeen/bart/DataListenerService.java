package com.dougkeen.bart;

import android.content.Intent;
import android.support.v4.app.NotificationManagerCompat;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by mmullins on 11/29/14.
 */
public class DataListenerService extends WearableListenerService {
    private WearableDeparture departure = null;

    @Override
    public void onCreate() {
        super.onCreate();
        departure = new WearableDeparture(this);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        NotificationManagerCompat nm = NotificationManagerCompat.from(this);
        for (DataEvent e : dataEventBuffer) {
            DataMap dataMap = null;

            switch (e.getType()) {
                case DataEvent.TYPE_CHANGED:
                    dataMap = DataMapItem.fromDataItem(e.getDataItem()).getDataMap();
                    break;
                case DataEvent.TYPE_DELETED:
                    dataMap = null;
                    break;
            }

            departure.update(dataMap);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (intent.getBooleanExtra("stop_notification", false)) {
            departure.update(null);
        }

        return START_NOT_STICKY;
    }
}
