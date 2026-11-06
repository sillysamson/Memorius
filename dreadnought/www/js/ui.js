import {t} from './i18n.js';
export function h(tag,attrs={},...kids){const e=document.createElement(tag),props={};
 for(const[k,v]of Object.entries(attrs)){if(k.startsWith('on'))e.addEventListener(k.slice(2),v);else if(k=='value'||k=='checked')props[k]=v;else if(v!=null&&v!==false)e.setAttribute(k=='cls'?'class':k,v===true?'':v)}
 for(const c of kids.flat())e.append(c?.nodeType?c:document.createTextNode(c??''));Object.assign(e,props);return e}
export function modal(body,{ok='save',onOk,cancel=true}={}){const d=h('dialog');
 const close=()=>{d.close();d.remove()};
 const go=h('button',{onclick:async()=>{if(await onOk?.()!==false)close()}},t(ok));
 d.append(h('div',{cls:'col'},body,h('div',{cls:'row'},cancel?h('button',{cls:'s',onclick:close},t('cancel')):'',go)));
 document.body.append(d);d.showModal();return{close,d}}
export const field=(label,input)=>h('label',{cls:'col',style:'gap:4px'},h('small',{},label),input);
