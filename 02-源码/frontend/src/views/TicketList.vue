<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>投诉台账</h1>
        <div class="desc">全部投诉工单的统一检索入口，支持按状态、类别、责任部门与时效筛选</div>
      </div>
      <div class="acts">
        <el-button size="small" @click="doExport">导出当前筛选</el-button>
        <el-button size="small" type="primary" @click="$router.push('/intake/new')">新建投诉</el-button>
      </div>
    </div>

    <div class="card">
      <div class="filters">
        <el-input v-model="q.keyword" placeholder="编号 / 标题 / 客户 / 电话" clearable @keyup.enter="search" />
        <el-select v-model="q.status" placeholder="全部状态" clearable>
          <el-option v-for="s in opt.statuses" :key="s" :label="s" :value="s" />
        </el-select>
        <el-select v-model="q.categoryId" placeholder="全部类别" clearable>
          <el-option v-for="c in opt.categories" :key="c.id" :label="c.categoryName" :value="c.id" />
        </el-select>
        <el-select v-model="q.level" placeholder="全部级别" clearable>
          <el-option v-for="l in opt.levels" :key="l.levelCode" :label="`${l.levelCode} ${l.levelName}`" :value="l.levelCode" />
        </el-select>
        <el-select v-model="q.deptId" placeholder="全部责任部门" clearable>
          <el-option v-for="d2 in opt.depts" :key="d2.id" :label="d2.orgName" :value="d2.id" />
        </el-select>
        <el-select v-model="q.channel" placeholder="全部渠道" clearable style="width:150px">
          <el-option v-for="c in opt.channels" :key="c" :label="c" :value="c" />
        </el-select>
        <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期"
                        end-placeholder="结束日期" style="width:250px" />
        <el-checkbox v-model="q.overdue" :true-value="1" :false-value="undefined" label="仅看超期" />
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" style="width:100%" @row-dblclick="(r) => open(r)">
        <el-table-column prop="ticketNo" label="投诉编号" width="150">
          <template #default="{ row }"><el-link type="primary" @click="open(row)">{{ row.ticketNo }}</el-link></template>
        </el-table-column>
        <el-table-column label="图片" width="70">
          <template #default="{ row }">
            <img v-if="row.coverImage" class="thumb" :src="row.coverImage" alt="投诉图片" />
            <span v-else style="color:var(--text-sub)">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="投诉标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="客户 / 对象" min-width="150">
          <template #default="{ row }">
            <div>{{ row.customerName }}</div>
            <div style="font-size:11px;color:var(--text-sub)">{{ row.objectName || '—' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="类别" min-width="110" show-overflow-tooltip />
        <el-table-column label="级别" width="80">
          <template #default="{ row }">
            <span class="tag" :class="levelTag(row.levelCode)">{{ row.levelCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="channel" label="渠道" width="70" />
        <el-table-column label="状态" width="104">
          <template #default="{ row }"><span class="tag" :class="statusTag(row.status)">{{ row.status }}</span></template>
        </el-table-column>
        <el-table-column prop="deptName" label="责任部门" min-width="120" show-overflow-tooltip />
        <el-table-column prop="currentHandlerName" label="当前处理人" width="100" />
        <el-table-column label="要求完成" width="140">
          <template #default="{ row }">
            <span class="num" :style="{ color: isOverdue(row.deadlineTime, row.finishTime) ? 'var(--danger)' : '' }">
              {{ fmtTime(row.deadlineTime) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="标记" width="90">
          <template #default="{ row }">
            <span v-if="row.overdueFlag" class="tag t-red">超期</span>
            <span v-else-if="row.duplicateFlag" class="tag t-amber">重复</span>
            <span v-else-if="row.invalidFlag" class="tag t-gray">无效</span>
            <span v-else class="tag t-green">正常</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="open(row)">详情</el-button>
            <el-button v-if="row.status === '待受理'" link type="primary" @click="goDispatch(row)">分派</el-button>
          </template>
        </el-table-column>
        <template #empty><div style="padding:26px;color:var(--text-sub)">暂无符合条件的投诉</div></template>
      </el-table>

      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next, jumper" :total="total"
                       v-model:current-page="q.page" v-model:page-size="q.size"
                       :page-sizes="[10, 20, 50]" @current-change="load" @size-change="search" />
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AdminLayout from '../layouts/AdminLayout.vue'
import { api } from '../api'
import { fmtTime, statusTag, levelTag, isOverdue } from '../utils/format'

const router = useRouter()
const loading = ref(false)
const rows = ref([])
const total = ref(0)
const range = ref([])
const opt = reactive({ categories: [], levels: [], depts: [], channels: [], statuses: [] })
const q = reactive({ keyword: '', status: '', categoryId: null, level: '', deptId: null, channel: '', overdue: undefined, page: 1, size: 10 })

watch(range, (v) => { q.dateFrom = v?.[0]; q.dateTo = v?.[1] })

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
function search() { q.page = 1; load() }
function reset() {
  Object.assign(q, { keyword: '', status: '', categoryId: null, level: '', deptId: null, channel: '', overdue: undefined, page: 1 })
  range.value = []
  load()
}
function open(r) { router.push(`/ticket/detail/${r.id}`) }
function goDispatch(r) { router.push({ path: '/dispatch', query: { id: r.id } }) }
async function doExport() {
  const r = await api.exportTickets({ ...q, overdue: q.overdue || '' })
  ElMessage.success(`已生成导出文件：${r?.fileName || '投诉台账.xlsx'}（${r?.total || total.value} 条）`)
}

onMounted(async () => {
  const o = await api.options()
  Object.assign(opt, o)
  load()
})
</script>
