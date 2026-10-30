// Run locally with an authorized Firebase CLI session. Never include this in Android assets.
const fs=require('fs');const path=require('path');const crypto=require('crypto');const auth=require('firebase-tools/lib/auth');
(async()=>{
 const email=(process.argv[2]||'').trim().toLowerCase();
 if(!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email))throw Error('Provide the administrator email as the first argument');
 const root=path.resolve(__dirname,'..');
 const cfg=JSON.parse(fs.readFileSync(path.join(root,'app/google-services.json'),'utf8').replace(/^\uFEFF/,''));
 const project=cfg.project_info.project_id;if(project!=='coinly-44f3b')throw Error('Unexpected Firebase project');
 const account=auth.getGlobalDefaultAccount();if(!account)throw Error('Run firebase login first');
 const access=await auth.getAccessToken(account.tokens.refresh_token,['https://www.googleapis.com/auth/cloud-platform']);
 async function api(suffix,body){
  const response=await fetch(`https://identitytoolkit.googleapis.com/v1/projects/${project}/${suffix}`,{method:'POST',headers:{Authorization:`Bearer ${access.access_token}`,'Content-Type':'application/json'},body:JSON.stringify(body),signal:AbortSignal.timeout(20000)});
  const data=await response.json();if(!response.ok)throw Error(data.error?.message||`HTTP_${response.status}`);return data;
 }
 const found=await api('accounts:lookup',{email:[email]});let user=found.users?.[0];let created=false;
 if(!user){
  if(!process.argv.includes('--create'))throw Error('User not found; use --create to create this account');
  const password='Coinly1!'+crypto.randomBytes(24).toString('base64url');
  const result=await api('accounts',{email,password,displayName:'Administrador Coinly',emailVerified:false,returnSecureToken:false});
  user={localId:result.localId,customAttributes:'{}'};created=true;
  const secretDir=path.join(root,'private');fs.mkdirSync(secretDir,{recursive:true});
  fs.writeFileSync(path.join(secretDir,'news-admin-'+user.localId+'-initial-login.txt'),`Correo: ${email}\nContraseña inicial: ${password}\nCambia la contraseña al entrar. No publiques este archivo.\n`,{flag:'wx',mode:0o600});
 }
 if(user.disabled)throw Error('This account is disabled; it was not modified');
 const claims=JSON.parse(user.customAttributes||'{}');
 await api('accounts:update',{localId:user.localId,customAttributes:JSON.stringify({...claims,newsAdmin:true})});
 const verification=await api('accounts:lookup',{localId:[user.localId]});
 if(JSON.parse(verification.users?.[0]?.customAttributes||'{}').newsAdmin!==true)throw Error('Administrator claim verification failed');
 console.log(JSON.stringify({created,newsAdmin:true,initialCredentials:created?'private/news-admin-'+user.localId+'-initial-login.txt':null}));
})().catch(error=>{console.error(error.message);process.exitCode=1;});
