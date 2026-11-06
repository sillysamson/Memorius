export const THEMES=[['snow','Snow White'],['blue','Pastel Blue'],['pink','Pastel Pink'],['green','Pastel Green'],['yellow','Pastel Yellow'],['beige','Beige'],['amoled','AMOLED Black'],['eigengrau','Eigengrau'],['purple','Dark Purple'],['navy','Navy'],['burgundy','Burgundy'],['maroon','Maroon'],['dbeige','Dark Beige'],['walnut','Walnut Brown']];
const PAL=['#F4C7C3','#BFD7ED','#C8E6C9','#FFE9A8','#E1C8F0','#FFD3B0'],CB=['#E69F00','#56B4E9','#009E73','#F0E442','#0072B2','#D55E00'];
export const palette=cb=>cb=='1'?CB:PAL;
export const MARK={Homework:'✎',Exam:'!',Prepping:'◷'};
export function applyTheme(s){const i=THEMES.findIndex(x=>x[0]==s.theme);document.body.dataset.th=s.theme;document.body.classList.toggle('dark',i>5)}
