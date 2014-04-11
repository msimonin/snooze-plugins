package org.inria.myriads.snoozenode.groupmanager.managerpolicies.reconfiguration.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    
    @Override
    public void initialize() 
    {
        multiplicator_ = 10;
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
        
        // TODO ? in options plugins?
        ShareableResource resourceCpu = new ShareableResource("cpu");
        model.attach(resourceCpu);
        
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
            double totalCpu = Math.ceil(multiplicator_ * localController.getTotalCapacity().get(Globals.CPU_UTILIZATION_INDEX));
            
            resourceCpu.setCapacity(node, (int) totalCpu );
            leastLoadedNode = node;
            for (VirtualMachineMetaData virtualMachine : localController.getVirtualMachineMetaData().values())
            {
                VM v = model.newVM();
                vms.add(v);
                map.addRunningVM(v, node);
                mappingVirtualMachine.put(v, virtualMachine);
                ArrayList<Double> resourceDemand = estimator_.estimateVirtualMachineResourceDemand(virtualMachine);
                
                double resourceVMCpu = Math.ceil(multiplicator_ * resourceDemand.get(Globals.CPU_UTILIZATION_INDEX));
                
                constraints.add(new Preserve(v, "cpu", (int) resourceVMCpu));
                
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
