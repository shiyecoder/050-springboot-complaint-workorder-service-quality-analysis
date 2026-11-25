<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>时限预警中心</h1>
        <div class="desc">按 SLA 规则自动扫描工单时效，提前预警、超时升级，处理人须及时响应</div>
      </div>
      <div class="acts">
        <el-button size="small" @click="refreshAll"><el-icon><Refresh /></el-icon>刷新</el-button>
        <el-button size="small" type="primary" :loading="scanning" @click="scan"><el-icon><Bell /></el-icon>立即扫描</el-button>
      </div>
    </div>

    <div class="kpis kpis-4">
      <div class="kpi bad">
        <div class="lb"><span>未处理</span><span>条</span></div>
        <div class="vl num">{{ stats.pending }}</div>
        <div class="ft">需人工确认处置的预警</div>
      </div>
      <div class="kpi bad">
        <div class="lb"><span>严重预警</span><span>条</span></div>
        <div class="vl num">{{ stats.severe }}</div>
        <div class="ft">已超期或即将超期的高风险工单</div>
      </div>
      <div class="kpi warn">
        <div class="lb"><span>提醒预警</span><span>条</span></div>
        <div class="vl num">{{ stats.remind }}</div>
        <div class="ft">剩余时限不足的提示性预警</div>
      </div>
      <div class="kpi">
        <div class="lb"><span>今日新增</span><span>条</span></div>
        <div class="vl num">{{ stats.today }}</div>
        <div class="ft">今日扫描生成的预警</div>
      </div>
    </div>

    <div class="card tight">
      <div class="tip">
        <b>预警口径</b>
        <span>即将超时＝剩余时限不足总时限的 20%；处理超时＝已超过要求完成时间；长期未更新＝连续 48 小时无处理进展。</span>
      </div>
    </div>

    <div class="card">
      <div class="filters">
        <el-select v-model="q.type" placeholder="全部预警类型" clearable @change="search">
          <el-option label="即将超时" value="即将超时" />
          <el-option label="处理超时" value="处理超时" />
          <el-option label="长期未更新" value="长期未更新" />
        </el-select>
        <el-select v-model="q.level" placeholder="全部级别" clearable @change="search">
          <el-option label="严重" value="严重" />
          <el-option label="提醒" value="提醒" />
        </el-select>
        <el-select v-model="q.status" placeholder="全部处理状态" clearable @change="search">
          <el-option label="未处理" value="未处理" />
          <el-option label="已处理" value="已处理" />
        </el-select>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" style="width:100%">
        <el-table-column label="工单号" width="160">
          <template #default="{ row }">
            <el-link type="primary" @click="goTicket(row)">{{ row.orderNo || `工单 #${row.ticketId}` }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="级别" width="76">
          <template #default="{ row }">
            <span class="tag" :class="levelTag(row.levelCode)">{{ row.levelCode || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="预警类型" width="104">
          <template #default="{ row }">
            <span class="tag" :class="typeTag(row.warnType)">{{ row.warnType }}</span>
          </template>
        </el-table-column>
        <el-table-column label="预警等级" width="88">
          <template #default="{ row }">
            <span class="tag" :class="row.warnLevel === '严重' ? 't-red' : 't-amber'">{{ row.warnLevel }}</span>
          </template>
        </el-table-column>
        <el-table-column label="预警时间" width="140">
          <template #default="{ row }"><span class="num">{{ fmtTime(row.warnTime) }}</span></template>
        </el-table-column>
        <el-table-column label="要求完成" width="140">
          <template #default="{ row }"><span class="num">{{ fmtTime(row.deadlineTime) }}</span></template>
        </el-table-column>
        <el-table-column label="剩余 / 超时" width="130">
          <template #default="{ row }">
            <span class="num" :style="{ color: Number(row.gapMinutes) < 0 ? 'var(--danger)' : 'var(--success)' }">
              {{ gapText(row.gapMinutes) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="currentHandlerName" label="当前处理人" width="100">
          <template #default="{ row }">{{ row.currentHandlerName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="deptName" label="责任部门" min-width="110" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><span class="tag" :class="statusTag(row.status)">{{ row.status }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === '未处理'" link type="primary" @click="open(row)">标记已处理</el-button>
            <el-button link type="primary" @click="goTicket(row)">跳转工单</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div style="padding:26px;color:var(--text-sub)">当前筛选条件下暂无预警，可点击「立即扫描」重新生成</div>
        </template>
      </el-table>

      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next, jumper" :total="total"
                       v-model:current-page="q.page" v-model:page-size="q.size" :page-sizes="[10, 20, 50]"
                       @current-change="load" @size-change="search" />
      </div>
    </div>

    <!-- 处理预警 -->
    <el-dialog v-model="dlg" title="标记预警已处理" width="520px">
      <div class="sum">
        <div class="sum-h">
          <b>{{ cur?.orderNo || '—' }}</b>
          <span class="tag" :class="cur?.warnLevel === '严重' ? 't-red' : 't-amber'">{{ cur?.warnLevel }}</span>
          <span class="tag" :class="typeTag(cur?.warnType)">{{ cur?.warnType }}</span>
        </div>
        <div class="sum-b">{{ cur?.title || '—' }}</div>
        <div class="sum-b">要求完成：{{ fmtTime(cur?.deadlineTime) }} · {{ gapText(cur?.gapMinutes) }}</div>
      </div>

      <el-form label-width="92px" style="margin-top:12px">
        <el-form-item label="处理说明 *">
          <el-input v-model="fm.handleNote" type="textarea" :rows="3" maxlength="200" show-word-limit
                    placeholder="如：已电话催办责任人、已协调配件、已升级至部门负责人" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确认已处理</el-button>
      </template>
    </el-dialog>
  </AdminLayout>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh, Bell } from '@element-plus/icons-vue'
import AdminLayout from '../layouts/AdminLayout.vue'
import { api } from '../api'
import { fmtTime, fmtDate, statusTag, levelTag } from '../utils/format'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const scanning = ref(false)
const rows = ref([])
const total = ref(0)
const dlg = ref(false)
const cur = ref(null)

const q = reactive({ type: '', level: '', status: '', page: 1, size: 10 })
const stats = reactive({ pending: 0, severe: 0, remind: 0, today: 0 })
const fm = reactive({ handleNote: '' })

function typeTag(t) {
  if (t === '处理超时') return 't-red'
  if (t === '即将超时') return 't-amber'
  return 't-blue'
}

// 分钟差转「x小时y分」，负数表示已超期
function gapText(min) {
  if (min === null || min === undefined || min === '') return '—'
  const n = Number(min)
  if (isNaN(n)) return '—'
  const m = Math.abs(n)
  const day = Math.floor(m / 1440)
  const hour = Math.floor((m % 1440) / 60)
  const mi = m % 60
  const s = day > 0 ? `${day}天${hour}小时` : hour > 0 ? `${hour}小时${mi}分` : `${mi}分`
  return (n < 0 ? '超期 ' : '剩余 ') + s
}

async function load() {
  loading.value = true
  try {
    const p = { page: q.page, size: q.size }
    if (q.type) p.type = q.type
    if (q.status) p.status = q.status
    if (q.level) {
      // 后端未支持按预警等级筛选，改为拉取全量后前端过滤并本地分页
      p.page = 1
      p.size = 500
    }
    const r = await api.warningPage(p)
    let list = r.records || []
    if (q.level) {
      list = list.filter((x) => x.warnLevel === q.level)
      total.value = list.length
      const start = (q.page - 1) * q.size
      rows.value = list.slice(start, start + q.size)
    } else {
      rows.value = list
      total.value = r.total || 0
    }
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  const [all, pending] = await Promise.all([
    api.warningPage({ page: 1, size: 500 }),
    api.warningPage({ status: '未处理', page: 1, size: 1 })
  ])
  const recs = all.records || []
  stats.pending = pending.total || 0
  stats.severe = recs.filter((r) => r.warnLevel === '严重').length
  stats.remind = recs.filter((r) => r.warnLevel === '提醒').length
  const today = fmtDate(new Date())
  stats.today = recs.filter((r) => String(r.warnTime || '').slice(0, 10) === today).length
}

function search() {
  q.page = 1
  load()
}

function reset() {
  Object.assign(q, { type: '', level: '', status: '', page: 1 })
  load()
}

function refreshAll() {
  load()
  loadStats()
}

function goTicket(row) {
  router.push(`/ticket/detail/${row.ticketId}`)
}

function open(row) {
  cur.value = row
  fm.handleNote = ''
  dlg.value = true
}

async function submit() {
  if (!fm.handleNote.trim()) return ElMessage.warning('请填写处理说明')
  saving.value = true
  try {
    await api.handleWarning(cur.value.id, { handleNote: fm.handleNote.trim() })
    ElMessage.success('预警已标记为已处理')
    dlg.value = false
    refreshAll()
  } finally {
    saving.value = false
  }
}

async function scan() {
  scanning.value = true
  try {
    const r = await api.scanWarning()
    ElMessage.success(`扫描完成，新增 ${r?.created ?? 0} 条预警`)
    refreshAll()
  } finally {
    scanning.value = false
  }
}

onMounted(() => {
  load()
  loadStats()
})
</script>

<style scoped>
.kpis-4 { grid-template-columns: repeat(4, 1fr); }
.tip {
  display: flex; gap: 10px; align-items: baseline; font-size: 12px; color: var(--text-sub);
  border-left: 3px solid var(--warning); padding: 2px 0 2px 10px; line-height: 20px;
}
.tip b { color: var(--warning); white-space: nowrap; }
.sum { background: var(--tint); border-radius: 6px; padding: 10px 12px; }
.sum-h { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.sum-b { font-size: 12px; color: var(--text-sub); margin-top: 4px; }
@media (max-width: 768px) {
  .kpis-4 { grid-template-columns: repeat(2, 1fr); }
}
</style>
