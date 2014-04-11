#!/bin/bash

sites="rennes"
version=""

for site in $sites
do
  scp target/uber-btrPlaceConsolidation-0.0.1-SNAPSHOT.jar $site:public/snooze/experimental/btrPlaceConsolidation.jar
done

