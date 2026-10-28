/** Includes only login, approvals, notifications and audit; unrelated employee and HR routes are excluded. */
import {Navigate,Route,Routes} from 'react-router-dom';
import {useAuth} from './auth.jsx';
import Layout from './components/Layout.jsx';
import Login from './pages/Login.jsx';
import Approvals from './pages/Approvals.jsx';
import Notifications from './pages/Notifications.jsx';
import Audit from './pages/Audit.jsx';
function Protected({manager=false,children}) {
  const {user,checking}=useAuth();
  if(checking) return <div className="page-loading" role="status">Restoring your session…</div>;
  if(!user) return <Navigate to="/login" replace/>;
  if(manager && user.role!=='MANAGER') return <Navigate to="/notifications" replace/>;
  return <Layout>{children}</Layout>;
}
export default function App() {
  const {user}=useAuth();
  return <Routes>
    <Route path="/login" element={<Login/>}/>
    <Route path="/approvals" element={<Protected manager><Approvals/></Protected>}/>
    <Route path="/notifications" element={<Protected><Notifications/></Protected>}/>
    <Route path="/audit" element={<Protected><Audit/></Protected>}/>
    <Route path="*" element={<Navigate to={user?(user.role==='MANAGER'?'/approvals':'/notifications'):'/login'} replace/>}/>
  </Routes>;
}
