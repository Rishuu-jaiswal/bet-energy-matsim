package org.matsim.project.energy;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.vehicles.Vehicle;

/**
 * Link-based drive-energy-consumption model for
 * battery-electric freight vehicles in MATSim.
 *
 * The model considers:
 *
 * 1. Dynamic vehicle mass
 *    m = selfWeight + currentPayload
 *
 * 2. Rolling resistance
 *
 * 3. Aerodynamic resistance
 *
 * 4. Empirical truck acceleration as a function
 *    of link-average speed
 *
 * 5. Inertia resistance
 *
 * 6. Road grade
 *
 * 7. Propulsion drivetrain losses
 *
 * 8. Regenerative energy when calculated wheel
 *    power becomes negative
 *
 * Grade convention:
 *
 *  0.05 = +5% uphill
 *  0.00 = flat
 * -0.05 = -5% downhill
 */
public final class FreightOhdeSlaskiDriveEnergyConsumption
        implements DriveEnergyConsumption {

    // --------------------------------------------------
    // Physical constants
    // --------------------------------------------------

    private static final double G = 9.81;

    /**
     * Standard air density [kg/m3].
     */
    private static final double AIR_DENSITY = 1.225;


    // --------------------------------------------------
    // Vehicle-specific information
    // --------------------------------------------------

    private final Id<Vehicle> vehicleId;

    /**
     * Base/self vehicle mass supplied by the caller [kg].
     *
     * Total mass used by the energy model is:
     *
     * m = selfWeightKg + current payload
     */
    private final double selfWeightKg;

    /**
     * Provides the current freight payload [kg].
     */
    private final FreightVehiclePayloadTracker payloadTracker;


    // --------------------------------------------------
    // BET model parameters
    // --------------------------------------------------

    /**
     * Constant propulsion drivetrain efficiency [-].
     *
     * This is currently treated as a modelling parameter
     * and should be documented and checked through
     * sensitivity analysis / validation.
     */
    private static final double DRIVETRAIN_EFFICIENCY = 0.91;

    /**
     * Regenerative efficiency [-].
     *
     * When calculated wheel power becomes negative,
     * 80% of that mechanical energy is assumed to reach
     * the battery.
     *
     * This does not explicitly reconstruct individual
     * braking manoeuvres.
     */
    private static final double REGEN_EFFICIENCY = 0.80;

    /**
     * Rolling resistance coefficient [-].
     */
    private static final double ROLLING_COEFFICIENT = 0.0052;

    /**
     * Aerodynamic drag area CdA [m2].
     */
    private static final double DRAG_AREA_CDA = 5.63;

    /**
     * Empirical inertia scaling inherited from the
     * Ohde-Slaski average-speed formulation.
     *
     * This value has NOT yet been truck-calibrated.
     * It must be investigated and validated separately.
     */
    private static final double INERTIA_COEFFICIENT = 0.191;


    // --------------------------------------------------
    // Truck-calibrated acceleration relationship
    // --------------------------------------------------

    /**
     * a(v) = A1 * ln(v) + A2
     *
     * v must be in m/s.
     *
     * The coefficients were calibrated from truck
     * probe data using median positive acceleration
     * in sustained positive-acceleration episodes.
     *
     * Calibration range:
     * approximately 20-95 km/h.
     *
     * Values outside this range are extrapolated.
     */
    private static final double A1 = -0.1968093906;
    private static final double A2 =  0.7202936833;

    /**
     * Prevents ln(0).
     */
    private static final double ZERO_SPEED = 0.01;


    // --------------------------------------------------
    // MATSim link attributes
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

        if (selfWeightKg <= 0.0) {
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
     * Returns current freight payload [kg].
     */
    public double getGoodsWeightKg() {

        return payloadTracker.getPayloadKg(vehicleId);
    }


    /**
     * Returns current total vehicle mass [kg].
     *
     * m = m_self + m_payload
     */
    public double getTotalWeightKg() {

        return selfWeightKg
                + payloadTracker.getPayloadKg(vehicleId);
    }


    // --------------------------------------------------
    // Road grade
    // --------------------------------------------------

    /**
     * Reads road grade from the MATSim link.
     *
     * Examples:
     *
     *  0.05 = +5% uphill
     *  0.00 = flat
     * -0.05 = -5% downhill
     *
     * A missing grade attribute is interpreted as flat.
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
    // Battery power calculation
    // --------------------------------------------------

    /**
     * Calculates battery-side power [W].
     *
     * Positive:
     * battery discharge.
     *
     * Negative:
     * battery recuperation.
     */
    private double calculatePower(
            double speed,
            Link link) {

        double safeSpeed =
                Math.max(speed, ZERO_SPEED);


        // --------------------------------------------------
        // Dynamic total vehicle mass
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
         * theta = atan(grade)
         */
        double roadAngle =
                Math.atan(grade);


        // --------------------------------------------------
        // Rolling resistance
        // --------------------------------------------------

        /*
         * F_roll =
         *
         * Crr * m * g * cos(theta)
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
         * F_aero =
         *
         * 0.5 * rho * CdA * v^2
         */
        double aerodynamicResistance =
                0.5
                        * AIR_DENSITY
                        * DRAG_AREA_CDA
                        * safeSpeed
                        * safeSpeed;


        // --------------------------------------------------
        // Truck-calibrated acceleration approximation
        // --------------------------------------------------

        /*
         * a(v) =
         *
         * A1 * ln(v) + A2
         */
        double acceleration =
                A1 * Math.log(safeSpeed)
                        + A2;


        // --------------------------------------------------
        // Inertia resistance
        // --------------------------------------------------

        /*
         * F_inertia =
         *
         * cb * m * a(v)
         *
         * IMPORTANT:
         *
         * cb = 0.191 is currently inherited from the
         * original average-speed formulation.
         *
         * It still requires truck-specific investigation.
         */
        double inertiaResistance =
                INERTIA_COEFFICIENT
                        * totalWeightKg
                        * acceleration;


        // --------------------------------------------------
        // Grade resistance
        // --------------------------------------------------

        /*
         * F_grade =
         *
         * m * g * sin(theta)
         *
         * uphill:
         * positive
         *
         * downhill:
         * negative
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
         * P_wheel =
         *
         * v * F_total
         */
        double wheelPower =
                safeSpeed
                        * totalResistance;


        // --------------------------------------------------
        // Battery-side power
        // --------------------------------------------------

        if (wheelPower >= 0.0) {

            /*
             * Propulsion:
             *
             * P_battery =
             *
             * P_wheel / eta_drive
             */
            return wheelPower
                    / DRIVETRAIN_EFFICIENCY;

        } else {

            /*
             * Regeneration:
             *
             * P_battery =
             *
             * P_wheel * eta_regen
             *
             * wheelPower is negative, therefore the
             * resulting battery power remains negative.
             */
            return wheelPower
                    * REGEN_EFFICIENCY;
        }
    }


    // --------------------------------------------------
    // MATSim link energy calculation
    // --------------------------------------------------

    /**
     * Calculates battery energy consumed or recovered
     * during traversal of one MATSim link.
     *
     * Average link speed:
     *
     * v = linkLength / travelTime
     *
     * Energy:
     *
     * E = P_battery * travelTime
     *
     * Positive energy:
     * battery consumption.
     *
     * Negative energy:
     * recuperated battery energy.
     *
     * @return battery energy [J]
     */
    @Override
    public double calcEnergyConsumption(
            Link link,
            double travelTime,
            double linkEnterTime) {

        if (travelTime <= 0.0) {
            return 0.0;
        }


        // Average MATSim link speed [m/s]
        double averageSpeed =
                link.getLength()
                        / travelTime;


        // Battery-side power [W]
        double batteryPower =
                calculatePower(
                        averageSpeed,
                        link
                );


        // Link battery energy [J]
        return batteryPower
                * travelTime;
    }
}