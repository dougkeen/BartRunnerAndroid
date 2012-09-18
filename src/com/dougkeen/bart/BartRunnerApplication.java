package com.dougkeen.bart;

import com.dougkeen.bart.model.Departure;

import android.app.Application;

public class BartRunnerApplication extends Application {
	private Departure mBoardedDeparture;

	private boolean playAlarmRingtone;

	public boolean shouldPlayAlarmRingtone() {
		return playAlarmRingtone;
	}

	public void setPlayAlarmRingtone(boolean playAlarmRingtone) {
		this.playAlarmRingtone = playAlarmRingtone;
	}

	public Departure getBoardedDeparture() {
		return mBoardedDeparture;
	}

	public void setBoardedDeparture(Departure boardedDeparture) {
		this.mBoardedDeparture = boardedDeparture;
	}
}