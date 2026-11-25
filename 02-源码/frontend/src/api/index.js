import axios from 'axios'
import { ElMessage } from 'element-plus'

// 统一请求实例：后端返回 { code, message, data }
const http = axios.create({ baseURL: '/api', timeout: 20000 })

http.interceptors.request.use((cfg) => {
  const token = localStorage.getItem('token')
  if (token) cfg.headers.Authorization = `Bearer ${token}`
  return cfg
})

http.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 200) return body.data
      // 页面切换瞬间的 401 不打扰用户
      if (body.code === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        if (!location.hash.includes('/login')) location.hash = '#/login'
        return Promise.reject(new Error(body.message || '登录已失效'))
      }
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body
  },
  (err) => {
    // 避免刷新/跳转时 axios 弹出 Failed to fetch
    if (document.visibilityState === 'visible') {
      ElMessage.error(err?.response?.data?.message || '网络异常，请稍后重试')
    }
    return Promise.reject(err)
  }
)

export default http

// ---- 接口集中定义，页面只依赖这里 ----
export const api = {
  login: (d) => http.post('/auth/login', d),
  me: () => http.get('/auth/me'),
  menus: () => http.get('/auth/menus'),
  updateProfile: (d) => http.put('/auth/profile', d),
  updatePassword: (d) => http.put('/auth/password', d),

  dashboard: (period) => http.get('/dashboard', { params: { period } }),
  options: () => http.get('/options'),

  ticketPage: (p) => http.get('/complaint-ticket/page', { params: p }),
  ticketDetail: (id) => http.get(`/complaint-ticket/${id}`),
  createTicket: (d) => http.post('/complaint-ticket', d),
  updateTicket: (id, d) => http.put(`/complaint-ticket/${id}`, d),
  transition: (id, d) => http.post(`/complaint-ticket/${id}/transition`, d),
  evaluate: (id) => http.post(`/complaint-ticket/${id}/evaluate`),
  addAttachment: (id, d) => http.post(`/complaint-ticket/${id}/attachment`, d),
  exportTickets: (p) => http.get('/complaint-ticket/export', { params: p }),

  orderDesk: () => http.get('/order/desk'),
  acceptOrder: (tid) => http.post(`/order/${tid}/accept`),
  handleDesk: () => http.get('/handle/desk'),
  submitHandle: (d) => http.post('/handle/submit', d),

  reviewPage: (p) => http.get('/review/page', { params: p }),
  review: (tid, d) => http.post(`/review/${tid}`, d),
  confirmPage: (p) => http.get('/confirm/page', { params: p }),
  confirm: (tid, d) => http.post(`/confirm/${tid}`, d),
  revisitPage: (p) => http.get('/revisit/page', { params: p }),
  revisit: (d) => http.post('/revisit', d),
  reopenPage: (p) => http.get('/reopen/page', { params: p }),
  handleReopen: (id, d) => http.post(`/reopen/${id}`, d),

  warningPage: (p) => http.get('/warning/page', { params: p }),
  handleWarning: (id, d) => http.post(`/warning/${id}/handle`, d),
  scanWarning: () => http.post('/warning/scan'),
  delayPage: (p) => http.get('/delay/page', { params: p }),
  approveDelay: (id, d) => http.post(`/delay/${id}/approve`, d),
  applyDelay: (d) => http.post('/delay', d),

  clusterPage: (p) => http.get('/cluster/page', { params: p }),
  clusterSummary: () => http.get('/cluster/summary'),
  manualLevel: (sid, d) => http.post(`/cluster/${sid}/manual`, d),
  clusterRule: () => http.get('/cluster/rule'),
  saveClusterRule: (d) => http.put('/cluster/rule', d),

  rectifyPage: (p) => http.get('/rectify/page', { params: p }),
  createRectify: (d) => http.post('/rectify', d),
  acceptRectify: (id, d) => http.post(`/rectify/${id}/accept`, d),

  knowledgePage: (p) => http.get('/knowledge/page', { params: p }),
  saveKnowledge: (d) => http.post('/knowledge', d),

  customerPage: (p) => http.get('/archive/customer/page', { params: p }),
  objectPage: (p) => http.get('/archive/object/page', { params: p }),
  userPage: (p) => http.get('/system/user/page', { params: p }),
  myTickets: (p) => http.get('/portal/my', { params: p }),

  messagePage: (p) => http.get('/system/message/page', { params: p }),
  unread: () => http.get('/system/message/unread'),
  readMessage: (id) => http.put(`/system/message/${id}/read`),
  readAll: () => http.put('/system/message/read-all'),
  roleList: () => http.get('/system/role/list'),
  menuList: () => http.get('/system/menu/list'),
  logPage: (p) => http.get('/system/log/page', { params: p })
}
