# Feature 5: Aadhaar ID Matching & Real Selfie Validation

## Overview

This feature ensures high KYC accuracy by validating that:
1. The Aadhaar photo from UIDAI matches the worker's selfie (liveness check)
2. The selfie is real (not spoofed) using face recognition confidence scoring
3. The Aadhaar number matches the ID provided in Step 2

---

## Validation Flow

### Step 1: Fetch Aadhaar Photo (via Setu API)

When worker enters their Aadhaar number and OTP in Step 1:

```java
// AadhaarService.java
public AadhaarKycData verifyAadhaarOtp(String aadhaarNumber, String otp) {
    // 1. Call Setu API: /kyc/aadhaar/otp/verify
    // Response includes:
    // - legalName
    // - photoBase64 (Aadhaar photo from UIDAI)
    // - dateOfBirth
    // - gender
    // - address
    
    // 2. Store Aadhaar photo in database for comparison
    worker.setAadhaarPhotoBase64(response.getPhotoBase64());
    
    return aadhaarKycData;
}
```

### Step 2: Capture Real Selfie

Frontend captures selfie with liveness detection:

```javascript
// Step1.js - Selfie Capture
const captureSelfie = async () => {
  // Open device camera
  const constraints = { video: { facingMode: 'user' } };
  const stream = await navigator.mediaDevices.getUserMedia(constraints);
  
  // Record 2-second video
  const recorder = new MediaRecorder(stream);
  recorder.start();
  
  setTimeout(() => {
    recorder.stop();
    
    // Extract frame from video
    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d');
    ctx.drawImage(video, 0, 0);
    
    // Convert to base64
    self

ieLivenessSelfie = canvas.toDataURL('image/jpeg');
    
    // Send to backend for validation
    await validateSelfieWithBackend(selfieImage);
  }, 2000);
};
```

### Step 3: Face Matching & Liveness Detection

Backend performs photo comparison:

```java
// RegistrationService.java - Enhanced Step 1
public RegistrationResponse registerStep1(Step1Request req) {
    // 1. Fetch Aadhaar data via OTP
    AadhaarKycData aadhaarData = aadhaarService.verifyAadhaarOtp(
        req.getAadhaarNumber(), 
        req.getAadhaarOtp()
    );
    
    // 2. Validate Aadhaar photo vs Selfie
    LivenessResult livenessResult = validateFaceMatch(
        aadhaarData.getPhotoBase64(),      // Aadhaar photo
        req.getLivenessSelfieBase64()      // Selfie from Step 1
    );
    
    // 3. Check confidence score
    if (livenessResult.getConfidence() < 0.85) {
        throw new Exception("Face match confidence too low. Please retake selfie.");
    }
    
    // 4. Store validation results
    worker.setAadhaarPhotoBase64(aadhaarData.getPhotoBase64());
    worker.setSelfieLivenessSelfieBase64(req.getLivenessSelfieBase64());
    worker.setSelfieVerified(true);
    worker.setSelfieLivenessConfidence(
        BigDecimal.valueOf(livenessResult.getConfidence())
    );
    worker.setAadhaarMatchConfidence(
        BigDecimal.valueOf(livenessResult.getConfidence())
    );
    
    return registrationResponse;
}

// Face matching algorithm - using external API or ML model
private LivenessResult validateFaceMatch(String aadhaarPhotoBase64, String selfieBase64) {
    try {
        // Option 1: Google Cloud Vision API
        // Option 2: AWS Rekognition
        // Option 3: Face++ (deepface)
        // Option 4: TensorFlow Face Detection Model
        
        // Simulated implementation:
        double confidence = performFaceComparison(aadhaarPhotoBase64, selfieBase64);
        
        return LivenessResult.builder()
            .match(confidence > 0.85)
            .confidence(confidence)
            .message(confidence > 0.85 ? "Face matched successfully" : "Face mismatch detected")
            .build();
    } catch (Exception e) {
        return LivenessResult.builder()
            .match(false)
            .confidence(0.0)
            .message("Face validation failed: " + e.getMessage())
            .build();
    }
}

private double performFaceComparison(String photo1, String photo2) {
    // Using OpenCV or similar library:
    // 1. Decode base64 images
    // 2. Detect faces using MTCNN or similar
    // 3. Extract face embeddings using FaceNet/ArcFace
    // 4. Compare embeddings using cosine similarity
    // 5. Return confidence (0.0 to 1.0)
    
    // Simulated score (in production, use actual ML model)
    return 0.92;  // 92% confidence
}
```

### Step 2 Validation: Aadhaar Number Matching

In Step 2 (ID Card verification), validate that extracted Aadhaar matches:

```java
// OcrService.java - Enhanced
public OcrResult extractAndValidateIdCard(String idCardImageBase64) {
    // 1. Extract text from ID card using Tesseract/Google Cloud Vision
    String extractedAadhaar = extractAadhaarNumberFromImage(idCardImageBase64);
    String extractedName = extractNameFromImage(idCardImageBase64);
    
    // 2. Compare with registered Aadhaar
    Worker worker = getWorkerFromSession();
    
    boolean aadhaarMatches = extractedAadhaar.equals(worker.getAadhaarNumber());
    boolean nameMatches = similarityScore(extractedName, worker.getLegalName()) > 0.85;
    
    return OcrResult.builder()
        .extractedPartnerId(extractedPartnerId)
        .extractedName(extractedName)
        .extractedPlatform(extractedPlatform)
        .nameMatchesAadhaar(nameMatches && aadhaarMatches)
        .message(
            aadhaarMatches && nameMatches 
                ? "ID verified successfully" 
                : "Aadhaar/Name mismatch with registered data"
        )
        .build();
}
```

---

## Implementation Options for Face Matching

### Option 1: Google Cloud Vision API (Recommended)

```java
import com.google.cloud.vision.v1.*;

public double compareFacesGoogleVision(String photo1Base64, String photo2Base64) {
    try (ImageAnnotatorClient vision = ImageAnnotatorClient.create()) {
        Image img1 = Image.newBuilder()
            .setContent(ByteString.copyFrom(Base64.getDecoder().decode(photo1Base64)))
            .build();
        
        Image img2 = Image.newBuilder()
            .setContent(ByteString.copyFrom(Base64.getDecoder().decode(photo2Base64)))
            .build();
        
        // Use Face Detection to extract face data
        FaceDetectionParams params = FaceDetectionParams.newBuilder()
            .setMaxResults(1)
            .build();
        
        // This would require additional matching logic
        // Return confidence score between 0.0 and 1.0
    }
}
```

### Option 2: AWS Rekognition

```java
import software.amazon.awssdk.services.rekognition.RekognitionClient;

public double compareFacesAWSRekognition(String photo1Base64, String photo2Base64) {
    RekognitionClient rekognitionClient = RekognitionClient.builder().build();
    
    CompareFacesRequest request = CompareFacesRequest.builder()
        .sourceImage(Image.builder()
            .bytes(SdkBytes.fromByteArray(Base64.getDecoder().decode(photo1Base64)))
            .build())
        .targetImage(Image.builder()
            .bytes(SdkBytes.fromByteArray(Base64.getDecoder().decode(photo2Base64)))
            .build())
        .similarityThreshold(85f)
        .build();
    
    CompareFacesResponse response = rekognitionClient.compareFaces(request);
    
    double confidence = response.faceMatches().isEmpty() ? 0.0 : 
        response.faceMatches().get(0).similarity() / 100.0;
    
    rekognitionClient.close();
    return confidence;
}
```

### Option 3: OpenCV + Python ML Service

```python
# backend/ml-service/face_matching.py

import cv2
import numpy as np
import base64
from deepface import DeepFace

def compare_faces(face1_base64, face2_base64):
    """
    Compare two face images using DeepFace library
    Returns confidence score (0.0 to 1.0)
    """
    try:
        # Decode base64 to images
        face1_bytes = base64.b64decode(face1_base64)
        face2_bytes = base64.b64decode(face2_base64)
        
        # Convert to numpy arrays
        nparr1 = np.frombuffer(face1_bytes, np.uint8)
        nparr2 = np.frombuffer(face2_bytes, np.uint8)
        
        img1 = cv2.imdecode(nparr1, cv2.IMREAD_COLOR)
        img2 = cv2.imdecode(nparr2, cv2.IMREAD_COLOR)
        
        # Use DeepFace to compare
        result = DeepFace.verify(img1, img2, model_name="Facenet", distance_metric="cosine")
        
        # DeepFace returns distance (lower = more similar)
        # Convert to confidence (1.0 = identical, 0.0 = different)
        confidence = 1.0 - result['distance']
        
        return min(confidence, 1.0)  # Cap at 1.0
    
    except Exception as e:
        return 0.0
```

---

## Configuration in application.yml

```yaml
payassure:
  # Face Matching Service
  face-matching:
    provider: aws-rekognition        # google-vision | aws-rekognition | local-opencv
    confidence-threshold: 0.85       # Minimum acceptable confidence
    max-retries: 3                   # Allow up to 3 selfie retakes
    
    # Google Cloud Vision
    google:
      project-id: ${GOOGLE_CLOUD_PROJECT_ID}
      credentials-path: ${GOOGLE_CLOUD_CREDENTIALS_PATH}
    
    # AWS Rekognition
    aws:
      region: ap-south-1
      access-key: ${AWS_ACCESS_KEY_ID}
      secret-key: ${AWS_SECRET_ACCESS_KEY}
    
    # Local OpenCV Service
    python:
      service-url: http://localhost:5000/api/compare-faces
      timeout-seconds: 10
  
  # OCR Configuration
  ocr:
    provider: google-vision          # google-vision | tesseract | aws-textract
    aadhaar-regex-pattern: ^\d{12}$
    name-similarity-threshold: 0.85
```

---

## Database Schema for Validations

```sql
ALTER TABLE workers ADD COLUMN IF NOT EXISTS (
    aadhaar_photo_base64 LONGTEXT,           -- Aadhaar photo from UIDAI
    selfie_photo_base64 LONGTEXT,            -- Captured selfie
    selfie_verified BOOLEAN DEFAULT FALSE,   -- Liveness passed?
    selfie_liveness_confidence DECIMAL(5, 4),-- Confidence 0.0-1.0
    aadhaar_match_confidence DECIMAL(5, 4)   -- Photo match confidence
);

-- Audit trail for validations
CREATE TABLE IF NOT EXISTS validation_audits (
    id VARCHAR(36) PRIMARY KEY,
    worker_id VARCHAR(36) NOT NULL,
    validation_type VARCHAR(50),     -- AADHAAR_OTP | FACE_MATCH | SELFIE_LIVENESS | ID_CARD_OCR
    status VARCHAR(20),              -- PASSED | FAILED | PENDING
    confidence DECIMAL(5, 4),
    error_message TEXT,
    attempt_number INT,
    created_at DATETIME,
    
    FOREIGN KEY (worker_id) REFERENCES workers(id),
    INDEX idx_worker (worker_id)
);
```

---

## Testing Validations

### Mock Test Data

For development/testing, bypass actual face matching:

```yaml
payassure:
  face-matching:
    mock-mode: true                  # Set to false in production
    mock-confidence: 0.95
```

### Test Scenarios

1. **Perfect Match**: Aadhaar photo and selfie are same person
   - Expected: confidence > 0.9, Proceed

2. **Poor Quality Selfie**: Image is blurry/low light
   - Expected: confidence < 0.8, Request retry

3. **Different Person**: Selfie is someone else
   - Expected: confidence < 0.6, Reject, Block account

4. **Spoofed Selfie**: Printed photo held to camera
   - Expected: Liveness detection fails, Reject

---

## Security Considerations

1. **Secure Storage**: Store photos with AES-256 encryption
2. **PII Masking**: Never log face data or Aadhaar numbers in plaintext
3. **Rate Limiting**: Limit validation attempts to prevent brute force
4. **Audit Trail**: Log all validation attempts with timestamp
5. **GDPR Compliance**: Allow workers to request photo deletion after 1 year

---

## Performance Optimization

- Cache face embeddings for efficiency
- Use batch processing for multiple validations
- Implement CDN for base64 image transmission
- Queue heavy computations asynchronously

---

## References

- **DeepFace**: https://github.com/serengil/deepface
- **Google Cloud Vision**: https://cloud.google.com/vision/docs/face-detection
- **AWS Rekognition**: https://docs.aws.amazon.com/rekognition/
- **OpenCV**: https://opencv.org/
- **Setu Aadhaar API**: https://docs.setu.co/api/aadhaar
