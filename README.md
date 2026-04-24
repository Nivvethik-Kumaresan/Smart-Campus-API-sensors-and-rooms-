Smart Campus: Sensor & Room Management API
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

8. Observability
Logging Filters: I implemented a unified logging mechanism using JAX-RS Filters. By intercepting both ContainerRequest and ContainerResponse, the system provides a centralized audit trail of every HTTP method, URI, and final status code, facilitating rapid debugging and traffic analysis.
