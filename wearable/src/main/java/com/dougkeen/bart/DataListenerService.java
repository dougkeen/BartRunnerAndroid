package com.dougkeen.bart;

import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by mmullins on 11/29/14.
 */
public class DataListenerService extends WearableListenerService {
    private static final int NOTIFICATION_ID = 1;

    public DataListenerService() {

    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        NotificationManagerCompat nm = NotificationManagerCompat.from(this);
        for (DataEvent e : dataEventBuffer) {
            switch (e.getType()) {
                case DataEvent.TYPE_CHANGED:
                    final DataMap dataMap = DataMapItem.fromDataItem(e.getDataItem()).getDataMap();
                    Date d = new Date(dataMap.getLong("departure_time"));
                    String s = new SimpleDateFormat("HH:mm").format(d).toString();
                    NotificationCompat.Builder b = new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.ic_launcher)
                            .setContentTitle("Train to " + dataMap.getString("destination").toUpperCase()
                                    + " at " + s)
                            ;
                    nm.notify(NOTIFICATION_ID, b.build());
                    break;
                case DataEvent.TYPE_DELETED:
                    nm.cancel(NOTIFICATION_ID);
                    break;
            }
        }
    }
}
