# SignalFx AppDynamics client agent

This repository contains agent and libraries for retrieving and reporting AppDynamics metrics
to SignalFx. You will need AppDynamics username/password and host information as well as 
SignalFx account and organization API token to report the data.

## Supported Languages

* Java 7+

## Sending metrics

appd-report-standalone module is a standalone process that parses configurations and report
AppDynamics metric every specified intervals.

To run
```
maven install
cd appd-report-standalone
maven exec:java
```

### Configurations

#### Environment Variables

Required
```
APPD_USERNAME=<AppDynamics Username>
APPD_PASSWORD=<AppDynamics Password>
APPD_HOST=<https://AppDynamics Host>
SIGNALFX_TOKEN=<SignalFx token>
```

Optional
```
SIGNALFX_APPD_METRICS=<metric configurations filename (default to metrics.json)>
APPD_INTERVAL=<time in minutes of metric lookup interval (default to 1 minute)>
```

#### Metrics.json

Metrics.json contains configurations for list of apps, metrics inside each app and
its dimensions mapping.

AppDynamics metric path is described as pipe(|) separated token
e.g. Performance|AppServer1|Resources|CPU.

The given metrics is reported to SignalFx with last the last token being metric name and the rest
of the token mapped to dimensions with dimensionsPathMap.

Certain token can be ignored by specifying - (dash) in dimensions path map.

e.g. MetricPath = Performance|AppServer1|Resources|CPU
     DimensionsPathMap = category|host|-|resource_type

     would be mapped to
     {
        metric_name : "CPU"
        dimensions {
             category: "Performance",
             host: "AppServer1",
             resource_type: "CPU"
         }
      }
      
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

appd-report-standalone also reports metrics pertain to the syncing process to SignalFx.

That includes:
- mtsReported
- mtsEmpty
- dataPointsReported
- appdRequestFailure
