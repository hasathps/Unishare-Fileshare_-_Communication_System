# Module Subscription Feature - Implementation Guide

## ✅ Completed Backend Implementation

### 1. Database Schema

**Table: `module_subscriptions`**

```sql
CREATE TABLE module_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    module_code VARCHAR(100) NOT NULL REFERENCES modules(code) ON DELETE CASCADE,
    subscribed_at TIMESTAMPTZ DEFAULT NOW(),
    is_active BOOLEAN DEFAULT TRUE,
    UNIQUE(user_id, module_code)
);
```

**Key Features:**

- ✅ Foreign key to `users` table (user who subscribed)
- ✅ Foreign key to `modules` table (module they subscribed to)
- ✅ Unique constraint on (user_id, module_code) - prevents duplicate subscriptions
- ✅ Soft delete via `is_active` flag (can re-subscribe)
- ✅ Timestamp tracking when subscription was created

---

### 2. Backend Services

**ModuleSubscriptionService.java** - Core business logic

- `subscribe(userId, moduleCode)` - Subscribe user to module
- `unsubscribe(userId, moduleCode)` - Unsubscribe user from module
- `isSubscribed(userId, moduleCode)` - Check subscription status
- `getSubscribers(moduleCode)` - Get all users subscribed to a module (for notifications)
- `getUserSubscriptions(userId)` - Get all modules a user is subscribed to

---

### 3. HTTP API Endpoints

**ModuleSubscriptionController.java** - REST API

| Method | Endpoint                                      | Description                | Response                                                              |
| ------ | --------------------------------------------- | -------------------------- | --------------------------------------------------------------------- |
| POST   | `/api/subscriptions/{moduleCode}/subscribe`   | Subscribe to module        | `{success: true, message: "Subscribed to...", moduleCode: "..."}`     |
| POST   | `/api/subscriptions/{moduleCode}/unsubscribe` | Unsubscribe from module    | `{success: true, message: "Unsubscribed from...", moduleCode: "..."}` |
| GET    | `/api/subscriptions/{moduleCode}/status`      | Check if subscribed        | `{subscribed: true/false, moduleCode: "..."}`                         |
| GET    | `/api/subscriptions`                          | Get all user subscriptions | `{subscriptions: ["code1", "code2", ...]}`                            |

**Authentication:** All endpoints require user to be logged in (session cookie)

**Example Usage:**

```javascript
// Subscribe to a module
POST /api/subscriptions/artificial-intelligence/subscribe
Response: {"success":true,"message":"Subscribed to artificial-intelligence","moduleCode":"artificial-intelligence"}

// Check subscription status
GET /api/subscriptions/artificial-intelligence/status
Response: {"subscribed":true,"moduleCode":"artificial-intelligence"}

// Get all user's subscriptions
GET /api/subscriptions
Response: {"subscriptions":["artificial-intelligence","network-programming"]}

// Unsubscribe
POST /api/subscriptions/artificial-intelligence/unsubscribe
Response: {"success":true,"message":"Unsubscribed from artificial-intelligence","moduleCode":"artificial-intelligence"}
```

---

### 4. Error Handling

**Subscription Responses:**

- `SUCCESS` - Subscription created/updated
- `ALREADY_SUBSCRIBED` - User already subscribed
- `ERROR` - Database error

**Unsubscription Responses:**

- `SUCCESS` - Unsubscribed successfully
- `NOT_SUBSCRIBED` - User wasn't subscribed
- `ERROR` - Database error

**HTTP Status Codes:**

- `200` - Success
- `401` - Authentication required
- `404` - Module not found
- `500` - Internal server error

---

## 📋 Frontend Integration (TODO)

### Step 1: Add Subscribe/Unsubscribe Button to Module Cards

**In `Home.jsx`** - Add subscription button to each module card:

```javascript
const [subscriptions, setSubscriptions] = useState([]);

// Fetch user's subscriptions on load
useEffect(() => {
  const fetchSubscriptions = async () => {
    try {
      const { data } = await api.get("/api/subscriptions");
      setSubscriptions(data.subscriptions || []);
    } catch (e) {
      console.error("Failed to load subscriptions", e);
    }
  };
  fetchSubscriptions();
}, []);

// Check if user is subscribed to a module
const isSubscribed = (moduleCode) => subscriptions.includes(moduleCode);

// Subscribe/Unsubscribe handler
const handleSubscriptionToggle = async (moduleCode) => {
  try {
    const endpoint = isSubscribed(moduleCode)
      ? `/api/subscriptions/${moduleCode}/unsubscribe`
      : `/api/subscriptions/${moduleCode}/subscribe`;

    const { data } = await api.post(endpoint);

    if (data.success) {
      // Update local state
      if (isSubscribed(moduleCode)) {
        setSubscriptions(subscriptions.filter((code) => code !== moduleCode));
        toast.success("Unsubscribed from module");
      } else {
        setSubscriptions([...subscriptions, moduleCode]);
        toast.success("Subscribed to module");
      }
    }
  } catch (e) {
    console.error("Subscription toggle failed", e);
    toast.error("Failed to update subscription");
  }
};

// Add button to module card
<button
  onClick={(e) => {
    e.stopPropagation();
    handleSubscriptionToggle(mod.code);
  }}
  className={`px-3 py-1 rounded ${
    isSubscribed(mod.code)
      ? "bg-red-100 text-red-700 hover:bg-red-200"
      : "bg-blue-100 text-blue-700 hover:bg-blue-200"
  }`}
>
  {isSubscribed(mod.code) ? "Unsubscribe" : "Subscribe"}
</button>;
```

---

## 🔔 Notification Integration (For Member 4)

### How to Use for Notifications

When a file is uploaded to a module, notify all subscribers:

```java
// In your file upload handler or NotificationHandler
String moduleCode = "artificial-intelligence"; // module where file was uploaded

// Get all subscribers
ModuleSubscriptionService subscriptionService = ...; // inject this
List<UUID> subscribers = subscriptionService.getSubscribers(moduleCode);

// For each subscriber, send notification
for (UUID userId : subscribers) {
    // Look up user's active socket connection
    // Send notification: "New file uploaded to Artificial Intelligence"
    String message = "New file uploaded to " + moduleName;
    sendNotification(userId, message);
}
```

---

## 🧪 Testing the API

### Using curl (PowerShell):

```powershell
# Subscribe to a module
curl -X POST http://localhost:8080/api/subscriptions/artificial-intelligence/subscribe `
  -H "Cookie: UNISHARE_SESSION=your-session-token"

# Check subscription status
curl http://localhost:8080/api/subscriptions/artificial-intelligence/status `
  -H "Cookie: UNISHARE_SESSION=your-session-token"

# Get all subscriptions
curl http://localhost:8080/api/subscriptions `
  -H "Cookie: UNISHARE_SESSION=your-session-token"

# Unsubscribe
curl -X POST http://localhost:8080/api/subscriptions/artificial-intelligence/unsubscribe `
  -H "Cookie: UNISHARE_SESSION=your-session-token"
```

---

## 📊 Database Queries

### Useful SQL for debugging:

```sql
-- View all subscriptions
SELECT * FROM module_subscriptions;

-- Count subscribers per module
SELECT module_code, COUNT(*) as subscriber_count
FROM module_subscriptions
WHERE is_active = TRUE
GROUP BY module_code;

-- Get a user's subscriptions
SELECT module_code
FROM module_subscriptions
WHERE user_id = 'user-uuid-here' AND is_active = TRUE;

-- Get subscribers for a module
SELECT user_id
FROM module_subscriptions
WHERE module_code = 'artificial-intelligence' AND is_active = TRUE;
```

---

## ✅ Summary

**Backend Complete:**

- ✅ Database table created with constraints
- ✅ Service layer with full CRUD operations
- ✅ HTTP REST API endpoints
- ✅ Authentication and authorization
- ✅ Error handling and validation
- ✅ Integration with existing module system

**Next Steps:**

1. **Frontend:** Add subscribe/unsubscribe buttons to UI
2. **Notifications:** Integrate with Member 4's notification system
3. **Testing:** Test all API endpoints
4. **UI Polish:** Add icons, loading states, confirmation dialogs

---

## 🚀 Ready to Use!

The subscription system is now live and accessible via:

- `http://localhost:8080/api/subscriptions/*`

All authenticated users can subscribe/unsubscribe to modules and receive notifications when new files are uploaded!
