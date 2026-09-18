<template>
  <div class="page-wrap">
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" size="small">
        <el-form-item label="分类名称">
          <el-input v-model="query.name" placeholder="请输入分类名称" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="分类类型">
          <el-select v-model="query.type" placeholder="全部" clearable style="width: 160px">
            <el-option label="服务项目分类" :value="1" />
            <el-option label="服务套餐分类" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="primary" plain @click="handleAdd">+ 新增分类</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="tableData" border stripe size="small">
        <el-table-column label="图标" width="80" align="center">
          <template slot-scope="scope">
            <img v-if="scope.row.icon" :src="scope.row.icon" class="cat-icon" />
            <i v-else class="el-icon-picture-outline empty-icon" />
          </template>
        </el-table-column>
        <el-table-column prop="name" label="分类名称" min-width="140" />
        <el-table-column label="分类类型" width="140">
          <template slot-scope="scope">
            <el-tag size="mini" :type="scope.row.type === 2 ? 'warning' : 'success'">
              {{ scope.row.type === 2 ? '服务套餐分类' : '服务项目分类' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="90" />
        <el-table-column label="状态" width="100">
          <template slot-scope="scope">
            <el-tag size="mini" :type="scope.row.status === 1 ? 'success' : 'info'">
              {{ scope.row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template slot-scope="scope">{{ formatTime(scope.row.updateTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="mini" @click="handleEdit(scope.row)">修改</el-button>
            <el-button type="text" size="mini" @click="handleStatus(scope.row)">
              {{ scope.row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button type="text" size="mini" class="danger-text" @click="handleDelete(scope.row)">删除</el-button>
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

    <el-dialog :title="form.id ? '修改分类' : '新增分类'" :visible.sync="dialogVisible" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" size="small">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="如：家政保洁" />
        </el-form-item>
        <el-form-item label="分类类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :label="1">服务项目分类</el-radio>
            <el-radio :label="2">服务套餐分类</el-radio>
          </el-radio-group>
          <div class="tip">服务项目分类下挂单项服务，服务套餐分类下挂组合套餐</div>
        </el-form-item>
        <el-form-item label="分类图标">
          <el-input v-model="form.icon" placeholder="图标图片 URL" />
          <div class="tip">小程序首页的分类导航会显示这个图标</div>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
          <span class="tip-inline">数字越小越靠前</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleSubmit">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import { getCategoryPage, addCategory, editCategory, deleCategory, enableOrDisableEmployee } from '@/api/category'

export default Vue.extend({
  name: 'Category',

  data() {
    return {
      query: { page: 1, pageSize: 10, name: '', type: null },
      total: 0,
      tableData: [] as any[],
      loading: false,
      dialogVisible: false,
      form: { id: null, name: '', type: 1, icon: '', sort: 0, status: 1 } as any,
      rules: {
        name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
        type: [{ required: true, message: '请选择分类类型', trigger: 'change' }]
      }
    }
  },

  created() {
    this.loadData()
  },

  methods: {
    formatTime(v: any) {
      return v ? String(v).replace('T', ' ').slice(0, 16) : '-'
    },

    loadData() {
      this.loading = true
      const params: any = { page: this.query.page, pageSize: this.query.pageSize }
      if (this.query.name) params.name = this.query.name
      if (this.query.type) params.type = this.query.type

      getCategoryPage(params)
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
      this.query = { page: 1, pageSize: 10, name: '', type: null }
      this.loadData()
    },
    handlePageChange(p: number) { this.query.page = p; this.loadData() },
    handleSizeChange(s: number) { this.query.pageSize = s; this.query.page = 1; this.loadData() },

    handleAdd() {
      this.form = { id: null, name: '', type: 1, icon: '', sort: 0, status: 1 }
      this.dialogVisible = true
    },

    handleEdit(row: any) {
      this.form = { ...row }
      this.dialogVisible = true
    },

    handleSubmit() {
      (this.$refs.formRef as any).validate((valid: boolean) => {
        if (!valid) return
        const req = this.form.id ? editCategory(this.form) : addCategory(this.form)
        req
          .then((res: any) => {
            if (res.data.code === 1) {
              this.$message.success(this.form.id ? '修改成功' : '新增成功')
              this.dialogVisible = false
              this.loadData()
            } else {
              this.$message.error(res.data.msg || '保存失败')
            }
          })
          .catch((err: any) => this.$message.error('请求失败：' + err.message))
      })
    },

    handleStatus(row: any) {
      const next = row.status === 1 ? 0 : 1
      enableOrDisableEmployee({ status: next, id: row.id }).then((res: any) => {
        if (res.data.code === 1) {
          this.$message.success(next === 1 ? '已启用' : '已禁用')
          this.loadData()
        }
      })
    },

    handleDelete(row: any) {
      this.$confirm('确认删除分类「' + row.name + '」吗？', '提示', { type: 'warning' })
        .then(() => {
          deleCategory(String(row.id)).then((res: any) => {
            if (res.data.code === 1) {
              this.$message.success('删除成功')
              this.loadData()
            } else {
              // 分类下有服务或套餐时后端会返回具体原因
              this.$message.error(res.data.msg || '删除失败')
            }
          })
        })
        .catch(() => {})
    }
  }
})
</script>

<style scoped>
.page-wrap { padding: 16px; }
.search-card { margin-bottom: 12px; }
.table-card { margin-bottom: 16px; }
.pager { margin-top: 14px; text-align: right; }
.cat-icon { width: 34px; height: 34px; border-radius: 6px; object-fit: cover; }
.empty-icon { font-size: 20px; color: #c8cdd6; }
.tip { color: #999; font-size: 12px; line-height: 1.5; }
.tip-inline { margin-left: 8px; color: #999; font-size: 12px; }
.danger-text { color: #f56c6c; }
</style>
