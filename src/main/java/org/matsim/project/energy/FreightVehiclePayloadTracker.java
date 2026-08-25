package org.matsim.project.energy;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores the current freight payload of each vehicle.
 * Payload values are interpreted as kilograms.
 */
public final class FreightVehiclePayloadTracker {

    private final Map<Id<Vehicle>, Double> payloadKgByVehicle =
            new HashMap<>();

    public double getPayloadKg(Id<Vehicle> vehicleId) {
        return payloadKgByVehicle.getOrDefault(vehicleId, 0.0);
    }

    public void addPayload(
            Id<Vehicle> vehicleId,
            double goodsWeightKg) {

        if (goodsWeightKg < 0.0) {
            throw new IllegalArgumentException(
                    "Added payload cannot be negative."
            );
        }

        double newPayload =
                getPayloadKg(vehicleId) + goodsWeightKg;

        payloadKgByVehicle.put(vehicleId, newPayload);
    }

    public void removePayload(
            Id<Vehicle> vehicleId,
            double goodsWeightKg) {

        if (goodsWeightKg < 0.0) {
            throw new IllegalArgumentException(
                    "Removed payload cannot be negative."
            );
        }

        double newPayload =
                getPayloadKg(vehicleId) - goodsWeightKg;

        if (newPayload < -1e-6) {
            throw new IllegalStateException(
                    "Vehicle " + vehicleId
                            + " would have negative payload: "
                            + newPayload + " kg"
            );
        }

        payloadKgByVehicle.put(
                vehicleId,
                Math.max(0.0, newPayload)
        );
    }

    public void reset() {
        payloadKgByVehicle.clear();
    }
}
