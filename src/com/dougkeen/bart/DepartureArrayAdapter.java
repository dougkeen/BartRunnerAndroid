package com.dougkeen.bart;

import java.util.List;

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

	private String currentViewSwitcherText;

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

		if (System.currentTimeMillis() % 6000 > 3000) {
			String trainLengthText = departure.getTrainLengthText();
			if (currentViewSwitcherText == null
					|| !currentViewSwitcherText.equals(trainLengthText)) {
				textSwitcher.setText(trainLengthText);
				currentViewSwitcherText = trainLengthText;
			}
		} else {
			String arrivalText = "Est. arrival "
					+ departure.getEstimatedArrivalTimeText(getContext());
			if (currentViewSwitcherText == null
					|| !currentViewSwitcherText.equals(arrivalText)) {
				textSwitcher.setText(arrivalText);
				currentViewSwitcherText = arrivalText;
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
					getContext(), android.R.anim.slide_in_left));
			textSwitcher.setOutAnimation(AnimationUtils.loadAnimation(
					getContext(), android.R.anim.slide_out_right));
		}
	}
}
