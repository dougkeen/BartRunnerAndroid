package com.dougkeen.bart;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.wearable.DataMap;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

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
    private Notification.Builder builder;
    private static Timer timer;

    public WearableDeparture(Context ctx) {
        context = ctx;
        notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent i = new Intent(context, DataListenerService.class)
                .putExtra("stop_notification", true);
        PendingIntent pi = PendingIntent.getService(context, 1, i, PendingIntent.FLAG_CANCEL_CURRENT);
        builder = new Notification.Builder(context)
                .setSmallIcon(com.dougkeen.bart.R.drawable.ic_launcher)
                .addAction(R.drawable.ic_launcher, "Cancel", pi)
                .setOngoing(true)
        ;

    }

    public void update(DataMap dataMap) {
        if (dataMap != null) {
            updateFromDataMap(dataMap);
        } else {
            cancelNotification();
            departure_time = null;
            destination = null;
        }
    }

    public void updateFromDataMap(DataMap dataMap) {
        departure_time = new Date(dataMap.getLong("departure_time"));
        destination = dataMap.getString("destination");

        if (timer == null) {
            timer = new Timer(true);
            timer.scheduleAtFixedRate(
                    new TimerTask() {
                        @Override
                        public void run() {
                            updateNotification();
                        }
                    },
                    0,
                    1000
            );
        }
    }

    public void updateNotification() {

        long duration = departure_time.getTime() - new Date().getTime();
        long minutes = duration / 60000;
        long seconds = ((duration - minutes * 60000) % 60000) / 1000;
        String s = String.format("%dm%02ds", minutes, seconds);
        builder.setContentTitle(destination.toUpperCase() + " train in " + s);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    public void cancelNotification() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        notificationManager.cancel(NOTIFICATION_ID);
    }
}
