# OmniTruth: Executive Summary & Project Vision

## 1. What is OmniTruth? (The "First-Time Reader" Guide)
OmniTruth is an **AI-Powered Truth Engine** for enterprises. 

**The Problem it Solves:** 
In most companies, critical information is "fragmented." If you want to know if a client's order is on time and compliant with their contract, you have to check a CRM (Salesforce), a Logistics tool, and a Billing system. This manual "detective work" is slow, expensive, and prone to human error.

**The Solution:**
OmniTruth uses the **Model Context Protocol (MCP)** to act as a universal bridge. It connects an AI (like Claude or GPT) directly to a company's internal data sources. Instead of a human searching through five apps, the AI uses OmniTruth to "call" the data, reason across it, and provide a single, verified answer.

**The "Magic":**
OmniTruth doesn't require companies to rewrite their software. It plugs into their existing APIs. Whether a company has simple data endpoints or complex internal tools, OmniTruth transforms them into "AI Skills" that can be used by employees and their customers.

---

## 2. How the "Onboarding" Works (From Admin to Backend)

When a company starts using OmniTruth, they don't write code; they use the **Admin Portal**. Here is the lifecycle of an onboarded API/Tool:

### Step 1: The Admin Action (The Portal)
The Admin logs into the Angular portal and adds a new capability.
*   **Example:** They add an API endpoint: `GET /api/shipping/status/{id}`.
*   **The Logic:** They define a rule: `if status == 'DELIVERED' and hours < 48 then 'COMPLIANT'`.
*   **The Save:** The portal sends this configuration to the OmniTruth backend.

### Step 2: The Ingestion (The Backend)
Your Spring Boot backend receives this configuration and performs the following:
1.  **Blueprint Storage:** The config is saved in a PostgreSQL database as a **"Tool Recipe."**
2.  **Dynamic Registration:** The MCP Server detects the new recipe. It automatically updates the list of "Available Tools" it shares with the AI.
3.  **Zero-Restart:** Because the system is dynamic, the AI immediately "sees" the new tool without the server needing to reboot.

### Step 3: The Internal Execution (The Engine)
When a user asks a question, the internal "engine" kicks in:
1.  **Intent Detection:** The AI sees the request and decides to call the `verify_shipping` tool.
2.  **Recipe Retrieval:** The MCP server pulls the "Recipe" for that tool from the DB.
3.  **Execution Pipeline:**
    *   **Fetch:** The engine calls the raw API endpoint.
    *   **Normalize:** It cleans the raw JSON into a simple format.
    *   **Evaluate:** It runs the logic (e.g., `hours < 48`) using the **Spring Expression Language (SpEL)**.
4.  **Final Synthesis:** The result is sent back to the AI, which explains it to the user.

---

## 3. The Final Results (The Value)

### For the Company (Internal Value)
*   **From Hours to Seconds:** A manager who spent 3 hours reconciling a shipment now gets the answer in 3 seconds.
*   **Proactive Risk Management:** The "Health Dashboard" alerts the admin *before* a client notices a failure.
*   **Reduced Labor:** Fewer employees are needed to perform manual "data chasing."

### For the Client (External Value)
*   **Instant Transparency:** The client no longer has to call a support line; they ask the AI portal and get a verified, real-time answer.
*   **Increased Trust:** The client sees "Truth Cards" (verified data) rather than vague promises from a support agent.
*   **Premium Experience:** The company appears highly modern and transparent, increasing client loyalty.

---

## 4. Quick-Start Guide for Developers
To understand the full system, read the documents in this order:
1.  **`Security_and_Compliance.md`** $\rightarrow$ To understand how we keep data safe.
2.  **`Technical_Specifications.md`** $\rightarrow$ To understand the Java/Angular stack.
3.  **`Full_Architecture_Blueprint.md`** $\rightarrow$ To see the end-to-end logic flow.
4.  **`MultiTenant_and_User_Experience.md`** $\rightarrow$ To see how different companies and users interact.
