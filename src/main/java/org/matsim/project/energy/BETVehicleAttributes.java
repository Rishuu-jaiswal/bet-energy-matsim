package org.matsim.project.energy;

import org.matsim.vehicles.Vehicle;

public final class BETVehicleAttributes {

    public static final String SELF_WEIGHT_KG = "selfWeightKg";

    private BETVehicleAttributes() {
    }

    public static void setSelfWeightKg(
            Vehicle vehicle,
            double selfWeightKg) {

        if (selfWeightKg <= 0) {
            throw new IllegalArgumentException(
                    "Self-weight must be greater than zero."
            );
        }

        vehicle.getAttributes().putAttribute(
                SELF_WEIGHT_KG,
                selfWeightKg
        );
    }

    public static double getSelfWeightKg(
            Vehicle vehicle) {

        Object value =
                vehicle.getAttributes()
                        .getAttribute(SELF_WEIGHT_KG);

        if (value == null) {
            throw new IllegalStateException(
                    "Vehicle " + vehicle.getId()
                            + " has no selfWeightKg attribute."
            );
        }

        return Double.parseDouble(
                value.toString()
        );
    }
}



