package org.matsim.project.energy;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;

public final class OhdeSlaskiDriveEnergyConsumption
        implements DriveEnergyConsumption {

    private static final double g = 9.81;
    private static final double m = 1525;
    private static final double m_s = m + 100;
    private static final double spr = 0.935;
    private static final double ft = 0.01;
    private static final double fa = 0.4193;
    private static final double cb = 0.191;

    private static final double a1 = -0.267;
    private static final double a2 = 0.99819;

    private static final int MAX_AVG_SPEED = 80;
    private static final int SPEED_STEPS_PER_UNIT = 10;
    private static final double ZERO_SPEED = 0.01;
    private static final double[] POWER;

    static {
        POWER = new double[MAX_AVG_SPEED * SPEED_STEPS_PER_UNIT];
        POWER[0] = calcPower(ZERO_SPEED);

        for (int i = 1; i < POWER.length; i++) {
            POWER[i] =
                    calcPower((double) i / SPEED_STEPS_PER_UNIT);
        }
    }

    private static double calcPower(double v) {
        return v * (
                ft * m_s * g
                        + fa * v * v
                        + cb * (a1 * Math.log(v) + a2) * m_s
        ) / spr;
    }

    @Override
    public double calcEnergyConsumption(
            Link link,
            double travelTime,
            double linkEnterTime) {

        if (travelTime == 0) {
            return 0;
        }

        double avgSpeed =
                link.getLength() / travelTime;

        int idx =
                (int) Math.round(
                        avgSpeed * SPEED_STEPS_PER_UNIT);

        return POWER[idx] * travelTime;
    }
}

