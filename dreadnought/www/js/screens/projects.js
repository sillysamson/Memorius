import {h,modal} from '../ui.js';import {t} from '../i18n.js';import * as db from '../db.js';import * as files from '../files.js';
export const title='projects';
export const menu=ctx=>[['addDoc',()=>add(ctx)],['delete',()=>ctx.setMode('delete')],['rearrange',()=>ctx.setMode('sort')]];
function add(ctx){const i=h('input',{type:'file',accept:'.pdf,.doc,.docx,.txt',onchange:async()=>{const f=i.files[0];if(!f)return;
 if(!/\.(pdf|docx?|txt)$/i.test(f.name))return alert('PDF, DOC, DOCX, TXT');
 const s=await files.save(f);await db.run('INSERT INTO docs(name,path,pos) VALUES(?,?,?)',[s.name,s.path,await db.nextPos('docs')]);ctx.refresh()}});i.click()}
export async function render(root,ctx){const rows=await db.q('SELECT * FROM docs ORDER BY pos DESC,id DESC');
 if(!rows.length)return root.append(h('p',{},t('empty')));
 for(const d of rows){const b=[];
  if(ctx.mode=='delete')b.push(h('button',{cls:'s',onclick:async()=>{await files.remove(d.path);await db.run('DELETE FROM docs WHERE id=?',[d.id]);ctx.refresh()}},t('delete')));
  else if(ctx.mode=='sort')b.push(h('button',{cls:'s',onclick:()=>db.swap('docs',d.id,-1).then(ctx.refresh)},'↑'),h('button',{cls:'s',onclick:()=>db.swap('docs',d.id,1).then(ctx.refresh)},'↓'));
  else{b.push(h('button',{cls:'s',onclick:()=>files.open(d.path).catch(e=>alert(e.message))},t('open')));
   if(/\.txt$/i.test(d.name))b.push(h('button',{cls:'s',onclick:async()=>{const a=h('textarea',{rows:12,value:await files.readText(d.path)});modal(a,{onOk:()=>files.writeText(d.path,a.value)})}},t('edit')))}
  root.append(h('div',{cls:'item'},h('span',{},d.name),h('span',{cls:'row'},b)))}}
