<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>重开记录</h1>
        <div class="desc">同一问题再次发生时登记重开申请，受理后自动生成新的处理工单并累计重开次数</div>
      </div>
      <div class="acts">
        <el-button size="small" @click="refreshAll"><el-icon><Refresh /></el-icon>刷新</el-button>
        <el-button size="small" type="primary" @click="$router.push('/ticket/list')">投诉台账</el-button>
      </div>
    </div>

    <div class="kpis kpis-4">
      <div class="kpi">
        <div class="lb"><span>重开总数</span><span>条</span></div>
        <div class="vl num">{{ stats.total }}</div>
        <div class="ft">累计重开申请记录</div>
      </div>
      <div class="kpi" :class="{ warn: stats.pending > 0 }">
        <div class="lb"><span>待处理</span><span>条</span></div>
        <div class="vl num">{{ stats.pending }}</div>
        <div class="ft">等待审核受理的重开申请</div>
      </div>
      <div class="kpi good">
        <div class="lb"><span>已受理</span><span>条</span></div>
        <div class="vl num">{{ stats.accepted }}</div>
        <div class="ft">已生成新的处理工单</div>
      </div>
    </div>

    <div class="card">
      <div class="filters">
        <el-select v-model="q.status" placeholder="全部状态" clearable @change="search">
          <el-option label="待处理" value="待处理" />
          <el-option label="已受理" value="已受理" />
          <el-option label="已驳回" value="已驳回" />
        </el-select>
        <el-input v-model="q.keyword" placeholder="重开编号 / 工单号 / 标题（本页内过滤）" clearable>
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="view" v-loading="loading" style="width:100%">
        <el-table-column prop="reopenNo" label="重开编号" width="110" />
        <el-table-column label="原工单" width="150">
          <template #default="{ row }">
            <el-link type="primary" @click="$router.push(`/ticket/detail/${row.ticketId}`)">{{ row.ticketNo || `#${row.ticketId}` }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="级别" width="76">
          <template #default="{ row }">
            <span class="tag" :class="levelTag(row.levelCode)">{{ row.levelCode || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="reopenReason" label="重开原因" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.reopenReason || '—' }}</template>
        </el-table-column>
        <el-table-column label="重开次数" width="90" align="right">
          <template #default="{ row }"><span class="num">{{ row.reopenCount || 0 }} 次</span></template>
        </el-table-column>
        <el-table-column prop="applicant" label="申请人" width="90">
          <template #default="{ row }">{{ row.applicant || '—' }}</template>
        </el-table-column>
        <el-table-column label="申请时间" width="140">
          <template #default="{ row }"><span class="num">{{ fmtTime(row.applyTime) }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="92">
          <template #default="{ row }"><span class="tag" :class="statusTag(row.status)">{{ row.status }}</span></template>
        </el-table-column>
        <el-table-column label="处理人" width="86">
          <template #default="{ row }">{{ row.handlerId ? `#${row.handlerId}` : '—' }}</template>
        </el-table-column>
        <el-table-column label="处理时间" width="140">
          <template #default="{ row }"><span class="num">{{ fmtTime(row.handleTime) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === '待处理'">
              <el-button link type="primary" @click="open(row, '已受理')">受理</el-button>
              <el-button link type="primary" @click="open(row, '已驳回')">驳回</el-button>
            </template>
            <el-tooltip v-else-if="row.handleNote" :content="row.handleNote" placement="top">
              <el-button link type="primary">处理说明</el-button>
            </el-tooltip>
            <span v-else style="color:var(--text-sub)">—</span>
          </template>
        </el-table-column>
        <template #empty>
          <div style="padding:26px;color:var(--text-sub)">暂无重开记录，客户反馈问题复发时可在工单详情发起重开</div>
        </template>
      </el-table>

      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next, jumper" :total="total"
                       v-model:current-page="q.page" v-model:page-size="q.size" :page-sizes="[10, 20, 50]"
                       @current-change="load" @size-change="search" />
      </div>
    </div>

    <!-- 受理 / 驳回 -->
    <el-dialog v-model="dlg" :title="fm.status === '已受理' ? '受理重开申请' : '驳回重开申请'" width="540px">
      <div class="sum">
        <div class="sum-h">
          <b>{{ cur?.reopenNo || '—' }}</b>
          <span class="tag" :class="levelTag(cur?.levelCode)">{{ cur?.levelCode || '—' }}</span>
        </div>
        <div class="sum-b">{{ cur?.ticketNo }} · {{ cur?.title }}</div>
        <div class="sum-b">原因：{{ cur?.reopenReason || '—' }}</div>
      </div>

      <el-form label-width="92px" style="margin-top:12px">
        <el-form-item label="处理结论 *">
          <el-radio-group v-model="fm.status">
            <el-radio-button value="已受理">受理</el-radio-button>
            <el-radio-button value="已驳回">驳回</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="处理说明 *">
          <el-input v-model="fm.handleNote" type="textarea" :rows="3" maxlength="200" show-word-limit
                    :placeholder="fm.status === '已受理' ? '说明核实结论与重开后的处理安排' : '说明不予受理的依据'" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">提交</el-button>
      </template>
    </el-dialog>
  </AdminLayout>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import AdminLayout from '../layouts/AdminLayout.vue'
import { api } from '../api'
import { fmtTime, statusTag, levelTag } from '../utils/format'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const total = ref(0)
const dlg = ref(false)
const cur = ref(null)

const q = reactive({ status: '', keyword: '', page: 1, size: 10 })
const stats = reactive({ total: 0, pending: 0, accepted: 0 })
const fm = reactive({ status: '已受理', handleNote: '' })

const view = computed(() => {
  const kw = q.keyword.trim()
  if (!kw) return rows.value
  return rows.value.filter((r) => `${r.reopenNo || ''}${r.ticketNo || ''}${r.title || ''}`.includes(kw))
})

async function load() {
  loading.value = true
  try {
    const p = { page: q.page, size: q.size }
    if (q.status) p.status = q.status
    const r = await api.reopenPage(p)
    rows.value = r.records || []
    total.value = r.total || 0
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  const [all, pending, accepted] = await Promise.all([
    api.reopenPage({ page: 1, size: 1 }),
    api.reopenPage({ status: '待处理', page: 1, size: 1 }),
    api.reopenPage({ status: '已受理', page: 1, size: 1 })
  ])
  stats.total = all.total || 0
  stats.pending = pending.total || 0
  stats.accepted = accepted.total || 0
}

function search() {
  q.page = 1
  load()
}

function reset() {
  Object.assign(q, { status: '', keyword: '', page: 1 })
  load()
}

function refreshAll() {
  load()
  loadStats()
}

function open(row, status) {
  cur.value = row
  Object.assign(fm, { status, handleNote: '' })
  dlg.value = true
}

async function submit() {
  if (!fm.handleNote.trim()) return ElMessage.warning('请填写处理说明')
  saving.value = true
  try {
    await api.handleReopen(cur.value.id, { status: fm.status, handleNote: fm.handleNote.trim() })
    ElMessage.success(fm.status === '已受理' ? '已受理，重开工单已生成' : '已驳回该重开申请')
    dlg.value = false
    refreshAll()
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  load()
  loadStats()
})
</script>

<style scoped>
.kpis-4 { grid-template-columns: repeat(4, 1fr); }
.sum { background: var(--tint); border-radius: 6px; padding: 10px 12px; }
.sum-h { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.sum-b { font-size: 12px; color: var(--text-sub); margin-top: 4px; }
@media (max-width: 768px) {
  .kpis-4 { grid-template-columns: repeat(2, 1fr); }
}
</style>
