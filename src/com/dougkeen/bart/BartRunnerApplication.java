package com.dougkeen.bart;

import android.app.Application;
import android.media.MediaPlayer;

import com.dougkeen.bart.model.Departure;
import com.dougkeen.util.Observable;

public class BartRunnerApplication extends Application {
	private Departure mBoardedDeparture;

	private Observable<Boolean> mAlarmPending = new Observable<Boolean>(false);

	private boolean mPlayAlarmRingtone;

	private boolean mAlarmSounding;

	private MediaPlayer mAlarmMediaPlayer;

	public boolean shouldPlayAlarmRingtone() {
		return mPlayAlarmRingtone;
	}

	public void setPlayAlarmRingtone(boolean playAlarmRingtone) {
		this.mPlayAlarmRingtone = playAlarmRingtone;
	}

	public Departure getBoardedDeparture() {
		return mBoardedDeparture;
	}

	public void setBoardedDeparture(Departure boardedDeparture) {
		this.mBoardedDeparture = boardedDeparture;
	}

	public boolean isAlarmSounding() {
		return mAlarmSounding;
	}

	public void setAlarmSounding(boolean alarmSounding) {
		this.mAlarmSounding = alarmSounding;
	}

	public MediaPlayer getAlarmMediaPlayer() {
		return mAlarmMediaPlayer;
	}

	public void setAlarmMediaPlayer(MediaPlayer alarmMediaPlayer) {
		this.mAlarmMediaPlayer = alarmMediaPlayer;
	}

	public boolean isAlarmPending() {
		return mAlarmPending.getValue();
	}

	public Observable<Boolean> getAlarmPendingObservable() {
		return mAlarmPending;
	}

	public void setAlarmPending(boolean alarmPending) {
		this.mAlarmPending.setValue(alarmPending);
	}

}