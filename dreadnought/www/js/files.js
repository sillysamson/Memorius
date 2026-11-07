import {settings} from './db.js';
const FS=()=>Capacitor.Plugins.Filesystem,D='DOCUMENTS';
export async function save(file){const data=await new Promise(r=>{const f=new FileReader();f.onload=()=>r(f.result.split(',')[1]);f.readAsDataURL(file)});
 const path=`${settings.dir}/${Date.now()}_${file.name}`;await FS().writeFile({path,data,directory:D,recursive:true});return{name:file.name,path}}
export async function open(path){const{uri}=await FS().getUri({path,directory:D});await Capacitor.Plugins.FileOpener.open({filePath:uri})}
export const remove=path=>FS().deleteFile({path,directory:D}).catch(()=>{});
export const readText=async path=>(await FS().readFile({path,directory:D,encoding:'utf8'})).data;
export const writeText=(path,data)=>FS().writeFile({path,data,directory:D,encoding:'utf8'});
