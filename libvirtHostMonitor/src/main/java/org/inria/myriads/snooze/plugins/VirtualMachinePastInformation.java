package org.inria.myriads.snooze.plugins;

import java.util.List;

import org.inria.myriads.snoozenode.localcontroller.monitoring.information.NetworkTrafficInformation;
import org.inria.myriads.snoozenode.localcontroller.monitoring.information.VirtualMachineInformation;

public class VirtualMachinePastInformation 
{
  
    private long previousCpuTime_; 
    
    private long previousSystemTime_;
    
    private double previousRxBytes_;
    
    private double previousTxBytes_;
    
    /**
     * @return the previousCpuTime
     */
    public long getPreviousCpuTime()
    {
        return previousCpuTime_;
    }

    /**
     * @param previousCpuTime the previousCpuTime to set
     */
    public void setPreviousCpuTime(long previousCpuTime)
    {
        previousCpuTime_ = previousCpuTime;
    }

    /**
     * @return the previousSystemTime
     */
    public long getPreviousSystemTime()
    {
        return previousSystemTime_;
    }

    /**
     * @param previousSystemTime the previousSystemTime to set
     */
    public void setPreviousSystemTime(long previousSystemTime)
    {
        previousSystemTime_ = previousSystemTime;
    }

    /**
     * @return the previousRxBytes
     */
    public double getPreviousRxBytes()
    {
        return previousRxBytes_;
    }

    /**
     * @param previousRxBytes the previousRxBytes to set
     */
    public void setPreviousRxBytes(double previousRxBytes)
    {
        previousRxBytes_ = previousRxBytes;
    }

    /**
     * @return the previousTxBytes
     */
    public double getPreviousTxBytes()
    {
        return previousTxBytes_;
    }

    /**
     * @param previousTxBytes the previousTxBytes to set
     */
    public void setPreviousTxBytes(double previousTxBytes)
    {
        previousTxBytes_ = previousTxBytes;
    }

   
    
    

}
