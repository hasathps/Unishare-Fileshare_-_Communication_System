import React, { useEffect, useMemo, useState } from 'react'
import { BarChart3, TrendingUp, Users, FileText, Activity, HardDrive, RefreshCw } from 'lucide-react'
import api from '../services/api'
import toast from 'react-hot-toast'

const colorClasses = {
  blue: 'bg-blue-100 text-blue-600',
  green: 'bg-green-100 text-green-600',
  purple: 'bg-purple-100 text-purple-600',
  orange: 'bg-orange-100 text-orange-600'
}

const formatNumber = (value) => {
  if (value === null || value === undefined || Number.isNaN(value)) return '0'
  return Number(value).toLocaleString()
}

const formatPercent = (value) => {
  if (value === null || value === undefined || Number.isNaN(value)) return '0%'
  const formatted = Number(value).toFixed(1)
  return `${Number(value) > 0 ? '+' : ''}${formatted}%`
}

const relativeTime = (isoDate) => {
  if (!isoDate) return 'Unknown time'
  const timestamp = new Date(isoDate)
  const diff = Date.now() - timestamp.getTime()
  if (diff < 60000) return 'Just now'
  const minutes = Math.floor(diff / 60000)
  if (minutes < 60) return `${minutes} min ago`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} h ago`
  const days = Math.floor(hours / 24)
  return `${days} d ago`
}

const MonitorSection = () => {
  const [metrics, setMetrics] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const fetchMetrics = async () => {
    setLoading(true)
    setError(null)
    try {
      const { data } = await api.get('/api/monitor')
      setMetrics(data)
    } catch (e) {
      console.error('Failed to load monitoring data', e)
      setError('Unable to load monitoring data right now.')
      toast.error('Failed to load monitoring data')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchMetrics()
  }, [])

  const system = metrics?.systemMonitor ?? {}
  const userActivity = metrics?.userActivity ?? {}
  const uploadStats = metrics?.uploadStatistics ?? {}
  const moduleAnalytics = metrics?.moduleAnalytics ?? {}
  const performance = metrics?.performanceMetrics ?? {}
  const activityFeed = metrics?.activityFeed ?? []

  const statCards = useMemo(() => {
    return [
      {
        label: 'Total Files',
        value: formatNumber(system.totalFiles),
        change: `${formatNumber(uploadStats.uploadsLast24h)} uploads in last 24h`,
        icon: FileText,
        color: 'blue'
      },
      {
        label: 'Active Users (24h)',
        value: formatNumber(system.activeUsers24h ?? userActivity.activeUsers24h),
        change: `${formatNumber(userActivity.activeUsers7d)} active this week`,
        icon: Users,
        color: 'green'
      },
      {
        label: 'Downloads (24h)',
        value: formatNumber(system.downloadsLast24h),
        change: `${formatPercent(system.downloadChangePercent)} vs prev 24h`,
        icon: TrendingUp,
        color: 'purple'
      },
      {
        label: 'Storage Used',
        value: system.storageUsedFormatted || '0 B',
        change: `${formatPercent(system.uploadChangePercent)} upload growth`,
        icon: HardDrive,
        color: 'orange'
      }
    ]
  }, [system, userActivity, uploadStats])

  const dailyUploads = uploadStats.dailyUploads ?? []
  const dailyDownloads = uploadStats.dailyDownloads ?? []
  const maxUploadValue = Math.max(...dailyUploads.map((d) => d.value || 0), 1)
  const maxDownloadValue = Math.max(...dailyDownloads.map((d) => d.value || 0), 1)

  if (loading) {
    return (
      <div className="max-w-6xl mx-auto p-6">
        <div className="bg-white rounded-lg shadow-md p-8 flex items-center justify-center text-gray-600">
          <RefreshCw className="animate-spin mr-3" size={20} />
          Loading system metrics...
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="max-w-6xl mx-auto p-6">
        <div className="bg-white rounded-lg shadow-md p-8 text-center text-gray-700">
          <p className="mb-4">{error}</p>
          <button
            onClick={fetchMetrics}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
          >
            Retry
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-6xl mx-auto p-6 space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h2 className="text-2xl font-semibold text-gray-800 flex items-center">
            <BarChart3 className="mr-2 text-blue-600" size={24} />
            System Monitor
          </h2>
          <p className="text-gray-600 mt-1">Track system health, usage, and engagement in real time.</p>
        </div>
        {performance.generatedAt && (
          <p className="text-sm text-gray-500">Last updated {relativeTime(performance.generatedAt)}</p>
        )}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {statCards.map((stat, index) => {
          const Icon = stat.icon
          return (
            <div key={index} className="bg-white rounded-lg shadow-md p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-600">{stat.label}</p>
                  <p className="text-2xl font-semibold text-gray-900 mt-1">{stat.value}</p>
                  <p className="text-xs text-gray-500 mt-1">{stat.change}</p>
                </div>
                <div className={`p-3 rounded-full ${colorClasses[stat.color]}`}>
                  <Icon size={22} />
                </div>
              </div>
            </div>
          )
        })}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white rounded-lg shadow-md p-6">
          <h3 className="text-lg font-semibold text-gray-800 mb-4">Upload Activity (7d)</h3>
          <div className="space-y-3">
            {dailyUploads.map((day) => (
              <div key={day.date} className="flex items-center">
                <span className="w-20 text-xs font-medium text-gray-500">{day.date.slice(5)}</span>
                <div className="flex-1 h-3 bg-gray-100 rounded-full overflow-hidden">
                  <div
                    className="h-full bg-blue-500 rounded-full transition-all"
                    style={{
                      width: `${Math.max((day.value / maxUploadValue) * 100, day.value > 0 ? 10 : 4)}%`
                    }}
                  />
                </div>
                <span className="w-12 text-right text-xs text-gray-600 ml-3">{day.value}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-md p-6">
          <h3 className="text-lg font-semibold text-gray-800 mb-4">Download Activity (7d)</h3>
          <div className="space-y-3">
            {dailyDownloads.map((day) => (
              <div key={day.date} className="flex items-center">
                <span className="w-20 text-xs font-medium text-gray-500">{day.date.slice(5)}</span>
                <div className="flex-1 h-3 bg-gray-100 rounded-full overflow-hidden">
                  <div
                    className="h-full bg-purple-500 rounded-full transition-all"
                    style={{
                      width: `${Math.max((day.value / maxDownloadValue) * 100, day.value > 0 ? 10 : 4)}%`
                    }}
                  />
                </div>
                <span className="w-12 text-right text-xs text-gray-600 ml-3">{day.value}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white rounded-lg shadow-md p-6">
          <h3 className="text-lg font-semibold text-gray-800 mb-4">Recent Activity</h3>
          <div className="space-y-4">
            {activityFeed.length === 0 && (
              <p className="text-sm text-gray-500">No recent activity recorded yet.</p>
            )}
            {activityFeed.map((activity, index) => (
              <div key={`${activity.type}-${index}`} className="flex items-start space-x-3">
                <div className="flex-shrink-0">
                  <div className="w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center">
                    <Activity size={16} className="text-blue-600" />
                  </div>
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-semibold text-gray-900">{activity.type}</p>
                  <p className="text-sm text-gray-700">
                    {activity.primary}
                    {activity.secondary && (
                      <span className="text-gray-500"> • {activity.secondary}</span>
                    )}
                  </p>
                  {activity.module && (
                    <p className="text-xs text-blue-600 bg-blue-50 inline-flex px-2 py-0.5 rounded-full mt-1">
                      {activity.module}
                    </p>
                  )}
                  <p className="text-xs text-gray-500 mt-1">{relativeTime(activity.timestamp)}</p>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-md p-6">
          <h3 className="text-lg font-semibold text-gray-800 mb-4">Top Uploaders</h3>
          <div className="space-y-3">
            {(uploadStats.topUploaders ?? []).map((uploader) => (
              <div key={uploader.key} className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-800">{uploader.key || 'Unknown user'}</p>
                  <p className="text-xs text-gray-500">Uploads last period</p>
                </div>
                <span className="px-3 py-1 bg-blue-50 text-blue-600 rounded-full text-xs font-semibold">
                  {formatNumber(uploader.count)}
                </span>
              </div>
            ))}
            {(uploadStats.topUploaders ?? []).length === 0 && (
              <p className="text-sm text-gray-500">No upload data yet.</p>
            )}
          </div>
        </div>
      </div>

      <div className="bg-white rounded-lg shadow-md p-6">
        <h3 className="text-lg font-semibold text-gray-800 mb-4">Module Analytics</h3>
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left font-medium text-gray-500 uppercase tracking-wider">Module</th>
                <th className="px-4 py-3 text-left font-medium text-gray-500 uppercase tracking-wider">Files</th>
                <th className="px-4 py-3 text-left font-medium text-gray-500 uppercase tracking-wider">Downloads</th>
                <th className="px-4 py-3 text-left font-medium text-gray-500 uppercase tracking-wider">Storage</th>
                <th className="px-4 py-3 text-left font-medium text-gray-500 uppercase tracking-wider">Subscribers</th>
                <th className="px-4 py-3 text-left font-medium text-gray-500 uppercase tracking-wider">Last Upload</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {(moduleAnalytics.modules ?? []).map((mod) => (
                <tr key={mod.code} className="hover:bg-gray-50">
                  <td className="px-4 py-3 font-medium text-gray-800">
                    <span className="text-blue-600 font-semibold">{mod.code}</span>
                    <span className="block text-gray-600 text-xs">{mod.name}</span>
                  </td>
                  <td className="px-4 py-3 text-gray-700">{formatNumber(mod.fileCount)}</td>
                  <td className="px-4 py-3 text-gray-700">{formatNumber(mod.downloadCount)}</td>
                  <td className="px-4 py-3 text-gray-700">{mod.storageFormatted}</td>
                  <td className="px-4 py-3 text-gray-700">{formatNumber(mod.subscriptionCount)}</td>
                  <td className="px-4 py-3 text-gray-500 text-xs">
                    {mod.lastUploadAt ? relativeTime(mod.lastUploadAt) : 'No uploads yet'}
                  </td>
                </tr>
              ))}
              {(moduleAnalytics.modules ?? []).length === 0 && (
                <tr>
                  <td colSpan={6} className="px-4 py-6 text-center text-gray-500">
                    No module analytics available yet.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      <div className="bg-white rounded-lg shadow-md p-6">
        <h3 className="text-lg font-semibold text-gray-800 mb-4">System Performance</h3>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-sm">
          <div className="p-4 border border-gray-200 rounded-lg">
            <p className="text-gray-500">Database Latency</p>
            <p className="text-lg font-semibold text-gray-800 mt-1">
              {performance.databaseLatencyMs >= 0 ? `${performance.databaseLatencyMs} ms` : 'Unavailable'}
            </p>
            <p className="text-xs text-gray-500 mt-2">Status: {performance.status}</p>
          </div>
          <div className="p-4 border border-gray-200 rounded-lg">
            <p className="text-gray-500">Uptime</p>
            <p className="text-lg font-semibold text-gray-800 mt-1">{performance.uptimeFormatted}</p>
            <p className="text-xs text-gray-500 mt-2">{formatNumber(performance.uptimeSeconds)} seconds</p>
          </div>
          <div className="p-4 border border-gray-200 rounded-lg">
            <p className="text-gray-500">Memory Usage</p>
            <p className="text-lg font-semibold text-gray-800 mt-1">{performance.memory?.usedFormatted}</p>
            <p className="text-xs text-gray-500 mt-2">
              Free: {performance.memory?.freeFormatted} / Total: {performance.memory?.totalBytes ? formatNumber(performance.memory.totalBytes) : '0'} B
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}

export default MonitorSection
