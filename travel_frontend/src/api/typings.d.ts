declare namespace API {
  // ========== AI 对话相关 ==========
  type AIConversation = {
    id?: number
    userId?: number
    title?: string
    provider?: string
    model?: string
    status?: string
    createTime?: string
    updateTime?: string
    isDelete?: boolean
  }

  type AIConversationVO = {
    id?: number
    userId?: number
    title?: string
    provider?: string
    model?: string
    status?: string
    createTime?: string
    updateTime?: string
    messageCount?: number
    lastMessage?: string
    lastMessageTime?: string
  }

  type AIMessage = {
    id?: number
    conversationId?: number
    role?: string
    content?: string
    tokensUsed?: number
    responseTime?: number
    createTime?: string
    isDelete?: boolean
  }

  type AIRequest = {
    provider?: string
    message: string
    systemPrompt?: string
    model?: string
    temperature?: number
    maxTokens?: number
    stream?: boolean
    history?: Message[]
    parameters?: Record<string, any>
  }

  type AIResponse = {
    content?: string
    model?: string
    provider?: string
    success?: boolean
    errorMessage?: string
    tokensUsed?: number
    responseTime?: number
    conversationTitle?: string
    metadata?: Record<string, any>
    fromCache?: boolean
    nullCacheMarker?: boolean
    errorCacheMarker?: boolean
  }

  type Message = {
    role?: string
    content?: string
    timestamp?: number
  }

  // ========== 行程相关 ==========
  type TripGenerateRequest = {
    destination?: string
    days?: number
    budget?: number
    theme?: string
  }

  type TripGenerateResponse = {
    plans?: TripPlan[]
  }

  type TripPlan = {
    planId?: string
    destination?: string
    days?: number
    budget?: number
    theme?: string
    dailyHighlights?: Record<string, any>
    description?: string
  }

  type TripSaveRequest = {
    planId?: string
    destination?: string
    days?: number
    budget?: number
    theme?: string
    startDate?: string
    endDate?: string
    dailyHighlights?: Record<string, any>
    structuredData?: string
  }

  type TripVO = {
    id?: number
    userId?: number
    destination?: string
    days?: number
    budget?: number
    theme?: string
    startDate?: string
    endDate?: string
    dailyHighlights?: Record<string, any>
    status?: string
    createTime?: string
    updateTime?: string
    photos?: TripPhotoVO[]
    publishedToInspiration?: boolean
    publishedPostId?: number
  }

  type TripPhotoVO = {
    id?: number
    tripId?: number
    photoUrl?: string
    shotTime?: string
    description?: string
    sortOrder?: number
    createTime?: string
  }

  // ========== 内容图片相关 ==========
  type ContentImageVO = {
    id?: number
    alt?: string
    photographer?: string
    photographerUrl?: string
    pexelsUrl?: string
    width?: number
    height?: number
    mediumUrl?: string
    largeUrl?: string
    large2xUrl?: string
    landscapeUrl?: string
  }

  // ========== 用户相关 ==========
  type LoginUserVO = {
    id?: number
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    editTime?: string
    createTime?: string
    updateTime?: string
  }

  type UserVO = {
    id?: number
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    createTime?: string
  }

  type User = {
    id?: number
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    userPassword?: string
    createTime?: string
    updateTime?: string
  }

  type UserLoginRequest = {
    userAccount: string
    userPassword: string
  }

  type UserRegisterRequest = {
    userAccount: string
    userPassword: string
    checkPassword: string
  }

  type UserAddRequest = {
    userAccount?: string
    userName?: string
    userRole?: string
    userProfile?: string
  }

  type UserUpdateRequest = {
    id?: number
    userAccount?: string
    userName?: string
    userRole?: string
    userProfile?: string
  }

  type UserUpdateMyRequest = {
    userName?: string
    userAvatar?: string
    userProfile?: string
  }

  type UserQueryRequest = {
    current?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    userName?: string
    userRole?: string
  }

  type SseEmitter = {
    timeout?: number
  }

  type ResponseDTOVoid = ResponseDTO<void>
  type ResponseDTOMapStringObject = ResponseDTO<Record<string, any>>
  type ResponseDTOUser = ResponseDTO<User>
  type ResponseDTOPageUserVO = ResponseDTO<{ records?: UserVO[]; total?: number; size?: number; current?: number; pages?: number }>

  type getUserConversationsParams = {
    userId: number
    pageNum?: number
    pageSize?: number
  }

  type clearConversationCacheParams = {
    conversationId: number
  }

  type rechargeQuotaParams = {
    userId: number
    tokens: number
  }

  type resetUserQuotaParams = {
    userId: number
    quota: number
  }

  // ========== 通用响应 ==========
  type ResponseDTO<T = any> = {
    code?: number
    message?: string
    data?: T
    timestamp?: number
  }

  type ResponseDTOBoolean = ResponseDTO<boolean>
  type ResponseDTOString = ResponseDTO<string>
  type ResponseDTOInteger = ResponseDTO<number>
  type ResponseDTOLong = ResponseDTO<number>

  type ResponseDTOAIConversation = ResponseDTO<AIConversation>
  type ResponseDTOAIResponse = ResponseDTO<AIResponse>
  type ResponseDTOListAIConversationVO = ResponseDTO<AIConversationVO[]>
  type ResponseDTOListAIMessage = ResponseDTO<AIMessage[]>

  type ResponseDTOTripVO = ResponseDTO<TripVO>
  type ResponseDTOListTripVO = ResponseDTO<TripVO[]>
  type ResponseDTOTripGenerateResponse = ResponseDTO<TripGenerateResponse>
  type ResponseDTOTripPhotoVO = ResponseDTO<TripPhotoVO>
  type ResponseDTOListTripPhotoVO = ResponseDTO<TripPhotoVO[]>

  type ResponseDTOListContentImageVO = ResponseDTO<ContentImageVO[]>

  type ResponseDTOLoginUserVO = ResponseDTO<LoginUserVO>
  type ResponseDTOUserVO = ResponseDTO<UserVO>

  // ========== 请求参数 ==========
  type createConversationParams = {
    userId: number
    title?: string
    provider?: string
    model?: string
  }

  type deleteConversationParams = {
    conversationId: number
    userId: number
  }

  type getConversationParams = {
    conversationId: number
    userId: number
  }

  type getConversationMessagesParams = {
    conversationId: number
    userId: number
  }

  type getRecentMessagesParams = {
    conversationId: number
    userId: number
    limit?: number
  }

  type updateConversationTitleParams = {
    conversationId: number
    userId: number
    title: string
  }

  type chatParams = {
    message: string
    voiceId?: number
    voiceSpeed?: number
  }

  type tourismStreamParams = {
    task: string
    context?: string
    goal?: string
    constraints?: string
    conversationId?: number
  }

  type getTripByIdParams = { id: number }
  type deleteTripParams = { id: number }
  type completeTripParams = { id: number }
  type getTripPhotosParams = { tripId: number }
  type deletePhotoParams = { photoId: number }
  type uploadPhotoParams = { tripId: number; photoUrl: string }
  type uploadPhotosParams = { tripId: number }

  type searchContentImagesParams = {
    query: string
    perPage?: number
  }

  type getUserByIdParams = { id: number }
  type getUserVOByIdParams = { id: number }

  type DeleteRequest = { id?: number }

  type publishToForumParams = { id: number }
  type ForumPublishRequest = {
    tripId?: number
    title?: string
    content?: string
    categoryId?: number
    tags?: string[]
  }
}
