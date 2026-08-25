package org.matsim.project;

import org.apache.logging.log4j.core.tools.picocli.CommandLine;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.ev.EvModule;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.freight.carriers.CarrierShipment;
import org.matsim.project.energy.FreightBetEnergyModule;
import org.matsim.project.energy.FreightVehiclePayloadTracker;

import java.util.HashMap;
import java.util.Map;


@CommandLine.Command(
		header = ":: MyScenario ::",
		version = "1.0"
)
public class MatsimModelImplementation
		extends MATSimApplication {


	/*
	 * Shared payload tracker.
	 *
	 * Freight events update this object.
	 * The BET energy model reads from the same object.
	 */
	private final FreightVehiclePayloadTracker payloadTracker =
			new FreightVehiclePayloadTracker();


	/*
	 * Mapping:
	 *
	 * shipment ID -> physical goods mass [kg]
	 *
	 * This will later be filled using the real freight input.
	 */
	private final Map<Id<CarrierShipment>, Double> shipmentMassKg =
			new HashMap<>();


	public MatsimModelImplementation() {
		super();
	}


	public static void main(String[] args) {

		MATSimApplication.execute(
				MatsimModelImplementation.class,
				"--config",
				"scenarios/equil/config-2026.xml"
		);
	}


	@Override
	protected Config prepareConfig(Config config) {

		config.controller().setOverwriteFileSetting(
				OverwriteFileSetting.deleteDirectoryIfExists
		);

		return config;
	}


	@Override
	protected void prepareScenario(Scenario scenario) {

		/*
		 * TEST GRADES
		 *
		 * Grade convention:
		 *  0.05 = +5% uphill
		 *  0.03 = +3% uphill
		 * -0.05 = -5% downhill
		 *  0.00 = flat
		 */

		// Set all links to flat first
		scenario.getNetwork()
				.getLinks()
				.values()
				.forEach(link ->
						link.getAttributes()
								.putAttribute("grade", 0.0)
				);

		// Link 1: +5% uphill
		scenario.getNetwork()
				.getLinks()
				.get(Id.createLinkId("1"))
				.getAttributes()
				.putAttribute("grade", 0.05);

		// Link 2: +3% uphill
		scenario.getNetwork()
				.getLinks()
				.get(Id.createLinkId("2"))
				.getAttributes()
				.putAttribute("grade", 0.03);

		// Link 3: -5% downhill
		scenario.getNetwork()
				.getLinks()
				.get(Id.createLinkId("3"))
				.getAttributes()
				.putAttribute("grade", -0.05);
	}


	@Override
	protected void prepareControler(Controler controler) {

		/*
		 * Standard MATSim EV contrib.
		 */
		controler.addOverridingModule(
				new EvModule()
		);


		/*
		 * Our custom BET freight energy module.
		 *
		 * This connects:
		 *
		 * Freight events
		 *      ↓
		 * Payload tracker
		 *      ↓
		 * Custom Ohde-Slaski model
		 */
		controler.addOverridingModule(
				new FreightBetEnergyModule(
						controler.getScenario().getVehicles(),
						payloadTracker,
						shipmentMassKg
				)
		);
	}
}