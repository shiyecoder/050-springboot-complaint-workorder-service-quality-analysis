<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>客户与对象档案</h1>
        <div class="desc">投诉关联的客户信息与产品对象档案，用于受理核实时快速确认历史服务与保修状态</div>
      </div>
      <div class="acts">
        <el-button size="small" @click="reload">刷新</el-button>
      </div>
    </div>

    <div class="card">
      <el-tabs v-model="tab" @tab-change="onTab">
        <!-- 客户档案 -->
        <el-tab-pane label="客户档案" name="customer">
          <div class="filters">
            <el-input v-model="cq.keyword" placeholder="客户编号 / 姓名 / 电话" clearable @keyup.enter="searchC" />
            <el-button type="primary" @click="searchC">查询</el-button>
            <el-button @click="resetC">重置</el-button>
            <span class="cnt-tip">共 {{ cTotal }} 位客户</span>
          </div>
          <el-table :data="cRows" v-loading="cLoading" style="width:100%">
            <el-table-column label="头像" width="62">
              <template #default="{ row }">
                <img v-if="row.avatar" class="ava" :src="row.avatar" alt="客户头像" />
                <span v-else class="ava-ph">{{ (row.customerName || '客')[0] }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="customerNo" label="客户编号" width="118" />
            <el-table-column prop="customerName" label="客户姓名" width="96" />
            <el-table-column prop="phone" label="联系电话" width="126" />
            <el-table-column label="类型" width="72">
              <template #default="{ row }">
                <span class="tag" :class="row.customerType === '企业' ? 't-blue' : 't-gray'">{{ row.customerType || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="region" label="区域" width="106" />
            <el-table-column label="客户等级" width="86">
              <template #default="{ row }">
                <span class="tag" :class="row.customerLevel === 'VIP' ? 't-amber' : 't-gray'">{{ row.customerLevel || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="email" label="邮箱" min-width="170" show-overflow-tooltip />
            <el-table-column prop="address" label="地址" min-width="180" show-overflow-tooltip />
            <el-table-column label="投诉次数" width="88" align="right">
              <template #default="{ row }"><b class="num">{{ row.ticketCount ?? 0 }}</b></template>
            </el-table-column>
            <el-table-column label="服务历史" min-width="210" show-overflow-tooltip>
              <template #default="{ row }">{{ row.serviceHistory || '—' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="72">
              <template #default="{ row }">
                <span class="tag" :class="row.status === '正常' ? 't-green' : 't-gray'">{{ row.status || '—' }}</span>
              </template>
            </el-table-column>
            <template #empty><div style="padding:26px;color:var(--text-sub)">暂无客户档案</div></template>
          </el-table>
          <div class="pager">
            <el-pagination background layout="total, sizes, prev, pager, next, jumper" :total="cTotal"
                           v-model:current-page="cq.page" v-model:page-size="cq.size" :page-sizes="[10, 20, 50]"
                           @current-change="loadC" @size-change="searchC" />
          </div>
        </el-tab-pane>

        <!-- 对象档案 -->
        <el-tab-pane label="对象档案" name="object">
          <div class="filters">
            <el-input v-model="oq.keyword" placeholder="对象编号 / 名称 / 型号" clearable @keyup.enter="searchO" />
            <el-button type="primary" @click="searchO">查询</el-button>
            <el-button @click="resetO">重置</el-button>
            <span class="cnt-tip">共 {{ oTotal }} 个对象</span>
          </div>
          <el-table :data="oRows" v-loading="oLoading" style="width:100%">
            <el-table-column label="图片" width="70">
              <template #default="{ row }">
                <img v-if="row.coverImage" class="thumb" :src="row.coverImage" alt="对象图片" />
                <span v-else style="color:var(--text-sub)">—</span>
              </template>
            </el-table-column>
            <el-table-column prop="objectNo" label="对象编号" width="118" />
            <el-table-column prop="objectName" label="对象名称" min-width="160" show-overflow-tooltip />
            <el-table-column label="类型" width="76">
              <template #default="{ row }">
                <span class="tag" :class="row.objectType === '产品' ? 't-blue' : 't-gray'">{{ row.objectType || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="brand" label="品牌" width="86" />
            <el-table-column prop="model" label="型号" width="104" />
            <el-table-column prop="storeName" label="购买门店" min-width="150" show-overflow-tooltip />
            <el-table-column label="购买日期" width="108">
              <template #default="{ row }"><span class="num">{{ fmtDate(row.purchaseDate) }}</span></template>
            </el-table-column>
            <el-table-column label="保修截止" width="108">
              <template #default="{ row }">
                <span class="num" :style="{ color: inWarranty(row.warrantyEnd) ? '' : 'var(--text-sub)' }">
                  {{ fmtDate(row.warrantyEnd) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="投诉次数" width="88" align="right">
              <template #default="{ row }"><b class="num">{{ row.ticketCount ?? 0 }}</b></template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <span class="tag" :class="row.status === '在用' ? 't-green' : row.status === '已过期' ? 't-amber' : 't-gray'">
                  {{ row.status || '—' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
            <template #empty><div style="padding:26px;color:var(--text-sub)">暂无对象档案</div></template>
          </el-table>
          <div class="pager">
            <el-pagination background layout="total, sizes, prev, pager, next, jumper" :total="oTotal"
                           v-model:current-page="oq.page" v-model:page-size="oq.size" :page-sizes="[10, 20, 50]"
                           @current-change="loadO" @size-change="searchO" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import AdminLayout from '../layouts/AdminLayout.vue'
import { api } from '../api'
import { fmtDate } from '../utils/format'

const tab = ref('customer')

const cLoading = ref(false)
const cRows = ref([])
const cTotal = ref(0)
const cq = reactive({ keyword: '', page: 1, size: 10 })

const oLoading = ref(false)
const oRows = ref([])
const oTotal = ref(0)
const oq = reactive({ keyword: '', page: 1, size: 10 })

function inWarranty(v) {
  if (!v) return false
  return new Date(String(v).replace(/-/g, '/')).getTime() > Date.now()
}

async function loadC() {
  cLoading.value = true
  try {
    const r = await api.customerPage({ keyword: cq.keyword || undefined, page: cq.page, size: cq.size })
    cRows.value = r.records || []
    cTotal.value = r.total || 0
  } finally {
    cLoading.value = false
  }
}
async function loadO() {
  oLoading.value = true
  try {
    const r = await api.objectPage({ keyword: oq.keyword || undefined, page: oq.page, size: oq.size })
    oRows.value = r.records || []
    oTotal.value = r.total || 0
  } finally {
    oLoading.value = false
  }
}

function searchC() { cq.page = 1; loadC() }
function searchO() { oq.page = 1; loadO() }
function resetC() { cq.keyword = ''; cq.page = 1; loadC() }
function resetO() { oq.keyword = ''; oq.page = 1; loadO() }

function onTab(name) {
  if (name === 'customer' && !cRows.value.length) loadC()
  if (name === 'object' && !oRows.value.length) loadO()
}

function reload() {
  if (tab.value === 'customer') loadC()
  else loadO()
}

onMounted(() => {
  loadC()
  loadO()
})
</script>

<style scoped>
.ava { width: 28px; height: 28px; border-radius: 50%; object-fit: cover; border: 1px solid var(--line); }
.ava-ph {
  display: inline-grid; place-items: center; width: 28px; height: 28px; border-radius: 50%;
  background: var(--tint); color: var(--text-sub); font-size: 12px;
}
.cnt-tip { font-size: 12px; color: var(--text-sub); margin-left: 4px; }
:deep(.el-tabs__header) { margin-bottom: 14px; }
</style>
