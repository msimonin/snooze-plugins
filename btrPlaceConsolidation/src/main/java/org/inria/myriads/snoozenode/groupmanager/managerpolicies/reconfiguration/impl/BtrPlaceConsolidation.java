package org.inria.myriads.snoozenode.groupmanager.managerpolicies.reconfiguration.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.globals.Globals;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.reconfiguration.ReconfigurationPlan;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.reconfiguration.ReconfigurationPolicy;
import org.inria.myriads.snoozenode.util.OutputUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import btrplace.model.DefaultModel;
import btrplace.model.Mapping;
import btrplace.model.Model;
import btrplace.model.Node;
import btrplace.model.VM;
import btrplace.model.constraint.Offline;
import btrplace.model.constraint.Preserve;
import btrplace.model.constraint.SatConstraint;
import btrplace.model.view.ShareableResource;
import btrplace.plan.event.Action;
import btrplace.plan.event.MigrateVM;
import btrplace.plan.event.ShutdownNode;
import btrplace.solver.SolverException;
import btrplace.solver.choco.ChocoReconfigurationAlgorithm;
import btrplace.solver.choco.DefaultChocoReconfigurationAlgorithm;

public class BtrPlaceConsolidation extends ReconfigurationPolicy 
{
    /** Logging instance. */
    private static final Logger log_ = LoggerFactory.getLogger(BtrPlaceConsolidation .class);


    /** Precision multiplicator.*/
    private int multiplicator_;
    
    /** mapping between Snooze resources and BtrResources (to take into account).*/
    private Map<Integer, ShareableResource> resources_;
    
    
    
    /**
     * Constructor
     */
    public BtrPlaceConsolidation() 
    {
        resources_ = new HashMap<Integer, ShareableResource>();        
    }

    @Override
    public void initialize() 
    {
        Map<String, String> options = reconfigurationSettings_.getOptions();
        String multiplicator = options.get("multiplicator");
        if (StringUtils.isEmpty(multiplicator))
        {
            multiplicator_ = 10;
        }
        else
        {
            multiplicator_ = Integer.valueOf(multiplicator);
        }
        String metrics = options.get("metrics");
        if (StringUtils.isEmpty(metrics))
        {
            metrics="cpu,mem,tx,rx";
        }
        
        String[] metrics_a = metrics.split(",");
        for (String m :  metrics_a)
        {
            if (m.equals("cpu"))
            {
                System.out.println("adding cpu");
                resources_.put(Globals.CPU_UTILIZATION_INDEX, new ShareableResource("cpu"));
                continue;
            }
            if (m.equals("mem"))
            {
                System.out.println("adding mem");
                resources_.put(Globals.MEMORY_UTILIZATION_INDEX, new ShareableResource("mem"));
                continue;
            }
            if (m.equals("rx"))
            {
                System.out.println("adding rx");
                resources_.put(Globals.NETWORK_RX_UTILIZATION_INDEX, new ShareableResource("rx"));
                continue;
            }
            if (m.equals("tx"))
            {
                System.out.println("adding tx");
                resources_.put(Globals.NETWORK_TX_UTILIZATION_INDEX, new ShareableResource("tx"));
                continue;
            }
        }
    }

    @Override
    public ReconfigurationPlan reconfigure(List<LocalControllerDescription> localControllers)
    {
        Guard.check(localControllers);
        log_.debug("Starting to compute the optimized virtual machine placement");
        OutputUtils.printLocalControllers(localControllers);
        
        if (localControllers.size() < 1)
        {
            log_.debug("Not enough local controllers to do consolidation!");
            return null;
        }
        
        // get the least loaded
        estimator_.sortLocalControllers(localControllers, true);
        
        //translate to btr plan model.
        ReconfigurationPlan reconfigurationPlan = btrReconfiguration(localControllers);

        
        return reconfigurationPlan;
    }

    private ReconfigurationPlan btrReconfiguration(
            List<LocalControllerDescription> localControllers) 
    {
        
        // TODO move that upper level.
        if (localControllers.size() < 1)
        {
            log_.debug("Not enough local controllers to do consolidation!");
            return null;
        }
        
        // Keep a mapping between Btr and Snooze.
        Map<Node, LocalControllerDescription> mappingLocalController = 
                new HashMap<Node, LocalControllerDescription>();
        
        // Keep a mapping between Btr and Snooze.
        Map<VM, VirtualMachineMetaData> mappingVirtualMachine = 
                new HashMap<VM, VirtualMachineMetaData>();
        
        // Create the model
        Model model = new DefaultModel();
        
        // Create the mapping
        Mapping map = model.getMapping();
        
        // Constraints
        List<SatConstraint> constraints = new ArrayList<SatConstraint>();
        
        // Add resource to model.
        for (ShareableResource shareableResource : resources_.values())
        {
            model.attach(shareableResource);
        }
        
        //ShareableResource resourceMem = new ShareableResource("mem");
        List<Node> nodes = new ArrayList<Node>();
        List<VM> vms = new ArrayList<VM>();
        
        
        Node leastLoadedNode = null;
        
        for (LocalControllerDescription localController : localControllers)
        {
            //exclude empty local controllers.
            if (localController.getVirtualMachineMetaData().size() < 1)
            {
                continue;
            }
            Node node = model.newNode();
            nodes.add(node);
            map.addOnlineNode(node);
            System.out.println("Adding node in model for the localcontroller " + localController.getId());
            mappingLocalController.put(node, localController);
            for (Entry<Integer, ShareableResource> entry : resources_.entrySet())
            {
                double totalResource = Math.ceil(multiplicator_ * localController.getTotalCapacity().get(entry.getKey()));
                entry.getValue().setCapacity(node, (int) totalResource);
            }
            
            leastLoadedNode = node;
            for (VirtualMachineMetaData virtualMachine : localController.getVirtualMachineMetaData().values())
            {
                VM v = model.newVM();
                vms.add(v);
                map.addRunningVM(v, node);
                mappingVirtualMachine.put(v, virtualMachine);
                ArrayList<Double> resourceDemand = estimator_.estimateVirtualMachineResourceDemand(virtualMachine);
                for (Entry<Integer, ShareableResource> entry : resources_.entrySet())
                {
                    int index = entry.getKey();
                    ShareableResource shareableResource = entry.getValue();
                    
                    double resourceVM= Math.ceil(multiplicator_ * resourceDemand.get(index));                  
                    constraints.add(new Preserve(v, shareableResource.getResourceIdentifier(), (int) resourceVM));    
                }   
                
                
                
            }

        }

        if (leastLoadedNode != null)
        {
            constraints.add(new Offline(leastLoadedNode));
        }
            
        
        ChocoReconfigurationAlgorithm ra = new DefaultChocoReconfigurationAlgorithm();
        try 
        {
            btrplace.plan.ReconfigurationPlan plan = ra.solve(model, constraints);
            
            if (plan == null)
            {
                //no plan has been found.
                return null;
            }
            
            // mapping for the snooze reconstruction plan
            Map<VirtualMachineMetaData, LocalControllerDescription> mapping = 
                    new HashMap<VirtualMachineMetaData, LocalControllerDescription>();
            int numberOfActiveLocalControllers = localControllers.size();
            int numberOfReleasedNodes = 0;
            for (Action action: plan)
            {
                if (action instanceof MigrateVM)
                {
                    MigrateVM migrateAction = (MigrateVM) action;
                    VM v = migrateAction.getVM();
                    Node destination = migrateAction.getDestinationNode();
                    mapping.put(mappingVirtualMachine.get(v), mappingLocalController.get(destination));
                }
                if (action instanceof ShutdownNode)
                {
                    numberOfReleasedNodes ++;
                }
            }
            
            int numberOfUsedNodes = numberOfActiveLocalControllers - numberOfReleasedNodes;
            
            ReconfigurationPlan reconfigurationPlan = new ReconfigurationPlan(
                    mapping,
                    numberOfUsedNodes,
                    numberOfReleasedNodes
                    );
            
            return reconfigurationPlan;
        }
        catch (SolverException ex) 
        {
            ex.printStackTrace();
        }
        return null;
    }

}
