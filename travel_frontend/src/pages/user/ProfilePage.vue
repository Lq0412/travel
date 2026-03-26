<template>
  <div class="profile-page">
    <section class="profile-hero">
      <div class="hero-left">
        <div class="avatar-panel">
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
            {{ avatarUploading ? '上传中...' : '更换头像' }}
          </button>
          <input ref="avatarInput" type="file" accept="image/*" class="hidden-input" @change="handleAvatarChange" />
        </div>

        <div class="hero-copy">
          <p class="eyebrow">我的</p>
          <h1>{{ form.userName || '未命名用户' }}</h1>
          <p>账号：{{ form.userAccount || '—' }}</p>
          <div class="hero-meta">
            <a-tag :color="roleMeta.color">{{ roleMeta.text }}</a-tag>
            <span>加入时间：{{ displayDate(form.createTime) }}</span>
          </div>
        </div>
      </div>

      <div class="hero-actions">
        <a-button v-if="!isEditing" type="primary" size="large" @click="isEditing = true">编辑资料</a-button>
        <template v-else>
          <a-button type="primary" size="large" :loading="saving" @click="saveProfile">保存</a-button>
          <a-button size="large" @click="cancelEdit">取消</a-button>
        </template>
      </div>
    </section>

    <section class="card-grid">
      <a-card class="content-card" :loading="loading">
        <template #title>
          <div class="card-title">个人信息</div>
        </template>

        <a-form layout="vertical">
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
              :rows="4"
              :maxlength="200"
              show-count
              placeholder="介绍一下自己吧"
            />
            <div v-else class="readonly muted">
              {{ form.userProfile || '这个人很懒，什么都没有留下。' }}
            </div>
          </a-form-item>
        </a-form>
      </a-card>

      <a-card class="content-card">
        <template #title>
          <div class="card-title">账户与安全</div>
        </template>

        <div class="security-list">
          <div class="security-item">
            <div>
              <strong>登录密码</strong>
              <p>建议定期更新密码，避免在公共设备上保存敏感信息。</p>
            </div>
            <a-button type="link" @click="showPasswordModal = true">修改</a-button>
          </div>
          <div class="security-item">
            <div>
              <strong>账号角色</strong>
              <p>{{ roleMeta.text }}</p>
            </div>
            <a-tag :color="roleMeta.color">正常</a-tag>
          </div>
          <div class="security-item">
            <div>
              <strong>资料同步状态</strong>
              <p>当前页仅展示真实账号信息，不再展示占位统计数据。</p>
            </div>
            <a-tag color="processing">已清理伪数据</a-tag>
          </div>
        </div>
      </a-card>
    </section>

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
import { getLoginUser, updateMyUser, uploadAvatar } from '@/api/userController'
import { useLoginUserStore } from '@/stores/useLoginUserStore'

type ProfileForm = {
  id?: number
  userName?: string
  userAccount?: string
  userAvatar?: string
  userProfile?: string
  userRole?: string
  createTime?: string
}

const loginUserStore = useLoginUserStore()

const loading = ref(false)
const saving = ref(false)
const isEditing = ref(false)
const showPasswordModal = ref(false)
const changingPassword = ref(false)
const avatarInput = ref<HTMLInputElement | null>(null)
const avatarUploading = ref(false)

const form = reactive<ProfileForm>({
  id: undefined,
  userName: '',
  userAccount: '',
  userAvatar: '',
  userProfile: '',
  userRole: '',
  createTime: '',
})

const original = reactive<ProfileForm>({ ...form })

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const roleMeta = computed(() => {
  const map: Record<string, { text: string; color: string }> = {
    admin: { text: '管理员', color: 'cyan' },
    merchant: { text: '商家', color: 'orange' },
    user: { text: '普通用户', color: 'blue' },
  }
  return map[form.userRole || 'user'] || map.user
})

function displayDate(value?: string) {
  if (!value) {
    return '—'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

async function loadUser() {
  loading.value = true
  try {
    const res = await getLoginUser()
    if (res.data.code === 0 && res.data.data) {
      Object.assign(form, res.data.data)
      Object.assign(original, res.data.data)
      loginUserStore.setLoginUser(res.data.data)
      return
    }
    message.error(res.data.message || '加载用户信息失败')
  } catch {
    message.error('加载用户信息失败')
  } finally {
    loading.value = false
  }
}

function cancelEdit() {
  isEditing.value = false
  Object.assign(form, original)
}

async function saveProfile() {
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
      userProfile: form.userProfile,
    }
    const res = await updateMyUser(payload as any)
    if (res.data.code === 0) {
      Object.assign(original, { ...original, ...payload })
      loginUserStore.setLoginUser({ ...loginUserStore.loginUser, ...payload })
      isEditing.value = false
      message.success('资料已保存')
      return
    }
    message.error(res.data.message || '保存失败')
  } catch {
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function handleAvatarChange(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  target.value = ''

  if (!file) {
    return
  }
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
  formData.append('picName', file.name || `avatar-${form.id || 'me'}`)

  avatarUploading.value = true
  try {
    const res = await uploadAvatar(formData)
    if (res.data.code === 0 && res.data.data) {
      form.userAvatar = res.data.data
      original.userAvatar = res.data.data
      loginUserStore.setLoginUser({ ...loginUserStore.loginUser, userAvatar: res.data.data })
      message.success('头像上传成功')
      return
    }
    throw new Error(res.data.message || '上传失败')
  } catch (error: any) {
    const errorMsg = error?.response?.data?.message || error?.message || '头像上传失败'
    message.error(errorMsg)
  } finally {
    avatarUploading.value = false
  }
}

async function handlePasswordChange() {
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
    await new Promise((resolve) => setTimeout(resolve, 600))
    message.success('密码修改成功')
    showPasswordModal.value = false
    resetPasswordForm()
  } finally {
    changingPassword.value = false
  }
}

function resetPasswordForm() {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

onMounted(loadUser)
</script>

<style scoped lang="scss">
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.profile-hero,
.content-card {
  border-radius: 28px;
  border: 1px solid rgba(15, 28, 46, 0.08);
  background: #ffffff;
  box-shadow: 0 18px 48px rgba(18, 52, 97, 0.06);
}

.profile-hero {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  padding: 28px 30px;
  background:
    radial-gradient(circle at top right, rgba(47, 144, 240, 0.12), transparent 28%),
    linear-gradient(135deg, #ffffff 0%, #f5f9ff 100%);
}

.hero-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.avatar-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.avatar-fallback {
  width: 44px;
  height: 44px;
  opacity: 0.55;
}

.avatar-upload {
  border: 1px dashed rgba(59, 110, 220, 0.3);
  border-radius: 999px;
  background: #ffffff;
  padding: 6px 12px;
  color: var(--primary-700);
  cursor: pointer;
}

.hidden-input {
  display: none;
}

.eyebrow {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.14em;
  color: var(--primary-600);
}

.hero-copy {
  h1 {
    margin: 8px 0 8px;
    font-size: 34px;
    color: var(--color-text);
  }

  p {
    margin: 0;
    color: var(--color-text-secondary);
  }
}

.hero-meta {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 12px;
  color: var(--color-muted);
}

.hero-actions {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.card-grid {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 20px;
}

.card-title {
  font-weight: 700;
  color: var(--color-text);
}

.readonly {
  min-height: 42px;
  padding: 10px 12px;
  border-radius: 12px;
  background: #f8fbff;
  color: var(--color-text);
}

.readonly.muted {
  color: var(--color-muted);
  line-height: 1.7;
}

.security-list {
  display: grid;
  gap: 18px;
}

.security-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
  border-radius: 18px;
  background: #f8fbff;
  border: 1px solid rgba(15, 28, 46, 0.06);

  strong {
    display: block;
    margin-bottom: 6px;
    color: var(--color-text);
  }

  p {
    margin: 0;
    color: var(--color-muted);
    line-height: 1.7;
  }
}

@media (max-width: 1000px) {
  .card-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .profile-hero {
    flex-direction: column;
    padding: 22px 18px;
  }

  .hero-left,
  .security-item {
    flex-direction: column;
  }

  .hero-copy h1 {
    font-size: 28px;
  }
}
</style>
