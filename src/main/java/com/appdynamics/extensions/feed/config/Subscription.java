package com.appdynamics.extensions.feed.config;

import org.apache.commons.lang.StringUtils;

public class Subscription {

	private String url;

	private String displayName;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		if (StringUtils.isNotBlank(url)) {
			if (url.startsWith("feed:")) {
				this.url = url.replaceFirst("feed", "http");
				
			} else if (!url.startsWith("http")) {
				this.url = "http://" + url;
				
			} else {
				this.url = url;
			}
			
		} else {
			this.url = null;
		}
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		if (StringUtils.isNotBlank(displayName)) {
			this.displayName = displayName.trim();
			
		} else {
			this.displayName = null;
		}
	}

}
