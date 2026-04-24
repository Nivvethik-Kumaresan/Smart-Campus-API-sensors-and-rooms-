/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.csa_cw;

/**
 *
 * @author nivve
 */
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

@Path("/")
public class DiscoveryResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDiscoveryInfo() {
        Map<String, Object> apiInfo = new HashMap<>();
        apiInfo.put("version", "1.0.0");
        apiInfo.put("status", "Running");
        apiInfo.put("admin_contact", "nivvethik.20241198@iit.ac.lk");

        // HATEOAS: Providing links to other resources 
        Map<String, String> links = new HashMap<>();
        links.put("rooms", "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");

        apiInfo.put("links", links);
        return Response.ok(apiInfo).build();
    }
}
