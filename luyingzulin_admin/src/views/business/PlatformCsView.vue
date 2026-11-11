<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import LinkUser from '@/components/common/LinkUser.vue'
import NoticeBadge from '@/components/common/NoticeBadge.vue'
import {
  getPlatformCsConversationPage,
  getPlatformCsMessagePage,
  markPlatformCsConversationRead,
  sendPlatformCsReply,
} from '@/api/modules/platform-cs'
import { showError, showSuccess } from '@/utils/modal'
import { useMenuBadgeStore } from '@/stores/menuBadge'

const menuBadgeStore = useMenuBadgeStore()

const listLoading = ref(false)
const messageLoading = ref(false)
const sending = ref(false)
const conversations = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const activeConversation = ref(null)
const messages = ref([])
const draft = ref('')
const messageScrollRef = ref(null)

const filters = reactive({
  keyword: '',
  unreadOnly: false,
})

const hasActiveConversation = computed(() => Boolean(activeConversation.value?.id))

async function loadConversations() {
  listLoading.value = true
  try {
    const data = await getPlatformCsConversationPage({
      page: pageNum.value,
      limit: pageSize.value,
      keyword: filters.keyword?.trim() || undefined,
      unreadOnly: filters.unreadOnly || undefined,
    })
    conversations.value = data.list || []
    total.value = data.total || 0
    if (!conversations.value.length) {
      activeConversation.value = null
      messages.value = []
      return
    }
    const currentId = activeConversation.value?.id
    const matched = conversations.value.find((item) => item.id === currentId)
    if (!matched) {
      await selectConversation(conversations.value[0])
    } else {
      activeConversation.value = matched
    }
  } catch (error) {
    conversations.value = []
    total.value = 0
    showError(error?.msg || error?.message || '加载会话列表失败')
  } finally {
    listLoading.value = false
  }
}

async function loadMessages() {
  if (!activeConversation.value?.id) {
    messages.value = []
    return
  }
  messageLoading.value = true
  try {
    const data = await getPlatformCsMessagePage({
      conversationId: activeConversation.value.id,
      page: 1,
      limit: 200,
    })
    messages.value = data.list || []
    await nextTick()
    scrollMessagesToBottom()
  } catch (error) {
    messages.value = []
    showError(error?.msg || error?.message || '加载消息失败')
  } finally {
    messageLoading.value = false
  }
}

function scrollMessagesToBottom() {
  const el = messageScrollRef.value
  if (!el) return
  el.scrollTop = el.scrollHeight
}

async function selectConversation(item) {
  if (!item?.id) return
  activeConversation.value = item
  await loadMessages()
  if ((item.adminUnread || 0) > 0) {
    try {
      await markPlatformCsConversationRead(item.id)
      item.adminUnread = 0
      item.unreadCount = 0
      void menuBadgeStore.refresh()
    } catch (error) {
      // 标记已读失败不阻断查看
    }
  }
}

async function handleSend() {
  const content = draft.value.trim()
  if (!content || !activeConversation.value?.id || sending.value) return
  sending.value = true
  try {
    await sendPlatformCsReply({
      conversationId: activeConversation.value.id,
      content,
    })
    draft.value = ''
    showSuccess('回复已发送')
    await Promise.all([loadMessages(), loadConversations()])
    void menuBadgeStore.refresh()
  } catch (error) {
    showError(error?.msg || error?.message || '发送失败')
  } finally {
    sending.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  void loadConversations()
}

function handlePageChange(page) {
  pageNum.value = page
  void loadConversations()
}

watch(
  () => filters.unreadOnly,
  () => {
    pageNum.value = 1
    void loadConversations()
  },
)

onMounted(async () => {
  await loadConversations()
})
</script>

<template>
  <el-card shadow="never" class="page-card">
    <template #header>
      <div class="page-header">
        <strong>官方客服会话</strong>
        <span class="page-header__desc">用户从「客服中心」进入的咨询，与商户 IM 独立</span>
      </div>
    </template>

    <div class="platform-cs-layout">
      <aside class="conversation-panel">
        <div class="panel-toolbar">
          <el-input
            v-model="filters.keyword"
            clearable
            placeholder="搜索用户昵称/手机号"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-checkbox v-model="filters.unreadOnly">仅未读</el-checkbox>
          <el-button type="primary" plain @click="handleSearch">搜索</el-button>
        </div>

        <div v-loading="listLoading" class="conversation-list">
          <div
            v-for="item in conversations"
            :key="item.id"
            class="conversation-item"
            :class="{ active: activeConversation?.id === item.id, unread: (item.adminUnread || 0) > 0 }"
            @click="selectConversation(item)"
          >
            <div class="conversation-item__head">
              <span class="conversation-item__name">{{ item.userName || `用户#${item.userId}` }}</span>
              <NoticeBadge :value="item.adminUnread || 0" />
            </div>
            <div class="conversation-item__phone">
              <LinkUser v-if="item.userId" :user-id="item.userId" :phone="item.userPhone" />
              <span v-else>-</span>
            </div>
            <div class="conversation-item__preview">{{ item.lastMsgPreview || '暂无消息' }}</div>
            <div class="conversation-item__time">{{ item.lastMsgTime || '' }}</div>
          </div>
          <el-empty v-if="!listLoading && !conversations.length" description="暂无会话" />
        </div>

        <div v-if="total > pageSize" class="panel-pagination">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="total"
            :page-size="pageSize"
            :current-page="pageNum"
            @current-change="handlePageChange"
          />
        </div>
      </aside>

      <section class="chat-panel">
        <template v-if="hasActiveConversation">
          <div class="chat-panel__head">
            <div>
              <strong>{{ activeConversation.userName || `用户#${activeConversation.userId}` }}</strong>
              <div class="chat-panel__sub">
                <LinkUser
                  v-if="activeConversation.userId"
                  :user-id="activeConversation.userId"
                  :phone="activeConversation.userPhone"
                />
              </div>
            </div>
          </div>

          <div ref="messageScrollRef" v-loading="messageLoading" class="chat-messages">
            <div
              v-for="item in messages"
              :key="item.id"
              class="message-row"
              :class="item.senderRole === 'admin' ? 'mine' : 'peer'"
            >
              <div class="message-bubble">{{ item.content }}</div>
              <div class="message-time">{{ item.createTime }}</div>
            </div>
            <el-empty v-if="!messageLoading && !messages.length" description="暂无消息，等待用户发起咨询" />
          </div>

          <div class="chat-input">
            <el-input
              v-model="draft"
              type="textarea"
              :rows="3"
              maxlength="500"
              show-word-limit
              placeholder="输入回复内容..."
              @keydown.ctrl.enter="handleSend"
            />
            <div class="chat-input__actions">
              <span class="chat-input__tip">Ctrl + Enter 发送</span>
              <el-button type="primary" :loading="sending" :disabled="!draft.trim()" @click="handleSend">
                发送回复
              </el-button>
            </div>
          </div>
        </template>
        <el-empty v-else description="请选择左侧会话" />
      </section>
    </div>
  </el-card>
</template>

<style scoped>
.page-card {
  min-height: calc(100vh - 140px);
}

.page-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-header__desc {
  font-size: 13px;
  font-weight: 400;
  color: var(--el-text-color-secondary);
}

.platform-cs-layout {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 16px;
  min-height: 640px;
}

.conversation-panel,
.chat-panel {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-blank);
  min-height: 640px;
}

.conversation-panel {
  display: flex;
  flex-direction: column;
}

.panel-toolbar {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.conversation-list {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 8px;
}

.conversation-item {
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.conversation-item:hover,
.conversation-item.active {
  background: var(--el-color-primary-light-9);
}

.conversation-item.unread {
  border: 1px solid var(--el-color-primary-light-7);
}

.conversation-item__head,
.conversation-item__phone,
.conversation-item__preview,
.conversation-item__time {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.conversation-item__name {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.conversation-item__phone {
  margin-top: 6px;
  font-size: 12px;
}

.conversation-item__preview {
  margin-top: 8px;
  font-size: 13px;
  color: var(--el-text-color-regular);
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conversation-item__time {
  margin-top: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.panel-pagination {
  padding: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.chat-panel {
  display: flex;
  flex-direction: column;
}

.chat-panel__head {
  padding: 16px 20px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.chat-panel__sub {
  margin-top: 6px;
  font-size: 13px;
}

.chat-messages {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 20px;
  background: #f8faf5;
}

.message-row {
  display: flex;
  flex-direction: column;
  margin-bottom: 16px;
  max-width: 72%;
}

.message-row.mine {
  margin-left: auto;
  align-items: flex-end;
}

.message-row.peer {
  align-items: flex-start;
}

.message-bubble {
  padding: 10px 14px;
  border-radius: 12px;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
}

.message-row.mine .message-bubble {
  background: linear-gradient(135deg, #7ed321, #6bc01a);
  color: #ffffff;
}

.message-row.peer .message-bubble {
  background: #ffffff;
  border: 1px solid var(--el-border-color-lighter);
  color: var(--el-text-color-primary);
}

.message-time {
  margin-top: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.chat-input {
  padding: 16px 20px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.chat-input__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
}

.chat-input__tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

@media (max-width: 960px) {
  .platform-cs-layout {
    grid-template-columns: 1fr;
  }
}
</style>
