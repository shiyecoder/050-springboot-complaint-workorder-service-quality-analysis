<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>工单分派</h1>
        <div class="desc">把待受理投诉分派到责任部门与具体处理人，分派后处理人可在 SLA 工单看板接单</div>
      </div>
      <div class="acts">
        <el-button size="small" @click="load"><el-icon><Refresh /></el-icon>刷新</el-button>
        <el-button size="small" type="primary" :disabled="!sel.length" @click="openBatch">
          批量分派{{ sel.length ? `（${sel.length}）` : '' }}
        </el-button>
      </div>
    </div>

    <div class="card">
      <div class="filters">
        <el-input v-model="q.keyword" placeholder="编号 / 标题 / 客户" clearable @keyup.enter="search">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="q.status" placeholder="全部状态" clearable>
          <el-option label="待受理" value="待受理" />
          <el-option label="已分派" value="已分派" />
        </el-select>
        <el-select v-model="q.level" placeholder="全部级别" clearable>
          <el-option v-for="l in opt.levels" :key="l.levelCode" :label="`${l.levelCode} ${l.levelName}`" :value="l.levelCode" />
        </el-select>
        <el-select v-model="q.deptId" placeholder="全部分派建议部门" clearable>
          <el-option v-for="d in opt.depts" :key="d.id" :label="d.orgName" :value="d.id" />
        </el-select>
        <el-checkbox v-model="q.overdue" :true-value="1" :false-value="undefined" label="仅看超期" />
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" style="width:100%" @selection-change="onSelect">
        <el-table-column type="selection" width="44" :selectable="(r) => r.status === '待受理'" />
        <el-table-column prop="ticketNo" label="投诉编号" width="150">
          <template #default="{ row }">
            <el-link type="primary" @click="$router.push(`/ticket/detail/${row.id}`)">{{ row.ticketNo }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="投诉标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="客户 / 对象" min-width="150">
          <template #default="{ row }">
            <div>{{ row.customerName || '—' }}</div>
            <div style="font-size:11px;color:var(--text-sub)">{{ row.objectName || '—' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="类别" min-width="130" show-overflow-tooltip />
        <el-table-column label="级别" width="76">
          <template #default="{ row }"><span class="tag" :class="levelTag(row.levelCode)">{{ row.levelCode }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><span class="tag" :class="statusTag(row.status)">{{ row.status }}</span></template>
        </el-table-column>
        <el-table-column prop="deptName" label="建议部门" min-width="120" show-overflow-tooltip />
        <el-table-column prop="currentHandlerName" label="当前处理人" width="100">
          <template #default="{ row }">{{ row.currentHandlerName || '—' }}</template>
        </el-table-column>
        <el-table-column label="要求完成" width="140">
          <template #default="{ row }">
            <span class="num" :style="{ color: isOverdue(row.deadlineTime, row.finishTime) ? 'var(--danger)' : '' }">
              {{ fmtTime(row.deadlineTime) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === '待受理'" link type="primary" @click="openFor([row])">分派</el-button>
            <el-button v-else link type="primary" @click="$router.push(`/ticket/detail/${row.id}`)">详情</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div style="padding:26px;color:var(--text-sub)">当前筛选条件下无待分派工单，可清空筛选或先在受理台登记投诉</div>
        </template>
      </el-table>

      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next, jumper" :total="total"
                       v-model:current-page="q.page" v-model:page-size="q.size" :page-sizes="[10, 20, 50]"
                       @current-change="load" @size-change="search" />
      </div>
    </div>

    <!-- 分派对话框：批量与单条共用 -->
    <el-dialog v-model="dlg" title="分派工单" width="520px">
      <div class="sum">
        <div class="sum-h">
          <b>{{ targets.length > 1 ? `已选择 ${targets.length} 条待受理投诉` : targets[0]?.ticketNo }}</b>
          <span class="tag" :class="levelTag(targets[0]?.levelCode)">{{ targets[0]?.levelCode || '—' }}</span>
        </div>
        <div class="sum-b" v-if="targets.length === 1">{{ targets[0]?.title }}</div>
        <div class="sum-b" v-else>批量分派将把所选投诉一并派给同一部门与处理人</div>
      </div>

      <el-form label-width="92px" style="margin-top:12px">
        <el-form-item label="责任部门 *">
          <el-select v-model="fm.deptId" placeholder="请选择部门" style="width:100%" @change="fm.handlerId = null">
            <el-option v-for="d in opt.depts" :key="d.id" :label="d.orgName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理人 *">
          <el-select v-model="fm.handlerId" placeholder="请选择该部门处理人" filterable style="width:100%"
                     :disabled="!fm.deptId">
            <el-option v-for="h in deptHandlers" :key="h.id" :label="`${h.realName}（${h.orgName}）`" :value="h.id" />
          </el-select>
          <div class="hint" v-if="fm.deptId && !deptHandlers.length">该部门暂无可选处理人，请更换部门</div>
        </el-form-item>
        <el-form-item label="优先级">
          <el-radio-group v-model="fm.priority">
            <el-radio-button value="高">高</el-radio-button>
            <el-radio-button value="中">中</el-radio-button>
            <el-radio-button value="低">低</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="计划完成">
          <el-date-picker v-model="fm.planFinishTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss"
                          placeholder="承诺客户的完成时间" style="width:100%" />
        </el-form-item>
        <el-form-item label="分派说明">
          <el-input v-model="fm.note" type="textarea" :rows="3" maxlength="200" show-word-limit
                    placeholder="说明分派依据、责任认定与客户关注点" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确认分派</el-button>
      </template>
    </el-dialog>
  </AdminLayout>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import AdminLayout from '../layouts/AdminLayout.vue'
import { api } from '../api'
import { fmtTime, statusTag, levelTag, isOverdue } from '../utils/format'

const route = useRoute()
const opt = reactive({ levels: [], depts: [], handlers: [] })
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const sel = ref([])
const dlg = ref(false)
const targets = ref([])

const q = reactive({ keyword: '', status: '待受理', level: '', deptId: null, overdue: undefined, page: 1, size: 10 })
const fm = reactive({ deptId: null, handlerId: null, priority: '中', planFinishTime: '', note: '' })

const deptHandlers = computed(() => (opt.handlers || []).filter((h) => Number(h.orgId) === Number(fm.deptId)))

async function load() {
  loading.value = true
  try {
    const r = await api.ticketPage({ ...q, overdue: q.overdue || '' })
    rows.value = r.records || []
    total.value = r.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  q.page = 1
  load()
}

function reset() {
  Object.assign(q, { keyword: '', status: '待受理', level: '', deptId: null, overdue: undefined, page: 1 })
  load()
}

function onSelect(list) {
  sel.value = list
}

function openFor(list) {
  targets.value = list
  fm.deptId = list[0]?.deptId || null
  fm.handlerId = list[0]?.currentHandlerId || null
  fm.priority = '中'
  fm.planFinishTime = ''
  fm.note = ''
  dlg.value = true
}

function openBatch() {
  if (!sel.value.length) return ElMessage.warning('请先勾选待受理工单')
  openFor(sel.value)
}

// 分派说明 + 优先级 + 计划完成一并写入流转记录，便于事后追溯（后端状态机只认 reason）
function buildReason() {
  const parts = []
  if (fm.note) parts.push(fm.note)
  parts.push(`优先级：${fm.priority}`)
  if (fm.planFinishTime) parts.push(`计划完成：${fm.planFinishTime}`)
  return parts.join('；')
}

async function submit() {
  if (!fm.deptId) return ElMessage.warning('请选择责任部门')
  if (!fm.handlerId) return ElMessage.warning('请选择处理人')
  if (!targets.value.length) return

  saving.value = true
  let ok = 0
  let fail = 0
  try {
    // 状态机逐个推进，单条失败不影响其余工单
    for (const t of targets.value) {
      try {
        await api.transition(t.id, {
          toStatus: '已分派',
          reason: buildReason(),
          deptId: fm.deptId,
          handlerId: fm.handlerId
        })
        ok++
      } catch (e) {
        fail++
      }
    }
    if (ok) ElMessage.success(`已分派 ${ok} 条${fail ? `，${fail} 条失败` : ''}`)
    else ElMessage.error('分派失败，请检查所选工单状态后重试')
    dlg.value = false
    sel.value = []
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  const o = await api.options()
  Object.assign(opt, o)
  await load()

  // 从台账/详情页带 query.id 进入时，自动打开该工单的分派对话框
  const id = Number(route.query.id)
  if (id) {
    const hit = rows.value.find((r) => r.id === id)
    if (hit) {
      openFor([hit])
    } else {
      try {
        const d = await api.ticketDetail(id)
        if (d?.ticket) {
          const t = d.ticket
          targets.value = [{
            id: t.id, ticketNo: t.ticketNo, title: t.title, levelCode: t.levelCode,
            deptId: t.deptId, currentHandlerId: t.currentHandlerId, status: t.status
          }]
          fm.deptId = t.deptId || null
          fm.handlerId = t.currentHandlerId || null
          dlg.value = true
        }
      } catch (e) {
        ElMessage.warning('未找到该投诉，可能已被处理')
      }
    }
  }
})
</script>

<style scoped>
.sum { background: var(--tint); border-radius: 6px; padding: 10px 12px; }
.sum-h { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.sum-b { font-size: 12px; color: var(--text-sub); margin-top: 4px; line-height: 18px; }
.hint { font-size: 11px; color: var(--text-sub); }
</style>
