<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>结果审核</h1>
        <div class="desc">对处理方案、证据与责任认定进行审核，通过后工单自动进入「待客户确认」</div>
      </div>
      <div class="acts">
        <el-button size="small" @click="refreshAll"><el-icon><Refresh /></el-icon>刷新</el-button>
        <el-button size="small" type="primary" @click="$router.push('/handle/desk')">去处理台</el-button>
      </div>
    </div>

    <div class="kpis kpis-4">
      <div class="kpi" :class="{ warn: stats.pending > 0 }">
        <div class="lb"><span>待审核数</span><span>单</span></div>
        <div class="vl num">{{ stats.pending }}</div>
        <div class="ft">工单状态为「待审核」</div>
      </div>
      <div class="kpi">
        <div class="lb"><span>今日已审</span><span>单</span></div>
        <div class="vl num">{{ stats.today }}</div>
        <div class="ft">今日已给出审核结论</div>
      </div>
      <div class="kpi good">
        <div class="lb"><span>通过率</span><span>%</span></div>
        <div class="vl num">{{ stats.passRate }}</div>
        <div class="ft">累计通过 {{ stats.pass }} 条</div>
      </div>
      <div class="kpi bad">
        <div class="lb"><span>驳回数</span><span>条</span></div>
        <div class="vl num">{{ stats.reject }}</div>
        <div class="ft">驳回后需退回处理人补充</div>
      </div>
    </div>

    <div class="card">
      <div class="filters">
        <el-select v-model="q.result" placeholder="全部审核结果" clearable @change="search">
          <el-option label="通过" value="通过" />
          <el-option label="驳回" value="驳回" />
        </el-select>
        <el-input v-model="q.keyword" placeholder="审核编号 / 标题（本页内过滤）" clearable>
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="view" v-loading="loading" style="width:100%">
        <el-table-column prop="reviewNo" label="审核编号" width="110" />
        <el-table-column label="关联工单" width="110">
          <template #default="{ row }">
            <el-link type="primary" @click="$router.push(`/ticket/detail/${row.ticketId}`)">工单 #{{ row.ticketId }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="190" show-overflow-tooltip />
        <el-table-column label="级别" width="76">
          <template #default="{ row }">
            <span class="tag" :class="levelTag(row.levelCode)">{{ row.levelCode || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="类别" min-width="140" show-overflow-tooltip />
        <el-table-column prop="deptName" label="责任部门" min-width="120" show-overflow-tooltip />
        <el-table-column label="方案评分" width="130">
          <template #default="{ row }">
            <div style="display:flex;align-items:center;gap:6px">
              <div class="bar" style="flex:1"><i :style="{ width: `${row.planScore || 0}%` }"></i></div>
              <span class="num" style="width:26px;text-align:right">{{ row.planScore ?? '—' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="证据齐备" width="92">
          <template #default="{ row }">
            <span class="tag" :class="row.evidenceFlag ? 't-green' : 't-amber'">{{ row.evidenceFlag ? '是' : '否' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="责任判定" width="92">
          <template #default="{ row }">
            <el-tooltip v-if="row.dutyCheck" :content="row.dutyCheck" placement="top">
              <span class="tag t-green">已明确</span>
            </el-tooltip>
            <span v-else class="tag t-amber">待明确</span>
          </template>
        </el-table-column>
        <el-table-column prop="reviewerName" label="提交人" width="90">
          <template #default="{ row }">{{ row.reviewerName || '—' }}</template>
        </el-table-column>
        <el-table-column label="提交时间" width="140">
          <template #default="{ row }"><span class="num">{{ fmtTime(row.reviewTime) }}</span></template>
        </el-table-column>
        <el-table-column label="审核结果" width="92">
          <template #default="{ row }">
            <span class="tag" :class="statusTag(row.reviewResult)">{{ row.reviewResult || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="open(row)">审核</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div style="padding:26px;color:var(--text-sub)">暂无符合条件的审核记录，可先到处理台提交处理方案</div>
        </template>
      </el-table>

      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next, jumper" :total="total"
                       v-model:current-page="q.page" v-model:page-size="q.size" :page-sizes="[10, 20, 50]"
                       @current-change="load" @size-change="search" />
      </div>
    </div>

    <!-- 审核对话框：展示方案要点后再给结论 -->
    <el-dialog v-model="dlg" title="处理方案审核" width="640px">
      <div v-loading="detailLoading">
        <div class="sum">
          <div class="sum-h">
            <b>{{ cur?.title || '—' }}</b>
            <span class="tag" :class="levelTag(cur?.levelCode)">{{ cur?.levelCode || '—' }}</span>
            <span class="tag t-gray">{{ cur?.reviewNo }}</span>
          </div>
          <div class="sum-b">{{ cur?.categoryName || '—' }} · 责任部门：{{ cur?.deptName || '—' }}</div>
        </div>

        <div class="desc-block" style="margin-top:10px">
          <div><span style="color:var(--text-sub)">处理方案：</span>{{ detail?.order?.solution || '—' }}</div>
          <div><span style="color:var(--text-sub)">处理结果：</span>{{ detail?.order?.result || '—' }}</div>
          <div><span style="color:var(--text-sub)">客户诉求：</span>{{ detail?.ticket?.demand || '—' }}</div>
          <div><span style="color:var(--text-sub)">证据材料：</span>{{ (detail?.attachments || []).length }} 份</div>
        </div>

        <el-form label-width="92px" style="margin-top:12px">
          <el-form-item label="审核结论 *">
            <el-radio-group v-model="fm.reviewResult">
              <el-radio-button value="通过">通过</el-radio-button>
              <el-radio-button value="驳回">驳回</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="方案评分">
            <el-input-number v-model="fm.planScore" :min="1" :max="100" :step="1" />
            <span class="hint">1~100 分，用于统计处理方案质量</span>
          </el-form-item>
          <el-form-item label="审核意见 *">
            <el-input v-model="fm.opinion" type="textarea" :rows="3" maxlength="200" show-word-limit
                      :placeholder="fm.reviewResult === '驳回' ? '请说明驳回原因与需补充的材料' : '请写明审核依据与结论'" />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">提交审核</el-button>
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
import { fmtTime, fmtDate, statusTag, levelTag } from '../utils/format'

const loading = ref(false)
const saving = ref(false)
const detailLoading = ref(false)
const rows = ref([])
const total = ref(0)
const dlg = ref(false)
const cur = ref(null)
const detail = ref(null)

const q = reactive({ result: '', keyword: '', page: 1, size: 10 })
const stats = reactive({ pending: 0, today: 0, passRate: 0, pass: 0, reject: 0 })
const fm = reactive({ reviewResult: '通过', planScore: 80, opinion: '' })

// 关键字只做本页内过滤，分页仍由后端负责
const view = computed(() => {
  const kw = q.keyword.trim()
  if (!kw) return rows.value
  return rows.value.filter((r) => `${r.reviewNo || ''}${r.title || ''}`.includes(kw))
})

async function load() {
  loading.value = true
  try {
    const p = { page: q.page, size: q.size }
    if (q.result) p.result = q.result
    const r = await api.reviewPage(p)
    rows.value = r.records || []
    total.value = r.total || 0
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  const [pending, pass, reject, all] = await Promise.all([
    api.ticketPage({ status: '待审核', page: 1, size: 1 }),
    api.reviewPage({ result: '通过', page: 1, size: 1 }),
    api.reviewPage({ result: '驳回', page: 1, size: 1 }),
    api.reviewPage({ page: 1, size: 500 })
  ])
  stats.pending = pending.total || 0
  stats.pass = pass.total || 0
  stats.reject = reject.total || 0
  const today = fmtDate(new Date())
  stats.today = (all.records || []).filter((r) => String(r.reviewTime || '').slice(0, 10) === today).length
  const sum = stats.pass + stats.reject
  stats.passRate = sum ? Math.round((stats.pass * 100) / sum) : 0
}

function search() {
  q.page = 1
  load()
}

function reset() {
  Object.assign(q, { result: '', keyword: '', page: 1 })
  load()
}

function refreshAll() {
  load()
  loadStats()
}

async function open(row) {
  cur.value = row
  detail.value = null
  Object.assign(fm, { reviewResult: '通过', planScore: row.planScore || 80, opinion: '' })
  dlg.value = true
  detailLoading.value = true
  try {
    detail.value = await api.ticketDetail(row.ticketId)
  } catch (e) {
    detail.value = null
  } finally {
    detailLoading.value = false
  }
}

async function submit() {
  if (!fm.reviewResult) return ElMessage.warning('请选择审核结论')
  if (!fm.opinion.trim()) {
    return ElMessage.warning(fm.reviewResult === '驳回' ? '驳回必须填写审核意见' : '请填写审核意见')
  }
  saving.value = true
  try {
    await api.review(cur.value.ticketId, {
      reviewResult: fm.reviewResult,
      opinion: fm.opinion.trim(),
      planScore: fm.planScore
    })
    ElMessage.success(fm.reviewResult === '通过' ? '审核通过，工单已进入待客户确认' : '已驳回，工单退回处理人')
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
