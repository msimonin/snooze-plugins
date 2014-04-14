package org.inria.myriads.snoozenode.groupmanager.managerpolicies.impl;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import junit.framework.TestCase;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.xml.DOMConfigurator;
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
public class testPathoBrtReconfigurationOneResources extends TestCase 
{
    
    /** Estimator.*/
    private ResourceDemandEstimator estimator_;
    
    ReconfigurationPolicy btrPlaceConsolidation_;

    private ReconfigurationSettings settings_;
    
    
    @Override
    protected void setUp() throws Exception 
    {
        estimator_ = EasyMock.createMock(StaticDynamicResourceDemandEstimator.class);
        settings_ = EasyMock.createMock(ReconfigurationSettings.class);
        btrPlaceConsolidation_ = new BtrPlaceConsolidation();
        
        btrPlaceConsolidation_.setEstimator(estimator_);
        btrPlaceConsolidation_.setReconfigurationSettings(settings_);
        HashMap<String, String> mapOptions = new HashMap<String,String>();
        mapOptions.put("metrics", "cpu,mem,rx,tx");
        expect(settings_.getOptions()).andReturn(mapOptions).anyTimes();
        replay(settings_);
        
        double test = 495663320d;
        int i = (int) test;
        System.out.println(i);
    }
    
    
   
    /**
     * test patho
     * No place to freed the first 
     * 
     */
    public void testPatho1()
    {
        
        btrPlaceConsolidation_.initialize();
        List<LocalControllerDescription> localControllers = generateHierarchy("src/test/resources/patho1/description1.json");
        setExpectations(localControllers);
        ReconfigurationPlan reconfigurationPlan = btrPlaceConsolidation_.reconfigure(localControllers);
        System.out.println(OutputUtils.toString(reconfigurationPlan));
        assertNotNull(reconfigurationPlan);
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
