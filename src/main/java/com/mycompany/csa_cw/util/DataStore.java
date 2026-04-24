/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.csa_cw.util;

/**
 *
 * @author nivve
 */
import com.mycompany.csa_cw.models.Room;
import com.mycompany.csa_cw.models.Sensor;
import com.mycompany.csa_cw.models.SensorReading;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class DataStore {
    private static DataStore instance;
    
    // Core data structures for rooms and sensors 
    private Map<String, Room> rooms = new ConcurrentHashMap<>();
    private Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    
    //Storage for historical sensor readings 
    // Maps Sensor ID -> List of historical readings
    private Map<String, List<SensorReading>> readingsStore = new ConcurrentHashMap<>();

    private DataStore() {}

    public static synchronized DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    public Map<String, Room> getRooms() { return rooms; }
    public Map<String, Sensor> getSensors() { return sensors; }

    //Fetches history for a specific sensor context 
    
    public List<SensorReading> getReadingsForSensor(String sensorId) {
        return readingsStore.getOrDefault(sensorId, new ArrayList<>());
    }

    //Appends a new reading to a specific sensor's history 
     
    public void addReading(String sensorId, SensorReading reading) {
        readingsStore.computeIfAbsent(sensorId, k -> new ArrayList<>()).add(reading);
    }
}