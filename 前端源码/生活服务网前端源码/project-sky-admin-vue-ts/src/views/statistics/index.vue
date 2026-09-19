<template>
  <div class="statistics-page">
    <!-- 顶部：日期范围 + 导出 -->
    <el-card shadow="never" class="filter-card">
      <div class="filter-bar">
        <div class="filter-left">
          <span class="filter-label">统计时间</span>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="yyyy-MM-dd"
            :picker-options="pickerOptions"
            @change="handleDateChange"
          />
          <el-radio-group v-model="quickRange" size="small" class="quick-range" @change="handleQuickRange">
            <el-radio-button :label="7">近 7 天</el-radio-button>
            <el-radio-button :label="30">近 30 天</el-radio-button>
            <el-radio-button :label="90">近 90 天</el-radio-button>
          </el-radio-group>
        </div>
        <el-button type="primary" icon="el-icon-download" @click="handleExport">导出报表</el-button>
      </div>
    </el-card>

    <!-- 报表 -->
    <el-card shadow="never" class="chart-card">
      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <!-- 营业额 -->
        <el-tab-pane label="营业额统计" name="turnover">
          <div class="summary-row">
            <div class="summary-item">
              <div class="summary-label">区间营业额</div>
              <div class="summary-value">{{ turnoverTotal }} 元</div>
            </div>
            <div class="summary-item">
              <div class="summary-label">日均营业额</div>
              <div class="summary-value">{{ turnoverAverage }} 元</div>
            </div>
            <div class="summary-item">
              <div class="summary-label">最高单日</div>
              <div class="summary-value">{{ turnoverBest }}</div>
            </div>
          </div>
          <div ref="turnoverChart" class="chart" />
          <p class="chart-tip">
            口径：已付款且服务已完成（待评价 / 已完成）的订单，按支付日期归集。
          </p>
        </el-tab-pane>

        <!-- 用户 -->
        <el-tab-pane label="用户统计" name="user">
          <div class="summary-row">
            <div class="summary-item">
              <div class="summary-label">区间新增用户</div>
              <div class="summary-value">{{ newUserTotal }} 人</div>
            </div>
            <div class="summary-item">
              <div class="summary-label">累计用户</div>
              <div class="summary-value">{{ totalUser }} 人</div>
            </div>
          </div>
          <div ref="userChart" class="chart" />
        </el-tab-pane>

        <!-- 订单 -->
        <el-tab-pane label="订单统计" name="order">
          <div class="summary-row">
            <div class="summary-item">
              <div class="summary-label">订单总数</div>
              <div class="summary-value">{{ orderReport.totalOrderCount || 0 }}</div>
            </div>
            <div class="summary-item">
              <div class="summary-label">有效订单</div>
              <div class="summary-value">{{ orderReport.validOrderCount || 0 }}</div>
            </div>
            <div class="summary-item">
              <div class="summary-label">订单完成率</div>
              <div class="summary-value">{{ orderReport.orderCompletionRate || 0 }}%</div>
            </div>
          </div>
          <div ref="orderChart" class="chart" />
          <p class="chart-tip">有效订单 = 服务确实做完了的订单（待评价 / 已完成），不含未付款和已取消。</p>
        </el-tab-pane>

        <!-- 销量排名 -->
        <el-tab-pane label="销量排名 Top10" name="top10">
          <div ref="top10Chart" class="chart chart-tall" />
          <el-empty v-if="!top10.nameList || top10.nameList.length === 0" description="该时间段还没有已完成的订单" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import * as echarts from 'echarts'
import { Message } from 'element-ui'
import {
  getTurnoverStatistics,
  getUserStatistics,
  getOrdersStatistics,
  getSalesTop10,
  exportReport
} from '@/api/report'

/**
 * 格式化日期为 yyyy-MM-dd。
 * 不用 toISOString()，因为它按 UTC 转换，东八区会少 8 小时，
 * 早上 8 点前取「今天」会得到昨天。
 */
function formatDate(date: Date): string {
  const y = date.getFullYear()
  const m = `${date.getMonth() + 1}`.padStart(2, '0')
  const d = `${date.getDate()}`.padStart(2, '0')
  return `${y}-${m}-${d}`
}

export default Vue.extend({
  name: 'Statistics',

  data() {
    const end = new Date()
    const begin = new Date()
    begin.setDate(begin.getDate() - 6)

    return {
      dateRange: [formatDate(begin), formatDate(end)],
      quickRange: 7,
      activeTab: 'turnover',
      // 已经初始化过的图表，切换 tab 时不用重复创建
      inited: {} as any,
      charts: {} as any,
      turnover: { dateList: [], turnoverList: [] } as any,
      user: { dateList: [], newUserList: [], totalUserList: [] } as any,
      orderReport: {} as any,
      top10: { nameList: [], numberList: [] } as any,
      // 不允许选未来日期：业务上还没有发生的数据查出来全是 0，容易误导
      pickerOptions: {
        disabledDate(time: Date) {
          return time.getTime() > Date.now()
        }
      }
    }
  },

  computed: {
    turnoverTotal(): string {
      const list = this.turnover.turnoverList || []
      return list.reduce((sum: number, v: any) => sum + Number(v || 0), 0).toFixed(2)
    },
    turnoverAverage(): string {
      const list = this.turnover.turnoverList || []
      if (list.length === 0) return '0.00'
      return (Number(this.turnoverTotal) / list.length).toFixed(2)
    },
    turnoverBest(): string {
      const dates = this.turnover.dateList || []
      const list = this.turnover.turnoverList || []
      if (list.length === 0) return '-'
      let max = -1
      let index = 0
      list.forEach((v: any, i: number) => {
        if (Number(v) > max) {
          max = Number(v)
          index = i
        }
      })
      return `${dates[index]}（${max.toFixed(2)} 元）`
    },
    newUserTotal(): number {
      return (this.user.newUserList || []).reduce(
        (sum: number, v: any) => sum + Number(v || 0),
        0
      )
    },
    totalUser(): number {
      const list = this.user.totalUserList || []
      return list.length ? Number(list[list.length - 1]) : 0
    }
  },

  mounted() {
    this.loadTurnover()
    window.addEventListener('resize', this.resizeCharts)
  },

  beforeDestroy() {
    window.removeEventListener('resize', this.resizeCharts)
  },

  methods: {
    getParams() {
      return {
        begin: this.dateRange[0],
        end: this.dateRange[1]
      }
    },

    handleDateChange() {
      // 手动选完日期后，快捷按钮不再高亮
      this.quickRange = 0 as any
      this.reloadAll()
    },

    handleQuickRange(days: number) {
      const end = new Date()
      const begin = new Date()
      begin.setDate(begin.getDate() - (days - 1))
      this.dateRange = [formatDate(begin), formatDate(end)]
      this.reloadAll()
    },

    handleTabClick(tab: any) {
      // tab 第一次打开时才初始化图表：
      // 隐藏的 tab 容器宽度是 0，提前 init 会得到一张宽度为 0 的空白图
      this.$nextTick(() => {
        const name = tab.name
        if (name === 'turnover') this.loadTurnover()
        if (name === 'user') this.loadUser()
        if (name === 'order') this.loadOrders()
        if (name === 'top10') this.loadTop10()
      })
    },

    reloadAll() {
      if (this.activeTab === 'turnover') this.loadTurnover()
      if (this.activeTab === 'user') this.loadUser()
      if (this.activeTab === 'order') this.loadOrders()
      if (this.activeTab === 'top10') this.loadTop10()
    },

    loadTurnover() {
      getTurnoverStatistics(this.getParams()).then((res: any) => {
        if (res.data.code === 1) {
          this.turnover = res.data.data || { dateList: [], turnoverList: [] }
          this.$nextTick(() => {
            this.renderLine('turnoverChart', 'turnover', this.turnover.dateList,
              [{ name: '营业额（元）', data: this.turnover.turnoverList }])
          })
        }
      })
    },

    loadUser() {
      getUserStatistics(this.getParams()).then((res: any) => {
        if (res.data.code === 1) {
          this.user = res.data.data || { dateList: [], newUserList: [], totalUserList: [] }
          this.$nextTick(() => {
            this.renderLine('userChart', 'user', this.user.dateList, [
              { name: '新增用户', data: this.user.newUserList },
              { name: '累计用户', data: this.user.totalUserList }
            ])
          })
        }
      })
    },

    loadOrders() {
      getOrdersStatistics(this.getParams()).then((res: any) => {
        if (res.data.code === 1) {
          this.orderReport = res.data.data || {}
          this.$nextTick(() => {
            this.renderLine('orderChart', 'order', this.orderReport.dateList, [
              { name: '订单总数', data: this.orderReport.orderCountList },
              { name: '有效订单', data: this.orderReport.validOrderCountList }
            ])
          })
        }
      })
    },

    loadTop10() {
      getSalesTop10(this.getParams()).then((res: any) => {
        if (res.data.code === 1) {
          this.top10 = res.data.data || { nameList: [], numberList: [] }
          this.$nextTick(() => {
            const names = (this.top10.nameList || []).slice().reverse()
            const numbers = (this.top10.numberList || []).slice().reverse()
            const chart = this.getChart('top10Chart', 'top10')
            if (!chart) return
            chart.setOption({
              tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
              grid: { left: 140, right: 40, top: 20, bottom: 30 },
              xAxis: { type: 'value', name: '销量' },
              yAxis: { type: 'category', data: names },
              series: [{
                name: '销量',
                type: 'bar',
                data: numbers,
                barMaxWidth: 22,
                itemStyle: { color: '#2B6DE8', borderRadius: [0, 4, 4, 0] },
                label: { show: true, position: 'right' }
              }]
            }, true)
          })
        }
      })
    },

    /**
     * 折线图统一画法：两条曲线的量级可能差很多（比如新增 3 人 / 累计 300 人），
     * 所以数量级差异超过 10 倍时给第二条线挂到右轴，避免一条被压成直线
     */
    renderLine(refName: string, key: string, dateList: string[], series: any[]) {
      const chart = this.getChart(refName, key)
      if (!chart) return

      const useSecondAxis = series.length > 1 && this.isDifferentScale(series[0].data, series[1].data)
      const option: any = {
        tooltip: { trigger: 'axis' },
        legend: { data: series.map((s) => s.name), right: 10 },
        grid: { left: 60, right: useSecondAxis ? 70 : 40, top: 50, bottom: 40 },
        xAxis: { type: 'category', boundaryGap: false, data: dateList },
        yAxis: [{ type: 'value' }],
        series: series.map((s, i) => ({
          name: s.name,
          type: 'line',
          smooth: true,
          showSymbol: dateList.length <= 31,
          yAxisIndex: useSecondAxis && i === 1 ? 1 : 0,
          areaStyle: series.length === 1 ? { opacity: 0.12 } : undefined,
          data: s.data || []
        }))
      }
      if (useSecondAxis) {
        option.yAxis.push({ type: 'value' })
      }
      chart.setOption(option, true)
    },

    isDifferentScale(a: any[], b: any[]): boolean {
      const max = (arr: any[]) => Math.max(...(arr || []).map((v) => Number(v || 0)), 0)
      const maxA = max(a)
      const maxB = max(b)
      if (maxA === 0 || maxB === 0) return false
      return Math.max(maxA, maxB) / Math.min(maxA, maxB) > 10
    },

    getChart(refName: string, key: string) {
      if (!this.inited[key]) {
        const dom = this.$refs[refName] as HTMLElement
        if (!dom) return null
        this.charts[key] = echarts.init(dom)
        this.inited[key] = true
      }
      return this.charts[key]
    },

    resizeCharts() {
      Object.keys(this.charts).forEach((key) => {
        if (this.charts[key]) this.charts[key].resize()
      })
    },

    handleExport() {
      exportReport(this.getParams()).then((res: any) => {
        const blob = new Blob([res.data], {
          type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = `运营数据报表_${this.dateRange[0]}_${this.dateRange[1]}.xlsx`
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)
        Message.success('报表已开始下载')
      }).catch(() => {
        Message.error('导出失败，请确认后端服务是否正常')
      })
    }
  }
})
</script>

<style lang="scss" scoped>
.statistics-page {
  padding: 20px;

  .filter-card {
    margin-bottom: 16px;
  }

  .filter-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .filter-left {
    display: flex;
    align-items: center;
  }

  .filter-label {
    margin-right: 12px;
    color: #606266;
    font-size: 14px;
  }

  .quick-range {
    margin-left: 16px;
  }

  .summary-row {
    display: flex;
    gap: 40px;
    padding: 8px 4px 20px;
  }

  .summary-label {
    color: #909399;
    font-size: 13px;
  }

  .summary-value {
    margin-top: 6px;
    color: #303133;
    font-size: 22px;
    font-weight: 600;
  }

  .chart {
    width: 100%;
    height: 380px;
  }

  .chart-tall {
    height: 460px;
  }

  .chart-tip {
    margin: 4px 0 0;
    color: #a8abb2;
    font-size: 12px;
  }
}
</style>
