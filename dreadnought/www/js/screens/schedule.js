import {h,modal,field} from '../ui.js';import {t} from '../i18n.js';import * as db from '../db.js';
const DAYS=['Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday'];
export const title='schedule';
const todayIdx=()=>(new Date().getDay()+6)%7;
export const menu=ctx=>[['addSch',()=>form(ctx)]];
export async function render(root,ctx){const rows=await db.q('SELECT * FROM sched ORDER BY id'),td=todayIdx();
 for(let n=0;n<7;n++){const d=(td+n)%7;root.append(h('h2',{},DAYS[d]+(d==td?' · '+t('today'):'')));
  const r=rows.filter(x=>x.day==d);if(!r.length)root.append(h('small',{},'—'));
  for(const x of r)root.append(h('div',{cls:'item'+(d==td?' today':'')},h('span',{},x.subject),h('span',{cls:'row'},
   h('button',{cls:'s',onclick:()=>form(ctx,x)},t('edit')),h('button',{cls:'s',onclick:async()=>{await db.run('DELETE FROM sched WHERE id=?',[x.id]);ctx.refresh()}},t('delete')))))}}
function form(ctx,r){r=r||{day:todayIdx(),subject:''};
 const day=h('select',{},DAYS.map((d,i)=>h('option',{value:i},d))),sub=h('input',{value:r.subject});day.value=r.day;
 modal(h('div',{cls:'col'},field(t('day'),day),field(t('subject'),sub)),{onOk:async()=>{if(!sub.value.trim())return false;
  r.id?await db.run('UPDATE sched SET day=?,subject=? WHERE id=?',[+day.value,sub.value.trim(),r.id]):await db.run('INSERT INTO sched(day,subject) VALUES(?,?)',[+day.value,sub.value.trim()]);ctx.refresh()}})}
