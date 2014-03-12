/**
 * Copyright (C) 2010-2013 Eugen Feller, INRIA <eugen.feller@inria.fr>
 *
 * This file is part of Snooze, a scalable, autonomic, and
 * energy-aware virtual machine (VM) management framework.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */
package org.inria.myriads.snooze.plugins;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.groupmanager.anomaly.resolver.api.AnomalyResolver;
import org.inria.myriads.snoozenode.localcontroller.monitoring.enums.LocalControllerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Anomaly resolver.
 * 
 * @author Matthieu simonin
 */
public final class AlwaysAnomalyRejected extends AnomalyResolver 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(AlwaysAnomalyRejected.class);

    /**
     * Constructor.
     */
    public AlwaysAnomalyRejected()
    {
        
    }
    
    @Override
    public void initialize() 
    {
      
    }
    
    /**
     * Called to resolve anomaly.
     * 
     * @param localControllerId     The anomaly local controller identifier
     * @param anomaly                 The local controller state
     * @throws Exception            The exception
     */
    public synchronized void resolveAnomaly(LocalControllerDescription anomalyLocalController, Object anomalyObject)
        throws Exception
    {
        Guard.check(anomalyLocalController, anomalyObject);
        log_.debug("Starting anomaly resolution");
              
        // Cast here
        LocalControllerState anomaly = (LocalControllerState) anomalyObject;
        log_.debug("The anomaly is " + String.valueOf(anomaly));
        
        stateMachine_.onAnomalyResolved();
        
    }
    
    @Override
    public synchronized boolean readyToResolve(String anomalyLocalControllerId, Object anomalyObject)
    {
        return true;
    }
   

}
