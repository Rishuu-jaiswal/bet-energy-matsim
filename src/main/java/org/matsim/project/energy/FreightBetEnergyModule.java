package org.matsim.project.energy;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.core.controler.AbstractModule;
import org.matsim.freight.carriers.CarrierShipment;
import org.matsim.vehicles.Vehicles;

import java.util.Map;

public final class FreightBetEnergyModule extends AbstractModule {

    private final Vehicles vehicles;
    private final FreightVehiclePayloadTracker payloadTracker;
    private final Map<Id<CarrierShipment>, Double> shipmentMassKg;

    public FreightBetEnergyModule(
            Vehicles vehicles,
            FreightVehiclePayloadTracker payloadTracker,
            Map<Id<CarrierShipment>, Double> shipmentMassKg) {

        this.vehicles = vehicles;
        this.payloadTracker = payloadTracker;
        this.shipmentMassKg = shipmentMassKg;
    }

    @Override
    public void install() {

        // --------------------------------------------------
        // Custom EV drive-energy model
        // --------------------------------------------------

        FreightOhdeSlaskiDriveEnergyConsumptionFactory driveFactory =
                new FreightOhdeSlaskiDriveEnergyConsumptionFactory(
                        vehicles,
                        payloadTracker
                );

        bind(DriveEnergyConsumption.Factory.class)
                .toInstance(driveFactory);


        // --------------------------------------------------
        // Freight payload event handler
        // --------------------------------------------------

        FreightPayloadEventHandler payloadEventHandler =
                new FreightPayloadEventHandler(
                        payloadTracker,
                        shipmentMassKg
                );

        addEventHandlerBinding()
                .toInstance(payloadEventHandler);
    }
}
