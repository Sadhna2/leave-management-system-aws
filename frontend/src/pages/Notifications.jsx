/** Reuses the original notification list with owned-record pagination and mark-as-read support. */
import {useCallback,useEffect,useState} from 'react';
import {api} from '../api.js';
import {Alert,Empty,Page,Pagination,EMPTY_PAGE} from '../components/ui.jsx';
export default function Notifications() {
  const [page,setPage]=useState(EMPTY_PAGE),[number,setNumber]=useState(0),[error,setError]=useState(''),[busy,setBusy]=useState(false);
  const load=useCallback(()=>{setBusy(true);setError('');return api.notifications(number).then(setPage)
    .catch(e=>setError(e.message)).finally(()=>setBusy(false));},[number]);
  useEffect(()=>{load();},[load]);
  function markRead(id) {setBusy(true);api.readNotification(id).then(load).catch(e=>setError(e.message)).finally(()=>setBusy(false));}
  return <Page title="Notifications" subtitle="Updates addressed to your employee account" back={false}>
    <Alert>{error}</Alert>{busy && <p role="status">Loading notifications…</p>}
    {!busy && page.content.length===0?<Empty>You're all caught up.</Empty>:<ul className="list-group shadow-sm">
      {page.content.map(item=><li className="list-group-item d-flex justify-content-between align-items-center gap-3" key={item.notificationId}>
        <div><span className={'badge me-2 '+(item.readFlag?'bg-secondary':'bg-success')}>{item.readFlag?'Read':'Unread'}</span>
          {item.message}<small className="text-secondary d-block mt-2">{new Date(item.createdAt+'Z').toLocaleString()}</small></div>
        {!item.readFlag && <button className="btn btn-outline-success btn-sm" disabled={busy} onClick={()=>markRead(item.notificationId)}>Mark read</button>}
      </li>)}</ul>}
    <Pagination page={page} onChange={setNumber} busy={busy}/>
  </Page>;
}
