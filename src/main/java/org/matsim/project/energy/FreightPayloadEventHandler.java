package org.matsim.project.energy;

import org.matsim.api.core.v01.Id;
import org.matsim.freight.carriers.CarrierShipment;
import org.matsim.freight.carriers.events.CarrierShipmentDeliveryEndEvent;
import org.matsim.freight.carriers.events.CarrierShipmentPickupEndEvent;
import org.matsim.freight.carriers.events.eventhandler.CarrierShipmentDeliveryEndEventHandler;
import org.matsim.freight.carriers.events.eventhandler.CarrierShipmentPickupEndEventHandler;

import java.util.Map;

/**
 * Updates freight vehicle payload from shipment pickup
 * and delivery events.
 */
public final class FreightPayloadEventHandler
        implements CarrierShipmentPickupEndEventHandler,
        CarrierShipmentDeliveryEndEventHandler {

    private final FreightVehiclePayloadTracker payloadTracker;

    /**
     * shipment ID -> physical shipment mass [kg]
     */
    private final Map<Id<CarrierShipment>, Double> shipmentMassKg;

    public FreightPayloadEventHandler(
            FreightVehiclePayloadTracker payloadTracker,
            Map<Id<CarrierShipment>, Double> shipmentMassKg) {

        this.payloadTracker = payloadTracker;
        this.shipmentMassKg = shipmentMassKg;
    }

    @Override
    public void handleEvent(
            CarrierShipmentPickupEndEvent event) {

        double massKg =
                getShipmentMassKg(event.getShipmentId());

        payloadTracker.addPayload(
                event.getVehicleId(),
                massKg
        );

        System.out.println(
                "BET PAYLOAD PICKUP"
                        + " | time=" + event.getTime()
                        + " | vehicle=" + event.getVehicleId()
                        + " | shipment=" + event.getShipmentId()
                        + " | added=" + massKg + " kg"
                        + " | payload="
                        + payloadTracker.getPayloadKg(
                        event.getVehicleId())
                        + " kg"
        );
    }

    @Override
    public void handleEvent(
            CarrierShipmentDeliveryEndEvent event) {

        double massKg =
                getShipmentMassKg(event.getShipmentId());

        payloadTracker.removePayload(
                event.getVehicleId(),
                massKg
        );

        System.out.println(
                "BET PAYLOAD DELIVERY"
                        + " | time=" + event.getTime()
                        + " | vehicle=" + event.getVehicleId()
                        + " | shipment=" + event.getShipmentId()
                        + " | removed=" + massKg + " kg"
                        + " | payload="
                        + payloadTracker.getPayloadKg(
                        event.getVehicleId())
                        + " kg"
        );
    }

    private double getShipmentMassKg(
            Id<CarrierShipment> shipmentId) {

        Double massKg = shipmentMassKg.get(shipmentId);

        if (massKg == null) {
            throw new IllegalStateException(
                    "No physical payload mass defined for shipment "
                            + shipmentId
            );
        }

        if (massKg < 0.0) {
            throw new IllegalStateException(
                    "Shipment mass cannot be negative for shipment "
                            + shipmentId
            );
        }

        return massKg;
    }

    @Override
    public void reset(int iteration) {
        payloadTracker.reset();
    }
}