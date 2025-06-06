# ClusterFuzz Security Architecture

## Overview

The ClusterFuzz Java security framework provides comprehensive authentication, authorization, and security controls for the fuzzing platform. It integrates with Google Cloud Identity, supports multiple OAuth2 providers, and implements role-based access control (RBAC).

## Security Components

### 1. Authentication Layer

#### OAuth2 Integration
- **Google OAuth2**: Primary authentication provider with Google Workspace integration
- **GitHub OAuth2**: Secondary provider for development and external contributors
- **JWT Tokens**: Stateless authentication for API access
- **Firebase Authentication**: Mobile and web client authentication

#### Supported Authentication Flows
1. **Authorization Code Flow**: Web application authentication
2. **Client Credentials Flow**: Service-to-service authentication
3. **JWT Bearer Token**: API authentication
4. **Firebase Token**: Mobile/web client authentication

### 2. Authorization Layer

#### Role-Based Access Control (RBAC)
- **Hierarchical Roles**: Admin > User > Bot > Guest
- **Dynamic Permissions**: Runtime permission evaluation
- **Resource-Level Security**: Fine-grained access control
- **Method-Level Security**: Annotation-based security

#### Default Roles
- **ADMIN**: Full system access, configuration management
- **USER**: Standard user access, testcase management
- **BOT**: Automated system access, task execution
- **GUEST**: Read-only access to public resources

### 3. Security Configuration

#### Environment-Specific Security
- **Development**: Relaxed security for local development
- **Staging**: Production-like security with test data
- **Production**: Full security enforcement

#### Security Policies
- **Session Management**: Stateless JWT-based sessions
- **CORS Configuration**: Cross-origin request handling
- **CSRF Protection**: Disabled for API endpoints, enabled for web
- **Rate Limiting**: API rate limiting and throttling

## Authentication Flows

### OAuth2 Authorization Code Flow

```
User -> ClusterFuzz: Access protected resource
ClusterFuzz -> Google: Redirect to OAuth2 authorization
User -> Google: Authenticate and authorize
Google -> ClusterFuzz: Authorization code callback
ClusterFuzz -> Google: Exchange code for tokens
Google -> ClusterFuzz: Access token + ID token
ClusterFuzz -> Database: Create/update user
ClusterFuzz -> User: Set authentication cookie/JWT
```

### JWT Token Authentication

```
Client -> ClusterFuzz: API request with JWT token
ClusterFuzz -> JwtService: Validate JWT token
JwtService -> JwtService: Verify signature and expiry
JwtService -> Database: Load user and permissions
Database -> JwtService: User details and roles
JwtService -> ClusterFuzz: Authentication result
ClusterFuzz -> Client: API response
```

## Authorization Model

### Permission Structure

```
clusterfuzz:
├── admin:
│   ├── config:read
│   ├── config:write
│   ├── users:manage
│   └── system:manage
├── testcases:
│   ├── read
│   ├── write
│   ├── delete
│   └── manage
├── issues:
│   ├── read
│   ├── write
│   ├── comment
│   └── manage
├── bots:
│   ├── read
│   ├── manage
│   └── execute
└── fuzzing:
    ├── read
    ├── execute
    └── manage
```

### Role Permissions Matrix

| Role | Config | Users | Testcases | Issues | Bots | Fuzzing |
|------|--------|-------|-----------|--------|------|---------|
| ADMIN | RW | RW | RW | RW | RW | RW |
| USER | R | R | RW | RW | R | R |
| BOT | R | - | R | R | RW | RW |
| GUEST | R | - | R | R | - | - |

## Security Features

### 1. Multi-Factor Authentication (MFA)
- **TOTP Support**: Time-based one-time passwords
- **SMS Backup**: SMS-based backup codes
- **Recovery Codes**: One-time recovery codes

### 2. Session Security
- **JWT Expiry**: Configurable token expiration
- **Refresh Tokens**: Secure token refresh mechanism
- **Session Invalidation**: Immediate session termination

### 3. API Security
- **Rate Limiting**: Request rate limiting per user/IP
- **Input Validation**: Comprehensive input sanitization
- **Output Encoding**: XSS prevention
- **SQL Injection Protection**: Parameterized queries

### 4. Audit Logging
- **Authentication Events**: Login/logout tracking
- **Authorization Events**: Permission checks
- **Administrative Actions**: Configuration changes
- **Security Events**: Failed authentication attempts

## Google Cloud Integration

### Identity and Access Management (IAM)
- **Service Account Authentication**: GCP service account integration
- **IAM Role Synchronization**: Automatic role sync from GCP IAM
- **Resource-Level Permissions**: GCP resource access control

### Firebase Integration
- **User Management**: Firebase user synchronization
- **Custom Claims**: Role-based custom claims
- **Security Rules**: Firebase security rule integration

## Security Configuration

### Environment Variables

```bash
# OAuth2 Configuration
GOOGLE_OAUTH2_CLIENT_ID=your-google-client-id
GOOGLE_OAUTH2_CLIENT_SECRET=your-google-client-secret
GOOGLE_OAUTH2_HOSTED_DOMAIN=your-domain.com

# JWT Configuration
JWT_SECRET_KEY=your-jwt-secret-key
JWT_EXPIRATION_TIME=3600
JWT_REFRESH_EXPIRATION_TIME=86400

# GCP Configuration
GCP_PROJECT_ID=your-gcp-project
GCP_SERVICE_ACCOUNT_KEY_PATH=/path/to/service-account.json

# Security Settings
SECURITY_REQUIRE_HTTPS=true
SECURITY_ENABLE_CSRF=false
SECURITY_SESSION_TIMEOUT=1800
```

### Application Properties

```yaml
clusterfuzz:
  security:
    oauth2:
      enabled: true
      google:
        client-id: ${GOOGLE_OAUTH2_CLIENT_ID}
        client-secret: ${GOOGLE_OAUTH2_CLIENT_SECRET}
        hosted-domain: ${GOOGLE_OAUTH2_HOSTED_DOMAIN:}
        restrict-to-domain: true
    jwt:
      secret-key: ${JWT_SECRET_KEY}
      expiration-time: ${JWT_EXPIRATION_TIME:3600}
      refresh-expiration-time: ${JWT_REFRESH_EXPIRATION_TIME:86400}
    gcp:
      enabled: true
      project-id: ${GCP_PROJECT_ID}
      service-account-key-path: ${GCP_SERVICE_ACCOUNT_KEY_PATH:}
```

## Security Best Practices

### 1. Development Security
- **Secure Defaults**: Security-first configuration
- **Principle of Least Privilege**: Minimal required permissions
- **Defense in Depth**: Multiple security layers
- **Regular Security Reviews**: Automated and manual security audits

### 2. Production Security
- **HTTPS Enforcement**: All communication over HTTPS
- **Security Headers**: Comprehensive security headers
- **Regular Updates**: Dependency and security updates
- **Monitoring**: Real-time security monitoring

### 3. Incident Response
- **Security Alerts**: Automated security alerting
- **Incident Logging**: Comprehensive incident tracking
- **Response Procedures**: Defined incident response procedures
- **Recovery Plans**: Security incident recovery plans

## Testing Security

### Security Test Categories
1. **Authentication Tests**: OAuth2 flow testing
2. **Authorization Tests**: Permission validation
3. **Input Validation Tests**: Injection attack prevention
4. **Session Security Tests**: Session management validation
5. **API Security Tests**: API endpoint security

### Security Testing Tools
- **OWASP ZAP**: Automated security scanning
- **SonarQube**: Static security analysis
- **Snyk**: Dependency vulnerability scanning
- **Custom Security Tests**: Application-specific security tests

## Compliance and Standards

### Security Standards
- **OWASP Top 10**: Protection against common vulnerabilities
- **OAuth 2.0**: Standard OAuth2 implementation
- **JWT Best Practices**: Secure JWT implementation
- **Google Security Standards**: GCP security best practices

### Compliance Requirements
- **Data Protection**: User data protection and privacy
- **Access Logging**: Comprehensive access logging
- **Audit Trails**: Complete audit trail maintenance
- **Security Documentation**: Up-to-date security documentation