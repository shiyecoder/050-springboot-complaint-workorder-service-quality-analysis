<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>客诉受理台</h1>
        <div class="desc">三栏工作区：左侧取待受理投诉，中部核实事实与证据，右侧完成受理判定并分派到责任部门</div>
      </div>
      <div class="acts">
        <el-button size="small" @click="refreshAll">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
        <el-button size="small" type="primary" @click="$router.push('/intake/new')">
          <el-icon><Plus /></el-icon>新建投诉登记
        </el-button>
      </div>
    </div>

    <!-- 今日受理概览：只统计与受理动作直接相关的四个数字 -->
    <div class="kpis kpis-4">
      <div class="kpi">
        <div class="lb"><span>今日新收</span><span>件</span></div>
        <div class="vl num">{{ stats.today }}</div>
        <div class="ft">今日 00:00 之后登记的投诉</div>
      </div>
      <div class="kpi" :class="{ warn: stats.pending > 0 }">
        <div class="lb"><span>待受理</span><span>件</span></div>
        <div class="vl num">{{ stats.pending }}</div>
        <div class="ft">尚未核实分派，需尽快处理</div>
      </div>
      <div class="kpi" :class="{ warn: stats.dup > 0 }">
        <div class="lb"><span>重复疑似</span><span>件</span></div>
        <div class="vl num">{{ stats.dup }}</div>
        <div class="ft">同一对象短期内重复投诉</div>
      </div>
      <div class="kpi">
        <div class="lb"><span>无效工单</span><span>件</span></div>
        <div class="vl num">{{ stats.invalid }}</div>
        <div class="ft">经核实不成立的投诉</div>
      </div>
    </div>

    <div class="desk">
      <!-- 左栏：待受理队列 -->
      <div class="card queue-card">
        <div class="card-h">
          <h2>待受理队列</h2>
          <span class="sub">{{ queueTotal }} 条</span>
        </div>
        <el-input v-model="q.keyword" size="small" placeholder="编号 / 客户 / 标题" clearable
                  @keyup.enter="searchQueue" @clear="searchQueue">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>

        <div class="qlist" v-loading="loadingQueue">
          <div v-for="r in queue" :key="r.id" class="qitem" :class="{ on: cur && cur.id === r.id }"
               @click="pick(r)">
            <div class="q-top">
              <span class="num q-no">{{ r.ticketNo }}</span>
              <span class="tag" :class="levelTag(r.levelCode)">{{ r.levelCode }}</span>
            </div>
            <div class="q-title">{{ r.title }}</div>
            <div class="q-meta">
              <span>{{ r.customerName || '—' }}</span>
              <span>{{ r.channel || '—' }}</span>
              <span class="num">{{ fmtTime(r.createTime) }}</span>
            </div>
            <div class="q-flags">
              <span v-if="r.overdueFlag" class="tag t-red">响应超期</span>
              <span v-else-if="r.duplicateFlag" class="tag t-amber">疑似重复</span>
              <span v-else class="tag t-gray">{{ r.categoryName || '未分类' }}</span>
            </div>
          </div>

          <div v-if="!loadingQueue && !queue.length" class="empty">
            <div class="empty-t">当前没有待受理的投诉</div>
            <div class="empty-d">新的投诉会实时进入这里；也可清空关键词查看全部，或直接新建投诉登记</div>
          </div>
        </div>

        <div class="pager" style="justify-content:center">
          <el-pagination background small layout="prev, pager, next" :total="queueTotal"
                         :page-size="q.size" v-model:current-page="q.page" @current-change="loadQueue" />
        </div>
      </div>

      <!-- 中栏：投诉事实与证据 -->
      <div class="card mid-card">
        <div class="card-h">
          <h2>投诉信息核实</h2>
          <span class="sub" v-if="cur">{{ cur.ticketNo }}</span>
        </div>

        <div v-if="!cur" class="empty big">
          <div class="empty-t">请从左侧队列选择一条投诉</div>
          <div class="empty-d">选中后可查看客户、对象、类别、诉求与证据图片，并在右侧完成受理分派</div>
        </div>

        <div v-else v-loading="loadingDetail">
          <div class="mid-title">{{ detail?.ticket?.title || cur.title }}</div>
          <div class="mid-tags">
            <span class="tag" :class="statusTag(detail?.ticket?.status || cur.status)">
              {{ detail?.ticket?.status || cur.status }}
            </span>
            <span class="tag" :class="levelTag(cur.levelCode)">{{ cur.levelCode }}</span>
            <span class="tag t-blue">{{ cur.categoryName || '—' }}</span>
            <span class="tag t-gray">{{ cur.problemType || '—' }}</span>
          </div>

          <div class="kv2">
            <div class="k">客户</div>
            <div>{{ detail?.ticket?.customerName || cur.customerName || '—' }}</div>
            <div class="k">联系电话</div>
            <div class="num">{{ detail?.ticket?.phone || cur.phone || '—' }}</div>
            <div class="k">投诉对象</div>
            <div>{{ detail?.ticket?.objectName || cur.objectName || '—' }}</div>
            <div class="k">受理渠道</div>
            <div>{{ detail?.ticket?.channel || cur.channel || '—' }}</div>
            <div class="k">发生时间</div>
            <div class="num">{{ fmtTime(detail?.ticket?.occurTime) }}</div>
            <div class="k">发生地点</div>
            <div>{{ detail?.ticket?.occurPlace || '—' }}</div>
            <div class="k">要求响应</div>
            <div class="num">{{ fmtTime(detail?.ticket?.responseDeadline) }}</div>
            <div class="k">要求完成</div>
            <div class="num">{{ fmtTime(detail?.ticket?.deadlineTime || cur.deadlineTime) }}</div>
          </div>

          <div class="blk-title">客户诉求</div>
          <div class="desc-block">{{ detail?.ticket?.demand || cur.demand || '客户未填写明确诉求' }}</div>

          <div class="blk-title">投诉内容</div>
          <div class="desc-block">{{ detail?.ticket?.content || '暂无详细描述' }}</div>

          <div class="blk-title" v-if="evidence.length">证据图片</div>
          <div class="evi" v-if="evidence.length">
            <el-image v-for="(src, i) in evidence" :key="src" class="evi-img" :src="src"
                      :preview-src-list="evidence" :initial-index="i" fit="cover" preview-teleported />
          </div>
        </div>
      </div>

      <!-- 右栏：受理动作 -->
      <div class="card act-card">
        <div class="card-h">
          <h2>受理动作</h2>
          <span class="sub">核实后分派</span>
        </div>

        <div v-if="!cur" class="empty">
          <div class="empty-t">尚未选择投诉</div>
          <div class="empty-d">选择左侧投诉后即可登记核实结论</div>
        </div>

        <el-form v-else label-position="top" size="small">
          <el-form-item label="核实结果">
            <el-select v-model="form.verify" placeholder="请选择核实结论" style="width:100%">
              <el-option v-for="v in verifyOptions" :key="v" :label="v" :value="v" />
            </el-select>
          </el-form-item>

          <el-form-item label="工单判定">
            <div class="switch-row">
              <span>判定为重复投诉</span>
              <el-switch v-model="form.duplicate" />
            </div>
            <div class="switch-row">
              <span>判定为无效投诉</span>
              <el-switch v-model="form.invalid" />
            </div>
          </el-form-item>

          <el-form-item label="分派部门">
            <el-select v-model="form.deptId" placeholder="请选择责任部门" style="width:100%" @change="onDeptChange">
              <el-option v-for="d in opt.depts" :key="d.id" :label="d.orgName" :value="d.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="处理人">
            <el-select v-model="form.handlerId" placeholder="请选择处理人" style="width:100%"
                       :disabled="!form.deptId" filterable>
              <el-option v-for="h in handlersOfDept" :key="h.id" :label="`${h.realName}（${h.orgName}）`" :value="h.id" />
            </el-select>
            <div class="hint" v-if="form.deptId && !handlersOfDept.length">该部门暂无可选处理人，请更换部门</div>
          </el-form-item>

          <el-form-item label="受理备注">
            <el-input v-model="form.note" type="textarea" :rows="4" maxlength="200" show-word-limit
                      placeholder="记录核实过程、客户表述与分派依据" />
          </el-form-item>

          <el-button type="primary" style="width:100%" :loading="submitting" @click="submit">
            受理并分派
          </el-button>
          <div class="hint" style="margin-top:8px">
            分派后工单状态变为「已分派」，处理人可在 SLA 工单看板接单；判定结论会写入流转记录留痕。
          </div>
        </el-form>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus, Refresh } from '@element-plus/icons-vue'
import AdminLayout from '../layouts/AdminLayout.vue'
import { api } from '../api'
import { fmtTime, fmtDate, statusTag, levelTag } from '../utils/format'

const opt = reactive({ categories: [], levels: [], depts: [], channels: [], statuses: [], handlers: [] })
const queue = ref([])
const queueTotal = ref(0)
const loadingQueue = ref(false)
const loadingDetail = ref(false)
const submitting = ref(false)
const cur = ref(null)
const detail = ref(null)

const q = reactive({ keyword: '', page: 1, size: 10 })
const stats = reactive({ today: 0, pending: 0, dup: 0, invalid: 0 })

// 核实结论：与需求文档「客服核实联系方式、关联对象和事实概况」对齐
const verifyOptions = ['情况属实', '部分属实', '需补充材料', '无法联系客户', '情况不属实']
const form = reactive({ verify: '情况属实', duplicate: false, invalid: false, deptId: null, handlerId: null, note: '' })

const handlersOfDept = computed(() =>
  (opt.handlers || []).filter((h) => Number(h.orgId) === Number(form.deptId))
)

// 证据图：主图 + 附件中的图片，去重后统一预览
const evidence = computed(() => {
  const list = []
  const t = detail.value?.ticket
  if (t?.coverImage) list.push(t.coverImage)
  ;(detail.value?.attachments || []).forEach((a) => {
    if (a.filePath && /\.(jpg|jpeg|png|webp|gif)$/i.test(a.filePath) && !list.includes(a.filePath)) {
      list.push(a.filePath)
    }
  })
  return list
})

async function loadQueue() {
  loadingQueue.value = true
  try {
    const r = await api.ticketPage({ status: '待受理', keyword: q.keyword, page: q.page, size: q.size })
    queue.value = r.records || []
    queueTotal.value = r.total || 0
    // 首次进入或当前选中项已不在队列中时，自动选中第一条，减少一次点击
    if (!cur.value || !queue.value.some((x) => x.id === cur.value.id)) {
      if (queue.value.length) pick(queue.value[0])
      else { cur.value = null; detail.value = null }
    }
  } finally {
    loadingQueue.value = false
  }
}

function searchQueue() {
  q.page = 1
  loadQueue()
}

async function pick(row) {
  cur.value = row
  loadingDetail.value = true
  try {
    detail.value = await api.ticketDetail(row.id)
    // 带出分类默认归属部门，减少客服重复选择
    const t = detail.value?.ticket || {}
    if (!form.deptId && t.deptId) form.deptId = t.deptId
  } catch (e) {
    detail.value = null
  } finally {
    loadingDetail.value = false
  }
}

function onDeptChange() {
  form.handlerId = null
}

async function loadStats() {
  const today = fmtDate(new Date())
  const [todayRes, pendingRes] = await Promise.all([
    api.ticketPage({ dateFrom: today, dateTo: today, page: 1, size: 1 }),
    api.ticketPage({ status: '待受理', page: 1, size: 1 })
  ])
  stats.today = todayRes.total || 0
  stats.pending = pendingRes.total || 0
  await scanFlags()
}

// 重复/无效标记没有独立查询参数，这里按页扫描全量台账后汇总（最多 10 页，足够覆盖演示数据）
async function scanFlags() {
  let page = 1
  let dup = 0
  let invalid = 0
  for (let guard = 0; guard < 10; guard++) {
    const r = await api.ticketPage({ page, size: 100 })
    const recs = r.records || []
    recs.forEach((t) => {
      if (t.duplicateFlag) dup++
      if (t.invalidFlag) invalid++
    })
    if (recs.length < 100 || page * 100 >= (r.total || 0)) break
    page++
  }
  stats.dup = dup
  stats.invalid = invalid
}

function resetForm() {
  form.verify = '情况属实'
  form.duplicate = false
  form.invalid = false
  form.deptId = null
  form.handlerId = null
  form.note = ''
}

async function submit() {
  if (!cur.value) return
  if (!form.verify) {
    ElMessage.warning('请先选择核实结果')
    return
  }
  if (!form.deptId) {
    ElMessage.warning('请选择分派部门')
    return
  }
  if (!form.handlerId) {
    ElMessage.warning('请选择处理人')
    return
  }
  // 判定结论后端无独立字段，随受理备注写入流转记录，保证可追溯
  const parts = [`核实结果：${form.verify}`]
  if (form.duplicate) parts.push('判定：重复投诉')
  if (form.invalid) parts.push('判定：无效投诉')
  if (form.note) parts.push(`受理备注：${form.note}`)
  const reason = parts.join('；')

  submitting.value = true
  try {
    // 状态机：待受理 → 已分派（同时落工单、写分派记录、给处理人发消息）
    await api.transition(cur.value.id, {
      toStatus: '已分派',
      reason,
      deptId: form.deptId,
      handlerId: form.handlerId
    })
    ElMessage.success(`已受理并分派：${cur.value.ticketNo}`)
    cur.value = null
    detail.value = null
    resetForm()
    await Promise.all([loadQueue(), loadStats()])
  } finally {
    submitting.value = false
  }
}

async function refreshAll() {
  await Promise.all([loadQueue(), loadStats()])
}

onMounted(async () => {
  const o = await api.options()
  Object.assign(opt, o)
  await refreshAll()
})
</script>

<style scoped>
.kpis-4 { grid-template-columns: repeat(4, 1fr); }
.desk { display: grid; grid-template-columns: 300px minmax(0, 1fr) 330px; gap: 14px; align-items: start; }
.queue-card, .mid-card, .act-card { margin-bottom: 0; }

.qlist { margin-top: 8px; max-height: 560px; overflow-y: auto; display: flex; flex-direction: column; gap: 8px; }
.qitem { border: 1px solid var(--line); border-radius: 6px; padding: 8px 10px; cursor: pointer; background: #fff; transition: .15s; }
.qitem:hover { border-color: var(--secondary); }
.qitem.on { border-color: var(--secondary); background: #F3F7F9; }
.q-top { display: flex; align-items: center; justify-content: space-between; }
.q-no { font-size: 12px; color: var(--text-sub); }
.q-title { font-size: 13px; font-weight: 600; margin: 4px 0; line-height: 20px; }
.q-meta { display: flex; gap: 10px; font-size: 11px; color: var(--text-sub); }
.q-flags { margin-top: 6px; }

.mid-title { font-size: 16px; font-weight: 600; line-height: 24px; }
.mid-tags { display: flex; flex-wrap: wrap; gap: 6px; margin: 8px 0 12px; }
.kv2 { display: grid; grid-template-columns: 78px 1fr 78px 1fr; gap: 8px 10px; font-size: 13px; }
.kv2 .k { color: var(--text-sub); }
.blk-title { font-size: 13px; font-weight: 600; margin: 14px 0 6px; }
.evi { display: flex; gap: 8px; flex-wrap: wrap; }
.evi-img { width: 96px; height: 72px; border-radius: 6px; border: 1px solid var(--line); }

.switch-row { display: flex; align-items: center; justify-content: space-between; width: 100%; font-size: 13px; padding: 2px 0; }
.hint { font-size: 11px; color: var(--text-sub); line-height: 18px; }

.empty { text-align: center; padding: 22px 10px; }
.empty.big { padding: 80px 20px; }
.empty-t { font-size: 13px; font-weight: 600; }
.empty-d { font-size: 12px; color: var(--text-sub); margin-top: 4px; line-height: 20px; }

@media (max-width: 1440px) {
  .desk { grid-template-columns: 260px minmax(0, 1fr) 300px; }
}
@media (max-width: 1200px) {
  .desk { grid-template-columns: 1fr; }
  .kpis-4 { grid-template-columns: repeat(2, 1fr); }
  .qlist { max-height: 320px; }
}
</style>
