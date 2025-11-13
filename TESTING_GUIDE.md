# 🧪 Testing the Complete Subscription & Notification System

## Prerequisites

✅ Backend running on http://localhost:8080
✅ Frontend running on http://localhost:3000 (or your port)
✅ Two user accounts (or register new ones)

---

## 📋 Step-by-Step Test Guide

### **Setup: Create Two User Sessions**

#### Tab 1 - User A (Subscriber)

```
Email: nethmi@gmail.com
Password: [your password]
```

#### Tab 2 - User B (Uploader)

```
Email: john@example.com
Password: [your password]
Or create a new account by clicking "Register"
```

---

## 🔔 **Test 1: Subscribe to Module**

### In Tab 1 (User A):

1. Login to UniShare
2. You should see the **Home** page with all modules
3. Find the **"Artificial Intelligence"** module card
4. Look at the **top-right corner** of the module card
5. You'll see a **bell icon** (gray, outlined)
6. **Click the bell icon** 🔕
7. ✅ **Expected Result:**
   - Bell icon turns **blue** and **filled** 🔔
   - Toast notification: "Subscribed to module notifications"
   - Icon tooltip: "Unsubscribe from notifications"

**Visual Change:**

```
Before: 🔕 (gray, outlined)
After:  🔔 (blue, filled)
```

---

## 📤 **Test 2: Upload a File to Subscribed Module**

### In Tab 2 (User B):

1. Login to UniShare
2. Click **"Upload"** in the navigation bar
3. Select **"Artificial Intelligence"** from the module dropdown
4. Choose any file from your computer (PDF, DOCX, image, etc.)
5. Click **"Upload Files"**
6. ✅ **Expected Result:**
   - Success toast: "Files uploaded successfully"
   - File appears in the module

**Backend Console Output:**

```
📢 Notified 1 subscribers about file upload to artificial-intelligence
```

---

## 🔔 **Test 3: Receive Notification**

### In Tab 1 (User A):

1. Stay on any page (Home, Upload, Chat, etc.)
2. Look at the **top-right corner** of the navbar
3. Within **5 seconds**, you should see:

**Visual Changes:**

```
Before: 🔔 (gray bell icon)
After:  🔔 [1] (bell icon with RED BADGE showing "1")
```

4. **Click the bell icon** in the navbar
5. ✅ **Expected Result:**
   - Dropdown opens showing:
     ```
     ┌─────────────────────────────────────────────┐
     │ Notifications    Mark all read   Clear all  │
     ├─────────────────────────────────────────────┤
     │ • New file uploaded to Artificial           │ ← Blue background
     │   Intelligence: filename.pdf by User B      │
     │   Artificial Intelligence • 2:30 PM         │
     └─────────────────────────────────────────────┘
     ```
   - Blue background indicates **unread**
   - Blue dot (•) on the right side
   - Module name in blue badge
   - Timestamp showing when uploaded

---

## 🖱️ **Test 4: Navigate via Notification**

### In Tab 1 (User A):

1. **Click on the notification** in the dropdown
2. ✅ **Expected Result:**
   - Dropdown closes
   - Navigates to **"Home"** tab
   - Then automatically opens **"Artificial Intelligence"** module
   - You see the file that User B uploaded
   - Notification is marked as **read** (blue background disappears)
   - Red badge count decreases (1 → 0, badge disappears if 0)

---

## 📥 **Test 5: Multiple Notifications**

### Repeat Test 2 & 3:

#### In Tab 2 (User B):

1. Upload **another file** to "Artificial Intelligence"
2. Upload a file to a **different module** (e.g., "Network Programming")

#### In Tab 1 (User A):

1. Check the bell icon
2. ✅ **Expected Result:**
   - Badge shows **[1]** (only subscribed to AI module)
   - Only see notification for "Artificial Intelligence"
   - NO notification for "Network Programming" (not subscribed)

---

## 🔔 **Test 6: Subscribe to Multiple Modules**

### In Tab 1 (User A):

1. Go to **Home**
2. Subscribe to **"Network Programming"** by clicking its bell icon
3. Subscribe to **"Database Management"** by clicking its bell icon
4. ✅ **Expected Result:**
   - Both bells turn blue and filled
   - Toast confirms subscription for each

### In Tab 2 (User B):

1. Upload files to **all three modules**:
   - Artificial Intelligence
   - Network Programming
   - Database Management

### In Tab 1 (User A):

1. Wait 5 seconds
2. Check notification badge
3. ✅ **Expected Result:**
   - Badge shows **[3]** (three new notifications)
   - Dropdown shows all three notifications
   - All have blue backgrounds (unread)

---

## ✅ **Test 7: Mark as Read & Clear**

### In Tab 1 (User A):

1. Click the bell icon (opens dropdown with 3 notifications)
2. Click **"Mark all read"** button
3. ✅ **Expected Result:**

   - All notifications turn white (no blue background)
   - Blue dots disappear
   - Red badge disappears ([3] → gone)
   - Toast: "All notifications marked as read"

4. Click **"Clear all"** button
5. ✅ **Expected Result:**
   - All notifications disappear
   - Shows empty state:
     ```
     🔔
     No notifications yet
     Subscribe to modules to get notified about new uploads
     ```
   - Toast: "All notifications cleared"

---

## 🔄 **Test 8: Auto-Refresh**

### In Tab 1 (User A):

1. Keep notification dropdown **closed**
2. Note the current badge count (e.g., [2])

### In Tab 2 (User B):

1. Upload a new file to a subscribed module

### In Tab 1 (User A):

1. **Don't refresh** the page
2. **Wait 5 seconds**
3. ✅ **Expected Result:**
   - Badge count automatically updates [2] → [3]
   - No need to refresh page!

---

## 🚫 **Test 9: Unsubscribe**

### In Tab 1 (User A):

1. Go to **Home**
2. Find a subscribed module (bell is blue/filled)
3. **Click the blue bell icon** 🔔
4. ✅ **Expected Result:**
   - Bell turns gray and outlined 🔕
   - Toast: "Unsubscribed from module"

### In Tab 2 (User B):

1. Upload a file to that unsubscribed module

### In Tab 1 (User A):

1. Wait 10 seconds
2. ✅ **Expected Result:**
   - **NO notification** received
   - Badge count doesn't increase
   - Successfully unsubscribed!

---

## 🎯 **Test 10: Self-Upload (No Notification)**

### In Tab 1 (User A):

1. Subscribe to "Artificial Intelligence"
2. Click **"Upload"** in navbar
3. Upload a file to "Artificial Intelligence"
4. ✅ **Expected Result:**
   - File uploads successfully
   - **NO notification** for yourself
   - Red badge **doesn't appear**
   - (You don't get notified about your own uploads)

---

## 🐛 **Troubleshooting**

### Notifications Not Appearing?

1. **Check backend console**:

   ```
   📢 Notified 1 subscribers about file upload to artificial-intelligence
   ```

   If you see this, backend is working.

2. **Check frontend console** (F12):

   - Look for errors in API calls
   - Check `/api/notifications/count` is returning successfully

3. **Verify subscription**:
   ```javascript
   // In browser console:
   fetch("http://localhost:8080/api/subscriptions", {
     credentials: "include",
   })
     .then((r) => r.json())
     .then(console.log);
   // Should return: { subscriptions: ["artificial-intelligence", ...] }
   ```

### Badge Not Updating?

- Refresh the page manually
- Check that you're logged in (session cookie valid)
- Check browser console for errors

### Dropdown Not Opening?

- Click the bell icon (not near it)
- Check for JavaScript errors in console
- Refresh the page

---

## ✅ **Success Criteria**

You've successfully tested the notification system if:

- ✅ Bell icons appear on module cards
- ✅ Clicking bell toggles subscription (gray ↔ blue)
- ✅ Uploading file triggers notification for subscribers
- ✅ Red badge appears on navbar bell (within 5 seconds)
- ✅ Clicking bell shows dropdown with notifications
- ✅ Unread notifications have blue background
- ✅ Clicking notification navigates to module
- ✅ Notification marked as read when clicked
- ✅ Badge count updates automatically
- ✅ "Mark all read" and "Clear all" work
- ✅ Unsubscribing stops notifications
- ✅ Self-uploads don't create notifications

---

## 🎉 **All Tests Passed?**

**Congratulations!** Your UniShare notification system is fully functional!

Users can now:

- Subscribe to modules they care about
- Receive real-time notifications when files are uploaded
- Navigate directly to modules from notifications
- Manage their notification preferences

The system is production-ready! 🚀
