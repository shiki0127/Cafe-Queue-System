<template>
  <div class="student-app">
    <header class="app-header">
      <div class="brand">
        <span class="brand-mark" aria-hidden="true"><Coffee :size="24" /></span>
        <div>
          <strong>校园饮品</strong>
          <span>智能排队取餐</span>
        </div>
      </div>

      <div class="student-login">
        <label>
          <span>当前学号</span>
          <input v-model.trim="studentId" type="text" autocomplete="username" />
        </label>
        <button class="button secondary" type="button" :disabled="loading.auth" @click="issueToken">
          <KeyRound :size="17" />
          <span>{{ token ? '切换学生' : '登录' }}</span>
        </button>
      </div>
    </header>

    <main class="layout">
      <aside class="progress-card" aria-label="当前订单进度">
        <p class="eyebrow">当前取餐</p>
        <h1>{{ orderHeadline }}</h1>
        <p class="muted">{{ orderSubtext }}</p>

        <ol class="steps">
          <li v-for="step in steps" :key="step.key" :class="{ done: step.done, current: step.current }">
            <span>{{ step.index }}</span>
            <div>
              <strong>{{ step.title }}</strong>
              <em>{{ step.text }}</em>
            </div>
          </li>
        </ol>

        <div class="pickup-box">
          <span>订单号</span>
          <strong>{{ currentOrder?.orderId || '下单后生成' }}</strong>
          <span>取餐码</span>
          <strong>{{ currentOrder?.queueTicketId || '支付后生成' }}</strong>
        </div>
      </aside>

      <section class="content">
        <section v-if="message.text" class="toast" :class="message.type" role="status" aria-live="polite">
          <CheckCircle2 v-if="message.type === 'success'" :size="18" />
          <AlertCircle v-else :size="18" />
          <span>{{ message.text }}</span>
        </section>

        <nav class="tabs" aria-label="页面切换">
          <button
            v-for="item in navItems"
            :key="item.id"
            type="button"
            :class="{ active: activeView === item.id }"
            @click="activeView = item.id"
          >
            <component :is="item.icon" :size="18" />
            <span>{{ item.label }}</span>
          </button>
        </nav>

        <section v-if="activeView === 'order'" class="page-grid">
          <article class="panel wide">
            <div class="panel-header">
              <div>
                <p class="eyebrow">选择饮品</p>
                <h2>今天想喝什么？</h2>
              </div>
              <button class="button ghost" type="button" :disabled="loading.refresh" @click="refreshAll">
                <RefreshCw :size="17" />
                <span>刷新</span>
              </button>
            </div>

            <div class="drink-grid">
              <button
                v-for="recipe in normalizedRecipes"
                :key="recipe.recipeCode"
                class="drink-card"
                :class="{ selected: orderForm.recipeCode === recipe.recipeCode }"
                type="button"
                @click="orderForm.recipeCode = recipe.recipeCode"
              >
                <span class="drink-icon"><CupSoda :size="24" /></span>
                <strong>{{ recipe.displayName }}</strong>
                <em>{{ recipe.description }}</em>
                <span class="price">{{ recipe.priceText }}</span>
              </button>
            </div>
          </article>

          <article class="panel">
            <div class="panel-header compact">
              <div>
                <p class="eyebrow">取餐点</p>
                <h2>选择咖啡机</h2>
              </div>
            </div>
            <div class="machine-list">
              <button
                v-for="device in normalizedDevices"
                :key="device.deviceId"
                class="machine-card"
                :class="{ selected: orderForm.machineId === device.deviceId }"
                type="button"
                :disabled="!device.online"
                @click="selectMachine(device.deviceId)"
              >
                <span><MapPin :size="18" />{{ device.displayName }}</span>
                <em :class="device.online ? 'good' : 'bad'">{{ device.online ? '可取餐' : '暂不可用' }}</em>
              </button>
            </div>
          </article>

          <article class="panel">
            <div class="panel-header compact">
              <div>
                <p class="eyebrow">优惠</p>
                <h2>校园券</h2>
              </div>
            </div>
            <div class="coupon-card">
              <TicketPercent :size="22" />
              <div>
                <strong>新人立减券</strong>
                <span>{{ orderForm.couponCode ? '已领取，可用于本单' : '点击领取后自动用于下单' }}</span>
              </div>
            </div>
            <button class="button secondary full" type="button" :disabled="loading.coupon" @click="issueCoupon">
              <span>{{ orderForm.couponCode ? '重新领取' : '领取优惠券' }}</span>
            </button>
          </article>

          <article class="panel wide">
            <div class="checkout">
              <div>
                <p class="eyebrow">确认订单</p>
                <h2>{{ selectedRecipeName }} · {{ selectedMachineName }}</h2>
                <p class="muted">提交后会生成订单号，请确认支付后查看取餐进度。</p>
              </div>
              <div class="checkout-actions">
                <button class="button primary" type="button" :disabled="loading.order" @click="createOrder">
                  <ShoppingBag :size="18" />
                  <span>{{ currentOrder ? '重新下单' : '提交订单' }}</span>
                </button>
                <button class="button pay" type="button" :disabled="!canPayOrder || loading.payment" @click="payOrder">
                  <CreditCard :size="18" />
                  <span>确认支付</span>
                </button>
              </div>
            </div>
          </article>
        </section>

        <section v-else-if="activeView === 'status'" class="page-grid">
          <article class="panel wide">
            <div class="panel-header">
              <div>
                <p class="eyebrow">制作进度</p>
                <h2>{{ orderHeadline }}</h2>
              </div>
              <button class="button ghost" type="button" @click="refreshOrderRelated">
                <RefreshCw :size="17" />
                <span>刷新进度</span>
              </button>
            </div>
            <div class="status-board">
              <div>
                <span>订单状态</span>
                <strong>{{ translateStatus(currentOrder?.status) }}</strong>
              </div>
              <div>
                <span>取餐码</span>
                <strong>{{ currentOrder?.queueTicketId || '支付后生成' }}</strong>
              </div>
              <div>
                <span>取餐设备</span>
                <strong>{{ selectedMachineName }}</strong>
              </div>
            </div>
            <div class="demo-actions">
              <button class="button primary" type="button" :disabled="!canCompleteOrder || loading.complete" @click="completeOrder">
                <CheckCircle2 :size="18" />
                <span>示例完成订单</span>
              </button>
            </div>
          </article>

          <article class="panel">
            <div class="panel-header compact">
              <h2>当前机器队列</h2>
              <button class="icon-button" type="button" aria-label="刷新队列" @click="loadQueue">
                <RefreshCw :size="17" />
              </button>
            </div>
            <div class="queue-list">
              <article v-for="ticket in queue" :key="ticket.ticketId" class="queue-item">
                <strong>{{ translateRecipe(ticket.recipeCode) }}</strong>
                <span>{{ translateStatus(ticket.status) }} · {{ ticket.ticketId }}</span>
              </article>
              <p v-if="!queue.length" class="empty">当前机器暂无排队</p>
            </div>
          </article>

          <article class="panel">
            <div class="panel-header compact">
              <h2>原料余量</h2>
              <button class="icon-button" type="button" aria-label="刷新库存" @click="loadInventory">
                <RefreshCw :size="17" />
              </button>
            </div>
            <div class="stock-list">
              <article v-for="(value, key) in inventory" :key="key">
                <span>{{ ingredientName(key) }}</span>
                <strong>{{ formatMg(value) }}</strong>
                <div class="bar"><i :style="{ width: stockPercent(value) + '%' }"></i></div>
              </article>
            </div>
          </article>
        </section>

        <section v-else class="page-grid">
          <article class="panel wide">
            <div class="panel-header">
              <div>
                <p class="eyebrow">取件提醒</p>
                <h2>我的通知</h2>
              </div>
              <button class="button ghost" type="button" @click="loadNotifications">
                <Bell :size="17" />
                <span>刷新通知</span>
              </button>
            </div>
            <div class="notification-list">
              <article v-for="note in notifications" :key="note.notificationId" class="notification-item">
                <span class="notice-icon"><BellRing :size="18" /></span>
                <div>
                  <strong>{{ note.title }}</strong>
                  <p>{{ note.content }}</p>
                  <em>{{ note.businessId || '系统通知' }}</em>
                </div>
              </article>
              <p v-if="!notifications.length" class="empty">暂无通知，下单后会在这里看到制作提醒。</p>
            </div>
          </article>
        </section>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  AlertCircle,
  Bell,
  BellRing,
  CheckCircle2,
  Coffee,
  CreditCard,
  CupSoda,
  KeyRound,
  ListOrdered,
  MapPin,
  RefreshCw,
  ShoppingBag,
  TicketPercent
} from 'lucide-vue-next'

const API_BASE = import.meta.env.VITE_API_BASE || ''

const recipeText = {
  LATTE: { name: '拿铁', description: '奶香更足，适合上午课程前', priceText: '18 元' },
  AMERICANO: { name: '美式咖啡', description: '清爽提神，适合自习和赶作业', priceText: '15 元' }
}

const machineText = {
  MACHINE_A01: '图书馆一楼咖啡机',
  MACHINE_B02: '教学楼东侧咖啡机',
  MACHINE_C03: '宿舍区服务点咖啡机'
}

const navItems = [
  { id: 'order', label: '点单', icon: Coffee },
  { id: 'status', label: '进度', icon: ListOrdered },
  { id: 'notice', label: '通知', icon: Bell }
]

const activeView = ref('order')
const studentId = ref(localStorage.getItem('cq_student_id') || 'stu_1001')
const token = ref(localStorage.getItem('cq_token') || '')
const devices = ref([])
const recipes = ref([])
const inventory = ref({})
const queue = ref([])
const notifications = ref([])
const currentOrder = ref(null)
const message = reactive({ text: '', type: 'success' })

const loading = reactive({
  auth: false,
  refresh: false,
  coupon: false,
  order: false,
  payment: false,
  complete: false
})

const orderForm = reactive({
  machineId: 'MACHINE_A01',
  recipeCode: 'LATTE',
  couponCode: ''
})

const normalizedRecipes = computed(() => {
  const source = recipes.value.length ? recipes.value : [{ recipeCode: 'LATTE' }, { recipeCode: 'AMERICANO' }]
  return source.map((recipe) => {
    const text = recipeText[recipe.recipeCode] || {}
    return {
      ...recipe,
      displayName: text.name || recipe.name || recipe.recipeCode,
      description: text.description || '校园热饮',
      priceText: text.priceText || '校园价'
    }
  })
})

const normalizedDevices = computed(() => devices.value.map((device) => ({
  ...device,
  displayName: machineText[device.deviceId] || device.location || device.deviceId
})))

const selectedRecipeName = computed(() => translateRecipe(orderForm.recipeCode))
const selectedMachineName = computed(() => {
  const device = normalizedDevices.value.find((item) => item.deviceId === orderForm.machineId)
  return device?.displayName || machineText[orderForm.machineId] || orderForm.machineId
})

const orderHeadline = computed(() => {
  if (!currentOrder.value) return '还没有正在制作的饮品'
  if (currentOrder.value.status === 'COMPLETED') return `${translateRecipe(currentOrder.value.recipeCode)}已完成`
  return `${translateRecipe(currentOrder.value.recipeCode)}正在${translateStatus(currentOrder.value.status)}`
})

const orderSubtext = computed(() => {
  if (!currentOrder.value) return '选择饮品和取餐点后提交订单，支付完成就会进入排队制作。'
  if (currentOrder.value.status === 'COMPLETED') return '订单流程已走完，可以查看完成通知或重新下单。'
  if (currentOrder.value.status === 'BREWING') return '咖啡机已经收到制作指令，请留意取件通知。'
  if (currentOrder.value.status === 'QUEUED') return '订单已进入队列，支付后会开始制作。'
  return '订单已创建，请完成支付。'
})

const canPayOrder = computed(() => currentOrder.value && ['CREATED', 'QUEUED'].includes(currentOrder.value.status))
const canCompleteOrder = computed(() => currentOrder.value?.status === 'BREWING')
const paymentDone = computed(() => ['BREWING', 'COMPLETED'].includes(currentOrder.value?.status))

const steps = computed(() => [
  { index: '1', key: 'coupon', title: '领取优惠', text: orderForm.couponCode ? '已使用新人立减券' : '可领取后再下单', done: Boolean(orderForm.couponCode), current: !orderForm.couponCode },
  { index: '2', key: 'order', title: '提交订单', text: currentOrder.value ? '订单已提交' : '选择饮品和取餐点', done: Boolean(currentOrder.value), current: Boolean(orderForm.couponCode) && !currentOrder.value },
  { index: '3', key: 'pay', title: '确认支付', text: paymentDone.value ? '已支付，正在制作' : '等待确认支付', done: paymentDone.value, current: Boolean(currentOrder.value) && !paymentDone.value },
  { index: '4', key: 'pickup', title: '等待取餐', text: currentOrder.value?.status === 'COMPLETED' ? '订单已完成' : '制作完成后取餐', done: currentOrder.value?.status === 'COMPLETED', current: currentOrder.value?.status === 'BREWING' }
])

async function api(path, options = {}) {
  const headers = { ...(options.headers || {}) }
  if (options.body && !(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json'
  }
  if (token.value) {
    headers.Authorization = `Bearer ${token.value}`
  }
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
    body: options.body && headers['Content-Type'] === 'application/json' ? JSON.stringify(options.body) : options.body
  })
  const contentType = response.headers.get('content-type') || ''
  const payload = contentType.includes('application/json') ? await response.json() : await response.text()
  if (!response.ok) {
    throw new Error(typeof payload === 'string' ? payload : payload.message || `请求失败：${response.status}`)
  }
  if (payload && payload.success === false) {
    throw new Error(payload.message || payload.code || '请求失败')
  }
  return payload?.data ?? payload
}

function notify(text, type = 'success') {
  message.text = text
  message.type = type
  window.clearTimeout(notify.timer)
  notify.timer = window.setTimeout(() => {
    message.text = ''
  }, 3600)
}

async function issueToken() {
  loading.auth = true
  try {
    const data = await api('/api/auth/token', {
      method: 'POST',
      body: { studentId: studentId.value, role: 'STUDENT' }
    })
    token.value = data.accessToken
    localStorage.setItem('cq_token', token.value)
    localStorage.setItem('cq_student_id', studentId.value)
    currentOrder.value = null
    notifications.value = []
    notify('学生身份已切换')
  } catch (error) {
    notify(error.message, 'error')
  } finally {
    loading.auth = false
  }
}

async function ensureToken() {
  if (!token.value) {
    await issueToken()
  }
}

async function loadDevices() {
  devices.value = await api('/api/devices')
  const onlineDevice = devices.value.find((item) => item.online)
  if (!devices.value.some((item) => item.deviceId === orderForm.machineId) && onlineDevice) {
    orderForm.machineId = onlineDevice.deviceId
  }
}

async function loadRecipes() {
  recipes.value = await api('/api/recipes')
  if (!recipes.value.some((item) => item.recipeCode === orderForm.recipeCode) && recipes.value[0]) {
    orderForm.recipeCode = recipes.value[0].recipeCode
  }
}

async function loadInventory() {
  inventory.value = await api(`/api/inventory/machines/${orderForm.machineId}`)
}

async function loadQueue() {
  queue.value = await api(`/api/queues/machines/${orderForm.machineId}`)
}

async function loadNotifications() {
  notifications.value = await api(`/api/notifications/students/${studentId.value}?limit=20`)
}

async function loadCurrentOrder() {
  if (!currentOrder.value?.orderId) return
  currentOrder.value = await api(`/api/orders/${currentOrder.value.orderId}`)
}

async function refreshAll() {
  loading.refresh = true
  try {
    await ensureToken()
    await Promise.all([loadDevices(), loadRecipes(), loadInventory(), loadQueue(), loadNotifications()])
  } catch (error) {
    notify(error.message, 'error')
  } finally {
    loading.refresh = false
  }
}

async function refreshOrderRelated() {
  try {
    await ensureToken()
    await Promise.all([loadCurrentOrder(), loadQueue(), loadInventory(), loadNotifications()])
    notify('进度已刷新')
  } catch (error) {
    notify(error.message, 'error')
  }
}

async function issueCoupon() {
  loading.coupon = true
  try {
    await ensureToken()
    const data = await api('/api/coupons/issue', {
      method: 'POST',
      body: { studentId: studentId.value, templateCode: 'WELCOME_3' }
    })
    orderForm.couponCode = data.couponCode
    notify('优惠券已领取')
  } catch (error) {
    notify(error.message, 'error')
  } finally {
    loading.coupon = false
  }
}

async function createOrder() {
  loading.order = true
  try {
    await ensureToken()
    const data = await api('/api/orders', {
      method: 'POST',
      body: {
        studentId: studentId.value,
        machineId: orderForm.machineId,
        recipeCode: orderForm.recipeCode,
        couponCode: orderForm.couponCode || null
      }
    })
    currentOrder.value = data
    activeView.value = 'order'
    notify(`订单已提交，订单号：${data.orderId}，请确认支付`)
  } catch (error) {
    notify(error.message, 'error')
  } finally {
    loading.order = false
  }
}

async function payOrder() {
  if (!currentOrder.value) return
  loading.payment = true
  try {
    const data = await api(`/api/orders/${currentOrder.value.orderId}/payment-callback`, {
      method: 'POST',
      body: {
        callbackId: `pay_${currentOrder.value.orderId}_${Date.now()}`,
        payload: JSON.stringify({ channel: '学生端模拟支付', amount: 18 })
      }
    })
    currentOrder.value = data
    activeView.value = 'status'
    notify('支付成功，正在制作')
    await Promise.all([loadInventory(), loadQueue(), loadNotifications()])
  } catch (error) {
    notify(error.message, 'error')
  } finally {
    loading.payment = false
  }
}

async function completeOrder() {
  if (!currentOrder.value) return
  loading.complete = true
  try {
    const data = await api(`/api/orders/${currentOrder.value.orderId}/complete`, {
      method: 'POST'
    })
    currentOrder.value = data
    activeView.value = 'status'
    notify('订单已完成')
    await Promise.all([loadInventory(), loadQueue(), loadNotifications()])
  } catch (error) {
    notify(error.message, 'error')
  } finally {
    loading.complete = false
  }
}

function selectMachine(machineId) {
  orderForm.machineId = machineId
  loadInventory().catch((error) => notify(error.message, 'error'))
  loadQueue().catch((error) => notify(error.message, 'error'))
}

function translateRecipe(code) {
  return recipeText[code]?.name || code || '饮品'
}

function translateStatus(status) {
  return {
    CREATED: '待支付',
    PAID: '已支付',
    QUEUED: '排队中',
    BREWING: '制作中',
    READY: '待取餐',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
    WAITING: '排队中'
  }[status] || '待开始'
}

function ingredientName(key) {
  return {
    coffee_mg: '咖啡豆',
    milk_mg: '牛奶',
    water_mg: '水'
  }[key] || key
}

function formatMg(value) {
  if (value >= 1000000) return `${(value / 1000000).toFixed(1)} 千克`
  if (value >= 1000) return `${(value / 1000).toFixed(0)} 克`
  return `${value} 毫克`
}

function stockPercent(value) {
  return Math.max(8, Math.min(100, Math.round((value / 8000000) * 100)))
}

onMounted(refreshAll)
</script>
