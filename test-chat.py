import requests
import json

url = "http://localhost:8080/api/nl-search"

payload = {
    # "query": "Show me all delivered parcels at Location A excluding expired parcels"
    # "query": "Show me all expired orders"
    # "query": "Show me all orders excluding expired"
    # "query": "Delivered orders at Location A"
    # "query": "Show me all orders"
    # "query": "Customer stored orders excluding expired"
    "query": "all orders from yesterday to today"
}

response = requests.post(url, json=payload)

print("Status code:", response.status_code)
try:
    print("Response JSON:", response.json())
except json.JSONDecodeError:
    print("Response text:", response.text)
