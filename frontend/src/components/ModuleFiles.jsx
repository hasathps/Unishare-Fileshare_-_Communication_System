import React, { useEffect, useState, useMemo } from "react";
import {
  ArrowLeft,
  UploadCloud,
  FileText,
  Download,
  Trash2,
} from "lucide-react";
import api from "../services/api";
import toast from "react-hot-toast";
import { useAuth } from "../context/AuthContext";

const ModuleFiles = ({ module, onUploadClick, onBack, refreshKey }) => {
  const { user } = useAuth();
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");

  useEffect(() => {
    if (!module) return;

    const abortController = new AbortController();

    const fetchFiles = async () => {
      setLoading(true);
      try {
        const { data } = await api.get(`/api/modules/${module.code}`, {
          signal: abortController.signal,
        });
        if (data && Array.isArray(data.files)) {
          setFiles(data.files);
        } else {
          console.warn("Unexpected response format:", data);
          setFiles([]);
        }
      } catch (e) {
        // Don't show error if request was cancelled (component unmounted or module changed)
        if (!abortController.signal.aborted) {
          console.error("Failed to load module files", e);
          if (e.code !== "ERR_CANCELED" && e.name !== "CanceledError") {
            toast.error("Failed to load files for module");
          }
          setFiles([]);
        }
      } finally {
        if (!abortController.signal.aborted) {
          setLoading(false);
        }
      }
    };

    fetchFiles();

    // Cleanup: abort request if component unmounts or module changes
    return () => {
      abortController.abort();
    };
  }, [module, refreshKey]);
  const handleDelete = async (file) => {
    if (!file.id) {
      toast.error("Cannot delete: missing file id");
      return;
    }
    if (
      !window.confirm(
        `Delete file "${file.filename}"? This action cannot be undone.`
      )
    ) {
      return;
    }
    try {
      await api.delete(`/api/files/${file.id}`);
      toast.success("File deleted");
      // Refetch
      const { data } = await api.get(`/api/modules/${module.code}`);
      setFiles(data.files || []);
    } catch (e) {
      console.error("Delete failed", e);
      toast.error("Failed to delete file");
    }
  };

  const filtered = useMemo(() => {
    const q = search.toLowerCase();
    return files.filter(
      (f) =>
        f.filename?.toLowerCase().includes(q) ||
        f.uploaderName?.toLowerCase().includes(q)
    );
  }, [files, search]);

  if (!module) return null;

  return (
    <div className="max-w-6xl mx-auto p-6">
      <div className="flex items-center mb-4">
        <button
          onClick={onBack}
          className="text-gray-600 hover:text-blue-600 flex items-center mr-4"
        >
          <ArrowLeft size={18} className="mr-1" /> Back
        </button>
        <h1 className="text-2xl font-bold text-gray-800">
          {module.name}
          <span className="ml-3 text-sm font-medium text-blue-600 bg-blue-50 px-2 py-1 rounded-full align-middle">
            {files.length} file{files.length !== 1 && "s"}
          </span>
        </h1>
      </div>
      <p className="text-gray-600 mb-6">{module.description}</p>
      <div className="flex flex-col sm:flex-row gap-4 mb-6">
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search by file name or uploader..."
          className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        />
        <button
          onClick={onUploadClick}
          className="px-5 py-2 rounded-lg bg-blue-600 text-white font-medium flex items-center hover:bg-blue-700 shadow"
        >
          <UploadCloud size={18} className="mr-2" /> Upload Files
        </button>
      </div>
      <div className="bg-white rounded-lg shadow overflow-hidden">
        <table className="w-full">
          <thead>
            <tr className="bg-gray-50">
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Filename
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Uploader
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Uploaded
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Size
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {loading && (
              <tr>
                <td colSpan={5} className="p-6 text-center text-gray-500">
                  Loading files...
                </td>
              </tr>
            )}
            {!loading && filtered.length === 0 && (
              <tr>
                <td colSpan={5} className="p-6 text-center text-gray-500">
                  No files match your search.
                </td>
              </tr>
            )}
            {filtered.map((f) => (
              <tr key={f.id || f.filename} className="hover:bg-gray-50">
                <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-900">
                  <div className="flex items-center">
                    <FileText size={16} className="text-blue-600 mr-2" />
                    {f.filename}
                  </div>
                </td>
                <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-700">
                  {f.uploaderName || "Unknown"}
                </td>
                <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-700">
                  {f.uploadDate}
                </td>
                <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-700">
                  {f.formattedFileSize || f.fileSize + " B"}
                </td>
                <td className="px-4 py-3 whitespace-nowrap text-sm">
                  <div className="flex items-center space-x-2">
                    {/* Preview removed per request */}
                    {f.secureUrl && (
                      <a
                        href={f.secureUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        download
                        className="text-green-600 hover:text-green-800 p-1"
                        title="Download"
                      >
                        <Download size={16} />
                      </a>
                    )}
                    {/* Only show delete button if current user uploaded this file */}
                    {user && f.uploaderName === user.email && (
                      <button
                        onClick={() => handleDelete(f)}
                        className="text-red-600 hover:text-red-800 p-1"
                        title="Delete"
                      >
                        <Trash2 size={16} />
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default ModuleFiles;
