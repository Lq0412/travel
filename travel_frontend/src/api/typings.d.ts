declare namespace API {
  type adminGetMerchantParams = {
    id: number
  }

  type adminGetProductParams = {
    id: number
  }

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

  type cancelLikeMessageParams = {
    messageId: number
  }

  type cancelOrderParams = {
    orderId: number
    reason?: string
  }

  type Cart = {
    id?: number
    userId?: number
    productId?: number
    merchantId?: number
    quantity?: number
    specs?: string
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type CartVO = {
    id?: number
    userId?: number
    productId?: number
    merchantId?: number
    quantity?: number
    specs?: string
    createTime?: string
    updateTime?: string
    product?: Product
    subtotal?: number
  }

  type Category = {
    id?: number
    name?: string
    icon?: string
    sort?: number
  }

  type chatParams = {
    message: string
    voiceId?: number
    voiceSpeed?: number
  }

  type checkCommentLikeParams = {
    commentId: number
  }

  type checkPostFavoriteParams = {
    postId: number
  }

  type checkPostLikeParams = {
    postId: number
  }

  type clearConversationCacheParams = {
    conversationId: number
  }

  type clearPictureCacheParams = {
    pictureId: number
  }

  type Comment = {
    id?: number
    userId?: number
    postId?: number
    content?: string
    likeCount?: number
    parentId?: number
    replyToUserId?: number
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type CommentAddRequest = {
    postId?: number
    content?: string
    parentId?: number
    replyToUserId?: number
  }

  type CommentLikeRequest = {
    commentId?: number
  }

  type CommentQueryRequest = {
    postId?: number
    current?: number
    pageSize?: number
    commentId?: number
  }

  type completeTripParams = {
    id: number
  }

  type createConversationParams = {
    userId: number
    title?: string
    provider?: string
    model?: string
  }

  type CreateOrderItem = {
    productId?: number
    quantity?: number
    price?: number
    specs?: string
  }

  type CreateOrderRequest = {
    merchantId?: number
    contactName?: string
    contactPhone?: string
    shippingAddress?: string
    remark?: string
    payMethod?: number
    items?: CreateOrderItem[]
  }

  type deleteCategoryParams = {
    id: number
  }

  type deleteCommentParams = {
    commentId: number
  }

  type deleteConversationParams = {
    conversationId: number
    userId: number
  }

  type deleteMessageParams = {
    messageId: number
  }

  type deletePhotoParams = {
    photoId: number
  }

  type DeleteRequest = {
    id?: number
  }

  type deleteTripParams = {
    id: number
  }

  type digitalHumanStreamParams = {
    task: string
    context?: string
    goal?: string
    constraints?: string
    conversationId?: number
  }

  type ForumPublishRequest = {
    tripId?: number
    memoryCardId?: number
    title?: string
    content?: string
    categoryId?: number
    tags?: string[]
  }

  type getCategoryByIdParams = {
    id: number
  }

  type getConversationMessagesParams = {
    conversationId: number
    userId: number
  }

  type getConversationParams = {
    conversationId: number
    userId: number
  }

  type getHotTagsParams = {
    limit?: number
  }

  type getMemoryCardByTripIdParams = {
    tripId: number
  }

  type getMemoryCardStatusParams = {
    taskId: string
  }

  type getMerchantDetailParams = {
    merchantId: number
  }

  type getMerchantListParams = {
    queryRequest: MerchantQueryRequest
  }

  type getMerchantOrderDetailParams = {
    orderId: number
  }

  type getMerchantProductsParams = {
    merchantId: number
    queryRequest: MerchantQueryRequest
  }

  type getMyOrderDetailParams = {
    orderId: number
  }

  type getPictureByIdParams = {
    id: number
  }

  type getPictureVOByIdParams = {
    id: number
  }

  type getPostByIdParams = {
    id: number
  }

  type getProductByIdParams = {
    id: number
  }

  type getRecentMessagesParams = {
    conversationId: number
    userId: number
    limit?: number
  }

  type getRecommendedMerchantsParams = {
    limit?: number
  }

  type getScenicDetailParams = {
    id: number
  }

  type getTagsByPostIdParams = {
    postId: number
  }

  type getTripByIdParams = {
    id: number
  }

  type getTripPhotosParams = {
    tripId: number
  }

  type getUserByIdParams = {
    id: number
  }

  type getUserConversationsParams = {
    userId: number
    pageNum?: number
    pageSize?: number
  }

  type getUserVOByIdParams = {
    id: number
  }

  type IPageMerchant = {
    size?: number
    current?: number
    total?: number
    records?: Merchant[]
    pages?: number
  }

  type IPageOrder = {
    size?: number
    current?: number
    total?: number
    records?: Order[]
    pages?: number
  }

  type IPageProduct = {
    size?: number
    current?: number
    total?: number
    records?: Product[]
    pages?: number
  }

  type likeMessageParams = {
    messageId: number
  }

  type LikeRequest = {
    postId?: number
  }

  type listMerchantOrdersParams = {
    current?: number
    size?: number
    orderNo?: string
    status?: number
  }

  type listMyOrdersParams = {
    current?: number
    size?: number
  }

  type listOrderItemsParams = {
    orderId: number
  }

  type listProductsParams = {
    merchantId?: number
    category?: string
    name?: string
    current?: number
    pageSize?: number
  }

  type LoginUserVO = {
    id?: number
    userAzccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    editTime?: string
    createTime?: string
    updateTime?: string
  }

  type MemoryCardGenerateRequest = {
    tripId?: number
    photoUrls?: string[]
    templateName?: string
  }

  type MemoryCardGenerateResponse = {
    taskId?: string
    status?: string
    imageUrl?: string
    errorMessage?: string
  }

  type MemoryCardVO = {
    id?: number
    tripId?: number
    userId?: number
    templateName?: string
    imageUrl?: string
    taskId?: string
    status?: string
    errorMessage?: string
    retryCount?: number
    createTime?: string
    updateTime?: string
  }

  type Merchant = {
    id?: number
    userId?: number
    name?: string
    type?: number
    introduction?: string
    coverUrl?: string
    logoUrl?: string
    longitude?: number
    latitude?: number
    location?: string
    openHours?: string
    contactPhone?: string
    rating?: number
    status?: number
    minPrice?: number
    maxPrice?: number
    tags?: string
    businessHours?: string
    serviceScore?: number
    environmentScore?: number
    featureTags?: string
    isVerified?: number
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type merchantCancelOrderParams = {
    orderId: number
    reason?: string
  }

  type merchantListProductsParams = {
    current?: number
    pageSize?: number
    name?: string
    status?: number
  }

  type MerchantQueryRequest = {
    current?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    name?: string
    type?: number
    status?: number
    userId?: number
    searchText?: string
    minRating?: number
    maxRating?: number
    location?: string
  }

  type Message = {
    role?: string
    content?: string
    timestamp?: number
  }

  type MessageWallDTO = {
    scenicSpotId: number
    content?: string
    textColor?: string
    fontSize?: number
    backgroundColor?: string
    backgroundId?: number
    isBarrage?: boolean
    barrageSpeed?: number
    barrageTrajectory?: number
    isAnonymous?: boolean
    messageType?: number
  }

  type MessageWallQueryRequest = {
    scenicSpotId?: number
    userId?: number
    messageType?: number
    status?: number
    isBarrage?: boolean
    keyword?: string
    pageSize?: number
    current?: number
  }

  type MessageWallVO = {
    id?: number
    scenicSpotId?: number
    userId?: number
    userName?: string
    userAvatar?: string
    content?: string
    likes?: number
    textColor?: string
    fontSize?: number
    backgroundColor?: string
    backgroundId?: number
    backgroundUrl?: string
    isBarrage?: boolean
    barrageSpeed?: number
    barrageTrajectory?: number
    isAnonymous?: boolean
    messageType?: number
    status?: number
    isLiked?: boolean
    createTime?: string
  }

  type Order = {
    id?: number
    orderNo?: string
    userId?: number
    merchantId?: number
    totalAmount?: number
    actualAmount?: number
    discountAmount?: number
    status?: number
    payTime?: string
    payMethod?: number
    transactionId?: string
    contactName?: string
    contactPhone?: string
    remark?: string
    shippingAddress?: string
    invoiceInfo?: string
    cancelReason?: string
    refundReason?: string
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type OrderItem = {
    column?: string
    asc?: boolean
  }

  type PageComment = {
    records?: Comment[]
    total?: number
    size?: number
    current?: number
    orders?: OrderItem[]
    optimizeCountSql?: PageComment
    searchCount?: PageComment
    optimizeJoinOfCountSql?: boolean
    maxLimit?: number
    countId?: string
    pages?: number
  }

  type PageMessageWallVO = {
    records?: MessageWallVO[]
    total?: number
    size?: number
    current?: number
    orders?: OrderItem[]
    optimizeCountSql?: PageMessageWallVO
    searchCount?: PageMessageWallVO
    optimizeJoinOfCountSql?: boolean
    maxLimit?: number
    countId?: string
    pages?: number
  }

  type PagePicture = {
    records?: Picture[]
    total?: number
    size?: number
    current?: number
    orders?: OrderItem[]
    optimizeCountSql?: PagePicture
    searchCount?: PagePicture
    optimizeJoinOfCountSql?: boolean
    maxLimit?: number
    countId?: string
    pages?: number
  }

  type PagePictureVO = {
    records?: PictureVO[]
    total?: number
    size?: number
    current?: number
    orders?: OrderItem[]
    optimizeCountSql?: PagePictureVO
    searchCount?: PagePictureVO
    optimizeJoinOfCountSql?: boolean
    maxLimit?: number
    countId?: string
    pages?: number
  }

  type PagePost = {
    records?: Post[]
    total?: number
    size?: number
    current?: number
    orders?: OrderItem[]
    optimizeCountSql?: PagePost
    searchCount?: PagePost
    optimizeJoinOfCountSql?: boolean
    maxLimit?: number
    countId?: string
    pages?: number
  }

  type PagePostVO = {
    records?: PostVO[]
    total?: number
    size?: number
    current?: number
    orders?: OrderItem[]
    optimizeCountSql?: PagePostVO
    searchCount?: PagePostVO
    optimizeJoinOfCountSql?: boolean
    maxLimit?: number
    countId?: string
    pages?: number
  }

  type PageUserVO = {
    records?: UserVO[]
    total?: number
    size?: number
    current?: number
    orders?: OrderItem[]
    optimizeCountSql?: PageUserVO
    searchCount?: PageUserVO
    optimizeJoinOfCountSql?: boolean
    maxLimit?: number
    countId?: string
    pages?: number
  }

  type payOrderParams = {
    orderId: number
    payMethod: number
  }

  type Picture = {
    id?: number
    url?: string
    thumbnailUrl?: string
    name?: string
    introduction?: string
    category?: string
    tags?: string
    picSize?: number
    picWidth?: number
    picHeight?: number
    picScale?: number
    picFormat?: string
    userId?: number
    createTime?: string
    editTime?: string
    updateTime?: string
    reviewStatus?: number
    reviewMessage?: string
    reviewerId?: number
    reviewTime?: string
    isDelete?: number
  }

  type PictureEditRequest = {
    id?: number
    name?: string
    introduction?: string
    category?: string
    tags?: string[]
  }

  type PictureQueryRequest = {
    current?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    name?: string
    introduction?: string
    category?: string
    tags?: string[]
    picSize?: number
    picWidth?: number
    picHeight?: number
    picScale?: number
    picFormat?: string
    searchText?: string
    userId?: number
    reviewStatus?: number
    reviewMessage?: string
    reviewerId?: number
    startEditTime?: string
    endEditTime?: string
  }

  type PictureReviewRequest = {
    id?: number
    reviewStatus?: number
    reviewMessage?: string
  }

  type PictureTagCategory = {
    tagList?: string[]
    categoryList?: string[]
  }

  type PictureUpdateRequest = {
    id?: number
    name?: string
    introduction?: string
    category?: string
    tags?: string[]
  }

  type PictureUploadByBatchRequest = {
    searchText?: string
    count?: number
    namePrefix?: string
  }

  type PictureUploadRequest = {
    id?: number
    fileUrl?: string
    picName?: string
  }

  type PictureVO = {
    id?: number
    url?: string
    thumbnailUrl?: string
    name?: string
    introduction?: string
    tags?: string[]
    category?: string
    picSize?: number
    picWidth?: number
    picHeight?: number
    picScale?: number
    picFormat?: string
    userId?: number
    createTime?: string
    editTime?: string
    updateTime?: string
    user?: UserVO
  }

  type Post = {
    id?: number
    userId?: number
    title?: string
    content?: string
    coverUrl?: string
    viewCount?: number
    likeCount?: number
    commentCount?: number
    status?: number
    createTime?: string
    updateTime?: string
    isDelete?: number
    categoryId?: number
  }

  type PostAddRequest = {
    title?: string
    content?: string
    coverUrl?: string
    categoryId?: number
    tags?: string[]
  }

  type PostQueryRequest = {
    current?: number
    pageSize?: number
    sortField?: string
    keyword?: string
    tagName?: string
    tagNames?: string[]
    tagId?: number
    tagIds?: number[]
    categoryId?: number
  }

  type PostVO = {
    id?: number
    title?: string
    content?: string
    coverUrl?: string
    viewCount?: number
    likeCount?: number
    commentCount?: number
    createTime?: string
    authorName?: string
    authorAvatar?: string
    tags?: string[]
  }

  type Product = {
    id?: number
    merchantId?: number
    name?: string
    categoryId?: number
    category?: string
    price?: number
    originalPrice?: number
    coverUrl?: string
    images?: string
    description?: string
    specs?: string
    stock?: number
    sales?: number
    status?: number
    isRecommend?: number
    weight?: number
    serviceGuarantee?: string
    limitPerUser?: number
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type ProductQueryRequest = {
    current?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
  }

  type publishToForumParams = {
    id: number
  }

  type rechargeQuotaParams = {
    userId: number
    tokens: number
  }

  type resetUserQuotaParams = {
    userId: number
    quota: number
  }

  type ResponseDTOAIConversation = {
    code?: number
    message?: string
    data?: AIConversation
    timestamp?: number
  }

  type ResponseDTOAIResponse = {
    code?: number
    message?: string
    data?: AIResponse
    timestamp?: number
  }

  type ResponseDTOBoolean = {
    code?: number
    message?: string
    data?: boolean
    timestamp?: number
  }

  type ResponseDTOCategory = {
    code?: number
    message?: string
    data?: Category
    timestamp?: number
  }

  type ResponseDTOInteger = {
    code?: number
    message?: string
    data?: number
    timestamp?: number
  }
  type ResponseDTOString = {
    code?: number
    message?: string
    data?: string
    timestamp?: number
  }

  type ResponseDTOIPageMerchant = {
    code?: number
    message?: string
    data?: IPageMerchant
    timestamp?: number
  }

  type ResponseDTOIPageOrder = {
    code?: number
    message?: string
    data?: IPageOrder
    timestamp?: number
  }

  type ResponseDTOIPageProduct = {
    code?: number
    message?: string
    data?: IPageProduct
    timestamp?: number
  }

  type ResponseDTOListAIConversationVO = {
    code?: number
    message?: string
    data?: AIConversationVO[]
    timestamp?: number
  }

  type ResponseDTOListAIMessage = {
    code?: number
    message?: string
    data?: AIMessage[]
    timestamp?: number
  }

  type ResponseDTOListCartVO = {
    code?: number
    message?: string
    data?: CartVO[]
    timestamp?: number
  }

  type ResponseDTOListCategory = {
    code?: number
    message?: string
    data?: Category[]
    timestamp?: number
  }

  type ResponseDTOListMerchant = {
    code?: number
    message?: string
    data?: Merchant[]
    timestamp?: number
  }

  type ResponseDTOListOrderItem = {
    code?: number
    message?: string
    data?: OrderItem[]
    timestamp?: number
  }

  type ResponseDTOListScenicMessageWallVO = {
    code?: number
    message?: string
    data?: ScenicMessageWallVO[]
    timestamp?: number
  }

  type ResponseDTOListScenicSpot = {
    code?: number
    message?: string
    data?: ScenicSpot[]
    timestamp?: number
  }

  type ResponseDTOListTag = {
    code?: number
    message?: string
    data?: Tag[]
    timestamp?: number
  }

  type ResponseDTOListTripPhotoVO = {
    code?: number
    message?: string
    data?: TripPhotoVO[]
    timestamp?: number
  }

  type ResponseDTOListTripVO = {
    code?: number
    message?: string
    data?: TripVO[]
    timestamp?: number
  }

  type ResponseDTOLoginUserVO = {
    code?: number
    message?: string
    data?: LoginUserVO
    timestamp?: number
  }

  type ResponseDTOLong = {
    code?: number
    message?: string
    data?: number
    timestamp?: number
  }

  type ResponseDTOMapStringObject = {
    code?: number
    message?: string
    data?: Record<string, any>
    timestamp?: number
  }

  type ResponseDTOMemoryCardGenerateResponse = {
    code?: number
    message?: string
    data?: MemoryCardGenerateResponse
    timestamp?: number
  }

  type ResponseDTOMemoryCardVO = {
    code?: number
    message?: string
    data?: MemoryCardVO
    timestamp?: number
  }

  type ResponseDTOMerchant = {
    code?: number
    message?: string
    data?: Merchant
    timestamp?: number
  }

  type ResponseDTOMessageWallVO = {
    code?: number
    message?: string
    data?: MessageWallVO
    timestamp?: number
  }

  type ResponseDTOOrder = {
    code?: number
    message?: string
    data?: Order
    timestamp?: number
  }

  type ResponseDTOPageComment = {
    code?: number
    message?: string
    data?: PageComment
    timestamp?: number
  }

  type ResponseDTOPageMessageWallVO = {
    code?: number
    message?: string
    data?: PageMessageWallVO
    timestamp?: number
  }

  type ResponseDTOPagePicture = {
    code?: number
    message?: string
    data?: PagePicture
    timestamp?: number
  }

  type ResponseDTOPagePictureVO = {
    code?: number
    message?: string
    data?: PagePictureVO
    timestamp?: number
  }

  type ResponseDTOPagePost = {
    code?: number
    message?: string
    data?: PagePost
    timestamp?: number
  }

  type ResponseDTOPagePostVO = {
    code?: number
    message?: string
    data?: PagePostVO
    timestamp?: number
  }

  type ResponseDTOPageUserVO = {
    code?: number
    message?: string
    data?: PageUserVO
    timestamp?: number
  }

  type ResponseDTOPicture = {
    code?: number
    message?: string
    data?: Picture
    timestamp?: number
  }

  type ResponseDTOPictureTagCategory = {
    code?: number
    message?: string
    data?: PictureTagCategory
    timestamp?: number
  }

  type ResponseDTOPictureVO = {
    code?: number
    message?: string
    data?: PictureVO
    timestamp?: number
  }

  type ResponseDTOPostVO = {
    code?: number
    message?: string
    data?: PostVO
    timestamp?: number
  }

  type ResponseDTOProduct = {
    code?: number
    message?: string
    data?: Product
    timestamp?: number
  }

  type ResponseDTOScenicMessageWallVO = {
    code?: number
    message?: string
    data?: ScenicMessageWallVO
    timestamp?: number
  }

  type ResponseDTOScenicSpot = {
    code?: number
    message?: string
    data?: ScenicSpot
    timestamp?: number
  }

  type ResponseDTOTripGenerateResponse = {
    code?: number
    message?: string
    data?: TripGenerateResponse
    timestamp?: number
  }

  type ResponseDTOTripPhotoVO = {
    code?: number
    message?: string
    data?: TripPhotoVO
    timestamp?: number
  }

  type ResponseDTOTripVO = {
    code?: number
    message?: string
    data?: TripVO
    timestamp?: number
  }

  type ResponseDTOUser = {
    code?: number
    message?: string
    data?: User
    timestamp?: number
  }

  type ResponseDTOUserVO = {
    code?: number
    message?: string
    data?: UserVO
    timestamp?: number
  }

  type ResponseDTOVoid = {
    code?: number
    message?: string
    data?: Record<string, any>
    timestamp?: number
  }

  type reviewMessageParams = {
    messageId: number
    status: number
  }

  type ScenicMessageWallDTO = {
    scenicSpotId: number
    title: string
    description?: string
  }

  type ScenicMessageWallVO = {
    scenicSpotId?: number
    scenicSpotName?: string
    title?: string
    description?: string
    messageCount?: number
    createTime?: string
    updateTime?: string
  }

  type ScenicSpot = {
    id?: number
    name?: string
    introduction?: string
    coverUrl?: string
    messageWallTitle?: string
    messageWallDescription?: string
    location?: string
    longitude?: number
    latitude?: number
    tags?: string
    openHours?: string
    ticketInfo?: string
    rating?: number
    heatValue?: number
    viewCount?: number
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type searchTagsParams = {
    keyword: string
  }

  type shipOrderParams = {
    orderId: number
    trackingNumber?: string
  }

  type SseEmitter = {
    timeout?: number
  }

  type Tag = {
    id?: number
    name?: string
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type tourismStreamParams = {
    task: string
    context?: string
    goal?: string
    constraints?: string
    conversationId?: number
  }

  type TripGenerateRequest = {
    destination?: string
    days?: number
    budget?: number
    theme?: string
  }

  type TripGenerateResponse = {
    plans?: TripPlan[]
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
    memoryCard?: MemoryCardVO
  }

  type updateConversationTitleParams = {
    conversationId: number
    userId: number
    title: string
  }

  type updatePostTagsParams = {
    postId: number
  }

  type uploadPhotoParams = {
    tripId: number
    photoUrl: string
  }

  type uploadPhotosParams = {
    tripId: number
  }

  type uploadPictureParams = {
    pictureUploadRequest: PictureUploadRequest
  }

  type User = {
    id?: number
    userAccount?: string
    userPassword?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    editTime?: string
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type UserAddRequest = {
    userName?: string
    userAccount?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
  }

  type UserLoginRequest = {
    userPassword?: string
    userAccount?: string
  }

  type UserQueryRequest = {
    current?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    userName?: string
    userAccount?: string
    userProfile?: string
    userRole?: string
  }

  type UserRegisterRequest = {
    userAccount?: string
    userPassword?: string
    checkPassword?: string
  }

  type UserUpdateRequest = {
    id?: number
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
  }

  type UserUpdateMyRequest = {
    id?: number
    userName?: string
    userAvatar?: string
    userProfile?: string
  }

  type UserVO = {
    id?: number
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    createTime?: string
  }
}
