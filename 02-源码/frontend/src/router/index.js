import { createRouter, createWebHashHistory } from 'vue-router'
import { store } from '../store/user'

// 页面组件按路由懒加载，首屏只加载登录/看板
const routes = [
  { path: '/login', component: () => import('../views/Login.vue'), meta: { title: '登录', public: true } },

  // 客户门户（浅色前台布局）
  { path: '/portal', component: () => import('../views/Portal.vue'), meta: { title: '我的投诉', portal: true } },
  { path: '/portal/new', component: () => import('../views/PortalNew.vue'), meta: { title: '提交投诉', portal: true } },

  // 管理端
  { path: '/dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '服务质量驾驶舱' } },
  { path: '/intake', component: () => import('../views/IntakeDesk.vue'), meta: { title: '客诉受理台' } },
  { path: '/intake/new', component: () => import('../views/IntakeNew.vue'), meta: { title: '新建投诉登记' } },
  { path: '/ticket/list', component: () => import('../views/TicketList.vue'), meta: { title: '投诉台账' } },
  { path: '/ticket/detail/:id', component: () => import('../views/TicketDetail.vue'), meta: { title: '投诉详情' } },
  { path: '/dispatch', component: () => import('../views/Dispatch.vue'), meta: { title: '工单分派' } },
  { path: '/order/desk', component: () => import('../views/OrderDesk.vue'), meta: { title: 'SLA工单看板' } },
  { path: '/handle/desk', component: () => import('../views/HandleDesk.vue'), meta: { title: '我的处理台' } },
  { path: '/handle/edit', component: () => import('../views/HandleEdit.vue'), meta: { title: '提交处理方案' } },
  { path: '/review', component: () => import('../views/Review.vue'), meta: { title: '结果审核' } },
  { path: '/confirm', component: () => import('../views/Confirm.vue'), meta: { title: '客户确认跟踪' } },
  { path: '/revisit', component: () => import('../views/Revisit.vue'), meta: { title: '回访与评价' } },
  { path: '/reopen', component: () => import('../views/Reopen.vue'), meta: { title: '重开记录' } },
  { path: '/warning', component: () => import('../views/Warning.vue'), meta: { title: '时限预警中心' } },
  { path: '/delay', component: () => import('../views/Delay.vue'), meta: { title: '延时申请审批' } },
  { path: '/cluster', component: () => import('../views/Cluster.vue'), meta: { title: '共性问题分析' } },
  { path: '/cluster/rule', component: () => import('../views/ClusterRule.vue'), meta: { title: '聚类规则配置' } },
  { path: '/rectify', component: () => import('../views/Rectify.vue'), meta: { title: '整改任务' } },
  { path: '/knowledge', component: () => import('../views/Knowledge.vue'), meta: { title: '知识库' } },
  { path: '/archive', component: () => import('../views/Archive.vue'), meta: { title: '客户与对象档案' } },
  { path: '/sla', component: () => import('../views/Sla.vue'), meta: { title: '规则与SLA配置' } },
  { path: '/message', component: () => import('../views/Message.vue'), meta: { title: '站内消息' } },
  { path: '/profile', component: () => import('../views/Profile.vue'), meta: { title: '个人中心' } },
  { path: '/system/user', component: () => import('../views/SystemUser.vue'), meta: { title: '用户与角色' } },
  { path: '/system/dict', component: () => import('../views/SystemDict.vue'), meta: { title: '字典与参数' } },
  { path: '/system/log', component: () => import('../views/SystemLog.vue'), meta: { title: '操作日志' } },

  { path: '/', redirect: () => (store.user.roleCode === 'CUSTOMER' ? '/portal' : '/dashboard') },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({ history: createWebHashHistory(), routes })

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (to.meta.public) return true
  if (!token) return '/login'
  // 客户账号只走门户，其他角色不进门户
  if (store.user.roleCode === 'CUSTOMER' && !to.meta.portal && to.path !== '/message' && to.path !== '/profile') {
    return '/portal'
  }
  return true
})

export default router
