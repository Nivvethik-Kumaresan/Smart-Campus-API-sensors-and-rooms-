/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.csa_cw.resources;

/**
 *
 * @author nivve
 */
import com.mycompany.csa_cw.exceptions.LinkedResourceNotFoundException;
import com.mycompany.csa_cw.models.Sensor;
import com.mycompany.csa_cw.models.Room;
import com.mycompany.csa_cw.util.DataStore;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
public class SensorResource {

    private DataStore dataStore = DataStore.getInstance();

    @GET
    public List<Sensor> getSensors(@QueryParam("type") String type) {
        List<Sensor> allSensors = new ArrayList<>(dataStore.getSensors().values());

        // Task 3.2: Filtered Retrieval based on query parameter 
        if (type != null && !type.isEmpty()) {
            return allSensors.stream()
                    .filter(s -> s.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }
        return allSensors;
    }

    @GET
    @Path("/{sensorId}")
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = dataStore.getSensors().get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(sensor).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response registerSensor(Sensor sensor) {
        Room room = dataStore.getRooms().get(sensor.getRoomId());

        // Task 5.2: Validate that the Room actually exists 
        if (room == null) {
            // Triggers LinkedResourceMapper (422 Unprocessable Entity) 
            throw new LinkedResourceNotFoundException("Cannot link sensor: Room ID '" + sensor.getRoomId() + "' not found.");
        }

        dataStore.getSensors().put(sensor.getId(), sensor);
        room.getSensorIds().add(sensor.getId());

        return Response.status(Response.Status.CREATED).entity(sensor).build();
    }

    //Task 4.1: Sub-Resource Locator Pattern 
    @Path("{sensorId}/readings")
    public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
        // Validation: Ensure the sensor exists before delegating
        if (!dataStore.getSensors().containsKey(sensorId)) {
            throw new WebApplicationException("Sensor not found", Response.Status.NOT_FOUND);
        }

        // Return a new instance of the sub-resource for this specific sensor 
        return new SensorReadingResource(sensorId);
    }
}
