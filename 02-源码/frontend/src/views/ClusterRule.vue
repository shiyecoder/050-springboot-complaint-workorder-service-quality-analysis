<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>聚类规则配置</h1>
        <div class="desc">六因子权重与相似度档位阈值决定共性问题组的判定结果，每次保存生成新版本，历史计算结果仍挂在原版本上可追溯</div>
      </div>
      <div class="acts">
        <el-button size="small" @click="$router.push('/cluster')">返回分析</el-button>
        <el-button size="small" @click="resetForm">恢复当前值</el-button>
        <el-button size="small" type="primary" :disabled="!canSave" :loading="saving" @click="save">保存并生成新版本</el-button>
      </div>
    </div>

    <!-- 当前生效版本 -->
    <div class="card active-card">
      <div class="ac-left">
        <span class="ac-badge">当前生效</span>
        <div class="ac-ver">{{ active.ruleVersion || '—' }}</div>
        <div class="ac-name">{{ active.ruleName || '结构化共性问题聚类规则' }}</div>
      </div>
      <div class="ac-mid">
        <div class="ac-kv"><span>权重合计</span><b class="num">{{ sumW }}%</b></div>
        <div class="ac-kv"><span>高相似阈值</span><b class="num">≥ {{ active.highThreshold || 80 }}</b></div>
        <div class="ac-kv"><span>中相似阈值</span><b class="num">≥ {{ active.midThreshold || 60 }}</b></div>
        <div class="ac-kv"><span>统计窗口</span><b class="num">{{ active.windowDays || 30 }} 天</b></div>
      </div>
      <div class="ac-right">
        <div class="ac-kv"><span>生效时间</span><b class="num">{{ fmtTime(active.effectiveTime) }}</b></div>
        <div class="ac-kv"><span>操作人</span><b>{{ active.operatorName || '—' }}</b></div>
        <div class="ac-kv"><span>变更说明</span><b>{{ active.remark || '—' }}</b></div>
      </div>
    </div>

    <div class="rule-grid">
      <!-- 左：权重与阈值 -->
      <div class="card" style="margin-bottom:0">
        <div class="card-h">
          <h2>六因子权重</h2>
          <span class="sub">各项加权后求和即为相似度总分（0–100）</span>
        </div>

        <div class="w-row" v-for="f in FACTORS" :key="f.key">
          <div class="w-label">
            <span>{{ f.label }}</span>
            <span class="w-desc">{{ f.desc }}</span>
          </div>
          <el-input-number v-model="form[f.key]" :min="0" :max="100" :step="5" controls-position="right" style="width:110px" />
          <span class="w-unit">%</span>
          <span class="w-bar"><i :style="{ width: Math.min(100, form[f.key]) + '%' }"></i></span>
        </div>

        <div class="sum-line" :class="sumW === 100 ? 'ok' : 'err'">
          <span>权重合计</span>
          <b class="num">{{ sumW }}%</b>
          <span class="hint">{{ sumW === 100 ? '配置合法' : `必须等于 100%，当前 ${sumW}%` }}</span>
        </div>

        <div class="card-h" style="margin-top:18px">
          <h2>档位阈值与统计窗口</h2>
        </div>
        <el-form label-width="112px" label-position="left">
          <el-form-item label="高相似阈值">
            <el-input-number v-model="form.highThreshold" :min="60" :max="99" :step="1" controls-position="right" style="width:110px" />
            <span class="f-tip">总分 ≥ 该值判定为「高相似」，进入共性问题组</span>
          </el-form-item>
          <el-form-item label="中相似阈值">
            <el-input-number v-model="form.midThreshold" :min="30" :max="98" :step="1" controls-position="right" style="width:110px" />
            <span class="f-tip">介于两阈值之间判定为「中相似」，需人工关注</span>
          </el-form-item>
          <el-form-item label="统计窗口天数">
            <el-input-number v-model="form.windowDays" :min="7" :max="90" :step="1" controls-position="right" style="width:110px" />
            <span class="f-tip">频次因子只统计该窗口内的同族工单</span>
          </el-form-item>
          <el-form-item label="变更说明">
            <el-input v-model="form.remark" maxlength="60" show-word-limit placeholder="不少于 5 个字，将随版本一起留痕" />
          </el-form-item>
        </el-form>

        <div class="desc-block" style="margin-top:4px">
          生效说明：保存后新版本立即生效，旧版本自动置为「停用」；已有的相似度计算结果不会被重算，
          仍保留原规则版本号与计算快照，用于事后追溯与解释。
        </div>

        <div v-if="warnText" class="warn-line">{{ warnText }}</div>
      </div>

      <!-- 右：版本历史 -->
      <div class="card" style="margin-bottom:0">
        <div class="card-h">
          <h2>版本历史</h2>
          <span class="sub">共 {{ versions.length }} 个版本</span>
        </div>
        <el-table :data="versions" v-loading="loading" height="560" style="width:100%"
                  :row-class-name="verRowClass">
          <el-table-column label="版本" width="66">
            <template #default="{ row }">
              <b :style="{ color: row.status === '启用' ? 'var(--secondary)' : '' }">{{ row.ruleVersion }}</b>
            </template>
          </el-table-column>
          <el-table-column label="六因子权重" min-width="150">
            <template #default="{ row }">
              <span class="num w-mini">{{ row.wProduct }}/{{ row.wStage }}/{{ row.wProblem }}/{{ row.wReason }}/{{ row.wDept }}/{{ row.wFreq }}</span>
            </template>
          </el-table-column>
          <el-table-column label="高/中阈值" width="92">
            <template #default="{ row }"><span class="num">{{ row.highThreshold }} / {{ row.midThreshold }}</span></template>
          </el-table-column>
          <el-table-column label="窗口" width="64">
            <template #default="{ row }"><span class="num">{{ row.windowDays }} 天</span></template>
          </el-table-column>
          <el-table-column label="生效时间" width="132">
            <template #default="{ row }"><span class="num" style="font-size:12px">{{ fmtTime(row.effectiveTime) }}</span></template>
          </el-table-column>
          <el-table-column prop="operatorName" label="操作人" width="82" />
          <el-table-column label="状态" width="72">
            <template #default="{ row }">
              <span class="tag" :class="row.status === '启用' ? 't-green' : 't-gray'">{{ row.status }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
        </el-table>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import AdminLayout from '../layouts/AdminLayout.vue'
import { api } from '../api'
import { fmtTime } from '../utils/format'

const FACTORS = [
  { key: 'wProduct', label: '关联产品', desc: '产品型号一致度（默认 25）' },
  { key: 'wStage', label: '服务环节', desc: '所处服务阶段一致度（默认 15）' },
  { key: 'wProblem', label: '问题类型', desc: '投诉问题归类一致度（默认 20）' },
  { key: 'wReason', label: '原因标签', desc: '根因标签重合度（默认 15）' },
  { key: 'wDept', label: '责任部门', desc: '责任部门一致度（默认 10）' },
  { key: 'wFreq', label: '近30天频次', desc: '窗口内同族工单密度（默认 15）' }
]

const loading = ref(false)
const saving = ref(false)
const versions = ref([])
const active = reactive({})
const form = reactive({
  wProduct: 25, wStage: 15, wProblem: 20, wReason: 15, wDept: 10, wFreq: 15,
  highThreshold: 80, midThreshold: 60, windowDays: 30, remark: ''
})

const sumW = computed(() => FACTORS.reduce((a, f) => a + Number(form[f.key] || 0), 0))
const warnText = computed(() => {
  if (sumW.value !== 100) return `权重合计为 ${sumW.value}%，必须等于 100% 才能保存`
  if (Number(form.highThreshold) <= Number(form.midThreshold)) return '高相似阈值必须大于中相似阈值'
  if (!form.remark || form.remark.trim().length < 5) return '变更说明不少于 5 个字'
  return ''
})
const canSave = computed(() => !warnText.value)

function verRowClass({ row }) {
  return row.status === '启用' ? 'on-row' : ''
}

function fillForm(src) {
  if (!src) return
  FACTORS.forEach((f) => { form[f.key] = Number(src[f.key] ?? form[f.key]) })
  form.highThreshold = Number(src.highThreshold ?? 80)
  form.midThreshold = Number(src.midThreshold ?? 60)
  form.windowDays = Number(src.windowDays ?? 30)
}

function resetForm() {
  fillForm(active)
  form.remark = ''
  ElMessage.info('已恢复为当前生效版本的配置')
}

async function load() {
  loading.value = true
  try {
    const d = await api.clusterRule()
    Object.assign(active, d?.active || {})
    versions.value = d?.versions || []
    fillForm(active)
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!canSave.value) {
    ElMessage.warning(warnText.value)
    return
  }
  saving.value = true
  try {
    const r = await api.saveClusterRule({
      wProduct: form.wProduct, wStage: form.wStage, wProblem: form.wProblem,
      wReason: form.wReason, wDept: form.wDept, wFreq: form.wFreq,
      highThreshold: form.highThreshold, midThreshold: form.midThreshold,
      windowDays: form.windowDays, remark: form.remark.trim()
    })
    ElMessage.success(`已生成新版本 ${r?.ruleVersion || ''}`)
    form.remark = ''
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.active-card {
  display: grid; grid-template-columns: 1.1fr 1fr 1.2fr; gap: 18px; align-items: center;
  border-left: 3px solid var(--secondary); background: linear-gradient(90deg, #F2F7F9 0%, #FFFFFF 62%);
}
.ac-left { display: flex; flex-direction: column; gap: 2px; }
.ac-badge {
  display: inline-block; width: fit-content; font-size: 11px; padding: 0 7px; height: 19px; line-height: 19px;
  border-radius: 4px; background: #E6EEF3; color: #2A5B6B; margin-bottom: 4px;
}
.ac-ver { font-size: 22px; font-weight: 600; color: var(--primary); line-height: 28px; }
.ac-name { font-size: 12px; color: var(--text-sub); }
.ac-mid, .ac-right { display: grid; grid-template-columns: 1fr 1fr; gap: 6px 16px; }
.ac-kv { font-size: 12px; color: var(--text-sub); display: flex; flex-direction: column; gap: 1px; }
.ac-kv b { font-size: 13px; color: var(--text); font-weight: 600; }

.rule-grid { display: grid; grid-template-columns: 1fr 1.15fr; gap: 14px; align-items: start; }
.w-row { display: grid; grid-template-columns: 1fr 110px 18px 1fr; gap: 8px; align-items: center; margin-bottom: 8px; }
.w-label { font-size: 13px; display: flex; flex-direction: column; }
.w-desc { font-size: 11px; color: var(--text-sub); }
.w-unit { font-size: 12px; color: var(--text-sub); }
.w-bar { height: 8px; border-radius: 4px; background: var(--tint); overflow: hidden; }
.w-bar i { display: block; height: 100%; background: var(--secondary); transition: width .2s; }
.sum-line {
  display: flex; align-items: center; gap: 10px; font-size: 12px; padding: 8px 12px; border-radius: 6px; margin-top: 4px;
}
.sum-line b { font-size: 16px; }
.sum-line.ok { background: #E4F0EA; color: #2B7955; }
.sum-line.err { background: #F8E5E7; color: #B02A37; }
.sum-line .hint { font-size: 11px; opacity: .85; }
.f-tip { font-size: 11px; color: var(--text-sub); margin-left: 10px; }
.warn-line { margin-top: 10px; font-size: 12px; color: var(--danger); }
.w-mini { font-size: 12px; letter-spacing: .3px; }
:deep(.on-row) { background: #F2F7F9 !important; }
@media (max-width: 1200px) {
  .rule-grid { grid-template-columns: 1fr; }
  .active-card { grid-template-columns: 1fr; }
}
</style>
