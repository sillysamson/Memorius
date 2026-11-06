import * as db from './db.js';import {h} from './ui.js';import {t,setLang} from './i18n.js';import {applyTheme} from './theme.js';
import * as cards from './screens/cards.js';import * as projects from './screens/projects.js';import * as schedule from './screens/schedule.js';import * as settings from './screens/settings.js';
const SCREENS={cards,projects,schedule,settings},app=document.getElementById('app');
const ctx={tab:'cards',mode:'view',refresh:()=>draw(),setMode(m){ctx.mode=m;draw()},restyle(){setLang(db.settings.lang);applyTheme(db.settings);draw()}};
let menuEl=null;
function draw(){const s=SCREENS[ctx.tab],main=h('main');app.replaceChildren(h('h1',{},t(s.title)),main,navBar());
 s.render(main,ctx).catch(e=>main.append(h('p',{},'Error: '+e.message)))}
function navBar(){const more=()=>{if(menuEl){menuEl.remove();menuEl=null;return}
  const items=SCREENS[ctx.tab].menu(ctx);if(!items.length)return;
  menuEl=h('div',{cls:'menu'},items.map(([k,fn])=>h('button',{onclick:()=>{menuEl.remove();menuEl=null;fn()}},t(k))));document.body.append(menuEl)};
 const tabs=Object.keys(SCREENS).map(k=>h('button',{cls:k==ctx.tab?'on':'',onclick:()=>{ctx.tab=k;ctx.mode='view';menuEl?.remove();menuEl=null;draw()}},t(k)));
 const dot=h('button',{cls:'dot','aria-label':'Menu',onclick:more},'⋯');
 return ctx.tab=='settings'?h('nav',{},h('div',{cls:'pill'},tabs,dot)):h('nav',{},h('div',{cls:'pill'},tabs),dot)}
async function splash(){const q=await db.randomQuote();if(!q)return;
 await new Promise(ok=>{const s=h('div',{cls:'splash'},h('h1',{},'Memorius'),h('p',{},`“${q.text}”`),h('small',{},'— '+q.author+(q.source?', '+q.source:'')),h('button',{onclick:()=>{s.remove();ok()}},t('cont')));document.body.append(s)})}
(async()=>{try{await db.init();setLang(db.settings.lang);applyTheme(db.settings);await splash();draw()}catch(e){app.textContent='Start error: '+e}})();
