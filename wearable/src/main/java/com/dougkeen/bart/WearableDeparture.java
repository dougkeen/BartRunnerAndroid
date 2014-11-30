package com.dougkeen.bart;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import com.google.android.gms.wearable.DataMap;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by mmullins on 11/30/14.
 */
public class WearableDeparture {
    private static final int NOTIFICATION_ID = 1337;
    private Date departure_time;
    private String destination;
    private Context context;
    private NotificationManager notificationManager;

    public WearableDeparture(Context ctx) {
        context = ctx;
        notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void update(DataMap dataMap) {
        if (dataMap != null) {
            updateFromDataMap(dataMap);
            updateNotification();
        } else {
            cancelNotification();
        }
    }

    public void updateFromDataMap(DataMap dataMap) {
        departure_time = new Date(dataMap.getLong("departure_time"));
        destination = dataMap.getString("destination");
    }

    public void updateNotification() {
        String s = new SimpleDateFormat("HH:mm").format(departure_time).toString();
        Notification.Builder b = new Notification.Builder(context)
                .setSmallIcon(com.dougkeen.bart.R.drawable.ic_launcher)
                .setContentTitle("Train to " + destination.toUpperCase()
                        + " at " + s);
        notificationManager.notify(NOTIFICATION_ID, b.build());
    }

    public void cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
    }
}
