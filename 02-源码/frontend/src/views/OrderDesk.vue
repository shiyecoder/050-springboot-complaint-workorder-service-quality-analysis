<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>SLA 工单看板</h1>
        <div class="desc">处理人视角：待接单、处理中工单按剩余时限排序，超期与临期工单优先处理</div>
      </div>
      <div class="acts">
        <el-radio-group v-model="filter" size="small">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="已分派">待接单</el-radio-button>
          <el-radio-button value="处理中">处理中</el-radio-button>
          <el-radio-button value="待审核">待审核</el-radio-button>
        </el-radio-group>
        <el-button size="small" @click="load"><el-icon><Refresh /></el-icon>刷新</el-button>
      </div>
    </div>

    <div class="kpis kpis-4">
      <div class="kpi" :class="{ warn: stat.wait > 0 }">
        <div class="lb"><span>待接单</span><span>单</span></div>
        <div class="vl num">{{ stat.wait }}</div>
        <div class="ft">已分派但尚未接单</div>
      </div>
      <div class="kpi">
        <div class="lb"><span>处理中</span><span>单</span></div>
        <div class="vl num">{{ stat.doing }}</div>
        <div class="ft">正在处理或待审核</div>
      </div>
      <div class="kpi" :class="{ warn: stat.soon > 0 }">
        <div class="lb"><span>临期</span><span>单</span></div>
        <div class="vl num">{{ stat.soon }}</div>
        <div class="ft">剩余不足 8 小时</div>
      </div>
      <div class="kpi" :class="{ bad: stat.over > 0 }">
        <div class="lb"><span>已超期</span><span>单</span></div>
        <div class="vl num">{{ stat.over }}</div>
        <div class="ft">需立即升级处理</div>
      </div>
    </div>

    <div class="desk">
      <div class="card">
        <div class="card-h">
          <h2>工单列表</h2>
          <span class="sub">按截止时间升序，共 {{ list.length }} 单</span>
        </div>

        <el-table :data="list" v-loading="loading" size="small" style="width:100%">
          <el-table-column prop="orderNo" label="工单号" width="140">
            <template #default="{ row }">
              <el-link type="primary" @click="$router.push(`/ticket/detail/${row.ticketId}`)">{{ row.orderNo }}</el-link>
            </template>
          </el-table-column>
          <el-table-column prop="ticketNo" label="客诉编号" width="140" />
          <el-table-column prop="title" label="投诉标题" min-width="180" show-overflow-tooltip />
          <el-table-column label="级别" width="70">
            <template #default="{ row }"><span class="tag" :class="levelTag(row.levelCode)">{{ row.levelCode }}</span></template>
          </el-table-column>
          <el-table-column prop="nodeName" label="当前节点" width="90" />
          <el-table-column prop="handlerName" label="处理人" width="88">
            <template #default="{ row }">{{ row.handlerName || '—' }}</template>
          </el-table-column>
          <el-table-column prop="deptName" label="责任部门" width="120" show-overflow-tooltip />
          <el-table-column prop="priority" label="优先级" width="76" />
          <el-table-column label="要求完成" width="140">
            <template #default="{ row }"><span class="num">{{ fmtTime(row.deadlineTime) }}</span></template>
          </el-table-column>
          <el-table-column label="剩余时间" width="130">
            <template #default="{ row }">
              <span class="num" :style="{ color: row.overdueFlag ? 'var(--danger)' : '' }">{{ remainText(row) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <span class="tag" :class="row.overdueFlag ? 't-red' : statusTag(row.status)">
                {{ row.overdueFlag ? '超期' : row.status }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status === '已分派'" link type="primary" @click="accept(row)">接单</el-button>
              <el-button link type="primary" @click="$router.push(`/ticket/detail/${row.ticketId}`)">详情</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <div style="padding:26px;color:var(--text-sub)">当前筛选条件下没有待办工单，可切换状态或稍后刷新</div>
          </template>
        </el-table>
      </div>

      <div class="card">
        <div class="card-h"><h2>协同任务</h2><span class="sub">{{ coops.length }} 项</span></div>
        <el-table :data="coops" v-loading="loading" size="small" style="width:100%">
          <el-table-column prop="ticketNo" label="客诉编号" width="132">
            <template #default="{ row }">
              <el-link type="primary" @click="$router.push(`/ticket/detail/${row.ticketId}`)">{{ row.ticketNo }}</el-link>
            </template>
          </el-table-column>
          <el-table-column prop="deptName" label="协办部门" width="110" />
          <el-table-column prop="handlerName" label="协办人" width="84">
            <template #default="{ row }">{{ row.handlerName || '—' }}</template>
          </el-table-column>
          <el-table-column prop="taskContent" label="协办内容" min-width="200" show-overflow-tooltip />
          <el-table-column label="截止" width="136">
            <template #default="{ row }"><span class="num">{{ fmtTime(row.deadlineTime) }}</span></template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }"><span class="tag" :class="statusTag(row.status)">{{ row.status }}</span></template>
          </el-table-column>
          <template #empty>
            <div style="padding:20px;color:var(--text-sub)">暂无跨部门协同任务</div>
          </template>
        </el-table>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import AdminLayout from '../layouts/AdminLayout.vue'
import { api } from '../api'
import { fmtTime, fmtGap, statusTag, levelTag } from '../utils/format'

const loading = ref(false)
const todo = ref([])
const coops = ref([])
const filter = ref('')
// 剩余时间需要「实时」跳动，这里每 30 秒刷新一次当前时间基准
const nowStr = ref(fmtTime(new Date()))
let timer = null

const list = computed(() => (filter.value ? todo.value.filter((t) => t.status === filter.value) : todo.value))

const stat = computed(() => {
  const s = { wait: 0, doing: 0, soon: 0, over: 0 }
  todo.value.forEach((t) => {
    if (t.status === '已分派') s.wait++
    if (t.status === '处理中' || t.status === '待审核') s.doing++
    if (t.overdueFlag) s.over++
    else if (t.deadlineTime && hoursLeft(t.deadlineTime) <= 8) s.soon++
  })
  return s
})

function hoursLeft(deadline) {
  const a = new Date(String(deadline).replace(/-/g, '/')).getTime()
  if (isNaN(a)) return 999
  return (a - Date.now()) / 3600000
}

// fmtGap(from, to)：to 早于 from 时输出「剩余 X」。这里把截止时间当 from、当前时间当 to，
// 截止在未来即得到「剩余 xx」，已过期则得到「已过 xx」
function remainText(row) {
  if (!row.deadlineTime) return '—'
  return fmtGap(row.deadlineTime, nowStr.value)
}

async function load() {
  loading.value = true
  try {
    const r = await api.orderDesk()
    todo.value = r.todo || []
    coops.value = r.coops || []
  } finally {
    loading.value = false
  }
}

async function accept(row) {
  await api.acceptOrder(row.ticketId)
  ElMessage.success(`已接单：${row.orderNo}`)
  await load()
}

onMounted(async () => {
  await load()
  timer = setInterval(() => { nowStr.value = fmtTime(new Date()) }, 30000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.kpis-4 { grid-template-columns: repeat(4, 1fr); }
.desk { display: grid; grid-template-columns: minmax(0, 1.9fr) minmax(0, 1fr); gap: 14px; align-items: start; }
.desk .card { margin-bottom: 0; }
@media (max-width: 1400px) {
  .desk { grid-template-columns: 1fr; }
  .kpis-4 { grid-template-columns: repeat(2, 1fr); }
}
</style>
