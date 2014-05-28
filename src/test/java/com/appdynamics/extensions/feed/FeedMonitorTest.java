package com.appdynamics.extensions.feed;

import static com.appdynamics.extensions.feed.Metrics.ACTIVE_SUBSCRIPTION;
import static com.appdynamics.extensions.feed.Metrics.AUTHORS_COUNT;
import static com.appdynamics.extensions.feed.Metrics.TOPICS_IN_LAST_HOUR;
import static com.appdynamics.extensions.feed.Metrics.TOTAL_TOPICS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyPrivate;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.io.File;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.appdynamics.extensions.PathResolver;
import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({FeedMonitor.class, PathResolver.class})
@PowerMockIgnore({"org.apache.*, javax.xml.*"})
public class FeedMonitorTest {
	
	@Mock
	private MetricWriter mockMetricWriter;
	
	private FeedMonitor classUnderTest;
	
	@Before
	public void setUp() throws Exception {
		classUnderTest = spy(new FeedMonitor());
		mockStatic(PathResolver.class);
		when(PathResolver.resolveDirectory(AManagedMonitor.class)).thenReturn(new File("./target"));
		whenNew(MetricWriter.class).withArguments(any(AManagedMonitor.class), anyString()).thenReturn(mockMetricWriter);
	}
	
	@Test(expected=TaskExecutionException.class)
	public void testWithNoArgs() throws TaskExecutionException {
		classUnderTest.execute(null, null);
	}
	
	@Test(expected=TaskExecutionException.class)
	public void testWithEmptyArgs() throws TaskExecutionException {
		classUnderTest.execute(new HashMap<String, String>(), null);
	}
	
	@Test(expected=TaskExecutionException.class)
	public void testWithNonExistentConfigFile() throws TaskExecutionException {
		Map<String, String> args = Maps.newHashMap();
		args.put("config-file","src/test/resources/conf/no_config.yaml");
		
		classUnderTest.execute(args, null);
	}
	
	@Test(expected=TaskExecutionException.class)
	public void testWithInvalidConfigFile() throws TaskExecutionException {
		Map<String, String> args = Maps.newHashMap();
		args.put("config-file","src/test/resources/conf/invalid_config.yaml");
		
		classUnderTest.execute(args, null);		
	}
	
	@Test
	public void testWithRSSFeed() throws Exception {
		String testRssPath = setUpTestRSSFeed();
		
		URL testURL = new URL("file://" + testRssPath);
		whenNew(URL.class).withArguments(anyString()).thenReturn(testURL);
		
		Map<String, String> args = Maps.newHashMap();
		args.put("config-file","src/test/resources/conf/config-test-rss.yaml");
		
		classUnderTest.execute(args, null);
		
		verifyMetric("Custom Metrics|RSS Feed|TestRSS|" + AUTHORS_COUNT.getDisplayName(), 5);
		verifyMetric("Custom Metrics|RSS Feed|TestRSS|" + TOTAL_TOPICS.getDisplayName(), 6);
		verifyMetric("Custom Metrics|RSS Feed|TestRSS|" + TOPICS_IN_LAST_HOUR.getDisplayName(), 3);
		verifyMetric("Custom Metrics|RSS Feed|" + ACTIVE_SUBSCRIPTION.getDisplayName(), 1);
	}
	
	@Test
	public void testWithAtomFeed() throws Exception {
		String testAtomPath = setUpTestAtomFeed();
		
		URL testURL = new URL("file://" + testAtomPath);
		whenNew(URL.class).withArguments(anyString()).thenReturn(testURL);
		
		Map<String, String> args = Maps.newHashMap();
		args.put("config-file","src/test/resources/conf/config-test-atom.yaml");
		
		classUnderTest.execute(args, null);
		
		verifyMetric("Custom Metrics|RSS Feed|TestAtom|" + AUTHORS_COUNT.getDisplayName(), 2);
		verifyMetric("Custom Metrics|RSS Feed|TestAtom|" + TOTAL_TOPICS.getDisplayName(), 10);
		verifyMetric("Custom Metrics|RSS Feed|TestAtom|" + TOPICS_IN_LAST_HOUR.getDisplayName(), 5);
		verifyMetric("Custom Metrics|RSS Feed|" + ACTIVE_SUBSCRIPTION.getDisplayName(), 1);		
	}
	
	@Test
	public void testWithMultipleFeeds() throws Exception {
		String testRssPath = setUpTestRSSFeed();
		URL testRssUrl = new URL("file://" + testRssPath);
		
		String testAtomPath = setUpTestAtomFeed();
		URL testAtomUrl = new URL("file://" + testAtomPath);
		
		whenNew(URL.class).withArguments(anyString()).thenReturn(testRssUrl, testAtomUrl);
		
		Map<String, String> args = Maps.newHashMap();
		args.put("config-file","src/test/resources/conf/config.yaml");
		
		classUnderTest.execute(args, null);
		
		verifyMetric("Custom Metrics|RSS Feed|TestRSS|" + AUTHORS_COUNT.getDisplayName(), 5);
		verifyMetric("Custom Metrics|RSS Feed|TestRSS|" + TOTAL_TOPICS.getDisplayName(), 6);
		verifyMetric("Custom Metrics|RSS Feed|TestRSS|" + TOPICS_IN_LAST_HOUR.getDisplayName(), 3);
		
		verifyMetric("Custom Metrics|RSS Feed|TestAtom|" + AUTHORS_COUNT.getDisplayName(), 2);
		verifyMetric("Custom Metrics|RSS Feed|TestAtom|" + TOTAL_TOPICS.getDisplayName(), 10);
		verifyMetric("Custom Metrics|RSS Feed|TestAtom|" + TOPICS_IN_LAST_HOUR.getDisplayName(), 5);		
		
		verifyMetric("Custom Metrics|RSS Feed|" + ACTIVE_SUBSCRIPTION.getDisplayName(), 2);
	}
	
	private String setUpTestRSSFeed() throws Exception {
		// simulating new rss content every half an hour
		String rssDateFormat = "EEE, dd MMM yyyy HH:mm:ss zzz";
		Object[] testPublishDates = {
				generateTimestamp(rssDateFormat, 0),
				generateTimestamp(rssDateFormat, 30),
				generateTimestamp(rssDateFormat, 60),
				generateTimestamp(rssDateFormat, 90),
				generateTimestamp(rssDateFormat, 120),
				generateTimestamp(rssDateFormat, 180)
		};
		
		String unformattedRssPath = "/test-rss.xml";
		String formattedRssPath = new File("./target/formatted-test-rss.xml").getAbsolutePath();
		generateFormattedTestFeed(unformattedRssPath, formattedRssPath, testPublishDates);
		return formattedRssPath;
	}
	
	private String setUpTestAtomFeed() throws Exception {
		// simulating new atom content every 15 mins
		String atomDateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSz";
		Object[] testPublishDates = {
				generateTimestamp(atomDateFormat, 0),
				generateTimestamp(atomDateFormat, 15),
				generateTimestamp(atomDateFormat, 30),
				generateTimestamp(atomDateFormat, 45),
				generateTimestamp(atomDateFormat, 60),
				generateTimestamp(atomDateFormat, 75),
				generateTimestamp(atomDateFormat, 90),
				generateTimestamp(atomDateFormat, 105),
				generateTimestamp(atomDateFormat, 120),
				generateTimestamp(atomDateFormat, 135)
		};
		
		String unformattedAtomPath = "/test-atom.xml";
		String formattedAtomPath = new File("./target/formatted-test-atom.xml").getAbsolutePath();
		generateFormattedTestFeed(unformattedAtomPath, formattedAtomPath, testPublishDates);
		return formattedAtomPath;
	}
	
	private void verifyMetric(String metricName, long value) throws Exception {
		verifyPrivate(classUnderTest).invoke("printCollectiveObservedCurrent", 
				metricName, BigInteger.valueOf(value));
	}
	
	private void generateFormattedTestFeed(String originalFilePath, String newFilePath, Object[] testPublishDates) throws Exception {
		String unformattedContent = getUnformattedFeedContent(originalFilePath);
		String formattedContent = formatWithPublishDates(unformattedContent, testPublishDates);
		writeFormattedFeedContent(newFilePath, formattedContent);
	}
	
	private String getUnformattedFeedContent(String originalTestFilepath) throws Exception {
		
		File file = new File(this.getClass().getResource(originalTestFilepath).getPath());
		
		RandomAccessFile randomAccessFile = null;
		StringBuilder content = new StringBuilder();
		
		try {
			randomAccessFile = new RandomAccessFile(file, "r");
			String currentLineToSearch = null;
			
			while((currentLineToSearch = randomAccessFile.readLine()) != null) {
				content.append(currentLineToSearch).append(System.getProperty("line.separator"));
			}
			
		} finally {
			if (randomAccessFile != null) {
				randomAccessFile.close();
			}
		}
		
		return content.toString();
	}
	
	private String formatWithPublishDates(String feed, Object[] dates) {
		return String.format(feed, dates);
	}
	
	private void writeFormattedFeedContent(String filepath, String feed) throws Exception {
    	File file = new File(filepath);
    	FileWriter fileWriter = null;
    	
    	try {
    		fileWriter = new FileWriter(file, false);
    		fileWriter.write(feed);
    		
    	} finally {
    		if (fileWriter != null) {
    			fileWriter.close();
    		}
    	}
	}
	
	private String generateTimestamp(String dateFormat, int noOfMinsToDeduct) {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MINUTE, -1 * noOfMinsToDeduct);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		
		DateFormat formatter = new SimpleDateFormat(dateFormat);
		return formatter.format(new Date(calendar.getTimeInMillis()));
	}
}
