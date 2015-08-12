package com.dougkeen.bart.controls;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import com.dougkeen.bart.model.TextProvider;

public class CountdownTextView extends TextView implements
        Ticker.TickSubscriber {

    private TextProvider mTextProvider;
    private int mTickInterval;

    public CountdownTextView(Context context) {
        super(context);
    }

    public CountdownTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setInstanceVarsFromAttrs(attrs);
    }

    public CountdownTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setInstanceVarsFromAttrs(attrs);
    }

    private void setInstanceVarsFromAttrs(AttributeSet attrs) {
        int tickInterval = attrs.getAttributeIntValue(
                "http://schemas.android.com/apk/res/com.dougkeen.bart",
                "tickInterval", 0);
        if (tickInterval > 0) {
            setTickInterval(tickInterval);
        }
    }

    public void setTextProvider(TextProvider provider) {
        mTextProvider = provider;
        Ticker.getInstance().addSubscriber(this, getContext());
    }

    @Override
    public int getTickInterval() {
        return mTickInterval;
    }

    public void setTickInterval(int tickInterval) {
        this.mTickInterval = tickInterval;
    }

    @Override
    public void onTick(long tickNumber) {
        setText(mTextProvider.getText(tickNumber));
    }

}
