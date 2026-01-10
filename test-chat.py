import requests
import json

url = "http://localhost:8080/api/nl-search"

payload = {
    "query": "Show me all delivered parcels at Location A excluding expired parcels"
}

response = requests.post(url, json=payload)

print("Status code:", response.status_code)
try:
    print("Response JSON:", response.json())
except json.JSONDecodeError:
    print("Response text:", response.text)
