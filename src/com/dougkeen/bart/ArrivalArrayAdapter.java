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

import com.dougkeen.bart.data.Arrival;

public class ArrivalArrayAdapter extends ArrayAdapter<Arrival> {

	public ArrivalArrayAdapter(Context context, int textViewResourceId,
			Arrival[] objects) {
		super(context, textViewResourceId, objects);
	}

	public ArrivalArrayAdapter(Context context, int resource,
			int textViewResourceId, Arrival[] objects) {
		super(context, resource, textViewResourceId, objects);
	}

	public ArrivalArrayAdapter(Context context, int resource,
			int textViewResourceId, List<Arrival> objects) {
		super(context, resource, textViewResourceId, objects);
	}

	public ArrivalArrayAdapter(Context context, int resource,
			int textViewResourceId) {
		super(context, resource, textViewResourceId);
	}

	public ArrivalArrayAdapter(Context context, int textViewResourceId,
			List<Arrival> objects) {
		super(context, textViewResourceId, objects);
	}

	public ArrivalArrayAdapter(Context context, int textViewResourceId) {
		super(context, textViewResourceId);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view;
		if (convertView != null && convertView instanceof RelativeLayout) {
			view = convertView;
		} else {
			LayoutInflater inflater = LayoutInflater.from(getContext());
			view = inflater.inflate(R.layout.arrival_listing, parent, false);
		}

		Arrival arrival = getItem(position);
		((TextView) view.findViewById(R.id.destinationText)).setText(arrival
				.getDestination().toString());
		((TextView) view.findViewById(R.id.trainLengthText)).setText(arrival
				.getTrainLengthText());
		ImageView colorBar = (ImageView) view
				.findViewById(R.id.destinationColorBar);
		((GradientDrawable) colorBar.getDrawable()).setColor(Color
				.parseColor(arrival.getDestinationColor()));
		((TextView) view.findViewById(R.id.countdown)).setText(arrival
				.getCountdownText());
		((TextView) view.findViewById(R.id.uncertainty)).setText(arrival
				.getUncertaintyText());
		if (arrival.isBikeAllowed()) {
			((ImageView) view.findViewById(R.id.bikeIcon))
					.setVisibility(View.VISIBLE);
		} else {
			((ImageView) view.findViewById(R.id.bikeIcon))
					.setVisibility(View.INVISIBLE);
		}
		if (arrival.getRequiresTransfer()) {
			((ImageView) view.findViewById(R.id.xferIcon))
					.setVisibility(View.VISIBLE);
		} else {
			((ImageView) view.findViewById(R.id.xferIcon))
					.setVisibility(View.INVISIBLE);
		}

		return view;
	}

}
