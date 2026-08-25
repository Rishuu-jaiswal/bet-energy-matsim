package org.matsim.project.energy;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.vehicles.Vehicle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FreightOhdeSlaskiDriveEnergyConsumptionTest {

    @Test
    void payloadTrackerShouldChangeVehicleEnergyConsumption() {

        // --------------------------------------------------
        // Create test link
        // --------------------------------------------------

        Network network = NetworkUtils.createNetwork();
        NetworkFactory factory = network.getFactory();

        Node from = factory.createNode(
                Id.createNodeId("from"),
                new Coord(0, 0)
        );

        Node to = factory.createNode(
                Id.createNodeId("to"),
                new Coord(1000, 0)
        );

        network.addNode(from);
        network.addNode(to);

        Link link = factory.createLink(
                Id.createLinkId("testLink"),
                from,
                to
        );

        link.setLength(1000.0);
        network.addLink(link);


        // --------------------------------------------------
        // Create vehicle and shared payload tracker
        // --------------------------------------------------

        Id<Vehicle> vehicleId =
                Id.createVehicleId("BET_01");

        FreightVehiclePayloadTracker tracker =
                new FreightVehiclePayloadTracker();


        // Empty BET self-weight = 12,000 kg
        FreightOhdeSlaskiDriveEnergyConsumption energyModel =
                new FreightOhdeSlaskiDriveEnergyConsumption(
                        vehicleId,
                        12000.0,
                        tracker
                );


        double travelTime = 100.0;


        // --------------------------------------------------
        // CASE 1: Empty BET
        // --------------------------------------------------

        double emptyEnergy =
                energyModel.calcEnergyConsumption(
                        link,
                        travelTime,
                        0.0
                );

        System.out.println(
                "Empty payload [kg] = "
                        + energyModel.getGoodsWeightKg()
        );

        System.out.println(
                "Empty total weight [kg] = "
                        + energyModel.getTotalWeightKg()
        );

        System.out.println(
                "Empty energy [J] = "
                        + emptyEnergy
        );


        // --------------------------------------------------
        // CASE 2: Freight pickup
        // --------------------------------------------------

        tracker.addPayload(
                vehicleId,
                8000.0
        );

        double loadedEnergy =
                energyModel.calcEnergyConsumption(
                        link,
                        travelTime,
                        0.0
                );

        System.out.println(
                "Loaded payload [kg] = "
                        + energyModel.getGoodsWeightKg()
        );

        System.out.println(
                "Loaded total weight [kg] = "
                        + energyModel.getTotalWeightKg()
        );

        System.out.println(
                "Loaded energy [J] = "
                        + loadedEnergy
        );


        // --------------------------------------------------
        // CASE 3: Deliver 3000 kg
        // --------------------------------------------------

        tracker.removePayload(
                vehicleId,
                3000.0
        );

        double partlyLoadedEnergy =
                energyModel.calcEnergyConsumption(
                        link,
                        travelTime,
                        0.0
                );

        System.out.println(
                "After delivery payload [kg] = "
                        + energyModel.getGoodsWeightKg()
        );

        System.out.println(
                "After delivery total weight [kg] = "
                        + energyModel.getTotalWeightKg()
        );

        System.out.println(
                "After delivery energy [J] = "
                        + partlyLoadedEnergy
        );


        // --------------------------------------------------
        // Checks
        // --------------------------------------------------

        assertEquals(
                5000.0,
                energyModel.getGoodsWeightKg(),
                0.001
        );

        assertEquals(
                17000.0,
                energyModel.getTotalWeightKg(),
                0.001
        );

        assertTrue(
                loadedEnergy > partlyLoadedEnergy
        );

        assertTrue(
                partlyLoadedEnergy > emptyEnergy
        );
    }
}


