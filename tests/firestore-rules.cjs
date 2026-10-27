const {initializeTestEnvironment, assertFails, assertSucceeds} = require('@firebase/rules-unit-testing');
const {doc, setDoc, getDoc, getDocs, collection, updateDoc, deleteDoc} = require('firebase/firestore');
const fs = require('fs');
const path = require('path');
const root=path.resolve(__dirname,'..');
const projectId=JSON.parse(fs.readFileSync(path.join(root,'app/google-services.json'))).project_info.project_id;
(async()=>{
 const env=await initializeTestEnvironment({projectId,firestore:{host:'127.0.0.1',port:8180,rules:fs.readFileSync(path.join(root,'firestore.rules'),'utf8')}});
 let checks=0;
 async function ok(name,task,pass=true){ await (pass?assertSucceeds(task):assertFails(task)); console.log('PASS '+name); checks++; }
 try {
  await env.clearFirestore();
  const a=env.authenticatedContext('alice',{email:'alice@example.test'}).firestore();
  const b=env.authenticatedContext('bob',{email:'bob@example.test'}).firestore();
  const anon=env.unauthenticatedContext().firestore();
  const profile={name:'Alice',email:'alice@example.test',currency:'USD',university:'',career:''};
  const m={id:'m1',description:'Almuerzo',amount:5,type:'expense',category:'Alimentación',date:'2026-10-26',paymentMethod:'Efectivo',tags:['comida']};
  const budget={id:'b1',category:'Alimentación',limit:200,period:'2026-10'};
  await ok('anonymous cannot read profile',getDoc(doc(anon,'usuarios/alice')),false);
  await ok('owner can create profile',setDoc(doc(a,'usuarios/alice'),profile));
  await ok('owner can read profile',getDoc(doc(a,'usuarios/alice')));
  await ok('other account cannot read profile',getDoc(doc(b,'usuarios/alice')),false);
  await ok('other account cannot overwrite profile',setDoc(doc(b,'usuarios/alice'),profile),false);
  await ok('cannot store password in profile',updateDoc(doc(a,'usuarios/alice'),{password:'secret'}),false);
  await ok('cannot impersonate email',updateDoc(doc(a,'usuarios/alice'),{email:'bob@example.test'}),false);
  await ok('owner can create movement',setDoc(doc(a,'usuarios/alice/movimientos/m1'),m));
  await ok('owner can list movements',getDocs(collection(a,'usuarios/alice/movimientos')));
  await ok('other account cannot list movements',getDocs(collection(b,'usuarios/alice/movimientos')),false);
  await ok('other account cannot change movement',updateDoc(doc(b,'usuarios/alice/movimientos/m1'),{amount:1}),false);
  await ok('negative amount rejected',updateDoc(doc(a,'usuarios/alice/movimientos/m1'),{amount:-1}),false);
  await ok('wrong document id rejected',updateDoc(doc(a,'usuarios/alice/movimientos/m1'),{id:'other'}),false);
  await ok('invalid movement type rejected',updateDoc(doc(a,'usuarios/alice/movimientos/m1'),{type:'admin'}),false);
  await ok('extra movement fields rejected',updateDoc(doc(a,'usuarios/alice/movimientos/m1'),{uid:'bob'}),false);
  await ok('invalid date format rejected',updateDoc(doc(a,'usuarios/alice/movimientos/m1'),{date:'26/10/2026'}),false);
  await ok('too many tags rejected',updateDoc(doc(a,'usuarios/alice/movimientos/m1'),{tags:Array(21).fill('a')}),false);
  await ok('owner can create budget',setDoc(doc(a,'usuarios/alice/presupuestos/b1'),budget));
  await ok('spent is derived and cannot be forged',updateDoc(doc(a,'usuarios/alice/presupuestos/b1'),{spent:0}),false);
  await ok('zero budget rejected',updateDoc(doc(a,'usuarios/alice/presupuestos/b1'),{limit:0}),false);
  await ok('wrong period rejected',updateDoc(doc(a,'usuarios/alice/presupuestos/b1'),{period:'Octubre 2026'}),false);
  await ok('other account cannot delete budget',deleteDoc(doc(b,'usuarios/alice/presupuestos/b1')),false);
  await ok('owner can edit movement',updateDoc(doc(a,'usuarios/alice/movimientos/m1'),{amount:10}));
  await ok('owner can delete movement',deleteDoc(doc(a,'usuarios/alice/movimientos/m1')));
  await ok('owner can delete budget',deleteDoc(doc(a,'usuarios/alice/presupuestos/b1')));
  console.log(`RESULT ${checks} checks passed`);
 } finally {await env.clearFirestore(); await env.cleanup();}
})().catch(e=>{console.error(e);process.exitCode=1;});
