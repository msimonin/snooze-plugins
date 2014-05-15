package org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.globals.Globals;
import org.inria.myriads.snoozecommon.util.MathUtils;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.dispatching.DispatchingPlan;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.PlacementPlan;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.PlacementPolicy;

import btrplace.model.DefaultModel;
import btrplace.model.Mapping;
import btrplace.model.Model;
import btrplace.model.Node;
import btrplace.model.VM;
import btrplace.model.constraint.Preserve;
import btrplace.model.constraint.Running;
import btrplace.model.constraint.SatConstraint;
import btrplace.model.view.ShareableResource;
import btrplace.plan.DependencyBasedPlanApplier;
import btrplace.plan.TimeBasedPlanApplier;
import btrplace.plan.event.Action;
import btrplace.plan.event.BootVM;
import btrplace.solver.SolverException;
import btrplace.solver.choco.ChocoReconfigurationAlgorithm;
import btrplace.solver.choco.DefaultChocoReconfigurationAlgorithm;

public class BtrPlacePlacement extends PlacementPolicy 
{

	/** mapping between Snooze resources and BtrResources (to take into account).*/
    private Map<Integer, ShareableResource> resources_;

    
    /** Resource multiplicator.*/
    private Map<Integer, Double> resourceMultiplicators_;
  
    /**
     *
     * Constructor.
     *     
     */
    public BtrPlacePlacement() 
    {
        resourceMultiplicators_ = new HashMap<Integer, Double>();
        resources_ = new HashMap<Integer, ShareableResource>();
    }
	
	@Override
	public void initialize() 
	{
		Map<String, String> options  = schedulerSettings_.getOptions();
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
                resourceMultiplicators_.put(Globals.CPU_UTILIZATION_INDEX, 10d);
                continue;
            }
            if (m.equals("mem"))
            {
                System.out.println("adding mem");
                resources_.put(Globals.MEMORY_UTILIZATION_INDEX, new ShareableResource("mem"));
                resourceMultiplicators_.put(Globals.MEMORY_UTILIZATION_INDEX, 1/1024d);
                continue;
            }
            if (m.equals("rx"))
            {
                System.out.println("adding rx");
                resources_.put(Globals.NETWORK_RX_UTILIZATION_INDEX, new ShareableResource("rx"));
                resourceMultiplicators_.put(Globals.NETWORK_RX_UTILIZATION_INDEX, 10d);
                continue;
            }
            if (m.equals("tx"))
            {
                System.out.println("adding tx");
                resources_.put(Globals.NETWORK_TX_UTILIZATION_INDEX, new ShareableResource("tx"));
                resourceMultiplicators_.put(Globals.NETWORK_TX_UTILIZATION_INDEX, 10d);
                continue;
            }
        }

	}

	@Override
	public PlacementPlan place(List<VirtualMachineMetaData> virtualMachines,
			List<LocalControllerDescription> localControllers) 
	{
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
        
        List<Node> nodes = new ArrayList<Node>();
        List<VM> vms = new ArrayList<VM>();
        double multiplicator;
        
        for (LocalControllerDescription localController : localControllers)
        {
        	Node node = model.newNode();
        	nodes.add(node);
        	map.addOnlineNode(node);
        	mappingLocalController.put(node , localController);
        	
        	
        	ArrayList<Double> capacity = 
        			estimator_.computeLocalControllerCapacity(localController);
        	
        	
        	for (Entry<Integer, ShareableResource> entry : resources_.entrySet())
        	{
        		multiplicator = resourceMultiplicators_.get(entry.getKey());
        		double totalResource = Math.ceil(multiplicator * capacity.get(entry.getKey()));
        		entry.getValue().setCapacity(node, (int) totalResource);
        	}
        }
        
        for (VirtualMachineMetaData virtualMachine : virtualMachines)
        {
        	VM v = model.newVM();
        	vms.add(v);
        	map.addReadyVM(v);
            constraints.add(new Running(v));
            mappingVirtualMachine.put(v, virtualMachine);
            
            System.out.println("Adding virtual machine in model " + virtualMachine.getVirtualMachineLocation().getVirtualMachineId());
            ArrayList<Double> resourceDemand = virtualMachine.getRequestedCapacity();
            for (Entry<Integer, ShareableResource> entry : resources_.entrySet())
            {
                int index = entry.getKey();
                ShareableResource shareableResource = entry.getValue();
                multiplicator = resourceMultiplicators_.get(index);
                double resourceVM= Math.ceil(multiplicator * resourceDemand.get(index));                  
                System.out.println("Preserve constraint" + " / " +shareableResource.getResourceIdentifier() + " value : " + (int) resourceVM);
                constraints.add(new Preserve(v, shareableResource.getResourceIdentifier(), (int) resourceVM));    
            }   
        }
        
        List<LocalControllerDescription> usedLocalControllers = new ArrayList<LocalControllerDescription>();
        List<VirtualMachineMetaData> unassignedVirtualMachines = new ArrayList<VirtualMachineMetaData>();
        
        ChocoReconfigurationAlgorithm ra = new DefaultChocoReconfigurationAlgorithm();
        try 
        {
            btrplace.plan.ReconfigurationPlan plan = ra.solve(model, constraints);
           
            if (plan == null)
            {
                System.out.println("BtrPlace was unable to find a solution");
                unassignedVirtualMachines.addAll(virtualMachines);
                return new PlacementPlan(usedLocalControllers, unassignedVirtualMachines);
            }
            
            System.out.println("Time-based plan:");
            System.out.println(new TimeBasedPlanApplier().toString(plan));
            System.out.println("\nDependency based plan:");
            System.out.println(new DependencyBasedPlanApplier().toString(plan));
            
            Map<String, LocalControllerDescription> targetLocalControllers =  
                    new HashMap<String, LocalControllerDescription>(); 
            
            for (Action action : plan)
            {
                if (action instanceof BootVM)
                {
                    BootVM bootAction = (BootVM) action;
                    VM v = bootAction.getVM();
                    Node destination = bootAction.getDestinationNode();
                    LocalControllerDescription localControllerDestination = mappingLocalController.get(destination);
                    VirtualMachineMetaData virtualMachine = mappingVirtualMachine.get(v);
                    localControllerDestination.getAssignedVirtualMachines().add(virtualMachine);
                    targetLocalControllers.put(localControllerDestination.getId(), localControllerDestination);
                }
            }
            usedLocalControllers = new ArrayList<LocalControllerDescription>(targetLocalControllers.values());
            PlacementPlan placementPlan = new PlacementPlan(usedLocalControllers, unassignedVirtualMachines);
            return placementPlan;
        }
        catch (SolverException ex) 
        {
            ex.printStackTrace();
        }
        
        unassignedVirtualMachines = new ArrayList<VirtualMachineMetaData>(virtualMachines);
        usedLocalControllers = new ArrayList<LocalControllerDescription>();
        return new PlacementPlan(usedLocalControllers, unassignedVirtualMachines);
     
    }
	
}
