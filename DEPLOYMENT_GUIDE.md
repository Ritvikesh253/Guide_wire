# PayAssure Production Deployment Guide

Complete guide to deploy PayAssure (all 5 features) to production on AWS, Azure, GCP, or on-premise.

## Table of Contents
1. [Pre-Deployment Checklist](#pre-deployment-checklist)
2. [AWS Deployment](#aws-deployment)
3. [Azure Deployment](#azure-deployment)
4. [GCP Deployment](#gcp-deployment)
5. [On-Premise Deployment](#on-premise-deployment)
6. [Production Configuration](#production-configuration)
7. [Monitoring & Scaling](#monitoring--scaling)
8. [Security & Compliance](#security--compliance)

---

## Pre-Deployment Checklist

- [ ] All features tested locally with `docker-compose up`
- [ ] Environment variables configured in `.env.prod`
- [ ] SSL/TLS certificates obtained (Let's Encrypt or AWS ACM)
- [ ] Database backups configured
- [ ] API keys obtained:
  - [ ] Setu Aadhaar API (production access)
  - [ ] RazorpayX (production account)
  - [ ] OpenWeatherMap API key
  - [ ] AWS Rekognition credentials (optional)
- [ ] Kubernetes manifests created (if using K8s)
- [ ] CI/CD pipeline configured (GitHub Actions, GitLab CI, Jenkins, etc.)
- [ ] Load testing completed (target: 1000 concurrent requests)
- [ ] Security audit completed

---

## AWS Deployment

### Option 1: ECS Fargate (Serverless Containers)

#### 1.1 Create ECR Repository
```bash
# Login to AWS
aws ecr get-login-password --region ap-south-1 | \
  docker login --username AWS --password-stdin 123456789012.dkr.ecr.ap-south-1.amazonaws.com

# Create ECR repositories
aws ecr create-repository --repository-name payassure-backend --region ap-south-1
aws ecr create-repository --repository-name payassure-ml-service --region ap-south-1
aws ecr create-repository --repository-name payassure-frontend --region ap-south-1

# Build and push images
docker build -t payassure-backend:latest ./backend
docker tag payassure-backend:latest 123456789012.dkr.ecr.ap-south-1.amazonaws.com/payassure-backend:latest
docker push 123456789012.dkr.ecr.ap-south-1.amazonaws.com/payassure-backend:latest

# Repeat for ml-service and frontend
```

#### 1.2 Create RDS MySQL Database
```bash
aws rds create-db-instance \
  --db-instance-identifier payassure-db \
  --db-instance-class db.t3.micro \
  --engine mysql \
  --engine-version 8.0.35 \
  --master-username admin \
  --master-user-password "YourSecurePassword123!" \
  --allocated-storage 20 \
  --storage-type gp3 \
  --publicly-accessible false \
  --region ap-south-1
```

#### 1.3 Create ECS Cluster & Services
```bash
# Create cluster
aws ecs create-cluster --cluster-name payassure-prod --region ap-south-1

# Register task definitions
aws ecs register-task-definition \
  --family payassure-backend \
  --network-mode awsvpc \
  --requires-compatibilities FARGATE \
  --cpu 512 \
  --memory 1024 \
  --container-definitions file://ecs-task-backend.json \
  --region ap-south-1

# Create service
aws ecs create-service \
  --cluster payassure-prod \
  --service-name payassure-backend \
  --task-definition payassure-backend:1 \
  --desired-count 2 \
  --launch-type FARGATE \
  --network-configuration \
    "awsvpcConfiguration={subnets=[subnet-xxx,subnet-yyy],securityGroups=[sg-xxx],assignPublicIp=DISABLED}" \
  --load-balancers targetGroupArn=arn:aws:elasticloadbalancing:...,containerName=payassure-backend,containerPort=8080 \
  --region ap-south-1

# Enable auto-scaling
aws autoscaling create-auto-scaling-group \
  --auto-scaling-group-name payassure-backend-asg \
  --launch-template LaunchTemplateName=payassure-backend,Version=\$Latest \
  --min-size 2 \
  --max-size 10 \
  --desired-capacity 2
```

#### 1.4 Create ELB/ALB
```bash
aws elbv2 create-load-balancer \
  --name payassure-alb \
  --subnets subnet-xxx subnet-yyy \
  --security-groups sg-xxx \
  --scheme internet-facing \
  --type application \
  --region ap-south-1

# Create target groups
aws elbv2 create-target-group \
  --name payassure-backend-tg \
  --protocol HTTP \
  --port 8080 \
  --vpc-id vpc-xxx \
  --target-type ip \
  --health-check-path /api/zones \
  --health-check-interval-seconds 30
```

### Option 2: Kubernetes on EKS

#### 2.1 Create EKS Cluster
```bash
eksctl create cluster \
  --name payassure-prod \
  --version 1.28 \
  --region ap-south-1 \
  --nodes 3 \
  --node-type t3.medium \
  --enable-ssm

# Get config
aws eks update-kubeconfig --region ap-south-1 --name payassure-prod
```

#### 2.2 Deploy PayAssure Helm Chart
```bash
# Create Helm chart structure
helm create payassure

# Install chart
helm install payassure ./payassure \
  --namespace payassure \
  --create-namespace \
  --values values-prod.yaml

# Verify deployment
kubectl get pods -n payassure
kubectl logs -n payassure deployment/payassure-backend
```

#### 2.3 Helm Values File (values-prod.yaml)
```yaml
replicaCount: 3

image:
  repository: 123456789012.dkr.ecr.ap-south-1.amazonaws.com/payassure-backend
  tag: "1.0.0"
  pullPolicy: IfNotPresent

service:
  type: LoadBalancer
  port: 80
  targetPort: 8080

resources:
  limits:
    cpu: 500m
    memory: 512Mi
  requests:
    cpu: 250m
    memory: 256Mi

autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70

ingress:
  enabled: true
  className: "nginx"
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
  hosts:
    - host: api.payassure.com
      paths:
        - path: /
          pathType: Prefix
  tls:
    - secretName: payassure-tls
      hosts:
        - api.payassure.com

env:
  - name: SPRING_PROFILE
    value: "kubernetes"
  - name: MYSQL_HOST
    value: "payassure-mysql.default.svc.cluster.local"
  - name: JWT_SECRET
    valueFrom:
      secretKeyRef:
        name: payassure-secrets
        key: jwt_secret

mysql:
  enabled: true
  auth:
    rootPassword: "SecureRootPwd123!"
    database: payassure_db
    username: payassure_user
    password: "SecureUserPwd123!"
  primary:
    resources:
      limits:
        memory: 256Mi
        cpu: 250m
```

---

## Azure Deployment

### Option 1: Azure Container Instances (ACI)

```bash
# Create resource group
az group create --name payassure-prod --location southeastasia

# Create container registry
az acr create --resource-group payassure-prod \
  --name payassureregistry \
  --sku Basic

# Build and push image
az acr build --registry payassureregistry \
  --image payassure-backend:latest ./backend

# Deploy to ACI
az container create \
  --resource-group payassure-prod \
  --name payassure-backend \
  --image payassureregistry.azurecr.io/payassure-backend:latest \
  --cpu 2 \
  --memory 1 \
  --registry-login-server payassureregistry.azurecr.io \
  --registry-username payassureregistry \
  --registry-password $PASSWORD \
  --environment-variables \
    SPRING_PROFILE=azure \
    MYSQL_HOST=payassure-mysql.mysql.database.azure.com \
  --ports 8080 \
  --dns-name-label payassure-backend
```

### Option 2: Azure Kubernetes Service (AKS)

```bash
# Create AKS cluster
az aks create --resource-group payassure-prod \
  --name payassure-aks \
  --node-count 3 \
  --vm-set-type VirtualMachineScaleSets \
  --enable-managed-identity \
  --network-plugin azure

# Get credentials
az aks get-credentials --resource-group payassure-prod \
  --name payassure-aks

# Deploy same Helm chart as EKS (see above)
helm install payassure ./payassure \
  --values values-prod-azure.yaml
```

### Option 3: Azure App Service (WebApps)

```bash
# Create App Service Plan
az appservice plan create --name payassure-plan \
  --resource-group payassure-prod \
  --sku B2 \
  --is-linux

# Create App Service
az webapp create --resource-group payassure-prod \
  --plan payassure-plan \
  --name payassure-backend \
  --deployment-container-image-name payassureregistry.azurecr.io/payassure-backend:latest

# Configure app settings
az webapp config appsettings set --name payassure-backend \
  --resource-group payassure-prod \
  --settings \
    SPRING_PROFILE=azure \
    MYSQL_HOST=payassure-mysql.mysql.database.azure.com \
    PORT=8080
```

---

## GCP Deployment

### Option 1: Cloud Run (Serverless)

```bash
# Build with Cloud Build
gcloud builds submit --tag gcr.io/payassure-prod/payassure-backend ./backend

# Deploy to Cloud Run
gcloud run deploy payassure-backend \
  --image gcr.io/payassure-prod/payassure-backend \
  --platform managed \
  --region asia-south1 \
  --allow-unauthenticated \
  --set-env-vars SPRING_PROFILE=gcp,MYSQL_HOST=10.0.0.3 \
  --memory 512Mi \
  --cpu 1 \
  --timeout 300 \
  --max-instances 100
```

### Option 2: GKE (Google Kubernetes Engine)

```bash
# Create GKE cluster
gcloud container clusters create payassure-prod \
  --zone asia-south1-a \
  --num-nodes 3 \
  --machine-type e2-standard-2 \
  --enable-autoscaling \
  --min-nodes 2 \
  --max-nodes 10

# Deploy Helm chart
helm install payassure ./payassure \
  --namespace payassure \
  --create-namespace \
  --values values-prod-gcp.yaml
```

### Option 3: Compute Engine + GCE

```bash
# Create VM instance
gcloud compute instances create payassure-backend \
  --image-family debian-11 \
  --image-project debian-cloud \
  --machine-type e2-standard-4 \
  --zone asia-south1-a \
  --scopes compute-rw,storage-ro \
  --metadata-from-file startup-script=startup.sh

# startup.sh content
#!/bin/bash
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh
git clone https://github.com/payassure/payassure.git
cd payassure
docker-compose up -d
```

---

## On-Premise Deployment

### Linux Server Setup

```bash
# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh | sudo sh

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" \
  -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Install MySQL (optional if not using Docker)
sudo apt-get update
sudo apt-get install -y mysql-server-8.0

# Clone PayAssure
git clone https://github.com/payassure/payassure.git
cd payassure

# Copy production config
cp .env.example .env.prod

# Start services
docker-compose -f docker-compose.prod.yml up -d
```

### Nginx Reverse Proxy Configuration

```nginx
# /etc/nginx/sites-available/payassure

upstream payassure_backend {
    server localhost:8080;
    server localhost:8081;
    server localhost:8082;
    keepalive 32;
}

server {
    listen 80;
    server_name api.payassure.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.payassure.com;

    ssl_certificate /etc/letsencrypt/live/api.payassure.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.payassure.com/privkey.pem;

    client_max_body_size 10M;

    location /api {
        proxy_pass http://payassure_backend;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    location /health {
        proxy_pass http://payassure_backend;
        access_log off;
    }
}
```

---

## Production Configuration

### .env.prod Template

```bash
# ═══════════════════════════════════════════════════════════════════
# PRODUCTION ENVIRONMENT VARIABLES
# ═══════════════════════════════════════════════════════════════════

# ─ Database
MYSQL_HOST=prod-mysql.example.com
MYSQL_PORT=3306
MYSQL_DATABASE=payassure_prod
MYSQL_USER=payassure_prod_user
MYSQL_PASSWORD=CHANGE_ME_STRONG_PASSWORD_32_CHARS_MIN

# ─ Spring Boot
SPRING_PROFILE=production
SERVER_PORT=8080
JWT_SECRET=CHANGE_ME_MIN_32_CHARS_RANDOM_STRING
JWT_EXPIRY=86400000

# ─ Setu Aadhaar API (Production)
AADHAAR_API_KEY=setu_prod_key_xxx
AADHAAR_API_SECRET=setu_prod_secret_xxx

# ─ RazorpayX (Production)
RAZORPAY_KEY_ID=rzp_live_xxxx
RAZORPAY_SECRET_KEY=rzp_live_secret_xxxx
RAZORPAYX_ACCOUNT_NUMBER=2121021200061746
RAZORPAYX_USERNAME=prod_username
RAZORPAYX_PASSWORD=prod_password

# ─ Weather API (Production)
OPENWEATHER_API_KEY=prod_openweather_key

# ─ AWS Rekognition (Face Recognition)
AWS_REGION=ap-south-1
AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY

# ─ ML Service
ML_SERVICE_PORT=5000
ML_SERVICE_HOST=ml-service.internal
FLASK_ENV=production

# ─ Disruption API
DISRUPTION_MOCK_API_URL=http://mock-api.internal/api/disruptionEvents

# ─ Frontend
REACT_APP_API_BASE_URL=https://api.payassure.com
REACT_APP_MAPBOX_TOKEN=pk_live_xxxx
NODE_ENV=production

# ─ Monitoring
DATADOG_API_KEY=dd_api_key_xxxxx
SENTRY_DSN=https://key@sentry.io/12345
LOG_LEVEL=INFO

# ─ SSL/TLS
SSL_CERT_PATH=/etc/ssl/certs/api.payassure.com.crt
SSL_KEY_PATH=/etc/ssl/private/api.payassure.com.key

# ─ Backup
BACKUP_ENABLED=true
BACKUP_SCHEDULE="0 2 * * *"  # 2 AM daily
BACKUP_RETENTION_DAYS=30
```

---

## Monitoring & Scaling

### CloudWatch / Datadog Setup

```yaml
# datadog-agent-values.yaml
datadog:
  apiKey: YOUR_DATADOG_API_KEY
  site: datadoghq.com
  tags:
    - "env:production"
    - "service:payassure"

agents:
  containers:
    agent:
      resources:
        limits:
          cpu: 200m
          memory: 256Mi

clusterAgent:
  enabled: true
```

### Auto-Scaling Configuration

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: payassure-backend-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: payassure-backend
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 50
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:
      - type: Percent
        value: 100
        periodSeconds: 15
      - type: Pods
        value: 4
        periodSeconds: 15
      selectPolicy: Max
```

---

## Security & Compliance

### SSL/TLS Certificate (Let's Encrypt)

```bash
# Using Certbot
sudo apt-get install certbot python3-certbot-nginx
sudo certbot certonly --nginx -d api.payassure.com -d payassure.com

# For Kubernetes (cert-manager)
kubectl apply -f - <<EOF
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: admin@payassure.com
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
    - http01:
        ingress:
          class: nginx
EOF
```

### Database Encryption

```sql
-- Enable encryption at rest
ALTER DATABASE payassure_prod ENCRYPTION 'Y';

-- Create encrypted user connection
CREATE USER 'payassure_user'@'%' IDENTIFIED BY 'StrongPassword123!' REQUIRE SSL;
GRANT ALL PRIVILEGES ON payassure_db.* TO 'payassure_user'@'%';
```

### Secrets Management

```bash
# Using HashiCorp Vault
vault kv put secret/payassure/prod \
  jwt_secret="$(openssl rand -base64 32)" \
  mysql_password="$(openssl rand -base64 32)" \
  razorpay_secret="rzp_live_xxxx"

# Using Kubernetes Secrets
kubectl create secret generic payassure-secrets \
  --from-literal=jwt_secret='...' \
  --from-literal=mysql_password='...' \
  --from-literal=razorpay_key='...' \
  -n payassure
```

### GDPR Compliance

```sql
-- Data retention policy
CREATE EVENT delete_old_data
ON SCHEDULE EVERY 1 DAY
DO
  DELETE FROM access_logs WHERE created_at < DATE_SUB(NOW(), INTERVAL 90 DAY);

-- Implement right to deletion
DELIMITER //
CREATE PROCEDURE delete_worker_data(IN worker_id INT)
BEGIN
  DELETE FROM payout_receipts WHERE claim_id IN 
    (SELECT id FROM claims WHERE worker_id = worker_id);
  DELETE FROM claims WHERE worker_id = worker_id;
  DELETE FROM active_policies WHERE worker_id = worker_id;
  DELETE FROM workers WHERE id = worker_id;
END //
DELIMITER ;
```

---

## Post-Deployment Validation

```bash
#!/bin/bash
# deployment-check.sh

# Check backend health
curl -f https://api.payassure.com/api/zones || exit 1

# Check database connectivity
curl -f https://api.payassure.com/api/auth/health || exit 1

# Check ML service
curl -f http://ml-service:5000/health || exit 1

# Run smoke tests
docker run --network host \
  payassure-e2e-tests:latest \
  pytest tests/smoke/*.py

echo "All deployment checks passed ✓"
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| High latency (>1s) | Check RDS CPU/connections; scale up instance or add replicas |
| Database connection timeouts | Verify VPC security groups allow MySQL traffic (port 3306) |
| Memory leak in backend | Check for unbounded cache; restart pods via rolling update |
| ML service not responding | Check Python service logs; verify GPU allocation if using GPU |
| Certificate renewal failed | Ensure renewal cron job is running; check certbot logs |

---

*Last Updated: January 15, 2024*
*PayAssure v1.0.0 — Production Deployment Guide*
