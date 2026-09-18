<template>
  <div class="page-wrap">
    <el-card shadow="never" class="search-card">
      <div slot="header" class="card-head">
        <span>批量生成排期</span>
        <span class="tip">选了日期区间 x 师傅 x 服务 x 时段后，系统按笛卡尔积批量生成，并自动把库存预热到 Redis</span>
      </div>
      <el-form :model="genForm" size="small" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="10">
            <el-form-item label="日期区间">
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                value-format="yyyy-MM-dd"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="7">
            <el-form-item label="每个时段">
              <el-input-number v-model="genForm.slotDuration" :min="30" :step="30" style="width: 120px" />
              <span class="unit">分钟</span>
            </el-form-item>
          </el-col>
          <el-col :span="7">
            <el-form-item label="名额">
              <el-input-number v-model="genForm.totalStock" :min="1" style="width: 120px" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="服务人员">
              <el-select v-model="genForm.providerIds" multiple placeholder="可多选" style="width: 100%">
                <el-option v-for="p in providers" :key="p.id" :label="p.name" :value="p.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="服务项目">
              <el-select v-model="genForm.serviceIds" multiple placeholder="可多选" style="width: 100%">
                <el-option v-for="s in serviceItems" :key="s.id" :label="s.name" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="时段起点">
          <el-time-select
            v-for="(t, i) in genForm.startTimes"
            :key="i"
            v-model="genForm.startTimes[i]"
            start="06:00"
            step="00:30"
            end="22:00"
            placeholder="选择时间"
            style="width: 130px; margin-right: 8px"
          />
          <el-button type="text" @click="addTime">+ 加时段</el-button>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="generating" @click="submitGenerate">生成排期</el-button>
          <el-button @click="handleWarmUp">手动预热库存到 Redis</el-button>
          <el-button @click="handleReconcile">库存对账</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <div slot="header">排期列表</div>
      <el-form :inline="true" :model="query" size="small" style="margin-bottom: 10px">
        <el-form-item label="服务人员">
          <el-select v-model="query.providerId" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="p in providers" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="query.serviceDate" type="date" value-format="yyyy-MM-dd" placeholder="选择日期" style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadSlots">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="slots" border stripe size="small" max-height="420">
        <el-table-column prop="serviceDate" label="服务日期" width="120" />
        <el-table-column label="时段" width="150">
          <template slot-scope="scope">{{ scope.row.startTime }} ~ {{ scope.row.endTime }}</template>
        </el-table-column>
        <el-table-column prop="providerName" label="服务人员" width="110" />
        <el-table-column prop="serviceId" label="服务项目ID" width="110" />
        <el-table-column label="名额" width="140">
          <template slot-scope="scope">
            {{ scope.row.bookedCount }} / {{ scope.row.totalStock }}
            <el-progress
              :percentage="percent(scope.row)"
              :show-text="false"
              :stroke-width="6"
              style="margin-top: 4px"
            />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template slot-scope="scope">
            <el-tag size="mini" :type="scope.row.status === 1 ? 'success' : 'info'">
              {{ scope.row.status === 1 ? '可预约' : '已关闭' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template slot-scope="scope">
            <el-button type="text" size="mini" @click="toggleStatus(scope.row)">
              {{ scope.row.status === 1 ? '关闭' : '开放' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import { batchCreateSlot, querySlotList, slotStatusByStatus, warmUpSlotStock, reconcileSlotStock } from '@/api/slot'
import { queryProviderList } from '@/api/provider'
import { queryServiceItemList } from '@/api/serviceItem'

export default Vue.extend({
  name: 'Slot',

  data() {
    return {
      dateRange: [] as string[],
      genForm: {
        providerIds: [] as number[],
        serviceIds: [] as number[],
        startTimes: ['09:00', '14:00'],
        slotDuration: 120,
        totalStock: 1
      },
      generating: false,

      providers: [] as any[],
      serviceItems: [] as any[],

      query: { providerId: null, serviceDate: '' },
      slots: [] as any[],
      loading: false
    }
  },

  created() {
    this.loadOptions()
    this.loadSlots()
  },

  methods: {
    loadOptions() {
      queryProviderList({}).then((res: any) => {
        if (res.data.code === 1) this.providers = (res.data.data || []).filter((p: any) => p.status === 1)
      })
      queryServiceItemList({ status: 1 }).then((res: any) => {
        if (res.data.code === 1) this.serviceItems = res.data.data || []
      })
    },

    addTime() {
      this.genForm.startTimes.push('09:00')
    },

    percent(row: any) {
      if (!row.totalStock) return 0
      return Math.round((row.bookedCount / row.totalStock) * 100)
    },

    submitGenerate() {
      if (!this.dateRange || this.dateRange.length !== 2) {
        this.$message.warning('请选择日期区间')
        return
      }
      if (!this.genForm.providerIds.length || !this.genForm.serviceIds.length) {
        this.$message.warning('请至少选择一位服务人员和一个服务项目')
        return
      }
      const times = this.genForm.startTimes.filter((t: string) => !!t)
      if (!times.length) {
        this.$message.warning('请至少设置一个时段')
        return
      }

      this.generating = true
      batchCreateSlot({
        providerIds: this.genForm.providerIds,
        serviceIds: this.genForm.serviceIds,
        startDate: this.dateRange[0],
        endDate: this.dateRange[1],
        startTimes: times,
        slotDuration: this.genForm.slotDuration,
        totalStock: this.genForm.totalStock
      })
        .then((res: any) => {
          if (res.data.code === 1) {
            this.$message.success('排期生成成功，库存已预热到 Redis')
            this.loadSlots()
          } else {
            this.$message.error(res.data.msg || '生成失败')
          }
        })
        .catch((err: any) => this.$message.error('请求失败：' + err.message))
        .finally(() => { this.generating = false })
    },

    loadSlots() {
      this.loading = true
      const params: any = {}
      if (this.query.providerId) params.providerId = this.query.providerId
      if (this.query.serviceDate) params.serviceDate = this.query.serviceDate
      querySlotList(params)
        .then((res: any) => {
          if (res.data.code === 1) this.slots = res.data.data || []
        })
        .catch((err: any) => this.$message.error('请求失败：' + err.message))
        .finally(() => { this.loading = false })
    },

    toggleStatus(row: any) {
      const next = row.status === 1 ? 0 : 1
      slotStatusByStatus({ status: next, id: row.id }).then((res: any) => {
        if (res.data.code === 1) {
          this.$message.success(next === 1 ? '已开放' : '已关闭')
          this.loadSlots()
        }
      })
    },

    handleWarmUp() {
      if (!this.dateRange || this.dateRange.length !== 2) {
        this.$message.warning('请先在上方选择日期区间')
        return
      }
      warmUpSlotStock({ begin: this.dateRange[0], end: this.dateRange[1] }).then((res: any) => {
        if (res.data.code === 1) {
          this.$message.success('已预热 ' + res.data.data + ' 个时段到 Redis')
        } else {
          this.$message.error(res.data.msg || '预热失败')
        }
      })
    },

    handleReconcile() {
      reconcileSlotStock().then((res: any) => {
        if (res.data.code === 1) {
          this.$message.success('对账完成，修正了 ' + res.data.data + ' 个时段')
          this.loadSlots()
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
.card-head { display: flex; align-items: baseline; }
.card-head .tip { margin-left: 12px; color: #999; font-size: 12px; font-weight: 400; }
.unit { margin-left: 6px; color: #666; font-size: 12px; }
</style>
