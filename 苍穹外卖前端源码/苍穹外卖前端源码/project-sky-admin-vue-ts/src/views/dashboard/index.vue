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
            <div class="stat-num">{{ stats[s.key] || 0 }}</div>
            <div class="stat-label">{{ s.label }}</div>
          </div>
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
import { getServiceOrderStatistics } from '@/api/serviceOrder'
import { getServiceItemPage } from '@/api/serviceItem'
import { getServicePackagePage } from '@/api/servicePackage'
import { getProviderPage } from '@/api/provider'

export default Vue.extend({
  name: 'Dashboard',

  data() {
    return {
      stats: {} as any,
      orderStats: [
        { key: 'toBeAccepted', label: '待接单', icon: 'el-icon-bell', color: '#E6A23C', bg: '#FDF6EC' },
        { key: 'accepted', label: '已接单', icon: 'el-icon-user', color: '#2B6DE8', bg: '#ECF2FE' },
        { key: 'inService', label: '服务中', icon: 'el-icon-time', color: '#67C23A', bg: '#F0F9EB' },
        { key: 'toBeReviewed', label: '待评价', icon: 'el-icon-chat-dot-round', color: '#909399', bg: '#F4F4F5' }
      ],
      resources: [
        { key: 'item', label: '在售服务项目', value: '-', action: '管理服务项目', path: '/serviceItem' },
        { key: 'pkg', label: '已启用套餐', value: '-', action: '管理服务套餐', path: '/servicePackage' },
        { key: 'provider', label: '可接单服务人员', value: '-', action: '管理服务人员', path: '/provider' }
      ] as any[]
    }
  },

  created() {
    this.loadStats()
    this.loadResources()
  },

  methods: {
    loadStats() {
      getServiceOrderStatistics().then((res: any) => {
        if (res.data.code === 1) this.stats = res.data.data || {}
      })
    },

    /**
     * 资源数量直接用分页接口返回的 total，pageSize 传 1 只为了拿总数
     */
    loadResources() {
      getServiceItemPage({ page: 1, pageSize: 1, status: 1 }).then((res: any) => {
        if (res.data.code === 1) this.resources[0].value = res.data.data.total
      })
      getServicePackagePage({ page: 1, pageSize: 1, status: 1 }).then((res: any) => {
        if (res.data.code === 1) this.resources[1].value = res.data.data.total
      })
      getProviderPage({ page: 1, pageSize: 1, status: 1 }).then((res: any) => {
        if (res.data.code === 1) this.resources[2].value = res.data.data.total
      })
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
