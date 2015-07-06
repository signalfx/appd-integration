Agent tool for submitting AppDynamics metrics to SignalFx.

## Configurations

### Set environment variables

```
APPD_USERNAME=<AppDynamics Username>
APPD_PASSWORD=<AppDynamics Password>
APPD_HOST=<https://AppDynamics Host>
SIGNALFX_TOKEN=<SignalFx token>
```

### metrics.json

```
{
    "interval": <Sync interval in minutes>,
    "apps":[
               {
                   "name": "App Name"
                   "metrics": [
                       {
                           "metric_path": "Metric Path for AppDynamics"
                       },
                       {
                           "metric_path: "Another Metric Path for AppDynamics"
                           "metric_name": "Optional SignalFX Metric Name, default to metric_path is empty"
                           "dimensions": {
                               "key": "value",
                               "key2": "value2"
                           }
                       }
                   ]
               }
           ]
}
```

metric_name and dimensions are optional.
