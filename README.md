Agent tool for submitting AppDynamics metrics to SignalFx.

## Configurations

### config.properties

```
appd.username=<AppDynamics Username>
appd.password=<AppDynamics Password>
appd.host=<https://AppDynamics Host>
signalfx.token=<SignalFx token>
interval=<Sync interval in minutes>
```

### metrics.json

```
[
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
```

metric_name and dimensions are optional.
