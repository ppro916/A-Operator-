from flask import Flask, render_template, request, jsonify, redirect, url_for, flash
import os
from firebase_config import FirebaseManager
from telegram_bot import TelegramBot
import logging
from datetime import datetime

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)
app.secret_key = os.getenv('SECRET_KEY', 'your-secret-key-here')

# Initialize Firebase and Telegram
firebase_manager = FirebaseManager()
telegram_bot = TelegramBot()

@app.route('/')
def index():
    """Main landing page"""
    return render_template('index.html')

@app.route('/dashboard')
def dashboard():
    """Dashboard showing connected devices and sharing status"""
    try:
        # Get connected devices from Firebase
        devices = firebase_manager.get_connected_devices()
        
        # Get recent activity
        recent_files = firebase_manager.get_recent_files(limit=10)
        recent_locations = firebase_manager.get_recent_locations(limit=5)
        
        return render_template('dashboard.html', 
                             devices=devices,
                             recent_files=recent_files,
                             recent_locations=recent_locations)
    except Exception as e:
        logger.error(f"Dashboard error: {e}")
        flash('Error loading dashboard data', 'error')
        return render_template('dashboard.html', devices=[], recent_files=[], recent_locations=[])

@app.route('/stop_sharing', methods=['GET', 'POST'])
def stop_sharing():
    """Stop sharing page and functionality"""
    if request.method == 'POST':
        device_id = request.form.get('device_id')
        
        if not device_id:
            flash('Device ID is required', 'error')
            return redirect(url_for('stop_sharing'))
        
        try:
            # Update Firebase to stop sharing for the device
            result = firebase_manager.stop_sharing(device_id)
            
            if result:
                # Send Telegram notification
                telegram_bot.send_notification(f"Sharing stopped for device: {device_id}")
                flash(f'Sharing stopped successfully for device {device_id}', 'success')
            else:
                flash('Failed to stop sharing', 'error')
                
        except Exception as e:
            logger.error(f"Stop sharing error: {e}")
            flash('Error stopping sharing', 'error')
        
        return redirect(url_for('dashboard'))
    
    # GET request - show devices that can be stopped
    try:
        active_devices = firebase_manager.get_active_devices()
        return render_template('stop_sharing.html', devices=active_devices)
    except Exception as e:
        logger.error(f"Stop sharing page error: {e}")
        return render_template('stop_sharing.html', devices=[])

@app.route('/start_sharing', methods=['POST'])
def start_sharing():
    """Start sharing for a device"""
    device_id = request.form.get('device_id')
    
    if not device_id:
        return jsonify({'success': False, 'message': 'Device ID required'})
    
    try:
        result = firebase_manager.start_sharing(device_id)
        
        if result:
            telegram_bot.send_notification(f"Sharing started for device: {device_id}")
            return jsonify({'success': True, 'message': 'Sharing started successfully'})
        else:
            return jsonify({'success': False, 'message': 'Failed to start sharing'})
            
    except Exception as e:
        logger.error(f"Start sharing error: {e}")
        return jsonify({'success': False, 'message': 'Error starting sharing'})

@app.route('/api/devices')
def api_devices():
    """API endpoint to get device status"""
    try:
        devices = firebase_manager.get_connected_devices()
        return jsonify({'success': True, 'devices': devices})
    except Exception as e:
        logger.error(f"API devices error: {e}")
        return jsonify({'success': False, 'error': str(e)})

@app.route('/api/files')
def api_files():
    """API endpoint to get recent files"""
    try:
        limit = request.args.get('limit', 20, type=int)
        files = firebase_manager.get_recent_files(limit=limit)
        return jsonify({'success': True, 'files': files})
    except Exception as e:
        logger.error(f"API files error: {e}")
        return jsonify({'success': False, 'error': str(e)})

@app.route('/api/locations')
def api_locations():
    """API endpoint to get recent locations"""
    try:
        limit = request.args.get('limit', 10, type=int)
        locations = firebase_manager.get_recent_locations(limit=limit)
        return jsonify({'success': True, 'locations': locations})
    except Exception as e:
        logger.error(f"API locations error: {e}")
        return jsonify({'success': False, 'error': str(e)})

@app.route('/health')
def health_check():
    """Health check endpoint"""
    return jsonify({
        'status': 'healthy',
        'timestamp': datetime.now().isoformat(),
        'firebase_connected': firebase_manager.is_connected(),
        'telegram_configured': telegram_bot.is_configured()
    })

@app.errorhandler(404)
def not_found(error):
    return render_template('index.html'), 404

@app.errorhandler(500)
def internal_error(error):
    logger.error(f"Internal server error: {error}")
    return jsonify({'error': 'Internal server error'}), 500

if __name__ == '__main__':
    # Check environment setup
    if not firebase_manager.is_configured():
        logger.warning("Firebase not properly configured. Check credentials.")
    
    if not telegram_bot.is_configured():
        logger.warning("Telegram bot not configured. Check TELEGRAM_BOT_TOKEN.")
    
    # Run the Flask application
    app.run(host='0.0.0.0', port=5000, debug=True)
