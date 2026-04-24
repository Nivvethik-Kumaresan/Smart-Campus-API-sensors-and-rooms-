/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.csa_cw.resources;

/**
 *
 * @author nivve
 */
import com.mycompany.csa_cw.exceptions.SensorUnavailableException;
import com.mycompany.csa_cw.models.Sensor;
import com.mycompany.csa_cw.models.SensorReading;
import com.mycompany.csa_cw.util.DataStore;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

public class SensorReadingResource {

    private String sensorId;
    private DataStore dataStore = DataStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<SensorReading> getReadings() {
        // Fetch history for this specific sensor 
        return dataStore.getReadingsForSensor(sensorId);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addReading(SensorReading reading) {
        Sensor parentSensor = dataStore.getSensors().get(sensorId);

        // Task 5.3: Check if sensor is under maintenance 
        if (parentSensor != null && "MAINTENANCE".equalsIgnoreCase(parentSensor.getStatus())) {
            // Triggers SensorUnavailableMapper (403 Forbidden) 
            throw new SensorUnavailableException("Sensor '" + sensorId + "' is in MAINTENANCE and cannot accept readings.");
        }

        reading.setId(java.util.UUID.randomUUID().toString());
        reading.setTimestamp(System.currentTimeMillis());
        dataStore.addReading(sensorId, reading);

        if (parentSensor != null) {
            parentSensor.setCurrentValue(reading.getValue());
        }

        return Response.status(Response.Status.CREATED).entity(reading).build();
    }
}
