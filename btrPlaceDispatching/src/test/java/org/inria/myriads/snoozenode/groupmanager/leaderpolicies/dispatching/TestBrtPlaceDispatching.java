package org.inria.myriads.snoozenode.groupmanager.leaderpolicies.dispatching;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.TestCase;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.easymock.EasyMock;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.groupmanager.summary.GroupManagerSummaryInformation;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerList;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualmachine.VirtualMachinesList;
import org.inria.myriads.snoozenode.configurator.scheduler.GroupLeaderSchedulerSettings;
import org.inria.myriads.snoozenode.configurator.scheduler.ReconfigurationSettings;
import org.inria.myriads.snoozenode.estimator.api.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.estimator.api.impl.StaticDynamicResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.dispatching.impl.BtrPlaceDispatching;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.reconfiguration.ReconfigurationPlan;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.reconfiguration.ReconfigurationPolicy;
import org.inria.myriads.snoozenode.util.OutputUtils;

/**
 * @author msimonin
 *
 */
public class TestBrtPlaceDispatching extends TestCase 
{
    
    /** Estimator.*/
    private ResourceDemandEstimator estimator_;
    
    private BtrPlaceDispatching btrPlaceDispatching_;

    private GroupLeaderSchedulerSettings settings_;
    
    
    @Override
    protected void setUp() throws Exception {
        estimator_ = EasyMock.createMock(StaticDynamicResourceDemandEstimator.class);
        settings_ = EasyMock.createMock(GroupLeaderSchedulerSettings.class);
        btrPlaceDispatching_ = new BtrPlaceDispatching();
        
        btrPlaceDispatching_.setEstimator(estimator_);
        btrPlaceDispatching_.setGroupLeaderSchedulingSettings(settings_);
        HashMap<String, String> mapOptions = new HashMap<String,String>();
        mapOptions.put("metrics", "cpu");
        expect(settings_.getOptions()).andReturn(mapOptions).anyTimes();
        replay(settings_);
    }
    
    
    public void testOneVmOneGmWithNoRoom()
    {
        btrPlaceDispatching_.initialize();
        GroupManagerSummaryInformationList summaryList= loadGroupManagers("src/test/resources/gms-0.json");
        List<GroupManagerDescription> groupManagers = createGroupManagersFromSummary(summaryList);
        VirtualMachinesList virtualMachines = loadVirtualMachines("src/test/resources/vms-1.json");
        DispatchingPlan plan = btrPlaceDispatching_.dispatch(virtualMachines.getVirtualMachines(), groupManagers);
        
        assertNull(plan);
        
    }
    
    public void testOneVmOneGmOk()
    {
        btrPlaceDispatching_.initialize();
        GroupManagerSummaryInformationList summaryList= loadGroupManagers("src/test/resources/gms-1.json");
        List<GroupManagerDescription> groupManagers = createGroupManagersFromSummary(summaryList);
        VirtualMachinesList virtualMachines = loadVirtualMachines("src/test/resources/vms-1.json");
        DispatchingPlan plan = btrPlaceDispatching_.dispatch(virtualMachines.getVirtualMachines(), groupManagers);
        
        assertNotNull(plan);
        assertEquals(1,plan.getGroupManagers().size());
        GroupManagerDescription groupManager = plan.getGroupManagers().get(0);
        assertEquals("gm1", groupManager.getId());
        assertEquals(1, groupManager.getVirtualMachines().size());
        VirtualMachineMetaData virtualMachine = groupManager.getVirtualMachines().get(0);
        assertEquals("vm1", virtualMachine.getVirtualMachineLocation().getVirtualMachineId());
    }

    
    public void testTwoVmOneGmOnlyOneVmOk()
    {
        btrPlaceDispatching_.initialize();
        GroupManagerSummaryInformationList summaryList= loadGroupManagers("src/test/resources/gms-1.json");
        List<GroupManagerDescription> groupManagers = createGroupManagersFromSummary(summaryList);
        VirtualMachinesList virtualMachines = loadVirtualMachines("src/test/resources/vms-2.json");
        DispatchingPlan plan = btrPlaceDispatching_.dispatch(virtualMachines.getVirtualMachines(), groupManagers);
        assertNull(plan);        
    }
    
    
    public void testFourVmOneGmAllVmOk()
    {
        btrPlaceDispatching_.initialize();
        GroupManagerSummaryInformationList summaryList= loadGroupManagers("src/test/resources/gms-1.json");
        List<GroupManagerDescription> groupManagers = createGroupManagersFromSummary(summaryList);
        VirtualMachinesList virtualMachines = loadVirtualMachines("src/test/resources/vms-3.json");
        DispatchingPlan plan = btrPlaceDispatching_.dispatch(virtualMachines.getVirtualMachines(), groupManagers);
        
        assertNotNull(plan);
        assertEquals(1,plan.getGroupManagers().size());
        GroupManagerDescription groupManager = plan.getGroupManagers().get(0);
        assertEquals("gm1", groupManager.getId());
        assertEquals(4, groupManager.getVirtualMachines().size());
    }
    
    private List<GroupManagerDescription> createGroupManagersFromSummary(
            GroupManagerSummaryInformationList summaryList) 
    {
        
        List<GroupManagerDescription> groupManagers = new ArrayList<GroupManagerDescription>();
        
        for (Entry<String, GroupManagerSummaryInformation> entry : summaryList.getSummaries().entrySet())
        {
            GroupManagerSummaryInformation summary = entry.getValue();
            String groupManagerId = entry.getKey();
            GroupManagerDescription groupManager = new GroupManagerDescription();
            groupManager.setHostname(groupManagerId);
            groupManager.setId(groupManagerId);
            groupManagers.add(groupManager);
            //set expectation
            // We use the active capacity as capacity (beware of that)
            expect(estimator_.computeGroupManagerCapacity(groupManager)).andReturn(summary.getActiveCapacity()).anyTimes();
        }
        replay(estimator_);
        return groupManagers;
    }

    private VirtualMachinesList loadVirtualMachines(String descriptionFile) 
    {
        List<LocalControllerDescription> descriptions = null;
        VirtualMachinesList list = new VirtualMachinesList();
        try 
        {
            list = new ObjectMapper().readValue(new File(descriptionFile), VirtualMachinesList.class);                
        }
        catch (JsonParseException e) 
        {
            e.printStackTrace();
        } catch (JsonMappingException e) 
        {
            e.printStackTrace();
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
        return list;
    }


    
  
    private GroupManagerSummaryInformationList loadGroupManagers(String descriptionFile)
    {
    
        List<LocalControllerDescription> descriptions = null;
        GroupManagerSummaryInformationList list = new GroupManagerSummaryInformationList();
        try 
        {
            list = new ObjectMapper().readValue(new File(descriptionFile), GroupManagerSummaryInformationList.class);                
        }
        catch (JsonParseException e) 
        {
            e.printStackTrace();
        } catch (JsonMappingException e) 
        {
            e.printStackTrace();
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
        return list;
    }
}
