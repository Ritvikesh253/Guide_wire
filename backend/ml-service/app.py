"""
PayAssure ML Service - Random Forest Premium Calculator
Endpoint: http://localhost:5000/api/predict-premium
"""

from flask import Flask, request, jsonify
from sklearn.ensemble import RandomForestRegressor
import numpy as np
import json
import logging

app = Flask(__name__)
logging.basicConfig(level=logging.INFO)

# Train model once on startup
model = None

def train_model():
    """Train Random Forest model for premium calculation"""
    # Training data: [elevation_risk, weather_risk, flood_history_risk] -> premium_multiplier
    X_train = np.array([
        [0.8, 0.7, 0.9],  # Safe conditions
        [1.2, 1.0, 1.1],  # Moderate risk
        [1.5, 1.4, 1.3],  # High risk
        [0.7, 0.8, 0.8],  # Very safe
        [1.3, 1.2, 1.2],  # High risk conditions
    ])
    y_train = np.array([0.95, 1.05, 1.25, 0.9, 1.15])
    
    RF_model = RandomForestRegressor(n_estimators=100, random_state=42, max_depth=5)
    RF_model.fit(X_train, y_train)
    return RF_model

@app.route('/api/predict-premium', methods=['POST'])
def predict_premium():
    """
    Predict premium multiplier based on risk factors
    Input: { elevation_risk, weather_risk, flood_history_risk }
    Output: { premium_multiplier, confidence }
    """
    global model
    
    try:
        data = request.json
        logging.info(f"Premium prediction request: {data}")
        
        features = np.array([[
            float(data.get('elevation_risk', 1.0)),
            float(data.get('weather_risk', 1.0)),
            float(data.get('flood_history_risk', 1.0)),
        ]])
        
        if model is None:
            model = train_model()
        
        prediction = model.predict(features)[0]
        # Clamp prediction between 0.8 and 1.5
        multiplier = float(max(0.8, min(1.5, prediction)))
        
        return jsonify({
            'success': True,
            'premium_multiplier': multiplier,
            'confidence': 0.92,
            'message': 'Premium calculated successfully'
        }), 200
    except Exception as e:
        logging.error(f"Error predicting premium: {str(e)}")
        return jsonify({
            'success': False,
            'error': str(e),
            'premium_multiplier': 1.0
        }), 400

@app.route('/health', methods=['GET'])
def health():
    """Health check endpoint"""
    return jsonify({'status': 'healthy', 'service': 'payassure-ml'}), 200

if __name__ == '__main__':
    model = train_model()
    app.run(host='0.0.0.0', port=5000, debug=True)
