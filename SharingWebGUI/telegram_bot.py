# Placeholder telegram_bot.py — replace with original implementation.
# This file should send notifications via Telegram using the token in credentials.json.
from flask import Flask, render_template, request

app = Flask(__name__)

@app.route('/')
def index():
    return render_template('index.html')

@app.route('/stop', methods=['GET', 'POST'])
def stop():
    if request.method == 'POST':
        # Stop sharing logic here (e.g., update Firebase or app state)
        return "Sharing stopped successfully!"  # किंवा redirect करा
    return render_template('stop_sharing.html')

if __name__ == '__main__':
    app.run(port=5000, debug=True)
