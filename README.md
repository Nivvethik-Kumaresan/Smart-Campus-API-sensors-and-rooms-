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

## 4. Technical Justification & Report (Q&A)

Question: In your report, explain the default lifecycle of a JAX-RS Resource class. Is a
new instance instantiated for every incoming request, or does the runtime treat it as a
singleton? Elaborate on how this architectural decision impacts the way you manage and
synchronize your in-memory data structures (maps/lists) to prevent data loss or race con
ditions.

Answer: By default, the JAX-RS runtime treats resource classes as request-scoped, meaning a new instance is instantiated for every incoming HTTP request and is subsequently disposed of after the response is sent . This architectural decision promotes isolation and simplifies development, as instance-level thread safety for local variables is not a concern.

However, because the resource is short-lived, state must be externalized to a long-lived component—typically a Singleton utility class like a DataStore. Since multiple request-scoped resource instances may interact with this single shared DataStore simultaneously, we must implement strict synchronization strategies. Using ConcurrentHashMap for our collections ensures high-performance thread safety by providing fine-grained locking, preventing race conditions or data loss during concurrent write operations without the overhead of globally synchronized methods.

Question: Why is the provision of ”Hypermedia” (links and navigation within responses)
considered a hallmark of advanced RESTful design (HATEOAS)? How does this approach
benefit client developers compared to static documentation?

Answer: Hypermedia as the Engine of Application State (HATEOAS) represents the highest level of maturity in RESTful design because it makes the API self-descriptive and navigable. Instead of requiring client developers to hardcode URIs from static documentation, the API provides dynamic links that guide the client on available next steps based on the current state of the resource.

This benefits client developers by significantly decoupling the client from the server’s URI structure. If the server’s path logic changes, the client (which follows relation links like "self" or "rooms") remains unaffected, whereas a client built on static documentation would break. Furthermore, it reduces the learning curve by allowing developers to "explore" the API through its responses, much like a user navigates the web through hyperlinks.

Question: When returning a list of rooms, what are the implications of returning only
IDs versus returning the full room objects? Consider network bandwidth and client side
processing.

Answer: The choice between returning IDs versus full objects involves a trade-off between payload size and "chattiness". Returning only IDs minimizes initial network bandwidth usage, which is ideal for massive collections. However, it forces the client to perform "N+1" requests—fetching the list once and then making an individual call for every room to get details—which increases total latency and client-side complexity in managing multiple asynchronous calls.

Returning full objects increases the initial payload overhead but significantly improves the client experience by providing all necessary metadata in a single round-trip. For a campus infrastructure API where the number of rooms is manageable, returning full objects is usually superior as it simplifies client-side processing and reduces the total number of network handshakes required to populate a dashboard.

 Question: Is the DELETE operation idempotent in your implementation? Provide a detailed
justification by describing what happens if a client mistakenly sends the exact same DELETE
request for a room multiple times.

Answer: Yes, the DELETE operation is idempotent. An operation is idempotent if its side effects on the server state remain the same regardless of how many times it is performed. In this implementation, the first DELETE /{roomId} request successfully removes the room from the DataStore. If the client sends the exact same request again, the server checks the DataStore, finds that the room no longer exists, and effectively does nothing further to the system state. While the response code might differ (e.g., returning 204 for the first success and 404 for subsequent calls), the result on the server—the room's absence—remains identical, satisfying the principle of idempotency.

Question: We explicitly use the @Consumes (MediaType.APPLICATION_JSON) annotation on
the POST method. Explain the technical consequences if a client attempts to send data in
a different format, such as text/plain or application/xml. How does JAX-RS handle this
mismatch?

Answer: If a client sends a request with a Content-Type header (such as text/plain) that does not match the @Consumes(MediaType.APPLICATION_JSON) annotation on the resource method, JAX-RS will automatically block the request. The runtime will respond with an HTTP 415 Unsupported Media Type error. This enforcement ensures that the backend logic is protected from processing incompatible data formats that could lead to parsing errors, security vulnerabilities, or unexpected application crashes, as the framework handles this mismatch before the code even reaches the method's body.

Question: You implemented this filtering using @QueryParam. Contrastthiswithanalterna
tive design where the type is part of the URL path (e.g., /api/vl/sensors/type/CO2). Why
is the queryparameterapproachgenerallyconsideredsuperiorforfilteringandsearching
collections?

Answer: In RESTful design, Path Parameters are used to identify a specific, unique resource (e.g., a specific sensor ID), whereas Query Parameters are used to filter or sort a collection. The query parameter approach is superior for filtering because it keeps the URL structure clean and scalable. Using path segments for filters makes the URL rigid; if a client wanted to filter by both type AND status, a path-based approach would require complex, non-standard path nesting. Query strings, however, allow for optional, combinable parameters (e.g., ?type=CO2&status=ACTIVE) without changing the base resource path, providing a more intuitive and flexible interface for searching.

Question: Discuss the architectural benefits of the Sub-Resource Locator pattern. How
doesdelegating logic to separate classes help manage complexity in large APIs compared
to defining every nested path (e.g., sensors/{id}/readings/{rid}) in one massive con
troller class?

Answer: The Sub-Resource Locator pattern prevents the creation of "God Classes" that handle too many responsibilities. By delegating nested paths (like /sensors/{id}/readings) to a dedicated SensorReadingResource, we achieve a clean Separation of Concerns. This approach makes the code more maintainable and readable; developers working on reading history logic don't have to sift through a massive SensorResource file. Furthermore, it mirrors the physical hierarchy of the campus—a reading exists within the context of a sensor—making the API structure more logical and easier for new developers to understand.
 
Question: Why is HTTP 422 often considered more semantically accurate than a standard
404 whenthe issue is a missing reference inside a valid JSON payload?

Answer: HTTP 404 Not Found traditionally implies that the URI itself is invalid or the endpoint does not exist. In contrast, 422 Unprocessable Entity is semantically superior when the client sends a syntactically perfect JSON body, but the data inside it is logically invalid—such as referencing a Room ID that doesn't exist in the database. Using 422 explicitly communicates to the client that while the server understood the request, it cannot process it due to a business logic violation or a broken dependency, providing clearer debugging information than a generic 404.

Question: From a cybersecurity standpoint, explain the risks associated with exposing
internal Java stack traces to external API consumers. What specific information could an
attacker gather from such a trace?

Answer: Exposing raw Java stack traces is a critical security vulnerability because it reveals internal system details that should be hidden from the public. An attacker can gather sensitive information such as internal package names, class hierarchies, and file paths on the server. Most dangerously, traces often expose the specific versions of libraries (e.g., Jersey or Jackson) and the database schema. If an attacker identifies an outdated library version with a known vulnerability (CVE), they can launch a precise exploit. Implementing a global "Safety Net" mapper ensures these technical details stay in the server logs while the user receives only a sanitized 500 error.

Question: Why is it advantageous to use JAX-RS filters for cross-cutting concerns like
logging, rather than manually inserting Logger.info() statements inside every single re
source method?

Answer: Using JAX-RS filters for logging is advantageous because it handles a "cross-cutting concern" in a centralized, decoupled manner. If we manually inserted logs into every method, the code would be cluttered with repetitive statements, increasing the risk of human error or inconsistency. Filters intercept the request before it reaches the resource and after it leaves, ensuring that every transaction is logged automatically. This "Write Once, Apply Everywhere" approach ensures the API remains observable and maintainable without polluting the core business logic with boilerplate logging code.

***
