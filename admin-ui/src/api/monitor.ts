import http from './http'

export interface CacheKeyVO {
  key: string
  ttlSeconds: number
}

export interface RedisStatsVO {
  keyCount: number
  keyspaceHits: number
  keyspaceMisses: number
  hitRate: number
  keys: CacheKeyVO[]
}

export interface PartitionStatsVO {
  partition: number
  endOffset: number
  committedOffset: number
  lag: number
}

export interface KafkaStatsVO {
  topic: string
  consumerGroup: string
  totalLag: number
  partitions: PartitionStatsVO[]
}

export const getRedisStats = (): Promise<RedisStatsVO> => http.get('/api/v1/admin/monitor/redis')
export const getKafkaStats = (): Promise<KafkaStatsVO> => http.get('/api/v1/admin/monitor/kafka')
