/** Reuses the previous approval cards with the scoped pending queue, required comments and paginated responses. */
import {useCallback,useEffect,useState} from 'react';
import {Link} from 'react-router-dom';
import {api} from '../api.js';
import {Alert,Empty,Page,Pagination,Status,fmtDate,EMPTY_PAGE} from '../components/ui.jsx';
export default function Approvals() {
  const [page,setPage]=useState(EMPTY_PAGE),[number,setNumber]=useState(0),[comments,setComments]=useState({});
  const [error,setError]=useState(''),[ok,setOk]=useState(''),[loading,setLoading]=useState(true),[busy,setBusy]=useState(null);
  const load=useCallback(()=>{setLoading(true);setError('');return api.pending(number).then(setPage)
    .catch(e=>setError(e.message)).finally(()=>setLoading(false));},[number]);
  useEffect(()=>{load();},[load]);
  function decide(leave,approve) {
    const comment=(comments[leave.leaveId]||'').trim();
    if(!comment || busy!==null) return;
    setBusy(leave.leaveId);setError('');setOk('');
    (approve?api.approve(leave.leaveId,comment):api.reject(leave.leaveId,comment))
      .then(()=>{setOk('Request #'+leave.leaveId+(approve?' approved.':' rejected.'));return load();})
      .catch(e=>setError(e.message)).finally(()=>setBusy(null));
  }
  return <Page title="Approvals" subtitle="Pending leave requests assigned to you" back={false}>
    <Alert>{error}</Alert><Alert type="success">{ok}</Alert>
    <button className="btn btn-outline-success btn-sm mb-3" disabled={loading || busy!==null} onClick={load}>Refresh requests</button>
    {loading?<p role="status">Loading requests…</p>:page.content.length===0?<Empty>No pending requests.</Empty>:<div className="row g-3">
      {page.content.map(leave=><div className="col-md-6" key={leave.leaveId}><div className="card shadow-sm h-100"><div className="card-body">
        <div className="d-flex justify-content-between"><strong>Employee #{leave.empId}</strong><Status value={leave.status}/></div>
        <p className="mb-1 mt-2">Request #{leave.leaveId} · Leave type #{leave.leaveTypeId}</p>
        <p>{fmtDate(leave.startDate)} → {fmtDate(leave.endDate)} · {leave.noOfDaysRequested} day(s)</p>
        <p className="text-secondary fst-italic">{leave.reason}</p>
        <label htmlFor={'comment-'+leave.leaveId} className="form-label">Decision comment <span className="text-danger">*</span></label>
        <textarea id={'comment-'+leave.leaveId} className="form-control decision-comment mb-2" maxLength={200} required
          value={comments[leave.leaveId]||''} onChange={event=>setComments({...comments,[leave.leaveId]:event.target.value})}/>
        <small className="text-secondary">A comment is required for approval or rejection (maximum 200 characters).</small>
        <div className="d-flex gap-2 flex-wrap mt-3">
          <button className="btn btn-success" disabled={busy!==null || !(comments[leave.leaveId]||'').trim()} onClick={()=>decide(leave,true)}>Approve</button>
          <button className="btn btn-outline-danger" disabled={busy!==null || !(comments[leave.leaveId]||'').trim()} onClick={()=>decide(leave,false)}>Reject</button>
          <Link className="btn btn-link btn-sm" to={'/audit?employeeId='+leave.empId}>View audit</Link>
        </div>{busy===leave.leaveId && <p className="small mt-2" role="status">Recording decision…</p>}
      </div></div></div>)}</div>}
    <Pagination page={page} onChange={setNumber} busy={loading || busy!==null}/>
  </Page>;
}
