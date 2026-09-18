<template>
  <div class="page-wrap">
    <!-- 搜索条件 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" size="small">
        <el-form-item label="服务名称">
          <el-input v-model="query.name" placeholder="请输入服务名称" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="服务分类">
          <el-select v-model="query.categoryId" placeholder="全部分类" clearable style="width: 150px">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px">
            <el-option label="起售" :value="1" />
            <el-option label="停售" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="primary" plain @click="handleAdd">+ 新增服务项目</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="tableData" border stripe size="small">
        <el-table-column prop="name" label="服务名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="分类" width="110" />
        <el-table-column label="价格" width="120">
          <template slot-scope="scope">
            <span class="price">¥{{ scope.row.price }}</span>
            <span v-if="scope.row.originalPrice" class="origin-price">¥{{ scope.row.originalPrice }}</span>
          </template>
        </el-table-column>
        <el-table-column label="时长" width="90">
          <template slot-scope="scope">{{ scope.row.duration }} 分钟</template>
        </el-table-column>
        <el-table-column label="服务方式" width="100">
          <template slot-scope="scope">
            <el-tag size="mini" :type="scope.row.serviceMode === 1 ? 'success' : 'warning'">
              {{ serviceModeText(scope.row.serviceMode) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="规格" min-width="180">
          <template slot-scope="scope">
            <template v-if="scope.row.specs && scope.row.specs.length">
              <el-tag
                v-for="(s, i) in scope.row.specs"
                :key="i"
                size="mini"
                class="spec-tag"
              >{{ s.name }}:{{ s.value }}<span v-if="Number(s.priceDelta) > 0">+{{ s.priceDelta }}元</span></el-tag>
            </template>
            <span v-else class="muted">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="sales" label="销量" width="80" />
        <el-table-column label="评分" width="80">
          <template slot-scope="scope">{{ scope.row.score || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template slot-scope="scope">
            <el-tag size="mini" :type="scope.row.status === 1 ? 'success' : 'info'">
              {{ scope.row.status === 1 ? '起售' : '停售' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="mini" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="text" size="mini" @click="handleStatus(scope.row)">
              {{ scope.row.status === 1 ? '停售' : '起售' }}
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

    <!-- 新增 / 编辑 -->
    <el-dialog
      :title="form.id ? '编辑服务项目' : '新增服务项目'"
      :visible.sync="dialogVisible"
      width="760px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" size="small">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="服务名称" prop="name">
              <el-input v-model="form.name" placeholder="如：深度保洁4小时" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="服务分类" prop="categoryId">
              <el-select v-model="form.categoryId" placeholder="请选择" style="width: 100%">
                <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="服务价格" prop="price">
              <el-input-number v-model="form.price" :min="0" :precision="2" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="原价">
              <el-input-number v-model="form.originalPrice" :min="0" :precision="2" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="计价单位">
              <el-select v-model="form.unit" style="width: 100%">
                <el-option v-for="u in ['次', '小时', '平方米', '台', '单']" :key="u" :label="u" :value="u" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="服务时长">
              <el-input-number v-model="form.duration" :min="10" :step="10" style="width: 100%" />
              <div class="tip">单位分钟，用于排期占时段</div>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="服务方式">
              <el-select v-model="form.serviceMode" style="width: 100%">
                <el-option label="上门服务" :value="1" />
                <el-option label="到店服务" :value="2" />
                <el-option label="都支持" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否需要预约">
              <el-radio-group v-model="form.needAppoint">
                <el-radio :label="1">需要</el-radio>
                <el-radio :label="0">不需要</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="服务图片">
          <el-input v-model="form.image" placeholder="图片 URL" />
        </el-form-item>

        <el-form-item label="服务描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="一句话说明这个服务做什么" />
        </el-form-item>

        <!-- 规格 -->
        <el-form-item label="服务规格">
          <div class="spec-editor">
            <div v-for="(s, i) in form.specs" :key="i" class="spec-row">
              <el-input v-model="s.name" placeholder="规格名，如 房屋面积" style="width: 180px" />
              <el-input v-model="s.value" placeholder="规格值，如 60-90平方米" style="width: 200px" />
              <el-input-number v-model="s.priceDelta" :precision="2" :controls="false" placeholder="加价" style="width: 110px" />
              <span class="unit-text">元</span>
              <el-button type="text" class="danger-text" @click="removeSpec(i)">删除</el-button>
            </div>
            <el-button type="text" @click="addSpec">+ 添加规格</el-button>
            <div class="tip">规格用于表达同服务不同档位的价格差异，比如保洁按面积分档。不需要就留空。</div>
          </div>
        </el-form-item>

        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">起售</el-radio>
            <el-radio :label="0">停售</el-radio>
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
  getServiceItemPage,
  queryServiceItemById,
  addServiceItem,
  editServiceItem,
  deleteServiceItem,
  serviceItemStatusByStatus
} from '@/api/serviceItem'
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
    unit: '次',
    duration: 60,
    serviceMode: 1,
    needAppoint: 1,
    image: '',
    description: '',
    status: 1,
    specs: [] as any[]
  }
}

export default Vue.extend({
  name: 'ServiceItem',

  data() {
    return {
      query: {
        page: 1,
        pageSize: 10,
        name: '',
        categoryId: null,
        status: null
      },
      total: 0,
      tableData: [] as any[],
      categories: [] as any[],
      loading: false,

      dialogVisible: false,
      saving: false,
      form: emptyForm(),
      rules: {
        name: [{ required: true, message: '请输入服务名称', trigger: 'blur' }],
        categoryId: [{ required: true, message: '请选择服务分类', trigger: 'change' }],
        price: [{ required: true, message: '请输入服务价格', trigger: 'blur' }]
      }
    }
  },

  created() {
    this.loadCategories()
    this.loadData()
  },

  methods: {
    emptyForm,

    serviceModeText(mode: number) {
      if (mode === 1) return '上门'
      if (mode === 2) return '到店'
      if (mode === 3) return '都支持'
      return '-'
    },

    // 只取服务项目分类（type = 1），套餐分类不在这里出现
    loadCategories() {
      queryCategoryList({ type: 1 }).then((res: any) => {
        if (res.data.code === 1) {
          this.categories = res.data.data || []
        }
      })
    },

    loadData() {
      this.loading = true
      const params: any = {
        page: this.query.page,
        pageSize: this.query.pageSize
      }
      if (this.query.name) params.name = this.query.name
      if (this.query.categoryId) params.categoryId = this.query.categoryId
      if (this.query.status !== null && this.query.status !== '') params.status = this.query.status

      getServiceItemPage(params)
        .then((res: any) => {
          if (res.data.code === 1) {
            this.tableData = (res.data.data && res.data.data.records) || []
            this.total = Number((res.data.data && res.data.data.total) || 0)
          } else {
            this.$message.error(res.data.msg || '查询失败')
          }
        })
        .catch((err: any) => {
          this.$message.error('请求失败：' + err.message)
        })
        .finally(() => {
          this.loading = false
        })
    },

    handleSearch() {
      this.query.page = 1
      this.loadData()
    },

    handleReset() {
      this.query = { page: 1, pageSize: 10, name: '', categoryId: null, status: null }
      this.loadData()
    },

    handlePageChange(p: number) {
      this.query.page = p
      this.loadData()
    },

    handleSizeChange(s: number) {
      this.query.pageSize = s
      this.query.page = 1
      this.loadData()
    },

    handleAdd() {
      this.form = this.emptyForm()
      this.dialogVisible = true
    },

    handleEdit(row: any) {
      queryServiceItemById(row.id).then((res: any) => {
        if (res.data.code === 1 && res.data.data) {
          const d = res.data.data
          this.form = {
            id: d.id,
            name: d.name,
            categoryId: d.categoryId,
            price: Number(d.price),
            originalPrice: d.originalPrice ? Number(d.originalPrice) : null,
            unit: d.unit || '次',
            duration: d.duration || 60,
            serviceMode: d.serviceMode || 1,
            needAppoint: d.needAppoint === 0 ? 0 : 1,
            image: d.image || '',
            description: d.description || '',
            status: d.status,
            specs: (d.specs || []).map((s: any) => ({
              name: s.name,
              value: s.value,
              priceDelta: Number(s.priceDelta || 0)
            }))
          }
          this.dialogVisible = true
        }
      })
    },

    addSpec() {
      this.form.specs.push({ name: '', value: '', priceDelta: 0 })
    },

    removeSpec(i: number) {
      this.form.specs.splice(i, 1)
    },

    handleSubmit() {
      (this.$refs.formRef as any).validate((valid: boolean) => {
        if (!valid) return

        // 规格名和规格值都填了才提交，避免产生半截数据
        const specs = this.form.specs.filter((s: any) => s.name && s.value)
        const payload: any = { ...this.form, specs }

        this.saving = true
        const req = this.form.id ? editServiceItem(payload) : addServiceItem(payload)
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
          .catch((err: any) => {
            this.$message.error('请求失败：' + err.message)
          })
          .finally(() => {
            this.saving = false
          })
      })
    },

    handleStatus(row: any) {
      const next = row.status === 1 ? 0 : 1
      serviceItemStatusByStatus({ status: next, id: row.id }).then((res: any) => {
        if (res.data.code === 1) {
          this.$message.success(next === 1 ? '已起售' : '已停售')
          this.loadData()
        } else {
          this.$message.error(res.data.msg || '操作失败')
        }
      })
    },

    handleDelete(row: any) {
      this.$confirm('确认删除服务项目「' + row.name + '」吗？', '提示', {
        type: 'warning'
      })
        .then(() => {
          deleteServiceItem(String(row.id)).then((res: any) => {
            if (res.data.code === 1) {
              this.$message.success('删除成功')
              this.loadData()
            } else {
              // 起售中或被套餐引用时后端会返回具体原因
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
.page-wrap {
  padding: 16px;
}
.search-card {
  margin-bottom: 12px;
}
.table-card {
  margin-bottom: 16px;
}
.pager {
  margin-top: 14px;
  text-align: right;
}
.price {
  color: #F56C6C;
  font-weight: 600;
}
.origin-price {
  margin-left: 6px;
  color: #999;
  text-decoration: line-through;
  font-size: 12px;
}
.spec-tag {
  margin: 2px 4px 2px 0;
}
.muted {
  color: #bbb;
}
.tip {
  color: #999;
  font-size: 12px;
  line-height: 1.5;
}
.spec-editor {
  width: 100%;
}
.spec-row {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}
.spec-row > * {
  margin-right: 8px;
}
.unit-text {
  color: #666;
  font-size: 12px;
}
.danger-text {
  color: #f56c6c;
}
</style>
