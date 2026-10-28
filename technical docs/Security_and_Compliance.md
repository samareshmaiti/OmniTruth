# OmniTruth: Security & Compliance Framework

## 1. Security Philosophy
OmniTruth operates on the principle of **Zero-Trust Architecture** and **Least Privilege**. Because it handles sensitive enterprise data, the system is designed so that the software vendor (you) never has access to the client's raw data.

### 1.1 Data Privacy & Sovereignty
*   **On-Premise/VPC Deployment:** The MCP server is deployed within the client's own Virtual Private Cloud (VPC). Data never leaves the client's perimeter.
*   **Stateless Execution:** The MCP server does not store client data. It acts as a pass-through (Proxy) between the AI and the data source.
*   **Encryption in Transit:** All communication between the AI Host, MCP Server, and internal APIs must use **TLS 1.3**.

### 1.2 Access Control & Authorization
*   **Identity Propagation:** The MCP server does not use a single "Master Key." It propagates the user's identity (via JWT) to the downstream APIs to ensure the user only sees data they are authorized to see.
*   **Read-Only Mandate:** All database connectors are strictly configured for `READ-ONLY` access. The MCP server is physically unable to perform `INSERT`, `UPDATE`, or `DELETE` operations.
*   **Scoped Tooling:** Admins can define which users/roles have access to which specific MCP tools.

---

## 2. Regulatory Compliance Mapping
The platform is designed to be compliant with global standards by implementing specific "Guardrail" modules:

| Regulation | Requirement | OmniTruth Implementation |
| :--- | :--- | :--- |
| **GDPR (EU)** | Right to be forgotten / Data Minimized | PII Masking layer in the Normalizer. No PII is stored in MCP logs. |
| **HIPAA (US)** | Patient Data Privacy | Dedicated "Healthcare Pack" with encrypted audit trails and BAA-compliant deployment. |
| **SOC2 Type II** | Operational Security | Automated heartbeat monitoring and detailed audit logs of every tool call. |
| **PCI-DSS** | Credit Card Security | Strict exclusion of any fields matching PAN patterns via regex filters in the Normalizer. |

---

## 3. Audit & Governance
Every action taken by the AI through the MCP is recorded in an **Immutable Audit Log**.
*   **Log Content:** `Timestamp` $\rightarrow$ `User ID` $\rightarrow$ `Tool Called` $\rightarrow$ `Parameters` $\rightarrow$ `Result Hash`.
*   **Tamper-Proofing:** Logs are streamed to the client's internal logging system (e.g., Splunk, ELK Stack) so they cannot be altered.
