<template>
  <div class="page-wrap">
    <!-- 状态统计 -->
    <el-row :gutter="12" class="stat-row">
      <el-col :span="6" v-for="s in statCards" :key="s.key">
        <el-card shadow="hover" class="stat-card" @click.native="filterByStatus(s.status)">
          <div class="stat-num" :style="{ color: s.color }">{{ stats[s.key] || 0 }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" size="small">
        <el-form-item label="订单号">
          <el-input v-model="query.number" placeholder="精确匹配" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="query.phone" placeholder="模糊匹配" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="tableData" border stripe size="small">
        <el-table-column prop="number" label="订单号" width="190" show-overflow-tooltip />
        <el-table-column label="预约服务时间" width="160">
          <template slot-scope="scope">{{ formatTime(scope.row.serviceTime) }}</template>
        </el-table-column>
        <el-table-column label="服务方式" width="90">
          <template slot-scope="scope">
            <el-tag size="mini" :type="scope.row.serviceMode === 1 ? 'success' : 'warning'">
              {{ scope.row.serviceMode === 1 ? '上门' : '到店' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="consignee" label="联系人" width="90" />
        <el-table-column prop="phone" label="联系电话" width="125" />
        <el-table-column prop="address" label="服务地址" min-width="180" show-overflow-tooltip />
        <el-table-column label="金额" width="100">
          <template slot-scope="scope"><span class="price">¥{{ scope.row.amount }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template slot-scope="scope">
            <el-tag size="mini" :type="statusTagType(scope.row.status)">{{ statusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="服务人员" width="100">
          <template slot-scope="scope">
            <span v-if="scope.row.providerId">#{{ scope.row.providerId }}</span>
            <span v-else class="muted">未指派</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="mini" @click="handleDetail(scope.row)">详情</el-button>
            <el-button
              v-if="scope.row.status === 2"
              type="text"
              size="mini"
              @click="handleDispatch(scope.row)"
            >派单</el-button>
            <el-button
              v-if="scope.row.status === 4"
              type="text"
              size="mini"
              @click="handleComplete(scope.row)"
            >完成</el-button>
            <el-button
              v-if="scope.row.status === 1 || scope.row.status === 2"
              type="text"
              size="mini"
              class="danger-text"
              @click="handleCancel(scope.row)"
            >取消</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pager"
        background
        :current-page="query.page"
        :page-size="query.pageSize"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        :total="total"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </el-card>

    <!-- 订单详情 -->
    <el-dialog title="订单详情" :visible.sync="detailVisible" width="700px">
      <template v-if="detail">
        <el-descriptions :column="2" border size="small" title="预约信息">
          <el-descriptions-item label="订单号">{{ detail.order.number }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag size="mini" :type="statusTagType(detail.order.status)">{{ statusText(detail.order.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="预约时间">{{ formatTime(detail.order.serviceTime) }}</el-descriptions-item>
          <el-descriptions-item label="服务方式">{{ detail.order.serviceMode === 1 ? '上门服务' : '到店服务' }}</el-descriptions-item>
          <el-descriptions-item label="预计时长">{{ detail.order.serviceDuration }} 分钟</el-descriptions-item>
          <el-descriptions-item label="订单金额">¥{{ detail.order.amount }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ detail.order.consignee }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ detail.order.phone }}</el-descriptions-item>
          <el-descriptions-item label="服务地址" :span="2">{{ detail.order.address || '（到店服务，无地址）' }}</el-descriptions-item>
          <el-descriptions-item label="核销码" :span="2">{{ detail.order.verifyCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detail.order.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions :column="2" border size="small" title="服务人员" class="mt16">
          <el-descriptions-item label="姓名">{{ detail.providerName || '未指派' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ detail.providerPhone || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-table :data="detail.items" border size="mini" class="mt16">
          <el-table-column prop="name" label="服务项目" min-width="140" />
          <el-table-column prop="spec" label="规格" min-width="140" />
          <el-table-column prop="price" label="单价" width="90" />
          <el-table-column prop="number" label="数量" width="70" />
          <el-table-column prop="amount" label="小计" width="90" />
        </el-table>

        <el-descriptions :column="1" border size="small" title="时间轴" class="mt16">
          <el-descriptions-item label="下单">{{ formatTime(detail.order.orderTime) }}</el-descriptions-item>
          <el-descriptions-item label="支付">{{ formatTime(detail.order.checkoutTime) }}</el-descriptions-item>
          <el-descriptions-item label="派单">{{ formatTime(detail.order.dispatchTime) }}</el-descriptions-item>
          <el-descriptions-item label="接单">{{ formatTime(detail.order.acceptTime) }}</el-descriptions-item>
          <el-descriptions-item label="开始服务">{{ formatTime(detail.order.startTime) }}</el-descriptions-item>
          <el-descriptions-item label="服务完成">{{ formatTime(detail.order.finishTime) }}</el-descriptions-item>
          <el-descriptions-item label="取消时间">{{ formatTime(detail.order.cancelTime) }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>

    <!-- 派单 -->
    <el-dialog title="派单" :visible.sync="dispatchVisible" width="480px">
      <el-form label-width="90px" size="small">
        <el-form-item label="订单号">
          <span>{{ currentRow && currentRow.number }}</span>
        </el-form-item>
        <el-form-item label="派单方式">
          <el-radio-group v-model="dispatchMode">
            <el-radio label="auto">自动派单</el-radio>
            <el-radio label="manual">指定师傅</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="dispatchMode === 'manual'" label="选择师傅">
          <el-select v-model="selectedProviderId" placeholder="请选择" style="width: 100%">
            <el-option v-for="p in providers" :key="p.id" :label="providerLabel(p)" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-alert
          v-else
          type="info"
          :closable="false"
          title="按技能匹配、接单状态、评分排序、档期冲突依次筛选，自动挑出最合适的一位"
        />
      </el-form>
      <div slot="footer">
        <el-button @click="dispatchVisible = false">取 消</el-button>
        <el-button type="primary" :loading="dispatching" @click="submitDispatch">确认派单</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import {
  getServiceOrderPage,
  getServiceOrderStatistics,
  queryServiceOrderDetail,
  dispatchServiceOrder,
  cancelServiceOrder,
  completeServiceOrder
} from '@/api/serviceOrder'
import { queryAvailableProvider } from '@/api/provider'

export default Vue.extend({
  name: 'ServiceOrder',

  data() {
    return {
      query: { page: 1, pageSize: 10, number: '', phone: '', status: null },
      total: 0,
      tableData: [] as any[],
      loading: false,

      stats: {} as any,
      statCards: [
        { key: 'toBeAccepted', label: '待接单', status: 2, color: '#e6a23c' },
        { key: 'accepted', label: '已接单', status: 3, color: '#409eff' },
        { key: 'inService', label: '服务中', status: 4, color: '#67c23a' },
        { key: 'toBeReviewed', label: '待评价', status: 5, color: '#909399' }
      ],

      statusOptions: [
        { value: 1, label: '待付款' },
        { value: 2, label: '待接单' },
        { value: 3, label: '已接单' },
        { value: 4, label: '服务中' },
        { value: 5, label: '待评价' },
        { value: 6, label: '已完成' },
        { value: 7, label: '已取消' }
      ],

      detailVisible: false,
      detail: null as any,

      dispatchVisible: false,
      dispatchMode: 'auto',
      dispatchOrderId: null,
      currentRow: null as any as any,
      selectedProviderId: null,
      providers: [] as any[],
      dispatching: false
    }
  },

  created() {
    this.loadStats()
    this.loadData()
  },

  methods: {
    statusText(s: number) {
      const hit = this.statusOptions.find((o: any) => o.value === s)
      return hit ? hit.label : '-'
    },

    statusTagType(s: number) {
      if (s === 2) return 'warning'
      if (s === 3) return ''
      if (s === 4) return 'success'
      if (s === 5) return 'info'
      if (s === 6) return 'success'
      if (s === 7) return 'danger'
      return 'info'
    },

    formatTime(v: any) {
      if (!v) return '-'
      return String(v).replace('T', ' ').slice(0, 16)
    },

    loadStats() {
      getServiceOrderStatistics().then((res: any) => {
        if (res.data.code === 1) this.stats = res.data.data || {}
      })
    },

    loadData() {
      this.loading = true
      const params: any = { page: this.query.page, pageSize: this.query.pageSize }
      if (this.query.number) params.number = this.query.number
      if (this.query.phone) params.phone = this.query.phone
      if (this.query.status !== null && this.query.status !== '') params.status = this.query.status

      getServiceOrderPage(params)
        .then((res: any) => {
          if (res.data.code === 1) {
            this.tableData = (res.data.data && res.data.data.records) || []
            this.total = Number((res.data.data && res.data.data.total) || 0)
          }
        })
        .catch((err: any) => this.$message.error('请求失败：' + err.message))
        .finally(() => { this.loading = false })
    },

    filterByStatus(status: number) {
      this.query.status = status
      this.query.page = 1
      this.loadData()
    },

    handleSearch() { this.query.page = 1; this.loadData() },
    handleReset() {
      this.query = { page: 1, pageSize: 10, number: '', phone: '', status: null }
      this.loadData()
    },
    handlePageChange(p: number) { this.query.page = p; this.loadData() },
    handleSizeChange(s: number) { this.query.pageSize = s; this.query.page = 1; this.loadData() },

    handleDetail(row: any) {
      queryServiceOrderDetail(row.id).then((res: any) => {
        if (res.data.code === 1) {
          this.detail = res.data.data
          this.detailVisible = true
        } else {
          this.$message.error(res.data.msg || '查询失败')
        }
      })
    },

    handleDispatch(row: any) {
      this.currentRow = row
      this.dispatchOrderId = row.id
      this.dispatchMode = 'auto'
      this.selectedProviderId = null
      this.providers = []
      this.dispatchVisible = true
      // 拉一份可接单的师傅列表，指定派单时用
      queryAvailableProvider({ categoryId: null }).then((res: any) => {
        if (res.data.code === 1) this.providers = res.data.data || []
      })
    },

    providerLabel(p: any) {
      const names = (p.categoryNames || []).join('/')
      return p.name + '（' + (names || '未配置技能') + '，评分 ' + (p.score || '-') + '）'
    },

    submitDispatch() {
      if (this.dispatchMode === 'manual' && !this.selectedProviderId) {
        this.$message.warning('请选择要指派的师傅')
        return
      }
      this.dispatching = true
      dispatchServiceOrder({
        orderId: this.dispatchOrderId,
        providerId: this.dispatchMode === 'manual' ? this.selectedProviderId : null
      })
        .then((res: any) => {
          if (res.data.code === 1) {
            this.$message.success('派单成功')
            this.dispatchVisible = false
            this.loadData()
            this.loadStats()
          } else {
            this.$message.error(res.data.msg || '派单失败')
          }
        })
        .catch((err: any) => this.$message.error('请求失败：' + err.message))
        .finally(() => { this.dispatching = false })
    },

    handleComplete(row: any) {
      this.$confirm('确认该订单的服务已完成？', '提示', { type: 'warning' }).then(() => {
        completeServiceOrder(String(row.id)).then((res: any) => {
          if (res.data.code === 1) {
            this.$message.success('已标记为完成')
            this.loadData()
            this.loadStats()
          } else {
            this.$message.error(res.data.msg || '操作失败')
          }
        })
      }).catch(() => {})
    },

    handleCancel(row: any) {
      this.$prompt('请输入取消原因', '取消订单', { inputValue: '运营取消' }).then(({ value }: any) => {
        cancelServiceOrder({ id: row.id, cancelReason: value }).then((res: any) => {
          if (res.data.code === 1) {
            this.$message.success('已取消，名额已回补')
            this.loadData()
            this.loadStats()
          } else {
            this.$message.error(res.data.msg || '操作失败')
          }
        })
      }).catch(() => {})
    }
  }
})
</script>

<style scoped>
.page-wrap { padding: 16px; }
.stat-row { margin-bottom: 12px; }
.stat-card { cursor: pointer; text-align: center; }
.stat-num { font-size: 28px; font-weight: 700; line-height: 1.2; }
.stat-label { color: #909399; font-size: 13px; margin-top: 4px; }
.search-card { margin-bottom: 12px; }
.table-card { margin-bottom: 16px; }
.pager { margin-top: 14px; text-align: right; }
.price { color: #F56C6C; font-weight: 600; }
.muted { color: #bbb; }
.danger-text { color: #f56c6c; }
.mt16 { margin-top: 16px; }
</style>
