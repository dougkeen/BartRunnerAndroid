package com.dougkeen.bart.data;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import android.content.Context;
import android.graphics.Color;
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

	public DepartureArrayAdapter(Context context, int textViewResourceId,
			Departure[] objects) {
		super(context, textViewResourceId, objects);
	}

	public DepartureArrayAdapter(Context context, int resource,
			int textViewResourceId, Departure[] objects) {
		super(context, resource, textViewResourceId, objects);
	}

	public DepartureArrayAdapter(Context context, int resource,
			int textViewResourceId, List<Departure> objects) {
		super(context, resource, textViewResourceId, objects);
	}

	public DepartureArrayAdapter(Context context, int resource,
			int textViewResourceId) {
		super(context, resource, textViewResourceId);
	}

	public DepartureArrayAdapter(Context context, int textViewResourceId,
			List<Departure> objects) {
		super(context, textViewResourceId, objects);
	}

	public DepartureArrayAdapter(Context context, int textViewResourceId) {
		super(context, textViewResourceId);
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

		TimedTextSwitcher textSwitcher = (TimedTextSwitcher) view
				.findViewById(R.id.trainLengthText);
		initTextSwitcher(textSwitcher, R.layout.train_length_arrival_textview);

		final String arrivesAtDestinationPrefix = getContext().getString(
				R.string.arrives_at_destination);
		final String estimatedArrivalTimeText = departure
				.getEstimatedArrivalTimeText(getContext());
		if (!StringUtils.isBlank(estimatedArrivalTimeText)) {
			textSwitcher.setCurrentText(arrivesAtDestinationPrefix
					+ estimatedArrivalTimeText);
		} else {
			textSwitcher.setCurrentText(departure.getTrainLengthText());
		}
		textSwitcher.setTextProvider(new TextProvider() {
			@Override
			public String getText(long tickNumber) {
				if (tickNumber % 4 == 0) {
					return departure.getTrainLengthText();
				} else {
					final String estimatedArrivalTimeText = departure
							.getEstimatedArrivalTimeText(getContext());
					if (StringUtils.isBlank(estimatedArrivalTimeText)) {
						return "";
					} else {
						return arrivesAtDestinationPrefix
								+ estimatedArrivalTimeText;
					}
				}
			}
		});

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
							.getEstimatedDepartureTimeText(getContext());
				}
			}
		});

		if (departure.isBikeAllowed()) {
			((ImageView) view.findViewById(R.id.bikeIcon))
					.setVisibility(View.VISIBLE);
		} else {
			((ImageView) view.findViewById(R.id.bikeIcon))
					.setVisibility(View.INVISIBLE);
		}
		if (departure.getRequiresTransfer()) {
			((ImageView) view.findViewById(R.id.xferIcon))
					.setVisibility(View.VISIBLE);
		} else {
			((ImageView) view.findViewById(R.id.xferIcon))
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
