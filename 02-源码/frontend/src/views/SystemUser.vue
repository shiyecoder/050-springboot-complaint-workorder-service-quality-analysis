<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>用户与角色</h1>
        <div class="desc">系统账号按角色划分数据范围与菜单权限，客户账号只进入客户门户；角色菜单在演示环境保存于本页状态</div>
      </div>
      <div class="acts">
        <el-button size="small" @click="reload">刷新</el-button>
      </div>
    </div>

    <div class="card">
      <el-tabs v-model="tab" @tab-change="onTab">
        <!-- 用户列表 -->
        <el-tab-pane label="用户列表" name="user">
          <div class="filters">
            <el-input v-model="q.keyword" placeholder="账号 / 姓名 / 电话" clearable @keyup.enter="search" />
            <el-button type="primary" @click="search">查询</el-button>
            <el-button @click="reset">重置</el-button>
            <span class="cnt-tip">共 {{ total }} 个账号</span>
          </div>
          <el-table :data="rows" v-loading="loading" style="width:100%">
            <el-table-column label="头像" width="62">
              <template #default="{ row }">
                <img v-if="row.avatar" class="ava" :src="row.avatar" alt="用户头像" />
                <span v-else class="ava-ph">{{ (row.realName || row.username || 'U')[0] }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="username" label="账号" width="110" />
            <el-table-column prop="realName" label="姓名" width="96" />
            <el-table-column prop="phone" label="电话" width="126" />
            <el-table-column label="角色" width="128">
              <template #default="{ row }">
                <span class="tag" :class="roleTag(row.roleCode)">{{ row.roleName || row.roleCode || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="orgName" label="所属组织" min-width="150" show-overflow-tooltip />
            <el-table-column label="数据范围" width="106">
              <template #default="{ row }">{{ scopeText(row.dataScope) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <span class="tag" :class="Number(row.status) === 1 ? 't-green' : 't-gray'">
                  {{ Number(row.status) === 1 ? '启用' : '停用' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="最后登录" width="134">
              <template #default="{ row }"><span class="num" style="font-size:12px">{{ fmtTime(row.lastLoginTime) }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="88" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openUser(row)">查看</el-button>
              </template>
            </el-table-column>
            <template #empty><div style="padding:26px;color:var(--text-sub)">暂无账号</div></template>
          </el-table>
          <div class="pager">
            <el-pagination background layout="total, sizes, prev, pager, next, jumper" :total="total"
                           v-model:current-page="q.page" v-model:page-size="q.size" :page-sizes="[10, 20, 50]"
                           @current-change="load" @size-change="search" />
          </div>
        </el-tab-pane>

        <!-- 角色管理 -->
        <el-tab-pane label="角色管理" name="role">
          <div class="card-h" style="margin-top:2px">
            <h2>角色与数据范围</h2>
            <span class="sub">共 {{ roles.length }} 个角色 · 菜单权限可分配</span>
          </div>
          <el-table :data="roles" v-loading="roleLoading" style="width:100%">
            <el-table-column label="角色代码" width="112">
              <template #default="{ row }">
                <b>{{ row.roleCode }}</b>
              </template>
            </el-table-column>
            <el-table-column prop="roleName" label="角色名称" width="160" />
            <el-table-column label="数据范围" width="112">
              <template #default="{ row }">{{ scopeText(row.dataScope) }}</template>
            </el-table-column>
            <el-table-column prop="remark" label="说明" min-width="280" show-overflow-tooltip />
            <el-table-column label="菜单数" width="80" align="right">
              <template #default="{ row }"><span class="num">{{ row.menuCount ?? '—' }}</span></template>
            </el-table-column>
            <el-table-column label="账号数" width="80" align="right">
              <template #default="{ row }"><span class="num">{{ row.userCount ?? '—' }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="104" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openMenus(row)">分配菜单</el-button>
              </template>
            </el-table-column>
            <template #empty><div style="padding:26px;color:var(--text-sub)">暂无角色</div></template>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 用户信息 -->
    <el-drawer v-model="userDrawer" title="账号信息" size="420px">
      <template v-if="curUser">
        <div class="u-head">
          <img v-if="curUser.avatar" class="ava-lg" :src="curUser.avatar" alt="用户头像" />
          <span v-else class="ava-lg ph">{{ (curUser.realName || curUser.username || 'U')[0] }}</span>
          <div>
            <div class="u-name">{{ curUser.realName }}</div>
            <div class="u-sub">{{ curUser.username }} · {{ curUser.roleName }}</div>
          </div>
        </div>
        <div class="kv" style="margin-top:14px">
          <span class="k">联系电话</span><span>{{ curUser.phone || '—' }}</span>
          <span class="k">所属组织</span><span>{{ curUser.orgName || '—' }}</span>
          <span class="k">数据范围</span><span>{{ scopeText(curUser.dataScope) }}</span>
          <span class="k">账号状态</span><span>{{ Number(curUser.status) === 1 ? '启用' : '停用' }}</span>
          <span class="k">最后登录</span><span class="num">{{ fmtTime(curUser.lastLoginTime) }}</span>
        </div>
      </template>
    </el-drawer>

    <!-- 分配菜单 -->
    <el-drawer v-model="menuDrawer" :title="`分配菜单 · ${curRole?.roleName || ''}`" size="420px">
      <div class="drawer-tip">勾选该角色可访问的菜单；系统管理下的「菜单管理」不对业务角色开放，已隐藏。</div>
      <el-tree ref="treeRef" :data="menuTree" node-key="id" show-checkbox default-expand-all
               :props="{ label: 'label', children: 'children' }" class="menu-tree" />
      <template #footer>
        <div style="display:flex;justify-content:flex-end;gap:8px">
          <el-button @click="menuDrawer = false">取消</el-button>
          <el-button type="primary" @click="saveMenus">保存</el-button>
        </div>
      </template>
    </el-drawer>
  </AdminLayout>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import AdminLayout from '../layouts/AdminLayout.vue'
import { api } from '../api'
import { fmtTime } from '../utils/format'

const tab = ref('user')

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const q = reactive({ keyword: '', page: 1, size: 10 })

const roleLoading = ref(false)
const roles = ref([])

const userDrawer = ref(false)
const curUser = ref(null)

const menuDrawer = ref(false)
const treeRef = ref(null)
const curRole = ref(null)
const menuTree = ref([])
const checkedMap = reactive({})

const SCOPE = { ALL: '全部数据', ORG: '本组织', SELF: '本人', DEPT: '本部门' }
const ROLE_TAG = { ADMIN: 't-red', LEADER: 't-blue', CS: 't-blue', DEPT: 't-amber', CUSTOMER: 't-gray' }

function scopeText(v) {
  return SCOPE[v] || v || '—'
}
function roleTag(code) {
  return ROLE_TAG[code] || 't-gray'
}

async function load() {
  loading.value = true
  try {
    const r = await api.userPage({ keyword: q.keyword || undefined, page: q.page, size: q.size })
    rows.value = r.records || []
    total.value = r.total || 0
  } finally {
    loading.value = false
  }
}
function search() { q.page = 1; load() }
function reset() { q.keyword = ''; q.page = 1; load() }

async function loadRoles() {
  roleLoading.value = true
  try {
    const list = await api.roleList()
    roles.value = (list || []).map((r) => ({
      id: r.id,
      roleCode: r.roleCode || r.role_code,
      roleName: r.roleName || r.role_name,
      dataScope: r.dataScope || r.data_scope,
      remark: r.remark,
      menuCount: r.menuCount ?? r.menu_count,
      userCount: r.userCount ?? r.user_count
    }))
  } finally {
    roleLoading.value = false
  }
}

/** 平铺菜单 → 树；过滤「菜单管理」及其子节点 */
function buildTree(list) {
  const flat = []
  const walk = (arr, pid) => {
    (arr || []).forEach((m) => {
      const id = m.id
      const label = m.name || m.menuName || ''
      const parentId = m.parentId ?? m.parent_id ?? pid ?? 0
      flat.push({
        id, parentId, label,
        path: m.path, sortNo: m.sortNo ?? m.sort_no ?? 0
      })
      if (Array.isArray(m.children) && m.children.length) walk(m.children, id)
    })
  }
  walk(list, 0)

  const hidden = new Set()
  flat.forEach((n) => { if (n.label.includes('菜单管理')) hidden.add(n.id) })
  let grew = true
  while (grew) {
    grew = false
    flat.forEach((n) => {
      if (!hidden.has(n.id) && hidden.has(n.parentId)) { hidden.add(n.id); grew = true }
    })
  }

  const nodes = flat
    .filter((n) => !hidden.has(n.id))
    .map((n) => ({ ...n, children: [] }))
  const map = new Map(nodes.map((n) => [n.id, n]))
  const roots = []
  nodes.forEach((n) => {
    const p = map.get(n.parentId)
    if (p) p.children.push(n)
    else roots.push(n)
  })
  const sortRec = (arr) => {
    arr.sort((a, b) => a.sortNo - b.sortNo)
    arr.forEach((x) => { if (x.children.length) sortRec(x.children) })
  }
  sortRec(roots)
  return roots
}

async function openMenus(role) {
  curRole.value = role
  if (!menuTree.value.length) {
    const list = await api.menuList()
    menuTree.value = buildTree(list)
  }
  menuDrawer.value = true
  setTimeout(() => {
    const saved = checkedMap[role.id] || []
    if (treeRef.value) treeRef.value.setCheckedKeys(saved)
  }, 80)
}

function saveMenus() {
  const keys = treeRef.value ? treeRef.value.getCheckedKeys() : []
  checkedMap[curRole.value.id] = keys
  menuDrawer.value = false
  ElMessage.success(`「${curRole.value.roleName}」菜单权限已保存（演示）`)
}

function openUser(row) {
  curUser.value = row
  userDrawer.value = true
}

function onTab(name) {
  if (name === 'role' && !roles.value.length) loadRoles()
}

function reload() {
  if (tab.value === 'user') load()
  else loadRoles()
}

onMounted(() => {
  load()
  loadRoles()
})
</script>

<style scoped>
.ava { width: 28px; height: 28px; border-radius: 50%; object-fit: cover; border: 1px solid var(--line); }
.ava-ph {
  display: inline-grid; place-items: center; width: 28px; height: 28px; border-radius: 50%;
  background: var(--tint); color: var(--text-sub); font-size: 12px;
}
.cnt-tip { font-size: 12px; color: var(--text-sub); margin-left: 4px; }
.u-head { display: flex; align-items: center; gap: 12px; padding-bottom: 14px; border-bottom: 1px solid var(--line); }
.ava-lg { width: 52px; height: 52px; border-radius: 50%; object-fit: cover; border: 1px solid var(--line); }
.ava-lg.ph { display: grid; place-items: center; background: var(--tint); color: var(--text-sub); font-size: 18px; }
.u-name { font-size: 15px; font-weight: 600; }
.u-sub { font-size: 12px; color: var(--text-sub); margin-top: 2px; }
.drawer-tip { font-size: 12px; color: var(--text-sub); line-height: 19px; background: var(--tint); border-radius: 6px; padding: 8px 10px; margin-bottom: 12px; }
.menu-tree { font-size: 13px; }
:deep(.el-tabs__header) { margin-bottom: 12px; }
</style>
