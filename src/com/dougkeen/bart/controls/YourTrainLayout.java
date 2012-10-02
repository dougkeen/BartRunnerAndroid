package com.dougkeen.bart.controls;

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

import com.dougkeen.bart.R;
import com.dougkeen.bart.model.Departure;
import com.dougkeen.bart.model.TextProvider;

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

	public void updateFromDeparture(final Departure boardedDeparture) {
		((TextView) findViewById(R.id.yourTrainDestinationText))
				.setText(boardedDeparture.getTrainDestination().toString());

		((TextView) findViewById(R.id.yourTrainTrainLengthText))
				.setText(boardedDeparture.getTrainLengthText());

		ImageView colorBar = (ImageView) findViewById(R.id.yourTrainDestinationColorBar);
		((GradientDrawable) colorBar.getDrawable()).setColor(Color
				.parseColor(boardedDeparture.getTrainDestinationColor()));
		if (boardedDeparture.isBikeAllowed()) {
			((ImageView) findViewById(R.id.yourTrainBikeIcon))
					.setVisibility(View.VISIBLE);
		} else {
			((ImageView) findViewById(R.id.yourTrainBikeIcon))
					.setVisibility(View.INVISIBLE);
		}
		if (boardedDeparture.getRequiresTransfer()) {
			((ImageView) findViewById(R.id.yourTrainXferIcon))
					.setVisibility(View.VISIBLE);
		} else {
			((ImageView) findViewById(R.id.yourTrainXferIcon))
					.setVisibility(View.INVISIBLE);
		}
		CountdownTextView departureCountdown = (CountdownTextView) findViewById(R.id.yourTrainDepartureCountdown);
		CountdownTextView arrivalCountdown = (CountdownTextView) findViewById(R.id.yourTrainArrivalCountdown);

		final TextProvider textProvider = new TextProvider() {
			@Override
			public String getText(long tickNumber) {
				if (boardedDeparture.hasDeparted()) {
					return "Departed";
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
}
