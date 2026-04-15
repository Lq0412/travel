<template>
  <div class="mall-page">
    <div class="mall-header">
      <div class="header-left">
        <h1 class="title">自定义美食商品库</h1>
        <p class="subtitle">管理您的自定义美食商品，作为工作台推荐的独立数据源。</p>
      </div>
      <a-button type="primary" size="large" @click="handleAdd">
        <template #icon><PlusOutlined /></template>
        新增商品
      </a-button>
    </div>

    <!-- 筛选区 -->
    <div class="filter-bar">
      <a-input-search
        v-model:value="searchKeyword"
        placeholder="搜索商品名称、城市标签或描述..."
        style="max-width: 400px"
        @search="handleSearch"
      />
      <div class="filters-right">
        <a-select v-model:value="filterCity" placeholder="按目的地筛选" style="width: 150px" allowClear>
          <a-select-option v-for="c in cityOptions" :key="c" :value="c">
            {{ c }}
          </a-select-option>
        </a-select>
        <a-select v-model:value="filterStatus" placeholder="上架状态" style="width: 120px" allowClear>
          <a-select-option value="recommend">推荐中</a-select-option>
          <a-select-option value="unrecommend">已下架</a-select-option>
        </a-select>
      </div>
    </div>

    <!-- 列表区 -->
    <template v-if="filteredProducts.length > 0">
      <div class="product-grid">
        <div
          v-for="product in filteredProducts"
          :key="product.id"
          class="product-card"
          @click="goToProductDetail(product.id)"
        >
          <div class="card-cover">
            <img :src="product.cover || defaultCover" :alt="product.name" />
            <div class="action-btn top-left">
              <DeleteOutlined @click.stop="handleDelete(product)" />
            </div>
            <div class="action-btn top-right" @click.stop="handleEdit(product)">
              <EditOutlined />
            </div>
          </div>
          <div class="card-content">
            <h3 class="product-name">{{ product.name }}</h3>
            <p class="product-desc">{{ product.description }}</p>
            <div class="card-footer">
              <div class="footer-user">
                <a-avatar size="small" :src="loginUserStore.loginUser.userAvatar || 'https://unpkg.com/lucide-static@latest/icons/user-circle.svg'" />
                <span class="user-name">Publish</span>
              </div>
              <div class="footer-switches">
                <a-switch
                  :checked="product.isRecommendable"
                  :loading="savingProductId === product.id"
                  @change="handleToggleRecommend(product, $event)"
                  class="custom-switch"
                />
              </div>
            </div>
            <a-button type="primary" block class="detail-btn" @click.stop="goToProductDetail(product.id)">
              查看详情
            </a-button>
          </div>
        </div>
      </div>
    </template>
    
    <div v-else class="empty-state">
      <a-empty description="暂无匹配商品，请添加或调整筛选条件" />
    </div>

    <!-- 编辑/新增抽屉 -->
    <a-drawer
      :title="editingProduct?.id ? '编辑商品' : '新增商品'"
      placement="right"
      :open="drawerVisible"
      @close="drawerVisible = false"
      width="450"
      class="product-drawer"
    >
      <a-form layout="vertical" v-if="editingProduct">
        <a-form-item label="商品名称" required>
          <a-input v-model:value="editingProduct.name" placeholder="浪漫双人套餐..." />
        </a-form-item>
        
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="所属城市/目的地" required>
              <a-input v-model:value="editingProduct.city" placeholder="首尔" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="标签类别">
              <a-select v-model:value="editingProduct.tags" mode="tags" placeholder="输入后回车" :token-separators="[',', '，']" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="商品简介">
          <a-textarea v-model:value="editingProduct.description" :rows="3" placeholder="简短吸引人的介绍..." />
        </a-form-item>

        <a-form-item label="详细地址" required>
          <a-textarea
            v-model:value="editingProduct.address"
            :rows="2"
            placeholder="填写门店或可导航的详细地址，保存时会自动解析坐标"
          />
        </a-form-item>

        <a-form-item label="商品封面" required>
          <ProductCoverUpload v-model="editingProduct.cover" :default-cover="defaultCover" />
        </a-form-item>

        <a-row :gutter="16">
          <a-col :span="12">
            <div class="setting-item">
              <span>作为推荐项</span>
              <a-switch v-model:checked="editingProduct.isRecommendable" />
            </div>
          </a-col>
          <a-col :span="12">
            <div class="setting-item">
              <span>可一键购买</span>
              <a-switch v-model:checked="editingProduct.isPurchasable" />
            </div>
          </a-col>
        </a-row>

        <div class="drawer-footer">
          <a-button @click="drawerVisible = false">取消</a-button>
          <a-button type="primary" @click="handleSaveDrawer">保存</a-button>
        </div>
      </a-form>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import type { Product } from '@/types/product'
import { v4 as uuidv4 } from 'uuid'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import ProductCoverUpload from '@/components/mall/ProductCoverUpload.vue'
import { deleteProductFromServer, fetchMyProducts, saveProductToServer } from '@/utils/productCatalog'

const loginUserStore = useLoginUserStore()
const router = useRouter()
const products = ref<Product[]>([])
const searchKeyword = ref('')
const filterCity = ref<string | undefined>()
const filterStatus = ref<string | undefined>()
const defaultCover = 'https://images.unsplash.com/photo-1414235077428-338989a2e8c0?auto=format&fit=crop&q=80&w=800'

// 抽屉状态
const drawerVisible = ref(false)
const editingProduct = ref<Product | null>(null)
const savingProductId = ref<string | null>(null)

onMounted(() => {
  void loadProducts()
})

const cityOptions = computed(() => {
  const cities = new Set(products.value.map(p => p.city).filter(Boolean))
  return Array.from(cities)
})

const filteredProducts = computed(() => {
  return products.value.filter(p => {
    // keyword Match
    if (searchKeyword.value) {
      const kw = searchKeyword.value.toLowerCase()
      const matchName = p.name.toLowerCase().includes(kw)
      const matchDesc = p.description.toLowerCase().includes(kw)
      const matchCity = p.city && p.city.toLowerCase().includes(kw)
      const matchTags = p.tags && p.tags.some(t => t.toLowerCase().includes(kw))
      if (!matchName && !matchDesc && !matchCity && !matchTags) {
        return false
      }
    }
    // city Match
    if (filterCity.value && p.city !== filterCity.value) {
      return false
    }
    // status Match
    if (filterStatus.value) {
      if (filterStatus.value === 'recommend' && !p.isRecommendable) return false
      if (filterStatus.value === 'unrecommend' && p.isRecommendable) return false
    }

    return true
  })
})

const handleSearch = () => {
  // handled by computed
}

const goToProductDetail = (productId: string) => {
  router.push(`/products/${productId}`)
}

const loadProducts = async () => {
  try {
    products.value = await fetchMyProducts()
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '加载商品失败'
    message.error(errorMessage)
  }
}

const handleAdd = () => {
  editingProduct.value = {
    id: '',
    name: '',
    city: '',
    address: '',
    latitude: undefined,
    longitude: undefined,
    tags: [],
    description: '',
    isRecommendable: true,
    isPurchasable: true,
    cover: '',
  }
  drawerVisible.value = true
}

const handleEdit = (product: Product) => {
  editingProduct.value = JSON.parse(JSON.stringify(product)) // deep copy
  drawerVisible.value = true
}

const handleDelete = (product: Product) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除商品 "${product.name}" 吗？`,
    async onOk() {
      products.value = await deleteProductFromServer(product.id)
      message.success('已删除')
    }
  })
}

const handleToggleRecommend = async (product: Product, checked: boolean) => {
  savingProductId.value = product.id
  try {
    products.value = await saveProductToServer({
      ...product,
      isRecommendable: checked,
    })
    message.success('商品状态已更新')
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '更新商品状态失败'
    message.error(errorMessage)
  } finally {
    savingProductId.value = null
  }
}

const handleSaveDrawer = async () => {
  if (!editingProduct.value) return
  const p = editingProduct.value
  
  if (!p.name || !p.city || !p.address || !p.cover) {
    message.warning('请填写完整的必填项')
    return
  }

  try {
    const nextProduct: Product = p.id
      ? { ...p }
      : {
          ...p,
          id: uuidv4(),
        }
    products.value = await saveProductToServer(nextProduct)
    message.success('保存成功')
    drawerVisible.value = false
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '保存失败'
    message.error(errorMessage)
  }
}
</script>

<style scoped>
.mall-page {
  width: 100%;
  padding: 24px 0 0;
  box-sizing: border-box;
}

.mall-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}
.mall-header .title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: #111827;
}
.mall-header .subtitle {
  display: none; /* Hide subtitle to match image */
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.filters-right {
  display: flex;
  gap: 12px;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
}

.product-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
  transition: transform 0.2s, box-shadow 0.2s;
  display: flex;
  flex-direction: column;
}
.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 20px rgba(0,0,0,0.08);
}

.card-cover {
  position: relative;
  height: 200px;
  overflow: hidden;
}
.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}
.product-card:hover .card-cover img {
  transform: scale(1.05);
}

.status-badge {
  display: none; /* Removed based on image reference */
}

.action-btn {
  position: absolute;
  top: 12px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #6b7280;
  opacity: 0;
  transition: opacity 0.2s, color 0.2s;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}
.action-btn.top-left {
  left: 12px;
}
.action-btn.top-right {
  right: 12px;
}
.product-card:hover .action-btn {
  opacity: 1;
}
.action-btn:hover {
  color: #0284c7;
}

.card-content {
  padding: 16px;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.product-name {
  margin: 0 0 4px 0;
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.product-desc {
  margin: 0 0 16px 0;
  font-size: 12px;
  color: #9ca3af;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}
.product-tags {
  display: none; /* Hide tags to match image */
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-btn {
  margin-top: 12px;
}
.footer-user {
  display: flex;
  align-items: center;
  gap: 8px;
}
.footer-user .user-name {
  font-size: 14px;
  color: #374151;
  font-weight: 500;
}
.footer-switches {
  display: flex;
  align-items: center;
}
.custom-switch {
  background-color: #3b82f6; /* Blue switch */
}
.empty-state {
  padding: 60px 0;
  background: #fff;
  border-radius: 12px;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f9fafb;
  border-radius: 8px;
  border: 1px solid #f3f4f6;
}
.drawer-footer {
  margin-top: 40px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 768px) {
  .mall-page {
    padding-top: 12px;
  }

  .mall-header,
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }

  .filters-right {
    width: 100%;
    flex-wrap: wrap;
  }
}
</style>
