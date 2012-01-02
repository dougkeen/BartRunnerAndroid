package com.dougkeen.bart.data;

public enum RoutesColumns {
	_ID("_id", "INTEGER", false),
	FROM_STATION("FROM_STATION", "TEXT", false),
	TO_STATION("TO_STATION", "TEXT", false),
	FARE("FARE", "TEXT", true),
	FARE_LAST_UPDATED("FARE_LAST_UPDATED", "INTEGER", true);

	
	// This class cannot be instantiated
	private RoutesColumns(String string, String type, Boolean nullable) {
		this.string = string;
		this.sqliteType = type;
		this.nullable = nullable;
	}

	public final String string;
	public final String sqliteType;
	public final Boolean nullable;

	protected String getColumnDef() {
		return string + " " + sqliteType + (nullable?"":" NOT NULL");
	}
}
