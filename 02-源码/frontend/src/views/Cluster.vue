<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>共性问题分析</h1>
        <div class="desc">按六因子加权计算工单相似度并聚为共性问题组：规则可配置、计算可留痕、结果可解释、档位可人工修正</div>
      </div>
      <div class="acts">
        <el-button size="small" @click="$router.push('/cluster/rule')">规则配置</el-button>
        <el-button size="small" type="primary" @click="reload">刷新</el-button>
      </div>
    </div>

    <div class="kpis k4">
      <div class="kpi bad">
        <div class="lb"><span>高相似问题组数</span><span>组</span></div>
        <div class="vl num">{{ stat.highGroups }}</div>
        <div class="ft">聚类平均分 ≥ {{ rule.highThreshold || 80 }}</div>
      </div>
      <div class="kpi warn">
        <div class="lb"><span>中相似条数</span><span>条</span></div>
        <div class="vl num">{{ stat.mid }}</div>
        <div class="ft">{{ rule.midThreshold || 60 }}–{{ (rule.highThreshold || 80) - 1 }} 分区间</div>
      </div>
      <div class="kpi">
        <div class="lb"><span>涉及工单总数</span><span>件</span></div>
        <div class="vl num">{{ stat.total }}</div>
        <div class="ft">已纳入聚类计算</div>
      </div>
      <div class="kpi">
        <div class="lb"><span>人工修正次数</span><span>次</span></div>
        <div class="vl num">{{ stat.manual }}</div>
        <div class="ft">规则结果的人为纠偏留痕</div>
      </div>
    </div>

    <div class="clu-grid">
      <!-- 左：聚类汇总 -->
      <div class="card" style="margin-bottom:0">
        <div class="card-h">
          <h2>聚类汇总</h2>
          <span class="sub">共 {{ summary.length }} 组 · 点击行可筛选明细</span>
        </div>
        <el-table :data="summary" v-loading="sumLoading" height="302" style="width:100%"
                  :row-class-name="sumRowClass" @row-click="pickCluster">
          <el-table-column prop="clusterName" label="聚类名称" min-width="158" show-overflow-tooltip />
          <el-table-column label="关联工单数" width="90" align="right">
            <template #default="{ row }"><span class="num">{{ row.cnt }}</span></template>
          </el-table-column>
          <el-table-column label="平均相似度" width="140">
            <template #default="{ row }">
              <div class="score-cell">
                <el-progress :percentage="Math.min(100, Math.round(Number(row.avgScore) || 0))"
                             :stroke-width="6" :show-text="false" :color="barColor(row.avgScore)" />
                <span class="num">{{ Number(row.avgScore || 0).toFixed(1) }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="高相似" width="66" align="right">
            <template #default="{ row }"><span class="num">{{ Number(row.highCnt) || 0 }}</span></template>
          </el-table-column>
          <el-table-column label="人工" width="56" align="right">
            <template #default="{ row }"><span class="num">{{ Number(row.manualCnt) || 0 }}</span></template>
          </el-table-column>
          <el-table-column label="采纳" width="56" align="right">
            <template #default="{ row }"><span class="num">{{ Number(row.adoptCnt) || 0 }}</span></template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 右：两张图 -->
      <div class="clu-right">
        <div class="chart-card">
          <div class="ch"><span>相似度档位分布</span><span>单位：条</span></div>
          <Chart :option="distOpt" />
        </div>
        <div class="chart-card">
          <div class="ch"><span>聚类工单量 TOP6</span><span>单位：件</span></div>
          <Chart :option="topOpt" />
        </div>
      </div>
    </div>

    <!-- 明细 -->
    <div class="card" style="margin-top:14px">
      <div class="card-h">
        <h2>相似度计算明细</h2>
        <div style="display:flex;align-items:center;gap:8px">
          <el-tag v-if="focusCluster" closable type="info" @close="clearCluster">{{ focusCluster }}</el-tag>
          <el-select v-model="q.level" placeholder="全部档位" clearable style="width:130px" @change="search">
            <el-option label="高相似" value="HIGH" />
            <el-option label="中相似" value="MID" />
            <el-option label="低相似" value="LOW" />
          </el-select>
          <span class="sub">规则版本 {{ rule.ruleVersion || '—' }} · 六因子加权</span>
        </div>
      </div>

      <el-table :data="rows" v-loading="loading" style="width:100%">
        <el-table-column label="工单号" width="126">
          <template #default="{ row }">
            <el-link type="primary" @click="$router.push(`/ticket/detail/${row.ticketId}`)">{{ row.ticketNo }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip />
        <el-table-column prop="clusterName" label="聚类名称" min-width="168" show-overflow-tooltip />
        <el-table-column label="六因子得分" width="228">
          <template #default="{ row }">
            <div class="fac">
              <span v-for="f in factors(row)" :key="f.key" class="fac-i" :class="f.cls" :title="f.tip">{{ f.val }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="总分" width="66" align="right">
          <template #default="{ row }"><b class="num">{{ Number(row.similarity || 0).toFixed(1) }}</b></template>
        </el-table-column>
        <el-table-column label="档位" width="80">
          <template #default="{ row }">
            <span class="tag" :class="clusterLabel(row.level)[1]">{{ clusterLabel(row.level)[0] }}</span>
          </template>
        </el-table-column>
        <el-table-column label="计算时间" width="132">
          <template #default="{ row }"><span class="num" style="font-size:12px">{{ fmtTime(row.calcTime) }}</span></template>
        </el-table-column>
        <el-table-column label="人工修正" min-width="150">
          <template #default="{ row }">
            <template v-if="row.manualLevel">
              <span class="tag" :class="clusterLabel(row.manualLevel)[1]">人工改为 {{ clusterLabel(row.manualLevel)[0] }}</span>
              <div class="mn" :title="row.manualReason">{{ row.manualReason || '—' }}</div>
            </template>
            <span v-else style="color:var(--text-sub)">—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openReason(row)">查看解释</el-button>
            <el-button link type="primary" @click="openManual(row)">人工修正</el-button>
          </template>
        </el-table-column>
        <template #empty><div style="padding:26px;color:var(--text-sub)">暂无聚类计算结果</div></template>
      </el-table>

      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next, jumper" :total="total"
                       v-model:current-page="q.page" v-model:page-size="q.size" :page-sizes="[10, 20, 50]"
                       @current-change="load" @size-change="search" />
      </div>
    </div>

    <!-- 计算解释抽屉 -->
    <el-drawer v-model="reasonDrawer" title="相似度计算解释与留痕" size="560px">
      <template v-if="cur">
        <div class="dr-head">
          <div>
            <div class="dr-title">{{ cur.clusterName }}</div>
            <div class="dr-sub">{{ cur.ticketNo }} · {{ cur.title }}</div>
          </div>
          <div style="text-align:right">
            <div class="dr-score num">{{ Number(cur.similarity || 0).toFixed(1) }}</div>
            <span class="tag" :class="clusterLabel(cur.level)[1]">{{ clusterLabel(cur.level)[0] }}</span>
          </div>
        </div>

        <div class="dr-sec">
          <div class="dr-t">六因子得分（加权得分 / 权重）</div>
          <div v-for="f in factors(cur)" :key="f.key" class="factor-row">
            <span style="color:var(--text-sub)">{{ f.label }}</span>
            <span class="bar"><i :style="{ width: f.pct + '%', background: f.color }"></i></span>
            <span class="num" style="text-align:right">{{ f.val }} / {{ f.w }}</span>
          </div>
        </div>

        <div class="dr-sec">
          <div class="dr-t">命中理由</div>
          <div class="desc-block">
            <div v-for="(r, i) in asList(cur.reasons)" :key="i" class="li">{{ r }}</div>
          </div>
        </div>

        <div class="dr-sec">
          <div class="dr-t">未满足项</div>
          <div class="desc-block">{{ asList(cur.unmet).join('；') || '无' }}</div>
        </div>

        <div class="dr-sec">
          <div class="dr-t">改进建议</div>
          <div class="desc-block">{{ asList(cur.improve).join('；') || '无需改进' }}</div>
        </div>

        <div class="dr-sec">
          <div class="dr-t">计算输入快照</div>
          <pre class="code">{{ snapshotText(cur.inputSnapshot) }}</pre>
        </div>

        <div class="dr-sec">
          <div class="dr-t">计算留痕</div>
          <div class="kv">
            <span class="k">规则版本</span><span>{{ cur.ruleVersion || '—' }}</span>
            <span class="k">计算时间</span><span class="num">{{ fmtTime(cur.calcTime) }}</span>
            <span class="k">责任部门</span><span>{{ cur.deptName || '—' }}</span>
            <span class="k">工单类别</span><span>{{ cur.categoryName || '—' }}</span>
            <span class="k">是否采纳</span><span>{{ cur.adoptFlag ? '已采纳为共性问题' : '未采纳' }}</span>
            <span class="k">人工修正</span>
            <span>
              <template v-if="cur.manualLevel">
                改为 {{ clusterLabel(cur.manualLevel)[0] }}（{{ cur.manualBy || '—' }} · {{ fmtTime(cur.manualTime) }}）
                <div class="mn" style="margin-top:2px">原因：{{ cur.manualReason || '—' }}</div>
              </template>
              <template v-else>—</template>
            </span>
          </div>
        </div>
      </template>
    </el-drawer>

    <!-- 人工修正对话框 -->
    <el-dialog v-model="manualDialog" title="人工修正相似度档位" width="440px">
      <div v-if="cur" class="mn-ticket">
        {{ cur.ticketNo }} · 系统判定 <b>{{ clusterLabel(cur.level)[0] }}</b>
        （总分 {{ Number(cur.similarity || 0).toFixed(1) }}）
      </div>
      <el-form label-width="76px" style="margin-top:10px">
        <el-form-item label="修正档位">
          <el-radio-group v-model="manualForm.level">
            <el-radio-button value="HIGH">高相似</el-radio-button>
            <el-radio-button value="MID">中相似</el-radio-button>
            <el-radio-button value="LOW">低相似</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="修正原因">
          <el-input v-model="manualForm.reason" type="textarea" :rows="3" maxlength="120" show-word-limit
                    placeholder="请说明修正依据，将随规则结果一并留痕" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="manualDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitManual">确认修正</el-button>
      </template>
    </el-dialog>
  </AdminLayout>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import AdminLayout from '../layouts/AdminLayout.vue'
import Chart from '../components/Chart.vue'
import { api } from '../api'
import { fmtTime, clusterLabel } from '../utils/format'

const AXIS = {
  axisLine: { lineStyle: { color: '#DEE4E9' } },
  axisLabel: { color: '#5B6A76', fontSize: 11 },
  splitLine: { lineStyle: { color: '#EDF1F4' } }
}

const loading = ref(false)
const sumLoading = ref(false)
const saving = ref(false)
const rows = ref([])
const allRows = ref([])
const summary = ref([])
const total = ref(0)
const focusCluster = ref('')
const q = reactive({ level: '', page: 1, size: 10 })

const rule = reactive({ ruleVersion: '', highThreshold: 80, midThreshold: 60 })
const stat = reactive({ high: 0, mid: 0, low: 0, highGroups: 0, total: 0, manual: 0 })

const reasonDrawer = ref(false)
const manualDialog = ref(false)
const cur = ref(null)
const manualForm = reactive({ level: 'MID', reason: '' })

// 六因子：字段、名称、权重（权重取当前生效规则）
const DEF = [
  { key: 'sProduct', label: '关联产品', w: 25 },
  { key: 'sStage', label: '服务环节', w: 15 },
  { key: 'sProblem', label: '问题类型', w: 20 },
  { key: 'sReason', label: '原因标签', w: 15 },
  { key: 'sDept', label: '责任部门', w: 10 },
  { key: 'sFreq', label: '近30天频次', w: 15 }
]
const W_KEYS = { sProduct: 'wProduct', sStage: 'wStage', sProblem: 'wProblem', sReason: 'wReason', sDept: 'wDept', sFreq: 'wFreq' }

function factors(row) {
  return DEF.map((d) => {
    const w = Number(rule[W_KEYS[d.key]] || d.w)
    const v = Number(row[d.key] || 0)
    const pct = Math.max(0, Math.min(100, Math.round((v / (w || 1)) * 100)))
    const color = pct >= 80 ? '#356A7A' : pct >= 50 ? '#8AA6B2' : '#B4C0CC'
    const cls = pct >= 80 ? 't-blue' : pct >= 50 ? 't-gray' : 't-gray'
    return { ...d, w, val: Number.isInteger(v) ? v : v.toFixed(1), pct, color, cls, tip: `${d.label}（权重 ${w}）：${v} 分，命中度 ${pct}%` }
  })
}

function barColor(v) {
  const n = Number(v) || 0
  if (n >= Number(rule.highThreshold || 80)) return '#356A7A'
  if (n >= Number(rule.midThreshold || 60)) return '#8AA6B2'
  return '#B4C0CC'
}

function asList(v) {
  if (v === null || v === undefined || v === '') return []
  const s = String(v).trim()
  if (s.startsWith('[')) {
    try {
      const a = JSON.parse(s)
      if (Array.isArray(a)) return a.map((x) => String(x))
    } catch (e) { /* 按分隔符兜底 */ }
  }
  return s.split(/[;；\n]+/).map((x) => x.trim()).filter(Boolean)
}

function snapshotText(v) {
  if (!v) return '—'
  const s = String(v).trim()
  if (s.startsWith('{') || s.startsWith('[')) {
    try { return JSON.stringify(JSON.parse(s), null, 2) } catch (e) { return s }
  }
  return s.split(/[;；]+/).map((x) => x.trim()).filter(Boolean).map((x) => '· ' + x).join('\n')
}

const distOpt = computed(() => {
  const labels = ['高相似', '中相似', '低相似']
  const values = [stat.high, stat.mid, stat.low || 0]
  return {
    color: ['#356A7A', '#456C7E', '#8AA6B2'],
    tooltip: { trigger: 'axis' },
    grid: { left: 8, right: 14, top: 14, bottom: 4, containLabel: true },
    xAxis: { type: 'category', data: labels, ...AXIS },
    yAxis: { type: 'value', ...AXIS },
    series: [{
      type: 'bar', data: values, barMaxWidth: 36,
      label: { show: true, position: 'top', fontSize: 11, color: '#5B6A76' },
      itemStyle: { borderRadius: [3, 3, 0, 0] }
    }]
  }
})

const topOpt = computed(() => {
  const top = [...summary.value].sort((a, b) => Number(b.cnt) - Number(a.cnt)).slice(0, 6).reverse()
  return {
    color: ['#356A7A'],
    tooltip: { trigger: 'axis' },
    grid: { left: 8, right: 26, top: 8, bottom: 2, containLabel: true },
    xAxis: { type: 'value', ...AXIS },
    yAxis: {
      type: 'category', data: top.map((x) => x.clusterName), ...AXIS,
      axisLabel: { ...AXIS.axisLabel, width: 92, overflow: 'truncate', interval: 0 }
    },
    series: [{
      type: 'bar', data: top.map((x) => Number(x.cnt)), barMaxWidth: 12,
      label: { show: true, position: 'right', fontSize: 11, color: '#5B6A76' },
      itemStyle: { borderRadius: [0, 3, 3, 0] }
    }]
  }
})

function sumRowClass({ row }) {
  return focusCluster.value && row.clusterName === focusCluster.value ? 'on-row' : ''
}

async function loadSummary() {
  sumLoading.value = true
  try {
    const list = await api.clusterSummary()
    summary.value = Array.isArray(list) ? list : []
    stat.highGroups = summary.value.filter((s) => Number(s.highCnt) > 0).length
    stat.manual = summary.value.reduce((a, s) => a + (Number(s.manualCnt) || 0), 0)
  } finally {
    sumLoading.value = false
  }
}

async function loadCounts() {
  const [h, m, l] = await Promise.all([
    api.clusterPage({ level: 'HIGH', page: 1, size: 1 }),
    api.clusterPage({ level: 'MID', page: 1, size: 1 }),
    api.clusterPage({ level: 'LOW', page: 1, size: 1 })
  ])
  stat.high = h?.total || 0
  stat.mid = m?.total || 0
  stat.low = l?.total || 0
  stat.total = stat.high + stat.mid + stat.low
}

async function load() {
  loading.value = true
  try {
    if (focusCluster.value) {
      if (!allRows.value.length) {
        const r = await api.clusterPage({ level: q.level || undefined, page: 1, size: 200 })
        allRows.value = r.records || []
      }
      const list = allRows.value.filter((x) => x.clusterName === focusCluster.value)
      total.value = list.length
      const start = (q.page - 1) * q.size
      rows.value = list.slice(start, start + q.size)
      return
    }
    const r = await api.clusterPage({ level: q.level || undefined, page: q.page, size: q.size })
    rows.value = r.records || []
    total.value = r.total || 0
  } finally {
    loading.value = false
  }
}

function search() { q.page = 1; allRows.value = []; load() }

function pickCluster(row) {
  if (focusCluster.value === row.clusterName) {
    clearCluster()
    return
  }
  focusCluster.value = row.clusterName
  q.page = 1
  allRows.value = []
  load()
}

function clearCluster() {
  focusCluster.value = ''
  allRows.value = []
  q.page = 1
  load()
}

function openReason(row) {
  cur.value = row
  reasonDrawer.value = true
}

function openManual(row) {
  cur.value = row
  manualForm.level = row.manualLevel || row.level || 'MID'
  manualForm.reason = ''
  manualDialog.value = true
}

async function submitManual() {
  if (!manualForm.reason || manualForm.reason.trim().length < 5) {
    ElMessage.warning('请填写不少于 5 个字的修正原因')
    return
  }
  saving.value = true
  try {
    await api.manualLevel(cur.value.id, { level: manualForm.level, manualLevel: manualForm.level, reason: manualForm.reason.trim() })
    ElMessage.success('已保存人工修正，结果已留痕')
    manualDialog.value = false
    allRows.value = []
    await Promise.all([load(), loadSummary()])
  } finally {
    saving.value = false
  }
}

async function reload() {
  allRows.value = []
  await Promise.all([loadSummary(), loadCounts(), load()])
}

onMounted(async () => {
  try {
    const d = await api.clusterRule()
    if (d?.active) Object.assign(rule, d.active)
  } catch (e) { /* 规则读取失败不影响明细 */ }
  reload()
})
</script>

<style scoped>
.k4 { grid-template-columns: repeat(4, 1fr); }
.clu-grid { display: grid; grid-template-columns: 1.25fr 1fr; gap: 14px; }
.clu-right { display: grid; grid-template-rows: 1fr 1fr; gap: 14px; }
.clu-right .chart-card { height: 157px; }
.score-cell { display: flex; align-items: center; gap: 6px; }
.score-cell .el-progress { flex: 1; }
.score-cell span { font-size: 12px; color: var(--text-sub); width: 34px; text-align: right; }
.fac { display: grid; grid-template-columns: repeat(6, 1fr); gap: 3px; }
.fac-i {
  display: block; text-align: center; font-size: 11px; line-height: 19px; border-radius: 3px;
  font-variant-numeric: tabular-nums; cursor: help;
}
.mn { font-size: 11px; color: var(--text-sub); margin-top: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.mn-ticket { background: var(--tint); border-radius: 6px; padding: 8px 10px; font-size: 12px; color: var(--text-sub); }
.dr-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; padding-bottom: 12px; border-bottom: 1px solid var(--line); }
.dr-title { font-size: 15px; font-weight: 600; }
.dr-sub { font-size: 12px; color: var(--text-sub); margin-top: 2px; }
.dr-score { font-size: 26px; font-weight: 600; color: var(--secondary); }
.dr-sec { margin-top: 14px; }
.dr-t { font-size: 12px; font-weight: 600; color: var(--text-sub); margin-bottom: 6px; }
.dr-sec .li { line-height: 21px; }
.dr-sec .factor-row { grid-template-columns: 92px 1fr 72px; }
.dr-sec .mn { white-space: normal; }
.code {
  margin: 0; background: #F7F9FA; border: 1px solid var(--line); border-radius: 6px;
  padding: 10px 12px; font-size: 12px; line-height: 20px; color: #2A3A46;
  white-space: pre-wrap; word-break: break-all; max-height: 180px; overflow: auto;
  font-family: "SF Mono", Menlo, Consolas, monospace;
}
:deep(.on-row) { background: #EDF3F6 !important; cursor: pointer; }
@media (max-width: 1200px) {
  .clu-grid { grid-template-columns: 1fr; }
}
</style>
