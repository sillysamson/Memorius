import {h,modal,field} from '../ui.js';import {t,LANGS} from '../i18n.js';import * as db from '../db.js';import {THEMES} from '../theme.js';import {schedule} from '../notify.js';
export const title='settings';
export const menu=()=>[];
export async function render(root,ctx){const d={...db.settings};
 const sel=(k,o)=>h('select',{value:d[k],onchange:e=>d[k]=e.target.value},o.map(([v,l])=>h('option',{value:v},l)));
 root.append(field(t('language'),sel('lang',LANGS)),field(t('theme'),sel('theme',THEMES)),field(t('cb'),sel('cb',[['0',t('Off')],['1',t('on')]])),
  field(t('reminders'),sel('freq',[['Off',t('Off')],['DueDay',t('DueDay')],['D1',t('D1')],['Daily',t('Daily')]])),
  field(t('time'),h('input',{type:'time',value:d.time,onchange:e=>d.time=e.target.value})),
  field(t('folder'),h('input',{value:d.dir,onchange:e=>d.dir=e.target.value})),
  h('button',{onclick:()=>modal(h('p',{},t('confirm')),{ok:'apply',onOk:async()=>{
   d.dir=d.dir.replace(/[\\\/.]+/g,'').trim()||'Memorius';const nf=d.freq!=db.settings.freq||d.time!=db.settings.time;
   for(const k of['lang','theme','cb','freq','time','dir'])await db.set(k,d[k]);
   if(nf)for(const c of await db.q('SELECT * FROM cards WHERE done=0'))await schedule(c);ctx.restyle()}})},t('apply')))}
