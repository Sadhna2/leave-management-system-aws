/** Restores identity from the authenticated backend; passwords and user roles are never trusted from local storage. */
import {createContext,useContext,useEffect,useState} from 'react';
import {api,session} from './api.js';
const AuthContext=createContext(null);
export const ROLE_LABEL={EMPLOYEE:'Employee',MANAGER:'Reporting Manager',HR:'HR',ADMIN:'Admin'};
export function AuthProvider({children}) {
  const [user,setUser]=useState(null),[checking,setChecking]=useState(Boolean(session.getToken()));
  useEffect(()=>{
    const expire=()=>{setUser(null);setChecking(false);};
    window.addEventListener('lms-session-expired',expire);
    if(session.getToken()) api.me().then(setUser).catch(()=>{session.setToken(null);setUser(null);}).finally(()=>setChecking(false));
    return ()=>window.removeEventListener('lms-session-expired',expire);
  },[]);
  const login=credentials=>api.login(credentials.username,credentials.password).then(result=>{
    session.setToken(result.accessToken);setUser({username:result.username,empId:result.empId,role:result.role});
  });
  const logout=()=>api.logout().finally(()=>{session.setToken(null);setUser(null);});
  return <AuthContext.Provider value={{user,checking,login,logout}}>{children}</AuthContext.Provider>;
}
export const useAuth=()=>useContext(AuthContext);
