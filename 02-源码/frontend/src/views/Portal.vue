<template>
  <PortalLayout>
    <!-- 欢迎条：客户身份 + 服务承诺 -->
    <div class="welcome">
      <div>
        <div class="hi">您好，{{ store.user.realName || store.user.username }}</div>
        <div class="promise">
          我们承诺 2 小时内响应您的投诉、48 小时内给出处理方案，处理完成后会电话回访确认结果。
        </div>
      </div>
      <el-button type="primary" @click="$router.push('/portal/new')">
        <el-icon><Plus /></el-icon>提交新投诉
      </el-button>
    </div>

    <div class="kpis kpis-4">
      <div class="kpi">
        <div class="lb"><span>我的投诉总数</span><span>件</span></div>
        <div class="vl num">{{ stats.total }}</div>
        <div class="ft">含历史与处理中的投诉</div>
      </div>
      <div class="kpi" :class="{ warn: stats.processing > 0 }">
        <div class="lb"><span>处理中</span><span>件</span></div>
        <div class="vl num">{{ stats.processing }}</div>
        <div class="ft">已受理，正在处理</div>
      </div>
      <div class="kpi" :class="{ warn: stats.toConfirm > 0 }">
        <div class="lb"><span>待我确认</span><span>件</span></div>
        <div class="vl num">{{ stats.toConfirm }}</div>
        <div class="ft">请确认问题是否解决</div>
      </div>
      <div class="kpi good">
        <div class="lb"><span>已完成</span><span>件</span></div>
        <div class="vl num">{{ stats.done }}</div>
        <div class="ft">待回访与已归档</div>
      </div>
    </div>

    <div class="card">
      <div class="card-h">
        <h2>我的投诉</h2>
        <el-radio-group v-model="q.status" size="small" @change="search">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button v-for="s in opt.statuses" :key="s" :value="s">{{ s }}</el-radio-button>
        </el-radio-group>
      </div>

      <div v-loading="loading" class="plist">
        <div class="pcard" v-for="t in rows" :key="t.id">
          <div class="pimg-wrap">
            <img v-if="t.coverImage" class="pimg" :src="t.coverImage" alt="投诉图片" />
            <div v-else class="no-img">无图片</div>
          </div>

          <div class="pbody">
            <div class="p-top">
              <span class="num no">{{ t.ticketNo }}</span>
              <span class="tag" :class="levelTag(t.levelCode)">{{ t.levelCode }}</span>
              <span class="tag" :class="statusTag(t.status)">{{ t.status }}</span>
              <span v-if="t.overdueFlag" class="tag t-red">已超期</span>
            </div>
            <div class="p-title">{{ t.title }}</div>
            <div class="p-meta">
              <span>{{ t.categoryName || '—' }}</span>
              <span>{{ t.objectName || '—' }}</span>
              <span>{{ t.channel || '—' }}</span>
            </div>

            <!-- 7 段进度：待受理→已分派→处理中→待审核→待客户确认→待回访→已归档 -->
            <div class="prog">
              <div class="segs">
                <i v-for="(s, i) in FLOW" :key="s"
                   :class="{ done: i < curIndex(t.status), cur: i === curIndex(t.status) }"></i>
              </div>
              <span class="prog-lb">{{ t.status }}</span>
            </div>

            <div class="p-foot">
              <span class="num">提交 {{ fmtTime(t.createTime) }}</span>
              <span class="num">要求完成 {{ fmtTime(t.deadlineTime) }}</span>
              <div class="p-acts">
                <el-button link type="primary" @click="openDetail(t)">查看详情</el-button>
                <template v-if="t.status === '待客户确认'">
                  <el-button size="small" @click="openConfirm(t)">不认可</el-button>
                  <el-button size="small" type="primary" @click="openConfirm(t, true)">确认解决</el-button>
                </template>
              </div>
            </div>
          </div>
        </div>

        <div v-if="!loading && !rows.length" class="empty">
          <div class="empty-t">还没有符合条件的投诉</div>
          <div class="empty-d">可以切换上方状态，或点击「提交新投诉」登记问题，我们会尽快受理</div>
        </div>
      </div>

      <div class="pager">
        <el-pagination background layout="total, prev, pager, next" :total="total"
                       v-model:current-page="q.page" :page-size="q.size" @current-change="load" />
      </div>
    </div>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawer" size="620px" :title="cur?.ticketNo || '投诉详情'">
      <div v-loading="detailLoading">
        <template v-if="detail?.ticket">
          <div class="d-title">{{ detail.ticket.title }}</div>
          <div class="d-tags">
            <span class="tag" :class="statusTag(detail.ticket.status)">{{ detail.ticket.status }}</span>
            <span class="tag" :class="levelTag(detail.ticket.levelCode)">{{ detail.ticket.levelCode }}</span>
            <span class="tag t-blue">{{ detail.ticket.categoryName }}</span>
          </div>

          <div class="kv" style="margin-top:12px">
            <div class="k">投诉对象</div><div>{{ detail.ticket.objectName || '—' }}</div>
            <div class="k">受理渠道</div><div>{{ detail.ticket.channel || '—' }}</div>
            <div class="k">发生时间</div><div class="num">{{ fmtTime(detail.ticket.occurTime) }}</div>
            <div class="k">发生地点</div><div>{{ detail.ticket.occurPlace || '—' }}</div>
            <div class="k">要求完成</div><div class="num">{{ fmtTime(detail.ticket.deadlineTime) }}</div>
            <div class="k">当前处理人</div><div>{{ detail.ticket.currentHandlerName || '待分派' }}</div>
          </div>

          <div class="d-sub">我的诉求</div>
          <div class="desc-block">{{ detail.ticket.demand || '未填写诉求' }}</div>
          <div class="d-sub">问题描述</div>
          <div class="desc-block">{{ detail.ticket.content || '—' }}</div>

          <div class="d-sub" v-if="detail.order">处理方案</div>
          <div class="desc-block" v-if="detail.order">
            {{ detail.order.solution || '处理人尚未提交方案' }}
            <div style="color:var(--text-sub);margin-top:4px">{{ detail.order.result || '' }}</div>
          </div>

          <div class="d-sub" v-if="detail.revisit">回访结果</div>
          <div class="desc-block" v-if="detail.revisit">
            满意度 {{ satisfactionText(detail.revisit.satisfaction) }}；{{ detail.revisit.opinion || '—' }}
          </div>

          <div class="d-sub">处理进度</div>
          <div class="tl" v-if="detail.timeline?.length">
            <div class="tl-item" v-for="(it, i) in detail.timeline" :key="i">
              <div>{{ it.content }}</div>
              <div class="tm">{{ it.operatorName || '系统' }} · <span class="num">{{ fmtTime(it.operateTime) }}</span></div>
            </div>
          </div>
          <div v-else class="empty-d">暂无流转记录</div>

          <div class="drawer-foot" v-if="detail.ticket.status === '待客户确认'">
            <el-button @click="openConfirm(detail.ticket)">不认可</el-button>
            <el-button type="primary" @click="openConfirm(detail.ticket, true)">确认解决</el-button>
          </div>
        </template>
      </div>
    </el-drawer>

    <!-- 确认对话框 -->
    <el-dialog v-model="dlg" title="确认处理结果" width="440px">
      <div class="dlg-tip">工单 {{ cfTicket?.ticketNo }}：{{ cfTicket?.title }}</div>
      <el-form label-width="80px" style="margin-top:10px">
        <el-form-item label="确认结果">
          <el-radio-group v-model="cf.accept">
            <el-radio :value="true">问题已解决</el-radio>
            <el-radio :value="false">不认可</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="cf.opinion" type="textarea" :rows="3"
                    :placeholder="cf.accept ? '可补充您的评价（选填）' : '请说明不满意的原因，不少于 5 个字'" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitConfirm">提交</el-button>
      </template>
    </el-dialog>
  </PortalLayout>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PortalLayout from '../layouts/PortalLayout.vue'
import { api } from '../api'
import { store } from '../store/user'
import { fmtTime, statusTag, levelTag, satisfactionText } from '../utils/format'

// 主流程七个节点，用于卡片上的进度指示
const FLOW = ['待受理', '已分派', '处理中', '待审核', '待客户确认', '待回访', '已归档']

const opt = reactive({ statuses: [] })
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const detailLoading = ref(false)
const saving = ref(false)
const stats = reactive({ total: 0, processing: 0, toConfirm: 0, done: 0 })

const q = reactive({ status: '', page: 1, size: 6 })

const drawer = ref(false)
const detail = ref(null)
const cur = ref(null)
const dlg = ref(false)
const cf = reactive({ accept: true, opinion: '' })
const cfTicket = ref(null)

function curIndex(status) {
  const i = FLOW.indexOf(status)
  return i < 0 ? 0 : i
}

async function load() {
  loading.value = true
  try {
    const r = await api.myTickets({ page: q.page, size: q.size, status: q.status || undefined })
    rows.value = r.records || []
    total.value = r.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  q.page = 1
  load()
}

// 顶部四个数字按全量数据统计，不受当前分页与筛选影响
async function loadStats() {
  const r = await api.myTickets({ page: 1, size: 200 })
  const list = r.records || []
  stats.total = r.total || list.length
  stats.processing = list.filter((t) => ['待受理', '已分派', '处理中', '待审核'].includes(t.status)).length
  stats.toConfirm = list.filter((t) => t.status === '待客户确认').length
  stats.done = list.filter((t) => ['待回访', '已归档'].includes(t.status)).length
}

async function openDetail(t) {
  cur.value = t
  drawer.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await api.ticketDetail(t.id)
  } finally {
    detailLoading.value = false
  }
}

function openConfirm(t, accept = false) {
  cfTicket.value = t
  cf.accept = accept
  cf.opinion = ''
  dlg.value = true
}

async function submitConfirm() {
  if (!cf.accept && (cf.opinion || '').trim().length < 5) {
    return ElMessage.warning('请填写不少于 5 个字的不认可原因')
  }
  saving.value = true
  try {
    // 接受 → 待回访；不认可 → 退回处理中，由后端状态机处理
    await api.confirm(cfTicket.value.id, {
      accept: cf.accept,
      confirmResult: cf.accept ? '接受' : '提出异议',
      opinion: cf.opinion
    })
    ElMessage.success(cf.accept ? '感谢确认，我们会安排回访' : '已提交异议，我们会重新处理')
    dlg.value = false
    drawer.value = false
    await Promise.all([load(), loadStats()])
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  const o = await api.options()
  opt.statuses = o.statuses || []
  await Promise.all([load(), loadStats()])
})
</script>

<style scoped>
.welcome {
  display: flex; align-items: center; justify-content: space-between; gap: 20px;
  background: linear-gradient(120deg, #19324A, #356A7A);
  color: #fff; border-radius: var(--radius-card); padding: 18px 22px; margin-bottom: 12px;
}
.welcome .hi { font-size: 18px; font-weight: 600; }
.welcome .promise { font-size: 12px; color: rgba(255,255,255,.8); margin-top: 4px; line-height: 20px; }

.kpis-4 { grid-template-columns: repeat(4, 1fr); }
/* 状态 tab 较多，宽度不足时换行，避免撑破卡片头 */
.card-h { flex-wrap: wrap; row-gap: 8px; }
.plist { display: flex; flex-direction: column; gap: 10px; }
.pcard { display: flex; gap: 14px; border: 1px solid var(--line); border-radius: 8px; padding: 12px; }
.pcard:hover { border-color: var(--secondary); }
.pimg-wrap { flex: 0 0 132px; }
.pimg { width: 132px; height: 96px; object-fit: cover; border-radius: 6px; border: 1px solid var(--line); }
.no-img {
  width: 132px; height: 96px; border-radius: 6px; background: var(--tint);
  display: grid; place-items: center; font-size: 12px; color: var(--text-sub);
}
.pbody { flex: 1; min-width: 0; }
.p-top { display: flex; align-items: center; gap: 8px; }
.p-top .no { font-size: 12px; color: var(--text-sub); }
.p-title { font-size: 14px; font-weight: 600; margin: 6px 0 4px; line-height: 22px; }
.p-meta { display: flex; gap: 14px; font-size: 12px; color: var(--text-sub); }

.prog { display: flex; align-items: center; gap: 10px; margin: 10px 0 8px; }
.segs { display: flex; gap: 4px; flex: 1; }
.segs i { display: block; height: 5px; border-radius: 3px; background: var(--tint); flex: 1; }
.segs i.done { background: #B4C0CC; }
.segs i.cur { background: var(--secondary); }
.prog-lb { font-size: 11px; color: var(--text-sub); }

.p-foot { display: flex; align-items: center; gap: 16px; font-size: 12px; color: var(--text-sub); }
.p-acts { margin-left: auto; display: flex; align-items: center; gap: 6px; }

.empty { text-align: center; padding: 34px 10px; }
.empty-t { font-size: 13px; font-weight: 600; }
.empty-d { font-size: 12px; color: var(--text-sub); margin-top: 4px; }

.d-title { font-size: 16px; font-weight: 600; line-height: 24px; }
.d-tags { display: flex; gap: 6px; margin-top: 8px; }
.d-sub { font-size: 13px; font-weight: 600; margin: 14px 0 6px; }
.drawer-foot { display: flex; justify-content: flex-end; gap: 10px; margin-top: 18px; padding-top: 12px; border-top: 1px solid var(--line); }
.dlg-tip { font-size: 13px; background: var(--tint); border-radius: 6px; padding: 8px 10px; }

@media (max-width: 960px) {
  .kpis-4 { grid-template-columns: repeat(2, 1fr); }
  .pcard { flex-direction: column; }
}
</style>
