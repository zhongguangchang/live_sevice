<template>
  <div class="dashboard">
    <!-- 顶部欢迎条 -->
    <div class="welcome">
      <div class="welcome-text">
        <h2>生活服务网 · 运营工作台</h2>
        <p>在线预约上门服务，派单、服务进度、评价全流程在这里统一管理</p>
      </div>
      <div class="welcome-actions">
        <el-button type="primary" @click="$router.push('/serviceItem')">上架服务</el-button>
        <el-button @click="$router.push('/slot')">去排期</el-button>
        <el-button @click="$router.push('/serviceOrder')">去派单</el-button>
      </div>
    </div>

    <!-- 订单状态 -->
    <div class="section-title">待处理订单</div>
    <el-row :gutter="14" class="stat-row">
      <el-col :span="6" v-for="s in orderStats" :key="s.key">
        <div class="stat-card" @click="$router.push('/serviceOrder')">
          <div class="stat-icon" :style="{ background: s.bg, color: s.color }">
            <i :class="s.icon" />
          </div>
          <div class="stat-body">
            <div class="stat-num">{{ overview[s.key] || 0 }}</div>
            <div class="stat-label">{{ s.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 今日经营 + 近 7 天趋势 -->
    <div class="section-title">今日经营</div>
    <el-row :gutter="14">
      <el-col :span="6" v-for="t in todayStats" :key="t.key">
        <div class="today-card">
          <div class="today-label">{{ t.label }}</div>
          <div class="today-num">{{ t.value }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="today-card turnover-card">
          <div class="today-label">近 7 天营业额趋势</div>
          <div ref="trendChart" class="trend-chart" />
        </div>
      </el-col>
    </el-row>

    <!-- 资源概览 -->
    <div class="section-title">平台资源</div>
    <el-row :gutter="14">
      <el-col :span="8" v-for="r in resources" :key="r.key">
        <div class="res-card">
          <div class="res-label">{{ r.label }}</div>
          <div class="res-num">{{ r.value }}</div>
          <div class="res-link" @click="$router.push(r.path)">{{ r.action }} →</div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import * as echarts from 'echarts'
import { getBusinessOverview } from '@/api/report'

export default Vue.extend({
  name: 'Dashboard',

  data() {
    return {
      // 所有数字都来自同一个概览接口：
      // 原来要调 4 个接口各取一个字段，任何一次失败都会让页面缺一块
      overview: {} as any,
      trendChart: null as any,
      orderStats: [
        { key: 'toBeAccepted', label: '待接单', icon: 'el-icon-bell', color: '#E6A23C', bg: '#FDF6EC' },
        { key: 'accepted', label: '已接单', icon: 'el-icon-user', color: '#2B6DE8', bg: '#ECF2FE' },
        { key: 'inService', label: '服务中', icon: 'el-icon-time', color: '#67C23A', bg: '#F0F9EB' },
        { key: 'toBeReviewed', label: '待评价', icon: 'el-icon-chat-dot-round', color: '#909399', bg: '#F4F4F5' }
      ],
      todayStats: [
        { key: 'todayTurnover', label: '今日营业额（元）', value: '0.00' },
        { key: 'todayOrderCount', label: '今日新增订单', value: '0' },
        { key: 'todayUserCount', label: '今日新增用户', value: '0' }
      ] as any[],
      resources: [
        { key: 'serviceItemCount', label: '在售服务项目', value: '-', action: '管理服务项目', path: '/serviceItem' },
        { key: 'servicePackageCount', label: '已启用套餐', value: '-', action: '管理服务套餐', path: '/servicePackage' },
        { key: 'providerCount', label: '可接单服务人员', value: '-', action: '管理服务人员', path: '/provider' }
      ] as any[]
    }
  },

  created() {
    this.loadOverview()
  },

  mounted() {
    window.addEventListener('resize', this.resizeChart)
  },

  beforeDestroy() {
    window.removeEventListener('resize', this.resizeChart)
  },

  methods: {
    loadOverview() {
      getBusinessOverview().then((res: any) => {
        if (res.data.code !== 1) return
        this.overview = res.data.data || {}

        this.todayStats[0].value = Number(this.overview.todayTurnover || 0).toFixed(2)
        this.todayStats[1].value = String(this.overview.todayOrderCount || 0)
        this.todayStats[2].value = String(this.overview.todayUserCount || 0)

        this.resources.forEach((r) => {
          r.value = String(this.overview[r.key] || 0)
        })

        this.$nextTick(this.renderTrend)
      })
    },

    renderTrend() {
      const dom = this.$refs.trendChart as HTMLElement
      if (!dom) return
      if (!this.trendChart) this.trendChart = echarts.init(dom)
      this.trendChart.setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: 8, right: 12, top: 20, bottom: 4, containLabel: true },
        xAxis: {
          type: 'category',
          data: this.overview.recentDateList || [],
          axisLabel: { fontSize: 10 }
        },
        yAxis: { type: 'value', show: false },
        series: [{
          type: 'line',
          smooth: true,
          symbolSize: 5,
          data: this.overview.recentTurnoverList || [],
          itemStyle: { color: '#2B6DE8' },
          areaStyle: { opacity: 0.15 }
        }]
      }, true)
    },

    resizeChart() {
      if (this.trendChart) this.trendChart.resize()
    }
  }
})
</script>

<style lang="scss" scoped>
.dashboard {
  padding: 16px;
}

.welcome {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 22px 26px;
  border-radius: 10px;
  color: #fff;
  background: linear-gradient(120deg, #1E2C4C 0%, #2B6DE8 100%);

  h2 {
    margin: 0 0 6px;
    font-size: 19px;
    font-weight: 600;
    letter-spacing: 1px;
  }
  p {
    margin: 0;
    font-size: 13px;
    color: rgba(255, 255, 255, 0.78);
  }
  .welcome-actions {
    .el-button {
      border: 1px solid rgba(255, 255, 255, 0.55);
      background: rgba(255, 255, 255, 0.12);
      color: #fff;
      &:hover {
        background: rgba(255, 255, 255, 0.24);
        color: #fff;
      }
    }
  }
}

.section-title {
  margin: 22px 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: #1B2437;
}

.stat-row {
  margin-bottom: 4px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 18px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #EBEEF5;
  cursor: pointer;
  transition: box-shadow 0.2s;
  &:hover {
    box-shadow: 0 4px 16px rgba(43, 109, 232, 0.12);
  }
  .stat-icon {
    width: 46px;
    height: 46px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 22px;
  }
  .stat-body {
    margin-left: 14px;
  }
  .stat-num {
    font-size: 24px;
    font-weight: 700;
    color: #1B2437;
    line-height: 1.2;
  }
  .stat-label {
    margin-top: 2px;
    font-size: 13px;
    color: #909399;
  }
}

.today-card {
  height: 96px;
  padding: 18px 20px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #EBEEF5;

  .today-label {
    font-size: 13px;
    color: #909399;
  }
  .today-num {
    margin-top: 8px;
    font-size: 26px;
    font-weight: 700;
    color: #1B2437;
  }
}

.turnover-card {
  padding-bottom: 6px;
}

.trend-chart {
  width: 100%;
  height: 46px;
}

.res-card {
  padding: 20px 22px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #EBEEF5;

  .res-label {
    font-size: 13px;
    color: #909399;
  }
  .res-num {
    margin: 8px 0 10px;
    font-size: 28px;
    font-weight: 700;
    color: #2B6DE8;
  }
  .res-link {
    font-size: 13px;
    color: #2B6DE8;
    cursor: pointer;
    &:hover {
      text-decoration: underline;
    }
  }
}
</style>
