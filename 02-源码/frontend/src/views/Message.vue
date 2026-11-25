<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>站内消息</h1>
        <div class="desc">待处理、待审核、待验收与时限预警等系统提醒集中在这里，点击消息即可查看并标记已读</div>
      </div>
      <div class="acts">
        <el-button size="small" @click="refreshAll"><el-icon><Refresh /></el-icon>刷新</el-button>
        <el-button size="small" type="primary" :loading="marking" @click="markAll">
          <el-icon><Check /></el-icon>全部标为已读
        </el-button>
      </div>
    </div>

    <div class="card">
      <div class="msg-wrap">
        <!-- 左侧类型分类 -->
        <div class="msg-nav">
          <div v-for="t in types" :key="t.value" class="nav-i" :class="{ on: q.type === t.value }" @click="changeType(t.value)">
            <span>{{ t.label }}</span>
            <span v-if="counts[t.value]" class="cnt">{{ counts[t.value] > 99 ? '99+' : counts[t.value] }}</span>
          </div>
        </div>

        <!-- 右侧消息列表 -->
        <div class="msg-main" v-loading="loading">
          <div class="msg-hd">
            <div>
              <b>{{ curLabel }}</b>
              <span class="sub">共 {{ total }} 条{{ unreadAll ? `，未读 ${unreadAll} 条` : '，暂无未读' }}</span>
            </div>
          </div>

          <div v-for="m in rows" :key="m.id" class="msg" :class="{ unread: !m.isRead }" @click="openMsg(m)">
            <div class="msg-h">
              <span class="tag" :class="msgTag(m.msgType)">{{ m.msgType }}</span>
              <span class="msg-t">{{ m.title }}</span>
              <span class="msg-tm num">{{ fmtTime(String(m.createTime || '').replace('T', ' ')) }}</span>
            </div>
            <div class="msg-c" :class="{ clamp: openId !== m.id }">{{ m.content || '—' }}</div>
            <div v-if="openId === m.id" class="msg-a">
              <span style="font-size:12px;color:var(--text-sub)">消息编号 #{{ m.id }} · {{ m.isRead ? '已读' : '未读' }}</span>
              <el-link v-if="m.linkUrl" type="primary" @click.stop="goLink(m)">前往处理</el-link>
            </div>
          </div>

          <div v-if="!rows.length && !loading" class="empty">当前分类暂无消息</div>

          <div class="pager">
            <el-pagination background layout="total, sizes, prev, pager, next, jumper" :total="total"
                           v-model:current-page="q.page" v-model:page-size="q.size" :page-sizes="[10, 20, 50]"
                           @current-change="load" @size-change="changeSize" />
          </div>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh, Check } from '@element-plus/icons-vue'
import AdminLayout from '../layouts/AdminLayout.vue'
import { api } from '../api'
import { fmtTime } from '../utils/format'

const router = useRouter()
const loading = ref(false)
const marking = ref(false)
const rows = ref([])
const total = ref(0)
const unreadAll = ref(0)
const openId = ref(null)

const types = [
  { label: '全部', value: '' },
  { label: '待处理', value: '待处理' },
  { label: '待审核', value: '待审核' },
  { label: '待验收', value: '待验收' },
  { label: '时限预警', value: '时限预警' },
  { label: '系统通知', value: '系统通知' }
]

const q = reactive({ type: '', page: 1, size: 10 })
const counts = reactive({ '全部': 0, '待处理': 0, '待审核': 0, '待验收': 0, '时限预警': 0, '系统通知': 0 })

const curLabel = computed(() => types.find((t) => t.value === q.type)?.label || '全部')

function msgTag(t) {
  const map = { 待处理: 't-blue', 待审核: 't-amber', 待验收: 't-amber', 时限预警: 't-red', 系统通知: 't-gray' }
  return map[t] || 't-gray'
}

async function load() {
  loading.value = true
  try {
    const p = { page: q.page, size: q.size }
    if (q.type) p.type = q.type
    const r = await api.messagePage(p)
    rows.value = r.records || []
    total.value = r.total || 0
    if (typeof r.unread === 'number') {
      unreadAll.value = r.unread
      counts['全部'] = r.unread
    }
  } finally {
    loading.value = false
  }
}

// 各分类未读数：后端未提供按类型统计，拉取全量后本地归集
async function loadCounts() {
  const r = await api.messagePage({ page: 1, size: 500 })
  Object.keys(counts).forEach((k) => { if (k !== '全部') counts[k] = 0 })
  let all = 0
  ;(r.records || []).forEach((m) => {
    if (!m.isRead) {
      all++
      if (counts[m.msgType] !== undefined) counts[m.msgType]++
    }
  })
  unreadAll.value = typeof r.unread === 'number' ? r.unread : all
  counts['全部'] = unreadAll.value
}

function changeType(v) {
  q.type = v
  q.page = 1
  openId.value = null
  load()
}

function changeSize() {
  q.page = 1
  load()
}

async function refreshAll() {
  await load()
  await loadCounts()
}

async function openMsg(m) {
  if (openId.value === m.id) {
    openId.value = null
    return
  }
  openId.value = m.id
  if (m.isRead) return
  await api.readMessage(m.id)
  m.isRead = 1
  unreadAll.value = Math.max(0, unreadAll.value - 1)
  counts['全部'] = unreadAll.value
  if (counts[m.msgType] !== undefined) counts[m.msgType] = Math.max(0, counts[m.msgType] - 1)
}

function goLink(m) {
  if (m.linkUrl) router.push(m.linkUrl)
}

async function markAll() {
  if (!unreadAll.value) return ElMessage.info('当前没有未读消息')
  marking.value = true
  try {
    await api.readAll()
    ElMessage.success('已将全部消息标记为已读')
    rows.value.forEach((m) => { m.isRead = 1 })
    openId.value = null
    refreshAll()
  } finally {
    marking.value = false
  }
}

onMounted(() => {
  load()
  loadCounts()
})
</script>

<style scoped>
.msg-wrap { display: grid; grid-template-columns: 168px minmax(0, 1fr); gap: 16px; align-items: start; }
.msg-nav { border-right: 1px solid var(--line); padding-right: 12px; }
.nav-i {
  display: flex; align-items: center; justify-content: space-between;
  height: 34px; padding: 0 10px; border-radius: 6px; margin-bottom: 2px;
  font-size: 13px; color: var(--text-sub); cursor: pointer;
}
.nav-i:hover { background: var(--tint); }
.nav-i.on { background: #E6EEF3; color: #2A5B6B; font-weight: 600; }
.nav-i .cnt {
  min-width: 18px; height: 18px; line-height: 18px; padding: 0 5px; border-radius: 9px;
  background: var(--danger); color: #fff; font-size: 11px; text-align: center;
}
.msg-main { min-width: 0; }
.msg-hd { display: flex; align-items: center; justify-content: space-between; padding-bottom: 8px; font-size: 13px; }
.msg-hd .sub { font-size: 12px; color: var(--text-sub); margin-left: 10px; }
.msg {
  border-left: 3px solid transparent; border-bottom: 1px solid var(--line);
  padding: 9px 12px; cursor: pointer;
}
.msg:hover { background: #F7F9FA; }
.msg.unread { border-left-color: var(--secondary); background: #FAFCFD; }
.msg-h { display: flex; align-items: center; gap: 8px; }
.msg-t { font-size: 13px; flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.msg.unread .msg-t { font-weight: 600; }
.msg-tm { flex: 0 0 auto; font-size: 11px; color: var(--text-sub); }
.msg-c { font-size: 12px; color: var(--text-sub); line-height: 20px; margin-top: 4px; }
.msg-c.clamp { display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.msg-a { display: flex; align-items: center; gap: 12px; margin-top: 6px; }
.empty { padding: 40px 0; text-align: center; color: var(--text-sub); font-size: 13px; }
@media (max-width: 768px) {
  .msg-wrap { grid-template-columns: 1fr; }
  .msg-nav { border-right: none; border-bottom: 1px solid var(--line); padding: 0 0 8px; }
}
</style>
