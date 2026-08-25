package org.matsim.project;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class RunSiouxFallsBaseline {

    public static void main(String[] args) {

        String configFile =
                "scenarios/siouxfalls-bet/config.xml";

        // Load Sioux Falls configuration
        Config config =
                ConfigUtils.loadConfig(configFile);

        // Load the scenario/network
        Scenario scenario =
                ScenarioUtils.loadScenario(config);

        System.out.println(
                "Sioux Falls nodes = "
                        + scenario.getNetwork().getNodes().size()
        );

        System.out.println(
                "Sioux Falls links = "
                        + scenario.getNetwork().getLinks().size()
        );

        System.out.println(
                "Sioux Falls population = "
                        + scenario.getPopulation().getPersons().size()
        );

        System.out.println(
                "SIOUX FALLS BASELINE LOADED SUCCESSFULLY"
        );
    }
}

