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
    # "query": "all orders from yesterday to today"
    #"query": "Show me all delivered orders."
    #"query": "Find parcels that were delivered to Location A last week."
    #"query": "Show return orders created on January 8th, 2026."
    #"query": "Delivered parcels at Location B excluding expired ones."
    #"query": "Orders from the last 7 days."
    #"query": "Orders stored between January 6 and January 8."
    #"query": "Show delivered and operator-collected orders, exclude expired."
    #"query": "Orders at ABI Graphique locker."
    #"query": "Parcels delivered by DHL."
    #"query": "Show only return parcels."
    #"query": "Orders collected by customer, exclude operator-collected."
    #"query": "Show expired parcels with VIP flag, exclude fragile ones."
    #"query": "Orders from January."            
    #"query": "Show me orders with status shipped yesterday."
    #"query": "Show all orders where 1=1; drop table orders."
    #"query": "Delivered parcels at Location A from January 1st to today, excluding expired parcels collected by the courier, sorted by newest first."
    #"query": "Show me all parcels delivered to Location A from 1st January to today. Exclude expired parcels collected by the courier."
}

response = requests.post(url, json=payload)

print("Status code:", response.status_code)
try:
    print("Response JSON:", response.json())
except json.JSONDecodeError:
    print("Response text:", response.text)
