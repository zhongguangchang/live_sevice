<template>
  <div class="page-wrap">
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" size="small">
        <el-form-item label="姓名">
          <el-input v-model="query.name" placeholder="请输入姓名" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="primary" plain @click="handleAdd">+ 新增员工</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="tableData" border stripe size="small">
        <el-table-column prop="username" label="登录账号" width="140" />
        <el-table-column prop="name" label="姓名" width="110" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="性别" width="80">
          <template slot-scope="scope">{{ scope.row.sex === '1' ? '男' : '女' }}</template>
        </el-table-column>
        <el-table-column label="角色" width="140">
          <template slot-scope="scope">
            <el-tag size="mini" :type="roleTagType(scope.row.role)">{{ roleText(scope.row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="账号状态" width="110">
          <template slot-scope="scope">
            <el-tag size="mini" :type="scope.row.status === 1 ? 'success' : 'danger'">
              {{ scope.row.status === 1 ? '正常' : '已锁定' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最后操作时间" width="170">
          <template slot-scope="scope">{{ formatTime(scope.row.updateTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="mini" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="text" size="mini" @click="handleStatus(scope.row)">
              {{ scope.row.status === 1 ? '锁定' : '启用' }}
            </el-button>
            <el-button
              v-if="scope.row.username !== 'admin'"
              type="text"
              size="mini"
              class="danger-text"
              @click="handleDelete(scope.row)"
            >删除</el-button>
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

    <el-dialog :title="form.id ? '编辑员工' : '新增员工'" :visible.sync="dialogVisible" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" size="small">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="登录账号" prop="username">
              <el-input v-model="form.username" :disabled="!!form.id" placeholder="用于登录后台" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-radio-group v-model="form.sex">
                <el-radio label="1">男</el-radio>
                <el-radio label="0">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="身份证号" prop="idNumber">
          <el-input v-model="form.idNumber" maxlength="18" />
        </el-form-item>

        <el-form-item label="角色" prop="role">
          <el-radio-group v-model="form.role">
            <el-radio :label="1">超级管理员</el-radio>
            <el-radio :label="2">运营</el-radio>
            <el-radio :label="3">派单员</el-radio>
          </el-radio-group>
          <div class="tip">
            超级管理员可管理账号；运营负责服务项目、套餐和评价；派单员只处理订单和派单
          </div>
        </el-form-item>

        <el-alert
          v-if="!form.id"
          type="info"
          :closable="false"
          title="新增员工的初始密码为 123456，请提醒对方登录后尽快修改"
          style="margin-bottom: 8px"
        />
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
import { getEmployeeList, addEmployee, editEmployee, queryEmployeeById, enableOrDisableEmployee } from '@/api/employee'

export default Vue.extend({
  name: 'Employee',

  data() {
    return {
      query: { page: 1, pageSize: 10, name: '' },
      total: 0,
      tableData: [] as any[],
      loading: false,
      dialogVisible: false,
      form: { id: null, username: '', name: '', phone: '', sex: '1', idNumber: '', role: 3 } as any,
      rules: {
        username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
        name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
        phone: [
          { required: true, message: '请输入手机号', trigger: 'blur' },
          { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }
        ],
        role: [{ required: true, message: '请选择角色', trigger: 'change' }]
      }
    }
  },

  created() {
    this.loadData()
  },

  methods: {
    roleText(role: any) {
      return role === 1 ? '超级管理员' : role === 2 ? '运营' : role === 3 ? '派单员' : '-'
    },
    roleTagType(role: any) {
      return role === 1 ? 'danger' : role === 2 ? '' : 'info'
    },
    formatTime(v: any) {
      return v ? String(v).replace('T', ' ').slice(0, 16) : '-'
    },

    loadData() {
      this.loading = true
      const params: any = { page: this.query.page, pageSize: this.query.pageSize }
      if (this.query.name) params.name = this.query.name

      getEmployeeList(params)
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
      this.query = { page: 1, pageSize: 10, name: '' }
      this.loadData()
    },
    handlePageChange(p: number) { this.query.page = p; this.loadData() },
    handleSizeChange(s: number) { this.query.pageSize = s; this.query.page = 1; this.loadData() },

    handleAdd() {
      this.form = { id: null, username: '', name: '', phone: '', sex: '1', idNumber: '', role: 3 }
      this.dialogVisible = true
    },

    handleEdit(row: any) {
      queryEmployeeById(String(row.id)).then((res: any) => {
        if (res.data.code === 1 && res.data.data) {
          const d = res.data.data
          this.form = {
            id: d.id,
            username: d.username,
            name: d.name,
            phone: d.phone,
            sex: d.sex || '1',
            idNumber: d.idNumber || '',
            role: d.role || 3
          }
          this.dialogVisible = true
        }
      })
    },

    handleSubmit() {
      (this.$refs.formRef as any).validate((valid: boolean) => {
        if (!valid) return
        const req = this.form.id ? editEmployee(this.form) : addEmployee(this.form)
        req
          .then((res: any) => {
            if (res.data.code === 1) {
              this.$message.success(this.form.id ? '修改成功' : '新增成功')
              this.dialogVisible = false
              this.loadData()
            } else {
              // 账号重复等情况后端会返回原因
              this.$message.error(res.data.msg || '保存失败')
            }
          })
          .catch((err: any) => this.$message.error('请求失败：' + err.message))
      })
    },

    handleStatus(row: any) {
      const next = row.status === 1 ? 0 : 1
      this.$confirm(next === 0 ? '锁定后该账号将无法登录，确认？' : '确认启用该账号？', '提示', { type: 'warning' })
        .then(() => {
          enableOrDisableEmployee({ status: next, id: row.id }).then((res: any) => {
            if (res.data.code === 1) {
              this.$message.success(next === 1 ? '已启用' : '已锁定')
              this.loadData()
            } else {
              this.$message.error(res.data.msg || '操作失败')
            }
          })
        })
        .catch(() => {})
    },

    handleDelete(row: any) {
      // 后端没有单独的删除员工接口，这里用「锁定」代替，保留操作痕迹
      this.$message.info('平台不提供删除账号，请使用「锁定」停用该账号')
    }
  }
})
</script>

<style scoped>
.page-wrap { padding: 16px; }
.search-card { margin-bottom: 12px; }
.table-card { margin-bottom: 16px; }
.pager { margin-top: 14px; text-align: right; }
.tip { color: #999; font-size: 12px; line-height: 1.5; }
.danger-text { color: #f56c6c; }
</style>
