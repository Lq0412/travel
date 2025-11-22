<template>
  <div class="scenic-manage-page">
    <div class="page-header">
      <h1>景点管理</h1>
      <a-button type="primary" @click="showCreateModal = true" size="large">
        <template #icon>➕</template>
        添加景点
      </a-button>
    </div>

    <!-- 搜索和筛选 -->
    <div class="search-section">
      <a-input-search
        v-model:value="searchKeyword"
        placeholder="搜索景点名称、地址"
        style="width: 300px"
        @search="handleSearch"
        allow-clear
      />
      <a-select
        v-model:value="statusFilter"
        placeholder="筛选状态"
        style="width: 150px; margin-left: 12px"
        @change="handleSearch"
        allow-clear
      >
        <a-select-option :value="1">启用</a-select-option>
        <a-select-option :value="0">禁用</a-select-option>
      </a-select>
    </div>

    <!-- 景点列表 -->
    <a-table
      :columns="columns"
      :data-source="scenicList"
      :loading="loading"
      :pagination="pagination"
      @change="handleTableChange"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'coverUrl'">
          <a-image
            :src="record.coverUrl || 'https://via.placeholder.com/100x60'"
            :width="100"
            :height="60"
            style="object-fit: cover; border-radius: 4px"
          />
        </template>

        <template v-else-if="column.key === 'status'">
          <a-tag :color="record.status === 1 ? 'green' : 'red'">
            {{ record.status === 1 ? '启用' : '禁用' }}
          </a-tag>
        </template>

        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="link" @click="handleEdit(record)">编辑</a-button>
            <a-button type="link" @click="handleViewDetails(record)">详情</a-button>
            <a-popconfirm
              :title="`确定${record.status === 1 ? '禁用' : '启用'}该景点吗？`"
              @confirm="handleToggleStatus(record)"
            >
              <a-button type="link" :danger="record.status === 1">
                {{ record.status === 1 ? '禁用' : '启用' }}
              </a-button>
            </a-popconfirm>
            <a-popconfirm
              title="确定删除该景点吗？"
              @confirm="handleDelete(record.id)"
            >
              <a-button type="link" danger>删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 创建/编辑景点模态框 -->
    <a-modal
      v-model:open="showCreateModal"
      :title="editingScenic ? '编辑景点' : '添加景点'"
      :width="800"
      @ok="handleSubmit"
      @cancel="handleCancel"
    >
      <a-form
        :model="formData"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="景点名称" required>
          <a-input v-model:value="formData.name" placeholder="请输入景点名称" />
        </a-form-item>

        <a-form-item label="景点地址" required>
          <a-input v-model:value="formData.address" placeholder="请输入景点地址" />
        </a-form-item>

        <a-form-item label="景点描述">
          <a-textarea
            v-model:value="formData.description"
            placeholder="请输入景点描述"
            :rows="4"
            :maxlength="500"
            show-count
          />
        </a-form-item>

        <a-form-item label="封面图片URL">
          <a-input v-model:value="formData.coverUrl" placeholder="请输入图片URL" />
          <div v-if="formData.coverUrl" style="margin-top: 8px">
            <a-image :src="formData.coverUrl" :width="200" />
          </div>
        </a-form-item>

        <a-form-item label="票价（元）">
          <a-input-number
            v-model:value="formData.price"
            :min="0"
            :precision="2"
            style="width: 100%"
            placeholder="请输入票价"
          />
        </a-form-item>

        <a-form-item label="开放时间">
          <a-input v-model:value="formData.openingHours" placeholder="例如：08:00-18:00" />
        </a-form-item>

        <a-form-item label="联系电话">
          <a-input v-model:value="formData.contactPhone" placeholder="请输入联系电话" />
        </a-form-item>

        <a-form-item label="推荐等级">
          <a-rate v-model:value="formData.rating" :count="5" allow-half />
        </a-form-item>

        <a-form-item label="状态">
          <a-radio-group v-model:value="formData.status">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 景点详情模态框 -->
    <a-modal
      v-model:open="showDetailModal"
      title="景点详情"
      :width="800"
      :footer="null"
    >
      <a-descriptions v-if="viewingScenic" :column="2" bordered>
        <a-descriptions-item label="景点ID">{{ viewingScenic.id }}</a-descriptions-item>
        <a-descriptions-item label="景点名称">{{ viewingScenic.name }}</a-descriptions-item>
        <a-descriptions-item label="地址" :span="2">{{ viewingScenic.address }}</a-descriptions-item>
        <a-descriptions-item label="描述" :span="2">{{ viewingScenic.description || '暂无' }}</a-descriptions-item>
        <a-descriptions-item label="票价">{{ viewingScenic.price ? `¥${viewingScenic.price}` : '免费' }}</a-descriptions-item>
        <a-descriptions-item label="评分">
          <a-rate :value="viewingScenic.rating" disabled allow-half />
        </a-descriptions-item>
        <a-descriptions-item label="开放时间" :span="2">{{ viewingScenic.openingHours || '暂无' }}</a-descriptions-item>
        <a-descriptions-item label="联系电话">{{ viewingScenic.contactPhone || '暂无' }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="viewingScenic.status === 1 ? 'green' : 'red'">
            {{ viewingScenic.status === 1 ? '启用' : '禁用' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="创建时间" :span="2">{{ viewingScenic.createTime }}</a-descriptions-item>
        <a-descriptions-item label="封面图片" :span="2">
          <a-image v-if="viewingScenic.coverUrl" :src="viewingScenic.coverUrl" :width="300" />
          <span v-else>暂无图片</span>
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { listSpots, addSpot, updateSpot, deleteSpot } from '@/api/scenicController'

const loading = ref(false)
const scenicList = ref<API.ScenicSpot[]>([])
const searchKeyword = ref('')
const statusFilter = ref<number>()

const showCreateModal = ref(false)
const showDetailModal = ref(false)
const editingScenic = ref<API.ScenicSpot | null>(null)
const viewingScenic = ref<API.ScenicSpot | null>(null)

const formData = reactive<API.ScenicSpotAddRequest>({
  name: '',
  address: '',
  description: '',
  coverUrl: '',
  price: 0,
  openingHours: '',
  contactPhone: '',
  rating: 0,
  status: 1,
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (total: number) => `共 ${total} 条`,
})

const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    key: 'id',
    width: 80,
  },
  {
    title: '封面',
    dataIndex: 'coverUrl',
    key: 'coverUrl',
    width: 120,
  },
  {
    title: '景点名称',
    dataIndex: 'name',
    key: 'name',
    width: 150,
  },
  {
    title: '地址',
    dataIndex: 'address',
    key: 'address',
    width: 200,
  },
  {
    title: '票价',
    dataIndex: 'price',
    key: 'price',
    width: 100,
    customRender: ({ text }: any) => text ? `¥${text}` : '免费',
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 100,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
    width: 180,
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right',
    width: 250,
  },
]

// 加载景点列表
const loadScenics = async () => {
  loading.value = true
  try {
    const res = await listSpots()
    if (res.data.code === 0 && res.data.data) {
      let list = res.data.data
      
      // 根据搜索关键词过滤
      if (searchKeyword.value) {
        list = list.filter((item: API.ScenicSpot) => 
          item.name?.includes(searchKeyword.value) || 
          item.address?.includes(searchKeyword.value)
        )
      }
      
      // 根据状态过滤
      if (statusFilter.value !== undefined) {
        list = list.filter((item: API.ScenicSpot) => item.status === statusFilter.value)
      }
      
      scenicList.value = list
      pagination.total = list.length
    }
  } catch (error) {
    message.error('加载景点列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  loadScenics()
}

// 表格变化
const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
}

// 编辑
const handleEdit = (record: API.ScenicSpot) => {
  editingScenic.value = record
  Object.assign(formData, {
    name: record.name,
    address: record.address,
    description: record.description,
    coverUrl: record.coverUrl,
    price: record.price,
    openingHours: record.openingHours,
    contactPhone: record.contactPhone,
    rating: record.rating,
    status: record.status,
  })
  showCreateModal.value = true
}

// 查看详情
const handleViewDetails = (record: API.ScenicSpot) => {
  viewingScenic.value = record
  showDetailModal.value = true
}

// 切换状态
const handleToggleStatus = async (record: API.ScenicSpot) => {
  try {
    const newStatus = record.status === 1 ? 0 : 1
    await updateSpot({
      id: record.id,
      status: newStatus,
    })
    message.success('状态更新成功')
    loadScenics()
  } catch (error) {
    message.error('状态更新失败')
    console.error(error)
  }
}

// 删除
const handleDelete = async (id: number) => {
  try {
    await deleteSpot({ id })
    message.success('删除成功')
    loadScenics()
  } catch (error) {
    message.error('删除失败')
    console.error(error)
  }
}

// 提交表单
const handleSubmit = async () => {
  try {
    if (!formData.name || !formData.address) {
      message.warning('请填写必填项')
      return
    }

    if (editingScenic.value) {
      // 编辑
      await updateSpot({
        id: editingScenic.value.id,
        ...formData,
      })
      message.success('更新成功')
    } else {
      // 新增
      await addSpot(formData)
      message.success('添加成功')
    }
    
    showCreateModal.value = false
    loadScenics()
  } catch (error) {
    message.error('操作失败')
    console.error(error)
  }
}

// 取消
const handleCancel = () => {
  showCreateModal.value = false
  editingScenic.value = null
  Object.assign(formData, {
    name: '',
    address: '',
    description: '',
    coverUrl: '',
    price: 0,
    openingHours: '',
    contactPhone: '',
    rating: 0,
    status: 1,
  })
}

onMounted(() => {
  loadScenics()
})
</script>

<style scoped lang="scss">
.scenic-manage-page {
  padding: 24px;
  background: var(--color-bg);
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 20px;
  background: var(--color-bg-secondary);
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);

  h1 {
    margin: 0;
    font-size: 24px;
    font-weight: 600;
    color: var(--color-text);
  }
}

.search-section {
  margin-bottom: 16px;
  padding: 16px;
  background: var(--color-bg-secondary);
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.ant-table {
  background: var(--color-bg-secondary);
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
</style>

