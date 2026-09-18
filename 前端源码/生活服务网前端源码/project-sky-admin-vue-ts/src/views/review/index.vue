<template>
  <div class="page-wrap">
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" size="small">
        <el-form-item label="服务项目ID">
          <el-input v-model="query.serviceId" placeholder="服务项目 id" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="评分">
          <el-select v-model="query.score" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="n in [5,4,3,2,1]" :key="n" :label="n + ' 星'" :value="n" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="显示" :value="1" />
            <el-option label="已隐藏" :value="0" />
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
        <el-table-column label="用户" width="120">
          <template slot-scope="scope">{{ scope.row.userName || '匿名用户' }}</template>
        </el-table-column>
        <el-table-column label="综合评分" width="130">
          <template slot-scope="scope">
            <el-rate :value="Number(scope.row.score)" disabled show-score text-color="#e6a23c" />
          </template>
        </el-table-column>
        <el-table-column label="三维打分" width="180">
          <template slot-scope="scope">
            <div class="dim">态度 {{ scope.row.serviceScore || '-' }} · 速度 {{ scope.row.speedScore || '-' }} · 质量 {{ scope.row.qualityScore || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="providerName" label="服务人员" width="100" />
        <el-table-column prop="serviceName" label="服务项目" min-width="130" />
        <el-table-column prop="content" label="评价内容" min-width="200" show-overflow-tooltip />
        <el-table-column label="回复" min-width="150" show-overflow-tooltip>
          <template slot-scope="scope">
            <span v-if="scope.row.reply">{{ scope.row.reply }}</span>
            <span v-else class="muted">未回复</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template slot-scope="scope">
            <el-tag size="mini" :type="scope.row.status === 1 ? 'success' : 'info'">
              {{ scope.row.status === 1 ? '显示' : '已隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="mini" @click="handleReply(scope.row)">回复</el-button>
            <el-button type="text" size="mini" @click="toggleStatus(scope.row)">
              {{ scope.row.status === 1 ? '隐藏' : '显示' }}
            </el-button>
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

    <el-dialog title="回复评价" :visible.sync="replyVisible" width="520px">
      <div class="origin-review" v-if="currentRow">
        <div class="label">用户评价：</div>
        <div>{{ currentRow.content || '（无文字内容）' }}</div>
      </div>
      <el-input v-model="replyText" type="textarea" :rows="3" placeholder="输入回复内容" />
      <div slot="footer">
        <el-button @click="replyVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitReply">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import { getReviewPage, replyReview, reviewStatusByStatus } from '@/api/review'

export default Vue.extend({
  name: 'Review',

  data() {
    return {
      query: { page: 1, pageSize: 10, serviceId: '', score: null, status: null },
      total: 0,
      tableData: [] as any[],
      loading: false,

      replyVisible: false,
      currentRow: null as any,
      replyText: ''
    }
  },

  created() {
    this.loadData()
  },

  methods: {
    loadData() {
      this.loading = true
      const params: any = { page: this.query.page, pageSize: this.query.pageSize }
      if (this.query.serviceId) params.serviceId = this.query.serviceId
      if (this.query.score) params.score = this.query.score
      if (this.query.status !== null && this.query.status !== '') params.status = this.query.status

      getReviewPage(params)
        .then((res: any) => {
          if (res.data.code === 1) {
            this.tableData = (res.data.data && res.data.data.records) || []
            this.total = Number((res.data.data && res.data.data.total) || 0)
          }
        })
        .catch((err: any) => this.$message.error('请求失败：' + err.message))
        .finally(() => { this.loading = false })
    },

    handleSearch() { this.query.page = 1; this.loadData() },
    handleReset() {
      this.query = { page: 1, pageSize: 10, serviceId: '', score: null, status: null }
      this.loadData()
    },
    handlePageChange(p: number) { this.query.page = p; this.loadData() },
    handleSizeChange(s: number) { this.query.pageSize = s; this.query.page = 1; this.loadData() },

    handleReply(row: any) {
      this.currentRow = row
      this.replyText = row.reply || ''
      this.replyVisible = true
    },

    submitReply() {
      if (!this.replyText) {
        this.$message.warning('请输入回复内容')
        return
      }
      replyReview({ id: this.currentRow.id, reply: this.replyText }).then((res: any) => {
        if (res.data.code === 1) {
          this.$message.success('回复成功')
          this.replyVisible = false
          this.loadData()
        } else {
          this.$message.error(res.data.msg || '回复失败')
        }
      })
    },

    toggleStatus(row: any) {
      const next = row.status === 1 ? 0 : 1
      reviewStatusByStatus({ status: next, id: row.id }).then((res: any) => {
        if (res.data.code === 1) {
          this.$message.success(next === 1 ? '已显示' : '已隐藏')
          this.loadData()
        }
      })
    }
  }
})
</script>

<style scoped>
.page-wrap { padding: 16px; }
.search-card { margin-bottom: 12px; }
.table-card { margin-bottom: 16px; }
.pager { margin-top: 14px; text-align: right; }
.dim { font-size: 12px; color: #666; }
.muted { color: #bbb; }
.origin-review { margin-bottom: 12px; padding: 10px; background: #f5f7fa; border-radius: 4px; font-size: 13px; }
.origin-review .label { color: #909399; margin-bottom: 4px; }
</style>
