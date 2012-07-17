package com.dougkeen.bart.controls;

import org.apache.commons.lang3.StringUtils;

import com.dougkeen.bart.model.TextProvider;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextSwitcher;

public class TimedTextSwitcher extends TextSwitcher implements
		Ticker.TickSubscriber {

	public TimedTextSwitcher(Context context, AttributeSet attrs) {
		super(context, attrs);
		setInstanceVarsFromAttrs(attrs);
	}

	public TimedTextSwitcher(Context context) {
		super(context);
	}

	private void setInstanceVarsFromAttrs(AttributeSet attrs) {
		int tickInterval = attrs.getAttributeIntValue(
				"http://schemas.android.com/apk/res/com.dougkeen.bart",
				"tickInterval", 0);
		if (tickInterval > 0) {
			setTickInterval(tickInterval);
		}
	}

	private int mTickInterval;
	private TextProvider[] mTextProviderArray;
	private int mCurrentIndex = 0;

	@Override
	public int getTickInterval() {
		return mTickInterval;
	}

	public void setTickInterval(int tickInterval) {
		this.mTickInterval = tickInterval;
	}

	public void setTextProviders(TextProvider[] textProviders) {
		mTextProviderArray = textProviders;
		Ticker.getInstance().addSubscriber(this);
	}

	private String mLastText;

	@Override
	public void onTick() {
		String text = mTextProviderArray[mCurrentIndex].getText();
		if (StringUtils.isNotBlank(text)
				&& !StringUtils.equalsIgnoreCase(text, mLastText)) {
			mLastText = text;
			setText(text);
		}
		mCurrentIndex = (mCurrentIndex + 1) % mTextProviderArray.length;
	}

}
