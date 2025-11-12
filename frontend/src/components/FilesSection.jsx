import React from "react";
import { FileText, Download, Trash2 } from "lucide-react";

const FilesSection = () => {
  // Mock data for demonstration
  const files = [
    {
      name: "Sample Document.pdf",
      module: "IN3111",
      uploader: "John Doe",
      date: "2025-01-25",
      size: "2.3 MB",
    },
    {
      name: "Lecture Notes.docx",
      module: "CS101",
      uploader: "Jane Smith",
      date: "2025-01-24",
      size: "1.8 MB",
    },
    {
      name: "Assignment.pdf",
      module: "MATH201",
      uploader: "Mike Johnson",
      date: "2025-01-23",
      size: "3.1 MB",
    },
  ];

  return (
    <div className="max-w-6xl mx-auto p-6">
      <div className="bg-white rounded-lg shadow-md">
        <div className="px-6 py-4 border-b border-gray-200">
          <h2 className="text-2xl font-semibold text-gray-800 flex items-center">
            <FileText className="mr-2 text-blue-600" size={24} />
            Files Library
          </h2>
          <p className="text-gray-600 mt-2">
            Browse and download shared study materials
          </p>
        </div>

        <div className="p-6">
          {/* Search and Filter */}
          <div className="mb-6 flex flex-col sm:flex-row gap-4">
            <div className="flex-1">
              <input
                type="text"
                placeholder="Search files..."
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
            <select className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent">
              <option value="">All Modules</option>
              <option value="IN3111">IN3111</option>
              <option value="CS101">CS101</option>
              <option value="MATH201">MATH201</option>
            </select>
          </div>

          {/* Files Table */}
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="bg-gray-50">
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    File Name
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Module
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Uploader
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Date
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Size
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {files.map((file, index) => (
                  <tr key={index} className="hover:bg-gray-50">
                    <td className="px-4 py-4 whitespace-nowrap">
                      <div className="flex items-center">
                        <FileText className="mr-2 text-blue-600" size={16} />
                        <span className="text-sm font-medium text-gray-900">
                          {file.name}
                        </span>
                      </div>
                    </td>
                    <td className="px-4 py-4 whitespace-nowrap">
                      <span className="px-2 py-1 text-xs font-semibold rounded-full bg-blue-100 text-blue-800">
                        {file.module}
                      </span>
                    </td>
                    <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-900">
                      {file.uploader}
                    </td>
                    <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-900">
                      {file.date}
                    </td>
                    <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-900">
                      {file.size}
                    </td>
                    <td className="px-4 py-4 whitespace-nowrap text-sm font-medium">
                      <div className="flex space-x-2">
                        {/* Preview removed per request */}
                        <button className="text-green-600 hover:text-green-900 p-1">
                          <Download size={16} />
                        </button>
                        <button className="text-green-600 hover:text-green-900 p-1">
                          <Download size={16} />
                        </button>
                        <button className="text-red-600 hover:text-red-900 p-1">
                          <Trash2 size={16} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
};

export default FilesSection;
