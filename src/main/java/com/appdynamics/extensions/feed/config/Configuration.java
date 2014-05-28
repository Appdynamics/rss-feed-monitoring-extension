package com.appdynamics.extensions.feed.config;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class Configuration {
	
	private String metricPrefix;
	
	private List<Subscription> subscriptions;

	public String getMetricPrefix() {
		return metricPrefix;
	}

	public void setMetricPrefix(String metricPrefix) {
		if (StringUtils.isNotBlank(metricPrefix)) {
			this.metricPrefix = metricPrefix.trim();
			
		} else {
			this.metricPrefix = null;
			
		}
	}

	public List<Subscription> getSubscriptions() {
		return subscriptions != null ? subscriptions : new ArrayList<Subscription>();
	}

	public void setSubscriptions(List<Subscription> subscriptions) {
		this.subscriptions = subscriptions;
	}

}
