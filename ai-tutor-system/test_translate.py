import urllib.request
import json

data = json.dumps({
    "text": "hello",
    "sourceLanguage": "en",
    "targetLanguage": "zh"
}).encode('utf-8')

req = urllib.request.Request(
    'http://localhost:5000/api/translation/translate', 
    data=data, 
    headers={'Content-Type': 'application/json'}
)

try:
    with urllib.request.urlopen(req) as response:
        print(response.read().decode('utf-8'))
except Exception as e:
    print(e)
    if hasattr(e, 'read'):
        print(e.read().decode('utf-8'))
