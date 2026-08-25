package org.matsim.project.energy;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FreightVehiclePayloadTrackerTest {

    @Test
    void shouldTrackPickupAndDeliveryPayload() {

        FreightVehiclePayloadTracker tracker =
                new FreightVehiclePayloadTracker();

        Id<Vehicle> vehicleId =
                Id.createVehicleId("BET_01");


        // Initially empty
        assertEquals(
                0.0,
                tracker.getPayloadKg(vehicleId),
                0.001
        );


        // Pick up 8000 kg
        tracker.addPayload(vehicleId, 8000.0);

        System.out.println(
                "After pickup [kg] = "
                        + tracker.getPayloadKg(vehicleId)
        );

        assertEquals(
                8000.0,
                tracker.getPayloadKg(vehicleId),
                0.001
        );


        // Deliver 3000 kg
        tracker.removePayload(vehicleId, 3000.0);

        System.out.println(
                "After first delivery [kg] = "
                        + tracker.getPayloadKg(vehicleId)
        );

        assertEquals(
                5000.0,
                tracker.getPayloadKg(vehicleId),
                0.001
        );


        // Deliver remaining 5000 kg
        tracker.removePayload(vehicleId, 5000.0);

        System.out.println(
                "After final delivery [kg] = "
                        + tracker.getPayloadKg(vehicleId)
        );

        assertEquals(
                0.0,
                tracker.getPayloadKg(vehicleId),
                0.001
        );
    }
}