package org.matsim.project.energy;

import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

public final class FreightOhdeSlaskiDriveEnergyConsumptionFactory
        implements DriveEnergyConsumption.Factory {

    private final Vehicles vehicles;
    private final FreightVehiclePayloadTracker payloadTracker;


    public FreightOhdeSlaskiDriveEnergyConsumptionFactory(
            Vehicles vehicles,
            FreightVehiclePayloadTracker payloadTracker) {

        this.vehicles = vehicles;
        this.payloadTracker = payloadTracker;
    }


    @Override
    public DriveEnergyConsumption create(
            ElectricVehicle electricVehicle) {

        Vehicle vehicle =
                vehicles.getVehicles().get(
                        electricVehicle.getId()
                );

        if (vehicle == null) {
            throw new IllegalStateException(
                    "MATSim vehicle not found for EV "
                            + electricVehicle.getId()
            );
        }

        double selfWeightKg =
                BETVehicleAttributes.getSelfWeightKg(
                        vehicle
                );

        return new FreightOhdeSlaskiDriveEnergyConsumption(
                electricVehicle.getId(),
                selfWeightKg,
                payloadTracker
        );
    }
}

