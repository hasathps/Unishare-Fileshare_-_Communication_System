# Adding New Modules to UniShare

This guide explains how to add new course modules to the UniShare system. Follow these steps to ensure proper integration with both frontend and backend components.

## 📋 Prerequisites

- Basic understanding of Java and React
- Access to the UniShare codebase
- Both frontend and backend servers running

## 🎯 Step-by-Step Guide

### Step 1: Add Module to Backend Service

**File:** `backend/src/main/java/com/unishare/service/FileService.java`

1. **Locate the `createUploadDirectories()` method** (around line 28)
2. **Add your new module** to the modules array:

```java
// Create module directories
String[] modules = {"IN3111", "CS101", "MATH201", "YOUR_NEW_MODULE"};
```

**Example:**
```java
String[] modules = {"IN3111", "CS101", "MATH201", "PHYS202", "CHEM103"};
```

### Step 2: Add Module to Frontend Component

**File:** `frontend/src/components/FileUploader.jsx`

1. **Locate the modules array** (around line 13)
2. **Add your new module** to the array:

```javascript
const modules = ['IN3111', 'CS101', 'MATH201', 'YOUR_NEW_MODULE']
```

**Example:**
```javascript
const modules = ['IN3111', 'CS101', 'MATH201', 'PHYS202', 'CHEM103']
```

### Step 3: Update Files Section Component

**File:** `frontend/src/components/FilesSection.jsx`

1. **Locate the module filter dropdown** (around line 25)
2. **Add your new module** to the options:

```javascript
<select className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent">
  <option value="">All Modules</option>
  <option value="IN3111">IN3111</option>
  <option value="CS101">CS101</option>
  <option value="MATH201">MATH201</option>
  <option value="YOUR_NEW_MODULE">YOUR_NEW_MODULE</option>
</select>
```

### Step 4: Update Chat Section Component

**File:** `frontend/src/components/ChatSection.jsx`

1. **Locate the module selection dropdown** (around line 85)
2. **Add your new module** to the options:

```javascript
<select className="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent">
  <option value="">Select Module</option>
  <option value="IN3111">IN3111</option>
  <option value="CS101">CS101</option>
  <option value="MATH201">MATH201</option>
  <option value="YOUR_NEW_MODULE">YOUR_NEW_MODULE</option>
</select>
```

### Step 5: Update Monitor Section Component

**File:** `frontend/src/components/MonitorSection.jsx`

1. **Locate the module statistics section** (around line 95)
2. **Add your new module** to the modules array:

```javascript
{['IN3111', 'CS101', 'MATH201', 'YOUR_NEW_MODULE'].map((module, index) => (
  <div key={index} className="text-center p-4 border border-gray-200 rounded-lg">
    <h4 className="text-lg font-semibold text-gray-800 mb-2">{module}</h4>
    // ... rest of the component
  </div>
))}
```

### Step 6: Create Module Directory

**Manual Step:** Create the physical directory for file storage

1. **Navigate to the backend directory:**
   ```bash
   cd backend/uploads
   ```

2. **Create the new module directory:**
   ```bash
   mkdir YOUR_NEW_MODULE
   ```

3. **Add a .gitkeep file** to ensure the directory is tracked by git:
   ```bash
   echo "# Files for YOUR_NEW_MODULE" > YOUR_NEW_MODULE/.gitkeep
   ```

### Step 7: Compile and Test

1. **Compile the backend:**
   ```bash
   cd backend
   javac -cp . -d build/classes src/main/java/com/unishare/service/FileService.java
   ```

2. **Restart the backend server:**
   ```bash
   java -cp build\classes com.unishare.UniShareServer
   ```

3. **Test the frontend** (if not already running):
   ```bash
   cd frontend
   npm run dev
   ```

4. **Verify the new module appears** in all dropdown menus
5. **Test uploading files** to the new module
6. **Check that files are saved** in the correct directory

## 🔍 Verification Checklist

- [ ] New module appears in FileUploader dropdown
- [ ] New module appears in FilesSection filter
- [ ] New module appears in ChatSection selector
- [ ] New module appears in MonitorSection statistics
- [ ] Files uploaded to new module are saved correctly
- [ ] Module directory exists in `backend/uploads/`
- [ ] No compilation errors in backend
- [ ] No console errors in frontend

## 📝 Example: Adding PHYS202 Module

Here's a complete example of adding a "PHYS202" module:

### Backend Changes
```java
// In FileService.java
String[] modules = {"IN3111", "CS101", "MATH201", "PHYS202"};
```

### Frontend Changes
```javascript
// In FileUploader.jsx
const modules = ['IN3111', 'CS101', 'MATH201', 'PHYS202']

// In FilesSection.jsx
<option value="PHYS202">PHYS202</option>

// In ChatSection.jsx
<option value="PHYS202">PHYS202</option>

// In MonitorSection.jsx
{['IN3111', 'CS101', 'MATH201', 'PHYS202'].map((module, index) => (
  // ... component code
))}
```

### Directory Creation
```bash
mkdir backend/uploads/PHYS202
echo "# Files for PHYS202" > backend/uploads/PHYS202/.gitkeep
```

## ⚠️ Important Notes

1. **Module Names**: Use consistent naming convention (e.g., all caps, alphanumeric)
2. **Case Sensitivity**: Ensure module names match exactly across all files
3. **Directory Names**: Module directory names should match the module codes exactly
4. **Testing**: Always test file uploads to ensure proper module assignment
5. **Git**: Don't forget to commit the new module directory and .gitkeep file

## 🐛 Troubleshooting

### Module Not Appearing in Dropdown
- Check that the module name is added to all frontend components
- Verify there are no typos in the module name
- Refresh the frontend page

### Files Not Saving to Correct Module
- Verify the backend FileService has the new module
- Check that the module directory exists
- Restart the backend server after changes

### Compilation Errors
- Ensure all Java files are compiled together
- Check for syntax errors in the modules array
- Verify package imports are correct

## 📞 Support

If you encounter issues while adding new modules, please:
1. Check this guide thoroughly
2. Verify all steps were completed
3. Check the console for error messages
4. Create an issue in the repository with details

---

**Happy coding! 🚀**
