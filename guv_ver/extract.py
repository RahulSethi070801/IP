import os
import json
import pandas as pd
from glob import glob
from tqdm import tqdm

path_to_json = './'
version='v19.0'
json_files = [pos_json for pos_json in glob(
    f'./{version}/**/FinalJSONDataMatches.json')]

# dataframe
jsons_data = pd.DataFrame(columns=['func_oldfunccode', 'func_newfunccode', 'func_newdoc', 'func_newJdoc', 'func_olddoc',
                                   'func_oldJdoc', 'func_addlines', 'func_addcode', 'func_dellines', 'func_delcode', 'func_path', 'func_pathinproj','func_methodsig',
                                   'doc_oldfunccode', 'doc_newfunccode', 'doc_newdoc', 'doc_newJdoc', 'doc_olddoc',
                                   'doc_oldJdoc', 'doc_addlines', 'doc_addcode', 'doc_dellines', 'doc_delcode', 'doc_path', 'doc_pathinproj','doc_methodsig', 'id', 'flag', 'commit'])
count = 0
for index, js in enumerate(tqdm(json_files)):
    # print(js)
    # print(os.path.join(path_to_json, js))
    with open(os.path.join(path_to_json, js)) as json_file:
        print(f'{js}')
        json_text = json.load(json_file)
        # print(json_file)
        # print(len(json_text))
        # break

        for i in range(len(json_text)):
            func_oldfunccode = json_text[i]['func']['oldfunccode']
            func_newfunccode = json_text[i]['func']['newfunccode']
            func_newdoc = json_text[i]['func']['newdoc']
            func_newJdoc = json_text[i]['func']['newJdoc']
            func_olddoc = json_text[i]['func']['olddoc']
            func_oldJdoc = json_text[i]['func']['oldJdoc']
            func_addlines = json_text[i]['func']['addlines']
            func_addcode = json_text[i]['func']['addcode']
            func_dellines = json_text[i]['func']['dellines']
            func_delcode = json_text[i]['func']['delcode']
            func_path = json_text[i]['func']['path']
            func_pathinproj = json_text[i]['func']['pathinproj']
            func_methodsig = json_text[i]['func']['methodSig']
            
            doc_oldfunccode = json_text[i]['doc']['oldfunccode']
            doc_newfunccode = json_text[i]['doc']['newfunccode']
            doc_newdoc = json_text[i]['doc']['newdoc']
            doc_newJdoc = json_text[i]['doc']['newJdoc']
            doc_olddoc = json_text[i]['doc']['olddoc']
            doc_oldJdoc = json_text[i]['doc']['oldJdoc']
            doc_addlines = json_text[i]['doc']['addlines']
            doc_addcode = json_text[i]['doc']['addcode']
            doc_dellines = json_text[i]['doc']['dellines']
            doc_delcode = json_text[i]['func']['delcode']
            doc_path = json_text[i]['doc']['path']
            doc_pathinproj = json_text[i]['doc']['pathinproj']
            doc_methodsig = json_text[i]['doc']['methodSig']

            id = json_text[i]['id']
            flag = json_text[i]['flag']
            commit = json_text[i]['commit']

            count += 1

            jsons_data.loc[count] = [func_oldfunccode, func_newfunccode, func_newdoc, func_newJdoc, func_olddoc,
                                     func_oldJdoc, func_addlines, func_addcode, func_dellines, func_delcode, func_path, func_pathinproj,func_methodsig,
                                     doc_oldfunccode, doc_newfunccode, doc_newdoc, doc_newJdoc, doc_olddoc,
                                     doc_oldJdoc, doc_addlines, doc_addcode, doc_dellines, doc_delcode, doc_path, doc_pathinproj,doc_methodsig, id, flag, commit]


jsons_data.to_csv(f'{version}.csv')
