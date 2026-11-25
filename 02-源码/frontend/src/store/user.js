import { reactive, computed } from 'vue'

/**
 * 登录态：后端 /auth/login 返回 { token, user, menus }
 * user 字段：id/username/realName/avatar/roleCode/roleName/orgName/dataScope
 * menus 为树：{ id, parentId, name, path, icon, sortNo, children }
 */
function readUser() {
  try {
    const raw = localStorage.getItem('user')
    if (!raw || raw === 'undefined' || raw === 'null') return {}
    return JSON.parse(raw) || {}
  } catch (e) {
    return {}
  }
}

export const store = reactive({
  user: readUser(),
  menus: (() => {
    try {
      return JSON.parse(localStorage.getItem('menus') || '[]') || []
    } catch (e) {
      return []
    }
  })(),
  unread: 0
})

export function setLogin(data) {
  localStorage.setItem('token', data.token)
  localStorage.setItem('user', JSON.stringify(data.user || {}))
  localStorage.setItem('menus', JSON.stringify(data.menus || []))
  store.user = data.user || {}
  store.menus = data.menus || []
}

export function clearLogin() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  localStorage.removeItem('menus')
  store.user = {}
  store.menus = []
}

export function isCustomer() {
  return store.user.roleCode === 'CUSTOMER'
}

export const roleCode = computed(() => store.user.roleCode || '')

/** 侧栏底色按角色区分（design-system 第 5 节） */
export const sideColor = computed(() => {
  const map = {
    LEADER: '#19324A',
    CS: '#23415C',
    DEPT: '#2A4E5E',
    ADMIN: '#121F2E'
  }
  return map[store.user.roleCode] || '#19324A'
})

/** 角色简称，用于顶栏 */
export const roleShort = computed(() => {
  const map = { LEADER: '客服组长', CS: '客服专员', DEPT: '部门处理人', ADMIN: '系统管理员', CUSTOMER: '客户' }
  return map[store.user.roleCode] || store.user.roleName || ''
})
