import Vue from "vue";
import Router from "vue-router";
import Layout from "@/layout/index.vue";
import {
  getToken,
  setToken,
  removeToken,
  getStoreId,
  setStoreId,
  removeStoreId,
  setUserInfo,
  getUserInfo,
  removeUserInfo
} from "@/utils/cookies";
import store from "@/store";

Vue.use(Router);

/**
 * 生活服务网 · 管理端路由
 *
 * 相对原项目的调整：
 * - 去掉「菜品管理」「套餐管理」「订单管理」，换成服务项目 / 服务套餐 / 派单中心
 * - 新增「服务人员管理」「排期管理」「评价管理」「服务区域」
 * - 图标沿用原有的 iconfont 类名，只是视觉标识，不影响功能
 */
const router = new Router({
  scrollBehavior: (to, from, savedPosition) => {
    if (savedPosition) {
      return savedPosition;
    }
    return { x: 0, y: 0 };
  },
  base: process.env.BASE_URL,
  routes: [
    {
      path: "/login",
      component: () =>
        import(/* webpackChunkName: "login" */ "@/views/login/index.vue"),
      meta: { title: "生活服务网", hidden: true, notNeedAuth: true }
    },
    {
      path: "/404",
      component: () => import(/* webpackChunkName: "404" */ "@/views/404.vue"),
      meta: { title: "生活服务网", hidden: true, notNeedAuth: true }
    },
    {
      path: "/",
      component: Layout,
      redirect: "/dashboard",
      children: [
        {
          path: "dashboard",
          component: () =>
            import(/* webpackChunkName: "dashboard" */ "@/views/dashboard/index.vue"),
          name: "Dashboard",
          meta: {
            title: "工作台",
            icon: "dashboard",
            affix: true
          }
        },
        {
          path: "/serviceOrder",
          component: () =>
            import(/* webpackChunkName: "serviceOrder" */ "@/views/serviceOrder/index.vue"),
          meta: {
            title: "派单中心",
            icon: "icon-order"
          }
        },
        {
          path: "/serviceItem",
          component: () =>
            import(/* webpackChunkName: "serviceItem" */ "@/views/serviceItem/index.vue"),
          meta: {
            title: "服务项目管理",
            icon: "icon-dish"
          }
        },
        {
          path: "/servicePackage",
          component: () =>
            import(/* webpackChunkName: "servicePackage" */ "@/views/servicePackage/index.vue"),
          meta: {
            title: "服务套餐管理",
            icon: "icon-combo"
          }
        },
        {
          path: "/provider",
          component: () =>
            import(/* webpackChunkName: "provider" */ "@/views/provider/index.vue"),
          meta: {
            title: "服务人员管理",
            icon: "icon-employee"
          }
        },
        {
          path: "/slot",
          component: () =>
            import(/* webpackChunkName: "slot" */ "@/views/slot/index.vue"),
          meta: {
            title: "排期管理",
            icon: "icon-category"
          }
        },
        {
          path: "/review",
          component: () =>
            import(/* webpackChunkName: "review" */ "@/views/review/index.vue"),
          meta: {
            title: "评价管理",
            icon: "icon-statistics"
          }
        },
        {
          path: "/serviceArea",
          component: () =>
            import(/* webpackChunkName: "serviceArea" */ "@/views/serviceArea/index.vue"),
          meta: {
            title: "服务区域",
            icon: "icon-category"
          }
        },
        {
          path: "/statistics",
          component: () =>
            import(/* webpackChunkName: "statistics" */ "@/views/statistics/index.vue"),
          meta: {
            title: "数据统计",
            icon: "icon-statistics"
          }
        },
        {
          path: "category",
          component: () =>
            import(/* webpackChunkName: "category" */ "@/views/category/index.vue"),
          meta: {
            title: "分类管理",
            icon: "icon-category"
          }
        },
        {
          path: "employee",
          component: () =>
            import(/* webpackChunkName: "employee" */ "@/views/employee/index.vue"),
          meta: {
            title: "员工管理",
            icon: "icon-employee"
          }
        },
        {
          path: "/employee/add",
          component: () =>
            import(/* webpackChunkName: "employeeAdd" */ "@/views/employee/addEmployee.vue"),
          meta: {
            title: "添加员工",
            hidden: true
          }
        }
      ]
    },
    {
      path: "*",
      redirect: "/404",
      meta: { hidden: true }
    }
  ]
});

export default router;
