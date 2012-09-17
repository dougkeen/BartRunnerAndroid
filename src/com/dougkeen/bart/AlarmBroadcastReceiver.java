package com.dougkeen.bart;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		WakeLocker.acquire(context);

		Intent targetIntent = new Intent(Intent.ACTION_VIEW, intent.getData());
		targetIntent.putExtra("boardedDeparture", intent.getExtras()
				.getParcelable("departure"));
		targetIntent.putExtra("soundAlarm", true);
		targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		context.startActivity(targetIntent);
	}

}
