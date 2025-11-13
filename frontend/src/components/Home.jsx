import React, { useEffect, useState } from "react";
import { Search, Bell, BellOff } from "lucide-react";
import api from "../services/api";
import toast from "react-hot-toast";
import { useAuth } from "../context/AuthContext";

const Home = ({ onSelectModule }) => {
  const { user } = useAuth();
  const [modules, setModules] = useState([]);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [subscriptions, setSubscriptions] = useState([]);

  useEffect(() => {
    const abortController = new AbortController();

    const fetchModules = async () => {
      setLoading(true);
      try {
        const { data } = await api.get("/api/modules", {
          signal: abortController.signal,
        });
        if (data && Array.isArray(data.modules)) {
          setModules(data.modules);
          setError(null);
        } else {
          console.warn("Unexpected modules response format:", data);
          setModules([]);
          setError("Unexpected modules response format.");
        }
      } catch (e) {
        // Don't show error if request was cancelled (e.g., component unmounted)
        if (!abortController.signal.aborted) {
          console.error("Failed to load modules", e);
          if (e.code !== "ERR_CANCELED" && e.name !== "CanceledError") {
            toast.error("Failed to load modules");
          }
          setModules([]);
          setError("Unable to retrieve modules. Please try again.");
        }
      } finally {
        if (!abortController.signal.aborted) {
          setLoading(false);
        }
      }
    };

    const fetchSubscriptions = async () => {
      try {
        const { data } = await api.get("/api/subscriptions", {
          signal: abortController.signal,
        });
        if (data && Array.isArray(data.subscriptions)) {
          setSubscriptions(data.subscriptions);
        }
      } catch (e) {
        if (!abortController.signal.aborted) {
          console.error("Failed to load subscriptions", e);
          // Don't show error toast for subscriptions, just log it
        }
      }
    };

    fetchModules();
    fetchSubscriptions();

    // Cleanup: abort request if component unmounts
    return () => {
      abortController.abort();
    };
  }, []);

  const filtered = modules.filter(
    (m) =>
      m.name.toLowerCase().includes(query.toLowerCase()) ||
      m.code.toLowerCase().includes(query.toLowerCase())
  );

  const isSubscribed = (moduleCode) => subscriptions.includes(moduleCode);

  const handleSubscriptionToggle = async (e, moduleCode) => {
    e.stopPropagation(); // Prevent module card click

    const subscribed = isSubscribed(moduleCode);
    const endpoint = subscribed
      ? `/api/subscriptions/${moduleCode}/unsubscribe`
      : `/api/subscriptions/${moduleCode}/subscribe`;

    try {
      const { data } = await api.post(endpoint);

      if (data.success) {
        if (subscribed) {
          setSubscriptions(subscriptions.filter((code) => code !== moduleCode));
          toast.success(`Unsubscribed from module`);
        } else {
          setSubscriptions([...subscriptions, moduleCode]);
          toast.success(`Subscribed to module notifications`);
        }
      }
    } catch (e) {
      console.error("Subscription toggle failed", e);
      toast.error("Failed to update subscription");
    }
  };

  return (
    <div className="max-w-6xl mx-auto p-6">
      <div className="mb-8 text-center">
        <h1 className="text-3xl font-bold text-gray-800 mb-2">
          Welcome, {user?.displayName || user?.email}
        </h1>
        <p className="text-gray-600 max-w-2xl mx-auto">
          UniShare helps you discover and share study materials across modules.
          Search a module and dive into its shared resources.
        </p>
      </div>
      <div className="mb-6 max-w-md mx-auto flex items-center border border-gray-300 rounded-lg bg-white px-3">
        <Search size={18} className="text-gray-500 mr-2" />
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search modules..."
          className="w-full py-2 focus:outline-none"
        />
      </div>
      {loading ? (
        <div className="text-center text-gray-600">Loading modules...</div>
      ) : error ? (
        <div className="text-center text-red-600">{error}</div>
      ) : (
        <div className="grid gap-6 grid-cols-1 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map((mod) => (
            <div
              key={mod.code}
              className="relative bg-white rounded-xl shadow hover:shadow-md transition p-5 border border-gray-200 group cursor-pointer"
              onClick={() => onSelectModule(mod)}
            >
              <div className="flex justify-between items-start mb-2">
                <span className="inline-block px-2 py-1 text-xs font-semibold rounded-full bg-blue-100 text-blue-800 group-hover:bg-blue-200">
                  {mod.code}
                </span>
                <div className="flex items-center gap-2">
                  {mod.fileCount > 0 && (
                    <span className="text-xs text-gray-500">
                      {mod.fileCount} file{mod.fileCount !== 1 && "s"}
                    </span>
                  )}
                  <button
                    onClick={(e) => handleSubscriptionToggle(e, mod.code)}
                    className={`p-1.5 rounded-full transition-colors ${
                      isSubscribed(mod.code)
                        ? "bg-blue-100 text-blue-700 hover:bg-blue-200"
                        : "bg-gray-100 text-gray-500 hover:bg-gray-200"
                    }`}
                    title={
                      isSubscribed(mod.code)
                        ? "Unsubscribe from notifications"
                        : "Subscribe to notifications"
                    }
                  >
                    {isSubscribed(mod.code) ? (
                      <Bell size={14} fill="currentColor" />
                    ) : (
                      <BellOff size={14} />
                    )}
                  </button>
                </div>
              </div>
              <h2 className="text-lg font-semibold text-gray-800 mb-1 group-hover:text-blue-700">
                {mod.name}
              </h2>
              <p className="text-sm text-gray-600 line-clamp-3">
                {mod.description}
              </p>
            </div>
          ))}
          {filtered.length === 0 && (
            <div className="col-span-full text-center text-gray-500">
              No modules match "{query}"
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default Home;
