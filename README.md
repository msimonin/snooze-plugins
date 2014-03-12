snooze-plugins
==============

Project to store the Snooze plugins. The plugin management will be available in Snooze 2.0.

How to use plugins
------------------

Plugins are *.jar* files. 
Generate it from source or download it from the snooze website and make it available from Snooze.
Basically you have to set up the `globals.pluginsDirectory` parameter in the snoozenode config file to indicate where to find the plugins.
Update the configuration file of Snooze to point to the directory containing the plugin. 

Specific informations below.

## List of plugins


### RandomScheduling : 

This plugin is used by a groupmanager to randomly assign virtual machines to a local controller.


To use this plugin, update the configuration file as follow :

```
# Placement policy (FirstFit, RoundRobin, custom)
groupManagerScheduler.placementPolicy = org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.impl.RandomScheduling
```
    

## Development

* Fork the repository
* Make your bug fixes or feature additions by following our coding conventions (see the [snoozecheckstyle](https://github.com/snoozesoftware/snoozecheckstyle) repository)
* Send a pull request


