package org.matsim.project.energy;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.vehicles.Vehicle;

/**
 * Ohde-Slaski based drive-energy-consumption model
 * extended for battery-electric freight vehicles.
 *
 * Extensions:
 * 1. Dynamic mass:
 *      m = self weight + current goods weight
 *
 * 2. Road grade:
 *      F_roll  = Cr * m * g * cos(theta)
 *      F_grade = m * g * sin(theta)
 *
 * 3. Regenerative braking:
 *      positive wheel power -> battery discharge
 *      negative wheel power -> battery recuperation
 *
 * Grade convention:
 *      0.05  = +5% uphill
 *      0.00  = flat
 *     -0.05  = -5% downhill
 *
 * theta = atan(grade)
 */
public final class FreightOhdeSlaskiDriveEnergyConsumption
        implements DriveEnergyConsumption {

    // --------------------------------------------------
    // Physical constant
    // --------------------------------------------------

    private static final double G = 9.81;


    // --------------------------------------------------
    // Vehicle-specific information
    // --------------------------------------------------

    private final Id<Vehicle> vehicleId;

    /**
     * Empty/self mass of the BET [kg].
     */
    private final double selfWeightKg;

    /**
     * Provides current freight payload [kg].
     */
    private final FreightVehiclePayloadTracker payloadTracker;


    // --------------------------------------------------
    // Ohde-Slaski parameters
    // --------------------------------------------------

    /**
     * Propulsion drivetrain efficiency [-].
     */
    private static final double DRIVETRAIN_EFFICIENCY = 0.935;

    /**
     * Regenerative braking efficiency [-].
     *
     * 0.70 means that 70% of available negative
     * mechanical wheel energy reaches the battery.
     *
     * This should later be calibrated/documented.
     */
    private static final double REGEN_EFFICIENCY = 0.70;

    private static final double ROLLING_COEFFICIENT = 0.01;

    private static final double AERODYNAMIC_COEFFICIENT = 0.4193;

    private static final double INERTIA_COEFFICIENT = 0.191;


    // --------------------------------------------------
    // Acceleration approximation
    // --------------------------------------------------

    /*
     * a(v) = A1 * ln(v) + A2
     */
    private static final double A1 = -0.267;

    private static final double A2 = 0.99819;

    private static final double ZERO_SPEED = 0.01;


    // --------------------------------------------------
    // Link attributes
    // --------------------------------------------------

    private static final String GRADE_ATTRIBUTE = "grade";


    // --------------------------------------------------
    // Constructor
    // --------------------------------------------------

    public FreightOhdeSlaskiDriveEnergyConsumption(
            Id<Vehicle> vehicleId,
            double selfWeightKg,
            FreightVehiclePayloadTracker payloadTracker) {

        if (vehicleId == null) {
            throw new IllegalArgumentException(
                    "Vehicle ID cannot be null."
            );
        }

        if (selfWeightKg <= 0) {
            throw new IllegalArgumentException(
                    "Vehicle self-weight must be greater than zero."
            );
        }

        if (payloadTracker == null) {
            throw new IllegalArgumentException(
                    "Payload tracker cannot be null."
            );
        }

        this.vehicleId = vehicleId;
        this.selfWeightKg = selfWeightKg;
        this.payloadTracker = payloadTracker;
    }


    // --------------------------------------------------
    // Vehicle mass
    // --------------------------------------------------

    /**
     * Current freight payload [kg].
     */
    public double getGoodsWeightKg() {

        return payloadTracker.getPayloadKg(vehicleId);
    }


    /**
     * Current total vehicle mass [kg].
     *
     * m = m_self + m_goods
     */
    public double getTotalWeightKg() {

        return selfWeightKg
                + payloadTracker.getPayloadKg(vehicleId);
    }


    // --------------------------------------------------
    // Grade
    // --------------------------------------------------

    /**
     * Reads the grade from the MATSim link.
     *
     *  0.05 = +5% uphill
     *  0.00 = flat
     * -0.05 = -5% downhill
     *
     * Missing grade -> flat road.
     */
    public double getGrade(Link link) {

        Object gradeAttribute =
                link.getAttributes()
                        .getAttribute(GRADE_ATTRIBUTE);

        if (gradeAttribute == null) {
            return 0.0;
        }

        return Double.parseDouble(
                gradeAttribute.toString()
        );
    }


    // --------------------------------------------------
    // Power calculation
    // --------------------------------------------------

    /**
     * Calculates battery-side drive power [W].
     *
     * Positive power:
     * battery is discharged.
     *
     * Negative power:
     * battery is recuperating energy.
     */
    private double calculatePower(
            double speed,
            Link link) {

        double safeSpeed =
                Math.max(speed, ZERO_SPEED);


        // --------------------------------------------------
        // Dynamic mass
        // --------------------------------------------------

        double totalWeightKg =
                getTotalWeightKg();


        // --------------------------------------------------
        // Road grade
        // --------------------------------------------------

        double grade =
                getGrade(link);

        /*
         * grade = tan(theta)
         *
         * therefore:
         *
         * theta = atan(grade)
         */
        double roadAngle =
                Math.atan(grade);


        // --------------------------------------------------
        // Rolling resistance
        // --------------------------------------------------

        /*
         * F_roll = Cr * m * g * cos(theta)
         */
        double rollingResistance =
                ROLLING_COEFFICIENT
                        * totalWeightKg
                        * G
                        * Math.cos(roadAngle);


        // --------------------------------------------------
        // Aerodynamic resistance
        // --------------------------------------------------

        /*
         * F_aero = fa * v^2
         */
        double aerodynamicResistance =
                AERODYNAMIC_COEFFICIENT
                        * safeSpeed
                        * safeSpeed;


        // --------------------------------------------------
        // Acceleration approximation
        // --------------------------------------------------

        /*
         * a(v) = A1 * ln(v) + A2
         */
        double acceleration =
                A1 * Math.log(safeSpeed)
                        + A2;


        // --------------------------------------------------
        // Inertia resistance
        // --------------------------------------------------

        /*
         * F_inertia = cb * a(v) * m
         *
         * If acceleration becomes negative,
         * this term becomes negative.
         */
        double inertiaResistance =
                INERTIA_COEFFICIENT
                        * acceleration
                        * totalWeightKg;


        // --------------------------------------------------
        // Grade resistance
        // --------------------------------------------------

        /*
         * F_grade = m * g * sin(theta)
         *
         * uphill:
         * F_grade > 0
         *
         * downhill:
         * F_grade < 0
         */
        double gradeResistance =
                totalWeightKg
                        * G
                        * Math.sin(roadAngle);


        // --------------------------------------------------
        // Total longitudinal force
        // --------------------------------------------------

        /*
         * F_total =
         *
         * F_roll
         * + F_aero
         * + F_inertia
         * + F_grade
         */
        double totalResistance =
                rollingResistance
                        + aerodynamicResistance
                        + inertiaResistance
                        + gradeResistance;


        // --------------------------------------------------
        // Mechanical wheel power
        // --------------------------------------------------

        /*
         * P_wheel = v * F_total
         *
         * P_wheel > 0:
         * propulsion required
         *
         * P_wheel < 0:
         * braking energy available
         */
        double wheelPower =
                safeSpeed
                        * totalResistance;


        // --------------------------------------------------
        // Battery-side power
        // --------------------------------------------------

        if (wheelPower >= 0.0) {

            /*
             * PROPULSION
             *
             * Battery must provide more power than
             * reaches the wheels because of drivetrain losses.
             *
             * P_battery = P_wheel / eta_drive
             */
            return wheelPower
                    / DRIVETRAIN_EFFICIENCY;

        } else {

            /*
             * REGENERATIVE BRAKING
             *
             * Only part of the available mechanical
             * braking energy reaches the battery.
             *
             * P_battery = P_wheel * eta_regen
             *
             * The result remains negative.
             */
            return wheelPower
                    * REGEN_EFFICIENCY;
        }
    }


    // --------------------------------------------------
    // MATSim energy calculation
    // --------------------------------------------------

    /**
     * Calculates battery energy consumed/recovered
     * while traversing one MATSim link.
     *
     * E = P * t
     *
     * Positive energy:
     * battery discharge.
     *
     * Negative energy:
     * battery recuperation.
     *
     * @return battery energy [J]
     */
    @Override
    public double calcEnergyConsumption(
            Link link,
            double travelTime,
            double linkEnterTime) {

        if (travelTime <= 0) {
            return 0.0;
        }


        // Average speed [m/s]
        double averageSpeed =
                link.getLength()
                        / travelTime;


        // Battery-side power [W]
        double batteryPower =
                calculatePower(
                        averageSpeed,
                        link
                );


        // Battery energy [J]
        return batteryPower
                * travelTime;
    }
}