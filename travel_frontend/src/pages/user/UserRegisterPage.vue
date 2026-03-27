<template>
  <div class="auth-page">
    <!-- 背景图 -->
    <div class="auth-bg">
      <img src="https://picsum.photos/1920/1080?random=1" alt="背景">
      <div class="auth-bg-overlay"></div>
    </div>

    <!-- 返回首页 -->
    <router-link to="/" class="back-link">
      <span>← 返回首页</span>
    </router-link>

    <!-- 注册卡片 -->
    <div class="auth-card">
      <div class="auth-header">
        <h1>旅刻</h1>
        <p>创建你的账户</p>
      </div>

      <a-form
        :model="formState"
        name="registerForm"
        autocomplete="off"
        @finish="handleSubmit"
        layout="vertical"
      >
        <a-form-item
          label="用户名"
          name="userAccount"
          :rules="[{ required: true, message: '请输入用户名' }]"
        >
          <a-input
            v-model:value="formState.userAccount"
            placeholder="请输入用户名"
            size="large"
          />
        </a-form-item>

        <a-form-item
          label="密码"
          name="userPassword"
          :rules="[
            { required: true, message: '请输入密码' },
            { min: 8, message: '密码长度不能小于8位' }
          ]"
        >
          <a-input-password
            v-model:value="formState.userPassword"
            placeholder="至少8位字符"
            size="large"
          />
        </a-form-item>

        <a-form-item
          label="确认密码"
          name="checkPassword"
          :rules="[
            { required: true, message: '请确认密码' },
            { validator: validatePassword }
          ]"
        >
          <a-input-password
            v-model:value="formState.checkPassword"
            placeholder="再次输入密码"
            size="large"
          />
        </a-form-item>

        <a-form-item>
          <a-button
            type="primary"
            html-type="submit"
            size="large"
            block
            :loading="loading"
          >
            注册
          </a-button>
        </a-form-item>
      </a-form>

      <div class="auth-footer">
        <span>已有账号？</span>
        <router-link to="/user/login">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue';
import { userRegister } from '@/api/userController';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
});

const loading = ref(false);
const router = useRouter();

const validatePassword = async (_rule: unknown, value: string) => {
  if (value !== formState.userPassword) {
    throw new Error('两次输入的密码不一致');
  }
};

const handleSubmit = async (values: API.UserRegisterRequest) => {
  try {
    loading.value = true;

    if (!values.userAccount || !values.userPassword || !values.checkPassword) {
      message.error('请填写完整的注册信息');
      return;
    }

    if (values.userPassword !== values.checkPassword) {
      message.error('两次输入的密码不一致');
      return;
    }

    const res = await userRegister({
      userAccount: values.userAccount,
      userPassword: values.userPassword,
      checkPassword: values.checkPassword
    });

    if (res.data.code === 0) {
      message.success('注册成功，请登录');
      router.push({ path: '/user/login', replace: true });
    } else {
      message.error('注册失败，' + res.data.message);
    }
  } catch (error) {
    console.error('注册错误:', error);
    message.error('注册失败，请检查网络连接');
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped lang="scss">
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  position: relative;
}

.auth-bg {
  position: absolute;
  inset: 0;
  z-index: 0;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    object-position: center;
    filter: brightness(0.7);
  }

  .auth-bg-overlay {
    position: absolute;
    inset: 0;
    background: linear-gradient(135deg, rgba(0,0,0,0.3) 0%, rgba(0,0,0,0.5) 100%);
  }
}

.back-link {
  position: fixed;
  top: 24px;
  left: 24px;
  z-index: 10;
  color: rgba(255,255,255,0.8);
  text-decoration: none;
  font-size: 14px;
  transition: color 0.2s;

  &:hover {
    color: #fff;
  }
}

.auth-card {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 400px;
  background: rgba(255,255,255,0.95);
  backdrop-filter: blur(10px);
  border: none;
  border-radius: var(--radius-lg);
  padding: 40px 32px;
  box-shadow: 0 25px 50px -12px rgba(0,0,0,0.25);
}

.auth-header {
  text-align: center;
  margin-bottom: 32px;

  h1 {
    font-size: 28px;
    font-weight: 700;
    color: var(--color-text);
    margin-bottom: 8px;
  }

  p {
    font-size: 14px;
    color: var(--color-muted);
  }
}

.auth-footer {
  text-align: center;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid var(--color-border);
  font-size: 14px;
  color: var(--color-muted);

  a {
    color: var(--accent-600);
    text-decoration: none;
    margin-left: 4px;

    &:hover {
      text-decoration: underline;
    }
  }
}

:deep(.ant-form-item-label > label) {
  color: var(--color-text);
  font-weight: 500;
}

:deep(.ant-input),
:deep(.ant-input-password) {
  background: var(--color-bg);
  border-color: var(--color-border);

  &:hover {
    border-color: var(--accent-400);
  }

  &:focus {
    border-color: var(--accent-600);
    box-shadow: 0 0 0 2px rgba(255, 107, 53, 0.1);
  }
}
</style>
