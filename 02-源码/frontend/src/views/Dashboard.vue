<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>服务质量驾驶舱</h1>
        <div class="desc">近 {{ period }} 天投诉受理、处理时效与共性问题的整体视图</div>
      </div>
      <div class="acts">
        <el-radio-group v-model="period" size="small" @change="load">
          <el-radio-button :value="7">近7天</el-radio-button>
          <el-radio-button :value="30">近30天</el-radio-button>
          <el-radio-button :value="90">近90天</el-radio-button>
        </el-radio-group>
        <el-button size="small" @click="load">刷新</el-button>
      </div>
    </div>

    <!-- 6 个核心 KPI -->
    <div class="kpis">
      <div class="kpi">
        <div class="lb"><span>投诉总量</span><span>件</span></div>
        <div class="vl num">{{ k.total }}</div>
        <div class="ft">平均响应 {{ k.respHours || 0 }} 小时</div>
      </div>
      <div class="kpi" :class="{ warn: k.waitAccept > 0 }">
        <div class="lb"><span>待受理</span><span>件</span></div>
        <div class="vl num">{{ k.waitAccept || 0 }}</div>
        <div class="ft">尚未进入流转</div>
      </div>
      <div class="kpi">
        <div class="lb"><span>处理中</span><span>件</span></div>
        <div class="vl num">{{ k.handling || 0 }}</div>
        <div class="ft">含分派待接单</div>
      </div>
      <div class="kpi" :class="{ warn: k.soonDue > 0 }">
        <div class="lb"><span>即将超时</span><span>件</span></div>
        <div class="vl num">{{ k.soonDue || 0 }}</div>
        <div class="ft">剩余不足 20% 时限</div>
      </div>
      <div class="kpi" :class="{ bad: k.overdue > 0 }">
        <div class="lb"><span>已超期</span><span>件</span></div>
        <div class="vl num">{{ k.overdue || 0 }}</div>
        <div class="ft">需升级处理</div>
      </div>
      <div class="kpi good">
        <div class="lb"><span>满意度</span><span>分</span></div>
        <div class="vl num">{{ k.satisfaction || 0 }}</div>
        <div class="ft">按时解决 {{ k.onTimeRate || 0 }}% · 一次解决 {{ k.firstSolveRate || 0 }}%</div>
      </div>
    </div>

    <!-- 7 个图表，两行铺满一屏 -->
    <div class="dash-grid">
      <div class="chart-card" style="grid-column: span 2">
        <div class="ch"><span style="font-size:13px;font-weight:600">投诉受理趋势</span><span>单位：件</span></div>
        <Chart :option="optTrend" />
      </div>
      <div class="chart-card">
        <div class="ch"><span style="font-size:13px;font-weight:600">流转状态分布</span></div>
        <Chart :option="optStatus" />
      </div>
      <div class="chart-card">
        <div class="ch"><span style="font-size:13px;font-weight:600">受理渠道构成</span></div>
        <Chart :option="optChannel" />
      </div>
      <div class="chart-card">
        <div class="ch"><span style="font-size:13px;font-weight:600">投诉类别 TOP6</span></div>
        <Chart :option="optCategory" />
      </div>
      <div class="chart-card">
        <div class="ch"><span style="font-size:13px;font-weight:600">责任部门工单量</span></div>
        <Chart :option="optDept" />
      </div>
      <div class="chart-card">
        <div class="ch"><span style="font-size:13px;font-weight:600">共性问题相似度分布</span></div>
        <Chart :option="optCluster" />
      </div>
      <div class="chart-card">
        <div class="ch"><span style="font-size:13px;font-weight:600">回访满意度分布</span></div>
        <Chart :option="optSat" />
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import AdminLayout from '../layouts/AdminLayout.vue'
import Chart from '../components/Chart.vue'
import { api } from '../api'

const period = ref(30)
const k = reactive({})
const d = reactive({ trend: null, statusDist: null, categoryTop: null, deptRank: null, channel: null, clusterDist: null, satisfaction: null })

const C = ['#356A7A', '#456C7E', '#8AA6B2', '#B4C0CC', '#19324A', '#2B7955', '#8A6117']
const AXIS = { axisLine: { lineStyle: { color: '#DEE4E9' } }, axisLabel: { color: '#5B6A76', fontSize: 11 }, splitLine: { lineStyle: { color: '#EDF1F4' } } }

function pie(src, radius = ['42%', '66%']) {
  const labels = src?.labels || []
  const values = src?.values || []
  if (!values.length) return {}
  return {
    color: C,
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', right: 0, top: 'center', itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 11, color: '#5B6A76' } },
    series: [{
      type: 'pie', radius, center: ['36%', '52%'], avoidLabelOverlap: true,
      label: { show: false }, labelLine: { show: false },
      data: labels.map((n, i) => ({ name: n, value: values[i] }))
    }]
  }
}

function bar(src, horizontal = false) {
  const labels = src?.labels || []
  const values = src?.values || []
  if (!values.length) return {}
  const cat = { type: 'category', data: labels, ...AXIS, axisLabel: { ...AXIS.axisLabel, interval: 0, width: 96, overflow: 'truncate' } }
  const val = { type: 'value', ...AXIS }
  return {
    color: ['#356A7A'],
    tooltip: { trigger: 'axis' },
    grid: { left: 8, right: 16, top: 10, bottom: 4, containLabel: true },
    xAxis: horizontal ? val : cat,
    yAxis: horizontal ? cat : val,
    series: [{ type: 'bar', data: values, barMaxWidth: horizontal ? 12 : 20, itemStyle: { borderRadius: horizontal ? [0, 3, 3, 0] : [3, 3, 0, 0] } }]
  }
}

const optTrend = computed(() => {
  const s = d.trend
  if (!s || !s.values?.length) return {}
  return {
    color: ['#356A7A'],
    tooltip: { trigger: 'axis' },
    grid: { left: 8, right: 12, top: 12, bottom: 4, containLabel: true },
    xAxis: { type: 'category', data: s.labels, ...AXIS, boundaryGap: false },
    yAxis: { type: 'value', ...AXIS },
    series: [{
      type: 'line', data: s.values, smooth: true, symbolSize: 5,
      areaStyle: { color: 'rgba(53,106,122,.14)' }, lineStyle: { width: 2 }
    }]
  }
})
const optStatus = computed(() => pie(d.statusDist))
const optChannel = computed(() => pie(d.channel, ['40%', '62%']))
const optCategory = computed(() => bar(d.categoryTop, true))
const optDept = computed(() => bar(d.deptRank, true))
const optCluster = computed(() => pie(d.clusterDist))
const optSat = computed(() => {
  const s = d.satisfaction
  if (!s || !s.values?.length) return {}
  return {
    color: ['#456C7E'],
    tooltip: { trigger: 'axis' },
    grid: { left: 8, right: 12, top: 12, bottom: 4, containLabel: true },
    xAxis: { type: 'category', data: s.labels, ...AXIS },
    yAxis: { type: 'value', ...AXIS },
    series: [{ type: 'bar', data: s.values, barMaxWidth: 26, itemStyle: { borderRadius: [3, 3, 0, 0] } }]
  }
})

async function load() {
  const r = await api.dashboard(period.value)
  Object.assign(k, r.kpis || {})
  d.trend = r.trend
  d.statusDist = r.statusDist
  d.categoryTop = r.categoryTop
  d.deptRank = r.deptRank
  d.channel = r.channel
  d.clusterDist = r.clusterDist
  d.satisfaction = r.satisfaction
}
onMounted(load)
</script>
