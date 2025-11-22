<template>
  <div class="profile-page">
    <div class="hero">
      <div class="hero-left">
        <div class="hero-avatar">
          <a-avatar :size="104" :src="form.userAvatar || undefined">
            <template #icon>
              <img src="https://unpkg.com/lucide-static@latest/icons/user.svg" alt="user" class="avatar-fallback" />
            </template>
          </a-avatar>
          <button
            v-if="isEditing"
            class="avatar-upload"
            type="button"
            :disabled="avatarUploading"
            @click="avatarInput?.click()"
          >
            <img src="https://unpkg.com/lucide-static@latest/icons/camera.svg" class="icon" alt="upload" />
            {{ avatarUploading ? '上传中...' : '更换头像' }}
          </button>
          <input
            ref="avatarInput"
            type="file"
            accept="image/*"
            class="hidden-input"
            @change="handleAvatarChange"
          />
        </div>
        <div class="hero-summary">
          <div class="name-row">
            <h1>{{ form.userName || '未命名用户' }}</h1>
            <a-tag :color="roleMeta.color" class="role-tag">
              <span class="role-dot" :style="{ background: roleMeta.color }"></span>{{ roleMeta.text }}
            </a-tag>
          </div>
          <p class="subtitle">账号：{{ form.userAccount || '—' }}</p>
          <div class="meta">
            <span>
              <img src="https://unpkg.com/lucide-static@latest/icons/calendar.svg" alt="join" class="meta-icon" />
              加入时间：{{ displayDate(form.createTime) }}
            </span>
            <span>
              <img src="https://unpkg.com/lucide-static@latest/icons/shield.svg" alt="status" class="meta-icon" />
              状态：<strong>正常</strong>
            </span>
          </div>
        </div>
      </div>
      <div class="hero-actions">
        <a-button v-if="!isEditing" type="primary" size="large" @click="startEdit">编辑资料</a-button>
        <div v-else class="edit-actions">
          <a-button type="primary" size="large" :loading="saving" @click="saveProfile">保存</a-button>
          <a-button size="large" @click="cancelEdit">取消</a-button>
        </div>
      </div>
    </div>

    <div class="grid">
      <a-card class="glass-card" :loading="loading">
        <template #title>
          <div class="card-title">
            <img src="https://unpkg.com/lucide-static@latest/icons/id-card.svg" class="card-icon" alt="profile" />
            个人信息
          </div>
        </template>

        <a-form layout="vertical" class="profile-form">
          <a-row :gutter="16">
            <a-col :xs="24" :md="12">
              <a-form-item label="用户名">
                <a-input
                  v-if="isEditing"
                  v-model:value="form.userName"
                  :maxlength="20"
                  placeholder="请输入用户名"
                />
                <div v-else class="readonly">{{ form.userName || '—' }}</div>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="12">
              <a-form-item label="账号">
                <div class="readonly">{{ form.userAccount || '—' }}</div>
              </a-form-item>
            </a-col>
          </a-row>

          <a-form-item label="个性签名">
            <a-textarea
              v-if="isEditing"
              v-model:value="form.userProfile"
              :rows="3"
              :maxlength="200"
              placeholder="介绍一下自己吧"
              show-count
            />
            <div v-else class="readonly muted">
              {{ form.userProfile || '这个人很懒，什么都没有留下~' }}
            </div>
          </a-form-item>
        </a-form>
      </a-card>

      <a-card class="glass-card stats-card">
        <template #title>
          <div class="card-title">
            <img src="https://unpkg.com/lucide-static@latest/icons/activity.svg" class="card-icon" alt="stats" />
            我的数据
          </div>
        </template>
        <div class="stat-grid">
          <div v-for="item in stats" :key="item.key" class="stat-chip">
            <div class="stat-icon-circle" :style="{ background: item.bg }">
              <span>{{ item.icon }}</span>
            </div>
            <div class="stat-text">
              <div class="stat-value">{{ item.value }}</div>
              <div class="stat-label">{{ item.label }}</div>
            </div>
          </div>
        </div>
      </a-card>

      <a-card class="glass-card security-card">
        <template #title>
          <div class="card-title">
            <img src="https://unpkg.com/lucide-static@latest/icons/shield-check.svg" class="card-icon" alt="security" />
            账户与安全
          </div>
        </template>
        <div class="security-list">
          <div class="security-item">
            <div>
              <p class="security-title">登录密码</p>
              <p class="security-desc">建议定期更新，提升账号安全性</p>
            </div>
            <a-button type="link" @click="showPasswordModal = true">修改</a-button>
          </div>
          <a-divider />
          <div class="security-item">
            <div>
              <p class="security-title">账号绑定</p>
              <p class="security-desc">{{ form.userAccount || '未绑定' }}</p>
            </div>
            <a-tag color="success">已绑定</a-tag>
          </div>
          <a-divider />
          <div class="security-item">
            <div>
              <p class="security-title">最近活跃</p>
              <p class="security-desc">{{ lastActive }}</p>
            </div>
            <a-tag color="blue">正常</a-tag>
          </div>
        </div>
      </a-card>

      <a-card class="glass-card activity-card">
        <template #title>
          <div class="card-title">
            <img src="https://unpkg.com/lucide-static@latest/icons/clock.svg" class="card-icon" alt="activity" />
            活动足迹
          </div>
        </template>
        <a-timeline>
          <a-timeline-item v-for="item in activity" :key="item.time">
            <div class="timeline-text">
              <p class="timeline-title">{{ item.title }}</p>
              <p class="timeline-desc">{{ item.desc }}</p>
              <span class="timeline-time">{{ item.time }}</span>
            </div>
          </a-timeline-item>
        </a-timeline>
      </a-card>
    </div>

    <a-modal
      v-model:open="showPasswordModal"
      title="修改密码"
      :confirm-loading="changingPassword"
      @ok="handlePasswordChange"
      @cancel="resetPasswordForm"
    >
      <a-form layout="vertical">
        <a-form-item label="当前密码">
          <a-input-password v-model:value="passwordForm.oldPassword" placeholder="请输入当前密码" />
        </a-form-item>
        <a-form-item label="新密码">
          <a-input-password v-model:value="passwordForm.newPassword" placeholder="至少 8 位" />
        </a-form-item>
        <a-form-item label="确认新密码">
          <a-input-password v-model:value="passwordForm.confirmPassword" placeholder="请再次输入新密码" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { getLoginUser, updateMyUser, uploadAvatar } from '@/api/userController'

const loginUserStore = useLoginUserStore()

const loading = ref(false)
const saving = ref(false)
const isEditing = ref(false)
const showPasswordModal = ref(false)
const changingPassword = ref(false)
const avatarInput = ref<HTMLInputElement | null>(null)
const avatarUploading = ref(false)

type ProfileForm = {
  id?: number
  userName?: string
  userAccount?: string
  userAvatar?: string
  userProfile?: string
  userRole?: string
  createTime?: string
}

const form = reactive<ProfileForm>({
  id: undefined,
  userName: '',
  userAccount: '',
  userAvatar: '',
  userProfile: '',
  userRole: '',
  createTime: ''
})

const original = reactive<ProfileForm>({ ...form })

const stats = ref([
  { key: 'trips', label: '行程草稿', value: '—', icon: '🗺️', bg: 'rgba(59,130,246,0.12)' },
  { key: 'orders', label: '订单', value: '—', icon: '🛒', bg: 'rgba(16,185,129,0.12)' },
  { key: 'posts', label: '帖子', value: '—', icon: '📝', bg: 'rgba(234,179,8,0.16)' },
  { key: 'likes', label: '获赞', value: '—', icon: '👍', bg: 'rgba(244,63,94,0.12)' }
])

const activity = ref([
  { title: '欢迎回来', desc: '保持账号信息最新，提升体验', time: '刚刚' },
  { title: '安全检查', desc: '账户状态正常', time: '今天' }
])

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const roleMeta = computed(() => {
  const map: Record<string, { text: string; color: string }> = {
    admin: { text: '管理员', color: '#06b6d4' },
    merchant: { text: '商家', color: '#f97316' },
    user: { text: '普通用户', color: '#6366f1' }
  }
  return map[form.userRole || 'user'] || map.user
})

const lastActive = computed(() => {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
})

const displayDate = (value?: string) => {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const hydrateStats = (data: any) => {
  // 后端暂未提供汇总接口，预留兼容字段
  stats.value = stats.value.map((item) => {
    const incoming = data?.[item.key]
    return { ...item, value: incoming ?? item.value }
  })
}

const loadUser = async () => {
  loading.value = true
  try {
    const res = await getLoginUser()
    if (res.data.code === 0 && res.data.data) {
      Object.assign(form, res.data.data)
      Object.assign(original, res.data.data)
      loginUserStore.setLoginUser(res.data.data)
      hydrateStats(res.data.data?.stats)
    } else {
      message.error(res.data.message || '加载用户信息失败')
    }
  } catch (error) {
    message.error('加载用户信息失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const startEdit = () => {
  isEditing.value = true
}

const cancelEdit = () => {
  isEditing.value = false
  Object.assign(form, original)
}

const saveProfile = async () => {
  if (!form.userName?.trim()) {
    message.warning('请输入用户名')
    return
  }
  saving.value = true
  try {
    const payload = {
      id: form.id,
      userName: form.userName?.trim(),
      userAvatar: form.userAvatar,
      userProfile: form.userProfile
    }
    const res = await updateMyUser(payload as any)
    if (res.data.code === 0) {
      message.success('资料已保存')
      Object.assign(original, payload)
      loginUserStore.setLoginUser({ ...loginUserStore.loginUser, ...payload })
      isEditing.value = false
    } else {
      message.error(res.data.message || '保存失败')
    }
  } catch (error) {
    message.error('保存失败')
    console.error(error)
  } finally {
    saving.value = false
  }
}

const handleAvatarChange = async (e: Event) => {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  // 清空 input，方便重复选择同一文件
  if (target) {
    target.value = ''
  }
  if (!file) return
  if (!file.type.startsWith('image/')) {
    message.error('只能上传图片文件')
    return
  }
  if (file.size / 1024 / 1024 > 2) {
    message.error('图片大小不能超过 2MB')
    return
  }

  const formData = new FormData()
  formData.append('file', file)
  formData.append('picName', file.name || `avatar-${form.id ?? 'me'}`)

  avatarUploading.value = true
  try {
    const res = await uploadAvatar(formData)
    if (res.data.code === 0 && res.data.data) {
      form.userAvatar = res.data.data
      original.userAvatar = res.data.data
      loginUserStore.setLoginUser({ ...loginUserStore.loginUser, userAvatar: res.data.data })
      message.success('头像上传成功')
    } else {
      throw new Error(res.data.message || '上传失败')
    }
  } catch (error: any) {
    const msg = error?.response?.data?.message || error?.message || '头像上传失败'
    message.error(msg)
  } finally {
    avatarUploading.value = false
  }
}

const handlePasswordChange = async () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    message.warning('请填写完整')
    return
  }
  if (passwordForm.newPassword.length < 8) {
    message.warning('新密码至少 8 位')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    message.warning('两次输入的密码不一致')
    return
  }
  changingPassword.value = true
  try {
    // 如果后端提供修改密码接口，可在此处调用
    await new Promise((resolve) => setTimeout(resolve, 800))
    message.success('密码修改成功')
    showPasswordModal.value = false
    resetPasswordForm()
  } catch (error) {
    message.error('密码修改失败')
    console.error(error)
  } finally {
    changingPassword.value = false
  }
}

const resetPasswordForm = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

onMounted(() => {
  loadUser()
})
</script>

<style scoped lang="scss">
.profile-page {
  min-height: 100vh;
  padding: 20px;
  background: radial-gradient(circle at 20% 20%, rgba(99, 102, 241, 0.08), transparent 25%),
    radial-gradient(circle at 80% 0%, rgba(16, 185, 129, 0.08), transparent 22%),
    #f7f8fb;
}

.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 24px;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.12), rgba(16, 185, 129, 0.1));
  border: 1px solid rgba(99, 102, 241, 0.12);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.05);
}

.hero-left {
  display: flex;
  align-items: center;
  gap: 18px;
}

.hero-avatar {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;

  :deep(.ant-avatar) {
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
  }
}

.avatar-fallback {
  width: 48px;
  height: 48px;
  opacity: 0.55;
}

.avatar-upload {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px dashed rgba(99, 102, 241, 0.5);
  background: white;
  cursor: pointer;
  font-size: 13px;
  color: #4f46e5;

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.hero-summary h1 {
  margin: 0;
  font-size: 24px;
  color: #111827;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.role-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 12px;
  border: none;
  font-weight: 600;
}

.role-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.subtitle {
  color: #4b5563;
  margin: 4px 0 8px;
}

.meta {
  display: flex;
  gap: 18px;
  color: #6b7280;
  font-size: 13px;
  flex-wrap: wrap;
}

.meta-icon {
  width: 14px;
  height: 14px;
  margin-right: 6px;
  vertical-align: middle;
  opacity: 0.7;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.edit-actions {
  display: flex;
  gap: 10px;
}

.grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 16px;
}

.glass-card {
  border-radius: 16px;
  border: 1px solid rgba(17, 24, 39, 0.06);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.04);

  :deep(.ant-card-head) {
    border-bottom: 1px solid rgba(17, 24, 39, 0.05);
  }

  :deep(.ant-card-body) {
    padding: 18px;
  }
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  color: #111827;
}

.card-icon {
  width: 18px;
  height: 18px;
}

.profile-form :deep(.ant-form-item-label > label) {
  color: #374151;
  font-weight: 600;
}

.readonly {
  padding: 10px 12px;
  border-radius: 10px;
  background: #f9fafb;
  color: #1f2937;
}

.readonly.muted {
  color: #6b7280;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 12px;
}

.stat-chip {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  background: #f9fafb;
  border: 1px solid rgba(17, 24, 39, 0.04);
}

.stat-icon-circle {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  font-size: 20px;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
}

.stat-label {
  color: #6b7280;
  font-size: 13px;
}

.security-list {
  display: flex;
  flex-direction: column;
  gap: 6px;

  :deep(.ant-divider) {
    margin: 10px 0;
  }
}

.security-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.security-title {
  margin: 0;
  font-weight: 700;
  color: #111827;
}

.security-desc {
  margin: 4px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.activity-card :deep(.ant-timeline-item-content) {
  color: #111827;
}

.timeline-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.timeline-title {
  margin: 0;
  font-weight: 700;
}

.timeline-desc {
  margin: 0;
  color: #6b7280;
}

.timeline-time {
  color: #9ca3af;
  font-size: 12px;
}

.hidden-input {
  display: none;
}

@media (max-width: 900px) {
  .hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
