<!-- Add Leaflet.js to handle GeoJSON zones and spatial selection -->
<!-- Update your frontend package.json to include Leaflet -->

To add Leaflet.js to your React project, run:

```bash
npm install leaflet react-leaflet
```

Then import in your component:
```javascript
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
```

Features:
1. Display Chennai map centered at [13.0827, 80.2707]
2. Render zones as GeoJSON polygons (fetched from backend)
3. Click on map to select working area
4. Display risk index in real-time
5. Show estimated premium based on zone + ML calculation
