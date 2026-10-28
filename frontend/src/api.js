/** Central API client for Member 3 only; runtime configuration keeps the API stage out of the compiled bundle. */
let token=sessionStorage.getItem('lms_access_token');
export const session={getToken:()=>token,setToken(value){
  token=value;
  if(value) sessionStorage.setItem('lms_access_token',value); else sessionStorage.removeItem('lms_access_token');
}};
const config=fetch('/config.json',{cache:'no-store'}).then(response=>response.json()).then(value=>{
  const base=value.apiBaseUrl || (['localhost','127.0.0.1'].includes(location.hostname)?'http://localhost:8080':'');
  if(!base) throw new Error('The service is temporarily unavailable. Please contact your administrator.');
  const parsed=new URL(base);
  if(parsed.protocol!=='https:' && !['localhost','127.0.0.1'].includes(parsed.hostname)) throw new Error('A secure service connection is required.');
  return base.replace(/\/$/,'');
});
/** Converts every failed response to a displayable error and expires the local session on HTTP 401. */
function request(path,options={}) {
  return config.then(base=>fetch(base+path,{...options,headers:{
    ...(options.body?{'Content-Type':'application/json'}:{}),...(token?{Authorization:'Bearer '+token}:{}),...options.headers
  }})).then(async response=>{
    if(response.status===204) return null;
    const value=await response.json().catch(()=>null);
    if(response.status===401 && token) {session.setToken(null);window.dispatchEvent(new Event('lms-session-expired'));}
    if(!response.ok) throw new Error(value?.message || 'The request could not be completed ('+response.status+').');
    return value;
  }).catch(error=>{throw error instanceof TypeError?new Error('Cannot reach the service. Please check your connection and retry.'):error;});
}
const post=(path,body)=>request(path,{method:'POST',...(body?{body:JSON.stringify(body)}:{})});
export const api={
  login:(username,password)=>post('/api/auth/login',{username,password}),
  me:()=>request('/api/auth/me'),logout:()=>post('/api/auth/logout'),
  pending:(page=0)=>request('/api/manager/leaves/pending?page='+page+'&size=20'),
  approve:(id,comment)=>post('/api/manager/leave/'+id+'/approve',{comment}),
  reject:(id,comment)=>post('/api/manager/leave/'+id+'/reject',{comment}),
  notifications:(page=0)=>request('/api/notifications?page='+page+'&size=20'),
  readNotification:id=>request('/api/notifications/'+id+'/read',{method:'PATCH'}),
  audit:(empId,page=0)=>request('/api/audit-logs/employee/'+encodeURIComponent(empId)+'?page='+page+'&size=20')
};
