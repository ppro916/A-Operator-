# Placeholder telegram_bot.py — replace with original implementation.
# This file should send notifications via Telegram using the token in credentials.json.

import json
import requests
import os

# credentials.json ची योग्य path set करणे (SharingWebGUI च्या फोल्डरच्या नुसार adjust कर)
credentials_path = os.path.join(os.path.dirname(__file__), '../MySharingApp/credentials.json')

with open(credentials_path, 'r') as f:
    config = json.load(f)

bot_token = config['telegram']['botToken']
chat_id = config['telegram']['chatId']

def send_telegram_message(message):
    url = f"https://api.telegram.org/bot{bot_token}/sendMessage"
    payload = {
        'chat_id': chat_id,
        'text': message
    }
    try:
        response = requests.post(url, data=payload)
        return response.json()
    except Exception as e:
        return {"error": str(e)}

# Example usage:
if __name__ == "__main__":
    res = send_telegram_message("Test message from MySharingApp Telegram bot!")
    print(res)
