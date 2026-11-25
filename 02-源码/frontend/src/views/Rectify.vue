<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>整改任务</h1>
        <div class="desc">由共性问题聚类生成整改任务，跟踪措施落地与验收，形成闭环改进</div>
      </div>
      <div class="acts">
        <el-button size="small" @click="refreshAll"><el-icon><Refresh /></el-icon>刷新</el-button>
        <el-button size="small" type="primary" @click="openCreate"><el-icon><Plus /></el-icon>新建整改任务</el-button>
      </div>
    </div>

    <div class="kpis kpis-4">
      <div class="kpi">
        <div class="lb"><span>整改中</span><span>项</span></div>
        <div class="vl num">{{ stats.doing }}</div>
        <div class="ft">措施执行中的整改任务</div>
      </div>
      <div class="kpi warn">
        <div class="lb"><span>待验收</span><span>项</span></div>
        <div class="vl num">{{ stats.checking }}</div>
        <div class="ft">已提交待验收确认</div>
      </div>
      <div class="kpi good">
        <div class="lb"><span>已验收</span><span>项</span></div>
        <div class="vl num">{{ stats.accepted }}</div>
        <div class="ft">验收通过并跟踪效果</div>
      </div>
      <div class="kpi bad">
        <div class="lb"><span>逾期</span><span>项</span></div>
        <div class="vl num">{{ stats.overdue }}</div>
        <div class="ft">已过截止日期且未验收</div>
      </div>
    </div>

    <div class="card">
      <div class="filters">
        <el-select v-model="q.status" placeholder="全部状态" clearable @change="search">
          <el-option v-for="s in statusOpts" :key="s" :label="s" :value="s" />
        </el-select>
        <el-input v-model="q.keyword" placeholder="任务编号 / 主题 / 聚类（本页内过滤）" clearable>
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="view" v-loading="loading" style="width:100%">
        <el-table-column prop="taskNo" label="任务编号" width="140" />
        <el-table-column prop="title" label="整改主题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="clusterName" label="来源聚类" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.clusterName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="deptName" label="责任部门" min-width="110" show-overflow-tooltip />
        <el-table-column prop="measure" label="整改措施" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">{{ row.measure || '—' }}</template>
        </el-table-column>
        <el-table-column label="负责人" width="90">
          <template #default="{ row }">{{ row.ownerName || '—' }}</template>
        </el-table-column>
        <el-table-column label="截止日期" width="112">
          <template #default="{ row }">
            <span class="num" :style="{ color: isLate(row) ? 'var(--danger)' : '' }">{{ fmtDate(row.deadline) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="关联投诉" width="92" align="right">
          <template #default="{ row }">
            <el-tooltip :content="row.sourceTickets || '—'" placement="top">
              <span class="num">{{ ticketCount(row.sourceTickets) }} 条</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="92">
          <template #default="{ row }"><span class="tag" :class="statusTag(row.status)">{{ row.status }}</span></template>
        </el-table-column>
        <el-table-column label="验收次数" width="90" align="right">
          <template #default="{ row }"><span class="num">{{ row.acceptCount || 0 }} 次</span></template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === '待验收'" link type="primary" @click="openAccept(row)">验收</el-button>
            <el-button v-else-if="row.status === '已验收'" link type="primary" @click="openClose(row)">关闭</el-button>
            <span v-else style="color:var(--text-sub)">—</span>
          </template>
        </el-table-column>
        <template #empty>
          <div style="padding:32px 16px;text-align:center;color:var(--text-sub)">
            <div style="font-size:13px;margin-bottom:4px">暂无整改任务</div>
            <div style="font-size:12px">可从共性问题分析页按聚类下发整改任务，或点击右上角手动新建</div>
          </div>
        </template>
      </el-table>

      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next, jumper" :total="total"
                       v-model:current-page="q.page" v-model:page-size="q.size" :page-sizes="[10, 20, 50]"
                       @current-change="load" @size-change="search" />
      </div>
    </div>

    <!-- 新建整改任务 -->
    <el-dialog v-model="createDlg" title="新建整改任务" width="620px">
      <el-form label-width="96px">
        <el-form-item label="整改主题 *">
          <el-input v-model="cf.title" maxlength="60" placeholder="如：安装服务-预约延误 共性问题整改" />
        </el-form-item>
        <el-form-item label="来源聚类 *">
          <el-input v-model="cf.clusterName" maxlength="60" placeholder="如：安装服务·预约延误" />
        </el-form-item>
        <el-form-item label="责任部门 *">
          <el-select v-model="cf.deptId" placeholder="请选择责任部门" style="width:100%">
            <el-option v-for="d in opt.depts" :key="d.id" :label="d.orgName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人 *">
          <el-select v-model="cf.ownerId" placeholder="请选择负责人" filterable style="width:100%">
            <el-option v-for="h in opt.handlers" :key="h.id" :label="`${h.realName}（${h.orgName}）`" :value="h.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止日期 *">
          <el-date-picker v-model="cf.deadline" type="date" value-format="YYYY-MM-DD" placeholder="选择整改完成日期" style="width:100%" />
        </el-form-item>
        <el-form-item label="整改措施 *">
          <el-input v-model="cf.measure" type="textarea" :rows="3" maxlength="300" show-word-limit
                    placeholder="写明整改动作、责任人与验证方式" />
        </el-form-item>
        <el-form-item label="关联工单号">
          <el-input v-model="cf.sourceTickets" maxlength="200" placeholder="多个工单号用英文逗号分隔，如 TS20261015018,TS20261015019" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="createDlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitCreate">创建任务</el-button>
      </template>
    </el-dialog>

    <!-- 验收 / 关闭 -->
    <el-dialog v-model="actDlg" :title="actMode === 'close' ? '关闭整改任务' : '整改验收'" width="540px">
      <div class="sum">
        <div class="sum-h">
          <b>{{ cur?.taskNo || '—' }}</b>
          <span class="tag" :class="statusTag(cur?.status)">{{ cur?.status }}</span>
        </div>
        <div class="sum-b">{{ cur?.title || '—' }}</div>
        <div class="sum-b">责任部门：{{ cur?.deptName || '—' }} · 负责人：{{ cur?.ownerName || '—' }} · 截止：{{ fmtDate(cur?.deadline) }}</div>
      </div>

      <div class="desc-block" style="margin-top:10px">
        <span style="color:var(--text-sub)">整改措施：</span>{{ cur?.measure || '—' }}
      </div>

      <el-form label-width="92px" style="margin-top:12px">
        <el-form-item v-if="actMode !== 'close'" label="验收结论 *">
          <el-radio-group v-model="af.accepted">
            <el-radio-button :value="true">通过</el-radio-button>
            <el-radio-button :value="false">不通过</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="actMode === 'close' ? '关闭说明 *' : '验收意见 *'">
          <el-input v-model="af.note" type="textarea" :rows="3" maxlength="200" show-word-limit
                    :placeholder="actMode === 'close' ? '说明任务关闭依据与后续跟踪安排' : '说明验收结论与仍需改进之处'" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="actDlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitAct">{{ actMode === 'close' ? '确认关闭' : '提交验收' }}</el-button>
      </template>
    </el-dialog>
  </AdminLayout>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search, Plus } from '@element-plus/icons-vue'
import AdminLayout from '../layouts/AdminLayout.vue'
import { api } from '../api'
import { fmtDate, statusTag, isOverdue } from '../utils/format'

const statusOpts = ['待下发', '整改中', '待验收', '已验收', '已关闭']

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const total = ref(0)
const createDlg = ref(false)
const actDlg = ref(false)
const actMode = ref('accept')
const cur = ref(null)

const opt = reactive({ depts: [], handlers: [] })
const q = reactive({ status: '', keyword: '', page: 1, size: 10 })
const stats = reactive({ doing: 0, checking: 0, accepted: 0, overdue: 0 })

const cf = reactive({ title: '', clusterName: '', deptId: null, ownerId: null, deadline: '', measure: '', sourceTickets: '' })
const af = reactive({ accepted: true, note: '' })

const view = computed(() => {
  const kw = q.keyword.trim()
  if (!kw) return rows.value
  return rows.value.filter((r) => `${r.taskNo || ''}${r.title || ''}${r.clusterName || ''}`.includes(kw))
})

// 截止日期已过且未验收、未关闭即为逾期
function isLate(row) {
  if (!row || row.status === '已验收' || row.status === '已关闭') return false
  return isOverdue(row.deadline, row.finishTime)
}

function ticketCount(s) {
  if (!s) return 0
  return String(s).split(',').filter((x) => x.trim()).length
}

async function load() {
  loading.value = true
  try {
    const p = { page: q.page, size: q.size }
    if (q.status) p.status = q.status
    const r = await api.rectifyPage(p)
    rows.value = r.records || []
    total.value = r.total || 0
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  const all = await api.rectifyPage({ page: 1, size: 500 })
  const recs = all.records || []
  stats.doing = recs.filter((r) => r.status === '整改中').length
  stats.checking = recs.filter((r) => r.status === '待验收').length
  stats.accepted = recs.filter((r) => r.status === '已验收').length
  stats.overdue = recs.filter((r) => isLate(r)).length
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

function openCreate() {
  Object.assign(cf, { title: '', clusterName: '', deptId: null, ownerId: null, deadline: '', measure: '', sourceTickets: '' })
  createDlg.value = true
}

async function submitCreate() {
  if (!cf.title.trim()) return ElMessage.warning('请填写整改主题')
  if (!cf.clusterName.trim()) return ElMessage.warning('请填写来源聚类')
  if (!cf.deptId) return ElMessage.warning('请选择责任部门')
  if (!cf.ownerId) return ElMessage.warning('请选择负责人')
  if (!cf.deadline) return ElMessage.warning('请选择截止日期')
  if (!cf.measure.trim()) return ElMessage.warning('请填写整改措施')
  saving.value = true
  try {
    await api.createRectify({
      title: cf.title.trim(),
      clusterName: cf.clusterName.trim(),
      deptId: cf.deptId,
      measure: cf.measure.trim(),
      deadline: cf.deadline,
      ownerId: cf.ownerId,
      sourceTickets: cf.sourceTickets.trim()
    })
    ElMessage.success('整改任务已创建')
    createDlg.value = false
    refreshAll()
  } finally {
    saving.value = false
  }
}

function openAccept(row) {
  cur.value = row
  actMode.value = 'accept'
  Object.assign(af, { accepted: true, note: '' })
  actDlg.value = true
}

function openClose(row) {
  cur.value = row
  actMode.value = 'close'
  Object.assign(af, { accepted: true, note: '' })
  actDlg.value = true
}

async function submitAct() {
  if (!af.note.trim()) return ElMessage.warning(actMode.value === 'close' ? '请填写关闭说明' : '请填写验收意见')
  saving.value = true
  try {
    // 关闭动作同样通过验收接口提交，由后端按当前状态推进至已关闭
    await api.acceptRectify(cur.value.id, { accepted: actMode.value === 'close' ? true : af.accepted, note: af.note.trim() })
    if (actMode.value === 'close') ElMessage.success('整改任务已关闭')
    else ElMessage.success(af.accepted ? '验收通过' : '已退回，整改继续')
    actDlg.value = false
    refreshAll()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  const o = await api.options()
  Object.assign(opt, o)
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
