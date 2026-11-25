<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>规则与 SLA 配置</h1>
        <div class="desc">严重级别定义决定工单的响应/处理时限，SLA 时限规则再按投诉类别细化；时限与预警比例共同驱动预警、升级与超期判定</div>
      </div>
      <div class="acts">
        <el-button size="small" @click="load">刷新</el-button>
        <el-button size="small" @click="$router.push('/warning')">前往时限预警中心</el-button>
      </div>
    </div>

    <!-- 规则如何驱动预警 -->
    <div class="card flow-card">
      <div class="fl-step">
        <span class="fl-no">1</span>
        <div>
          <div class="fl-t">级别定义时限</div>
          <div class="fl-d">按 P1–P4 设定基础响应与处理时限，P1 紧急工单重点关注</div>
        </div>
      </div>
      <span class="fl-arrow">→</span>
      <div class="fl-step">
        <span class="fl-no">2</span>
        <div>
          <div class="fl-t">类别规则细化</div>
          <div class="fl-d">同类投诉按类别单独约定时限，建单时写入工单的响应/完成时间</div>
        </div>
      </div>
      <span class="fl-arrow">→</span>
      <div class="fl-step">
        <span class="fl-no">3</span>
        <div>
          <div class="fl-t">预警比例触发</div>
          <div class="fl-d">已用时限达到 <b>warnRatio</b> 时进入预警中心，超期则按规则升级</div>
        </div>
      </div>
      <span class="fl-arrow">→</span>
      <div class="fl-step">
        <span class="fl-no">4</span>
        <div>
          <div class="fl-t">超期升级留痕</div>
          <div class="fl-d">超期工单标记并推送消息，回访/重开时作为考核依据</div>
        </div>
      </div>
    </div>

    <div class="card">
      <el-tabs v-model="tab">
        <el-tab-pane label="SLA 时限规则" name="sla">
          <div class="card-h" style="margin-top:2px">
            <h2>按类别约定的时限</h2>
            <span class="sub">共 {{ slaRows.length }} 条规则 · 时限单位：小时</span>
          </div>
          <el-table :data="slaRows" v-loading="loading" style="width:100%">
            <el-table-column prop="ruleName" label="规则名称" min-width="230" show-overflow-tooltip />
            <el-table-column label="适用类别" min-width="160">
              <template #default="{ row }">{{ categoryName(row.categoryId) }}</template>
            </el-table-column>
            <el-table-column label="级别" width="80">
              <template #default="{ row }">
                <span class="tag" :class="levelTag(row.levelCode)">{{ row.levelCode }}</span>
              </template>
            </el-table-column>
            <el-table-column label="响应时限" width="92" align="right">
              <template #default="{ row }"><span class="num">{{ row.responseHours }} h</span></template>
            </el-table-column>
            <el-table-column label="处理时限" width="92" align="right">
              <template #default="{ row }"><span class="num">{{ row.handleHours }} h</span></template>
            </el-table-column>
            <el-table-column label="预警比例" width="120">
              <template #default="{ row }">
                <div class="ratio">
                  <span class="bar"><i :style="{ width: pct(row.warnRatio) + '%' }"></i></span>
                  <span class="num">{{ pct(row.warnRatio) }}%</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="超期升级" width="94">
              <template #default="{ row }">
                <span class="tag" :class="row.overdueUpgrade ? 't-red' : 't-gray'">{{ row.overdueUpgrade ? '自动升级' : '不升级' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="生效日期" width="106">
              <template #default="{ row }"><span class="num">{{ fmtDate(row.effectiveDate) }}</span></template>
            </el-table-column>
            <el-table-column label="状态" width="72">
              <template #default="{ row }">
                <span class="tag" :class="row.status === '生效' ? 't-green' : 't-gray'">{{ row.status }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="170" show-overflow-tooltip />
            <template #empty><div style="padding:26px;color:var(--text-sub)">暂无 SLA 规则</div></template>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="严重级别定义" name="severity">
          <div class="card-h" style="margin-top:2px">
            <h2>P1–P4 级别基准</h2>
            <span class="sub">共 {{ sevRows.length }} 个级别 · 时限单位：小时</span>
          </div>
          <el-table :data="sevRows" v-loading="loading" style="width:100%">
            <el-table-column label="级别代码" width="90">
              <template #default="{ row }">
                <span class="tag" :class="levelTag(row.levelCode)">{{ row.levelCode }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="levelName" label="名称" width="100" />
            <el-table-column label="响应时限" width="100" align="right">
              <template #default="{ row }"><span class="num">{{ row.responseHours }} h</span></template>
            </el-table-column>
            <el-table-column label="处理时限" width="100" align="right">
              <template #default="{ row }"><span class="num">{{ row.handleHours }} h</span></template>
            </el-table-column>
            <el-table-column label="升级时限" width="100" align="right">
              <template #default="{ row }"><span class="num">{{ row.escalateHours }} h</span></template>
            </el-table-column>
            <el-table-column label="重点关注" width="92">
              <template #default="{ row }">
                <span class="tag" :class="row.focusFlag ? 't-red' : 't-gray'">{{ row.focusFlag ? '是' : '否' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="说明" min-width="300" show-overflow-tooltip />
            <el-table-column label="状态" width="76">
              <template #default="{ row }">
                <span class="tag" :class="row.status === '启用' ? 't-green' : 't-gray'">{{ row.status }}</span>
              </template>
            </el-table-column>
            <template #empty><div style="padding:26px;color:var(--text-sub)">暂无级别定义</div></template>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AdminLayout from '../layouts/AdminLayout.vue'
import { api } from '../api'
import { fmtDate, levelTag } from '../utils/format'

const tab = ref('sla')
const loading = ref(false)
const slaRows = ref([])
const sevRows = ref([])
const categories = ref([])

function categoryName(id) {
  const c = categories.value.find((x) => String(x.id) === String(id))
  return c?.categoryName || '—'
}

function pct(v) {
  const n = Number(v)
  if (isNaN(n)) return 0
  return Math.round(n * 100)
}

async function load() {
  loading.value = true
  try {
    const o = await api.options()
    categories.value = o?.categories || []
    slaRows.value = o?.slaRules || []
    sevRows.value = o?.severityRules || []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.flow-card { display: flex; align-items: center; gap: 14px; }
.fl-step { display: flex; gap: 10px; align-items: flex-start; flex: 1; }
.fl-no {
  flex: 0 0 22px; width: 22px; height: 22px; border-radius: 50%; background: #E6EEF3; color: #2A5B6B;
  display: grid; place-items: center; font-size: 12px; font-weight: 600; margin-top: 1px;
}
.fl-t { font-size: 13px; font-weight: 600; }
.fl-d { font-size: 11px; color: var(--text-sub); line-height: 18px; margin-top: 2px; }
.fl-arrow { color: var(--line); font-size: 14px; }
.ratio { display: flex; align-items: center; gap: 6px; }
.ratio .bar { flex: 1; }
.ratio .num { font-size: 12px; color: var(--text-sub); width: 32px; }
:deep(.el-tabs__header) { margin-bottom: 10px; }
@media (max-width: 1200px) {
  .flow-card { flex-wrap: wrap; }
  .fl-arrow { display: none; }
}
</style>
