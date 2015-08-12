package com.dougkeen.bart.controls;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.Checkable;
import android.widget.RelativeLayout;

import com.dougkeen.bart.R;

public class DepartureListItemLayout extends RelativeLayout implements
        Checkable {

    public DepartureListItemLayout(Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.departure_listing, this,
                true);
    }

    private boolean mChecked;

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void setChecked(boolean checked) {
        mChecked = checked;
        if (isChecked()) {
            setBackgroundDrawable(getContext().getResources().getDrawable(
                    R.color.blue_selection));
        } else {
            setBackgroundDrawable(getContext().getResources().getDrawable(
                    android.R.color.transparent));
        }
    }

    @Override
    public void toggle() {
        setChecked(!isChecked());
    }
}
