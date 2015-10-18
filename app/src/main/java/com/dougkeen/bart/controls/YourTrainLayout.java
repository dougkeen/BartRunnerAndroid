package com.dougkeen.bart.controls;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Checkable;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.dougkeen.bart.R;
import com.dougkeen.bart.model.Departure;
import com.dougkeen.bart.model.TextProvider;
import com.dougkeen.util.Observer;

public class YourTrainLayout extends FrameLayout implements Checkable {

    private final TextView destinationText;
    private final TextView trainLength;
    private final View colorBar;
    private final ImageView bikeIcon;
    private final View xferIcon;
    private final CountdownTextView departureCountdown;
    private final CountdownTextView arrivalCountdown;
    private final TextView alarmText;

    public YourTrainLayout(Context context) {
        this(context, null);
    }

    public YourTrainLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public YourTrainLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        LayoutInflater.from(context).inflate(R.layout.your_train, this, true);

        destinationText = (TextView) findViewById(R.id.yourTrainDestinationText);
        trainLength = (TextView) findViewById(R.id.yourTrainTrainLengthText);
        colorBar = findViewById(R.id.yourTrainDestinationColorBar);
        bikeIcon = (ImageView) findViewById(R.id.yourTrainBikeIcon);
        xferIcon = findViewById(R.id.yourTrainXferIcon);
        departureCountdown = (CountdownTextView) findViewById(R.id.yourTrainDepartureCountdown);
        arrivalCountdown = (CountdownTextView) findViewById(R.id.yourTrainArrivalCountdown);
        alarmText = (TextView) findViewById(R.id.alarmText);
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
        int colorRes = isChecked() ? R.color.blue_selection : R.color.gray;
        setBackgroundResource(colorRes);
    }

    @Override
    public void toggle() {
        setChecked(!isChecked());
    }

    public void updateFromBoardedDeparture(final Departure boardedDeparture) {
        if (boardedDeparture == null) {
            return;
        }

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

        destinationText.setText(boardedDeparture.getTrainDestination().toString());
        trainLength.setText(boardedDeparture.getTrainLengthAndPlatform());

        colorBar.setBackgroundColor(boardedDeparture.getTrainDestinationColor());
        if (boardedDeparture.isBikeAllowed()) {
            bikeIcon.setImageResource(R.drawable.bike);
        } else {
            bikeIcon.setImageResource(R.drawable.nobike);
        }
        if (boardedDeparture.getRequiresTransfer()) {
            xferIcon.setVisibility(View.VISIBLE);
        } else {
            xferIcon.setVisibility(View.INVISIBLE);
        }

        updateAlarmIndicator();

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
            alarmText.setVisibility(GONE);
        } else {
            alarmText.setVisibility(VISIBLE);
            alarmText.setText(String.valueOf(mDeparture.getAlarmLeadTimeMinutes()));
        }
    }

    private final Runnable mUpdateAlarmIndicatorRunnable = new Runnable() {
        @Override
        public void run() {
            updateAlarmIndicator();
        }
    };
}
