package org.matsim.project.sioux;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.CarrierShipment;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;
import org.matsim.freight.carriers.controller.CarrierModule;
import org.matsim.project.energy.FreightEnergyHandler;
import org.matsim.project.energy.FreightPayloadEventHandler;
import org.matsim.project.energy.FreightVehiclePayloadTracker;

import java.util.HashMap;
import java.util.Map;

public class RunSiouxFallsFreight {

    public static void main(String[] args) throws Exception {

        String configFile;

        if (args.length > 0) {
            configFile = args[0];
        } else {
            configFile = "scenarios/siouxfalls-bet/config.xml";
        }

        System.out.println("=================================");
        System.out.println("BET EXPERIMENT");
        System.out.println("Config = " + configFile);
        System.out.println("=================================");

        // --------------------------------------------------
        // Load MATSim configuration
        // --------------------------------------------------

        Config config =
                ConfigUtils.loadConfig(
                        configFile,
                        new FreightCarriersConfigGroup()
                );

        // --------------------------------------------------
        // Load scenario
        // --------------------------------------------------

        Scenario scenario =
                ScenarioUtils.loadScenario(config);

        // --------------------------------------------------
        // Load freight input
        // --------------------------------------------------

        CarriersUtils.loadCarriersAccordingToFreightConfig(
                scenario
        );

        System.out.println(
                "Network links = "
                        + scenario.getNetwork().getLinks().size()
        );

        System.out.println(
                "Carriers loaded = "
                        + CarriersUtils
                        .getCarriers(scenario)
                        .getCarriers()
                        .size()
        );

        // --------------------------------------------------
        // Solve freight routing problem
        // --------------------------------------------------

        CarriersUtils.runJsprit(scenario);

        System.out.println(
                "JSPRIT FREIGHT SOLUTION CREATED"
        );

        // --------------------------------------------------
        // Controller
        // --------------------------------------------------

        Controler controler =
                new Controler(scenario);

        controler.addOverridingModule(
                new CarrierModule()
        );

        // --------------------------------------------------
        // Dynamic payload tracker
        // --------------------------------------------------

        FreightVehiclePayloadTracker payloadTracker =
                new FreightVehiclePayloadTracker();

        // --------------------------------------------------
        // Physical shipment masses [kg]
        // --------------------------------------------------

        Map<Id<CarrierShipment>, Double> shipmentMassKg =
                new HashMap<>();

        shipmentMassKg.put(
                Id.create(
                        "shipment1",
                        CarrierShipment.class
                ),
                8_000.0
        );

        // --------------------------------------------------
        // Payload event handler
        // --------------------------------------------------

        FreightPayloadEventHandler payloadEventHandler =
                new FreightPayloadEventHandler(
                        payloadTracker,
                        shipmentMassKg
                );

        controler.getEvents().addHandler(
                payloadEventHandler
        );

        // --------------------------------------------------
        // BET energy handler
        // --------------------------------------------------

        double truckSelfWeightKg =
                12_000.0;

        FreightEnergyHandler energyHandler =
                new FreightEnergyHandler(
                        scenario.getNetwork(),
                        payloadTracker,
                        truckSelfWeightKg
                );

        controler.getEvents().addHandler(
                energyHandler
        );

        // --------------------------------------------------
        // Run MATSim
        // --------------------------------------------------

        controler.run();

        // --------------------------------------------------
        // BET results
        // --------------------------------------------------

        double energyKWh =
                energyHandler.getTotalEnergyKWh();

        double distanceKm =
                energyHandler.getTotalDistanceKm();

        double energyIntensity =
                energyHandler.getEnergyIntensityKWhPerKm();

        System.out.println();
        System.out.println(
                "================================="
        );

        System.out.println(
                "BET ENERGY RESULTS"
        );

        System.out.println(
                "Energy consumption = "
                        + energyKWh
                        + " kWh"
        );

        System.out.println(
                "Distance = "
                        + distanceKm
                        + " km"
        );

        System.out.println(
                "Energy intensity = "
                        + energyIntensity
                        + " kWh/km"
        );

        System.out.println(
                "================================="
        );
    }
}