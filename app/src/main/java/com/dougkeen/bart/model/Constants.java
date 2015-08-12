package com.dougkeen.bart.model;

import android.net.Uri;

public class Constants {
    public static final String AUTHORITY = "com.dougkeen.bart.dataprovider";
    public static final String FAVORITE_CONTENT_TYPE = "vnd.android.cursor.dir/com.dougkeen.bart.favorite";
    public static final String ARBITRARY_ROUTE_UNDEFINED_TYPE = "vnd.android.cursor.dir/com.dougkeen.bart.arbitraryroute";
    public static final String ARBITRARY_ROUTE_TYPE = "vnd.android.cursor.item/com.dougkeen.bart.arbitraryroute";
    public static final String FAVORITE_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/com.dougkeen.bart.favorite";
    public static final Uri FAVORITE_CONTENT_URI = Uri.parse("content://"
            + AUTHORITY + "/favorites");
    public static final Uri ARBITRARY_ROUTE_CONTENT_URI_ROOT = Uri
            .parse("content://" + AUTHORITY + "/route");
    public static final String MAP_URL = "http://m.bart.gov/images/global/system-map29.gif";

    public static final String TAG = "com.dougkeen.BartRunner";
    public static final String API_KEY = "5LD9-IAYI-TRAT-MHHW";
    public static final String ACTION_ALARM = "com.dougkeen.action.ALARM";
    public static final String STATION_PAIR_EXTRA = "StationPair";
}
