<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>我的处理台</h1>
        <div class="desc">按泳道查看本人名下工单：先接单再提交处理方案，临期与超期工单标红提醒</div>
      </div>
      <div class="acts">
        <el-button size="small" @click="load"><el-icon><Refresh /></el-icon>刷新</el-button>
        <el-button size="small" type="primary" @click="$router.push('/order/desk')">SLA 工单看板</el-button>
      </div>
    </div>

    <div class="kpis kpis-4">
      <div class="kpi" :class="{ warn: stats.waiting > 0 }">
        <div class="lb"><span>待接单</span><span>单</span></div>
        <div class="vl num">{{ stats.waiting || 0 }}</div>
        <div class="ft">已分派到本人，待确认接单</div>
      </div>
      <div class="kpi">
        <div class="lb"><span>处理中</span><span>单</span></div>
        <div class="vl num">{{ stats.handling || 0 }}</div>
        <div class="ft">含已提交待审核</div>
      </div>
      <div class="kpi good">
        <div class="lb"><span>已完成</span><span>单</span></div>
        <div class="vl num">{{ stats.done || 0 }}</div>
        <div class="ft">待回访与已归档合计</div>
      </div>
      <div class="kpi" :class="{ bad: stats.overdue > 0 }">
        <div class="lb"><span>已超期</span><span>单</span></div>
        <div class="vl num">{{ stats.overdue || 0 }}</div>
        <div class="ft">超期工单建议先与客户沟通</div>
      </div>
    </div>

    <div class="lanes" v-loading="loading">
      <!-- 泳道一：待接单 -->
      <div class="lane">
        <div class="lane-h">
          <span>待接单</span>
          <span class="num">{{ waiting.length }}</span>
        </div>
        <div class="lane-body">
          <div v-for="w in waiting" :key="w.id" class="tcard">
            <div class="tc-top">
              <span class="num tc-no">{{ w.orderNo }}</span>
              <span class="tag" :class="levelTag(w.levelCode)">{{ w.levelCode }}</span>
            </div>
            <div class="tc-title">{{ w.title || '—' }}</div>
            <div class="tc-meta">
              <span>{{ w.customerName || '—' }}</span>
              <span>{{ w.serviceStage || w.nodeName }}</span>
            </div>
            <div class="tc-meta">
              <span>要求完成</span>
              <span class="num">{{ fmtTime(w.deadlineTime) }}</span>
            </div>
            <div class="tc-bottom">
              <span class="num" :style="{ color: w.overdueFlag ? 'var(--danger)' : 'var(--text-sub)' }">
                {{ remainText(w) }}
              </span>
              <div>
                <el-button link type="primary" @click="$router.push(`/ticket/detail/${w.ticketId}`)">详情</el-button>
                <el-button size="small" type="primary" @click="accept(w)">接单</el-button>
              </div>
            </div>
          </div>
          <div v-if="!waiting.length" class="lane-empty">没有待接单的工单，新分派会实时出现在这里</div>
        </div>
      </div>

      <!-- 泳道二：处理中 -->
      <div class="lane">
        <div class="lane-h">
          <span>处理中</span>
          <span class="num">{{ handling.length }}</span>
        </div>
        <div class="lane-body">
          <div v-for="h in handling" :key="h.id" class="tcard">
            <div class="tc-top">
              <span class="num tc-no">{{ h.orderNo }}</span>
              <span class="tag" :class="statusTag(h.status)">{{ h.status }}</span>
            </div>
            <div class="tc-title">{{ h.title || '—' }}</div>
            <div class="tc-meta">
              <span>{{ h.customerName || '—' }}</span>
              <span>{{ h.deptName || '—' }}</span>
            </div>
            <div class="tc-meta">
              <span>要求完成</span>
              <span class="num">{{ fmtTime(h.deadlineTime) }}</span>
            </div>
            <div class="tc-bottom">
              <span class="num" :style="{ color: h.overdueFlag ? 'var(--danger)' : 'var(--text-sub)' }">
                {{ remainText(h) }}
              </span>
              <div>
                <el-button link type="primary" @click="$router.push(`/ticket/detail/${h.ticketId}`)">详情</el-button>
                <el-button v-if="h.status === '处理中'" size="small" type="primary" @click="goEdit(h)">提交方案</el-button>
                <el-button v-else size="small" disabled>等待审核</el-button>
              </div>
            </div>
          </div>
          <div v-if="!handling.length" class="lane-empty">暂无处理中的工单，接单后即可在此提交处理方案</div>
        </div>
      </div>

      <!-- 泳道三：完成情况统计 -->
      <div class="lane">
        <div class="lane-h">
          <span>已完成</span>
          <span class="num">{{ stats.done || 0 }}</span>
        </div>
        <div class="lane-body">
          <div class="done-box">
            <div class="done-num num">{{ stats.done || 0 }}</div>
            <div class="done-lb">累计完成工单</div>
            <div class="done-row">
              <div><span class="num">{{ stats.handling || 0 }}</span><i>处理中</i></div>
              <div><span class="num">{{ stats.overdue || 0 }}</span><i>曾超期</i></div>
            </div>
          </div>
          <div class="done-tip">
            完成工单会进入客户确认与回访环节，可在「回访与评价」查看客户反馈；
            超期工单建议先在详情页登记沟通记录再提交方案。
          </div>
          <el-button style="width:100%" @click="$router.push('/ticket/list')">查看全部相关工单</el-button>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import AdminLayout from '../layouts/AdminLayout.vue'
import { api } from '../api'
import { fmtTime, fmtGap, statusTag, levelTag } from '../utils/format'

const loading = ref(false)
const router = useRouter()
const waiting = ref([])
const handling = ref([])
const stats = ref({ waiting: 0, handling: 0, done: 0, overdue: 0 })
const nowStr = ref(fmtTime(new Date()))
let timer = null

function remainText(row) {
  if (!row.deadlineTime) return '—'
  // 截止时间在前、当前时间在后：未来截止输出「剩余」，已过期输出「已过」
  return fmtGap(row.deadlineTime, nowStr.value)
}

async function load() {
  loading.value = true
  try {
    const r = await api.handleDesk()
    waiting.value = r.waiting || []
    handling.value = r.handling || []
    stats.value = r.stats || { waiting: 0, handling: 0, done: 0, overdue: 0 }
  } finally {
    loading.value = false
  }
}

// 接单：已分派 → 处理中
async function accept(row) {
  await api.acceptOrder(row.ticketId)
  ElMessage.success(`已接单：${row.orderNo}`)
  await load()
}

function goEdit(row) {
  router.push({ path: '/handle/edit', query: { ticketId: row.ticketId } })
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
.lanes { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; align-items: start; }
.lane { background: #fff; border: 1px solid var(--line); border-radius: var(--radius-card); padding: 12px 14px; min-height: 320px; }
.lane-h {
  display: flex; align-items: center; justify-content: space-between;
  font-size: 13px; font-weight: 600; padding-bottom: 8px; border-bottom: 1px solid var(--line);
}
.lane-h .num { color: var(--text-sub); font-weight: 400; }
.lane-body { display: flex; flex-direction: column; gap: 10px; padding-top: 10px; }

.tcard { border: 1px solid var(--line); border-radius: 6px; padding: 10px 12px; }
.tc-top { display: flex; align-items: center; justify-content: space-between; }
.tc-no { font-size: 12px; color: var(--text-sub); }
.tc-title { font-size: 13px; font-weight: 600; line-height: 20px; margin: 4px 0; }
.tc-meta { display: flex; justify-content: space-between; font-size: 11px; color: var(--text-sub); line-height: 18px; }
.tc-bottom { display: flex; align-items: center; justify-content: space-between; margin-top: 8px; padding-top: 8px; border-top: 1px dashed var(--line); }

.lane-empty { font-size: 12px; color: var(--text-sub); line-height: 20px; padding: 18px 4px; text-align: center; }
.done-box { background: var(--tint); border-radius: 8px; padding: 16px; text-align: center; }
.done-num { font-size: 32px; font-weight: 600; color: var(--success); line-height: 40px; }
.done-lb { font-size: 12px; color: var(--text-sub); }
.done-row { display: flex; justify-content: space-around; margin-top: 12px; font-size: 15px; }
.done-row i { display: block; font-size: 11px; color: var(--text-sub); font-style: normal; }
.done-tip { font-size: 12px; color: var(--text-sub); line-height: 20px; }

@media (max-width: 1200px) {
  .lanes { grid-template-columns: 1fr; }
  .kpis-4 { grid-template-columns: repeat(2, 1fr); }
}
</style>
