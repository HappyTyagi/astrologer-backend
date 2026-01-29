# OTP Authentication API Examples

## Base URL
```
http://localhost:1234
```

## 1. Send OTP (mLogin)

**Endpoint:** `POST /otp/send`

**Request Body:**
```json
{
  "mobileNo": "7906396608"
}
```

**Success Response (200 OK):**
```json
{
  "sessionId": "d4cd2517-1722-4381-8819-e3d420a62acd",
  "message": "OTP sent successfully",
  "mobileNo": "79****6608",
  "success": true
}
```

**Error Response (500 Internal Server Error):**
```json
{
  "sessionId": null,
  "message": "Failed to send OTP: Connection error",
  "mobileNo": null,
  "success": false
}
```

---

## 2. Verify OTP

**Endpoint:** `POST /otp/verify`

**Request Body:**
```json
{
  "otp": "521649",
  "sessionId": "d4cd2517-1722-4381-8819-e3d420a62acd",
  "mobileNo": "7906396608"
}
```

**Success Response - Existing User (200 OK):**
```json
{
  "success": true,
  "message": "OTP verified successfully",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 741,
  "name": "John Doe",
  "mobileNo": "7906396608",
  "email": "john@example.com",
  "isNewUser": false,
  "isProfileComplete": true
}
```

**Success Response - New User Created (200 OK):**
```json
{
  "success": true,
  "message": "New user created and logged in",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 1,
  "name": "User",
  "mobileNo": "7906396608",
  "email": null,
  "isNewUser": true,
  "isProfileComplete": false
}
```

**Error Response - Invalid OTP (401 Unauthorized):**
```json
{
  "success": false,
  "message": "Invalid OTP or session",
  "token": null,
  "refreshToken": null,
  "userId": null,
  "name": null,
  "mobileNo": null,
  "email": null,
  "isNewUser": null
}
```

**Error Response - Server Error (500 Internal Server Error):**
```json
{
  "success": false,
  "message": "Failed to verify OTP: Database connection error",
  "token": null,
  "refreshToken": null,
  "userId": null,
  "name": null,
  "mobileNo": null,
  "email": null,
  "isNewUser": null
}
```

---

## 3. Complete Profile (for new users)

**Endpoint:** `POST /profile/complete`

**Description:** Complete user profile after OTP verification when `isProfileComplete: false`

**Authentication:** Required (JWT Bearer token)

**Request Body:**
```json
{
  "mobileNo": "7906396608",
  "name": "John Doe",
  "dob": "2000-01-15",
  "birthTime": "14:30",
  "amPm": "PM",
  "gender": "Male",
  "stateId": 123,
  "districtId": 456,
  "latitude": 28.7041,
  "longitude": 77.1025,
  "address": "123 Main Street, Delhi"
}
```

**Success Response (200 OK):**
```json
{
  "userId": 1,
  "name": "John Doe",
  "email": null,
  "mobileNumber": "7906396608",
  "dateOfBirth": "2000-01-15",
  "age": 26,
  "genderMasterId": 1,
  "stateMasterId": 123,
  "districtMasterId": 456,
  "latitude": 28.7041,
  "longitude": 77.1025,
  "status": true,
  "message": "Profile completed successfully"
}
```

**Error Response (400 Bad Request):**
```json
{
  "userId": null,
  "name": null,
  "email": null,
  "mobileNumber": null,
  "dateOfBirth": null,
  "age": null,
  "genderMasterId": null,
  "stateMasterId": null,
  "districtMasterId": null,
  "latitude": null,
  "longitude": null,
  "status": false,
  "message": "Failed to complete profile: User not found"
}
```

---

## Flow

### New User Flow:
1. User enters mobile number
2. Call `POST /otp/send` with `mobileNo`
3. Receive `sessionId`
4. User enters OTP received via SMS
5. Call `POST /otp/verify` with `otp`, `sessionId`, and `mobileNo`
6. **New user is automatically created** with mobile number
7. Receive JWT `token` and `refreshToken` with `isNewUser: true` and `isProfileComplete: false`
8. **If `isProfileComplete: false`, call `POST /profile/complete`** with full profile details
9. Receive updated profile response with age calculated automatically
10. User can now make authenticated API calls using the token

### Existing User Flow:
1. User enters mobile number
2. Call `POST /otp/send` with `mobileNo`
3. Receive `sessionId`
4. User enters OTP received via SMS
5. Call `POST /otp/verify` with `otp`, `sessionId`, and `mobileNo`
6. **Existing user is verified and updated**
7. Receive JWT `token` and `refreshToken` with `isNewUser: false` and `isProfileComplete: true/false`
8. If `isProfileComplete: false`, redirect to profile completion page
9. User can now make authenticated API calls using the token

---

## Token Usage

Use the JWT token in subsequent API calls:

**Header:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Token Expiry:**
- Access Token: 7 days
- Refresh Token: 30 days

---

## Validation Rules

### mobileNo:
- Required
- Must be exactly 10 digits
- Pattern: `^[0-9]{10}$`

### otp:
- Required
- Must be exactly 6 digits
- Pattern: `^[0-9]{6}$`

### sessionId:
- Required
- UUID format (from send OTP response)

### dob (date of birth):
- Required (for profile completion)
- Format: YYYY-MM-DD
- Pattern: `^\d{4}-\d{2}-\d{2}$`
- Example: "2000-01-15"

### birthTime:
- Required (for profile completion)
- Format: HH:MM (24-hour format)
- Pattern: `^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$`
- Example: "14:30"

### amPm:
- Required (for profile completion)
- Must be either "AM" or "PM"
- Pattern: `^(AM|PM)$`

### gender:
- Required (for profile completion)
- Must be one of: "Male", "Female", "Other"
- Case-sensitive

### stateId and districtId:
- Required (for profile completion)
- Must be valid IDs from master tables
- Use `/api/states` and `/api/districts/{stateId}` to get available options

### latitude and longitude:
- Required (for profile completion)
- Must be valid decimal coordinates
- Example: 28.7041 (latitude), 77.1025 (longitude)

### address:
- Required (for profile completion)
- Free text field for user's location address

---

## Notes

- **Auto User Creation:** When a new mobile number is verified via OTP, a user account is automatically created
- **Profile Completeness:** New users will have `isProfileComplete: false` and should be prompted to complete their profile
- **Age Calculation:** Age is automatically calculated from DOB when profile is completed/updated
- **Gender Mapping:** Gender string ("Male"/"Female"/"Other") is automatically mapped to gender master ID
- **JWT Tokens:** Use the access token for API authentication, refresh token when access token expires
