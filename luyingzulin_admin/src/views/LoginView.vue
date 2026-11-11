<script setup>
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import {
  BRAND_ADMIN_TITLE,
  BRAND_COMPANY,
  BRAND_LOGO_TEXT,
  BRAND_TAGLINE,
} from '@/constants/brand'
import { validateForm } from '@/utils/form'
import { getCaptchaUrl, getUUID } from '@/utils/tool'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const formRef = ref()
const loading = ref(false)
const captchaUrl = ref('')
const DEFAULT_VIEWPORT = 'width=1600, minimum-scale=0.1, maximum-scale=10, user-scalable=yes, viewport-fit=cover'
const LOGIN_VIEWPORT = 'width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no, viewport-fit=cover'

const highlights = [
  '订单 · 库存 · 商户一体化运营',
  '与用户端统一的露营绿视觉',
  '实时掌握租赁与分销数据',
]

const form = reactive({
  username: '',
  password: '',
  captcha: '',
  uuid: '',
})

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

function getRedirectTarget() {
  const redirect = Array.isArray(route.query.redirect) ? route.query.redirect[0] : route.query.redirect
  if (!redirect || typeof redirect !== 'string' || redirect.startsWith('/login')) {
    return '/dashboard'
  }
  return redirect
}

function refreshCaptcha() {
  form.uuid = getUUID()
  captchaUrl.value = getCaptchaUrl(form.uuid)
}

function setViewport(content) {
  if (typeof document === 'undefined') return
  document.querySelector('meta[name="viewport"]')?.setAttribute('content', content)
}

async function blurActiveField() {
  if (typeof document === 'undefined') return
  const activeElement = document.activeElement
  if (activeElement instanceof HTMLElement) {
    activeElement.blur()
  }
  await nextTick()
}

async function handleLogin() {
  if (loading.value) return
  const valid = await validateForm(formRef)
  if (!valid) return
  loading.value = true
  try {
    await authStore.login(form)
    await blurActiveField()
    ElMessage.success('登录成功')
    window.location.replace(router.resolve(getRedirectTarget()).href)
  } catch (error) {
    ElMessage.error(error.msg || error.message || '登录失败')
    refreshCaptcha()
    form.captcha = ''
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  setViewport(LOGIN_VIEWPORT)
  refreshCaptcha()
})

onBeforeUnmount(() => {
  setViewport(DEFAULT_VIEWPORT)
})
</script>

<template>
  <div class="login-page">
    <div class="login-page__bg">
      <span class="login-page__orb login-page__orb--a" />
      <span class="login-page__orb login-page__orb--b" />
      <span class="login-page__orb login-page__orb--c" />
    </div>

    <div class="login-page__grid">
      <section class="login-hero">
        <div class="login-hero__badge">Camp Rental OS</div>
        <div class="login-hero__brand">
          <div class="login-hero__logo">{{ BRAND_LOGO_TEXT }}</div>
          <div>
            <h1>{{ BRAND_ADMIN_TITLE }}</h1>
            <p>{{ BRAND_TAGLINE }}</p>
          </div>
        </div>

        <ul class="login-hero__list">
          <li v-for="item in highlights" :key="item">
            <span class="login-hero__dot" />
            {{ item }}
          </li>
        </ul>

        <div class="login-hero__visual" aria-hidden="true">
          <div class="login-hero__mountain login-hero__mountain--back" />
          <div class="login-hero__mountain login-hero__mountain--front" />
          <div class="login-hero__tent">
            <div class="login-hero__tent-body" />
            <div class="login-hero__tent-door" />
          </div>
          <div class="login-hero__tree login-hero__tree--left" />
          <div class="login-hero__tree login-hero__tree--right" />
        </div>

        <footer class="login-hero__footer">{{ BRAND_COMPANY }}</footer>
      </section>

      <section class="login-panel">
        <div class="login-panel__card">
          <div class="login-panel__head">
            <strong>欢迎登录</strong>
            <span>使用管理员账号进入运营中心</span>
          </div>

          <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
            <el-form-item label="账号" prop="username">
              <el-input v-model="form.username" placeholder="请输入管理员账号" clearable size="large" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="form.password"
                type="password"
                show-password
                placeholder="请输入登录密码"
                size="large"
              />
            </el-form-item>
            <el-form-item label="验证码" prop="captcha">
              <div class="login-captcha">
                <el-input
                  v-model="form.captcha"
                  placeholder="输入右侧验证码"
                  clearable
                  size="large"
                  @keyup.enter="handleLogin"
                />
                <img
                  :src="captchaUrl"
                  alt="验证码"
                  class="login-captcha__image"
                  title="点击刷新验证码"
                  @click="refreshCaptcha"
                />
              </div>
            </el-form-item>
            <el-button
              native-type="button"
              type="primary"
              size="large"
              :loading="loading"
              class="login-panel__submit"
              @click="handleLogin"
            >
              进入运营中心
            </el-button>
          </el-form>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 24px;
  overflow: hidden;
  background: var(--login-gradient);
}

.login-page__bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.login-page__orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(0);
}

.login-page__orb--a {
  top: -120px;
  left: -80px;
  width: 320px;
  height: 320px;
  background: radial-gradient(circle, var(--login-glow-a), transparent 70%);
}

.login-page__orb--b {
  right: -60px;
  bottom: 10%;
  width: 280px;
  height: 280px;
  background: radial-gradient(circle, var(--login-glow-b), transparent 72%);
}

.login-page__orb--c {
  top: 18%;
  right: 28%;
  width: 160px;
  height: 160px;
  background: radial-gradient(circle, rgba(126, 211, 33, 0.12), transparent 70%);
}

.login-page__grid {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(360px, 420px);
  gap: 40px;
  width: min(1120px, 100%);
  align-items: center;
}

.login-hero {
  padding: 12px 8px;
}

.login-hero__badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 14px;
  border-radius: 999px;
  background: rgba(126, 211, 33, 0.12);
  color: #4a9e12;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.login-hero__brand {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-top: 28px;
}

.login-hero__logo {
  display: grid;
  place-items: center;
  width: 72px;
  height: 72px;
  border-radius: 22px;
  background: var(--logo-gradient);
  color: #fff;
  font-size: 34px;
  font-weight: 800;
  box-shadow: 0 16px 40px rgba(92, 184, 22, 0.28);
}

.login-hero__brand h1 {
  margin: 0 0 8px;
  font-size: clamp(1.8rem, 3vw, 2.6rem);
  line-height: 1.15;
  color: var(--ink);
}

.login-hero__brand p {
  margin: 0;
  color: var(--muted);
  font-size: 15px;
}

.login-hero__list {
  margin: 28px 0 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 12px;
}

.login-hero__list li {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #3d4a32;
  font-size: 14px;
}

.login-hero__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: linear-gradient(135deg, #7ed321, #5cb816);
  flex-shrink: 0;
}

.login-hero__visual {
  position: relative;
  height: 180px;
  margin-top: 36px;
}

.login-hero__mountain {
  position: absolute;
  bottom: 0;
  border-radius: 40% 40% 0 0;
}

.login-hero__mountain--back {
  left: 8%;
  width: 72%;
  height: 88px;
  background: linear-gradient(180deg, #d7e8c8 0%, #c5dbb0 100%);
}

.login-hero__mountain--front {
  left: 22%;
  width: 56%;
  height: 110px;
  background: linear-gradient(180deg, #b8d99a 0%, #9fc87d 100%);
}

.login-hero__tent {
  position: absolute;
  left: 50%;
  bottom: 18px;
  transform: translateX(-50%);
  width: 120px;
  height: 86px;
}

.login-hero__tent-body {
  position: absolute;
  inset: 0;
  background: linear-gradient(160deg, #7ed321 0%, #6bc01a 55%, #5cb816 100%);
  clip-path: polygon(50% 0%, 100% 100%, 0% 100%);
  box-shadow: 0 12px 24px rgba(92, 184, 22, 0.25);
}

.login-hero__tent-door {
  position: absolute;
  left: 50%;
  bottom: 0;
  transform: translateX(-50%);
  width: 28px;
  height: 34px;
  background: rgba(26, 33, 20, 0.18);
  border-radius: 14px 14px 0 0;
}

.login-hero__tree {
  position: absolute;
  bottom: 12px;
  width: 18px;
  height: 54px;
  border-radius: 999px;
  background: linear-gradient(180deg, #6bc01a 0%, #4a9e12 100%);
}

.login-hero__tree::before {
  content: '';
  position: absolute;
  top: -18px;
  left: 50%;
  transform: translateX(-50%);
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: radial-gradient(circle at 30% 30%, #8fd94a, #5cb816);
}

.login-hero__tree--left {
  left: 18%;
}

.login-hero__tree--right {
  right: 16%;
}

.login-hero__footer {
  margin-top: 24px;
  color: var(--muted);
  font-size: 13px;
}

.login-panel {
  display: flex;
  justify-content: center;
}

.login-panel__card {
  width: 100%;
  padding: 32px 28px 28px;
  border-radius: 28px;
  border: 1px solid rgba(255, 255, 255, 0.75);
  background: var(--card-bg);
  box-shadow:
    0 24px 60px rgba(26, 33, 20, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(18px);
}

.login-panel__head {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 24px;
}

.login-panel__head strong {
  font-size: 24px;
  color: var(--ink);
}

.login-panel__head span {
  color: var(--muted);
  font-size: 13px;
}

.login-panel__submit {
  width: 100%;
  margin-top: 4px;
  height: 46px;
  font-weight: 700;
  border: none;
  background: linear-gradient(135deg, #7ed321 0%, #6bc01a 50%, #5cb816 100%);
}

.login-panel__submit:hover,
.login-panel__submit:focus {
  background: linear-gradient(135deg, #8be032 0%, #74cb24 50%, #66c01f 100%);
}

@media (max-width: 992px) {
  .login-page__grid {
    grid-template-columns: 1fr;
    gap: 24px;
  }

  .login-hero__visual {
    display: none;
  }
}
</style>
