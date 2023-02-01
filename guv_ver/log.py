import pandas as pd
version='v3.5.2'

df=pd.read_csv(f'{version}.csv')
temp_list=[]
for row in df.iterrows():
	i=row[1]
	if(i.loc['id']==3):
		temp_list.append(f"{i.loc['func_methodsig']} calls {i.loc['doc_methodsig']}")
	elif(i.loc['id']==5):
		temp_list.append(f"{i.loc['doc_methodsig']} calls {i.loc['func_methodsig']}")

df['Calls'] = temp_list

df.to_csv(f'{version}.csv')
