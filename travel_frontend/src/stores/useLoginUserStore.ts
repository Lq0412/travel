import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getLoginUser } from '@/api/userController'

const guestUser: API.LoginUserVO = {
  id: undefined,
  userName: '未登录',
  userAvatar: '',
  userAccount: '',
  userRole: undefined,
}

export const useLoginUserStore = defineStore('loginUser', () => {
  const loginUser = ref<API.LoginUserVO>({ ...guestUser })

  async function fetchLoginUser() {
    try {
      const res = await getLoginUser()
      const isSuccess = res.data.code === 0 || res.data.code === 200
      loginUser.value = isSuccess && res.data.data ? res.data.data : { ...guestUser }
    } catch {
      loginUser.value = { ...guestUser }
    }
  }

  function setLoginUser(newLoginUser: API.LoginUserVO) {
    loginUser.value = {
      ...guestUser,
      ...newLoginUser,
    }
  }

  return { loginUser, fetchLoginUser, setLoginUser }
})
