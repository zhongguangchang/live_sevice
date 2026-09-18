<template>
  <div class="page-wrap">
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" size="small">
        <el-form-item label="套餐名称">
          <el-input v-model="query.name" placeholder="请输入套餐名称" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="套餐分类">
          <el-select v-model="query.categoryId" placeholder="全部分类" clearable style="width: 160px">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="primary" plain @click="handleAdd">+ 新增套餐</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="tableData" border stripe size="small">
        <el-table-column prop="name" label="套餐名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="分类" width="120" />
        <el-table-column label="套餐价" width="110">
          <template slot-scope="scope">
            <span class="price">¥{{ scope.row.price }}</span>
            <span v-if="scope.row.originalPrice" class="origin-price">¥{{ scope.row.originalPrice }}</span>
          </template>
        </el-table-column>
        <el-table-column label="包含服务" min-width="220">
          <template slot-scope="scope">
            <el-tag v-for="(it, i) in scope.row.items" :key="i" size="mini" class="item-tag">
              {{ it.name }} x{{ it.copies }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sales" label="销量" width="80" />
        <el-table-column label="状态" width="90">
          <template slot-scope="scope">
            <el-tag size="mini" :type="scope.row.status === 1 ? 'success' : 'info'">
              {{ scope.row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="mini" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="text" size="mini" @click="toggleStatus(scope.row)">
              {{ scope.row.status === 1 ? '停用' : '启用' }}
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

    <el-dialog
      :title="form.id ? '编辑服务套餐' : '新增服务套餐'"
      :visible.sync="dialogVisible"
      width="720px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" size="small">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="套餐名称" prop="name">
              <el-input v-model="form.name" placeholder="如：新居开荒保洁套餐" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="套餐分类" prop="categoryId">
              <el-select v-model="form.categoryId" placeholder="请选择" style="width: 100%">
                <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="套餐价格" prop="price">
              <el-input-number v-model="form.price" :min="0" :precision="2" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="原价">
              <el-input-number v-model="form.originalPrice" :min="0" :precision="2" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="总时长">
              <el-input-number v-model="form.duration" :min="10" :step="10" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="服务方式">
          <el-select v-model="form.serviceMode" style="width: 200px">
            <el-option label="上门服务" :value="1" />
            <el-option label="到店服务" :value="2" />
            <el-option label="都支持" :value="3" />
          </el-select>
        </el-form-item>

        <el-form-item label="套餐图片">
          <el-input v-model="form.image" placeholder="图片 URL" />
        </el-form-item>

        <el-form-item label="套餐描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>

        <el-form-item label="包含服务" prop="items">
          <div class="item-editor">
            <div v-for="(it, i) in form.items" :key="i" class="item-row">
              <el-select
                v-model="it.serviceId"
                placeholder="选择服务项目"
                style="width: 260px"
                @change="(v) => onServiceChange(v, i)"
              >
                <el-option v-for="s in serviceItems" :key="s.id" :label="s.name + '  ¥' + s.price" :value="s.id" />
              </el-select>
              <el-input-number v-model="it.copies" :min="1" :max="20" style="margin-left: 8px" />
              <span class="unit">份</span>
              <el-button type="text" class="danger-text" style="margin-left: 8px" @click="removeItem(i)">删除</el-button>
            </div>
            <el-button type="text" @click="addItem">+ 添加服务</el-button>
            <div class="tip">编辑套餐时明细会整体重写，所以每次保存都要把包含的服务完整选一遍</div>
          </div>
        </el-form-item>

        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <div slot="footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import {
  getServicePackagePage,
  queryServicePackageById,
  addServicePackage,
  editServicePackage,
  deleteServicePackage,
  servicePackageStatusByStatus
} from '@/api/servicePackage'
import { queryServiceItemList } from '@/api/serviceItem'
import { queryCategoryList } from '@/api/category'

// 表单初始值。放在模块作用域而不是 methods 里，
// 因为 data() 里要用它，而 data() 执行时 this 上还没有 methods，
// TypeScript 会报 "Property 'emptyForm' does not exist"。
function emptyForm() {
  return {
    id: null,
    name: '',
    categoryId: null,
    price: 0,
    originalPrice: null,
    image: '',
    description: '',
    duration: 120,
    serviceMode: 1,
    status: 1,
    items: [] as any[]
  }
}

export default Vue.extend({
  name: 'ServicePackage',

  data() {
    return {
      query: { page: 1, pageSize: 10, name: '', categoryId: null, status: null },
      total: 0,
      tableData: [] as any[],
      categories: [] as any[],
      serviceItems: [] as any[],
      loading: false,

      dialogVisible: false,
      saving: false,
      form: emptyForm(),
      rules: {
        name: [{ required: true, message: '请输入套餐名称', trigger: 'blur' }],
        categoryId: [{ required: true, message: '请选择套餐分类', trigger: 'change' }],
        price: [{ required: true, message: '请输入套餐价格', trigger: 'blur' }],
        items: [{ required: true, type: 'array', message: '请至少添加一个服务', trigger: 'change' }]
      }
    }
  },

  created() {
    this.loadCategories()
    this.loadServiceItems()
    this.loadData()
  },

  methods: {
    emptyForm,

    loadCategories() {
      // 套餐分类是 type = 2，和服务项目分类分开
      queryCategoryList({ type: 2 }).then((res: any) => {
        if (res.data.code === 1) this.categories = res.data.data || []
      })
    },

    loadServiceItems() {
      queryServiceItemList({ status: 1 }).then((res: any) => {
        if (res.data.code === 1) this.serviceItems = res.data.data || []
      })
    },

    loadData() {
      this.loading = true
      const params: any = { page: this.query.page, pageSize: this.query.pageSize }
      if (this.query.name) params.name = this.query.name
      if (this.query.categoryId) params.categoryId = this.query.categoryId
      if (this.query.status !== null && this.query.status !== '') params.status = this.query.status

      getServicePackagePage(params)
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
      queryServicePackageById(row.id).then((res: any) => {
        if (res.data.code === 1 && res.data.data) {
          const d = res.data.data
          this.form = {
            id: d.id,
            name: d.name,
            categoryId: d.categoryId,
            price: Number(d.price),
            originalPrice: d.originalPrice ? Number(d.originalPrice) : null,
            image: d.image || '',
            description: d.description || '',
            duration: d.duration || 120,
            serviceMode: d.serviceMode || 1,
            status: d.status,
            items: (d.items || []).map((it: any) => ({
              serviceId: it.serviceId,
              name: it.name,
              price: Number(it.price || 0),
              copies: it.copies || 1
            }))
          }
          this.dialogVisible = true
        }
      })
    },

    addItem() {
      this.form.items.push({ serviceId: null, name: '', price: 0, copies: 1 })
    },

    removeItem(i: number) {
      this.form.items.splice(i, 1)
    },

    // 选中服务后自动带上名称和单价，后端存的是快照，这样改名调价不影响历史套餐
    onServiceChange(serviceId: any, i: number) {
      const hit = this.serviceItems.find((s: any) => s.id === serviceId)
      if (hit) {
        this.form.items[i].name = hit.name
        this.form.items[i].price = Number(hit.price)
      }
    },

    handleSubmit() {
      (this.$refs.formRef as any).validate((valid: boolean) => {
        if (!valid) return
        const items = this.form.items.filter((it: any) => it.serviceId)
        if (!items.length) {
          this.$message.warning('请至少添加一个服务')
          return
        }
        this.saving = true
        const payload: any = { ...this.form, items }
        const req = this.form.id ? editServicePackage(payload) : addServicePackage(payload)
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

    toggleStatus(row: any) {
      const next = row.status === 1 ? 0 : 1
      servicePackageStatusByStatus({ status: next, id: row.id }).then((res: any) => {
        if (res.data.code === 1) {
          this.$message.success(next === 1 ? '已启用' : '已停用')
          this.loadData()
        } else {
          // 套餐内有已停售的服务时后端会拒绝启用
          this.$message.error(res.data.msg || '操作失败')
        }
      })
    },

    handleDelete(row: any) {
      this.$confirm('确认删除套餐「' + row.name + '」吗？', '提示', { type: 'warning' })
        .then(() => {
          deleteServicePackage(String(row.id)).then((res: any) => {
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
.pager { margin-top: 14px; text-align: right; }
.price { color: #F56C6C; font-weight: 600; }
.origin-price { margin-left: 6px; color: #999; text-decoration: line-through; font-size: 12px; }
.item-tag { margin: 2px 4px 2px 0; }
.item-editor { width: 100%; }
.item-row { display: flex; align-items: center; margin-bottom: 8px; }
.unit { margin-left: 6px; color: #666; font-size: 12px; }
.tip { color: #999; font-size: 12px; line-height: 1.5; }
.danger-text { color: #f56c6c; }
</style>
