package org.matsim.project.energy;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.fleet.ElectricFleetUtils;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.fleet.ElectricVehicleSpecification;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class FreightOhdeSlaskiDriveEnergyConsumptionFactoryTest {

    @Test
    void factoryShouldReadSelfWeightFromMatsimVehicle() {

        // --------------------------------------------------
        // Create MATSim vehicle
        // --------------------------------------------------

        Vehicles vehicles =
                VehicleUtils.createVehiclesContainer();

        VehicleType vehicleType =
                VehicleUtils.createVehicleType(
                        Id.create("BET_TYPE", VehicleType.class)
                );

        vehicles.addVehicleType(vehicleType);

        Vehicle vehicle =
                VehicleUtils.createVehicle(
                        Id.createVehicleId("BET_01"),
                        vehicleType
                );

        // Store empty/self weight on vehicle
        BETVehicleAttributes.setSelfWeightKg(
                vehicle,
                12000.0
        );

// Required by MATSim EV specification
        ElectricFleetUtils.setInitialSoc(
                vehicle,
                1.0
        );

        vehicles.addVehicle(vehicle);

        // --------------------------------------------------
        // Create corresponding EV specification
        // --------------------------------------------------

        ElectricVehicleSpecification specification =
                ElectricFleetUtils
                        .createElectricVehicleSpecificationDefaultImpl(
                                vehicle
                        );


        // --------------------------------------------------
        // Create EV
        //
        // We only need an ElectricVehicle object here so that
        // our drive-energy factory receives the vehicle ID.
        // --------------------------------------------------

        ElectricVehicle electricVehicle =
                new ElectricVehicle() {

                    @Override
                    public Id<Vehicle> getId() {
                        return specification.getId();
                    }

                    @Override
                    public org.matsim.contrib.ev.discharging.DriveEnergyConsumption
                    getDriveEnergyConsumption() {
                        return null;
                    }

                    @Override
                    public org.matsim.contrib.ev.discharging.AuxEnergyConsumption
                    getAuxEnergyConsumption() {
                        return null;
                    }

                    @Override
                    public org.matsim.contrib.ev.charging.ChargingPower
                    getChargingPower() {
                        return null;
                    }

                    @Override
                    public org.matsim.contrib.ev.fleet.Battery
                    getBattery() {
                        return null;
                    }

                    @Override
                    public ElectricVehicleSpecification
                    getVehicleSpecification() {
                        return specification;
                    }

                    @Override
                    public com.google.common.collect.ImmutableList<String>
                    getChargerTypes() {
                        return com.google.common.collect.ImmutableList.of();
                    }
                };


        // --------------------------------------------------
        // Create our factory
        // --------------------------------------------------

        FreightVehiclePayloadTracker tracker =
                new FreightVehiclePayloadTracker();

        FreightOhdeSlaskiDriveEnergyConsumptionFactory factory =
                new FreightOhdeSlaskiDriveEnergyConsumptionFactory(
                        vehicles,
                        tracker
                );


        // --------------------------------------------------
        // Factory creates energy model for BET_01
        // --------------------------------------------------

        var model =
                factory.create(electricVehicle);


        assertInstanceOf(
                FreightOhdeSlaskiDriveEnergyConsumption.class,
                model
        );

        FreightOhdeSlaskiDriveEnergyConsumption freightModel =
                (FreightOhdeSlaskiDriveEnergyConsumption) model;


        // Empty BET should initially be exactly 12,000 kg
        assertEquals(
                12000.0,
                freightModel.getTotalWeightKg(),
                0.001
        );


        // Simulate freight pickup
        tracker.addPayload(
                vehicle.getId(),
                8000.0
        );


        // Same model should now automatically see 20,000 kg
        assertEquals(
                20000.0,
                freightModel.getTotalWeightKg(),
                0.001
        );


        System.out.println(
                "BET self weight [kg] = 12000"
        );

        System.out.println(
                "BET payload [kg] = "
                        + freightModel.getGoodsWeightKg()
        );

        System.out.println(
                "BET total weight [kg] = "
                        + freightModel.getTotalWeightKg()
        );
    }
}


