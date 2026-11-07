import {settings,set} from './db.js';import {t} from './i18n.js';
const LN=()=>Capacitor.Plugins.LocalNotifications,ids=id=>[0,1,2].map(n=>({id:id*10+n}));
export const cancel=id=>LN().cancel({notifications:ids(id)});
async function allowed(){let p=await LN().checkPermissions();if(p.display!='granted')p=await LN().requestPermissions();return p.display=='granted'}
export async function schedule(c){await cancel(c.id);if(c.done||settings.freq=='Off')return;
 if(!await allowed()){alert(t('denied'));await set('freq','Off');return}
 const[h,m]=settings.time.split(':').map(Number),at=new Date(c.due.length==10?c.due+'T00:00':c.due);at.setHours(h,m,0,0);
 const n={title:c.subject,body:t(c.type)},go=(slot,s)=>LN().schedule({notifications:[{...n,id:c.id*10+slot,schedule:{...s,allowWhileIdle:true}}]});
 if(settings.freq=='DueDay'&&at>new Date())await go(0,{at});
 if(settings.freq=='D1'){at.setDate(at.getDate()-1);if(at>new Date())await go(1,{at})}
 if(settings.freq=='Daily')await go(2,{on:{hour:h,minute:m}})}
