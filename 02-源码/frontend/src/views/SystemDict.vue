<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>字典与参数</h1>
        <div class="desc">业务字典约束受理渠道、工单状态流转、共性档位与消息类型；系统参数决定各严重级别的时限与预警口径</div>
      </div>
      <div class="acts">
        <el-button size="small" @click="load">刷新</el-button>
      </div>
    </div>

    <div class="dict-grid">
      <!-- 受理渠道 -->
      <div class="card dict-card">
        <div class="card-h">
          <h2>受理渠道字典</h2>
          <span class="sub">{{ channels.length }} 项</span>
        </div>
        <div class="chips">
          <span v-for="c in channels" :key="c" class="chip">
            <span class="chip-n">{{ c }}</span>
            <span class="chip-d">{{ CHANNEL_DESC[c] || '其他来源渠道' }}</span>
          </span>
        </div>
        <div class="use">用途：建单时记录投诉来源，用于渠道构成分析，并影响受理时限与回访方式的选择。</div>
      </div>

      <!-- 工单状态 -->
      <div class="card dict-card">
        <div class="card-h">
          <h2>工单状态字典</h2>
          <span class="sub">{{ statuses.length }} 项</span>
        </div>
        <div class="status-line">
          <span v-for="(s, i) in statuses" :key="s" class="st">
            <span class="tag" :class="statusTag(s)">{{ s }}</span>
            <span v-if="i < statuses.length - 1" class="arw">→</span>
          </span>
        </div>
        <div class="status-desc">
          <div v-for="s in statuses" :key="s" class="sd">
            <span class="tag" :class="statusTag(s)">{{ s }}</span>
            <span>{{ STATUS_DESC[s] || '流程中间态' }}</span>
          </div>
        </div>
        <div class="use">用途：状态机驱动全部流转动作与待办归属，状态变更会同步写入流转记录，作为时效统计口径。</div>
      </div>

      <!-- 共性档位 -->
      <div class="card dict-card">
        <div class="card-h">
          <h2>共性档位字典</h2>
          <span class="sub">规则版本 {{ rule.ruleVersion || '—' }}</span>
        </div>
        <div class="level-line" v-for="l in LEVELS" :key="l.code">
          <span class="tag" :class="l.tag" style="width:64px;text-align:center">{{ l.name }}</span>
          <span class="lv-range num">{{ l.range }}</span>
          <span class="lv-use">{{ l.use }}</span>
        </div>
        <div class="kv" style="margin-top:10px">
          <span class="k">高相似阈值</span><span class="num">≥ {{ rule.highThreshold || 80 }}</span>
          <span class="k">中相似阈值</span><span class="num">≥ {{ rule.midThreshold || 60 }}</span>
          <span class="k">统计窗口</span><span class="num">{{ rule.windowDays || 30 }} 天</span>
        </div>
        <div class="use">用途：相似度总分按阈值划档，高相似进入共性问题组并推动整改，中相似人工关注，低相似仅留档。</div>
      </div>

      <!-- 消息类型 -->
      <div class="card dict-card">
        <div class="card-h">
          <h2>消息类型字典</h2>
          <span class="sub">{{ msgTypes.length }} 项</span>
        </div>
        <div class="chips">
          <span v-for="m in msgTypes" :key="m" class="chip">
            <span class="chip-n">{{ m }}</span>
            <span class="chip-d">{{ MSG_DESC[m] || '站内提醒' }}</span>
          </span>
        </div>
        <div class="use">用途：站内消息按类型归集，决定顶栏未读角标与消息中心的分类筛选，并作为待办入口跳转依据。</div>
      </div>
    </div>

    <!-- 系统参数 -->
    <div class="card">
      <div class="card-h">
        <h2>系统参数</h2>
        <span class="sub">由严重级别定义派生 · 时限单位：小时</span>
      </div>
      <el-table :data="paramRows" v-loading="loading" style="width:100%">
        <el-table-column label="级别" width="90">
          <template #default="{ row }">
            <span class="tag" :class="levelTag(row.levelCode)">{{ row.levelCode }} {{ row.levelName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="响应时限" width="120" align="right">
          <template #default="{ row }"><span class="num">{{ row.responseHours }} h</span></template>
        </el-table-column>
        <el-table-column label="处理时限" width="120" align="right">
          <template #default="{ row }"><span class="num">{{ row.handleHours }} h</span></template>
        </el-table-column>
        <el-table-column label="升级时限" width="120" align="right">
          <template #default="{ row }"><span class="num">{{ row.escalateHours }} h</span></template>
        </el-table-column>
        <el-table-column label="预警比例" width="120" align="right">
          <template #default="{ row }"><span class="num">{{ row.warnRatio }}%</span></template>
        </el-table-column>
        <el-table-column label="重点关注" width="100">
          <template #default="{ row }">
            <span class="tag" :class="row.focusFlag ? 't-red' : 't-gray'">{{ row.focusFlag ? '是' : '否' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="300" show-overflow-tooltip />
        <template #empty><div style="padding:26px;color:var(--text-sub)">暂无系统参数</div></template>
      </el-table>
      <div class="use" style="margin-top:12px">
        参数说明：响应/处理时限在建单时写入工单的响应截止与完成截止时间；已用时限达到「预警比例」时进入时限预警中心；
        超过升级时限仍无进展时按级别推送给上级并计入考核。
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import AdminLayout from '../layouts/AdminLayout.vue'
import { api } from '../api'
import { statusTag, levelTag } from '../utils/format'

const loading = ref(false)
const channels = ref([])
const statuses = ref([])
const msgTypes = ref([])
const paramRows = ref([])
const rule = reactive({ ruleVersion: '', highThreshold: 80, midThreshold: 60, windowDays: 30 })

const CHANNEL_DESC = {
  电话: '客服热线来电，需即时记录并转受理',
  门店: '门店前台代客登记，常见于到店投诉',
  邮件: '邮件来函，附件证据需人工核对',
  在线: '官网/APP 在线提交，自动带入客户信息',
  微信: '公众号/小程序入口，支持图片上传'
}

const STATUS_DESC = {
  待受理: '建单完成，等待客服核实分派',
  已分派: '已指定责任部门与处理人',
  处理中: '处理人已接单并提交处理方案',
  待审核: '方案提交后等待组长审核',
  待客户确认: '方案已通过，等待客户确认结果',
  待回访: '客户已确认，等待回访评价',
  已归档: '回访完成，工单闭环归档'
}

const MSG_DESC = {
  待处理: '新工单分派到处理人',
  待审核: '处理方案待审核',
  待验收: '整改任务待验收',
  时限预警: '接近或超过 SLA 时限',
  待回访: '工单待回访评价',
  系统通知: '系统级公告与配置变更'
}

const LEVELS = [
  { code: 'HIGH', name: '高相似', range: '≥ 80 分', tag: 't-red', use: '进入共性问题组，触发整改任务' },
  { code: 'MID', name: '中相似', range: '60 – 79 分', tag: 't-amber', use: '纳入观察，需人工复核确认' },
  { code: 'LOW', name: '低相似', range: '< 60 分', tag: 't-gray', use: '仅留档，不参与共性归集' }
]

async function load() {
  loading.value = true
  try {
    const [o, r, msg] = await Promise.all([
      api.options(),
      api.clusterRule().catch(() => null),
      api.messagePage({ page: 1, size: 50 }).catch(() => null)
    ])
    channels.value = o?.channels || []
    statuses.value = o?.statuses || []
    const sev = o?.severityRules || []
    const sla = o?.slaRules || []
    paramRows.value = sev.map((s) => {
      const hit = sla.find((x) => x.levelCode === s.levelCode)
      const ratio = hit && hit.warnRatio != null ? Math.round(Number(hit.warnRatio) * 100) : 80
      return {
        levelCode: s.levelCode, levelName: s.levelName,
        responseHours: s.responseHours, handleHours: s.handleHours, escalateHours: s.escalateHours,
        warnRatio: ratio, focusFlag: s.focusFlag, description: s.description
      }
    })
    if (r?.active) Object.assign(rule, r.active)
    const types = (msg?.records || []).map((m) => m.msgType).filter(Boolean)
    const uniq = [...new Set(types)]
    msgTypes.value = uniq.length ? uniq : Object.keys(MSG_DESC)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.dict-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.dict-card { display: flex; flex-direction: column; }
.chips { display: flex; flex-wrap: wrap; gap: 8px; }
.chip {
  display: flex; flex-direction: column; gap: 1px; padding: 6px 10px; border-radius: 6px;
  background: #F7F9FA; border: 1px solid var(--line); min-width: 132px;
}
.chip-n { font-size: 13px; font-weight: 600; }
.chip-d { font-size: 11px; color: var(--text-sub); }
.status-line { display: flex; flex-wrap: wrap; align-items: center; gap: 4px; margin-bottom: 10px; }
.st { display: inline-flex; align-items: center; gap: 4px; }
.arw { color: var(--line); font-size: 12px; }
.status-desc { display: grid; grid-template-columns: 1fr 1fr; gap: 4px 12px; }
.sd { display: flex; align-items: center; gap: 6px; font-size: 12px; color: var(--text-sub); line-height: 22px; }
.level-line { display: flex; align-items: center; gap: 10px; font-size: 12px; line-height: 26px; }
.lv-range { color: var(--text-sub); width: 88px; }
.lv-use { color: var(--text-sub); }
.use {
  margin-top: auto; padding-top: 10px; font-size: 12px; color: var(--text-sub); line-height: 19px;
  border-top: 1px dashed var(--line);
}
@media (max-width: 1100px) {
  .dict-grid { grid-template-columns: 1fr; }
  .status-desc { grid-template-columns: 1fr; }
}
</style>
