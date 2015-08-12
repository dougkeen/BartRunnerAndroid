package com.dougkeen.bart.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.dougkeen.bart.BartRunnerApplication;
import com.dougkeen.bart.activities.ViewDeparturesActivity;
import com.dougkeen.bart.model.Constants;
import com.dougkeen.bart.model.Departure;
import com.dougkeen.util.WakeLocker;

public class AlarmBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        BartRunnerApplication application = (BartRunnerApplication) context
                .getApplicationContext();
        final Departure boardedDeparture = application.getBoardedDeparture(true);
        if (boardedDeparture == null) {
            // Nothing to notify about
            return;
        }

        WakeLocker.acquire(context);

        application.setPlayAlarmRingtone(true);

        Intent targetIntent = new Intent(context, ViewDeparturesActivity.class);
        targetIntent.putExtra(Constants.STATION_PAIR_EXTRA,
                boardedDeparture.getStationPair());
        targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(targetIntent);

        boardedDeparture.notifyAlarmHasBeenHandled();
    }

}
