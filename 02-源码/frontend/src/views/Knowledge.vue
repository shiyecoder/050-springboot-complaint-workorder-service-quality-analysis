<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>知识库</h1>
        <div class="desc">把共性问题与典型处理经验沉淀为可检索的知识条目，处理人可据此快速复用标准做法</div>
      </div>
      <div class="acts">
        <el-button size="small" @click="load">刷新</el-button>
        <el-button size="small" type="primary" @click="openEdit()">新增知识</el-button>
      </div>
    </div>

    <div class="kb-wrap">
      <!-- 左侧分类统计 -->
      <div class="card kb-side">
        <div class="card-h"><h2>分类</h2></div>
        <div class="nav-i" :class="{ on: !q.category }" @click="pickCategory('')">
          <span>全部条目</span><span class="cnt num">{{ allRows.length }}</span>
        </div>
        <div v-for="c in cats" :key="c.name" class="nav-i" :class="{ on: q.category === c.name }" @click="pickCategory(c.name)">
          <span class="ellipsis" :title="c.name">{{ c.name }}</span><span class="cnt num">{{ c.cnt }}</span>
        </div>
        <div class="side-stat">
          <div class="ss"><span>条目总数</span><b class="num">{{ allRows.length }}</b></div>
          <div class="ss"><span>累计浏览</span><b class="num">{{ totalViews }}</b></div>
        </div>
      </div>

      <!-- 右侧列表 -->
      <div class="card" style="margin-bottom:0">
        <div class="filters">
          <el-input v-model="q.keyword" placeholder="标题 / 关键词" clearable @keyup.enter="search" />
          <el-select v-model="q.category" placeholder="全部分类" clearable @change="search">
            <el-option v-for="c in cats" :key="c.name" :label="c.name" :value="c.name" />
          </el-select>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </div>

        <el-table :data="rows" v-loading="loading" style="width:100%">
          <el-table-column prop="itemNo" label="编号" width="88" />
          <el-table-column label="标题" min-width="190">
            <template #default="{ row }">
              <el-link type="primary" @click="openView(row)">{{ row.title }}</el-link>
            </template>
          </el-table-column>
          <el-table-column prop="categoryName" label="分类" width="106" show-overflow-tooltip />
          <el-table-column label="关键词" min-width="150">
            <template #default="{ row }">
              <span v-for="k in keywords(row.keywords)" :key="k" class="tag t-blue" style="margin-right:4px">{{ k }}</span>
            </template>
          </el-table-column>
          <el-table-column label="来源工单" width="120">
            <template #default="{ row }">
              <el-link v-if="row.sourceTicketId" type="primary" @click="$router.push(`/ticket/detail/${row.sourceTicketId}`)">
                #{{ row.sourceTicketId }}
              </el-link>
              <span v-else style="color:var(--text-sub)">—</span>
            </template>
          </el-table-column>
          <el-table-column prop="authorName" label="作者" width="76" />
          <el-table-column label="浏览量" width="78" align="right">
            <template #default="{ row }"><span class="num">{{ row.viewCount ?? 0 }}</span></template>
          </el-table-column>
          <el-table-column label="状态" width="72">
            <template #default="{ row }">
              <span class="tag" :class="row.status === '启用' ? 't-green' : 't-gray'">{{ row.status }}</span>
            </template>
          </el-table-column>
          <el-table-column label="发布时间" width="132">
            <template #default="{ row }"><span class="num" style="font-size:12px">{{ fmtTime(row.publishTime) }}</span></template>
          </el-table-column>
          <el-table-column label="操作" width="104" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openView(row)">查看</el-button>
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            </template>
          </el-table-column>
          <template #empty><div style="padding:26px;color:var(--text-sub)">暂无知识条目</div></template>
        </el-table>

        <div class="pager">
          <el-pagination background layout="total, sizes, prev, pager, next, jumper" :total="total"
                         v-model:current-page="q.page" v-model:page-size="q.size" :page-sizes="[10, 20, 50]"
                         @current-change="load" @size-change="search" />
        </div>
      </div>
    </div>

    <!-- 查看全文 -->
    <el-drawer v-model="viewDrawer" title="知识条目" size="540px">
      <template v-if="cur">
        <div class="kb-title">{{ cur.title }}</div>
        <div class="kb-meta">
          <span class="tag t-gray">{{ cur.itemNo }}</span>
          <span class="tag t-blue">{{ cur.categoryName || '未分类' }}</span>
          <span class="tag" :class="cur.status === '启用' ? 't-green' : 't-gray'">{{ cur.status }}</span>
          <span>作者 {{ cur.authorName || '—' }}</span>
          <span class="num">浏览 {{ cur.viewCount ?? 0 }}</span>
        </div>
        <div class="kb-kw">
          <span v-for="k in keywords(cur.keywords)" :key="k" class="tag t-blue">{{ k }}</span>
          <span v-if="!keywords(cur.keywords).length" style="color:var(--text-sub);font-size:12px">无关键词</span>
        </div>
        <div class="kb-body">{{ cur.content || '—' }}</div>
        <div class="kv" style="margin-top:14px">
          <span class="k">发布时间</span><span class="num">{{ fmtTime(cur.publishTime) }}</span>
          <span class="k">来源工单</span>
          <span>{{ cur.sourceTicketId ? `#${cur.sourceTicketId}` : '—' }}</span>
        </div>
      </template>
    </el-drawer>

    <!-- 新增 / 编辑 -->
    <el-dialog v-model="editDialog" :title="form.id ? '编辑知识条目' : '新增知识条目'" width="620px">
      <el-form label-width="82px">
        <el-form-item label="标题">
          <el-input v-model="form.title" maxlength="60" show-word-limit placeholder="如：安装后管路渗水的处理规范" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryName" filterable allow-create placeholder="选择或输入分类" style="width:220px">
            <el-option v-for="c in cats" :key="c.name" :label="c.name" :value="c.name" />
          </el-select>
          <el-select v-model="form.status" style="width:120px;margin-left:8px">
            <el-option label="启用" value="启用" />
            <el-option label="停用" value="停用" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="form.keywords" placeholder="多个关键词用英文逗号分隔" />
        </el-form-item>
        <el-form-item label="正文">
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="适用场景 / 处理步骤 / 注意事项" />
        </el-form-item>
        <el-form-item label="来源工单">
          <el-input-number v-model="form.sourceTicketId" :min="0" :controls="false" placeholder="可选，工单 ID" style="width:150px" />
          <span class="f-tip">填工单 ID 可回溯到原始投诉</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </AdminLayout>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import AdminLayout from '../layouts/AdminLayout.vue'
import { api } from '../api'
import { fmtTime } from '../utils/format'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const allRows = ref([])
const total = ref(0)
const q = reactive({ keyword: '', category: '', page: 1, size: 10 })

const viewDrawer = ref(false)
const editDialog = ref(false)
const cur = ref(null)
const form = reactive({ id: null, title: '', categoryName: '售后服务', keywords: '', content: '', sourceTicketId: undefined, status: '启用' })

const cats = computed(() => {
  const m = new Map()
  allRows.value.forEach((x) => {
    const k = x.categoryName || '未分类'
    m.set(k, (m.get(k) || 0) + 1)
  })
  return [...m.entries()].map(([name, cnt]) => ({ name, cnt })).sort((a, b) => b.cnt - a.cnt)
})
const totalViews = computed(() => allRows.value.reduce((a, x) => a + (Number(x.viewCount) || 0), 0))

function keywords(v) {
  if (!v) return []
  return String(v).split(/[,，、;；]+/).map((x) => x.trim()).filter(Boolean)
}

async function load() {
  loading.value = true
  try {
    const r = await api.knowledgePage({ keyword: q.keyword || undefined, category: q.category || undefined, page: q.page, size: q.size })
    rows.value = r.records || []
    total.value = r.total || 0
  } finally {
    loading.value = false
  }
}

async function loadAll() {
  const r = await api.knowledgePage({ page: 1, size: 200 })
  allRows.value = r.records || []
}

function search() { q.page = 1; load() }
function reset() {
  q.keyword = ''
  q.category = ''
  q.page = 1
  load()
}
function pickCategory(name) {
  q.category = name
  search()
}

function openView(row) {
  cur.value = row
  viewDrawer.value = true
}

function openEdit(row) {
  if (row) {
    Object.assign(form, {
      id: row.id, title: row.title, categoryName: row.categoryName, keywords: row.keywords,
      content: row.content, sourceTicketId: row.sourceTicketId || undefined, status: row.status || '启用'
    })
  } else {
    Object.assign(form, { id: null, title: '', categoryName: cats.value[0]?.name || '售后服务', keywords: '', content: '', sourceTicketId: undefined, status: '启用' })
  }
  editDialog.value = true
}

async function submit() {
  if (!form.title || !form.title.trim()) {
    ElMessage.warning('请填写标题')
    return
  }
  saving.value = true
  try {
    await api.saveKnowledge({
      id: form.id || undefined, title: form.title.trim(), categoryName: form.categoryName,
      keywords: form.keywords, content: form.content, sourceTicketId: form.sourceTicketId, status: form.status
    })
    ElMessage.success(form.id ? '知识条目已更新' : '知识条目已创建')
    editDialog.value = false
    await Promise.all([load(), loadAll()])
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  load()
  loadAll()
})
</script>

<style scoped>
.kb-wrap { display: grid; grid-template-columns: 196px 1fr; gap: 14px; align-items: start; }
.kb-side { padding: 14px 12px; }
.kb-side .card-h { margin-bottom: 8px; padding: 0 4px; }
.nav-i {
  display: flex; align-items: center; justify-content: space-between; gap: 8px;
  padding: 6px 8px; border-radius: 5px; font-size: 13px; cursor: pointer; color: var(--text);
}
.nav-i:hover { background: var(--tint); }
.nav-i.on { background: #E6EEF3; color: #2A5B6B; font-weight: 600; }
.nav-i .cnt { font-size: 11px; color: var(--text-sub); }
.ellipsis { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.side-stat { margin-top: 12px; border-top: 1px solid var(--line); padding-top: 10px; }
.ss { display: flex; justify-content: space-between; font-size: 12px; color: var(--text-sub); line-height: 24px; }
.ss b { color: var(--text); font-size: 14px; }
.kb-title { font-size: 17px; font-weight: 600; line-height: 26px; }
.kb-meta { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; font-size: 12px; color: var(--text-sub); margin: 10px 0; }
.kb-kw { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 12px; }
.kb-body {
  background: var(--tint); border-radius: 6px; padding: 12px 14px; font-size: 13px; line-height: 24px;
  white-space: pre-wrap; word-break: break-word;
}
.f-tip { font-size: 11px; color: var(--text-sub); margin-left: 10px; }
</style>
