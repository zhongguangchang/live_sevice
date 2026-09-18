<template>
  <div class="page-wrap">
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" size="small">
        <el-form-item label="师傅姓名">
          <el-input v-model="query.name" placeholder="请输入姓名" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="擅长分类">
          <el-select v-model="query.categoryId" placeholder="全部分类" clearable style="width: 150px">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="接单状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="primary" plain @click="handleAdd">+ 新增服务人员</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="tableData" border stripe size="small">
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column label="擅长技能" min-width="200">
          <template slot-scope="scope">
            <template v-if="scope.row.categoryNames && scope.row.categoryNames.length">
              <el-tag v-for="(n, i) in scope.row.categoryNames" :key="i" size="mini" class="skill-tag">{{ n }}</el-tag>
            </template>
            <span v-else class="muted">未配置</span>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="90">
          <template slot-scope="scope">
            <span class="score">{{ scope.row.score || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="好评率" width="100">
          <template slot-scope="scope">{{ scope.row.goodRate != null ? scope.row.goodRate + '%' : '-' }}</template>
        </el-table-column>
        <el-table-column prop="orderCount" label="接单数" width="90" />
        <el-table-column label="从业年限" width="100">
          <template slot-scope="scope">{{ scope.row.workYears ? scope.row.workYears + ' 年' : '-' }}</template>
        </el-table-column>
        <el-table-column label="接单状态" width="110">
          <template slot-scope="scope">
            <el-tag size="mini" :type="statusTagType(scope.row.status)">{{ statusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="mini" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="text" size="mini" @click="handleChangeStatus(scope.row)">改状态</el-button>
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

    <el-dialog
      :title="form.id ? '编辑服务人员' : '新增服务人员'"
      :visible.sync="dialogVisible"
      width="700px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" size="small">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" placeholder="请输入师傅姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="form.phone" maxlength="11" placeholder="用于上门联系" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="性别">
              <el-radio-group v-model="form.sex">
                <el-radio label="1">男</el-radio>
                <el-radio label="0">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="从业年限">
              <el-input-number v-model="form.workYears" :min="0" :max="60" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="服务方式">
              <el-select v-model="form.serviceMode" style="width: 100%">
                <el-option label="上门服务" :value="1" />
                <el-option label="到店服务" :value="2" />
                <el-option label="都支持" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="接单状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="擅长技能" prop="categoryIds">
          <el-select v-model="form.categoryIds" multiple placeholder="选择他能做哪些分类的服务" style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
          <div class="tip">派单时只会把订单派给掌握了对应分类的师傅</div>
        </el-form-item>

        <el-form-item label="个人简介">
          <el-input v-model="form.intro" type="textarea" :rows="2" placeholder="如：10年家政经验，擅长深度保洁" />
        </el-form-item>

        <el-form-item label="资质证书">
          <el-input v-model="form.certImage" placeholder="健康证 / 电工证等资质图片 URL" />
        </el-form-item>
      </el-form>

      <div slot="footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">确 定</el-button>
      </div>
    </el-dialog>

    <!-- 改状态 -->
    <el-dialog title="修改接单状态" :visible.sync="statusVisible" width="380px">
      <el-select v-model="nextStatus" style="width: 100%">
        <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
      </el-select>
      <div slot="footer">
        <el-button @click="statusVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitStatus">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import {
  getProviderPage,
  queryProviderById,
  addProvider,
  editProvider,
  providerStatusByStatus
} from '@/api/provider'
import { queryCategoryList } from '@/api/category'

export default Vue.extend({
  name: 'Provider',

  data() {
    return {
      query: { page: 1, pageSize: 10, name: '', categoryId: null, status: null },
      total: 0,
      tableData: [] as any[],
      categories: [] as any[],
      loading: false,

      statusOptions: [
        { value: 1, label: '可接单' },
        { value: 2, label: '忙碌' },
        { value: 3, label: '休息中' },
        { value: 4, label: '已离职' }
      ],

      dialogVisible: false,
      saving: false,
      form: this.emptyForm(),
      rules: {
        name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
        phone: [
          { required: true, message: '请输入联系电话', trigger: 'blur' },
          { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }
        ],
        categoryIds: [{ required: true, type: 'array', message: '请至少选择一项技能', trigger: 'change' }]
      },

      statusVisible: false,
      currentId: null,
      nextStatus: 1
    }
  },

  created() {
    this.loadCategories()
    this.loadData()
  },

  methods: {
    emptyForm() {
      return {
        id: null,
        name: '',
        phone: '',
        sex: '1',
        workYears: 0,
        serviceMode: 1,
        status: 1,
        categoryIds: [] as number[],
        intro: '',
        certImage: ''
      }
    },

    statusText(s: number) {
      const hit = this.statusOptions.find((o: any) => o.value === s)
      return hit ? hit.label : '-'
    },

    statusTagType(s: number) {
      if (s === 1) return 'success'
      if (s === 2) return 'warning'
      if (s === 3) return 'info'
      return 'danger'
    },

    loadCategories() {
      queryCategoryList({ type: 1 }).then((res: any) => {
        if (res.data.code === 1) this.categories = res.data.data || []
      })
    },

    loadData() {
      this.loading = true
      const params: any = { page: this.query.page, pageSize: this.query.pageSize }
      if (this.query.name) params.name = this.query.name
      if (this.query.categoryId) params.categoryId = this.query.categoryId
      if (this.query.status !== null && this.query.status !== '') params.status = this.query.status

      getProviderPage(params)
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
      this.query = { page: 1, pageSize: 10, name: '', categoryId: null, status: null }
      this.loadData()
    },
    handlePageChange(p: number) { this.query.page = p; this.loadData() },
    handleSizeChange(s: number) { this.query.pageSize = s; this.query.page = 1; this.loadData() },

    handleAdd() {
      this.form = this.emptyForm()
      this.dialogVisible = true
    },

    handleEdit(row: any) {
      queryProviderById(row.id).then((res: any) => {
        if (res.data.code === 1 && res.data.data) {
          const d = res.data.data
          this.form = {
            id: d.id,
            name: d.name,
            phone: d.phone,
            sex: d.sex || '1',
            workYears: d.workYears || 0,
            serviceMode: d.serviceMode || 1,
            status: d.status,
            categoryIds: d.categoryIds || [],
            intro: d.intro || '',
            certImage: d.certImage || ''
          }
          this.dialogVisible = true
        }
      })
    },

    handleSubmit() {
      (this.$refs.formRef as any).validate((valid: boolean) => {
        if (!valid) return
        this.saving = true
        const req = this.form.id ? editProvider(this.form) : addProvider(this.form)
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
          .finally(() => { this.saving = false })
      })
    },

    handleChangeStatus(row: any) {
      this.currentId = row.id
      this.nextStatus = row.status
      this.statusVisible = true
    },

    submitStatus() {
      providerStatusByStatus({ status: this.nextStatus, id: this.currentId }).then((res: any) => {
        if (res.data.code === 1) {
          this.$message.success('状态已更新')
          this.statusVisible = false
          this.loadData()
        } else {
          this.$message.error(res.data.msg || '操作失败')
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
.skill-tag { margin: 2px 4px 2px 0; }
.muted { color: #bbb; }
.score { color: #e6a23c; font-weight: 600; }
.tip { color: #999; font-size: 12px; line-height: 1.5; }
</style>
