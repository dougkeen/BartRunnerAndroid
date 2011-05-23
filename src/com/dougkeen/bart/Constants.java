package com.dougkeen.bart;

import android.net.Uri;

public class Constants {
	public static final String AUTHORITY = "com.dougkeen.bart.dataprovider";
	public static final String FAVORITE_CONTENT_TYPE = "vnd.android.cursor.dir/com.dougkeen.bart.favorite";
	public static final String FAVORITE_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/com.dougkeen.bart.favorite";
	public static final Uri FAVORITE_CONTENT_URI = Uri.parse("content://"
			+ AUTHORITY + "/favorites");
}
