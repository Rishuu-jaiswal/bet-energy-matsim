package org.matsim.project.sioux;

import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

public class InspectFreightEvents {

    public static void main(String[] args) {

        String eventsFile =
                "output-freight-baseline/output_events.xml.zst";

        EventsManager events = EventsUtils.createEventsManager();

        events.addHandler(new Handler());

        new MatsimEventsReader(events)
                .readFile(eventsFile);
    }

    private static class Handler
            implements LinkEnterEventHandler,
            LinkLeaveEventHandler,
            VehicleEntersTrafficEventHandler,
            VehicleLeavesTrafficEventHandler {

        private boolean freight(String vehicleId) {
            return vehicleId.startsWith("freight_");
        }

        @Override
        public void handleEvent(VehicleEntersTrafficEvent event) {

            if (freight(event.getVehicleId().toString())) {
                System.out.printf(
                        "%.1f ENTER_TRAFFIC vehicle=%s link=%s%n",
                        event.getTime(),
                        event.getVehicleId(),
                        event.getLinkId()
                );
            }
        }

        @Override
        public void handleEvent(LinkEnterEvent event) {

            if (freight(event.getVehicleId().toString())) {
                System.out.printf(
                        "%.1f LINK_ENTER vehicle=%s link=%s%n",
                        event.getTime(),
                        event.getVehicleId(),
                        event.getLinkId()
                );
            }
        }

        @Override
        public void handleEvent(LinkLeaveEvent event) {

            if (freight(event.getVehicleId().toString())) {
                System.out.printf(
                        "%.1f LINK_LEAVE vehicle=%s link=%s%n",
                        event.getTime(),
                        event.getVehicleId(),
                        event.getLinkId()
                );
            }
        }

        @Override
        public void handleEvent(VehicleLeavesTrafficEvent event) {

            if (freight(event.getVehicleId().toString())) {
                System.out.printf(
                        "%.1f LEAVE_TRAFFIC vehicle=%s link=%s%n",
                        event.getTime(),
                        event.getVehicleId(),
                        event.getLinkId()
                );
            }
        }

        @Override
        public void reset(int iteration) {
        }
    }
}