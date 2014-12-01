package com.dougkeen.bart;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.wearable.DataMap;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by mmullins on 11/30/14.
 */
public class WearableDeparture {
    private static final int NOTIFICATION_ID = 1337;
    private static final SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
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
        Intent i = new Intent(context, DataListenerService.class)
                .putExtra("stop_notification", true)
                ;
        PendingIntent pi = PendingIntent.getService(context, 1, i, PendingIntent.FLAG_CANCEL_CURRENT);

        String s = formatter.format(departure_time).toString();
        Notification.Builder b = new Notification.Builder(context)
                .setSmallIcon(com.dougkeen.bart.R.drawable.ic_launcher)
                .setContentTitle("Train to " + destination.toUpperCase()
                        + " at " + s)
                .addAction(R.drawable.ic_launcher, "Cancel", pi)
                .setOngoing(true)
                ;
        notificationManager.notify(NOTIFICATION_ID, b.build());
    }

    public void cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
    }
}
