package com.dougkeen.bart.data;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher.ViewFactory;

import com.dougkeen.bart.R;
import com.dougkeen.bart.controls.CountdownTextView;
import com.dougkeen.bart.controls.DepartureListItemLayout;
import com.dougkeen.bart.controls.TimedTextSwitcher;
import com.dougkeen.bart.model.Departure;
import com.dougkeen.bart.model.TextProvider;

public class DepartureArrayAdapter extends ArrayAdapter<Departure> {

    private Drawable noBikeDrawable;
    private Drawable bikeDrawable;

    public DepartureArrayAdapter(Context context, int textViewResourceId,
                                 Departure[] objects) {
        super(context, textViewResourceId, objects);
        assignBikeDrawables();
    }

    private void assignBikeDrawables() {
        noBikeDrawable = getContext().getResources().getDrawable(
                R.drawable.nobike);
        bikeDrawable = getContext().getResources().getDrawable(R.drawable.bike);
    }

    public DepartureArrayAdapter(Context context, int resource,
                                 int textViewResourceId, Departure[] objects) {
        super(context, resource, textViewResourceId, objects);
        assignBikeDrawables();
    }

    public DepartureArrayAdapter(Context context, int resource,
                                 int textViewResourceId, List<Departure> objects) {
        super(context, resource, textViewResourceId, objects);
        assignBikeDrawables();
    }

    public DepartureArrayAdapter(Context context, int resource,
                                 int textViewResourceId) {
        super(context, resource, textViewResourceId);
        assignBikeDrawables();
    }

    public DepartureArrayAdapter(Context context, int textViewResourceId,
                                 List<Departure> objects) {
        super(context, textViewResourceId, objects);
        assignBikeDrawables();
    }

    public DepartureArrayAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        assignBikeDrawables();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView != null
                && convertView instanceof DepartureListItemLayout) {
            view = convertView;
        } else {
            view = new DepartureListItemLayout(getContext());
        }

        final Departure departure = getItem(position);

        ((Checkable) view).setChecked(departure.isSelected());

        ((TextView) view.findViewById(R.id.destinationText)).setText(departure
                .getTrainDestination().toString());

        final String arrivesAtDestinationPrefix = getContext().getString(
                R.string.arrives_at_destination);
        final String estimatedArrivalTimeText = departure
                .getEstimatedArrivalTimeText(getContext(), false);

        TextView estimatedArrival = (TextView) view
                .findViewById(R.id.estimatedArrival);
        if (estimatedArrival != null) {
            ((TextView) view.findViewById(R.id.trainLengthText))
                    .setText(departure.getTrainLengthAndPlatform());
            estimatedArrival.setText(arrivesAtDestinationPrefix
                    + estimatedArrivalTimeText);
        } else {
            TimedTextSwitcher textSwitcher = (TimedTextSwitcher) view
                    .findViewById(R.id.trainLengthText);
            initTextSwitcher(textSwitcher,
                    R.layout.train_length_arrival_textview);

            if (!StringUtils.isBlank(estimatedArrivalTimeText)) {
                textSwitcher.setCurrentText(arrivesAtDestinationPrefix
                        + estimatedArrivalTimeText);
            } else {
                textSwitcher.setCurrentText(departure.getTrainLengthAndPlatform());
            }
            textSwitcher.setTextProvider(new TextProvider() {
                @Override
                public String getText(long tickNumber) {
                    if (tickNumber % 4 == 0) {
                        return departure.getTrainLengthAndPlatform();
                    } else {
                        final String estimatedArrivalTimeText = departure
                                .getEstimatedArrivalTimeText(getContext(), false);
                        if (StringUtils.isBlank(estimatedArrivalTimeText)) {
                            return "";
                        } else {
                            return arrivesAtDestinationPrefix
                                    + estimatedArrivalTimeText;
                        }
                    }
                }
            });
        }

        ImageView colorBar = (ImageView) view
                .findViewById(R.id.destinationColorBar);
        ((GradientDrawable) colorBar.getDrawable()).setColor(Color
                .parseColor(departure.getTrainDestinationColor()));
        CountdownTextView countdownTextView = (CountdownTextView) view
                .findViewById(R.id.countdown);
        countdownTextView.setText(departure.getCountdownText());
        countdownTextView.setTextProvider(new TextProvider() {
            @Override
            public String getText(long tickNumber) {
                return departure.getCountdownText();
            }
        });

        TextView departureTime = (TextView) view
                .findViewById(R.id.departureTime);

        if (departureTime != null) {
            ((TextView) view.findViewById(R.id.uncertainty)).setText(departure
                    .getUncertaintyText());
            departureTime.setText(departure
                    .getEstimatedDepartureTimeText(getContext(), false));
        } else {
            TimedTextSwitcher uncertaintySwitcher = (TimedTextSwitcher) view
                    .findViewById(R.id.uncertainty);
            initTextSwitcher(uncertaintySwitcher, R.layout.uncertainty_textview);

            uncertaintySwitcher.setTextProvider(new TextProvider() {
                @Override
                public String getText(long tickNumber) {
                    if (tickNumber % 4 == 0) {
                        return departure.getUncertaintyText();
                    } else {
                        return departure
                                .getEstimatedDepartureTimeText(getContext(), false);
                    }
                }
            });
        }

        ImageView bikeIcon = (ImageView) view.findViewById(R.id.bikeIcon);
        if (departure.isBikeAllowed()) {
            bikeIcon.setImageDrawable(bikeDrawable);
        } else {
            bikeIcon.setImageDrawable(noBikeDrawable);
        }
        if (departure.getRequiresTransfer()) {
            view.findViewById(R.id.xferIcon)
                    .setVisibility(View.VISIBLE);
        } else {
            view.findViewById(R.id.xferIcon)
                    .setVisibility(View.INVISIBLE);
        }

        return view;
    }

    private void initTextSwitcher(TextSwitcher textSwitcher,
                                  final int layoutView) {
        if (textSwitcher.getInAnimation() == null) {
            textSwitcher.setFactory(new ViewFactory() {
                public View makeView() {
                    return LayoutInflater.from(getContext()).inflate(
                            layoutView, null);
                }
            });

            textSwitcher.setInAnimation(AnimationUtils.loadAnimation(
                    getContext(), android.R.anim.slide_in_left));
            textSwitcher.setOutAnimation(AnimationUtils.loadAnimation(
                    getContext(), android.R.anim.slide_out_right));
        }
    }
}
