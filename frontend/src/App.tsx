import { useCallback, useEffect, useState } from "react";
import {
  Activity, AlertCircle, ArrowRight, CheckCircle2, ClipboardList,
  FileText, Inbox, LogOut, Play, RefreshCw, ShieldCheck,
  Sparkles, Ticket as TicketIcon, XCircle
} from "lucide-react";
import "./App.css";

const API = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

type Customer = {
  customerId: string;
  name: string;
  email: string;
  tier: string;
  country: string;
  verified: boolean;
  tags: string[];
};

type OrderItem = {
  sku: string;
  name: string;
  quantity: number;
  category: string;
  finalSale: boolean;
};

type Order = {
  orderId: string;
  status: string;
  placedAt: string;
  deliveredAt: string | null;
  eligibleReturnUntil: string | null;
  total: number;
  currency: string;
  paymentStatus: string;
  trackingNumber: string;
  items: OrderItem[];
};

type Ticket = {
  ticketId: string;
  channel: string;
  subject: string;
  body: string;
  createdAt: string;
  status: string;
  customer: Customer;
  order: Order | null;
};

type Page<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
};

type Triage = {
  ticketId: string;
  category: string;
  priority: string;
  recommendedAction: string;
  reasoning: string;
  retrievedDocumentIds: string[];
};

type Citation = {
  documentId: string;
  title: string;
};

type Reply = {
  ticketId: string;
  status: string;
  draftReply: string;
  citations: Citation[];
  reasoning: string;
};

type Action = {
  actionId: string;
  ticketId: string;
  actionType: string;
  status: string;
  parameters: Record<string, string>;
  reason: string;
  requiresHumanApproval: boolean;
  createdAt: string;
  updatedAt: string;
};

type EvaluationCase = {
  caseId: string;
  ticketId: string;
  actualCategory: string;
  expectedCategory: string;
  categoryCorrect: boolean;
  actualPriority: string;
  expectedPriority: string;
  priorityCorrect: boolean;
  actualCitations: string[];
  citationCoverage: boolean;
  recommendedAction: string;
  actionPolicyCompliant: boolean;
  actualEscalation: boolean;
  escalationCorrect: boolean;
  failedAnswerRequirements: string[];
  replyStatus: string;
  passed: boolean;
};

type Evaluation = {
  runId: string;
  totalCases: number;
  passedCases: number;
  categoryAccuracy: number;
  priorityAccuracy: number;
  citationCoverage: number;
  unsafeActionBlockingRate: number;
  escalationAccuracy: number;
  answerRequirementCoverage: number;
  results: EvaluationCase[];
  startedAt: string;
  completedAt: string;
};

type Credentials = {
  username: string;
  password: string;
};

type AiTrace = {
  traceId: string;
  ticketId: string;
  runType: string;
  retrievedDocumentIds: string;
  toolActions: string;
  guardrailResult: string;
  finalStatus: string;
  createdAt: string;
};

function App() {
  const [credentials, setCredentials] = useState<Credentials | null>(null);
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [selected, setSelected] = useState<Ticket | null>(null);
  const [triage, setTriage] = useState<Triage | null>(null);
  const [reply, setReply] = useState<Reply | null>(null);
  const [action, setAction] = useState<Action | null>(null);
  const [evaluation, setEvaluation] = useState<Evaluation | null>(null);
  const [activeTab, setActiveTab] = useState("inbox");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");

  const [actions, setActions] = useState<Action[]>([]);
  const [actionStatusFilter, setActionStatusFilter] = useState("ALL");
  const [traces, setTraces] = useState<AiTrace[]>([]);
  const [allTraces, setAllTraces] = useState<AiTrace[]>([]);
  const [traceFilter, setTraceFilter] = useState("ALL");
  const [auditActions, setAuditActions] = useState<Action[]>([]);

  const api = useCallback(async <T,>(
    path: string,
    options: RequestInit = {}
  ): Promise<T> => {
    if (!credentials) throw new Error("Please sign in first.");

    const headers = new Headers(options.headers);
    headers.set(
      "Authorization",
      `Basic ${btoa(`${credentials.username}:${credentials.password}`)}`
    );

    if (options.body) headers.set("Content-Type", "application/json");

    const response = await fetch(`${API}${path}`, {
      ...options,
      headers
    });

    if (!response.ok) {
      const message = await response.text();
      throw new Error(message || `Request failed (${response.status})`);
    }

    return response.json() as Promise<T>;
  }, [credentials]);

  const loadTickets = useCallback(async () => {
    const page = await api<Page<Ticket>>("/api/tickets?page=0&size=50");
    setTickets(page.content);
    if (!selected && page.content.length) {
      setSelected(page.content[0]);
    }
  }, [api, selected]);

  useEffect(() => {
    if (!credentials) return;
    setLoading(true);
    loadTickets()
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [credentials, loadTickets]);

  const loadActions = useCallback(async () => {
  const query = actionStatusFilter === "ALL"
    ? ""
    : `?status=${encodeURIComponent(actionStatusFilter)}`;

  const result = await api<Action[]>(`/api/actions${query}`);
  setActions(result);
}, [api, actionStatusFilter]);

  useEffect(() => {
    if (!credentials || activeTab !== "actions") return;

    setLoading(true);
    loadActions()
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [credentials, activeTab, loadActions]);

  const loadAllTraces = useCallback(async () => {
    const result = await api<AiTrace[]>("/api/traces");
      setAllTraces(result);
    }, [api]);

  const loadActionsForAudit = useCallback(async () => {
    const result = await api<Action[]>("/api/actions");
    setAuditActions(result);
  }, [api]);

  useEffect(() => {
    if (!credentials || activeTab !== "audit") return;

    setLoading(true);
    Promise.all([loadAllTraces(), loadActionsForAudit()])
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [credentials, activeTab, loadAllTraces, loadActionsForAudit]);

  const loadTraces = useCallback(async (ticketId: string) => {
  const result = await api<AiTrace[]>(
      `/api/traces/tickets/${encodeURIComponent(ticketId)}`
    );
    setTraces(result);
  }, [api]);

  useEffect(() => {
    if (!credentials || activeTab !== "inbox" || !selected?.ticketId) {
      setTraces([]);
      return;
    }

    loadTraces(selected.ticketId)
      .catch((e) => setError(e.message));
  }, [credentials, activeTab, selected?.ticketId, loadTraces]);

  const selectTicket = async (ticket: Ticket) => {
    setSelected(ticket);
    setTriage(null);
    setReply(null);
    setAction(null);
    setError("");

    try {
      const details = await api<Ticket>(`/api/tickets/${ticket.ticketId}`);
      setSelected(details);
    } catch (e) {
      setError((e as Error).message);
    }
  };

  const runTriage = async () => {
    if (!selected) return;
    setLoading(true);
    setError("");
    try {
      const result = await api<Triage>(
        `/api/tickets/${selected.ticketId}/triage`,
        { method: "POST" }
      );
      setTriage(result);
      await loadTraces(selected.ticketId);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  };

  const generateReply = async () => {
    if (!selected) return;
    setLoading(true);
    setError("");
    try {
      const result = await api<Reply>(
        `/api/tickets/${selected.ticketId}/draft-reply`,
        { method: "POST" }
      );
      setReply(result);
      await loadTraces(selected.ticketId);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  };

  const submitRecommendedAction = async () => {
    if (!selected || !triage || !selected.customer) return;

    const actionType = triage.recommendedAction;
    if (actionType === "NONE") return;

    const order = selected.order;
    const reason = `Support action for ticket ${selected.ticketId}: ${selected.subject}`;
    const parameters: Record<string, string> = {
      reason,
      idempotency_key: crypto.randomUUID()
    };

    if (order) {
      parameters.order_id = order.orderId;
      if (order.trackingNumber) {
        parameters.tracking_number = order.trackingNumber;
      }
      if (order.items?.length) {
        parameters.sku = order.items[0].sku;
      }
    }

    if (actionType === "CREATE_REPLACEMENT_ORDER" ||
        actionType === "START_REFUND_REVIEW") {
      if (!order) {
        setError("This action requires an order linked to the ticket.");
        return;
      }
    }

    if (actionType === "START_REFUND_REVIEW") {
      parameters.amount = String(order?.total ?? 0);
    }

    if (actionType === "ISSUE_COUPON") {
      parameters.customer_id = selected.customer.customerId;
      parameters.amount = "0";
    }

    if (actionType === "LOCK_ACCOUNT") {
      parameters.customer_id = selected.customer.customerId;
    }

    if (actionType === "ESCALATE_TO_HUMAN") {
      parameters.ticket_id = selected.ticketId;
      parameters.queue = "support";
    }

    setLoading(true);
    setError("");

    try {
      const result = await api<Action>("/api/actions", {
        method: "POST",
        body: JSON.stringify({
          ticketId: selected.ticketId,
          actionType,
          parameters,
          amount: parameters.amount ? Number(parameters.amount) : null,
          reason
        })
      });
      setAction(result);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  };

  const reviewAction = async (decision: "APPROVE" | "REJECT") => {
    if (!action) return;
    setLoading(true);
    setError("");
    try {
      const result = await api<Action>(
        `/api/actions/${action.actionId}/review`,
        {
          method: "POST",
          body: JSON.stringify({ decision })
        }
      );
      setAction(result);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  };

  const executeAction = async () => {
    if (!action) return;
    setLoading(true);
    setError("");
    try {
      const result = await api<Action>(
        `/api/actions/${action.actionId}/execute`,
        { method: "POST" }
      );
      setAction(result);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  };

  const reviewQueuedAction = async (
  actionId: string,
  decision: "APPROVE" | "REJECT"
) => {
  setLoading(true);
  setError("");

  try {
    const updated = await api<Action>(
      `/api/actions/${actionId}/review`,
      {
        method: "POST",
        body: JSON.stringify({ decision })
      }
    );

    setActions(current =>
      current.map(item =>
        item.actionId === actionId ? updated : item
      )
    );
  } catch (e) {
    setError((e as Error).message);
  } finally {
    setLoading(false);
  }
};

const executeQueuedAction = async (actionId: string) => {
  setLoading(true);
  setError("");

  try {
    const updated = await api<Action>(
      `/api/actions/${actionId}/execute`,
      { method: "POST" }
    );

    setActions(current =>
      current.map(item =>
        item.actionId === actionId ? updated : item
      )
    );
  } catch (e) {
    setError((e as Error).message);
  } finally {
    setLoading(false);
  }
};

  const runEvaluation = async () => {
    setLoading(true);
    setError("");
    try {
      const result = await api<Evaluation>("/api/evaluations/run", {
        method: "POST"
      });
      setEvaluation(result);
      setActiveTab("evaluations");
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  };

  const filteredTickets = tickets.filter((ticket) =>
    `${ticket.ticketId} ${ticket.subject} ${ticket.customer?.name ?? ""}`
      .toLowerCase()
      .includes(search.toLowerCase())
  );

  if (!credentials) {
    return <Login onLogin={setCredentials} />;
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-icon"><ShieldCheck size={22} /></div>
          <div>
            <strong>TrustDesk</strong>
            <span>Support Operations</span>
          </div>
        </div>

        <div className="nav-label">WORKSPACE</div>
        <button
          className={`nav-item ${activeTab === "inbox" ? "active" : ""}`}
          onClick={() => setActiveTab("inbox")}
        >
          <Inbox size={18} /> Ticket Inbox
          <span className="nav-count">{tickets.length}</span>
        </button>
        <button
          className={`nav-item ${activeTab === "evaluations" ? "active" : ""}`}
          onClick={() => setActiveTab("evaluations")}
        >
          <Activity size={18} /> Evaluations
        </button>

        <button
          className={`nav-item ${activeTab === "actions" ? "active" : ""}`}
          onClick={() => setActiveTab("actions")}
        >
          <ClipboardList size={18} /> Action Queue
          <span className="nav-count">{actions.length}</span>
        </button>

        <button
          className={`nav-item ${activeTab === "audit" ? "active" : ""}`}
          onClick={() => setActiveTab("audit")}
        >
          <Activity size={18} /> Audit & Observability
        </button>

        <div className="sidebar-bottom">
          <div className="connection-status">
            <span className="status-dot" /> Backend connected
          </div>
          <button className="nav-item logout" onClick={() => setCredentials(null)}>
            <LogOut size={18} /> Sign out
          </button>
        </div>
      </aside>

      <main className="main-area">
        <header className="topbar">
          <div>
            <div className="breadcrumb">TrustDesk / Workspace</div>
            <h1>
              {activeTab === "inbox"
                ? "Support inbox"
                : activeTab === "actions"
                  ? "Action approval queue"
                  : activeTab === "audit"
                    ? "Audit & Observability"
                    : "AI evaluations"}
            </h1>
          </div>
          <div className="topbar-right">
            <span className="user-avatar">{credentials.username[0]?.toUpperCase()}</span>
            <span className="user-name">{credentials.username}</span>
          </div>
        </header>

        {error && (
          <div className="error-banner">
            <AlertCircle size={17} />
            <span>{error}</span>
            <button onClick={() => setError("")}><XCircle size={16} /></button>
          </div>
        )}

        {activeTab === "inbox" ? (
          <div className="workspace">
            <section className="ticket-panel">
              <div className="panel-heading">
                <div>
                  <h2>All tickets</h2>
                  <p>{tickets.length} support conversations</p>
                </div>
                <button className="icon-button" onClick={loadTickets} title="Refresh">
                  <RefreshCw size={17} />
                </button>
              </div>

              <div className="search-box">
                <Inbox size={16} />
                <input
                  placeholder="Search tickets..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                />
              </div>

              <div className="ticket-list">
                {filteredTickets.map((ticket) => (
                  <button
                    key={ticket.ticketId}
                    className={`ticket-row ${
                      selected?.ticketId === ticket.ticketId ? "selected" : ""
                    }`}
                    onClick={() => selectTicket(ticket)}
                  >
                    <div className="ticket-row-top">
                      <span>{ticket.ticketId}</span>
                      <span className="ticket-channel">{ticket.channel}</span>
                    </div>
                    <strong>{ticket.subject}</strong>
                    <div className="ticket-row-bottom">
                      <span>{ticket.customer?.name ?? "Customer"}</span>
                      <span className="status-pill">{ticket.status}</span>
                    </div>
                  </button>
                ))}
                {!filteredTickets.length && (
                  <div className="empty-state">No matching tickets found.</div>
                )}
              </div>
            </section>

            <section className="detail-panel">
              {selected ? (
                <>
                  <div className="ticket-detail-heading">
                    <div>
                      <div className="eyebrow">{selected.ticketId} · {selected.channel}</div>
                      <h2>{selected.subject}</h2>
                      <p>
                        Opened {new Date(selected.createdAt).toLocaleString()}
                      </p>
                    </div>
                    <span className="status-pill">{selected.status}</span>
                  </div>

                  <div className="detail-grid">
                    <div className="detail-card">
                      <h3>Customer</h3>
                      <strong>{selected.customer?.name}</strong>
                      <p>{selected.customer?.email}</p>
                      <p>{selected.customer?.tier} · {selected.customer?.country}</p>
                      <span className={`verification ${selected.customer?.verified ? "verified" : ""}`}>
                        {selected.customer?.verified ? "Verified customer" : "Not verified"}
                      </span>
                    </div>

                    <div className="detail-card">
                      <h3>Order information</h3>
                      {selected.order ? (
                        <>
                          <strong>{selected.order.orderId}</strong>
                          <p>{selected.order.status} · {selected.order.paymentStatus}</p>
                          <p>
                            {selected.order.currency} {selected.order.total}
                          </p>
                          <p>Return until: {selected.order.eligibleReturnUntil ?? "Not available"}</p>
                        </>
                      ) : (
                        <p>No linked order</p>
                      )}
                    </div>
                  </div>

                  <div className="message-card">
                    <div className="section-title">
                      <h3><TicketIcon size={17} /> Customer message</h3>
                    </div>
                    <p>{selected.body}</p>
                  </div>

                  <div className="ai-actions">
                    <button className="primary-button" onClick={runTriage} disabled={loading}>
                      <Sparkles size={17} /> Run AI triage
                    </button>
                    <button className="secondary-button" onClick={generateReply} disabled={loading}>
                      <FileText size={17} /> Generate reply
                    </button>
                  </div>

                  {triage && (
                    <div className="result-card">
                      <div className="section-title">
                        <h3><Sparkles size={17} /> AI triage result</h3>
                        <span className={`priority-pill ${triage.priority.toLowerCase()}`}>
                          {triage.priority}
                        </span>
                      </div>
                      <div className="triage-grid">
                        <div>
                          <span className="field-label">CATEGORY</span>
                          <strong>{triage.category.replaceAll("_", " ")}</strong>
                        </div>
                        <div>
                          <span className="field-label">RECOMMENDED ACTION</span>
                          <strong>{triage.recommendedAction.replaceAll("_", " ")}</strong>
                        </div>
                      </div>
                      <p className="reasoning">{triage.reasoning}</p>
                      <div className="citation-list">
                        <span className="field-label">RETRIEVED KNOWLEDGE</span>
                        {triage.retrievedDocumentIds.map((id) => (
                          <span className="citation-chip" key={id}>{id}</span>
                        ))}
                      </div>

                      {triage.recommendedAction !== "NONE" && (
                        <button
                          className="secondary-button action-create"
                          onClick={submitRecommendedAction}
                          disabled={loading || !!action}
                        >
                          <ArrowRight size={16} /> Create recommended action
                        </button>
                      )}
                    </div>
                  )}

                  {reply && (
                    <div className="result-card">
                      <div className="section-title">
                        <h3><FileText size={17} /> Draft reply</h3>
                        <span className={`status-pill ${reply.status === "DRAFT" ? "success" : "warning"}`}>
                          {reply.status.replaceAll("_", " ")}
                        </span>
                      </div>
                      <div className="reply-content">{reply.draftReply}</div>
                      <div className="citation-list">
                        <span className="field-label">POLICY CITATIONS</span>
                        {reply.citations.map((citation) => (
                          <span className="citation-chip" key={citation.documentId}>
                            {citation.documentId} · {citation.title}
                          </span>
                        ))}
                      </div>
                      <p className="muted-note">
                        Review the draft and citations before sending it to the customer.
                      </p>
                    </div>
                  )}

                  {action && (
                    <div className="result-card action-card">
                      <div className="section-title">
                        <h3><ClipboardList size={17} /> Action review</h3>
                        <span className="status-pill">{action.status}</span>
                      </div>
                      <p><strong>{action.actionType.replaceAll("_", " ")}</strong></p>
                      <p className="muted-note">Action ID: {action.actionId}</p>
                      <p>{action.reason}</p>

                      {action.status === "PENDING_APPROVAL" && (
                        <div className="button-row">
                          <button className="primary-button" onClick={() => reviewAction("APPROVE")} disabled={loading}>
                            <CheckCircle2 size={16} /> Approve
                          </button>
                          <button className="danger-button" onClick={() => reviewAction("REJECT")} disabled={loading}>
                            <XCircle size={16} /> Reject
                          </button>
                        </div>
                      )}

                      {action.status === "APPROVED" && (
                        <button className="primary-button" onClick={executeAction} disabled={loading}>
                          <Play size={16} /> Execute action
                        </button>
                      )}

                      {action.status === "EXECUTED" && (
                        <p className="success-message"><CheckCircle2 size={16} /> Action executed successfully.</p>
                      )}
                    </div>
                  )}
                  <div className="result-card trace-history-card">
                  <div className="section-title">
                    <div>
                      <h3>
                        <Activity size={17} /> AI Execution History
                      </h3>
                      <p className="muted-note">
                        Audit trail of AI operations for this ticket.
                      </p>
                    </div>

                    <button
                      className="icon-button"
                      onClick={() => loadTraces(selected.ticketId)}
                      disabled={loading}
                      title="Refresh trace history"
                    >
                      <RefreshCw size={17} />
                    </button>
                  </div>

                  {traces.length === 0 ? (
                    <div className="empty-state">
                      No AI execution traces available for this ticket.
                      Run triage or generate a reply to create a trace.
                    </div>
                  ) : (
                    <div className="trace-list">
                      {traces.map((trace) => (
                        <div className="trace-item" key={trace.traceId}>
                          <div className="trace-item-header">
                            <div className="trace-type">
                              <span className="trace-icon">
                                {trace.runType === "TRIAGE" ? (
                                  <Sparkles size={16} />
                                ) : (
                                  <FileText size={16} />
                                )}
                              </span>

                              <div>
                                <strong>
                                  {trace.runType.replaceAll("_", " ")}
                                </strong>
                                <span className="muted-note">
                                  {new Date(trace.createdAt).toLocaleString()}
                                </span>
                              </div>
                            </div>

                            <span
                              className={`status-pill ${
                                trace.finalStatus === "COMPLETED" ||
                                trace.finalStatus === "DRAFT"
                                  ? "success"
                                  : "warning"
                              }`}
                            >
                              {trace.finalStatus.replaceAll("_", " ")}
                            </span>
                          </div>

                          <div className="trace-details">
                            <div>
                              <span className="field-label">RETRIEVED DOCUMENTS</span>
                              <div className="citation-list">
                                {trace.retrievedDocumentIds
                                  ? trace.retrievedDocumentIds.split(",").map((id) => (
                                      <span className="citation-chip" key={id}>
                                        {id.trim()}
                                      </span>
                                    ))
                                  : <span className="muted-note">None</span>}
                              </div>
                            </div>

                            <div>
                              <span className="field-label">TOOL ACTIONS</span>
                              <p>{trace.toolActions || "No action recorded"}</p>
                            </div>

                            <div>
                              <span className="field-label">GUARDRAIL RESULT</span>
                              <span
                                className={`status-pill ${
                                  trace.guardrailResult === "PASSED"
                                    ? "success"
                                    : "warning"
                                }`}
                              >
                                <ShieldCheck size={13} />
                                {trace.guardrailResult.replaceAll("_", " ")}
                              </span>
                            </div>
                          </div>

                          <p className="trace-id">
                            Trace ID: {trace.traceId}
                          </p>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
                </>
              ) : (
                <div className="empty-state large">Select a ticket to view its details.</div>
              )}
            </section>
          </div>
                ) : activeTab === "actions" ? (
          <section className="evaluation-page">
            <div className="evaluation-header">
              <div>
                <h2>Action Approval Queue</h2>
                <p>Review and manage support actions across all tickets.</p>
              </div>

              <button
                className="secondary-button"
                onClick={loadActions}
                disabled={loading}
              >
                <RefreshCw size={17} /> Refresh
              </button>
            </div>

            <div className="detail-card">
              <label htmlFor="action-status-filter">
                Filter by status
              </label>

              <select
                id="action-status-filter"
                value={actionStatusFilter}
                onChange={(e) => setActionStatusFilter(e.target.value)}
              >
                <option value="ALL">All actions</option>
                <option value="PENDING_APPROVAL">Pending approval</option>
                <option value="APPROVED">Approved</option>
                <option value="REJECTED">Rejected</option>
                <option value="EXECUTED">Executed</option>
                <option value="FAILED">Failed</option>
              </select>
            </div>

            <div className="evaluation-table">
              <h3>Actions ({actions.length})</h3>

              {actions.length === 0 ? (
                <div className="empty-state">
                  No actions found for this filter.
                </div>
              ) : (
                <div className="action-queue-list">
                  {actions.map((item) => (
                    <div className="detail-card" key={item.actionId}>
                      <div className="section-title">
                        <h3>
                          {item.actionType.replaceAll("_", " ")}
                        </h3>
                        <span className="status-pill">
                          {item.status.replaceAll("_", " ")}
                        </span>
                      </div>

                      <p>
                        <strong>Ticket:</strong> {item.ticketId}
                      </p>

                      <p>
                        <strong>Action ID:</strong> {item.actionId}
                      </p>

                      <p>
                        <strong>Approval required:</strong>{" "}
                        {item.requiresHumanApproval ? "Yes" : "No"}
                      </p>

                      <p>{item.reason}</p>

                      <p className="muted-note">
                        Created: {new Date(item.createdAt).toLocaleString()}
                      </p>

                      {item.status === "PENDING_APPROVAL" && (
                        <div className="button-row">
                          <button
                            className="primary-button"
                            disabled={loading}
                            onClick={() =>
                              reviewQueuedAction(item.actionId, "APPROVE")
                            }
                          >
                            <CheckCircle2 size={16} /> Approve
                          </button>

                          <button
                            className="danger-button"
                            disabled={loading}
                            onClick={() =>
                              reviewQueuedAction(item.actionId, "REJECT")
                            }
                          >
                            <XCircle size={16} /> Reject
                          </button>
                        </div>
                      )}

                      {item.status === "APPROVED" && (
                        <button
                          className="primary-button"
                          disabled={loading}
                          onClick={() => executeQueuedAction(item.actionId)}
                        >
                          <Play size={16} /> Execute action
                        </button>
                      )}

                      {item.status === "REJECTED" && (
                        <p className="muted-note">
                          Action rejected. Execution is not permitted.
                        </p>
                      )}

                      {item.status === "EXECUTED" && (
                        <p className="success-message">
                          <CheckCircle2 size={16} /> Action executed.
                        </p>
                      )}

                      {item.status === "FAILED" && (
                        <p className="muted-note">
                          Action execution failed.
                        </p>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          </section>
          ) : activeTab === "audit" ? (
          <section className="evaluation-page">
            <div className="evaluation-header">
              <div>
                <h2>Audit & Observability</h2>
                <p>Monitor AI execution, knowledge retrieval and support actions.</p>
              </div>

              <button
                className="secondary-button"
                onClick={() => {
                  setLoading(true);
                  Promise.all([loadAllTraces(), loadActionsForAudit()])
                    .catch((e) => setError(e.message))
                    .finally(() => setLoading(false));
                }}
                disabled={loading}
              >
                <RefreshCw size={17} /> Refresh
              </button>
            </div>

            <div className="evaluation-summary">
              <div className="metric-card">
                <span>Total AI runs</span>
                <strong>{allTraces.length}</strong>
              </div>
              <div className="metric-card">
                <span>Escalations / review required</span>
                <strong>
                  {allTraces.filter(
                    (t) => t.guardrailResult === "REVIEW_REQUIRED"
                  ).length}
                </strong>
              </div>
              <div className="metric-card">
                <span>Support actions</span>
                <strong>{actions.length}</strong>
              </div>
              <div className="metric-card">
                <span>Executed actions</span>
                <strong>
                  {actions.filter((a) => a.status === "EXECUTED").length}
                </strong>
              </div>
            </div>

            <div className="detail-card">
              <div className="section-title">
                <h3>AI execution traces</h3>
                <select
                  value={traceFilter}
                  onChange={(e) => setTraceFilter(e.target.value)}
                >
                  <option value="ALL">All runs</option>
                  <option value="TRIAGE">Triage</option>
                  <option value="DRAFT_REPLY">Draft replies</option>
                  <option value="REVIEW_REQUIRED">Review required</option>
                </select>
              </div>

              <div className="trace-list">
                {allTraces
                  .filter((trace) =>
                    traceFilter === "ALL"
                      ? true
                      : traceFilter === "REVIEW_REQUIRED"
                        ? trace.guardrailResult === "REVIEW_REQUIRED"
                        : trace.runType === traceFilter
                  )
                  .map((trace) => (
                    <div className="trace-item" key={trace.traceId}>
                      <div className="trace-item-header">
                        <div>
                          <strong>{trace.runType.replaceAll("_", " ")}</strong>
                          <p className="muted-note">
                            Ticket: {trace.ticketId}
                          </p>
                        </div>
                        <span className={`status-pill ${
                          trace.guardrailResult === "PASSED" ? "success" : "warning"
                        }`}>
                          {trace.guardrailResult}
                        </span>
                      </div>

                      <p>
                        <strong>Outcome:</strong> {trace.finalStatus}
                      </p>
                      <p>
                        <strong>Recommended action:</strong> {trace.toolActions}
                      </p>
                      <div className="citation-list">
                        {trace.retrievedDocumentIds.split(",").filter(Boolean).map((id) => (
                          <span className="citation-chip" key={id}>{id.trim()}</span>
                        ))}
                      </div>
                      <p className="muted-note">
                        {new Date(trace.createdAt).toLocaleString()}
                      </p>
                    </div>
                  ))}
              </div>
            </div>

            <div className="detail-card">
              <h3>Support action audit</h3>
              {auditActions.map((item) => (
                <div className="trace-item" key={item.actionId}>
                  <div className="trace-item-header">
                    <strong>{item.actionType.replaceAll("_", " ")}</strong>
                    <span className="status-pill">{item.status}</span>
                  </div>
                  <p>Ticket: {item.ticketId}</p>
                  <p>Action ID: {item.actionId}</p>
                  <p>{item.reason}</p>
                </div>
              ))}
            </div>
          </section>
        ) : (
          <section className="evaluation-page">
            <div className="evaluation-header">
              <div>
                <h2>Evaluation suite</h2>
                <p>Measure triage quality, policy compliance and response safety.</p>
              </div>
              <button className="primary-button" onClick={runEvaluation} disabled={loading}>
                <Play size={17} /> Run evaluations
              </button>
            </div>

            {evaluation ? (
              <>
                <div className="evaluation-summary">
                  <div className="evaluation-total">
                    <span className="field-label">CASES PASSED</span>
                    <strong>{evaluation.passedCases}<small> / {evaluation.totalCases}</small></strong>
                  </div>
                  {[
                    ["Category accuracy", evaluation.categoryAccuracy],
                    ["Priority accuracy", evaluation.priorityAccuracy],
                    ["Citation coverage", evaluation.citationCoverage],
                    ["Unsafe action blocking", evaluation.unsafeActionBlockingRate],
                    ["Escalation accuracy", evaluation.escalationAccuracy],
                    ["Answer coverage", evaluation.answerRequirementCoverage]
                  ].map(([label, value]) => (
                    <div className="metric-card" key={String(label)}>
                      <span>{label}</span>
                      <strong>{Number(value).toFixed(1)}%</strong>
                      <div className="metric-track">
                        <div style={{ width: `${Number(value)}%` }} />
                      </div>
                    </div>
                  ))}
                </div>

                <div className="evaluation-table">
                  <h3>Individual test results</h3>
                  <div className="table-scroll">
                    <table>
                      <thead>
                        <tr>
                          <th>Case</th>
                          <th>Ticket</th>
                          <th>Category</th>
                          <th>Priority</th>
                          <th>Action policy</th>
                          <th>Escalation</th>
                          <th>Result</th>
                        </tr>
                      </thead>
                      <tbody>
                        {evaluation.results.map((item) => (
                          <tr key={item.caseId}>
                            <td>{item.caseId}</td>
                            <td>{item.ticketId}</td>
                            <td>{item.actualCategory}</td>
                            <td>{item.actualPriority}</td>
                            <td>{item.actionPolicyCompliant ? "Pass" : "Fail"}</td>
                            <td>{item.escalationCorrect ? "Pass" : "Fail"}</td>
                            <td>
                              <span className={`status-pill ${item.passed ? "success" : "warning"}`}>
                                {item.passed ? "Passed" : "Failed"}
                              </span>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              </>
            ) : (
              <div className="empty-state large">
                <Activity size={30} />
                <h3>No evaluation run yet</h3>
                <p>Run the evaluation suite to see your AI quality metrics.</p>
              </div>
            )}
          </section>
        )}

        {loading && <div className="loading-indicator">Processing request…</div>}
      </main>
    </div>
  );
}

function Login({ onLogin }: { onLogin: (credentials: Credentials) => void }) {
  const [username, setUsername] = useState("user");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError("");

    try {
      const response = await fetch(`${API}/api/tickets?page=0&size=1`, {
        headers: {
          Authorization: `Basic ${btoa(`${username}:${password}`)}`
        }
      });

      if (!response.ok) {
        throw new Error("Sign-in failed. Check your username and password.");
      }

      onLogin({ username, password });
    } catch (e) {
      setError((e as Error).message);
    }
  };

  return (
    <div className="login-page">
      <form className="login-card" onSubmit={submit}>
        <div className="brand-icon login-icon"><ShieldCheck size={25} /></div>
        <h1>TrustDesk</h1>
        <p>Sign in to your support operations workspace.</p>

        <label>Username</label>
        <input value={username} onChange={(e) => setUsername(e.target.value)} required />

        <label>Password</label>
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />

        {error && <div className="login-error">{error}</div>}

        <button className="primary-button login-button" type="submit">
          Sign in <ArrowRight size={17} />
        </button>
        <span className="login-footnote">Credentials are kept in memory for this session.</span>
      </form>
    </div>
  );
}

export default App;
