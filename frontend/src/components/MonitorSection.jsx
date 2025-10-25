import React from 'react'
import { BarChart3, TrendingUp, Users, FileText, Activity } from 'lucide-react'

const MonitorSection = () => {
  // Mock data for demonstration
  const stats = [
    { label: 'Total Files', value: '1,247', change: '+12%', icon: FileText, color: 'blue' },
    { label: 'Active Users', value: '89', change: '+5%', icon: Users, color: 'green' },
    { label: 'Downloads Today', value: '156', change: '+23%', icon: TrendingUp, color: 'purple' },
    { label: 'Storage Used', value: '2.3 GB', change: '+8%', icon: Activity, color: 'orange' }
  ]

  const recentActivity = [
    { action: 'New file uploaded', user: 'John Doe', module: 'CS101', time: '2 minutes ago' },
    { action: 'File downloaded', user: 'Jane Smith', module: 'IN3111', time: '5 minutes ago' },
    { action: 'New chat message', user: 'Mike Johnson', module: 'MATH201', time: '8 minutes ago' },
    { action: 'File deleted', user: 'Sarah Wilson', module: 'CS101', time: '12 minutes ago' }
  ]

  return (
    <div className="max-w-6xl mx-auto p-6">
      <div className="mb-6">
        <h2 className="text-2xl font-semibold text-gray-800 flex items-center">
          <BarChart3 className="mr-2 text-blue-600" size={24} />
          System Monitor
        </h2>
        <p className="text-gray-600 mt-2">Track system performance and user activity</p>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        {stats.map((stat, index) => {
          const Icon = stat.icon
          const colorClasses = {
            blue: 'bg-blue-100 text-blue-600',
            green: 'bg-green-100 text-green-600',
            purple: 'bg-purple-100 text-purple-600',
            orange: 'bg-orange-100 text-orange-600'
          }
          
          return (
            <div key={index} className="bg-white rounded-lg shadow-md p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-600">{stat.label}</p>
                  <p className="text-2xl font-semibold text-gray-900">{stat.value}</p>
                  <p className="text-sm text-green-600">{stat.change}</p>
                </div>
                <div className={`p-3 rounded-full ${colorClasses[stat.color]}`}>
                  <Icon size={24} />
                </div>
              </div>
            </div>
          )
        })}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Activity Chart Placeholder */}
        <div className="bg-white rounded-lg shadow-md p-6">
          <h3 className="text-lg font-semibold text-gray-800 mb-4">Upload Activity</h3>
          <div className="h-64 bg-gray-100 rounded-lg flex items-center justify-center">
            <div className="text-center">
              <BarChart3 size={48} className="text-gray-400 mx-auto mb-2" />
              <p className="text-gray-500">Chart visualization would go here</p>
            </div>
          </div>
        </div>

        {/* Recent Activity */}
        <div className="bg-white rounded-lg shadow-md p-6">
          <h3 className="text-lg font-semibold text-gray-800 mb-4">Recent Activity</h3>
          <div className="space-y-4">
            {recentActivity.map((activity, index) => (
              <div key={index} className="flex items-start space-x-3">
                <div className="flex-shrink-0">
                  <div className="w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center">
                    <Activity size={16} className="text-blue-600" />
                  </div>
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-gray-900">{activity.action}</p>
                  <p className="text-sm text-gray-600">
                    {activity.user} • <span className="px-2 py-1 text-xs font-semibold rounded-full bg-blue-100 text-blue-800">{activity.module}</span>
                  </p>
                  <p className="text-xs text-gray-500">{activity.time}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Module Statistics */}
      <div className="mt-6 bg-white rounded-lg shadow-md p-6">
        <h3 className="text-lg font-semibold text-gray-800 mb-4">Module Statistics</h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {['IN3111', 'CS101', 'MATH201'].map((module, index) => (
            <div key={index} className="text-center p-4 border border-gray-200 rounded-lg">
              <h4 className="text-lg font-semibold text-gray-800 mb-2">{module}</h4>
              <div className="space-y-2">
                <div className="flex justify-between">
                  <span className="text-sm text-gray-600">Files:</span>
                  <span className="text-sm font-medium">{Math.floor(Math.random() * 50) + 10}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-sm text-gray-600">Downloads:</span>
                  <span className="text-sm font-medium">{Math.floor(Math.random() * 200) + 50}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-sm text-gray-600">Users:</span>
                  <span className="text-sm font-medium">{Math.floor(Math.random() * 30) + 5}</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

export default MonitorSection
