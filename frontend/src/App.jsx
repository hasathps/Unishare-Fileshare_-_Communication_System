import React, { useState } from "react";
import Navbar from "./components/Navbar";
import FileUploader from "./components/FileUploader";
import ChatSection from "./components/ChatSection";
import MonitorSection from "./components/MonitorSection";
import LoginForm from "./components/LoginForm";
import { Toaster } from "react-hot-toast";
import { useAuth } from "./context/AuthContext";
import Home from "./components/Home";
import ModuleFiles from "./components/ModuleFiles";

function App() {
  const [activeTab, setActiveTab] = useState("home");
  const [selectedModule, setSelectedModule] = useState(null);
  const [refreshTick, setRefreshTick] = useState(0);
  const { user, loading } = useAuth();

  const handleModuleSelect = (mod) => {
    setSelectedModule(mod);
    setActiveTab("module");
  };

  const renderContent = () => {
    switch (activeTab) {
      case "home":
        return <Home onSelectModule={handleModuleSelect} />;
      case "module":
        return (
          <ModuleFiles
            module={selectedModule}
            refreshKey={refreshTick}
            onUploadClick={() => setActiveTab("upload")}
            onBack={() => setActiveTab("home")}
          />
        );
      case "upload":
        return (
          <div className="max-w-4xl mx-auto p-6">
            <div className="bg-white rounded-lg shadow-md">
              <div className="px-6 py-4 border-b border-gray-200">
                <h2 className="text-2xl font-semibold text-gray-800 flex items-center">
                  <svg
                    className="mr-2 text-blue-600"
                    width="24"
                    height="24"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                    <polyline points="7,10 12,15 17,10" />
                    <line x1="12" y1="15" x2="12" y2="3" />
                  </svg>
                  Upload Service
                </h2>
                <p className="text-gray-600 mt-2">
                  Share your study materials and resources with fellow students
                </p>
              </div>
              <div className="p-6">
                <FileUploader
                  initialModule={selectedModule?.code}
                  onUploaded={() => {
                    // navigate back and refresh module files
                    setActiveTab("module");
                    setRefreshTick((x) => x + 1);
                  }}
                />
              </div>
            </div>
          </div>
        );
      case "chat":
        return <ChatSection />;
      case "monitor":
        return <MonitorSection />;
      default:
        return null;
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-100">
        <div className="text-gray-600 text-lg font-medium">
          Loading UniShare...
        </div>
      </div>
    );
  }

  if (!user) {
    return (
      <>
        <LoginForm />
        <Toaster position="top-right" />
      </>
    );
  }

  return (
    <div className="min-h-screen bg-gray-100">
      {/* Modern Navbar */}
      <Navbar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        onModuleSelect={handleModuleSelect}
      />

      {/* Main Content */}
      <main className="py-8">{renderContent()}</main>

      {/* Toast Notifications */}
      <Toaster position="top-right" />
    </div>
  );
}

export default App;
