package com.dougkeen.bart;

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

import com.dougkeen.bart.model.Departure;

public class DepartureArrayAdapter extends ArrayAdapter<Departure> {

	private int refreshCounter = 1;

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
	
	public void incrementRefreshCounter() {
		refreshCounter++;
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

		Departure departure = getItem(position);
		((TextView) view.findViewById(R.id.destinationText)).setText(departure
				.getDestination().toString());

		TextSwitcher textSwitcher = (TextSwitcher) view
				.findViewById(R.id.trainLengthText);
		initTextSwitcher(textSwitcher);

		final String estimatedArrivalTimeText = departure
				.getEstimatedArrivalTimeText(getContext());
		String arrivalText = "Est. arrival " + estimatedArrivalTimeText;
		if (StringUtils.isBlank(estimatedArrivalTimeText)) {
			textSwitcher.setCurrentText(departure.getTrainLengthText());
		} else if (refreshCounter % 6 < 3) {
			String trainLengthText = departure.getTrainLengthText();
			if (refreshCounter % 6 == 0) {
				textSwitcher.setText(trainLengthText);
			} else {
				textSwitcher.setCurrentText(trainLengthText);
			}
		} else {
			if (refreshCounter % 6 == 3) {
				textSwitcher.setText(arrivalText);
			} else {
				textSwitcher.setCurrentText(arrivalText);
			}
		}
		ImageView colorBar = (ImageView) view
				.findViewById(R.id.destinationColorBar);
		((GradientDrawable) colorBar.getDrawable()).setColor(Color
				.parseColor(departure.getDestinationColor()));
		((TextView) view.findViewById(R.id.countdown)).setText(departure
				.getCountdownText());
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
					getContext(), android.R.anim.fade_in));
		}
	}
}
