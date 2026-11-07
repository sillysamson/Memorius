const P=Capacitor.Plugins,SQL=P.CapacitorSQLite,DB='memorius',$=s=>document.querySelector(s);
const esc=s=>String(s??'').replace(/[&<>"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c]));
const L={
en:{cards:'Cards',projects:'Projects',schedule:'Schedule',settings:'Settings',add:'Add',del:'Delete',re:'Rearrange',save:'Save',cancel:'Cancel',apply:'Apply',subject:'Subject',notes:'Notes',due:'Deadline',type:'Type',Homework:'Homework',Exam:'Exam',Prepping:'Prepping',cont:'Continue',addDoc:'Add Document',addSch:'Add Schedule',lang:'Language',theme:'Theme',cb:'Colorblind mode',dir:'Storage folder',freq:'Reminders',time:'Reminder time',Off:'Off',DueDay:'Due Day',D1:'D-1',Daily:'Daily Until Completed',confirm:'Apply these changes?',edit:'Edit',done:'Done',empty:'Nothing here yet',drop:'Drop files here or tap',perm:'Notification permission denied',today:'Today',open:'Open',files:'Files',done_:'Complete'},
fr:{cards:'Cartes',projects:'Projets',schedule:'Horaire',settings:'Réglages',add:'Ajouter',del:'Supprimer',re:'Réorganiser',save:'Enregistrer',cancel:'Annuler',apply:'Appliquer',subject:'Matière',notes:'Notes',due:'Échéance',type:'Type',Homework:'Devoirs',Exam:'Examen',Prepping:'Préparation',cont:'Continuer',lang:'Langue',theme:'Thème',Off:'Désactivé',freq:'Rappels',time:'Heure du rappel',confirm:'Appliquer ces changements ?',edit:'Modifier',empty:'Rien ici'},
id:{cards:'Kartu',projects:'Proyek',schedule:'Jadwal',settings:'Pengaturan',add:'Tambah',del:'Hapus',re:'Atur ulang',save:'Simpan',cancel:'Batal',apply:'Terapkan',subject:'Mata pelajaran',notes:'Catatan',due:'Tenggat',type:'Jenis',Homework:'PR',Exam:'Ujian',Prepping:'Persiapan',cont:'Lanjut',lang:'Bahasa',theme:'Tema',Off:'Mati',freq:'Pengingat',time:'Waktu pengingat',confirm:'Terapkan perubahan?',edit:'Ubah',empty:'Belum ada'},
ar:{cards:'البطاقات',projects:'المشاريع',schedule:'الجدول',settings:'الإعدادات',add:'إضافة',del:'حذف',re:'إعادة ترتيب',save:'حفظ',cancel:'إلغاء',apply:'تطبيق',subject:'المادة',notes:'ملاحظات',due:'الموعد',type:'النوع',cont:'متابعة',lang:'اللغة',theme:'المظهر',Off:'إيقاف',freq:'التذكيرات',time:'وقت التذكير',confirm:'تطبيق التغييرات؟',edit:'تعديل',empty:'لا شيء هنا'},
he:{cards:'כרטיסים',projects:'פרויקטים',schedule:'מערכת',settings:'הגדרות',add:'הוסף',del:'מחק',re:'סדר מחדש',save:'שמור',cancel:'ביטול',apply:'החל',subject:'מקצוע',notes:'הערות',due:'מועד',type:'סוג',cont:'המשך',lang:'שפה',theme:'ערכת נושא',Off:'כבוי',freq:'תזכורות',time:'שעת תזכורת',confirm:'להחיל שינויים?',edit:'ערוך',empty:'אין כלום'}};
L['en-GB']=L.en;
const LANGS=[['en','English (US)'],['en-GB','English (UK)'],['id','Indonesia'],['fr','Français'],['ar','العربية'],['he','עברית']];
const THEMES=[['snow','Snow White'],['blue','Pastel Blue'],['pink','Pastel Pink'],['green','Pastel Green'],['yellow','Pastel Yellow'],['beige','Beige'],['amoled','AMOLED Black'],['eigengrau','Eigengrau'],['purple','Dark Purple'],['navy','Navy'],['burgundy','Burgundy'],['maroon','Maroon'],['dbeige','Dark Beige'],['walnut','Walnut Brown']];
const COLORS=['#F4C7C3','#BFD7ED','#C8E6C9','#FFE9A8','#E1C8F0','#FFD3B0'];
const CBCOLORS=['#E69F00','#56B4E9','#009E73','#F0E442','#0072B2','#D55E00'];
const ICON={Homework:'✎',Exam:'!',Prepping:'◷'};
const DAYS=['Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday'];
let S={},tab='cards',rearr=false;
const t=k=>(L[S.lang]||L.en)[k]||L.en[k]||k;
const q=async(sql,v=[])=>(await SQL.query({database:DB,statement:sql,values:v})).values||[];
const run=(sql,v=[])=>SQL.run({database:DB,statement:sql,values:v,transaction:false});
const setS=async(k,v)=>{S[k]=v;await run('INSERT OR REPLACE INTO settings VALUES(?,?)',[k,String(v)])};

async function init(){
 await SQL.createConnection({database:DB,version:1,encrypted:false,mode:'no-encryption',readonly:false});
 await SQL.open({database:DB,readonly:false});
 await SQL.execute({database:DB,transaction:true,statements:`
 CREATE TABLE IF NOT EXISTS cards(id INTEGER PRIMARY KEY AUTOINCREMENT,subject TEXT,type TEXT,due TEXT,color TEXT,notes TEXT,files TEXT DEFAULT '[]',done INT DEFAULT 0,pos INT);
 CREATE TABLE IF NOT EXISTS docs(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,path TEXT,pos INT);
 CREATE TABLE IF NOT EXISTS sched(id INTEGER PRIMARY KEY AUTOINCREMENT,day INT,subject TEXT);
 CREATE TABLE IF NOT EXISTS quotes(id INTEGER PRIMARY KEY AUTOINCREMENT,text TEXT UNIQUE,author TEXT,source TEXT);
 CREATE TABLE IF NOT EXISTS settings(k TEXT PRIMARY KEY,v TEXT);`});
 (await q('SELECT * FROM settings')).forEach(r=>S[r.k]=r.v);
 S.lang=S.lang||'en';S.theme=S.theme||'snow';S.dir=S.dir||'Memorius';S.freq=S.freq||'Off';S.time=S.time||'18:00';
 // seed: new quotes in quotes.json are added on next launch, no code change needed
 for(const x of await (await fetch('quotes.json')).json())
  await run('INSERT OR IGNORE INTO quotes(text,author,source) VALUES(?,?,?)',[x.text,x.author,x.source||null]);
 theme();await splash();nav();show();
}
function theme(){const d=THEMES.findIndex(x=>x[0]==S.theme)>5;document.body.className=(d?'dark ':'')+(S.cb=='1'?'cb':'');
 document.body.dataset.t=S.theme;document.documentElement.dir=['ar','he'].includes(S.lang)?'rtl':'ltr';document.documentElement.lang=S.lang}
async function splash(){
 const all=await q('SELECT * FROM quotes'),pool=all.filter(x=>x.id!=S.lastQ),r=(pool.length?pool:all)[Math.floor(Math.random()*(pool.length||all.length))];
 if(!r){$('#splash').remove();return}
 await setS('lastQ',r.id);$('#q').textContent='“'+r.text+'”';$('#qa').textContent='— '+r.author+(r.source?', '+r.source:'');
 $('#go').textContent=t('cont');await new Promise(ok=>$('#go').onclick=()=>{$('#splash').remove();ok()});
}
function nav(){
 const tabs=['cards','projects','schedule','settings'];
 $('#pill').innerHTML=tabs.map(k=>`<button data-k="${k}" class="${k==tab?'on':''}">${t(k)}</button>`).join('')+(tab=='settings'?'<button class="dot" id="more2">⋯</button>':'');
 $('#more').hidden=tab=='settings';
 $('#pill').querySelectorAll('[data-k]').forEach(b=>b.onclick=()=>{tab=b.dataset.k;rearr=false;$('#menu').hidden=true;nav();show()});
 $('#more').onclick=menu;if($('#more2'))$('#more2').onclick=menu;
}
function menu(){
 const m=$('#menu'),items={cards:[['add',cardForm],['del',()=>{rearr='del';show()}],['re',()=>{rearr=true;show()}]],
 projects:[['addDoc',addDoc],['del',()=>{rearr='del';show()}],['re',()=>{rearr=true;show()}]],schedule:[['addSch',schForm]],settings:[]}[tab];
 if(!m.hidden){m.hidden=true;return}
 if(!items.length){return}
 m.innerHTML=items.map((x,i)=>`<button data-i="${i}">${t(x[0])}</button>`).join('');m.hidden=false;
 m.querySelectorAll('button').forEach(b=>b.onclick=()=>{m.hidden=true;items[b.dataset.i][1]()});
}
function show(){$('#title').textContent=t(tab);({cards,projects,schedule,settings})[tab]()}
function dlg(html,onsubmit){const d=$('#dlg');d.innerHTML=`<form class="col" method="dialog">${html}</form>`;d.showModal();
 const f=d.querySelector('form');f.onsubmit=async e=>{e.preventDefault();await onsubmit?.(new FormData(f),f);d.close()};
 d.querySelector('[data-x]')?.addEventListener('click',()=>d.close());return d}
const btns=()=>`<div class="row"><button type="button" class="sm" data-x>${t('cancel')}</button><button class="btn">${t('save')}</button></div>`;
const move=async(tbl,id,dir)=>{const r=await q(`SELECT id FROM ${tbl} ORDER BY pos DESC,id DESC`);const i=r.findIndex(x=>x.id==id),j=i+dir;
 if(j<0||j>=r.length)return;[r[i],r[j]]=[r[j],r[i]];for(let k=0;k<r.length;k++)await run(`UPDATE ${tbl} SET pos=? WHERE id=?`,[r.length-k,r[k].id]);show()};

// ---------- CARDS
async function cards(){
 const rows=await q('SELECT * FROM cards ORDER BY pos DESC,id DESC'),m=$('#main');
 m.innerHTML=rows.length?rows.map(c=>{const dt=c.type=='Prepping'?c.due.slice(0,10):c.due.replace('T',' ');
  return `<div class="card ${c.done?'done':''}" data-id="${c.id}" style="background:${c.color}"><div class="row"><h3>${esc(c.subject)}</h3><span class="tag" data-i="${ICON[c.type]}">${t(c.type)}</span></div><small>${dt}</small>
  <div class="row"><button class="sm" data-a="chk">${t('done_')}</button>${rearr=='del'?`<button class="sm" data-a="del">${t('del')}</button>`:''}${rearr===true?`<span><button class="sm" data-a="up">↑</button> <button class="sm" data-a="dn">↓</button></span>`:`<button class="sm" data-a="ed">${t('edit')}</button>`}</div></div>`}).join(''):`<p>${t('empty')}</p>`;
 m.querySelectorAll('.card').forEach(el=>{const id=+el.dataset.id;let tm;
  const hold=()=>tm=setTimeout(()=>preview(id),500),stop=()=>clearTimeout(tm);
  el.ontouchstart=hold;el.ontouchend=el.ontouchmove=el.ontouchcancel=stop;
  el.querySelectorAll('[data-a]').forEach(b=>b.onclick=async()=>{const a=b.dataset.a;
   if(a=='chk'){const c=rows.find(x=>x.id==id);await run('UPDATE cards SET done=? WHERE id=?',[c.done?0:1,id]);c.done?await notify({...c,done:0}):await cancelN(id);cards()}
   if(a=='del'){await run('DELETE FROM cards WHERE id=?',[id]);await cancelN(id);cards()}
   if(a=='up')move('cards',id,-1);if(a=='dn')move('cards',id,1);if(a=='ed')cardForm(rows.find(x=>x.id==id))})});
}
async function preview(id){const c=(await q('SELECT * FROM cards WHERE id=?',[id]))[0],f=JSON.parse(c.files);
 const d=dlg(`<h3>${esc(c.subject)}</h3><small>${c.due.replace('T',' ')}</small><p>${esc(c.notes)||'—'}</p><h2>${t('files')}</h2>${f.map((x,i)=>`<button type="button" class="sm" data-f="${i}">${esc(x.name)}</button>`).join('')||'—'}<button class="btn">OK</button>`);
 d.querySelectorAll('[data-f]').forEach(b=>b.onclick=()=>openFile(f[b.dataset.f].path))}
async function saveFile(file){const data=await new Promise(r=>{const x=new FileReader();x.onload=()=>r(x.result.split(',')[1]);x.readAsDataURL(file)});
 const path=`${S.dir}/${Date.now()}_${file.name}`;await P.Filesystem.writeFile({path,data,directory:'DOCUMENTS',recursive:true});return{name:file.name,path}}
async function openFile(path){const u=await P.Filesystem.getUri({path,directory:'DOCUMENTS'});
 try{await P.FileOpener.open({filePath:u.uri,contentType:undefined})}catch(e){alert(e.message||e)}}
function cardForm(c){c=c||{type:'Homework',color:COLORS[0],files:'[]',subject:'',notes:'',due:''};let files=JSON.parse(c.files);const pal=S.cb=='1'?CBCOLORS:COLORS;
 const d=dlg(`<input name="subject" required placeholder="${t('subject')}" value="${esc(c.subject)}">
 <select name="type">${['Homework','Exam','Prepping'].map(x=>`<option value="${x}" ${x==c.type?'selected':''}>${t(x)}</option>`).join('')}</select>
 <input name="due" id="due" required>
 <div class="sw">${pal.map(x=>`<label><input type="radio" name="color" value="${x}" ${x==c.color?'checked':''}><span style="background:${x}"></span></label>`).join('')}</div>
 <textarea name="notes" rows="3" placeholder="${t('notes')}">${esc(c.notes)}</textarea>
 <div class="drop" id="drop">${t('drop')}<div id="fl">${files.map(f=>esc(f.name)).join(', ')}</div></div><input type="file" id="fi" multiple hidden>${btns()}`,
 async fd=>{const o=Object.fromEntries(fd);const data=[c.id,o.subject,o.type,o.due,o.color||c.color,o.notes,JSON.stringify(files)];
  if(c.id){await run('UPDATE cards SET subject=?,type=?,due=?,color=?,notes=?,files=? WHERE id=?',[...data.slice(1),c.id])}
  else{const p=(await q('SELECT COALESCE(MAX(pos),0)+1 p FROM cards'))[0].p;await run('INSERT INTO cards(subject,type,due,color,notes,files,pos) VALUES(?,?,?,?,?,?,?)',[...data.slice(1),p])}
  const id=c.id||(await q('SELECT MAX(id) i FROM cards'))[0].i;await notify({...c,id,subject:o.subject,due:o.due,type:o.type,done:0});cards()});
 const due=d.querySelector('#due'),ty=d.querySelector('[name=type]'),setT=()=>{const v=due.value;due.type=ty.value=='Prepping'?'date':'datetime-local';try{due.value=due.type=='date'?v.slice(0,10):v}catch{}};
 ty.onchange=setT;setT();due.value=c.due?(due.type=='date'?c.due.slice(0,10):c.due):'';
 const add=async fs=>{for(const f of fs)files.push(await saveFile(f));d.querySelector('#fl').textContent=files.map(f=>f.name).join(', ')},dr=d.querySelector('#drop');
 dr.onclick=()=>d.querySelector('#fi').click();d.querySelector('#fi').onchange=e=>add(e.target.files);
 dr.ondragover=e=>e.preventDefault();dr.ondrop=e=>{e.preventDefault();add(e.dataTransfer.files)}}

// ---------- NOTIFICATIONS
const LN=()=>P.LocalNotifications;
async function cancelN(id){await LN().cancel({notifications:[0,1,2].map(n=>({id:id*10+n}))})}
async function notify(c){await cancelN(c.id);if(c.done||S.freq=='Off')return;
 let p=await LN().checkPermissions();if(p.display!='granted')p=await LN().requestPermissions();
 if(p.display!='granted'){alert(t('perm'));await setS('freq','Off');return}
 const [h,mi]=S.time.split(':').map(Number),due=new Date(c.due.length==10?c.due+'T00:00':c.due),at=new Date(due);at.setHours(h,mi,0,0);
 const base={title:c.subject,body:t(c.type)};
 if(S.freq=='DueDay'&&at>new Date())await LN().schedule({notifications:[{...base,id:c.id*10,schedule:{at,allowWhileIdle:true}}]});
 if(S.freq=='D1'){at.setDate(at.getDate()-1);if(at>new Date())await LN().schedule({notifications:[{...base,id:c.id*10+1,schedule:{at,allowWhileIdle:true}}]})}
 if(S.freq=='Daily')await LN().schedule({notifications:[{...base,id:c.id*10+2,schedule:{on:{hour:h,minute:mi},allowWhileIdle:true}}]});
}

// ---------- PROJECTS
async function projects(){
 const rows=await q('SELECT * FROM docs ORDER BY pos DESC,id DESC'),m=$('#main');
 m.innerHTML=rows.length?rows.map(r=>`<div class="item" data-id="${r.id}"><span>${esc(r.name)}</span><span>${rearr=='del'?`<button class="sm" data-a="del">${t('del')}</button>`:rearr===true?`<button class="sm" data-a="up">↑</button> <button class="sm" data-a="dn">↓</button>`:`<button class="sm" data-a="open">${t('open')}</button> ${/\.txt$/i.test(r.name)?`<button class="sm" data-a="ed">${t('edit')}</button>`:''}`}</span></div>`).join(''):`<p>${t('empty')}</p>`;
 m.querySelectorAll('.item').forEach(el=>{const id=+el.dataset.id,r=rows.find(x=>x.id==id);el.querySelectorAll('[data-a]').forEach(b=>b.onclick=async()=>{const a=b.dataset.a;
  if(a=='open')openFile(r.path);if(a=='up')move('docs',id,-1);if(a=='dn')move('docs',id,1);
  if(a=='del'){await P.Filesystem.deleteFile({path:r.path,directory:'DOCUMENTS'}).catch(()=>{});await run('DELETE FROM docs WHERE id=?',[id]);projects()}
  if(a=='ed'){const f=await P.Filesystem.readFile({path:r.path,directory:'DOCUMENTS',encoding:'utf8'});
   dlg(`<textarea name="t" rows="12">${esc(f.data)}</textarea>${btns()}`,async fd=>{await P.Filesystem.writeFile({path:r.path,data:fd.get('t'),directory:'DOCUMENTS',encoding:'utf8'})})}})});
}
function addDoc(){const i=document.createElement('input');i.type='file';i.accept='.pdf,.doc,.docx,.txt';i.onchange=async()=>{const f=i.files[0];if(!f)return;
 if(!/\.(pdf|docx?|txt)$/i.test(f.name))return alert('PDF, DOC, DOCX, TXT');const s=await saveFile(f),p=(await q('SELECT COALESCE(MAX(pos),0)+1 p FROM docs'))[0].p;
 await run('INSERT INTO docs(name,path,pos) VALUES(?,?,?)',[s.name,s.path,p]);projects()};i.click()}

// ---------- SCHEDULE
async function schedule(){
 const today=(new Date().getDay()+6)%7,rows=await q('SELECT * FROM sched ORDER BY id'),m=$('#main');
 m.innerHTML=DAYS.map((d,i)=>`<h2>${d}${i==today?' · '+t('today'):''}</h2>`+(rows.filter(r=>r.day==i).map(r=>`<div class="item" data-id="${r.id}"><span>${esc(r.subject)}</span><span><button class="sm" data-a="ed">${t('edit')}</button> <button class="sm" data-a="del">${t('del')}</button></span></div>`).join('')||`<small>—</small>`)).join('');
 // today first
 const h=[...m.children];const tIdx=h.findIndex(e=>e.tagName=='H2'&&e.textContent.includes(' · '));
 if(tIdx>=0){let end=tIdx+1;while(h[end]&&h[end].tagName!='H2')end++;const blk=h.slice(tIdx,end);blk.reverse().forEach(e=>m.prepend(e))}
 m.querySelectorAll('.item').forEach(el=>{const id=+el.dataset.id,r=rows.find(x=>x.id==id);el.querySelector('[data-a=del]').onclick=async()=>{await run('DELETE FROM sched WHERE id=?',[id]);schedule()};
  el.querySelector('[data-a=ed]').onclick=()=>schForm(r)});
}
function schForm(r){r=r||{day:(new Date().getDay()+6)%7,subject:''};
 dlg(`<select name="day">${DAYS.map((d,i)=>`<option value="${i}" ${i==r.day?'selected':''}>${d}</option>`).join('')}</select><input name="s" required placeholder="${t('subject')}" value="${esc(r.subject)}">${btns()}`,
 async fd=>{r.id?await run('UPDATE sched SET day=?,subject=? WHERE id=?',[+fd.get('day'),fd.get('s'),r.id]):await run('INSERT INTO sched(day,subject) VALUES(?,?)',[+fd.get('day'),fd.get('s')]);schedule()})}

// ---------- SETTINGS (staged; applied only after confirmation)
function settings(){
 const D={...S},m=$('#main'),sel=(k,opts)=>`<select data-k="${k}">${opts.map(o=>`<option value="${o[0]}" ${D[k]==o[0]?'selected':''}>${o[1]}</option>`).join('')}</select>`;
 m.innerHTML=`<h2>${t('lang')}</h2>${sel('lang',LANGS)}<h2>${t('theme')}</h2>${sel('theme',THEMES)}
 <h2>${t('cb')}</h2>${sel('cb',[['0','Off'],['1','On']])}<h2>${t('freq')}</h2>${sel('freq',[['Off',t('Off')],['DueDay',t('DueDay')],['D1','D-1'],['Daily',t('Daily')]])}
 <h2>${t('time')}</h2><input type="time" data-k="time" value="${D.time}"><h2>${t('dir')}</h2><input data-k="dir" value="${esc(D.dir)}">
 <button class="btn" id="ap">${t('apply')}</button>`;
 m.querySelectorAll('[data-k]').forEach(e=>e.onchange=()=>D[e.dataset.k]=e.value);
 $('#ap').onclick=()=>dlg(`<p>${t('confirm')}</p><div class="row"><button type="button" class="sm" data-x>${t('cancel')}</button><button class="btn">${t('apply')}</button></div>`,async()=>{
  const freqChanged=D.freq!=S.freq||D.time!=S.time;D.dir=D.dir.replace(/[\\.]+/g,'').trim()||'Memorius';
  for(const k of ['lang','theme','cb','freq','time','dir'])if(D[k]!==undefined)await setS(k,D[k]);
  theme();nav();show();
  if(freqChanged)for(const c of await q('SELECT * FROM cards WHERE done=0'))await notify(c)})}
init().catch(e=>{document.body.innerHTML='<pre style="padding:24px">'+e+'</pre>'});
