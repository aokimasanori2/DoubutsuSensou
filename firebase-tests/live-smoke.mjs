// Explicit opt-in: creates one real room and three temporary anonymous test users.
import {readFileSync} from 'node:fs';
import {randomInt} from 'node:crypto';
import assert from 'node:assert/strict';
import {initializeApp,deleteApp} from 'firebase/app';
import {getAuth,signInAnonymously,deleteUser} from 'firebase/auth';
import {getFirestore,doc,runTransaction,getDocFromServer,onSnapshot,terminate} from 'firebase/firestore';

if(process.argv[2]!=='--project=doubutsusensou-online') throw new Error('Explicit project opt-in required');
const config=JSON.parse(readFileSync(new URL('../app/google-services.json',import.meta.url),'utf8'));
assert.equal(config.project_info.project_id,'doubutsusensou-online');
const client=config.client.find(c=>c.client_info.android_client_info.package_name==='com.aokimasanori.doubutsusensou');
const sessions=[];
let step='authentication';
const timeout=setTimeout(()=>{console.error('Live test timed out');process.exit(1)},90000);
try {
  for(let i=0;i<3;i++) {
    const app=initializeApp({apiKey:client.api_key[0].current_key,projectId:config.project_info.project_id,
      appId:client.client_info.mobilesdk_app_id},`smoke-${i}-${Date.now()}`);
    const auth=getAuth(app);const {user}=await signInAnonymously(auth);
    sessions.push({app,auth,user,db:getFirestore(app)});
  }
  const [host,guest,third]=sessions;
  const code=String(randomInt(10000000,100000000));
  const ref=s=>doc(s.db,'rooms',code);
  let state={schema:1,hostUid:host.user.uid,guestUid:'',phase:'WAITING',readyOne:false,readyTwo:false,
    revision:0,expiresAt:Date.now()+86400000,positions:Array(20).fill(-1),activePlayer:'ONE',turn:1,winner:'',reason:''};
  step='create';
  await runTransaction(host.db,async tx=>{assert.equal((await tx.get(ref(host))).exists(),false);tx.set(ref(host),state)});
  const change=async(s,edit)=>runTransaction(s.db,async tx=>{
    const current=(await tx.get(ref(s))).data();const next=edit(current);tx.set(ref(s),next);return next;
  });
  step='join';
  await change(guest,d=>({...d,guestUid:guest.user.uid,phase:'SETUP',revision:d.revision+1}));
  await assert.rejects(getDocFromServer(ref(third)),e=>e.code==='permission-denied');
  step='prepare host';
  await change(host,d=>({...d,readyOne:true,revision:d.revision+1,positions:d.positions.map((x,i)=>i<10?6+i:x)}));
  step='prepare guest';
  state=await change(guest,d=>({...d,readyTwo:true,phase:'PLAYING',revision:d.revision+1,positions:d.positions.map((x,i)=>i>=10?26+i:x)}));
  assert.equal(state.revision,3);
  const observed=new Promise((resolve,reject)=>{
    const stop=onSnapshot(ref(guest),snapshot=>{
      if(snapshot.data()?.revision===4){stop();resolve(snapshot.data())}
    },reject);
  });
  step='move host';
  await change(host,d=>({...d,revision:d.revision+1,turn:2,activePlayer:'TWO',positions:d.positions.map((x,i)=>i===0?0:x)}));
  const moved=await observed;assert.equal(moved.positions[0],0);
  assert.deepEqual((await getDocFromServer(ref(guest))).data(),moved);
  step='move guest';
  await change(guest,d=>({...d,revision:d.revision+1,turn:3,activePlayer:'ONE',positions:d.positions.map((x,i)=>i===10?30:x)}));
  step='resign';
  await change(host,d=>({...d,revision:d.revision+1,phase:'FINISHED',winner:'TWO',reason:'RESIGNED'}));
  assert.equal((await getDocFromServer(ref(guest))).data().winner,'TWO');
  console.log('PASS: real anonymous sign-in, room creation/join, third-party denial, both placements, two-way moves, listener, resume and resignation.');
} catch(error) { console.error('Failed stage:',step); throw error; } finally {
  for(const s of sessions){await terminate(s.db);await deleteUser(s.user);await deleteApp(s.app)}
  clearTimeout(timeout);
}
