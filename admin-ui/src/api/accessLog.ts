import http from './http'
import type { PageResult } from './shortUrl'

export interface AccessLogVO {
  id: number
  shortCode: string
  clientIp: string
  userAgent: string
  referer: string
  accessedAt: string
}

export const listAccessLogs = (params: {
  shortCode: string
  page: number
  size: number
  startDate?: string
  endDate?: string
}): Promise<PageResult<AccessLogVO>> => http.get('/api/v1/admin/access-logs', { params })
