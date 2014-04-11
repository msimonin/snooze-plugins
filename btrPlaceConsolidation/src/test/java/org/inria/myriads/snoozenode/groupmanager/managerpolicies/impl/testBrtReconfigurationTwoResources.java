package org.inria.myriads.snoozenode.groupmanager.managerpolicies.impl;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import junit.framework.TestCase;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.easymock.EasyMock;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerList;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozenode.configurator.scheduler.ReconfigurationSettings;
import org.inria.myriads.snoozenode.estimator.api.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.estimator.api.impl.StaticDynamicResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.reconfiguration.ReconfigurationPlan;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.reconfiguration.ReconfigurationPolicy;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.reconfiguration.impl.BtrPlaceConsolidation;
import org.inria.myriads.snoozenode.util.OutputUtils;

/**
 * @author msimonin
 *
 */
public class testBrtReconfigurationTwoResources extends TestCase 
{
    
    /** Estimator.*/
    private ResourceDemandEstimator estimator_;
    
    ReconfigurationPolicy btrPlaceConsolidation_;

    private ReconfigurationSettings settings_;
    
    
    @Override
    protected void setUp() throws Exception {
        estimator_ = EasyMock.createMock(StaticDynamicResourceDemandEstimator.class);
        settings_ = EasyMock.createMock(ReconfigurationSettings.class);
        btrPlaceConsolidation_ = new BtrPlaceConsolidation();
        
        btrPlaceConsolidation_.setEstimator(estimator_);
        btrPlaceConsolidation_.setReconfigurationSettings(settings_);
        HashMap<String, String> mapOptions = new HashMap<String,String>();
        mapOptions.put("metrics", "cpu,mem");
       
        expect(settings_.getOptions()).andReturn(mapOptions).anyTimes();
        replay(settings_);
        // set options is missing

    }
    
    
    public void testNoLc()
    {
        btrPlaceConsolidation_.initialize();
        List<LocalControllerDescription> localControllers = generateHierarchy("src/test/resources/two/description4.json");
        setExpectations(localControllers);
        
        ReconfigurationPlan reconfigurationPlan = btrPlaceConsolidation_.reconfigure(localControllers);
        
        assertNull(reconfigurationPlan);
    }
    
    public void test1LCNoMigration()
    {
        btrPlaceConsolidation_.initialize();
        List<LocalControllerDescription> localControllers = generateHierarchy("src/test/resources/two/description3.json");
        setExpectations(localControllers);
       
        estimator_.sortLocalControllers(localControllers, true);
        
        ReconfigurationPlan reconfigurationPlan = btrPlaceConsolidation_.reconfigure(localControllers);
        
        assertNull(reconfigurationPlan);
    }
    
    
    public void test2LCs2VMsOneMigration()
    {
        
        btrPlaceConsolidation_.initialize();
        List<LocalControllerDescription> localControllers = generateHierarchy("src/test/resources/two/description1.json");
        setExpectations(localControllers);
       
        ReconfigurationPlan reconfigurationPlan = btrPlaceConsolidation_.reconfigure(localControllers);
        
        assertNotNull(reconfigurationPlan);
        assertEquals(1,reconfigurationPlan.getNumberOfMigrations());
    }
    
    public void test2LCs2VMsNoMigration()
    {
        
        btrPlaceConsolidation_.initialize();
        List<LocalControllerDescription> localControllers = generateHierarchy("src/test/resources/two/description2.json");
        setExpectations(localControllers);
       
        ReconfigurationPlan reconfigurationPlan = btrPlaceConsolidation_.reconfigure(localControllers);
        
        // plan impossible to find
        assertNull(reconfigurationPlan);
    }
    
    /**
     * 2 LCs, 1 VM each
     * Least loaded is lc1 so vm0-1 is migrated to lc0 
     * and lc1 is freed
     */
    public void test2LCs1VMCheckLeastLoaded()
    {
        
        btrPlaceConsolidation_.initialize();
        List<LocalControllerDescription> localControllers = generateHierarchy("src/test/resources/two/description5.json");
        setExpectations(localControllers);
     
        ReconfigurationPlan reconfigurationPlan = btrPlaceConsolidation_.reconfigure(localControllers);
        
        // plan impossible to find
        assertNotNull(reconfigurationPlan);
        assertEquals(1, reconfigurationPlan.getNumberOfReleasedNodes());
        assertEquals(1, reconfigurationPlan.getMapping().keySet().size());
        for (Entry<VirtualMachineMetaData, LocalControllerDescription>  entry : reconfigurationPlan.getMapping().entrySet())
        {
            VirtualMachineMetaData virtualMachine = entry.getKey();
            LocalControllerDescription localController = entry.getValue();
            assertEquals("vm0-1", virtualMachine.getVirtualMachineLocation().getVirtualMachineId());
            assertEquals("lc0", localController.getId());
        }
    }
    
    /**
     * 2 LCs, 1 empty
     * No migration 
     * 
     */
    public void test2LCsOneEmpty()
    {
        
        btrPlaceConsolidation_.initialize();
        List<LocalControllerDescription> localControllers = generateHierarchy("src/test/resources/two/description6.json");
        setExpectations(localControllers);
 
        ReconfigurationPlan reconfigurationPlan = btrPlaceConsolidation_.reconfigure(localControllers);
        System.out.println(OutputUtils.toString(reconfigurationPlan));
        assertNull(reconfigurationPlan);
    }
    
    /**
     * 3 LCs, 1 empty
     * No migration 
     * 
     */
    public void test3LCsOneEmpty()
    {
        
        btrPlaceConsolidation_.initialize();
        List<LocalControllerDescription> localControllers = generateHierarchy("src/test/resources/two/description7.json");
        setExpectations(localControllers);
        ReconfigurationPlan reconfigurationPlan = btrPlaceConsolidation_.reconfigure(localControllers);
        assertNotNull(reconfigurationPlan);
        assertEquals(1, reconfigurationPlan.getNumberOfReleasedNodes());
        assertEquals(1, reconfigurationPlan.getMapping().keySet().size());
        for (Entry<VirtualMachineMetaData, LocalControllerDescription>  entry : reconfigurationPlan.getMapping().entrySet())
        {
            VirtualMachineMetaData virtualMachine = entry.getKey();
            LocalControllerDescription localController = entry.getValue();
            assertEquals("vm0-1", virtualMachine.getVirtualMachineLocation().getVirtualMachineId());
            assertEquals("lc0", localController.getId());
        }
    }
    
    
    /**
     * 3 LCs, 1 empty
     * No place to freed the first 
     * 
     */
    public void test3LCsOneEmptyNoPlaceLeft()
    {
        
        btrPlaceConsolidation_.initialize();
        List<LocalControllerDescription> localControllers = generateHierarchy("src/test/resources/two/description8.json");
        setExpectations(localControllers);
        ReconfigurationPlan reconfigurationPlan = btrPlaceConsolidation_.reconfigure(localControllers);
        System.out.println(OutputUtils.toString(reconfigurationPlan));
        assertNull(reconfigurationPlan);
    }
    
    
    /**
     * 2 LCs, 1 VM each
     * "second resource limits " -> no migration 
     * 
     */
    public void test2LCsSecondResourceLimits()
    {
        btrPlaceConsolidation_.initialize();
        List<LocalControllerDescription> localControllers = generateHierarchy("src/test/resources/two/description9.json");
        setExpectations(localControllers);
        ReconfigurationPlan reconfigurationPlan = btrPlaceConsolidation_.reconfigure(localControllers);
        assertNull(reconfigurationPlan);
    }
    
    
    
    
    private void setExpectations(List<LocalControllerDescription> localControllers) 
    {       
        for (LocalControllerDescription localController: localControllers)
        {
            for(VirtualMachineMetaData virtualMachine : localController.getVirtualMachineMetaData().values())
            {
                expect(estimator_.estimateVirtualMachineResourceDemand(virtualMachine)).andReturn(virtualMachine.getRequestedCapacity()).anyTimes();
            }
        }
        
        estimator_.sortLocalControllers(localControllers, true);
        EasyMock.expectLastCall().anyTimes();
       
        replay(estimator_);
        
    }
    

    private List<LocalControllerDescription> generateHierarchy(String descriptionFile)
    {
        List<LocalControllerDescription> descriptions = null;
        LocalControllerList list = new LocalControllerList();
        try 
        {
            list = new ObjectMapper().readValue(new File(descriptionFile), LocalControllerList.class);
            descriptions = list.getLocalControllers();
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
        return descriptions;
    }
}
