package com.dougkeen.bart.controls;

import android.content.Context;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.AttributeSet;
import android.widget.Checkable;

import com.dougkeen.bart.R;

/**
 * A {@link android.widget.LinearLayout} that implements {@link Checkable} and changes
 * its background color when checked.
 */
public class CheckableLinearLayout extends LinearLayoutCompat implements Checkable {

    private boolean mChecked;

    public CheckableLinearLayout(Context context) {
        super(context);
    }

    public CheckableLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CheckableLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void setChecked(boolean checked) {
        mChecked = checked;
        int colorRes = isChecked() ? R.color.blue_selection : android.R.color.transparent;
        setBackgroundResource(colorRes);
    }

    @Override
    public void toggle() {
        setChecked(!isChecked());
    }
}
