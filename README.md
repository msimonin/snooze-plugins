snooze-plugins
==============

Project to store the Snooze plugins. The plugin management will be available in Snooze 3.0.

How to use plugins
------------------

Plugins are *.jar* files. 
Generate it from source or download it from the snooze website and make it available from Snooze.
Basically you have to set up the `globals.pluginsDirectory` parameter in the snoozenode config file to indicate where to find the plugins.
Update the configuration file of Snooze to point to the directory containing the plugin. 

Specific informations below.

## List of plugins

### AlwaysAnomalyDetected : 

This plugin will detect an anomaly (probably fake) after each cycle of the detection loop.

To use this plugin, update the configuration file as follow :

```
########### Anomaly Detection ######################################
localController.anomaly.detector = org.inria.myriads.snoozenode.localcontroller.anomaly.detector.api.impl.AlwaysAnomalyDetector

# Enable or disable detection
localController.anomaly.detector.enable=true

# NumberOfMonitoringEntries to take into account for the estimation
localController.anomaly.detector.numberOfMonitoringEntries = 10
# loop detection interval
localController.anomaly.detector.interval = 10000
# extra options
localController.anomaly.detector.options = {}

```

###  AlwaysAnomalyResolved :

This plugin will resolve an anomaly (actually with a no operation ) after each detection.

To use this plugin, update the configuration file as follow :

```
########### Anomaly Resolution ######################################
groupManager.anomaly.resolver = org.inria.myriads.snooze.plugins.AlwaysAnomalyRejected
# NumberOfMonitoringEntries to take into account for the estimation
groupManager.anomaly.resolver.numberOfMonitoringEntries = 10
# extra options
groupManager.anomaly.resolver.options = {"overloadpolicy" : "org.inria.myriads.snoozenode.groupmanager.managerpolicies.relocation.api.impl.GreedyOverloadRelocation", "underloadpolicy": "org.inria.myriads.snoozenode.groupmanager.managerpolicies.relocation.api.impl.GreedyUnderloadRelocation"}
```


### libvirtHostMonitor

This plugin will introduce a host monitor wich will retrieve metrics from Virtual Machines running on the host.

To use this plugin, update the configuration file as follow :

```
# Producer declaration
localController.hostmonitor = [producer],libvirt

# libvirt hostmonitor
localController.hostmonitor.libvirt = org.inria.myriads.snooze.plugins.LibVirtVirtualMachineHostMonitor
localController.hostmonitor.libvirt.options = {"hostname": "localhost", "port": "8649"}
localController.hostmonitor.libvirt.published = VMS_CPU,[VMS_MEM,VMS_RX,VMS_TX]
localController.hostmonitor.libvirt.interval = 3000
localController.hostmonitor.libvirt.numberOfMonitoringEntries = 30
localController.hostmonitor.libvirt.estimator = average
localController.hostmonitor.ganglia.thresholds.cpu_user = 0,30,70
localController.hostmonitor.ganglia.thresholds.mem_free = 0,50,80
```
### RandomScheduling : 

This plugin is used by a groupmanager to randomly assign virtual machines to a local controller.


To use this plugin, update the configuration file as follow :

```
groupManagerScheduler.placementPolicy = org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.impl.RandomScheduling
```

## Development

* Fork the repository
* Make your bug fixes or feature additions by following our coding conventions (see the [snoozecheckstyle](https://github.com/snoozesoftware/snoozecheckstyle) repository)
* Send a pull request


