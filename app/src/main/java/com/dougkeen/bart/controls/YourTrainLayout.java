package com.dougkeen.bart.controls;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dougkeen.bart.BartRunnerApplication;
import com.dougkeen.bart.R;
import com.dougkeen.bart.model.Departure;
import com.dougkeen.bart.model.TextProvider;
import com.dougkeen.util.Observer;

public class YourTrainLayout extends RelativeLayout implements Checkable {

	public YourTrainLayout(Context context) {
		super(context);
		assignLayout(context);
	}

	public YourTrainLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		assignLayout(context);
	}

	public YourTrainLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		assignLayout(context);
	}

	public void assignLayout(Context context) {
		LayoutInflater.from(context).inflate(R.layout.your_train, this, true);
	}

	private boolean mChecked;

	private Departure mDeparture;

	private final Observer<Integer> mAlarmLeadObserver = new Observer<Integer>() {
		@Override
		public void onUpdate(Integer newValue) {
			final Activity context = (Activity) getContext();
			if (context != null) {
				context.runOnUiThread(mUpdateAlarmIndicatorRunnable);
			}
		}
	};

	private final Observer<Boolean> mAlarmPendingObserver = new Observer<Boolean>() {
		@Override
		public void onUpdate(Boolean newValue) {
			final Activity context = (Activity) getContext();
			if (context != null) {
				context.runOnUiThread(mUpdateAlarmIndicatorRunnable);
			}
		}
	};

	@Override
	public boolean isChecked() {
		return mChecked;
	}

	@Override
	public void setChecked(boolean checked) {
		mChecked = checked;
		setBackground();
	}

	private void setBackground() {
		if (isChecked()) {
			setBackgroundDrawable(getContext().getResources().getDrawable(
					R.color.blue_selection));
		} else {
			setBackgroundDrawable(getContext().getResources().getDrawable(
					R.color.gray));
		}
	}

	@Override
	public void toggle() {
		setChecked(!isChecked());
	}

	public void updateFromBoardedDeparture() {
		final Departure boardedDeparture = ((BartRunnerApplication) ((Activity) getContext())
				.getApplication()).getBoardedDeparture();
		if (boardedDeparture == null)
			return;

		if (!boardedDeparture.equals(mDeparture)) {
			if (mDeparture != null) {
				mDeparture.getAlarmLeadTimeMinutesObservable()
						.unregisterObserver(mAlarmLeadObserver);
				mDeparture.getAlarmPendingObservable().unregisterObserver(
						mAlarmPendingObserver);
			}
			boardedDeparture.getAlarmLeadTimeMinutesObservable()
					.registerObserver(mAlarmLeadObserver);
			boardedDeparture.getAlarmPendingObservable().registerObserver(
					mAlarmPendingObserver);
		}

		mDeparture = boardedDeparture;

		((TextView) findViewById(R.id.yourTrainDestinationText))
				.setText(boardedDeparture.getTrainDestination().toString());

		((TextView) findViewById(R.id.yourTrainTrainLengthText))
				.setText(boardedDeparture.getTrainLengthAndPlatform());

		ImageView colorBar = (ImageView) findViewById(R.id.yourTrainDestinationColorBar);
		((GradientDrawable) colorBar.getDrawable()).setColor(Color
				.parseColor(boardedDeparture.getTrainDestinationColor()));
		ImageView bikeIcon = (ImageView) findViewById(R.id.yourTrainBikeIcon);
		if (boardedDeparture.isBikeAllowed()) {
			bikeIcon.setImageDrawable(getResources().getDrawable(
					R.drawable.bike));
		} else {
			bikeIcon.setImageDrawable(getResources().getDrawable(
					R.drawable.nobike));
		}
		if (boardedDeparture.getRequiresTransfer()) {
			((ImageView) findViewById(R.id.yourTrainXferIcon))
					.setVisibility(View.VISIBLE);
		} else {
			((ImageView) findViewById(R.id.yourTrainXferIcon))
					.setVisibility(View.INVISIBLE);
		}

		updateAlarmIndicator();

		CountdownTextView departureCountdown = (CountdownTextView) findViewById(R.id.yourTrainDepartureCountdown);
		CountdownTextView arrivalCountdown = (CountdownTextView) findViewById(R.id.yourTrainArrivalCountdown);

		final TextProvider textProvider = new TextProvider() {
			@Override
			public String getText(long tickNumber) {
				if (boardedDeparture.hasDeparted()) {
					return boardedDeparture.getCountdownText();
				} else {
					return "Leaves in " + boardedDeparture.getCountdownText()
							+ " " + boardedDeparture.getUncertaintyText();
				}
			}
		};
		departureCountdown.setText(textProvider.getText(0));
		departureCountdown.setTextProvider(textProvider);

		arrivalCountdown.setText(boardedDeparture
				.getEstimatedArrivalMinutesLeftText(getContext()));
		arrivalCountdown.setTextProvider(new TextProvider() {
			@Override
			public String getText(long tickNumber) {
				return boardedDeparture
						.getEstimatedArrivalMinutesLeftText(getContext());
			}
		});

		setBackground();
	}

	private void updateAlarmIndicator() {
		if (!mDeparture.isAlarmPending()) {
			findViewById(R.id.alarmText).setVisibility(GONE);
		} else {
			findViewById(R.id.alarmText).setVisibility(VISIBLE);
			((TextView) findViewById(R.id.alarmText)).setText(String
					.valueOf(mDeparture.getAlarmLeadTimeMinutes()));
		}
	}

	private final Runnable mUpdateAlarmIndicatorRunnable = new Runnable() {
		@Override
		public void run() {
			updateAlarmIndicator();
		}
	};
}
