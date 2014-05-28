package com.appdynamics.extensions.feed;

public enum Metrics {
	
	ACTIVE_SUBSCRIPTION("Active Subscription"),
	
	TOTAL_TOPICS("Total Topics"),
	
	TOPICS_IN_LAST_HOUR("Topics (Last Hour)"),
	
	AUTHORS_COUNT("Authors Count");

	private String displayName;
	
	private Metrics(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
