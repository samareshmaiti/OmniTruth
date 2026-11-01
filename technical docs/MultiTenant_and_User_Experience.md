# OmniTruth: Multi-Tenant Usage & Client Experience Guide

## 1. The Multi-Tenant Architecture
OmniTruth is designed as a **Multi-Tenant Platform**. While the core Java MCP Engine is a single piece of software, it behaves differently for every company based on its **Company Profile** and **Blueprint**.

### 1.1 How the System Distinguishes Companies
The system uses a **Tenant Identifier** (e.g., `CompanyID` or `TenantID`). This ID is passed in every request from the AI Host to the MCP Server.

*   **Request Header:** `X-Tenant-ID: COMP_LOGISTICS_001`
*   **MCP Logic:** Upon receiving a request, the server fetches the corresponding `Blueprint` and `Credential Set` from the database for that specific ID.
*   **Isolation:** Data from Company A is logically and physically separated from Company B. They never share blueprints, credentials, or cached data.

---

## 2. How Different Companies Use the Same Tool
The "Magic" is that the **AI's tools remain the same**, but the **Tool's implementation** changes based on the company's blueprint.

### Comparison: Same Tool $\rightarrow$ Different Industries

| Generic Tool | Logistics Company User | Healthcare Company User | Teradata (Tech) User |
| :--- | :--- | :--- | :--- |
| **`get_entity_info`** | Fetches shipment GPS, weight, and driver name. | Fetches patient vitals, current ward, and attending doctor. | Fetches cluster health, CPU load, and active queries. |
| **`verify_compliance`** | Checks if delivery was < 48h and driver was certified. | Checks if medication dosage is within safe limits for the patient. | Checks if query costs are within the monthly budget. |
| **`detect_anomaly`** | Flags a shipment that has stopped moving for 6 hours. | Flags a sudden spike in a patient's heart rate. | Flags a query that is 100x slower than the average. |

**The AI understands the context because of the System Prompt provided by the MCP:**
*   *For Logistics:* "You are a Logistics Expert. Use `verify_compliance` to check shipping SLAs."
*   *For Healthcare:* "You are a Medical Assistant. Use `verify_compliance` to check care plan adherence."

---

## 3. The Client Experience (User X)
This is the "B2B2C" layer. Company A (The Enterprise) provides a UI to User X (The Client).

### 3.1 The User Journey
1.  **Authentication:** User X logs into the Company A Portal (using OAuth2/SAML).
2.  **Identity Propagation:** The portal generates a JWT that says: `User: User_X, Company: Company_A, Role: Client`.
3.  **The Interaction:** User X asks the AI: *"Where is my order and why is it late?"*
4.  **The MCP Loop:** 
    *   The AI calls `get_entity_info`.
    *   The MCP Server validates that User X has permission to see that specific order.
    *   The MCP fetches the data $\rightarrow$ The AI synthesizes the answer.
5.  **The Result:** User X gets a human-like answer without ever talking to a support agent.

---

## 4. The UI Portal (Angular & TypeScript)
The entire management and interaction layer is built using **Angular**, providing a professional, type-safe, and scalable enterprise interface.

### 4.1 The Admin Dashboard (For Company A)
**Technology:** Angular 17+, TypeScript, Tailwind CSS.
**Key Modules:**
*   **Connector Manager:** A screen to add API endpoints (URL, Method, Headers).
*   **Visual Recipe Builder:** A node-based flow editor where admins drag-and-drop "Fetch" blocks and "Logic" blocks to create a tool.
*   **Health Monitor:** A real-time grid showing the status (Green/Yellow/Red) of all connected endpoints.
*   **Audit Log Viewer:** A table showing every tool call made by the AI and the resulting data.

### 4.2 The Client Portal (For User X)
**Technology:** Angular 17+, TypeScript, Tailwind CSS.
**Key Modules:**
*   **AI Chat Interface:** A clean, modern chat window with streaming responses.
*   **Truth Cards:** When the AI finds a "Fact" (e.g., a shipment status), it renders a **Truth Card**—a small UI component that shows the raw data and a "Verified by OmniTruth" badge.
*   **Action Center:** If the AI detects a problem (e.g., "Late Delivery"), the UI renders a button for the client to "Request Refund" or "Contact Manager" directly.

---

## 5. Technical Architecture Summary (Angular $\rightarrow$ MCP)

**The Request Flow:**
`User X (Angular UI)` $\xrightarrow{\text{JWT Token}}$ `Company A AI Backend` $\xrightarrow{\text{MCP Protocol}}$ `OmniTruth MCP Server (Java)` $\xrightarrow{\text{API Call}}$ `Internal Enterprise Data`.

**Why Angular/TypeScript?**
*   **Type Safety:** Ensures that the complex JSON structures of the Blueprints are handled without errors.
*   **Modularity:** Allows the "Admin Portal" and the "Client Portal" to be separate modules of the same application.
*   **Performance:** Fast rendering of the Health Dashboard and the real-time AI chat stream.
