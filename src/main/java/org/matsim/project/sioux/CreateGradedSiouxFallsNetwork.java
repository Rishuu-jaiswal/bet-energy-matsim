package org.matsim.project.sioux;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class CreateGradedSiouxFallsNetwork {

    public static void main(String[] args) {

        String inputNetwork =
                "scenarios/siouxfalls-bet/network.xml";

        String outputNetwork =
                "scenarios/siouxfalls-bet/network-with-grade.xml";

        Config config = ConfigUtils.createConfig();
        config.network().setInputFile(inputNetwork);

        Scenario scenario =
                ScenarioUtils.loadScenario(config);

        Network network =
                scenario.getNetwork();

        /*
         * Controlled synthetic grade experiment.
         *
         * Grade convention:
         *  0.05 = +5% uphill
         * -0.05 = -5% downhill
         *
         * IMPORTANT:
         * These are experimental grades, NOT measured
         * Sioux Falls terrain elevations.
         */

        for (Link link : network.getLinks().values()) {

            double grade = 0.0;

            /*
             * Assign grade according to vertical direction
             * in the abstract Sioux Falls coordinate system.
             *
             * Increasing y -> +5% uphill
             * Decreasing y -> -5% downhill
             * Same y       -> flat
             */

            double fromY =
                    link.getFromNode().getCoord().getY();

            double toY =
                    link.getToNode().getCoord().getY();

            if (toY > fromY) {
                grade = 0.05;
            } else if (toY < fromY) {
                grade = -0.05;
            }

            link.getAttributes().putAttribute(
                    "grade",
                    grade
            );

            System.out.printf(
                    "GRADE: link=%s fromY=%.1f toY=%.1f grade=%.3f%n",
                    link.getId(),
                    fromY,
                    toY,
                    grade
            );
        }

        new NetworkWriter(network)
                .write(outputNetwork);

        System.out.println();
        System.out.println(
                "Graded network written to: "
                        + outputNetwork
        );
    }
}