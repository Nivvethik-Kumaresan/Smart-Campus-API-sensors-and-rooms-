***

# Smart Campus: Sensor & Room Management API
**Developer:** Nivvethik Kumaresan
**Environment:** GlassFish 5.1.0 | JDK 8 | Maven | JAX-RS (Jersey)

## 1. API Design Overview
The Smart Campus API is a RESTful service designed to manage physical spaces (Rooms) and hardware (Sensors) across a university environment. The architecture adheres to several high-level REST principles:

* **Hierarchical Resource Structure:** The API utilizes a logical nesting of resources. For example, sensor readings are treated as sub-resources of a specific sensor (`/sensors/{id}/readings`), ensuring that data collection is always contextualized by its parent hardware.
* **HATEOAS-Driven Discovery:** The entry point provides a "service map" with hypermedia links, allowing clients to navigate the campus infrastructure without hardcoded knowledge of every endpoint.
* **Robust Error Handling:** By implementing custom `ExceptionMappers`, the API is "leak-proof." It intercepts business violations (like deleting a room with active sensors) and returns semantic JSON error bodies instead of default server stack traces, improving both security and developer experience.
* **Thread-Safe In-Memory Storage:** Since no external database is used, the system utilizes a Singleton `DataStore` with `ConcurrentHashMap` to manage state across concurrent HTTP requests.

## 2. Build & Launch Instructions
Follow these steps to deploy the API locally using NetBeans and GlassFish.

### Prerequisites
* **JDK 8:** Ensure your `JAVA_HOME` is set to JDK 8 (GlassFish 5 requirement).
* **GlassFish 5.1.0:** Configured as a Server in NetBeans.
* **Maven:** Integrated with your IDE.

### Step-by-Step Launch
1.  **Open Project:** Open the `CSA_CW` folder in NetBeans.
2.  **Clean and Build:** Right-click the project in the Projects pane and select **Clean and Build**. This will download the JAX-RS dependencies and package the application into a `.war` file.
3.  **Configure Server:** Ensure GlassFish is running. In the **Services** tab, right-click **GlassFish Server** and select **Start**.
4.  **Run Application:** Right-click the project and select **Run**.
5.  **Verify Deployment:** The API will be available at:  
    `http://localhost:8080/CSA_CW/api/v1/`

## 3. Sample Interactions (Curl Commands)
Open a terminal (or Git Bash) to test the successful implementation of the API logic.

### 3.1. API Discovery
Verify the entry point and metadata.
```bash
curl -X GET http://localhost:8080/CSA_CW/api/v1/
```

### 3.2. Register a New Room
Create a physical space to house sensors.
```bash
curl -X POST http://localhost:8080/CSA_CW/api/v1/rooms \
-H "Content-Type: application/json" \
-d '{"id": "LIB-301", "name": "Main Library Study Hall", "capacity": 50}'
```

### 3.3. Register a Sensor to a Room
Link a piece of hardware to the previously created room.
```bash
curl -X POST http://localhost:8080/CSA_CW/api/v1/sensors \
-H "Content-Type: application/json" \
-d '{"id": "TEMP-001", "type": "Temperature", "status": "ACTIVE", "roomId": "LIB-301"}'
```

### 3.4. Post a Sensor Reading (Sub-Resource)
Record a data point for a specific temperature sensor.
```bash
curl -X POST http://localhost:8080/CSA_CW/api/v1/sensors/TEMP-001/readings \
-H "Content-Type: application/json" \
-d '{"value": 22.5}'
```

### 3.5. Filter Sensors by Type
Retrieve only the temperature-related hardware on campus.
```bash
curl -X GET "http://localhost:8080/CSA_CW/api/v1/sensors?type=Temperature"
```

## 4. Technical Justification & Report

1. Architecture & Configuration
JAX-RS Configuration: The API is bootstrapped via the RestAppConfig class using the @ApplicationPath("/api/v1") annotation. This versioning strategy ensures that future iterations of the API can coexist without breaking existing client integrations.

Resource Lifecycle: I have utilized the default JAX-RS Request-scoped lifecycle for all resource classes. This ensures that a fresh instance is created for every HTTP request, preventing state leakage between concurrent users.

Data Synchronization: To manage the shared campus state, I implemented a Singleton DataStore utilizing ConcurrentHashMap. This strategy provides high-performance thread-safety for in-memory data structures, preventing race conditions during simultaneous write operations from different sensors.

2. Discovery & HATEOAS
Self-Documenting API: The entry point (GET /api/v1) returns a comprehensive metadata object.

HATEOAS Justification: By implementing Hypermedia as the Engine of Application State (HATEOAS), the API provides dynamic navigation links. This decouples the client from the server's internal URI structure, allowing the backend to evolve while remaining self-documenting for client developers.

3. Room Management
Full-Object vs. ID-Only Returns: For the collection retrieval (GET /rooms), I opted to return full room objects. While returning only IDs minimizes bandwidth, providing full metadata in a single response reduces "network chattiness," which is critical for the performance of low-power IoT dashboards on campus.

Deletion & Idempotency: The DELETE operation is implemented as strictly idempotent. Regardless of how many times a client calls DELETE for the same room ID, the final state of the server remains the same. The API correctly blocks the deletion of rooms containing active sensors (409 Conflict) to maintain hardware-to-space integrity.

4. Sensor Operations & Filtering
Content-Type Enforcement: By utilizing @Consumes(MediaType.APPLICATION_JSON), the API automatically manages media type mismatches. If a client attempts to send text/plain, JAX-RS triggers an HTTP 415 Unsupported Media Type error, protecting the service from processing malformed data.

Query vs. Path Parameters: I utilized Query Parameters (?type=...) for sensor filtering. In RESTful design, Path Parameters are best reserved for identifying specific resources, whereas Query Parameters provide the flexibility required for optional, combinable filtering criteria across a collection.

5. Sub-Resource Design
Managing Complexity: I implemented the Sub-Resource Locator pattern for sensor readings (/sensors/{id}/readings).

Delegation: By delegating reading-specific logic to a separate SensorReadingResource class, I avoided creating a "God Class" and improved code maintainability. This structure mirrors the physical reality where readings exist only within the context of a specific piece of hardware.

6. Advanced Error Handling & Security
Semantic Accuracy (422 vs. 404): I utilized HTTP 422 (Unprocessable Entity) for cases where a sensor is linked to a non-existent room. Unlike a 404 error (which implies the endpoint is missing), 422 indicates the JSON payload was syntactically correct but semantically invalid due to a broken reference.

Cybersecurity & Information Hiding: The GlobalExceptionMapper serves as a critical security layer. By intercepting all Throwable types and returning a generic 500 Internal Server Error JSON, I prevent the leakage of Java stack traces. Raw stack traces are a major security risk as they expose internal file paths, library versions, and logic flows to potential attackers.

7. Observability
Logging Filters: I implemented a unified logging mechanism using JAX-RS Filters. By intercepting both ContainerRequest and ContainerResponse, the system provides a centralized audit trail of every HTTP method, URI, and final status code, facilitating rapid debugging and traffic analysis.

***
