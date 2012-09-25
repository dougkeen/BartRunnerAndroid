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
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher.ViewFactory;

import com.dougkeen.bart.R;
import com.dougkeen.bart.controls.CountdownTextView;
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
		if (convertView != null && convertView instanceof RelativeLayout) {
			view = convertView;
		} else {
			LayoutInflater inflater = LayoutInflater.from(getContext());
			view = inflater.inflate(R.layout.departure_listing, parent, false);
		}

		final Departure departure = getItem(position);
		((TextView) view.findViewById(R.id.destinationText)).setText(departure
				.getTrainDestination().toString());

		TimedTextSwitcher textSwitcher = (TimedTextSwitcher) view
				.findViewById(R.id.trainLengthText);
		initTextSwitcher(textSwitcher);

		final String estimatedArrivalTimeText = departure
				.getEstimatedArrivalTimeText(getContext());
		if (!StringUtils.isBlank(estimatedArrivalTimeText)) {
			textSwitcher.setCurrentText("Est. arrival "
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
						return "Est. arrival " + estimatedArrivalTimeText;
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
		((TextView) view.findViewById(R.id.uncertainty)).setText(departure
				.getUncertaintyText());
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

	private void initTextSwitcher(TextSwitcher textSwitcher) {
		if (textSwitcher.getInAnimation() == null) {
			textSwitcher.setFactory(new ViewFactory() {
				public View makeView() {
					return LayoutInflater.from(getContext()).inflate(
							R.layout.train_length_arrival_textview, null);
				}
			});

			textSwitcher.setInAnimation(AnimationUtils.loadAnimation(
					getContext(), android.R.anim.slide_in_left));
			textSwitcher.setOutAnimation(AnimationUtils.loadAnimation(
					getContext(), android.R.anim.slide_out_right));
		}
	}
}
