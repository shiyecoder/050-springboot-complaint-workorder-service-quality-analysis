<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>操作日志</h1>
        <div class="desc">记录关键操作的操作人、模块、动作与耗时，用于问题追溯与权限审计；本页按最近日志本地筛选</div>
      </div>
      <div class="acts">
        <el-button size="small" @click="load">刷新</el-button>
      </div>
    </div>

    <div class="card">
      <div class="filters">
        <el-input v-model="f.operator" placeholder="操作人" clearable @keyup.enter="apply" />
        <el-select v-model="f.module" placeholder="全部模块" clearable>
          <el-option v-for="m in modules" :key="m" :label="m" :value="m" />
        </el-select>
        <el-input v-model="f.action" placeholder="动作关键字" clearable @keyup.enter="apply" />
        <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期"
                        end-placeholder="结束日期" style="width:250px" />
        <el-button type="primary" @click="apply">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <div class="log-stat">
        <span>匹配记录 <b class="num">{{ filtered.length }}</b> 条</span>
        <span>失败 <b class="num" :style="{ color: failCnt ? 'var(--danger)' : '' }">{{ failCnt }}</b> 条</span>
        <span class="sub">数据范围：最近 {{ allRows.length }} 条操作日志</span>
      </div>

      <el-table :data="pageRows" v-loading="loading" style="width:100%">
        <el-table-column label="时间" width="142">
          <template #default="{ row }"><span class="num" style="font-size:12px">{{ fmtTime(row.createTime) }}</span></template>
        </el-table-column>
        <el-table-column prop="userName" label="操作人" width="92" />
        <el-table-column prop="module" label="模块" width="110" />
        <el-table-column prop="action" label="动作" min-width="150" show-overflow-tooltip />
        <el-table-column label="请求方法" min-width="230" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="mono">{{ row.method || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="参数" min-width="170" show-overflow-tooltip>
          <template #default="{ row }"><span class="mono">{{ row.params || '—' }}</span></template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="120" />
        <el-table-column label="结果" width="80">
          <template #default="{ row }">
            <span class="tag" :class="Number(row.status) === 1 ? 't-green' : 't-red'">
              {{ Number(row.status) === 1 ? '成功' : '失败' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="86" align="right">
          <template #default="{ row }">
            <span class="num" :style="{ color: Number(row.costMs) > 500 ? 'var(--warning)' : '' }">{{ row.costMs ?? '—' }} ms</span>
          </template>
        </el-table-column>
        <template #empty><div style="padding:26px;color:var(--text-sub)">暂无操作日志</div></template>
      </el-table>

      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next, jumper" :total="filtered.length"
                       v-model:current-page="page" v-model:page-size="size" :page-sizes="[10, 20, 50]"
                       @size-change="onSize" />
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import AdminLayout from '../layouts/AdminLayout.vue'
import { api } from '../api'
import { fmtTime } from '../utils/format'

const loading = ref(false)
const allRows = ref([])
const page = ref(1)
const size = ref(10)
const range = ref([])
const f = reactive({ operator: '', module: '', action: '' })

const modules = computed(() => [...new Set(allRows.value.map((x) => x.module).filter(Boolean))])

const filtered = computed(() => {
  const op = f.operator.trim()
  const act = f.action.trim()
  const from = range.value?.[0]
  const to = range.value?.[1]
  return allRows.value.filter((x) => {
    if (op && !String(x.userName || '').includes(op)) return false
    if (f.module && x.module !== f.module) return false
    if (act && !String(x.action || '').includes(act)) return false
    const day = String(x.createTime || '').slice(0, 10)
    if (from && day && day < from) return false
    if (to && day && day > to) return false
    return true
  })
})

const pageRows = computed(() => {
  const start = (page.value - 1) * size.value
  return filtered.value.slice(start, start + size.value)
})

const failCnt = computed(() => filtered.value.filter((x) => Number(x.status) !== 1).length)

async function load() {
  loading.value = true
  try {
    const r = await api.logPage({ page: 1, size: 100 })
    allRows.value = r.records || []
  } finally {
    loading.value = false
  }
}

function apply() { page.value = 1 }
function onSize() { page.value = 1 }
function reset() {
  f.operator = ''
  f.module = ''
  f.action = ''
  range.value = []
  page.value = 1
}

onMounted(load)
</script>

<style scoped>
.log-stat {
  display: flex; align-items: center; gap: 18px; font-size: 12px; color: var(--text-sub);
  background: var(--tint); border-radius: 6px; padding: 7px 12px; margin-bottom: 10px;
}
.log-stat b { color: var(--text); font-size: 13px; margin: 0 2px; }
.mono { font-family: "SF Mono", Menlo, Consolas, monospace; font-size: 12px; }
</style>
