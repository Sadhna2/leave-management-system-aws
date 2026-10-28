/** Adapts the original split-screen login to authenticated bearer tokens and explicit employee identity. */
import {useState} from 'react';
import {Navigate,useNavigate} from 'react-router-dom';
import {useAuth} from '../auth.jsx';
import {Alert} from '../components/ui.jsx';
export default function Login() {
  const {user,login,checking}=useAuth(),navigate=useNavigate();
  const [username,setUsername]=useState(''),[password,setPassword]=useState(''),[error,setError]=useState(''),[busy,setBusy]=useState(false);
  if(checking) return <div className="page-loading" role="status">Restoring your session…</div>;
  if(user) return <Navigate to="/" replace/>;
  function submit(event) {
    event.preventDefault();setError('');setBusy(true);
    login({username:username.trim(),password}).then(()=>{setPassword('');navigate('/');})
      .catch(error=>setError(error.message)).finally(()=>setBusy(false));
  }
  return <div className="row g-0 min-vh-100">
    <div className="col-lg-6 auth-hero d-none d-lg-flex"><span className="brand-logo mb-4">L</span>
      <h1 className="display-5 fw-bold">Review leave with confidence.</h1><p className="fs-5 opacity-75">Approvals, notifications and a clear decision history.</p></div>
    <div className="col-lg-6 auth-side d-flex align-items-center justify-content-center p-4">
      <form className="card auth-card p-4" onSubmit={submit}>
        <div className="d-flex align-items-center gap-2 mb-3 fw-bold fs-4" style={{color:'#064e3b'}}><span className="brand-logo">L</span> LeaveHub</div>
        <h2 className="h4">Welcome back</h2><p className="text-secondary">Sign in to your approval workspace.</p><Alert>{error}</Alert>
        <div className="mb-3"><label htmlFor="username" className="form-label fw-semibold">Username</label>
          <input id="username" className="form-control" autoComplete="username" maxLength={50} value={username} onChange={e=>setUsername(e.target.value)} required/></div>
        <div className="mb-3"><label htmlFor="password" className="form-label fw-semibold">Password</label>
          <input id="password" type="password" className="form-control" autoComplete="current-password" maxLength={72} value={password} onChange={e=>setPassword(e.target.value)} required/></div>
        <button className="btn btn-primary w-100" disabled={busy}>{busy?'Signing in…':'Sign in'}</button>
        <p className="small text-secondary mt-3 mb-0">Use the account provided by your administrator.</p>
      </form></div></div>;
}
