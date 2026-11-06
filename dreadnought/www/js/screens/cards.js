import {h,modal,field} from '../ui.js';import {t} from '../i18n.js';import * as db from '../db.js';import * as files from '../files.js';import {schedule,cancel} from '../notify.js';import {palette,MARK} from '../theme.js';
const TYPES=['Homework','Exam','Prepping'];
export const title='cards';
export const menu=ctx=>[['add',()=>form(ctx)],['delete',()=>ctx.setMode('delete')],['rearrange',()=>ctx.setMode('sort')]];
export async function render(root,ctx){const rows=await db.q('SELECT * FROM cards ORDER BY pos DESC,id DESC');
 if(!rows.length)return root.append(h('p',{},t('empty')));
 for(const c of rows){let timer;const fmt=c.type=='Prepping'?c.due.slice(0,10):c.due.replace('T',' ');
  const act=ctx.mode=='delete'?h('button',{cls:'s',onclick:async()=>{await db.run('DELETE FROM cards WHERE id=?',[c.id]);await cancel(c.id);ctx.refresh()}},t('delete'))
   :ctx.mode=='sort'?h('span',{},h('button',{cls:'s',onclick:()=>db.swap('cards',c.id,-1).then(ctx.refresh)},'↑'),' ',h('button',{cls:'s',onclick:()=>db.swap('cards',c.id,1).then(ctx.refresh)},'↓'))
   :h('button',{cls:'s',onclick:()=>form(ctx,c)},t('edit'));
  const el=h('div',{cls:'card'+(c.done?' done':''),style:`background:${c.color}`,
   onpointerdown:()=>timer=setTimeout(()=>preview(c),500),onpointerup:()=>clearTimeout(timer),onpointermove:()=>clearTimeout(timer),onpointercancel:()=>clearTimeout(timer)},
   h('div',{cls:'row'},h('b',{},c.subject),h('span',{},MARK[c.type]+' '+t(c.type))),h('small',{},fmt),
   h('div',{cls:'row'},h('button',{cls:'s',onclick:async()=>{const d=c.done?0:1;await db.run('UPDATE cards SET done=? WHERE id=?',[d,c.id]);d?await cancel(c.id):await schedule({...c,done:0});ctx.refresh()}},t('done')),act));
  root.append(el)}}
function preview(c){const f=JSON.parse(c.files||'[]');
 modal(h('div',{cls:'col'},h('b',{},c.subject),h('small',{},c.type=='Prepping'?c.due.slice(0,10):c.due.replace('T',' ')),h('p',{},c.notes||'—'),h('small',{},t('files')),
  ...f.map(x=>h('button',{cls:'s',onclick:()=>files.open(x.path).catch(e=>alert(e.message))},x.name))),{ok:'ok',cancel:false})}
function form(ctx,c){c=c||{subject:'',type:'Homework',due:'',notes:'',color:palette(db.settings.cb)[0]};const list=JSON.parse(c.files||'[]');let color=c.color;
 const subject=h('input',{value:c.subject}),notes=h('textarea',{rows:3,value:c.notes}),
  type=h('select',{onchange:()=>{due.type=type.value=='Prepping'?'date':'datetime-local'}},TYPES.map(x=>h('option',{value:x},t(x)))),
  due=h('input',{type:c.type=='Prepping'?'date':'datetime-local'}),names=h('small',{},list.map(f=>f.name).join(', ')),
  add=async fs=>{for(const f of fs)list.push(await files.save(f));names.textContent=list.map(f=>f.name).join(', ')},
  pick=h('input',{type:'file',multiple:true,hidden:true,onchange:()=>add(pick.files)}),
  sw=h('div',{cls:'sw'},palette(db.settings.cb).map(x=>h('button',{style:`background:${x}`,cls:x==color?'on':'',onclick:e=>{color=x;sw.querySelectorAll('button').forEach(b=>b.classList.remove('on'));e.target.classList.add('on')}})));
 type.value=c.type;due.value=c.type=='Prepping'?c.due.slice(0,10):c.due;
 const drop=h('div',{cls:'drop',onclick:()=>pick.click(),ondragover:e=>e.preventDefault(),ondrop:e=>{e.preventDefault();add(e.dataTransfer.files)}},t('drop'),h('br'),names);
 modal(h('div',{cls:'col'},field(t('subject'),subject),field(t('type'),type),field(t('deadline'),due),sw,field(t('notes'),notes),drop,pick),{onOk:async()=>{
  if(!subject.value.trim()||!due.value)return false;
  const v=[subject.value.trim(),type.value,due.value,color,notes.value,JSON.stringify(list)];let id=c.id;
  if(id)await db.run('UPDATE cards SET subject=?,type=?,due=?,color=?,notes=?,files=? WHERE id=?',[...v,id]);
  else{await db.run('INSERT INTO cards(subject,type,due,color,notes,files,pos,done) VALUES(?,?,?,?,?,?,?,0)',[...v,await db.nextPos('cards')]);id=(await db.q('SELECT MAX(id) i FROM cards'))[0].i}
  await schedule({id,subject:v[0],type:v[1],due:v[2],done:0});ctx.refresh()}})}
