package org.matsim.project.energy;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;

public final class FreightEnergyHandler
        implements LinkEnterEventHandler,
        LinkLeaveEventHandler,
        VehicleLeavesTrafficEventHandler {

    private final Network network;
    private final FreightVehiclePayloadTracker payloadTracker;
    private final double selfWeightKg;

    private final Map<Id<Vehicle>, Double> enterTimes =
            new HashMap<>();

    private final Map<Id<Vehicle>, Id<Link>> enteredLinks =
            new HashMap<>();

    private final Map<Id<Vehicle>,
            FreightOhdeSlaskiDriveEnergyConsumption> energyModels =
            new HashMap<>();

    private double totalEnergyJ = 0.0;
    private double totalDistanceM = 0.0;


    public FreightEnergyHandler(
            Network network,
            FreightVehiclePayloadTracker payloadTracker,
            double selfWeightKg) {

        this.network = network;
        this.payloadTracker = payloadTracker;
        this.selfWeightKg = selfWeightKg;
    }


    @Override
    public void handleEvent(LinkEnterEvent event) {

        Id<Vehicle> vehicleId = event.getVehicleId();

        if (!isFreightVehicle(vehicleId)) {
            return;
        }

        enterTimes.put(
                vehicleId,
                event.getTime()
        );

        enteredLinks.put(
                vehicleId,
                event.getLinkId()
        );
    }


    @Override
    public void handleEvent(LinkLeaveEvent event) {

        Id<Vehicle> vehicleId = event.getVehicleId();

        if (!isFreightVehicle(vehicleId)) {
            return;
        }

        /*
         * Normal network traversal:
         *
         * LinkEnter -> LinkLeave
         */
        processLinkTraversal(
                vehicleId,
                event.getLinkId(),
                event.getTime()
        );
    }


    @Override
    public void handleEvent(
            VehicleLeavesTrafficEvent event) {

        Id<Vehicle> vehicleId = event.getVehicleId();

        if (!isFreightVehicle(vehicleId)) {
            return;
        }

        /*
         * MATSim freight destination link:
         *
         * LinkEnter -> VehicleLeavesTraffic
         *
         * There may be no LinkLeaveEvent for this final link.
         */
        if (enterTimes.containsKey(vehicleId)) {

            processLinkTraversal(
                    vehicleId,
                    event.getLinkId(),
                    event.getTime()
            );
        }

        /*
         * Critical:
         * never allow an enter time to survive across
         * pickup/delivery service activities.
         */
        enterTimes.remove(vehicleId);
        enteredLinks.remove(vehicleId);
    }


    private void processLinkTraversal(
            Id<Vehicle> vehicleId,
            Id<Link> linkId,
            double leaveTime) {

        Double enterTime =
                enterTimes.remove(vehicleId);

        Id<Link> storedLinkId =
                enteredLinks.remove(vehicleId);

        if (enterTime == null || storedLinkId == null) {
            return;
        }

        /*
         * Protect against mismatched events.
         */
        if (!storedLinkId.equals(linkId)) {
            throw new IllegalStateException(
                    "Link event mismatch for vehicle "
                            + vehicleId
                            + ": entered "
                            + storedLinkId
                            + " but left "
                            + linkId
            );
        }

        Link link =
                network.getLinks().get(linkId);

        if (link == null) {
            throw new IllegalStateException(
                    "Network link not found: " + linkId
            );
        }

        double travelTime =
                leaveTime - enterTime;

        if (travelTime <= 0.0) {
            return;
        }

        FreightOhdeSlaskiDriveEnergyConsumption model =
                energyModels.computeIfAbsent(
                        vehicleId,
                        id ->
                                new FreightOhdeSlaskiDriveEnergyConsumption(
                                        id,
                                        selfWeightKg,
                                        payloadTracker
                                )
                );

        double payloadKg =
                payloadTracker.getPayloadKg(vehicleId);

        double energyJ =
                model.calcEnergyConsumption(
                        link,
                        travelTime,
                        enterTime
                );

        totalEnergyJ += energyJ;
        totalDistanceM += link.getLength();

        System.out.printf(
                "BET LINK: vehicle=%s link=%s "
                        + "length=%.1f m travelTime=%.1f s "
                        + "payload=%.1f kg totalMass=%.1f kg "
                        + "energy=%.4f kWh%n",
                vehicleId,
                link.getId(),
                link.getLength(),
                travelTime,
                payloadKg,
                selfWeightKg + payloadKg,
                energyJ / 3_600_000.0
        );
    }


    private boolean isFreightVehicle(
            Id<Vehicle> vehicleId) {

        return vehicleId.toString()
                .startsWith("freight_");
    }


    public double getTotalEnergyKWh() {

        return totalEnergyJ / 3_600_000.0;
    }


    public double getTotalDistanceKm() {

        return totalDistanceM / 1000.0;
    }


    public double getEnergyIntensityKWhPerKm() {

        double distanceKm =
                getTotalDistanceKm();

        if (distanceKm <= 0.0) {
            return 0.0;
        }

        return getTotalEnergyKWh()
                / distanceKm;
    }


    @Override
    public void reset(int iteration) {

        enterTimes.clear();
        enteredLinks.clear();
        energyModels.clear();

        totalEnergyJ = 0.0;
        totalDistanceM = 0.0;
    }
}
