snooze-plugins
==============

Project to store the Snooze plugins. The plugin management will be available in Snooze 2.0.

How to use plugins
------------------

Plugins are *.jar* files. 
Generate it from source or download it from the snooze website and make it available from Snooze.
Update the configuration file of Snooze to point to the directory containing the plugin. 

Specific informations below.

## List of plugins


### RandomScheduling : 

This plugin is used by a groupmanager to randomly assign virtual machines to a local controller.


To use this plugin, update the configuration file as follow :

    ################## Group manager scheduler ###################
    groupManagerScheduler.pluginsDirectory = <directory containing your plugin>

    # Placement policy (FirstFit, RoundRobin, custom)
    groupManagerScheduler.placementPolicy = RandomScheduling

    

## Development

* Fork the repository
* Make your bug fixes or feature additions by following our coding conventions (see the [snoozecheckstyle](https://github.com/snoozesoftware/snoozecheckstyle) repository)
* Send a pull request


