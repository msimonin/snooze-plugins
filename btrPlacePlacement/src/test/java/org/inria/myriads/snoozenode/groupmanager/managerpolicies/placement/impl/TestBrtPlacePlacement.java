package org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.impl;


import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import junit.framework.TestCase;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.easymock.EasyMock;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerList;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualmachine.VirtualMachinesList;
import org.inria.myriads.snoozenode.configurator.scheduler.GroupManagerSchedulerSettings;
import org.inria.myriads.snoozenode.estimator.api.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.estimator.api.impl.StaticDynamicResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.PlacementPlan;

/**
 * @author msimonin
 *
 */
public class TestBrtPlacePlacement extends TestCase 
{
    
    /** Estimator.*/
    private ResourceDemandEstimator estimator_;
    
    private BtrPlacePlacement btrPlacePlacement_;

    private GroupManagerSchedulerSettings settings_;
    
    
    @Override
    protected void setUp() throws Exception {
        estimator_ = EasyMock.createMock(StaticDynamicResourceDemandEstimator.class);
        settings_ = EasyMock.createMock(GroupManagerSchedulerSettings.class);
        btrPlacePlacement_ = new BtrPlacePlacement();
        
        btrPlacePlacement_.setEstimator(estimator_);
        btrPlacePlacement_.setGroupManagerSettings(settings_);
        HashMap<String, String> mapOptions = new HashMap<String,String>();
        mapOptions.put("metrics", "cpu,mem,rx,tx");
        expect(settings_.getOptions()).andReturn(mapOptions).anyTimes();
        replay(settings_);
    }
    
    public void testLoadLocalControllers()
    {
    	LocalControllerList list = loadLocalControllers("src/test/resources/lcs-0.json");
    	assertEquals(1, list.getLocalControllers().size());
    	LocalControllerDescription lc0 = list.getLocalControllers().get(0);
    	assertNotNull(lc0);
    	assertEquals("lc0", lc0.getId());
    }
    
    public void testOneVmOneLcWithNoRoom()
    {
        btrPlacePlacement_.initialize();
        LocalControllerList list = loadLocalControllers("src/test/resources/lcs-0.json");
        setExpectations(list);
        VirtualMachinesList virtualMachines = loadVirtualMachines("src/test/resources/vms-1.json");
        PlacementPlan plan = btrPlacePlacement_.place(virtualMachines.getVirtualMachines(), list.getLocalControllers());
        
        assertNotNull(plan);
        assertEquals(0, plan.getLocalControllers().size());
        assertEquals(1, plan.gettUnassignedVirtualMachines().size());
    }
    
    
    public void testOneVmOneLcOk()
    {
    	btrPlacePlacement_.initialize();
    	LocalControllerList list = loadLocalControllers("src/test/resources/lcs-1.json");
        setExpectations(list);
        VirtualMachinesList virtualMachines = loadVirtualMachines("src/test/resources/vms-1.json");
        PlacementPlan plan = btrPlacePlacement_.place(virtualMachines.getVirtualMachines(), list.getLocalControllers());
        
        assertNotNull(plan);
        assertEquals(0, plan.gettUnassignedVirtualMachines().size());
        assertNotNull(plan.getLocalControllers().get(0).getAssignedVirtualMachines());
        ArrayList<VirtualMachineMetaData> assignedVirtualMachines = plan.getLocalControllers().get(0).getAssignedVirtualMachines();
        assertEquals(1, assignedVirtualMachines.size());
        assertEquals("vm1", assignedVirtualMachines.get(0).getVirtualMachineLocation().getVirtualMachineId());
    }
    
    public void testTwoVmOneLmOnlyOneVmOk()
    {
    	btrPlacePlacement_.initialize();
    	LocalControllerList list = loadLocalControllers("src/test/resources/lcs-1.json");
        setExpectations(list);
        VirtualMachinesList virtualMachines = loadVirtualMachines("src/test/resources/vms-2.json");
        PlacementPlan plan = btrPlacePlacement_.place(virtualMachines.getVirtualMachines(), list.getLocalControllers());
        
        assertNotNull(plan);
        assertEquals(0, plan.getLocalControllers().size());
        assertEquals(2, plan.gettUnassignedVirtualMachines().size());
    }
    
    public void testFourVmOneLcAllVmOk()
    {
    	btrPlacePlacement_.initialize();
    	LocalControllerList list = loadLocalControllers("src/test/resources/lcs-1.json");
        setExpectations(list);
        VirtualMachinesList virtualMachines = loadVirtualMachines("src/test/resources/vms-3.json");
        PlacementPlan plan = btrPlacePlacement_.place(virtualMachines.getVirtualMachines(), list.getLocalControllers());
        
        assertNotNull(plan);
        assertEquals(1, plan.getLocalControllers().size());
        assertEquals(0, plan.gettUnassignedVirtualMachines().size());
    }
    
    
    private void setExpectations(LocalControllerList list) 
    {
        for (LocalControllerDescription localController : list.getLocalControllers())
        {
            //set expectation
        	// we use the total capacity to reflect the left capacity
            expect(estimator_.computeLocalControllerCapacity(localController)).andReturn(localController.getTotalCapacity());
        }
        replay(estimator_);
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


    
  
    private LocalControllerList loadLocalControllers(String descriptionFile)
    {
    
        LocalControllerList list = new LocalControllerList();
        try 
        {
            list = new ObjectMapper().readValue(new File(descriptionFile), LocalControllerList.class);                
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
