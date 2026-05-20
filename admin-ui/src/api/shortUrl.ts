import http from './http'

export interface ShortUrlVO {
  id: number
  shortCode: string
  shortUrl: string
  originalUrl: string
  visitCount: number
  createdAt: string
  updatedAt: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export const listShortUrls = (params: {
  page: number
  size: number
  keyword?: string
}): Promise<PageResult<ShortUrlVO>> => http.get('/api/v1/admin/short-urls', { params })

export const deleteShortUrl = (id: number): Promise<void> =>
  http.delete(`/api/v1/admin/short-urls/${id}`)

export const createShortUrl = (url: string): Promise<{ shortCode: string; shortUrl: string }> =>
  http.post('/api/v1/short-url', { url })
