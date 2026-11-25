<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>延时申请审批</h1>
        <div class="desc">处理人因配件、客户时间等原因无法按期完成时提交延时申请，审批通过后同步调整工单时限</div>
      </div>
      <div class="acts">
        <el-button size="small" @click="refreshAll"><el-icon><Refresh /></el-icon>刷新</el-button>
        <el-button size="small" type="primary" @click="$router.push('/warning')">时限预警</el-button>
      </div>
    </div>

    <div class="kpis kpis-4">
      <div class="kpi" :class="{ warn: stats.pending > 0 }">
        <div class="lb"><span>待审批</span><span>条</span></div>
        <div class="vl num">{{ stats.pending }}</div>
        <div class="ft">等待审批的延时申请</div>
      </div>
      <div class="kpi good">
        <div class="lb"><span>已通过</span><span>条</span></div>
        <div class="vl num">{{ stats.passed }}</div>
        <div class="ft">已同意顺延工单时限</div>
      </div>
      <div class="kpi bad">
        <div class="lb"><span>已驳回</span><span>条</span></div>
        <div class="vl num">{{ stats.rejected }}</div>
        <div class="ft">未获批准，仍按时限考核</div>
      </div>
      <div class="kpi">
        <div class="lb"><span>平均延时</span><span>小时</span></div>
        <div class="vl num">{{ stats.avgHours }}</div>
        <div class="ft">按已提交申请统计</div>
      </div>
    </div>

    <div class="card">
      <div class="filters">
        <el-select v-model="q.status" placeholder="全部状态" clearable @change="search">
          <el-option label="待审批" value="待审批" />
          <el-option label="已通过" value="已通过" />
          <el-option label="已驳回" value="已驳回" />
        </el-select>
        <el-input v-model="q.keyword" placeholder="申请编号 / 工单号 / 标题（本页内过滤）" clearable>
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="view" v-loading="loading" style="width:100%">
        <el-table-column prop="applyNo" label="申请编号" width="140" />
        <el-table-column label="工单" width="150">
          <template #default="{ row }">
            <el-link type="primary" @click="$router.push(`/ticket/detail/${row.ticketId}`)">{{ row.ticketNo || `#${row.ticketId}` }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="170" show-overflow-tooltip />
        <el-table-column label="级别" width="76">
          <template #default="{ row }">
            <span class="tag" :class="levelTag(row.levelCode)">{{ row.levelCode || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="原要求完成" width="140">
          <template #default="{ row }"><span class="num">{{ fmtTime(row.oldDeadline) }}</span></template>
        </el-table-column>
        <el-table-column label="新要求完成" width="140">
          <template #default="{ row }"><span class="num">{{ fmtTime(row.newDeadline) }}</span></template>
        </el-table-column>
        <el-table-column label="延时" width="80" align="right">
          <template #default="{ row }"><span class="num">{{ row.delayHours || 0 }} 小时</span></template>
        </el-table-column>
        <el-table-column prop="reason" label="申请理由" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.reason || '—' }}</template>
        </el-table-column>
        <el-table-column prop="applicantName" label="申请人" width="90">
          <template #default="{ row }">{{ row.applicantName || '—' }}</template>
        </el-table-column>
        <el-table-column label="申请时间" width="140">
          <template #default="{ row }"><span class="num">{{ fmtTime(row.applyTime) }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="92">
          <template #default="{ row }"><span class="tag" :class="statusTag(row.status)">{{ row.status }}</span></template>
        </el-table-column>
        <el-table-column label="审批人" width="90">
          <template #default="{ row }">{{ row.approverName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="approveNote" label="审批意见" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.approveNote || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === '待审批'">
              <el-button link type="primary" @click="open(row, '已通过')">通过</el-button>
              <el-button link type="primary" @click="open(row, '已驳回')">驳回</el-button>
            </template>
            <span v-else style="color:var(--text-sub)">已审批</span>
          </template>
        </el-table-column>
        <template #empty>
          <div style="padding:32px 16px;text-align:center;color:var(--text-sub)">
            <div style="font-size:13px;margin-bottom:4px">当前没有延时申请</div>
            <div style="font-size:12px">处理人在「我的处理台」遇到客观原因无法按期完成时，可提交延时申请，提交后会出现在此处</div>
          </div>
        </template>
      </el-table>

      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next, jumper" :total="total"
                       v-model:current-page="q.page" v-model:page-size="q.size" :page-sizes="[10, 20, 50]"
                       @current-change="load" @size-change="search" />
      </div>
    </div>

    <!-- 审批对话框 -->
    <el-dialog v-model="dlg" :title="fm.status === '已通过' ? '通过延时申请' : '驳回延时申请'" width="560px">
      <div class="sum">
        <div class="sum-h">
          <b>{{ cur?.applyNo || '—' }}</b>
          <span class="tag" :class="levelTag(cur?.levelCode)">{{ cur?.levelCode || '—' }}</span>
        </div>
        <div class="sum-b">{{ cur?.ticketNo }} · {{ cur?.title }}</div>
      </div>

      <div class="cmp">
        <div class="cmp-i">
          <div class="cmp-lb">原要求完成</div>
          <div class="cmp-vl num">{{ fmtTime(cur?.oldDeadline) }}</div>
        </div>
        <div class="cmp-ar">→</div>
        <div class="cmp-i new">
          <div class="cmp-lb">新要求完成（延时 {{ cur?.delayHours || 0 }} 小时）</div>
          <div class="cmp-vl num">{{ fmtTime(cur?.newDeadline) }}</div>
        </div>
      </div>

      <div class="desc-block" style="margin-top:10px">
        <span style="color:var(--text-sub)">申请理由：</span>{{ cur?.reason || '—' }}
      </div>

      <el-form label-width="92px" style="margin-top:12px">
        <el-form-item label="审批结论 *">
          <el-radio-group v-model="fm.status">
            <el-radio-button value="已通过">通过</el-radio-button>
            <el-radio-button value="已驳回">驳回</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见 *">
          <el-input v-model="fm.approveNote" type="textarea" :rows="3" maxlength="200" show-word-limit
                    :placeholder="fm.status === '已通过' ? '说明同意延时的依据与调整后的跟进要求' : '说明不予延时的原因'" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">提交审批</el-button>
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
import { fmtTime, statusTag, levelTag, fmtNum } from '../utils/format'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const total = ref(0)
const dlg = ref(false)
const cur = ref(null)

const q = reactive({ status: '', keyword: '', page: 1, size: 10 })
const stats = reactive({ pending: 0, passed: 0, rejected: 0, avgHours: '0.0' })
const fm = reactive({ status: '已通过', approveNote: '' })

const view = computed(() => {
  const kw = q.keyword.trim()
  if (!kw) return rows.value
  return rows.value.filter((r) => `${r.applyNo || ''}${r.ticketNo || ''}${r.title || ''}`.includes(kw))
})

async function load() {
  loading.value = true
  try {
    const p = { page: q.page, size: q.size }
    if (q.status) p.status = q.status
    const r = await api.delayPage(p)
    rows.value = r.records || []
    total.value = r.total || 0
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  const [all, pending, passed, rejected] = await Promise.all([
    api.delayPage({ page: 1, size: 500 }),
    api.delayPage({ status: '待审批', page: 1, size: 1 }),
    api.delayPage({ status: '已通过', page: 1, size: 1 }),
    api.delayPage({ status: '已驳回', page: 1, size: 1 })
  ])
  stats.pending = pending.total || 0
  stats.passed = passed.total || 0
  stats.rejected = rejected.total || 0
  const recs = all.records || []
  const avg = recs.length ? recs.reduce((s, r) => s + Number(r.delayHours || 0), 0) / recs.length : 0
  stats.avgHours = fmtNum(avg, 1)
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
  Object.assign(fm, { status, approveNote: '' })
  dlg.value = true
}

async function submit() {
  if (!fm.approveNote.trim()) return ElMessage.warning('请填写审批意见')
  saving.value = true
  try {
    await api.approveDelay(cur.value.id, { status: fm.status, approveNote: fm.approveNote.trim() })
    ElMessage.success(fm.status === '已通过' ? '已通过延时申请，工单时限已顺延' : '已驳回该延时申请')
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
.cmp { display: grid; grid-template-columns: 1fr 24px 1fr; gap: 8px; align-items: center; margin-top: 12px; }
.cmp-i { border: 1px solid var(--line); border-radius: 6px; padding: 8px 10px; }
.cmp-i.new { border-color: var(--accent); background: #F3F7F9; }
.cmp-lb { font-size: 11px; color: var(--text-sub); }
.cmp-vl { font-size: 14px; font-weight: 600; margin-top: 2px; }
.cmp-ar { text-align: center; color: var(--text-sub); }
@media (max-width: 768px) {
  .kpis-4 { grid-template-columns: repeat(2, 1fr); }
}
</style>
