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

import static org.junit.jupiter.api.Assertions.assertTrue;

class FreightOhdeSlaskiGradeTest {

    @Test
    void uphillShouldConsumeMoreThanFlatAndDownhill() {

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
                Id.createLinkId("gradeTestLink"),
                from,
                to
        );

        link.setLength(1000.0);
        network.addLink(link);


        Id<Vehicle> vehicleId =
                Id.createVehicleId("BET_01");

        FreightVehiclePayloadTracker tracker =
                new FreightVehiclePayloadTracker();

        // 12 t empty + 8 t goods = 20 t total
        tracker.addPayload(vehicleId, 8000.0);

        FreightOhdeSlaskiDriveEnergyConsumption model =
                new FreightOhdeSlaskiDriveEnergyConsumption(
                        vehicleId,
                        12000.0,
                        tracker
                );

        // 1000 m / 100 s = 10 m/s
        double travelTime = 100.0;


        // Flat
        link.getAttributes()
                .putAttribute("grade", 0.0);

        double flatEnergy =
                model.calcEnergyConsumption(
                        link,
                        travelTime,
                        0.0
                );


        // +5% uphill
        link.getAttributes()
                .putAttribute("grade", 0.05);

        double uphillEnergy =
                model.calcEnergyConsumption(
                        link,
                        travelTime,
                        0.0
                );


        // -5% downhill
        link.getAttributes()
                .putAttribute("grade", -0.05);

        double downhillEnergy =
                model.calcEnergyConsumption(
                        link,
                        travelTime,
                        0.0
                );


        System.out.println(
                "Flat energy [J] = "
                        + flatEnergy
        );

        System.out.println(
                "Uphill +5% energy [J] = "
                        + uphillEnergy
        );

        System.out.println(
                "Downhill -5% energy [J] = "
                        + downhillEnergy
        );


        assertTrue(
                uphillEnergy > flatEnergy,
                "Uphill energy must be higher than flat energy"
        );

        assertTrue(
                flatEnergy > downhillEnergy,
                "Flat energy must be higher than downhill energy"
        );
    }
}



