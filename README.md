# RSS/Atom Feed Monitoring Extension  

##Use Case

Use for monitoring RSS and Atom feeds to report:

- total no of topics
- no of topics in the last hour
- no of authors
- subscriptions count

This extension only works with standalone machine agent. 

**Note : By default, the Machine agent and AppServer agent can only send a fixed number of metrics to the controller. To change this limit, please follow the instructions mentioned [here](http://docs.appdynamics.com/display/PRO14S/Metrics+Limits).**

##Installation
1. Run 'mvn clean install' from rss-feed-monitoring-extension directory
2. Copy and unzip RSSFeedMonitor.zip from 'target' directory into \<machine_agent_dir\>/monitors/
3. Edit config.yaml file and provide at least one feed subscription details, i.e.
	- url
	- displayName

4. Restart the Machine Agent.

## config.yaml
**Note: Please avoid using tab (\t) when editing yaml files. You may want to validate the yaml file using a [yaml validator](http://yamllint.com/).**

| Param | Description |
| ----- | ----- |
| url | The full feed url, e.g. http://www.engadget.com/rss.xml  |
| displayName | The display name for this feed. If not specified, domain name is used by default.|
| metricPrefix | The path prefix for viewing metrics in the metric browser. Default is "Custom Metrics|RSS Feed|"|

**To monitor multiple feeds, add a new set of url and displayName, preceded by - (dash) for each feed, see example below:**

~~~~
subscriptions:
  - url: "http://www.engadget.com/rss.xml"
    displayName: Engadget
  - url: "http://hayobethlehem.nl/feed/atom/"
    displayName: Hayobethlehem

metricPrefix:  "Custom Metrics|RSS Feed|"
~~~~

##Metric Path

Application Infrastructure Performance|\<Tier\>|Custom Metrics|RSS Feed|\<Feed Name\>|Total Topics

Application Infrastructure Performance|\<Tier\>|Custom Metrics|RSS Feed|\<Feed Name\>|Topics (Last Hour)

Application Infrastructure Performance|\<Tier\>|Custom Metrics|RSS Feed|\<Feed Name\>|Authors Count

Application Infrastructure Performance|\<Tier\>|Custom Metrics|RSS Feed|Active Subscription

##Contributing

Always feel free to fork and contribute any changes directly here on GitHub

##Community



##Support

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:ace-request@appdynamics.com).

