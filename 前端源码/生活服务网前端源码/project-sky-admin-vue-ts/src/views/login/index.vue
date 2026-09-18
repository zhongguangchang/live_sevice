<template>
  <div class="login">
    <div class="login-box">
      <!-- 左侧品牌区：用渐变色块替代原来的外卖插画 -->
      <div class="brand-panel">
        <div class="brand-logo">
          <i class="el-icon-service" />
          <span>生活服务网</span>
        </div>
        <p class="brand-slogan">家政保洁 · 家电维修 · 搬家运输</p>
        <p class="brand-slogan">开锁换锁 · 管道疏通 · 上门服务</p>
        <div class="brand-divider" />
        <p class="brand-desc">
          在线预约上门服务，师傅接单、服务进度实时可查、完成后可评价
        </p>
      </div>

      <!-- 右侧登录表单 -->
      <div class="login-form">
        <div class="login-form-title">账号登录</div>
        <el-form ref="loginForm" :model="loginForm" :rules="loginRules">
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              type="text"
              auto-complete="off"
              placeholder="账号"
              prefix-icon="iconfont icon-user"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="密码"
              prefix-icon="iconfont icon-lock"
              @keyup.enter.native="handleLogin"
            />
          </el-form-item>
          <el-form-item style="width: 100%">
            <el-button
              :loading="loading"
              class="login-btn"
              size="medium"
              type="primary"
              style="width: 100%"
              @click.native.prevent="handleLogin"
            >
              <span v-if="!loading">登 录</span>
              <span v-else>登录中...</span>
            </el-button>
          </el-form-item>
        </el-form>
        <p class="login-tip">默认账号 admin / 123456</p>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Watch } from 'vue-property-decorator'
import { Route } from 'vue-router'
import { Form as ElForm, Input } from 'element-ui'
import { UserModule } from '@/store/modules/user'
import { isValidUsername } from '@/utils/validate'

@Component({
  name: 'Login',
})
export default class extends Vue {
  private validateUsername = (rule: any, value: string, callback: Function) => {
    if (!value) {
      callback(new Error('请输入用户名'))
    } else {
      callback()
    }
  }
  private validatePassword = (rule: any, value: string, callback: Function) => {
    if (value.length < 6) {
      callback(new Error('密码必须在6位以上'))
    } else {
      callback()
    }
  }
  private loginForm = {
    username: 'admin',
    password: '123456',
  } as {
    username: String
    password: String
  }

  loginRules = {
    username: [{ validator: this.validateUsername, trigger: 'blur' }],
    password: [{ validator: this.validatePassword, trigger: 'blur' }],
  }
  private loading = false
  private redirect?: string

  @Watch('$route', { immediate: true })
  private onRouteChange(route: Route) {}

  // 登录
  private handleLogin() {
    ;(this.$refs.loginForm as ElForm).validate(async (valid: boolean) => {
      if (valid) {
        this.loading = true
        await UserModule.Login(this.loginForm as any)
          .then((res: any) => {
            if (String(res.code) === '1') {
              this.$router.push('/')
            } else {
              // this.$message.error(res.msg)
              this.loading = false
            }
          })
          .catch(() => {
            // this.$message.error('用户名或密码错误！')
            this.loading = false
          })
      } else {
        return false
      }
    })
  }
}
</script>

<style lang="scss">
.login {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  // 深海军蓝到品牌蓝的渐变，和侧边栏同一套色系
  background: linear-gradient(135deg, #16203A 0%, #2B6DE8 100%);
}

.login-box {
  width: 860px;
  height: 460px;
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  box-shadow: 0 18px 48px rgba(10, 22, 48, 0.35);
}

/* 左侧品牌区 */
.brand-panel {
  width: 58%;
  padding: 46px 40px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  justify-content: center;
  color: #fff;
  background: linear-gradient(160deg, #1E2C4C 0%, #2B6DE8 100%);
  position: relative;

  .brand-logo {
    display: flex;
    align-items: center;
    margin-bottom: 26px;
    i {
      font-size: 30px;
    }
    span {
      margin-left: 10px;
      font-size: 24px;
      font-weight: 600;
      letter-spacing: 2px;
    }
  }
  .brand-slogan {
    margin: 0 0 6px;
    font-size: 14px;
    letter-spacing: 1px;
    color: rgba(255, 255, 255, 0.88);
  }
  .brand-divider {
    width: 44px;
    height: 3px;
    margin: 22px 0;
    border-radius: 2px;
    background: rgba(255, 255, 255, 0.6);
  }
  .brand-desc {
    margin: 0;
    font-size: 13px;
    line-height: 1.9;
    color: rgba(255, 255, 255, 0.72);
  }
}

/* 右侧表单 */
.login-form {
  width: 42%;
  background: #ffffff;
  padding: 0 40px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  justify-content: center;

  .el-form {
    width: 100%;
  }
  .el-form-item {
    margin-bottom: 26px;
  }
  .el-form-item.is-error .el-input__inner {
    border: 0 !important;
    border-bottom: 1px solid #fd7065 !important;
    background: #fff !important;
  }
  .el-input__inner {
    border: 0;
    border-bottom: 1px solid #e9e9e8;
    border-radius: 0;
    font-size: 13px;
    font-weight: 400;
    color: #333333;
    height: 34px;
    line-height: 34px;
  }
  .el-input__prefix {
    left: 0;
  }
  .el-input--prefix .el-input__inner {
    padding-left: 26px;
  }
  .el-input__inner::placeholder {
    color: #aeb5c4;
  }
  .el-form-item--medium .el-form-item__content {
    line-height: 34px;
  }
  .el-input--medium .el-input__icon {
    line-height: 34px;
  }
}

.login-form-title {
  margin-bottom: 34px;
  font-size: 18px;
  font-weight: 600;
  color: #1B2437;
}

.login-btn {
  border-radius: 20px;
  padding: 12px 20px !important;
  margin-top: 6px;
  font-size: 13px;
  letter-spacing: 2px;
  border: 0;
  background: linear-gradient(90deg, #2B6DE8 0%, #4C8DFF 100%);
  &:hover,
  &:focus {
    background: linear-gradient(90deg, #4C8DFF 0%, #2B6DE8 100%);
    color: #ffffff;
  }
}

.login-tip {
  margin: 18px 0 0;
  text-align: center;
  font-size: 12px;
  color: #aeb5c4;
}
</style>
