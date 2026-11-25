<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>回访与评价</h1>
        <div class="desc">对已确认结案的工单开展回访，记录问题是否真正解决与客户满意度</div>
      </div>
      <div class="acts">
        <el-button size="small" @click="refreshAll"><el-icon><Refresh /></el-icon>刷新</el-button>
        <el-button size="small" type="primary" @click="$router.push('/confirm')">去客户确认</el-button>
      </div>
    </div>

    <div class="kpis kpis-4">
      <div class="kpi" :class="{ warn: stats.waiting > 0 }">
        <div class="lb"><span>待回访</span><span>单</span></div>
        <div class="vl num">{{ stats.waiting }}</div>
        <div class="ft">工单状态为「待回访」</div>
      </div>
      <div class="kpi">
        <div class="lb"><span>已回访</span><span>单</span></div>
        <div class="vl num">{{ stats.done }}</div>
        <div class="ft">累计回访记录数</div>
      </div>
      <div class="kpi good">
        <div class="lb"><span>解决率</span><span>%</span></div>
        <div class="vl num">{{ stats.solveRate }}</div>
        <div class="ft">回访中问题已解决占比</div>
      </div>
      <div class="kpi">
        <div class="lb"><span>平均满意度</span><span>分</span></div>
        <div class="vl num">{{ stats.avgScore }}</div>
        <div class="ft">满分 5 分</div>
      </div>
    </div>

    <div class="card">
      <div class="filters">
        <el-select v-model="q.solved" placeholder="全部回访结果" clearable @change="search">
          <el-option label="已解决" value="1" />
          <el-option label="未解决" value="0" />
        </el-select>
        <el-input v-model="q.keyword" placeholder="回访编号 / 工单号 / 客户（本页内过滤）" clearable>
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="view" v-loading="loading" style="width:100%">
        <el-table-column prop="revisitNo" label="回访编号" width="110" />
        <el-table-column label="工单" width="150">
          <template #default="{ row }">
            <el-link type="primary" @click="$router.push(`/ticket/detail/${row.ticketId}`)">{{ row.ticketNo || `#${row.ticketId}` }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="customerName" label="客户" width="90">
          <template #default="{ row }">{{ row.customerName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="revisitType" label="回访方式" width="90" />
        <el-table-column label="是否解决" width="90">
          <template #default="{ row }">
            <span class="tag" :class="row.solvedFlag ? 't-green' : 't-red'">{{ row.solvedFlag ? '是' : '否' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="满意度" width="150">
          <template #default="{ row }">
            <el-rate :model-value="row.satisfaction || 0" disabled size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="opinion" label="回访意见" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.opinion || '—' }}</template>
        </el-table-column>
        <el-table-column prop="revisitorName" label="回访人" width="90">
          <template #default="{ row }">{{ row.revisitorName || '—' }}</template>
        </el-table-column>
        <el-table-column label="回访时长" width="90" align="right">
          <template #default="{ row }"><span class="num">{{ row.durationMin ? `${row.durationMin} 分` : '—' }}</span></template>
        </el-table-column>
        <el-table-column label="回访时间" width="140">
          <template #default="{ row }"><span class="num">{{ fmtTime(row.revisitTime) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="open(row)">记录回访</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div style="padding:26px;color:var(--text-sub)">暂无回访记录，客户确认接受后可在此登记回访</div>
        </template>
      </el-table>

      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next, jumper" :total="total"
                       v-model:current-page="q.page" v-model:page-size="q.size" :page-sizes="[10, 20, 50]"
                       @current-change="load" @size-change="search" />
      </div>
    </div>

    <!-- 回访登记 -->
    <el-dialog v-model="dlg" title="记录回访" width="560px">
      <div class="sum">
        <div class="sum-h">
          <b>{{ cur?.ticketNo || '—' }}</b>
          <span class="tag t-gray">{{ cur?.revisitNo || '回访登记' }}</span>
        </div>
        <div class="sum-b">{{ cur?.title || '—' }}</div>
        <div class="sum-b">客户：{{ cur?.customerName || '—' }} · 责任部门：{{ cur?.deptName || '—' }}</div>
      </div>

      <el-form label-width="92px" style="margin-top:12px">
        <el-form-item label="回访方式 *">
          <el-radio-group v-model="fm.revisitType">
            <el-radio-button value="电话">电话</el-radio-button>
            <el-radio-button value="在线">在线</el-radio-button>
            <el-radio-button value="现场">现场</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="是否解决 *">
          <el-radio-group v-model="fm.solvedFlag">
            <el-radio-button :value="1">已解决</el-radio-button>
            <el-radio-button :value="0">未解决</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="满意度">
          <el-rate v-model="fm.satisfaction" show-score score-template="{value} 分" />
        </el-form-item>
        <el-form-item label="回访意见 *">
          <el-input v-model="fm.opinion" type="textarea" :rows="3" maxlength="200" show-word-limit
                    placeholder="记录客户对处理结果、服务态度的具体反馈" />
        </el-form-item>
        <el-form-item label="后续动作">
          <el-input v-model="fm.nextAction" maxlength="120" placeholder="如：纳入质量复盘、再次上门复检" />
        </el-form-item>
        <el-form-item label="回访时长">
          <el-input-number v-model="fm.durationMin" :min="1" :max="300" :step="1" />
          <span class="hint">单位：分钟</span>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存回访</el-button>
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
import { fmtTime, fmtNum } from '../utils/format'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const total = ref(0)
const dlg = ref(false)
const cur = ref(null)

const q = reactive({ solved: '', keyword: '', page: 1, size: 10 })
const stats = reactive({ waiting: 0, done: 0, solveRate: 0, avgScore: '0.0' })
const fm = reactive({ revisitType: '电话', solvedFlag: 1, satisfaction: 4, opinion: '', nextAction: '', durationMin: 10 })

const view = computed(() => {
  const kw = q.keyword.trim()
  if (!kw) return rows.value
  return rows.value.filter((r) => `${r.revisitNo || ''}${r.ticketNo || ''}${r.customerName || ''}`.includes(kw))
})

async function load() {
  loading.value = true
  try {
    const p = { page: q.page, size: q.size }
    if (q.solved !== '') p.solved = q.solved
    const r = await api.revisitPage(p)
    rows.value = r.records || []
    total.value = r.total || 0
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  const [waiting, all] = await Promise.all([
    api.ticketPage({ status: '待回访', page: 1, size: 1 }),
    api.revisitPage({ page: 1, size: 500 })
  ])
  const recs = all.records || []
  stats.waiting = waiting.total || 0
  stats.done = all.total || 0
  const solved = recs.filter((r) => Number(r.solvedFlag) === 1).length
  stats.solveRate = recs.length ? Math.round((solved * 100) / recs.length) : 0
  const scored = recs.filter((r) => Number(r.satisfaction) > 0)
  const avg = scored.length ? scored.reduce((s, r) => s + Number(r.satisfaction), 0) / scored.length : 0
  stats.avgScore = fmtNum(avg, 1)
}

function search() {
  q.page = 1
  load()
}

function reset() {
  Object.assign(q, { solved: '', keyword: '', page: 1 })
  load()
}

function refreshAll() {
  load()
  loadStats()
}

function open(row) {
  cur.value = row
  Object.assign(fm, { revisitType: '电话', solvedFlag: 1, satisfaction: 4, opinion: '', nextAction: '', durationMin: 10 })
  dlg.value = true
}

async function submit() {
  if (!fm.opinion.trim()) return ElMessage.warning('请填写回访意见')
  saving.value = true
  try {
    await api.revisit({
      ticketId: cur.value.ticketId,
      orderId: cur.value.orderId,
      revisitType: fm.revisitType,
      solvedFlag: fm.solvedFlag,
      satisfaction: fm.satisfaction,
      opinion: fm.opinion.trim(),
      nextAction: fm.nextAction,
      durationMin: fm.durationMin
    })
    ElMessage.success('回访记录已保存')
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
.hint { font-size: 11px; color: var(--text-sub); margin-left: 10px; }
@media (max-width: 768px) {
  .kpis-4 { grid-template-columns: repeat(2, 1fr); }
}
</style>
