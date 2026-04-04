"""
Mock Disruption API - Simulates real-time disruption events for Parametric Automation
Endpoint: http://localhost:8090/api/disruptionEvents
"""

from flask import Flask, jsonify
from datetime import datetime
import random
import logging

app = Flask(__name__)
logging.basicConfig(level=logging.INFO)

# Simulated zones
ZONES = ['Velachery', 'Adyar', 'T-Nagar', 'Mylapore', 'Guindy', 'Besant Nagar', 'Perambur', 'Madipakkam']

@app.route('/api/disruptionEvents', methods=['GET'])
def get_disruption_event():
    """
    Returns simulated disruption event data
    Response: { zone, rain_mm, status, strike, timestamp }
    """
    try:
        # Simulate various disruption scenarios
        scenarios = [
            {'zone': 'Velachery', 'rain_mm': 95, 'status': 'RED_ALERT', 'strike': 'TRUE'},
            {'zone': 'Adyar', 'rain_mm': 75, 'status': 'ORANGE_ALERT', 'strike': 'FALSE'},
            {'zone': 'T-Nagar', 'rain_mm': 45, 'status': 'GREEN', 'strike': 'FALSE'},
            {'zone': 'Mylapore', 'rain_mm': 120, 'status': 'RED_ALERT', 'strike': 'FALSE'},
            {'zone': 'Guindy', 'rain_mm': 0, 'status': 'GREEN', 'strike': 'FALSE'},
        ]
        
        # Pick random scenario
        event = random.choice(scenarios)
        
        logging.info(f"Disruption event: {event}")
        
        return jsonify({
            'zone': event['zone'],
            'rain_mm': event['rain_mm'],
            'status': event['status'],
            'strike': event['strike'],
            'timestamp': datetime.now().isoformat(),
            'magnitude': event['rain_mm'] / 200.0  # Normalize to 0-1
        }), 200
    except Exception as e:
        logging.error(f"Error generating disruption event: {str(e)}")
        return jsonify({
            'error': str(e),
            'zone': None,
            'rain_mm': 0
        }), 500

@app.route('/health', methods=['GET'])
def health():
    """Health check endpoint"""
    return jsonify({'status': 'healthy', 'service': 'mock-disruption-api'}), 200

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8090, debug=True)
