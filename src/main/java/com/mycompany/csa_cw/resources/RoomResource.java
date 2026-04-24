/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.csa_cw.resources;

/**
 *
 * @author nivve
 */
import com.mycompany.csa_cw.exceptions.RoomNotEmptyException;
import com.mycompany.csa_cw.models.Room;
import com.mycompany.csa_cw.util.DataStore;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private DataStore dataStore = DataStore.getInstance();

    @GET
    public List<Room> getAllRooms() {
        // Provide a comprehensive list of all rooms 
        return new ArrayList<>(dataStore.getRooms().values());
    }

    @POST
    public Response createRoom(Room room) {
        // Enable the creation of new rooms 
        dataStore.getRooms().put(room.getId(), room);
        return Response.status(Response.Status.CREATED).entity(room).build();
    }

    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        // Allow users to fetch detailed metadata for a specific room 
        Room room = dataStore.getRooms().get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(room).build();
    }

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = dataStore.getRooms().get(roomId);

        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Task 5.1: Check if room has sensors assigned 
        if (!room.getSensorIds().isEmpty()) {
            // Throwing this triggers the RoomNotEmptyMapper (409 Conflict) 
            throw new RoomNotEmptyException("Deletion failed: Room '" + roomId + "' currently has active sensors.");
        }

        dataStore.getRooms().remove(roomId);
        return Response.noContent().build();
    }
}
