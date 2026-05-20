import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({ timeout: 10000 })

http.interceptors.response.use(
  (res) => {
    const data = res.data
    if (data.code !== 0) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message))
    }
    return data.data
  },
  (err) => {
    ElMessage.error(err.message || '网络错误')
    return Promise.reject(err)
  },
)

export default http
