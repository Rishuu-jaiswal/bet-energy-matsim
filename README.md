
# BET Freight Energy Model in MATSim

This repository contains the implementation of a battery-electric truck (BET) energy consumption model integrated with MATSim freight simulation.

The main objective is to calculate link-level truck energy consumption while considering vehicle mass, changing freight payload, travel conditions, and road grade.

## Main Features

The implementation considers:

- Vehicle self-weight
- Dynamic freight payload
- Total vehicle mass
- Link distance
- Link travel time
- Rolling resistance
- Aerodynamic resistance
- Inertial resistance
- Road grade
- Drivetrain efficiency
- Regenerative braking

## Dynamic Vehicle Mass

Vehicle mass is calculated as:

```text
total vehicle mass = truck self-weight + current payload
```

The payload is updated during the freight tour using MATSim freight events.

For the current Sioux Falls experiment:

```text
Truck self-weight = 12,000 kg
Shipment payload = 8,000 kg
Loaded vehicle mass = 20,000 kg
Empty vehicle mass = 12,000 kg
```

These mass values are assumptions used for the controlled experiment.

## Sioux Falls Test Scenario

The model was tested using the Sioux Falls MATSim road network.

The same road-network topology is used for both experiments:

1. Flat-road case
2. Road-grade case

For the second case, a `grade` attribute was added to the existing network links. Therefore, `network-with-grade.xml` is not a different road network; it is the same network with additional grade information.

The current grade values are synthetic and are used to test the sensitivity of the energy model to road slope.

Examples:

```text
grade =  0.05  -> 5% uphill
grade =  0.00  -> flat
grade = -0.05  -> 5% downhill
```

## Freight Simulation

The freight scenario contains a carrier, freight vehicle, and shipment.

MATSim/jsprit generates the freight vehicle tour. During simulation, the energy model reads the actual link traversal events.

Payload changes are tracked during pickup and delivery so that energy is calculated using the appropriate vehicle mass.

## Important Files

### Energy model

`FreightOhdeSlaskiDriveEnergyConsumption.java`

Calculates BET energy consumption for each travelled link. It contains the physical resistance terms, road-grade contribution, drivetrain efficiency, and regenerative braking treatment.

### Energy event handler

`FreightEnergyHandler.java`

Reads MATSim link events, obtains link travel time and distance, calls the energy model, and accumulates total energy and distance.

### Payload tracker

`FreightVehiclePayloadTracker.java`

Stores the current freight payload of each vehicle.

### Payload event handler

`FreightPayloadEventHandler.java`

Updates vehicle payload when freight pickup and delivery events occur.

### Freight simulation runner

`RunSiouxFallsFreight.java`

Loads the Sioux Falls scenario and freight inputs, generates the freight solution, registers the payload and energy handlers, runs MATSim, and reports the BET energy results.

### Grade network generator

`CreateGradedSiouxFallsNetwork.java`

Adds the synthetic `grade` attribute to links of the Sioux Falls network.

### Event inspection

`InspectFreightEvents.java`

Was used to inspect MATSim freight events and verify vehicle movement through the network.

## Results

### Flat network

```text
Distance = 35.84 km
Energy consumption = 21.627 kWh
Energy intensity = 0.6034 kWh/km

### Network with synthetic grade

```text
Distance = 35.84 km
Energy consumption = 21.523 kWh
Energy intensity = 0.6005 kWh/km

The distance is unchanged because the same network topology and freight tour are used.

The energy consumption changes because the second experiment includes road-grade forces and regenerative braking.

Individual downhill links can produce negative traction-energy values when recovered regenerative energy exceeds propulsion demand on that link.

## Building the Project

On Windows:

```cmd
mvnw.cmd compile
```

## Running the Sioux Falls Freight Experiment

```cmd
mvnw.cmd exec:java -Dexec.mainClass=org.matsim.project.sioux.RunSiouxFallsFreight
```

## Tests

Energy-related tests are located under:

```text
src/test/java/org/matsim/project/energy/
```

The tests cover the freight energy calculation, payload tracking, factory implementation, and grade-dependent behaviour.

## Current Limitations

The Sioux Falls experiment is currently a controlled implementation test rather than a real-world validation.

In particular:

- Road grades are synthetic.
- Truck self-weight and shipment mass are experimental assumptions.
- The energy-model parameters still require appropriate calibration/validation for a real BET application.

The next step is therefore to apply the model to a real-world freight scenario containing realistic freight demand, vehicle characteristics, and elevation-derived road grades, and compare the resulting energy consumption against suitable reference or observed data.