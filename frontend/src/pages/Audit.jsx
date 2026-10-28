/** Adds the required read-only audit screen; managers can inspect assigned employees and employees see their own history. */
import {useCallback,useEffect,useState} from 'react';
import {useSearchParams} from 'react-router-dom';
import {api} from '../api.js';
import {useAuth} from '../auth.jsx';
import {Alert,Empty,Page,Pagination,Status,EMPTY_PAGE} from '../components/ui.jsx';
export default function Audit() {
  const {user}=useAuth(),[params]=useSearchParams();
  const [selected,setSelected]=useState(user.role==='MANAGER'?(params.get('employeeId')||String(user.empId)):String(user.empId));
  const [employee,setEmployee]=useState(selected),[page,setPage]=useState(EMPTY_PAGE),[number,setNumber]=useState(0);
  const [error,setError]=useState(''),[busy,setBusy]=useState(true);
  const load=useCallback(()=>{setBusy(true);setError('');return api.audit(employee,number).then(setPage)
    .catch(e=>{setPage(EMPTY_PAGE);setError(e.message);}).finally(()=>setBusy(false));},[employee,number]);
  useEffect(()=>{load();},[load]);
  function submit(event) {event.preventDefault();setNumber(0);setEmployee(selected);}
  return <Page title="Audit log" subtitle="Recorded manager decisions and comments" back={false}>
    {user.role==='MANAGER' && <form onSubmit={submit} className="d-flex align-items-end gap-2 mb-3">
      <div><label htmlFor="audit-employee" className="form-label">Employee ID</label><input id="audit-employee" type="number" min="1" step="1"
        max="2147483647" className="form-control" required value={selected} onChange={e=>setSelected(e.target.value)}/></div>
      <button className="btn btn-outline-success" disabled={busy}>View history</button></form>}
    <Alert>{error}</Alert>{busy?<p role="status">Loading audit history…</p>:page.content.length===0?<Empty>No decision history for this employee.</Empty>:
      <div className="table-responsive"><table className="table table-hover align-middle"><thead className="table-light"><tr>
        <th>When</th><th>Leave request</th><th>Decision</th><th>Manager</th><th>Comment</th></tr></thead><tbody>
        {page.content.map(item=><tr key={item.auditId}><td>{new Date(item.actionDate+'Z').toLocaleString()}</td><td>#{item.leaveId}</td>
          <td><Status value={item.actionStatus}/></td><td>#{item.actorEmpId}</td><td className="audit-details">{item.details}</td></tr>)}
      </tbody></table></div>}
    <Pagination page={page} onChange={setNumber} busy={busy}/>
  </Page>;
}
