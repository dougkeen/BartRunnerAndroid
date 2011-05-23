package com.dougkeen.bart.data;

public enum FavoritesColumns {
	_ID("_id", "INTEGER"),
	FROM_STATION("FROM_STATION", "TEXT"),
	TO_STATION("TO_STATION", "TEXT");

	// This class cannot be instantiated
	private FavoritesColumns(String string, String type) {
		this.string = string;
		this.sqliteType = type;
	}

	public final String string;
	public final String sqliteType;

	protected String getColumnDef() {
		return string + " " + sqliteType;
	}
}
