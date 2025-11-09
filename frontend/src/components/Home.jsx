import React, { useEffect, useState } from "react";
import { Search } from "lucide-react";
import api from "../services/api";
import toast from "react-hot-toast";
import { useAuth } from "../context/AuthContext";

const Home = ({ onSelectModule }) => {
  const { user } = useAuth();
  const [modules, setModules] = useState([]);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true);

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
        } else {
          console.warn("Unexpected modules response format:", data);
          setModules([]);
        }
      } catch (e) {
        // Don't show error if request was cancelled (e.g., component unmounted)
        if (!abortController.signal.aborted) {
          console.error("Failed to load modules", e);
          if (e.code !== "ERR_CANCELED" && e.name !== "CanceledError") {
            toast.error("Failed to load modules");
          }
          setModules([]);
        }
      } finally {
        if (!abortController.signal.aborted) {
          setLoading(false);
        }
      }
    };

    fetchModules();

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
      ) : (
        <div className="grid gap-6 grid-cols-1 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map((mod) => (
            <button
              key={mod.code}
              onClick={() => onSelectModule(mod)}
              className="text-left bg-white rounded-xl shadow hover:shadow-md transition p-5 border border-gray-200 group"
            >
              <div className="flex justify-between items-start mb-2">
                <span className="inline-block px-2 py-1 text-xs font-semibold rounded-full bg-blue-100 text-blue-800 group-hover:bg-blue-200">
                  {mod.code}
                </span>
                {mod.fileCount > 0 && (
                  <span className="text-xs text-gray-500">
                    {mod.fileCount} file{mod.fileCount !== 1 && "s"}
                  </span>
                )}
              </div>
              <h2 className="text-lg font-semibold text-gray-800 mb-1 group-hover:text-blue-700">
                {mod.name}
              </h2>
              <p className="text-sm text-gray-600 line-clamp-3">
                {mod.description}
              </p>
            </button>
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
