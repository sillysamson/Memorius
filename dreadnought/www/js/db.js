const P=()=>Capacitor.Plugins.CapacitorSQLite,N='memorius';
export const q=async(s,v=[])=>(await P().query({database:N,statement:s,values:v})).values||[];
export const run=(s,v=[])=>P().run({database:N,statement:s,values:v,transaction:false});
export const settings={};
export async function init(){
 await P().createConnection({database:N,version:1,encrypted:false,mode:'no-encryption',readonly:false});
 await P().open({database:N,readonly:false});
 await P().execute({database:N,transaction:true,statements:`
 CREATE TABLE IF NOT EXISTS cards(id INTEGER PRIMARY KEY AUTOINCREMENT,subject TEXT,type TEXT,due TEXT,color TEXT,notes TEXT,files TEXT,done INT DEFAULT 0,pos INT);
 CREATE TABLE IF NOT EXISTS docs(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,path TEXT,pos INT);
 CREATE TABLE IF NOT EXISTS sched(id INTEGER PRIMARY KEY AUTOINCREMENT,day INT,subject TEXT);
 CREATE TABLE IF NOT EXISTS quotes(id INTEGER PRIMARY KEY AUTOINCREMENT,text TEXT UNIQUE,author TEXT,source TEXT);
 CREATE TABLE IF NOT EXISTS settings(k TEXT PRIMARY KEY,v TEXT);`});
 for(const r of await q('SELECT * FROM settings'))settings[r.k]=r.v;
 Object.assign(settings,{lang:'en',theme:'snow',cb:'0',freq:'Off',time:'18:00',dir:'Memorius',...settings});
 for(const x of await (await fetch('data/quotes.json')).json())await run('INSERT OR IGNORE INTO quotes(text,author,source) VALUES(?,?,?)',[x.text,x.author,x.source??null]);
}
export async function set(k,v){settings[k]=String(v);await run('INSERT OR REPLACE INTO settings VALUES(?,?)',[k,String(v)])}
export async function nextPos(tbl){return(await q(`SELECT COALESCE(MAX(pos),0)+1 p FROM ${tbl}`))[0].p}
export async function swap(tbl,id,dir){const r=await q(`SELECT id FROM ${tbl} ORDER BY pos DESC,id DESC`),i=r.findIndex(x=>x.id==id),j=i+dir;
 if(j<0||j>=r.length)return;[r[i],r[j]]=[r[j],r[i]];for(let k=0;k<r.length;k++)await run(`UPDATE ${tbl} SET pos=? WHERE id=?`,[r.length-k,r[k].id])}
export async function randomQuote(){const all=await q('SELECT * FROM quotes');if(!all.length)return null;
 const pool=all.filter(x=>String(x.id)!==settings.lastQuote),pick=(pool.length?pool:all)[Math.floor(Math.random()*(pool.length||all.length))];
 await set('lastQuote',pick.id);return pick}
