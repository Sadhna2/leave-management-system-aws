/** Shared page, alert, status and date presentation reused from the previous project. */
import { Link } from "react-router-dom";

export function Page({ title, subtitle, children, back = true }) {
  return (
    <div>
      <div className="d-flex justify-content-between align-items-start flex-wrap gap-2 mb-1">
        <h1 className="h3 m-0" style={{ color: "#064e3b" }}>{title}</h1>
        {back && <Link to="/" className="btn btn-outline-success btn-sm">← Back to workspace</Link>}
      </div>
      {subtitle && <p className="text-secondary">{subtitle}</p>}
      {children}
    </div>
  );
}

export function Alert({ type = "danger", children }) {
  if (!children) return null;
  return <div className={"alert alert-" + type}>{children}</div>;
}

const COLORS = { PENDING: "warning text-dark", APPROVED: "success", REJECTED: "danger", CANCELLED: "secondary" };

export function Status({ value }) {
  return <span className={"badge rounded-pill bg-" + (COLORS[value] || "secondary")}>{value}</span>;
}

export function Empty({ children }) {
  return <div className="empty-box text-center text-secondary p-4">{children}</div>;
}

// Backend sends LocalDate as "2026-10-04" and LocalDateTime as an ISO string.
export const fmtDate = (d) =>
  d ? new Date(d).toLocaleDateString(undefined, { day: "2-digit", month: "short", year: "numeric" }) : "-";

/** Uses the backend's stable page metadata without silently hiding additional records. */
export function Pagination({page,onChange,busy}) {
  return <nav aria-label="Pagination" className="d-flex justify-content-between align-items-center mt-3">
    <button className="btn btn-outline-success btn-sm" disabled={busy || page.number===0} onClick={()=>onChange(page.number-1)}>Previous</button>
    <span className="text-secondary small">{page.totalElements} record(s) · Page {page.number+1} of {Math.max(1,page.totalPages)}</span>
    <button className="btn btn-outline-success btn-sm" disabled={busy || page.number+1>=page.totalPages} onClick={()=>onChange(page.number+1)}>Next</button>
  </nav>;
}
export const EMPTY_PAGE={content:[],number:0,size:20,totalElements:0,totalPages:0};
