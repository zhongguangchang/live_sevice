<template>
  <div class="page-wrap">
    <el-card shadow="never" class="search-card">
      <div slot="header" class="card-head">
        <span>服务区域</span>
        <span class="tip">平台不是哪儿都上门。用户下单时用地址的区级编号匹配这张表，匹配不到就提示暂未开通</span>
      </div>
      <el-form :inline="true" :model="query" size="small">
        <el-form-item label="区域名称">
          <el-input v-model="query.name" placeholder="如：海淀区" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="已开通" :value="1" />
            <el-option label="未开通" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button type="primary" plain @click="handleAdd">+ 新增区域</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="tableData" border stripe size="small">
        <el-table-column prop="name" label="区域名称" width="140" />
        <el-table-column prop="code" label="区级区划编号" width="150" />
        <el-table-column prop="cityName" label="所属城市" width="140" />
        <el-table-column prop="cityCode" label="城市编号" width="140" />
        <el-table-column prop="sort" label="排序" width="90" />
        <el-table-column label="状态" width="110">
          <template slot-scope="scope">
            <el-tag size="mini" :type="scope.row.status === 1 ? 'success' : 'info'">
              {{ scope.row.status === 1 ? '已开通' : '未开通' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template slot-scope="scope">
            <el-button type="text" size="mini" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="text" size="mini" @click="toggleStatus(scope.row)">
              {{ scope.row.status === 1 ? '停用' : '开通' }}
            </el-button>
            <el-button type="text" size="mini" class="danger-text" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog :title="form.id ? '编辑服务区域' : '新增服务区域'" :visible.sync="dialogVisible" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" size="small">
        <el-form-item label="区域名称" prop="name">
          <el-input v-model="form.name" placeholder="如：海淀区" />
        </el-form-item>
        <el-form-item label="区级编号" prop="code">
          <el-input v-model="form.code" placeholder="如：110108" />
          <div class="tip">必须是标准行政区划编号，用户地址里的区级编号要和它一致才能匹配上</div>
        </el-form-item>
        <el-form-item label="所属城市">
          <el-input v-model="form.cityName" placeholder="如：北京市" />
        </el-form-item>
        <el-form-item label="城市编号">
          <el-input v-model="form.cityCode" placeholder="如：110100" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">已开通</el-radio>
            <el-radio :label="0">未开通</el-radio>
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
import {
  queryServiceAreaList,
  addServiceArea,
  editServiceArea,
  deleteServiceArea,
  serviceAreaStatusByStatus
} from '@/api/serviceArea'

// 表单初始值。放在模块作用域而不是 methods 里，
// 因为 data() 里要用它，而 data() 执行时 this 上还没有 methods，
// TypeScript 会报 "Property 'emptyForm' does not exist"。
function emptyForm() {
  return { id: null, name: '', code: '', cityName: '', cityCode: '', sort: 0, status: 1 }
}

export default Vue.extend({
  name: 'ServiceArea',

  data() {
    return {
      query: { name: '', status: null },
      tableData: [] as any[],
      loading: false,
      dialogVisible: false,
      form: emptyForm(),
      rules: {
        name: [{ required: true, message: '请输入区域名称', trigger: 'blur' }],
        code: [{ required: true, message: '请输入区级区划编号', trigger: 'blur' }]
      }
    }
  },

  created() {
    this.loadData()
  },

  methods: {
    emptyForm,

    loadData() {
      this.loading = true
      const params: any = {}
      if (this.query.name) params.name = this.query.name
      if (this.query.status !== null && this.query.status !== '') params.status = this.query.status
      queryServiceAreaList(params)
        .then((res: any) => {
          if (res.data.code === 1) this.tableData = res.data.data || []
        })
        .catch((err: any) => this.$message.error('请求失败：' + err.message))
        .finally(() => { this.loading = false })
    },

    handleAdd() {
      this.form = this.emptyForm()
      this.dialogVisible = true
    },

    handleEdit(row: any) {
      this.form = { ...row }
      this.dialogVisible = true
    },

    handleSubmit() {
      (this.$refs.formRef as any).validate((valid: boolean) => {
        if (!valid) return
        const req = this.form.id ? editServiceArea(this.form) : addServiceArea(this.form)
        req.then((res: any) => {
          if (res.data.code === 1) {
            this.$message.success(this.form.id ? '修改成功' : '新增成功')
            this.dialogVisible = false
            this.loadData()
          } else {
            this.$message.error(res.data.msg || '保存失败')
          }
        })
      })
    },

    toggleStatus(row: any) {
      const next = row.status === 1 ? 0 : 1
      serviceAreaStatusByStatus({ status: next, id: row.id }).then((res: any) => {
        if (res.data.code === 1) {
          this.$message.success(next === 1 ? '已开通' : '已停用')
          this.loadData()
        }
      })
    },

    handleDelete(row: any) {
      this.$confirm('确认删除区域「' + row.name + '」吗？', '提示', { type: 'warning' })
        .then(() => {
          deleteServiceArea(String(row.id)).then((res: any) => {
            if (res.data.code === 1) {
              this.$message.success('删除成功')
              this.loadData()
            } else {
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
.card-head { display: flex; align-items: baseline; }
.card-head .tip { margin-left: 12px; color: #999; font-size: 12px; font-weight: 400; }
.tip { color: #999; font-size: 12px; line-height: 1.5; }
.danger-text { color: #f56c6c; }
</style>
