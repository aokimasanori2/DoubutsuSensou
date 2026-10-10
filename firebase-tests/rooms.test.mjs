import {readFileSync} from 'node:fs';
import {before,after,beforeEach,test} from 'node:test';
import assert from 'node:assert/strict';
import {initializeTestEnvironment,assertSucceeds,assertFails} from '@firebase/rules-unit-testing';
import {doc,getDoc,getDocs,collection,setDoc,updateDoc,runTransaction,onSnapshot} from 'firebase/firestore';

let env;
before(async()=>{env=await initializeTestEnvironment({projectId:'demo-doubutsu-online',
  firestore:{host:'127.0.0.1',port:8085,rules:readFileSync(new URL('../firebase/firestore.rules',import.meta.url),'utf8')}})});
after(async()=>{await env.cleanup()});
beforeEach(async()=>{await env.clearFirestore()});
const db=uid=>env.authenticatedContext(uid).firestore();
const ref=(uid,code='12345678')=>doc(db(uid),'rooms',code);
const empty=()=>({schema:1,hostUid:'host',guestUid:'',phase:'WAITING',readyOne:false,readyTwo:false,
  revision:0,expiresAt:Date.now()+86400000,positions:Array(20).fill(-1),activePlayer:'ONE',turn:1,winner:'',reason:''});
async function seed(data){await env.withSecurityRulesDisabled(async c=>setDoc(doc(c.firestore(),'rooms','12345678'),data))}
async function prepared(){
  let data=empty();await setDoc(ref('host'),data);
  data={...data,guestUid:'guest',phase:'SETUP',revision:1};await setDoc(ref('guest'),data);
  data={...data,readyOne:true,revision:2,positions:Array.from({length:20},(_,i)=>i<10?6+i:-1)};
  await setDoc(ref('host'),data);
  data={...data,readyTwo:true,phase:'PLAYING',revision:3,positions:data.positions.map((x,i)=>i>=10?26+i:x)};
  await setDoc(ref('guest'),data);return data;
}

test('anonymous outsiders cannot read and rooms cannot be listed',async()=>{
  await assertSucceeds(setDoc(ref('host'),empty()));
  await assertFails(getDoc(doc(env.unauthenticatedContext().firestore(),'rooms','12345678')));
  await assertFails(getDocs(collection(db('host'),'rooms')));
  await assertFails(setDoc(ref('host','bad-code'),empty()));
});
test('two seats are assigned atomically and a third person cannot enter or read',async()=>{
  await setDoc(ref('host'),empty());
  await assertSucceeds(getDoc(ref('guest')));
  await updateDoc(ref('guest'),{guestUid:'guest',phase:'SETUP',revision:1});
  await assertFails(getDoc(ref('third')));
  await assertFails(updateDoc(ref('third'),{guestUid:'third',revision:2}));
  await assertSucceeds(getDoc(ref('host')));
});
test('readiness preserves the other side and both ready starts the game',async()=>{
  const data=await prepared();assert.equal((await getDoc(ref('host'))).data().phase,'PLAYING');
  await assertFails(updateDoc(ref('guest'),{readyOne:false,revision:4}));
  await assertFails(updateDoc(ref('host'),{hostUid:'third',revision:4}));
  assert.equal(data.positions.filter(x=>x>=0).length,20);
});
test('invalid, duplicate and incomplete placements are rejected',async()=>{
  const data={...empty(),guestUid:'guest',phase:'SETUP',revision:1};await seed(data);
  const submit=positions=>setDoc(ref('host'),{...data,positions,readyOne:true,revision:2});
  await assertFails(submit(Array(20).fill(-1)));
  await assertFails(submit(Array.from({length:20},(_,i)=>i<10?6:-1)));
  await assertFails(submit(Array.from({length:20},(_,i)=>i<10?30+i:-1)));
});
test('only active player can move and repeated revisions are rejected',async()=>{
  const data=await prepared();
  const positions=[...data.positions];positions[0]=0;
  const next={...data,positions,activePlayer:'TWO',turn:2,revision:4};
  await assertFails(setDoc(ref('guest'),next));
  await assertSucceeds(setDoc(ref('host'),next));
  await assertFails(setDoc(ref('host'),next));
  await assertFails(updateDoc(ref('guest'),{expiresAt:Date.now()+90000000,revision:5}));
});
test('opponent cannot teleport and immobile pits cannot move',async()=>{
  const data=await prepared();
  const moved=[...data.positions];moved[10]=30;
  await assertFails(setDoc(ref('host'),{...data,positions:moved,activePlayer:'TWO',turn:2,revision:4}));
  const pits=[...data.positions];pits[8]=0;
  await assertFails(setDoc(ref('host'),{...data,positions:pits,activePlayer:'TWO',turn:2,revision:4}));
});
test('either player can resign, including during the other turn',async()=>{
  const data=await prepared();
  await assertSucceeds(setDoc(ref('guest'),{...data,phase:'FINISHED',winner:'ONE',reason:'RESIGNED',revision:4}));
  await assertFails(updateDoc(ref('host'),{phase:'PLAYING',winner:'',reason:'',revision:5}));
  await seed(data);
  await assertSucceeds(setDoc(ref('host'),{...data,phase:'FINISHED',winner:'TWO',reason:'RESIGNED',revision:4}));
});
test('expired room is readable to its owner but cannot be changed',async()=>{
  const data={...empty(),expiresAt:Date.now()-1};await seed(data);
  await assertSucceeds(getDoc(ref('host')));
  await assertFails(updateDoc(ref('guest'),{guestUid:'guest',phase:'SETUP',revision:1}));
});
test('two listeners receive the same committed move and reconnection reads it',async()=>{
  const data=await prepared();
  const guestDb=db('guest');
  const seen=new Promise((resolve,reject)=>{
    const timer=setTimeout(()=>{stop();reject(new Error('snapshot timeout'))},10000);
    const stop=onSnapshot(doc(guestDb,'rooms','12345678'),snapshot=>{
      if(snapshot.data()?.revision===4){clearTimeout(timer);stop();resolve(snapshot.data())}
    },reject);
  });
  const hostDb=db('host');
  await runTransaction(hostDb,async transaction=>{
    const document=doc(hostDb,'rooms','12345678');const current=(await transaction.get(document)).data();
    const positions=[...current.positions];positions[0]=0;
    transaction.set(document,{...current,positions,activePlayer:'TWO',turn:2,revision:4});
  });
  const update=await seen;
  assert.equal(update.positions[0],0);
  assert.deepEqual((await getDoc(ref('guest'))).data(),update);
});

test('both sides can capture pits, lose attacks and finish with a draw or home win',async()=>{
  for(const one of [true,false]) for(const ending of ['capture','loss','DRAW','HOME']) {
    const data={...empty(),guestUid:'guest',phase:'PLAYING',readyOne:true,readyTwo:true,
      activePlayer:one?'ONE':'TWO',positions:Array(20).fill(-1)};
    const own=one?6:16, enemy=one?18:8;
    data.positions[own]=18;data.positions[enemy]=19;
    await seed(data);
    const positions=[...data.positions];
    if(ending==='loss')positions[own]=-1;
    else {positions[own]=ending==='HOME'?(one?50:2):ending==='DRAW'?-1:19;positions[enemy]=-1;}
    const finished=['DRAW','HOME'].includes(ending);
    const next={...data,positions,revision:1,phase:finished?'FINISHED':'PLAYING',
      turn:finished?1:2,activePlayer:finished?data.activePlayer:(one?'TWO':'ONE'),
      reason:finished?ending:'',winner:ending==='HOME'?data.activePlayer:''};
    await assertSucceeds(setDoc(ref(one?'host':'guest'),next));
  }
});
