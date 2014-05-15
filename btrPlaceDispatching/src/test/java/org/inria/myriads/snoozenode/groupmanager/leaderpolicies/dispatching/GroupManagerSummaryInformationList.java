package org.inria.myriads.snoozenode.groupmanager.leaderpolicies.dispatching;

import java.util.HashMap;
import java.util.Map;

import org.inria.myriads.snoozecommon.communication.groupmanager.summary.GroupManagerSummaryInformation;

public class GroupManagerSummaryInformationList 
{

    Map<String, GroupManagerSummaryInformation> summaries_;

    /**
     * 
     */
    public GroupManagerSummaryInformationList() 
    {
        summaries_ = new HashMap<String, GroupManagerSummaryInformation>();
    }

    /**
     * @return the summaries
     */
    public Map<String, GroupManagerSummaryInformation> getSummaries()
    {
        return summaries_;
    }

    /**
     * @param summaries the summaries to set
     */
    public void setSummaries(Map<String, GroupManagerSummaryInformation> summaries) 
    {
        summaries_ = summaries;
    }
}
