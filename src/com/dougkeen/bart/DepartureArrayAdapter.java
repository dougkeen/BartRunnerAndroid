package com.dougkeen.bart;

import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dougkeen.bart.data.Departure;

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

		Departure departure = getItem(position);
		((TextView) view.findViewById(R.id.destinationText)).setText(departure
				.getDestination().toString());
		((TextView) view.findViewById(R.id.trainLengthText)).setText(departure
				.getTrainLengthText());
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

}
