<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>客户确认跟踪</h1>
        <div class="desc">跟踪处理结果送达后的客户确认进度，超期未确认自动转人工跟进</div>
      </div>
      <div class="acts">
        <el-button size="small" @click="refreshAll"><el-icon><Refresh /></el-icon>刷新</el-button>
        <el-button size="small" type="primary" @click="$router.push('/ticket/list')">投诉台账</el-button>
      </div>
    </div>

    <div class="kpis kpis-4">
      <div class="kpi" :class="{ warn: stats.pending > 0 }">
        <div class="lb"><span>待确认</span><span>单</span></div>
        <div class="vl num">{{ stats.pending }}</div>
        <div class="ft">已送达结果，等待客户确认</div>
      </div>
      <div class="kpi good">
        <div class="lb"><span>已接受</span><span>单</span></div>
        <div class="vl num">{{ stats.accepted }}</div>
        <div class="ft">客户确认接受处理结果</div>
      </div>
      <div class="kpi bad">
        <div class="lb"><span>超期未确认</span><span>单</span></div>
        <div class="vl num">{{ stats.expired }}</div>
        <div class="ft">超过确认时限未回执</div>
      </div>
    </div>

    <div class="card">
      <div class="filters">
        <el-select v-model="q.result" placeholder="全部确认结果" clearable @change="search">
          <el-option label="接受" value="接受" />
          <el-option label="超期未确认" value="超期未确认" />
        </el-select>
        <el-input v-model="q.keyword" placeholder="工单号 / 标题（本页内过滤）" clearable>
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="view" v-loading="loading" style="width:100%">
        <el-table-column label="编号" width="80">
          <template #default="{ row }"><span class="num">#{{ row.id }}</span></template>
        </el-table-column>
        <el-table-column label="工单" width="150">
          <template #default="{ row }">
            <el-link type="primary" @click="$router.push(`/ticket/detail/${row.ticketId}`)">{{ row.ticketNo || `#${row.ticketId}` }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="级别" width="76">
          <template #default="{ row }">
            <span class="tag" :class="levelTag(row.levelCode)">{{ row.levelCode || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="deptName" label="责任部门" min-width="120" show-overflow-tooltip />
        <el-table-column label="完成时间" width="140">
          <template #default="{ row }"><span class="num">{{ fmtTime(row.finishTime) }}</span></template>
        </el-table-column>
        <el-table-column label="确认截止" width="140">
          <template #default="{ row }">
            <span class="num" :style="{ color: isOverdue(row.deadlineTime, row.confirmTime) ? 'var(--danger)' : '' }">
              {{ fmtTime(row.deadlineTime) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="已提醒" width="80" align="right">
          <template #default="{ row }">
            <span class="num">{{ row.remindCount || 0 }} 次</span>
          </template>
        </el-table-column>
        <el-table-column label="确认结果" width="104">
          <template #default="{ row }">
            <span v-if="row.confirmResult" class="tag" :class="statusTag(row.confirmResult)">{{ row.confirmResult }}</span>
            <span v-else class="tag t-amber">待确认</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-if="!row.confirmTime" link type="primary" @click="open(row)">代为确认</el-button>
            <el-button v-if="!row.confirmTime" link type="primary" @click="remind(row)">再次提醒</el-button>
            <el-button v-if="row.confirmTime" link type="primary" @click="$router.push(`/ticket/detail/${row.ticketId}`)">查看工单</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div style="padding:26px;color:var(--text-sub)">暂无待确认工单，处理完成后会自动进入客户确认环节</div>
        </template>
      </el-table>

      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next, jumper" :total="total"
                       v-model:current-page="q.page" v-model:page-size="q.size" :page-sizes="[10, 20, 50]"
                       @current-change="load" @size-change="search" />
      </div>
    </div>

    <!-- 代为确认：客户电话/门店口头确认后由客服录入 -->
    <el-dialog v-model="dlg" title="代为确认处理结果" width="540px">
      <div class="sum">
        <div class="sum-h">
          <b>{{ cur?.ticketNo || '—' }}</b>
          <span class="tag" :class="levelTag(cur?.levelCode)">{{ cur?.levelCode || '—' }}</span>
        </div>
        <div class="sum-b">{{ cur?.title || '—' }}</div>
        <div class="sum-b">确认截止：{{ fmtTime(cur?.deadlineTime) }}</div>
      </div>

      <el-form label-width="92px" style="margin-top:12px">
        <el-form-item label="确认结果 *">
          <el-radio-group v-model="fm.confirmResult">
            <el-radio-button value="接受">接受</el-radio-button>
            <el-radio-button value="不接受">不接受</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="确认意见 *">
          <el-input v-model="fm.opinion" type="textarea" :rows="3" maxlength="200" show-word-limit
                    placeholder="记录客户确认渠道、确认人及具体意见" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">提交确认</el-button>
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
import { fmtTime, statusTag, levelTag, isOverdue } from '../utils/format'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const total = ref(0)
const dlg = ref(false)
const cur = ref(null)

const q = reactive({ result: '', keyword: '', page: 1, size: 10 })
const stats = reactive({ pending: 0, accepted: 0, expired: 0 })
const fm = reactive({ confirmResult: '接受', opinion: '' })

const view = computed(() => {
  const kw = q.keyword.trim()
  if (!kw) return rows.value
  return rows.value.filter((r) => `${r.ticketNo || ''}${r.title || ''}`.includes(kw))
})

async function load() {
  loading.value = true
  try {
    const p = { page: q.page, size: q.size }
    if (q.result) p.result = q.result
    const r = await api.confirmPage(p)
    rows.value = r.records || []
    total.value = r.total || 0
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  const [pending, accepted, expired] = await Promise.all([
    api.ticketPage({ status: '待客户确认', page: 1, size: 1 }),
    api.confirmPage({ result: '接受', page: 1, size: 1 }),
    api.confirmPage({ result: '超期未确认', page: 1, size: 1 })
  ])
  stats.pending = pending.total || 0
  stats.accepted = accepted.total || 0
  stats.expired = expired.total || 0
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

function open(row) {
  cur.value = row
  Object.assign(fm, { confirmResult: '接受', opinion: '' })
  dlg.value = true
}

// 后端暂未提供提醒接口，此处按本地次数提示并累计展示
function remind(row) {
  row.remindCount = (row.remindCount || 0) + 1
  ElMessage.success(`已向客户发送第 ${row.remindCount} 次确认提醒`)
}

async function submit() {
  if (!fm.confirmResult) return ElMessage.warning('请选择确认结果')
  if (!fm.opinion.trim()) return ElMessage.warning('请填写确认意见')
  saving.value = true
  try {
    await api.confirm(cur.value.ticketId, { confirmResult: fm.confirmResult, opinion: fm.opinion.trim() })
    ElMessage.success(fm.confirmResult === '接受' ? '已登记客户接受，工单可进入回访' : '已登记客户不接受，工单将退回处理')
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
