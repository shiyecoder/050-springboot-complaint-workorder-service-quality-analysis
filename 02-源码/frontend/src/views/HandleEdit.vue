<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>提交处理方案</h1>
        <div class="desc">记录调查情况与解决方案，提交后工单进入「待审核」，由客服组长审核通过后转客户确认</div>
      </div>
      <div class="acts">
        <el-button size="small" @click="$router.push('/handle/desk')">返回处理台</el-button>
        <el-button size="small" type="primary" :loading="saving" @click="submit">提交方案</el-button>
      </div>
    </div>

    <div class="edit-grid">
      <div>
        <!-- 未带参数进入时，先从本人处理中的工单里选一条 -->
        <div class="card" v-if="!ticketId">
          <div class="card-h"><h2>选择工单</h2><span class="sub">仅「处理中」的工单可以提交方案</span></div>
          <el-select v-model="picked" filterable placeholder="选择本人处理中的工单" style="width:100%"
                     @change="onPick">
            <el-option v-for="o in candidates" :key="o.ticketId"
                       :label="`${o.orderNo} · ${o.title || ''}`" :value="o.ticketId" />
          </el-select>
          <div class="empty-d" v-if="!candidates.length">
            本人暂无处理中的工单：请先在「我的处理台」接单，或由客服完成分派后回来提交方案
          </div>
        </div>

        <div class="card" v-if="ticketId">
          <div class="card-h">
            <h2>处理方案</h2>
            <span class="sub" v-if="tk">{{ tk.ticketNo }}</span>
          </div>

          <el-form :model="form" label-width="96px" label-position="right" size="default">
            <el-form-item label="处理方案 *">
              <el-input v-model="form.solution" type="textarea" :rows="6" maxlength="800" show-word-limit
                        placeholder="写明原因定位、处理措施、更换配件、客户沟通结论等，不少于 20 个字" />
              <div class="hint" :class="{ bad: form.solution.length > 0 && form.solution.length < 20 }">
                已输入 {{ form.solution.length }} 字{{ form.solution.length < 20 ? '，建议不少于 20 字以便审核与知识沉淀' : '' }}
              </div>
            </el-form-item>

            <el-form-item label="处理结果说明">
              <el-input v-model="form.result" type="textarea" :rows="3" maxlength="300" show-word-limit
                        placeholder="例如：已更换滤芯，现场测试正常，客户确认可以结案" />
            </el-form-item>

            <el-form-item label="计划完成时间">
              <el-date-picker v-model="form.planFinishTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss"
                              placeholder="承诺客户的处理完成时间" style="width:100%" />
            </el-form-item>

            <el-form-item label="需协同处理">
              <div style="width:100%">
                <div class="coop-row" v-for="(c, i) in form.coops" :key="i">
                  <el-select v-model="c.deptId" placeholder="协办部门" style="width:160px">
                    <el-option v-for="d in opt.depts" :key="d.id" :label="d.orgName" :value="d.id" />
                  </el-select>
                  <el-input v-model="c.content" placeholder="协办任务内容，例如：协助核实物流破损责任" />
                  <el-button link type="danger" @click="form.coops.splice(i, 1)">删除</el-button>
                </div>
                <el-button size="small" @click="addCoop">
                  <el-icon><Plus /></el-icon>添加协同任务
                </el-button>
                <div class="hint">协同需求会随方案一并记录在工单处理记录中，便于后续追溯</div>
              </div>
            </el-form-item>

            <el-form-item label="处理附件">
              <div style="width:100%">
                <el-upload action="/api/file/upload" name="file" :headers="uploadHeaders" :show-file-list="false"
                           accept=".jpg,.jpeg,.png,.webp,.pdf" :before-upload="beforeUpload"
                           :on-success="onUploaded" :on-error="onUploadError">
                  <el-button size="small"><el-icon><Upload /></el-icon>上传附件</el-button>
                </el-upload>
                <div class="att" v-if="form.attachPath">
                  <a :href="form.attachPath" target="_blank" rel="noopener">{{ form.attachPath }}</a>
                  <el-button link type="danger" @click="form.attachPath = ''">移除</el-button>
                </div>
              </div>
            </el-form-item>

            <div class="foot">
              <el-button @click="$router.push('/handle/desk')">取消</el-button>
              <el-button type="primary" :loading="saving" @click="submit">提交并转审核</el-button>
            </div>
          </el-form>
        </div>
      </div>

      <!-- 右侧：当前工单摘要，避免来回切页确认信息 -->
      <div class="card" v-if="tk">
        <div class="card-h"><h2>工单摘要</h2><span class="tag" :class="statusTag(tk.status)">{{ tk.status }}</span></div>
        <div class="kv">
          <div class="k">客诉编号</div><div class="num">{{ tk.ticketNo }}</div>
          <div class="k">投诉标题</div><div>{{ tk.title }}</div>
          <div class="k">客户</div><div>{{ tk.customerName || '—' }}</div>
          <div class="k">联系电话</div><div class="num">{{ tk.phone || '—' }}</div>
          <div class="k">投诉对象</div><div>{{ tk.objectName || '—' }}</div>
          <div class="k">类别</div><div>{{ tk.categoryName || '—' }}</div>
          <div class="k">级别</div>
          <div><span class="tag" :class="levelTag(tk.levelCode)">{{ tk.levelCode }}</span></div>
          <div class="k">要求完成</div><div class="num">{{ fmtTime(tk.deadlineTime) }}</div>
          <div class="k">剩余时间</div>
          <div class="num" :style="{ color: isOverdue(tk.deadlineTime, tk.finishTime) ? 'var(--danger)' : '' }">
            {{ remainText }}
          </div>
        </div>
        <div class="sub-title">客户诉求</div>
        <div class="desc-block">{{ tk.demand || '客户未填写明确诉求' }}</div>
        <div class="sub-title">投诉内容</div>
        <div class="desc-block">{{ tk.content || '暂无详细描述' }}</div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Upload } from '@element-plus/icons-vue'
import AdminLayout from '../layouts/AdminLayout.vue'
import { api } from '../api'
import { fmtTime, fmtGap, statusTag, levelTag, isOverdue } from '../utils/format'

const route = useRoute()
const router = useRouter()
const opt = reactive({ depts: [] })
const saving = ref(false)
const ticketId = ref(null)
const picked = ref(null)
const candidates = ref([])
const detail = ref(null)
// tk 只取工单主体，模板里直接用 tk.ticketNo 等字段
const tk = computed(() => detail.value?.ticket || null)

const form = reactive({ solution: '', result: '', planFinishTime: '', attachPath: '', coops: [] })

const uploadHeaders = computed(() => ({ Authorization: `Bearer ${localStorage.getItem('token') || ''}` }))
const remainText = computed(() =>
  tk.value ? fmtGap(tk.value.deadlineTime, fmtTime(new Date())) : '—'
)

function addCoop() {
  form.coops.push({ deptId: null, content: '' })
}

function beforeUpload(file) {
  if (file.size > 20 * 1024 * 1024) {
    ElMessage.warning('单个文件不能超过 20MB')
    return false
  }
  return true
}

function onUploaded(res) {
  if (res?.code === 200 && res.data?.url) {
    form.attachPath = res.data.url
    ElMessage.success('附件上传成功')
  } else {
    ElMessage.error(res?.message || '附件上传失败')
  }
}

function onUploadError() {
  ElMessage.error('附件上传失败，请稍后重试')
}

async function loadDetail(id) {
  ticketId.value = id
  detail.value = await api.ticketDetail(id)
  // 默认计划完成时间取工单要求完成时间，处理人可提前
  if (!form.planFinishTime && tk.value?.deadlineTime) form.planFinishTime = tk.value.deadlineTime
}

function onPick(id) {
  loadDetail(id)
}

async function submit() {
  if (!ticketId.value) return ElMessage.warning('请先选择要提交方案的工单')
  const sol = (form.solution || '').trim()
  if (sol.length < 20) return ElMessage.warning('处理方案请填写不少于 20 个字')
  if (tk.value?.status !== '处理中') {
    return ElMessage.warning(`当前工单状态为「${tk.value?.status || '未知'}」，只有处理中才能提交方案`)
  }

  // 协同需求没有独立接口，随处理结果一并落库，避免信息丢失
  const coopText = form.coops
    .filter((c) => c.deptId && (c.content || '').trim())
    .map((c) => {
      const dept = (opt.depts || []).find((d) => d.id === c.deptId)
      return `${dept?.orgName || '协办部门'}：${c.content.trim()}`
    })
    .join('；')
  const result = [form.result, coopText ? `【协同需求】${coopText}` : ''].filter(Boolean).join('；')

  saving.value = true
  try {
    // 后端要求同时提交「调查情况 content」与「解决方案 solution」，此处同源提交
    await api.submitHandle({
      ticketId: ticketId.value,
      content: sol,
      solution: sol,
      result: result || '问题已处理完毕，等待审核',
      planFinishTime: form.planFinishTime,
      attachPath: form.attachPath
    })
    ElMessage.success('处理方案已提交，等待审核')
    router.push('/handle/desk')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  const o = await api.options()
  Object.assign(opt, o)

  const id = Number(route.query.ticketId)
  if (id) {
    await loadDetail(id)
  } else {
    // 没带参数时，拉取本人处理台上的处理中工单供选择
    const desk = await api.handleDesk()
    candidates.value = (desk.handling || []).filter((o2) => o2.status === '处理中')
  }
})
</script>

<style scoped>
.edit-grid { display: grid; grid-template-columns: minmax(0, 1.6fr) minmax(0, 1fr); gap: 14px; align-items: start; }
.edit-grid .card { margin-bottom: 0; }
.hint { font-size: 11px; color: var(--text-sub); line-height: 18px; margin-top: 2px; }
.hint.bad { color: var(--warning); }
.coop-row { display: flex; gap: 8px; align-items: center; margin-bottom: 8px; }
.att { display: flex; align-items: center; gap: 10px; font-size: 12px; margin-top: 6px; }
.sub-title { font-size: 13px; font-weight: 600; margin: 12px 0 6px; }
.foot { display: flex; justify-content: flex-end; gap: 10px; padding-top: 8px; border-top: 1px dashed var(--line); }
.empty-d { font-size: 12px; color: var(--text-sub); margin-top: 10px; line-height: 20px; }

@media (max-width: 1200px) {
  .edit-grid { grid-template-columns: 1fr; }
}
</style>
