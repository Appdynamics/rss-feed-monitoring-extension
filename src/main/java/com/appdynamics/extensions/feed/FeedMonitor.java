package com.appdynamics.extensions.feed;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.feed.config.Configuration;
import com.appdynamics.extensions.feed.config.Subscription;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndPerson;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

/**
 * Monitors the given URL feeds (RSS or Atom) and reports the no of topics and authors
 * 
 * @author Florencio Sarmiento
 *
 */
public class FeedMonitor extends AManagedMonitor {
	
	public static final Logger LOGGER = Logger.getLogger("com.singularity.extensions.FeedMonitor");
	
	public static final String CONFIG_ARG = "config-file";
	
	public static final String DEFAULT_DELIMETER = "|";
	
	public static final String DEFAULT_METRIC_PREFIX = "Custom Metrics|RSS Feed|";
	
	private String metricPrefix;

	public FeedMonitor() {
		LOGGER.info(String.format("Using Monitor Version [%s]", getImplementationVersion()));
	}

	public TaskOutput execute(Map<String, String> args,
			TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
		
		LOGGER.info("Starting RSS Feed Monitoring task");
		debugLog("Args received were: " + args);
		
		if (args != null) {
			
			String configFilename = getConfigFilename(args.get(CONFIG_ARG));
			
			try {
				
				Configuration config = readConfig(configFilename);
				setMetricPrefix(config);
				
				for (Subscription subscription : config.getSubscriptions()) {
					processSubscription(subscription);
				}
				
				printCollectiveObservedCurrent(metricPrefix + Metrics.ACTIVE_SUBSCRIPTION.getDisplayName(), 
						BigInteger.valueOf(config.getSubscriptions().size()));
				
				return new TaskOutput("RSS Feed monitoring task successfully completed");
				
			} catch (FileNotFoundException ex) {
				LOGGER.error("Config file not found: " + configFilename, ex);
				
			} catch (Exception ex) {
				LOGGER.error("Unfortunately an issue has occurred: ", ex);
			}
		}
		
		throw new TaskExecutionException("RSS Feed monitoring task completed with failures.");
		
	}
	
    private Configuration readConfig(String configFilename) throws FileNotFoundException {
    	LOGGER.info("Reading config file: " + configFilename);
        Yaml yaml = new Yaml(new Constructor(Configuration.class));
        Configuration config = (Configuration) yaml.load(new FileInputStream(configFilename));
        return config;
    }
    
    private String getConfigFilename(String filename) {
        if(StringUtils.isBlank(filename)){
            return "";
        }
        
        //for absolute paths
        if(new File(filename).exists()){
            return filename;
        }
        
        //for relative paths
        File jarPath = PathResolver.resolveDirectory(AManagedMonitor.class);
        String configFileName = String.format("%s%s%s", jarPath, File.separator, filename);
        return configFileName;
    }
	
	private void processSubscription(Subscription subscription) {
		debugLog(String.format("Processing subscription [%s]", subscription.getUrl()));
		
		try {
			long lastHourInMillis = getLastHourInMillis();
			URL feedUrl = new URL(subscription.getUrl());
			SyndFeedInput input = new SyndFeedInput();
	        SyndFeed feed = input.build(new XmlReader(feedUrl));
	        
	        Set<String> authors = new HashSet<String>();
	        
	        List<SyndEntry> topics = (List<SyndEntry>) feed.getEntries();
	        long topicInLastHour = 0;
	        
			for (SyndEntry topic : topics) {
				collectAllAuthors(topic, authors);
				
				if (isTopicInTheLastHour(lastHourInMillis, topic)) {
					topicInLastHour++;
				}
			}
			
			Map<Metrics, BigInteger> feedMetrics = new HashMap<Metrics, BigInteger>();
			feedMetrics.put(Metrics.TOTAL_TOPICS, BigInteger.valueOf(topics.size()));
			feedMetrics.put(Metrics.TOPICS_IN_LAST_HOUR, BigInteger.valueOf(topicInLastHour));
			feedMetrics.put(Metrics.AUTHORS_COUNT, BigInteger.valueOf(authors.size()));
			
			uploadMetrics(subscription, feedMetrics);
			
		} catch (Exception ex) {
			LOGGER.error(String.format(
					"Unfortunately an error occurred while processing the subscription [%s]", 
					subscription.getUrl()),
					ex);
		}
	}
	
	private void setMetricPrefix(Configuration config) {
		metricPrefix = config.getMetricPrefix();
		
		if (StringUtils.isBlank(metricPrefix)) {
			metricPrefix = DEFAULT_METRIC_PREFIX;
			
		} else {
			metricPrefix = metricPrefix.trim();
			
			if (!metricPrefix.endsWith(DEFAULT_DELIMETER)) {
				metricPrefix = metricPrefix + DEFAULT_DELIMETER;
			}
		}
	}
	
	private void collectAllAuthors(SyndEntry topic, Set<String> authors) {
		String name = topic.getAuthor();
		
		if (StringUtils.isNotBlank(name)) {
			authors.add(name.trim().toUpperCase());
		}
		
		List<SyndPerson> tmpAuthors = topic.getAuthors();
		
		if (tmpAuthors != null) {
			 for (SyndPerson author : (List<SyndPerson>) tmpAuthors) {
				 
				 name = author != null ? author.getName() : null;
				 
				 if (StringUtils.isNotBlank(name)) {
					 authors.add(name.trim().toUpperCase());
				 }
			 }
		 }
	}
	
	private boolean isTopicInTheLastHour(long lastHourInMillis, SyndEntry topic) {
		Date topicDate = topic.getPublishedDate() != null ? 
				topic.getPublishedDate() : topic.getUpdatedDate();
		
		return topicDate != null &&
				topicDate.getTime() >= lastHourInMillis;
	}
	
	private long getLastHourInMillis() {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.HOUR, -1);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTimeInMillis();
	}
	
	private void uploadMetrics(Subscription subscription, Map<Metrics, BigInteger> feedMetrics) throws Exception {
		String baseMetricPrefix =  String.format("%s%s%s", metricPrefix, 
				getDisplayName(subscription), DEFAULT_DELIMETER);
		
		for (Map.Entry<Metrics, BigInteger> metric : feedMetrics.entrySet()) {
			printCollectiveObservedCurrent(baseMetricPrefix + metric.getKey().getDisplayName(), metric.getValue());
		}
	}
	
	private String getDisplayName(Subscription subscription) throws Exception {
		String displayName = subscription.getDisplayName();
		
		if (StringUtils.isBlank(displayName)) {
			displayName = new URL(subscription.getUrl()).getHost();
		}
		
		return displayName;
	}
	
    private void printCollectiveObservedCurrent(String metricName, BigInteger metricValue) {
        printMetric(metricName, metricValue,
                MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
                MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
        );
    }
    
    private void printMetric(String metricName, BigInteger metricValue, String aggregation, String timeRollup, String cluster) {
		MetricWriter metricWriter = getMetricWriter(metricName, aggregation,
				timeRollup, cluster);
        
        String value = metricValue != null ? metricValue.toString() : BigInteger.ZERO.toString();
        
        debugLog(String.format("Sending [%s/%s/%s] metric = %s = %s",
            		aggregation, timeRollup, cluster,
                    metricName, value));
        
        metricWriter.printMetric(value);
    }
    
	private void debugLog(String msg) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(msg);
		}
	}
	
    public static String getImplementationVersion(){
        return FeedMonitor.class.getPackage().getImplementationTitle();
    }

}
