# OmniTruth: Detailed Technical Specifications

## 1. Core Engine (The MCP Server)
The "Brain" that orchestrates the data flow.

### Technology Stack
*   **Language:** Java 21 (Virtual Threads/Project Loom for high-concurrency API calls).
*   **Framework:** Spring Boot 3.x (Production-ready, enterprise support).
*   **Protocol:** MCP (Model Context Protocol) via JSON-RPC.
*   **AI Integration:** Spring AI (Unified interface for different LLMs).

### Technical Implementation
*   **Dynamic Tool Registration:** On startup, the server reads the JSON blueprint and registers tools dynamically with the MCP Host.
*   **The Pipeline Executor:** A chain-of-responsibility pattern that executes `Fetch` $\rightarrow$ `Normalize` $\rightarrow$ `Evaluate`.
*   **SpEL Integration:** Uses `SpelExpressionParser` to evaluate business rules defined in the JSON blueprints at runtime.

---

## 2. Hallucination Control & Accuracy Framework
To ensure the AI never "guesses" facts, OmniTruth implements a **Deterministic Truth Architecture**. We move the "source of truth" from the LLM's weights to the MCP's execution.

### 2.1 Grounding via Tool-Centric Reasoning
The AI is strictly instructed to operate in **"Tool-First Mode."** 
*   **Constraint:** The AI is forbidden from answering operational questions using its internal knowledge. 
*   **Mechanism:** If a user asks "Is shipment X late?", the AI must call the `verify_compliance` tool. If the tool returns an error or "Not Found," the AI must report: *"I cannot find verified data for this shipment,"* rather than guessing.

### 2.2 Deterministic Logic (The SpEL Guard)
By using **Spring Expression Language (SpEL)** for business rules, the "Truth" is calculated by the Java backend, not the LLM.
*   **LLM's Role:** Acts as the "Translator" and "Synthesizer."
*   **Java's Role:** Acts as the "Judge."
*   **Example:** The LLM cannot decide if a shipment is late; it only reports the `Boolean` result returned by the Java engine's SpEL evaluation.

### 2.3 The "Truth Card" Validation (Verification Layer)
To prevent the AI from misinterpreting the tool's output, the MCP returns data in a **Structured Truth Format**:
*   **Input:** `verify_compliance`
*   **Output:** `{ "status": "FAIL", "reason": "driver_cert == FALSE", "evidence": "Driver D-402: No Certificate found in DB" }`
*   **The Guardrail:** The AI is prompted to quote the `evidence` field directly. This forces the AI to anchor its response in the raw data provided by the MCP.

### 2.4 Confidence Scoring & Human-in-the-Loop
For complex queries, the MCP implements **Confidence Scoring**:
*   **High Confidence:** Deterministic match found $\rightarrow$ AI answers directly.
*   **Low Confidence:** Partial match or ambiguous data $\rightarrow$ AI answers: *"I found a possible match, but I am only 60% confident. Would you like me to trigger a manual review by an agent?"*

---

## 3. Dual-Mode Execution Logic (API vs. Tool)
To ensure compatibility with all types of companies, the engine supports two distinct execution paths for tool calling:

### 3.1 Mode A: The "Intelligence Engine" (For Raw API Endpoints)
Used when the company only has raw data (APIs) and no existing business logic.
*   **Mechanism:** The MCP acts as the reasoning layer.
*   **Workflow:** `Fetch Raw Data (API)` $\rightarrow$ `Data Normalization` $\rightarrow$ `SpEL Logic Evaluation` $\rightarrow$ `Return Result`.

### 3.2 Mode B: The "Secure Proxy" (For Existing Internal Tools)
Used when the company already has internal microservices.
*   **Mechanism:** The MCP acts as a secure gateway/delegate.
*   **Workflow:** `Request Forwarding` $\rightarrow$ `Internal Tool Execution` $\rightarrow$ `Normalize Response`.

---

## 4. The Admin Control Plane (The Portal)
The management interface for the Enterprise IT team.

### Technology Stack
*   **Frontend:** Angular 17+ with TypeScript and Tailwind CSS.
*   **State Management:** TanStack Query.
*   **Backend:** Spring Boot REST API (Secured via OAuth2).
*   **Database:** PostgreSQL.

---

## 5. The Observability Layer (The Dashboard)
The system that ensures the "Truth" is always available.

### Technology Stack
*   **Monitoring:** Micrometer $\rightarrow$ Prometheus.
*   **Visualization:** Grafana.
*   **Scheduling:** Spring `@Scheduled` for heartbeat probes.

---

## 6. Data Integration Layer (The Connectors)
The "Plumbing" that connects to the real world.

### Technology Stack
*   **REST:** Spring `WebClient`.
*   **SQL:** `Spring Data JPA` + `HikariCP`.
*   **GraphQL:** `Spring GraphQL` client.
*   **Auth:** Spring Security OAuth2 Client.

---

## 7. Deployment Architecture
*   **Containerization:** Docker images.
*   **Orchestration:** Kubernetes (K8s) Helm charts.
*   **Environment Management:** Spring Profiles.
