# SignalFx AppDynamics client agent [![Build Status](https://travis-ci.org/signalfx/appd-integration.svg)](https://travis-ci.org/signalfx/appd-integration)

This repository contains agent and libraries for retrieving and reporting AppDynamics metrics
to SignalFx. You will need AppDynamics username/password and host information as well as 
SignalFx account and organization API token to report the data.

## Supported Languages

* Java 7+

## Sending metrics

appd-report-standalone module is a standalone process that parses configurations and report
AppDynamics metric every specified intervals. If your environment utilizes a HTTP/HTTPS
proxy then Maven needs to be configured to use it otherwise dependancies may fail to
install. Please click [https://maven.apache.org/guides/mini/guide-proxies.html](here) to
go to the Maven mini-guide for configuring a proxy.

To run:
```
$ git clone https://github.com/signalfx/appd-integration
$ cd appd-integration
$ mvn install
$ cd appd-report-standalone
```
Then, if using environmental variables:
```
$ mvn exec:java
```
Othewrise, if using system properties:
```
$ mvn exec:java -Dcom.signalfx.appd.username=<AppDynamics Username> -Dcom.signalfx.appd.password=<AppDynamics Password> -Dcom.signalfx.appd.host=<https://AppDynamics Host> -Dcom.signalfx.api.token=<SignalFx API Token>
```

### Configurations

#### Configuration Variables

| Environmental Variable | System Property            | Required | Description                                                     |
|------------------------|----------------------------|----------|-----------------------------------------------------------------|
| APPD_USERNAME          | com.signalfx.appd.username | &#x2713;        | AppDynamics Username                                            |
| APPD_PASSWORD          | com.signalfx.appd.password | &#x2713;        | AppDynamics Password                                            |
| APPD_HOST              | com.signalfx.appd.host     | &#x2713;        | AppDynamics Host                                                |
| SIGNALFX_TOKEN         | com.signalfx.api.token     | &#x2713;        | SignalFx API Token                                              |
| SIGNALFX_APPD_METRICS  | com.signalfx.appd.metrics  |          | Metric configurations filename (default to metrics.json)        |
| APPD_INTERVAL          | com.signalfx.appd.interval |          | Time in minutes of metric lookup interval (default to 1 minute) |


#### Metrics.json

Metrics.json contains configurations for list of apps, metrics inside each app and
its dimensions mapping.

AppDynamics metric paths are described as a pipe-delimited string (|),
for example Performance|AppServer1|Resources|CPU.

Each metric is reported to SignalFx with the last element of this path as the metric name,
and each previous element mapped to a dimension according to the dimensionsPathMap.

Elements can be ignored by specifying the target dimension as - (dash) in the dimensionsPathMap.

Wild cards (asterisk *) can be used to specify that all matching AppDynamics metrics are
to be collected. Mapping to dimensions through the dimensionsPathMap will still happen on
the actual value of that metric path element.

Example with ignoring element:

```
MetricPath = Performance|AppServer1|Resources|CPU
DimensionsPathMap = category|host|-

would be mapped to
{
    metric_name : "CPU"
    dimensions {
        category: "Performance",
        host: "AppServer1"
    }
}
```

Example with wildcard:
```
MetricPath = Performance|*|Resources|CPU
DimensionsPathMap = category|host|-

If the entity at the second level matches both 'Server1' and 'Server2'. We would get 2 metric
time series as
{
    metric_name : "CPU"
    dimensions {
        category: "Performance",
        host: "Server1"
    }
},
{
    metric_name : "CPU"
    dimensions {
        category: "Performance",
        host: "Server2"
    }
}
```

      
Optional extra dimensions can also be specified for each metric paths.

Following is a working example of metrics.json configurations
```
[
  {
    "name": "<Your App Name>",
    "metrics": [
      {
        "metric_path": "Application Infrastructure Performance|Tier2|Individual Nodes|*|Hardware Resources|*|*",
        "dimensions_path_map": "metric_type|tier|-|node|resource_type|component_type",
        "dimensions": {
          "key1": "value1",
          "key2": "value2"
        }
      },
      {
        "metric_path": "Application Infrastructure Performance|Tier2|Individual Nodes|*|Hardware Resources|*|*|*",
        "dimensions_path_map": "metric_type|tier|-|node|resource_type|component_type|component_instance"
      },
      {
        "metric_path": "Application Infrastructure Performance|Tier2|Individual Nodes|*|Agent|*|*",
        "dimensions_path_map": "metric_type|tier|-|node|-|category",
        "dimensions": {
          "key3": "value3"
        }
      }
    ]
  }
]
```

Default metrics.json is provided with Application Infrastructure Performance metrics configured.


### Process Status Metrics

appd-report-standalone also reports metrics pertaining to the syncing process to SignalFx.

That includes:
- mtsReported
- mtsEmpty
- dataPointsReported
- appdRequestFailure
