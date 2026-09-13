<template>
  <div class="profile-page page-bg">
    <el-card class="profile-header-card">
      <div class="profile-header">
        <div class="header-avatar">
          <template v-if="isSelf">
            <el-upload action="/api/file/upload" :headers="uploadHeaders" :show-file-list="false" :on-success="handleAvatarSuccess" :before-upload="beforeAvatarUpload" accept="image/jpeg,image/png,image/gif,image/webp">
              <el-avatar :size="90" :src="userStore.userInfo?.avatar" class="avatar-clickable" />
              <div class="avatar-overlay">更换头像</div>
            </el-upload>
          </template>
          <el-avatar v-else :size="90" :src="profileUser?.avatar || '/default-avatar.svg'" />
        </div>
        <div class="header-info">
          <div class="header-name-row">
            <h3 class="profile-name">{{ isSelf ? (userStore.userInfo?.nickname || userStore.userInfo?.username) : (profileUser?.nickname || profileUser?.username) }}</h3>
            <template v-if="isSelf">
              <span v-if="userStore.userInfo?.realVerified === 1" class="verify-badge verify-badge--ok">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg>
                已认证
              </span>
              <span v-else class="verify-badge verify-badge--no">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 15v-2h2v2h-2zm0-4V7h2v6h-2z"/></svg>
                未认证
              </span>
            </template>
            <span v-else-if="profileUser?.realVerified === 1" class="verify-badge verify-badge--ok">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg>
              已认证
            </span>
          </div>
          <div class="profile-stats">
            <template v-if="isSelf">
              <span>{{ selfFollowCounts.following }} 关注</span>
              <span class="stat-dot">·</span>
              <span>{{ selfFollowCounts.followers }} 粉丝</span>
              <template v-if="selfAvgRating > 0">
                <span class="stat-dot">·</span>
                <el-rate :model-value="selfAvgRating" disabled size="small" class="rate-inline" />
              </template>
            </template>
            <template v-else>
              <span>{{ followCounts.following }} 关注</span>
              <span class="stat-dot">·</span>
              <span>{{ followCounts.followers }} 粉丝</span>
              <template v-if="avgRating > 0">
                <span class="stat-dot">·</span>
                <el-rate :model-value="avgRating" disabled size="small" class="rate-inline" />
              </template>
              <template v-else>
                <span class="stat-dot">·</span>
                <span class="text-muted-sm">暂无评价</span>
              </template>
            </template>
          </div>
          <div class="profile-info-list">
            <template v-if="isSelf">
              <div class="info-item"><span class="info-label">用户名</span><span class="info-value">{{ userStore.userInfo?.username }}</span></div>
              <div class="info-item"><span class="info-label">手机号</span><span class="info-value">{{ userStore.userInfo?.phone || '未绑定' }}</span></div>
              <div class="info-item"><span class="info-label">邮箱</span><span class="info-value">{{ userStore.userInfo?.email || '未绑定' }}</span></div>
              <div class="info-item"><span class="info-label">学号</span><span class="info-value">{{ userStore.userInfo?.studentId || '未填写' }}</span></div>
              <div class="info-item"><span class="info-label">注册时间</span><span class="info-value">{{ formatDateTime(userStore.userInfo?.createTime) }}</span></div>
            </template>
            <template v-else>
              <div class="info-item"><span class="info-label">用户名</span><span class="info-value">{{ profileUser?.username }}</span></div>
              <div class="info-item"><span class="info-label">注册时间</span><span class="info-value">{{ formatDateTime(profileUser?.createTime) }}</span></div>
            </template>
          </div>
        </div>
        <div class="header-actions">
          <el-button v-if="!isSelf && userStore.token" :type="isFollowed ? 'warning' : 'primary'" @click="handleToggleFollow" :loading="followLoading" round>
            {{ isFollowed ? '已关注' : '关注' }}
          </el-button>
        </div>
      </div>
    </el-card>

    <template v-if="isSelf">
      <el-card class="profile-content-card">
        <el-tabs v-model="activeTab" class="profile-tabs">
          <el-tab-pane label="我的统计" name="stats">
            <div class="stats-grid">
              <div class="stat-card" @click="$router.push('/my-goods')">
                <div class="stat-icon stat-icon--sky"><svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor"><path d="M20 7h-4V5c0-1.1-.9-2-2-2h-4c-1.1 0-2 .9-2 2v2H4c-1.1 0-2 .9-2 2v11c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V9c0-1.1-.9-2-2-2zm-6-2v2h-4V5h4z"/></svg></div>
                <div class="stat-value">{{ stats.publishedGoods }}</div>
                <div class="stat-label">发布商品</div>
              </div>
              <div class="stat-card" @click="$router.push('/my-goods?status=ONLINE')">
                <div class="stat-icon stat-icon--green"><svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor"><path d="M7 18c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zM1 2v2h2l3.6 7.59-1.35 2.45c-.16.28-.25.61-.25.96 0 1.1.9 2 2 2h12v-2H7.42c-.14 0-.25-.11-.25-.25l.03-.12.9-1.63h7.45c.75 0 1.41-.41 1.75-1.03l3.58-6.49c.08-.14.12-.31.12-.48 0-.55-.45-1-1-1H5.21l-.94-2H1zm16 16c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"/></svg></div>
                <div class="stat-value">{{ stats.onlineGoods }}</div>
                <div class="stat-label">在售商品</div>
              </div>
              <div class="stat-card" @click="$router.push('/order?tab=buyer')">
                <div class="stat-icon stat-icon--amber"><svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor"><path d="M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/></svg></div>
                <div class="stat-value">{{ stats.buyerOrders }}</div>
                <div class="stat-label">我的订单</div>
              </div>
              <div class="stat-card" @click="$router.push('/order?tab=seller')">
                <div class="stat-icon stat-icon--teal"><svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor"><path d="M11.8 10.9c-2.27-.59-3-1.2-3-2.15 0-1.09 1.01-1.85 2.7-1.85 1.78 0 2.44.85 2.5 2.1h2.21c-.07-1.72-1.12-3.3-3.21-3.81V3h-3v2.16c-1.94.42-3.5 1.68-3.5 3.61 0 2.31 1.91 3.46 4.7 4.13 2.5.6 3 1.48 3 2.41 0 .69-.49 1.79-2.7 1.79-2.06 0-2.87-.92-2.98-2.1h-2.2c.12 2.19 1.76 3.42 3.68 3.83V21h3v-2.16c1.94-.42 3.5-1.45 3.5-3.23 0-2.27-1.64-3.46-4.7-4.12z"/></svg></div>
                <div class="stat-value">{{ stats.sellerOrders }}</div>
                <div class="stat-label">出售商品</div>
              </div>
              <div class="stat-card" @click="$router.push('/order?tab=buyer&status=FINISHED')">
                <div class="stat-icon stat-icon--cyan"><svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor"><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg></div>
                <div class="stat-value">{{ stats.finishedOrders }}</div>
                <div class="stat-label">完成购物</div>
              </div>
              <div class="stat-card" @click="$router.push('/address')">
                <div class="stat-icon stat-icon--pink"><svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/></svg></div>
                <div class="stat-label">收货地址</div>
              </div>
              <div class="stat-card">
                <div class="stat-icon stat-icon--red"><svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor"><path d="M20 4H4c-1.11 0-1.99.89-1.99 2L2 18c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V6c0-1.11-.89-2-2-2zm0 14H4v-6h16v6zm0-10H4V6h16v2z"/></svg></div>
                <div class="stat-value">¥{{ stats.totalSpent || 0 }}</div>
                <div class="stat-label">累计消费</div>
              </div>
              <div class="stat-card">
                <div class="stat-icon stat-icon--emerald"><svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor"><path d="M11 1v2h2V1h-2zm0 18v2h2v-2h-2zM3 11H1v2h2v-2zm18 0h-2v2h2v-2zM5.6 4.2L4.2 5.6l1.4 1.4 1.4-1.4-1.4-1.4zm12.8 12.8l-1.4 1.4 1.4 1.4 1.4-1.4-1.4-1.4zM18.4 4.2l-1.4 1.4 1.4 1.4 1.4-1.4-1.4-1.4zM5.6 16.8l-1.4-1.4-1.4 1.4 1.4 1.4 1.4-1.4zM12 7c-2.76 0-5 2.24-5 5s2.24 5 5 5 5-2.24 5-5-2.24-5-5-5z"/></svg></div>
                <div class="stat-value">¥{{ stats.totalEarned || 0 }}</div>
                <div class="stat-label">累计收入</div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="编辑资料" name="info">
            <div class="form-section">
              <div class="form-section-title">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
                基本信息
              </div>
              <el-form :model="infoForm" :rules="infoRules" ref="infoFormRef" label-width="80px" class="profile-form">
                <el-form-item label="昵称" prop="nickname">
                  <el-input v-model="infoForm.nickname" placeholder="请输入昵称" maxlength="20" show-word-limit clearable>
                    <template #prefix><svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" style="color:var(--text-muted)"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg></template>
                  </el-input>
                </el-form-item>
              </el-form>
            </div>
            <div class="form-section">
              <div class="form-section-title">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm-9 4h2v2h-2V8zm0 4h2v2h-2v-2zm-4-4h2v2H7V8zm0 4h2v2H7v-2zm-2 4h2v2H5v-2zm6 0h2v2h-2v-2zm4 0h2v2h-2v-2zm0-4h2v2h-2v-2zm0-4h2v2h-2V8z"/></svg>
                联系方式
              </div>
              <el-form :model="infoForm" :rules="infoRules" ref="infoFormRef2" label-width="80px" class="profile-form">
                <el-form-item label="手机号" prop="phone">
                  <el-input v-model="infoForm.phone" placeholder="请输入手机号" clearable>
                    <template #prefix><svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" style="color:var(--text-muted)"><path d="M17 1.01L7 1c-1.1 0-2 .9-2 2v18c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2V3c0-1.1-.9-1.99-2-1.99zM17 19H7V5h10v14z"/></svg></template>
                  </el-input>
                </el-form-item>
                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="infoForm.email" placeholder="请输入邮箱" clearable>
                    <template #prefix><svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" style="color:var(--text-muted)"><path d="M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/></svg></template>
                  </el-input>
                </el-form-item>
                <el-form-item>
                  <div class="form-actions">
                    <el-button type="primary" @click="handleUpdateInfo" :loading="infoLoading" round>保存修改</el-button>
                    <el-button @click="resetInfoForm" round>重置</el-button>
                  </div>
                </el-form-item>
              </el-form>
            </div>
          </el-tab-pane>

          <el-tab-pane label="修改密码" name="password">
            <div class="form-section">
              <div class="form-section-title">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
                安全设置
              </div>
              <el-form :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-width="100px" class="profile-form">
                <el-form-item label="当前密码" prop="oldPassword">
                  <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入当前密码" clearable />
                </el-form-item>
                <el-form-item label="新密码" prop="newPassword">
                  <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="8-20位密码" clearable />
                </el-form-item>
                <el-form-item label="确认新密码" prop="confirmPassword">
                  <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" clearable />
                </el-form-item>
                <el-form-item>
                  <div class="form-actions">
                    <el-button type="primary" @click="handleUpdatePwd" :loading="pwdLoading" round>修改密码</el-button>
                    <el-button @click="resetPwdForm" round>重置</el-button>
                  </div>
                </el-form-item>
              </el-form>
            </div>
          </el-tab-pane>

          <el-tab-pane label="实名认证" name="verify">
            <div v-if="userStore.userInfo?.realVerified === 1" class="verify-success-card">
              <div class="verify-success-icon">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg>
              </div>
              <h3 class="verify-success-title">实名认证已完成</h3>
              <p class="verify-success-desc">您的账号已通过实名认证</p>
              <div class="verify-info-grid">
                <div class="verify-info-item">
                  <span class="verify-info-label">真实姓名</span>
                  <span class="verify-info-value">{{ maskName(userStore.userInfo?.realName) }}</span>
                </div>
                <div class="verify-info-item">
                  <span class="verify-info-label">学号</span>
                  <span class="verify-info-value">{{ userStore.userInfo?.studentId }}</span>
                </div>
                <div class="verify-info-item">
                  <span class="verify-info-label">认证状态</span>
                  <span class="verify-info-value verify-info-status">已认证</span>
                </div>
              </div>
            </div>
            <div v-else class="verify-flow">
              <div class="verify-notice">
                <div class="verify-notice-icon">
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"/></svg>
                </div>
                <div class="verify-notice-content">
                  <h4>为什么要实名认证？</h4>
                  <ul>
                    <li>保障校园交易安全，建立可信交易环境</li>
                    <li>认证后可发布商品、参与买卖交易</li>
                    <li>认证信息仅用于身份核实，严格保密</li>
                  </ul>
                </div>
              </div>
              <el-steps :active="verifyStep" align-center class="verify-steps">
                <el-step title="填写信息" description="真实姓名与学号" />
                <el-step title="确认提交" description="核对信息并认证" />
                <el-step title="认证完成" description="获得交易权限" />
              </el-steps>
              <div v-if="verifyStep === 0" class="verify-step-content">
                <el-form :model="verifyForm" :rules="verifyRules" ref="verifyFormRef" label-width="90px" class="profile-form">
                  <el-form-item label="真实姓名" prop="realName">
                    <el-input v-model="verifyForm.realName" placeholder="请输入真实姓名" clearable>
                      <template #prefix><svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" style="color:var(--text-muted)"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg></template>
                    </el-input>
                  </el-form-item>
                  <el-form-item label="学号" prop="studentId">
                    <el-input v-model="verifyForm.studentId" placeholder="请输入学号" clearable>
                      <template #prefix><svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" style="color:var(--text-muted)"><path d="M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2zm2 4v2h10V7H7zm0 4v2h10v-2H7zm0 4v2h7v-2H7z"/></svg></template>
                    </el-input>
                  </el-form-item>
                  <el-form-item>
                    <div class="form-actions">
                      <el-button type="primary" @click="goVerifyStep2" round>下一步</el-button>
                    </div>
                  </el-form-item>
                </el-form>
              </div>
              <div v-if="verifyStep === 1" class="verify-step-content">
                <div class="verify-confirm-card">
                  <h4 class="verify-confirm-title">请确认以下信息</h4>
                  <div class="verify-confirm-list">
                    <div class="verify-confirm-item">
                      <span class="verify-confirm-label">真实姓名</span>
                      <span class="verify-confirm-value">{{ verifyForm.realName }}</span>
                    </div>
                    <div class="verify-confirm-item">
                      <span class="verify-confirm-label">学号</span>
                      <span class="verify-confirm-value">{{ verifyForm.studentId }}</span>
                    </div>
                  </div>
                  <div class="verify-confirm-warn">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z"/></svg>
                    提交后认证信息将不可更改，请仔细核对
                  </div>
                  <div class="form-actions">
                    <el-button @click="verifyStep = 0" round>返回修改</el-button>
                    <el-button type="primary" @click="handleVerify" :loading="verifyLoading" round>确认提交</el-button>
                  </div>
                </div>
              </div>
              <div v-if="verifyStep === 2" class="verify-step-content">
                <div class="verify-success-inline">
                  <svg width="40" height="40" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg>
                  <h3>认证成功</h3>
                  <p>您已获得校园交易权限</p>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="收款管理" name="payment">
            <div class="payment-tab" v-loading="payLoading">
              <div class="payment-tab-header">
                <el-button type="primary" size="small" round @click="openPayDialog()">添加收款账号</el-button>
              </div>
              <EmptyState v-if="payConfigs.length === 0 && !payLoading" icon="💳" title="暂无收款配置" description="添加支付宝收款账号，发布商品时自动关联" />
              <div v-else class="pay-config-list">
                <div v-for="config in payConfigs" :key="config.id" class="pay-config-card" :class="{ 'is-default': config.isDefault === 1 }">
                  <div class="pay-config-info">
                    <el-tag :type="config.isDefault === 1 ? 'primary' : 'info'" size="small">{{ config.isDefault === 1 ? '默认' : '支付宝' }}</el-tag>
                    <div class="pay-config-detail">
                      <div class="pay-config-account">{{ config.alipayAccount }}</div>
                      <div class="pay-config-name">{{ config.realName }}</div>
                    </div>
                  </div>
                  <div class="pay-config-actions">
                    <el-button v-if="config.isDefault !== 1" size="small" @click="handlePaySetDefault(config.id)">设为默认</el-button>
                    <el-button size="small" @click="openPayDialog(config)">编辑</el-button>
                    <el-button size="small" type="danger" text @click="handlePayDelete(config.id)">删除</el-button>
                  </div>
                </div>
              </div>
              <el-dialog v-model="payDialogVisible" :title="editingPayConfig ? '编辑收款账号' : '添加收款账号'" width="440px" append-to-body>
                <el-form :model="payForm" label-width="100px">
                  <el-form-item label="支付宝账号" required><el-input v-model="payForm.alipayAccount" placeholder="请输入支付宝账号" /></el-form-item>
                  <el-form-item label="真实姓名" required><el-input v-model="payForm.realName" placeholder="请输入真实姓名" /></el-form-item>
                  <el-form-item label="设为默认"><el-switch v-model="payForm.isDefault" :active-value="1" :inactive-value="0" /></el-form-item>
                </el-form>
                <template #footer>
                  <el-button @click="payDialogVisible = false">取消</el-button>
                  <el-button type="primary" :loading="paySubmitting" @click="handlePaySubmit">确定</el-button>
                </template>
              </el-dialog>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </template>
    <template v-else>
      <el-card class="other-goods-card">
        <h3 class="section-title">在售商品</h3>
        <el-row :gutter="16">
          <el-col :xs="12" :sm="8" :md="6" v-for="item in goodsList" :key="item.id">
            <GoodsCard :goods="item" />
          </el-col>
        </el-row>
        <EmptyState v-if="goodsList.length === 0 && !goodsLoading" icon="🏪" title="暂无在售商品" description="该用户暂无在售商品" />
        <el-pagination v-if="goodsTotal > goodsPageSize" v-model:current-page="goodsPageNum" :page-size="goodsPageSize" :total="goodsTotal" layout="prev, pager, next" @current-change="loadOtherUserGoods" class="goods-pagination" />
      </el-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { updateUserInfo, updatePassword, realNameVerify, uploadAvatar, getUserPublicInfo, getUserStats, type UserStatsVO } from '@/api/user'
import { getGoodsList } from '@/api/goods'
import { getFollowCounts, toggleFollow, isFollowing } from '@/api/follow'
import { getAverageRating } from '@/api/rating'
import { getPaymentConfigs, createPaymentConfig, updatePaymentConfig, deletePaymentConfig, setDefaultPaymentConfig, type PaymentConfigVO } from '@/api/paymentConfig'
import { formatDateTime } from '@/utils/labels'
import GoodsCard from '@/components/GoodsCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import type { FormInstance } from 'element-plus'
import type { UserVO } from '@/api/user'
import type { GoodsVO } from '@/api/goods'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const activeTab = ref('stats')
const isSelf = computed(() => !route.params.id || Number(route.params.id) === userStore.userInfo?.id)
const profileUser = ref<UserVO | null>(null)
const followCounts = ref<{ following: number; followers: number }>({ following: 0, followers: 0 })
const avgRating = ref(0)
const isFollowed = ref(false)
const followLoading = ref(false)
const goodsList = ref<GoodsVO[]>([])
const goodsLoading = ref(false)
const goodsPageNum = ref(1)
const goodsPageSize = ref(12)
const goodsTotal = ref(0)
const stats = ref<UserStatsVO>({ publishedGoods: 0, onlineGoods: 0, buyerOrders: 0, sellerOrders: 0, finishedOrders: 0, totalSpent: 0, totalEarned: 0 })
const selfFollowCounts = ref<{ following: number; followers: number }>({ following: 0, followers: 0 })
const selfAvgRating = ref(0)
const uploadHeaders = computed(() => ({ Authorization: userStore.token ? `Bearer ${userStore.token}` : '' }))
const beforeAvatarUpload = (file: File) => {
  const isImage = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type)
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isImage) ElMessage.error('仅支持 jpg/png/gif/webp 格式')
  if (!isLt10M) ElMessage.error('图片大小不能超过 10MB')
  return isImage && isLt10M
}
const handleAvatarSuccess = async (response: { code: number; data: string; message?: string }) => {
  if (response.code === 200 && response.data) {
    await uploadAvatar(response.data)
    await userStore.fetchUserInfo()
    ElMessage.success('头像更新成功')
  } else { ElMessage.error(response.message || '上传失败') }
}
const infoFormRef = ref<FormInstance>()
const infoFormRef2 = ref<FormInstance>()
const pwdFormRef = ref<FormInstance>()
const verifyFormRef = ref<FormInstance>()
const infoLoading = ref(false)
const pwdLoading = ref(false)
const verifyLoading = ref(false)
const verifyStep = ref(0)
const infoForm = reactive({ nickname: '', phone: '', email: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const verifyForm = reactive({ realName: '', studentId: '' })
const phoneValidator = (_rule: unknown, value: string, callback: (err?: Error) => void) => {
  if (value && !/^1[3-9]\d{9}$/.test(value)) callback(new Error('手机号格式不正确'))
  else callback()
}
const emailValidator = (_rule: unknown, value: string, callback: (err?: Error) => void) => {
  if (value && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) callback(new Error('邮箱格式不正确'))
  else callback()
}
const confirmPwdValidator = (_rule: unknown, value: string, callback: (err?: Error) => void) => {
  if (value !== pwdForm.newPassword) callback(new Error('两次密码不一致'))
  else callback()
}
const infoRules = {
  nickname: [{ max: 20, message: '昵称不能超过20个字符', trigger: 'blur' }],
  phone: [{ validator: phoneValidator, trigger: 'blur' }],
  email: [{ validator: emailValidator, trigger: 'blur' }]
}
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 8, max: 20, message: '密码长度8-20位', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请确认新密码', trigger: 'blur' }, { validator: confirmPwdValidator, trigger: 'blur' }]
}
const verifyRules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  studentId: [{ required: true, message: '请输入学号', trigger: 'blur' }]
}
const maskName = (name?: string) => {
  if (!name || name.length <= 1) return name
  return name.charAt(0) + '*'.repeat(name.length - 1)
}
const loadOtherUser = async () => {
  const userId = Number(route.params.id)
  if (!userId) return
  try { profileUser.value = await getUserPublicInfo(userId) } catch (e) {
    console.error(e); ElMessage.error('用户不存在'); router.replace('/'); return
  }
  try { followCounts.value = await getFollowCounts(userId) } catch (e) { console.error(e) }
  try { avgRating.value = await getAverageRating(userId) } catch (e) { console.error(e) }
  if (userStore.token && !isSelf.value) { try { isFollowed.value = await isFollowing(userId) } catch (e) { console.error(e) } }
  goodsPageNum.value = 1; loadOtherUserGoods()
}
const loadOtherUserGoods = async () => {
  const userId = Number(route.params.id)
  if (!userId) return
  goodsLoading.value = true
  try {
    const res = await getGoodsList({ pageNum: goodsPageNum.value, pageSize: goodsPageSize.value, userId, status: 'ONLINE' })
    goodsList.value = res.list || []; goodsTotal.value = res.total || 0
  } catch (e) { console.error(e) } finally { goodsLoading.value = false }
}
const handleToggleFollow = async () => {
  if (!userStore.token) { ElMessage.warning('请先登录'); return }
  followLoading.value = true
  try {
    await toggleFollow(Number(route.params.id))
    isFollowed.value = !isFollowed.value
    followCounts.value.followers += isFollowed.value ? 1 : -1
    ElMessage.success(isFollowed.value ? '已关注' : '已取消关注')
  } finally { followLoading.value = false }
}
const handleUpdateInfo = async () => {
  if (!infoFormRef.value) return
  await infoFormRef.value.validate()
  if (infoFormRef2.value) await infoFormRef2.value.validate()
  infoLoading.value = true
  try { await updateUserInfo(infoForm); await userStore.fetchUserInfo(); ElMessage.success('更新成功') }
  finally { infoLoading.value = false }
}
const resetInfoForm = () => {
  if (userStore.userInfo) {
    infoForm.nickname = userStore.userInfo.nickname || ''
    infoForm.phone = userStore.userInfo.phone || ''
    infoForm.email = userStore.userInfo.email || ''
  }
}
const handleUpdatePwd = async () => {
  if (!pwdFormRef.value) return
  await pwdFormRef.value.validate()
  pwdLoading.value = true
  try { await updatePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword }); ElMessage.success('密码修改成功，请重新登录'); await userStore.logout(); router.push('/login') }
  finally { pwdLoading.value = false }
}
const resetPwdForm = () => { pwdForm.oldPassword = ''; pwdForm.newPassword = ''; pwdForm.confirmPassword = '' }
const goVerifyStep2 = async () => {
  if (!verifyFormRef.value) return
  await verifyFormRef.value.validate()
  verifyStep.value = 1
}
const handleVerify = async () => {
  verifyLoading.value = true
  try { await realNameVerify(verifyForm.realName, verifyForm.studentId); await userStore.fetchUserInfo(); verifyStep.value = 2; ElMessage.success('认证成功') }
  finally { verifyLoading.value = false }
}
const payConfigs = ref<PaymentConfigVO[]>([])
const payLoading = ref(false)
const payDialogVisible = ref(false)
const paySubmitting = ref(false)
const editingPayConfig = ref<PaymentConfigVO | null>(null)
const payForm = ref({ alipayAccount: '', realName: '', isDefault: 0 })
const loadPayConfigs = async () => { payLoading.value = true; try { payConfigs.value = await getPaymentConfigs() || [] } finally { payLoading.value = false } }
const openPayDialog = (config?: PaymentConfigVO) => {
  if (config) { editingPayConfig.value = config; payForm.value = { alipayAccount: config.alipayAccount, realName: config.realName, isDefault: config.isDefault } }
  else { editingPayConfig.value = null; payForm.value = { alipayAccount: '', realName: '', isDefault: 0 } }
  payDialogVisible.value = true
}
const handlePaySubmit = async () => {
  if (!payForm.value.alipayAccount.trim()) { ElMessage.warning('请输入支付宝账号'); return }
  if (!payForm.value.realName.trim()) { ElMessage.warning('请输入真实姓名'); return }
  paySubmitting.value = true
  try {
    if (editingPayConfig.value) { await updatePaymentConfig(editingPayConfig.value.id, payForm.value.alipayAccount, payForm.value.realName, payForm.value.isDefault); ElMessage.success('修改成功') }
    else { await createPaymentConfig(payForm.value.alipayAccount, payForm.value.realName, payForm.value.isDefault); ElMessage.success('添加成功') }
    payDialogVisible.value = false; loadPayConfigs()
  } finally { paySubmitting.value = false }
}
const handlePaySetDefault = async (id: number) => { await setDefaultPaymentConfig(id); ElMessage.success('已设为默认'); loadPayConfigs() }
const handlePayDelete = async (id: number) => { await ElMessageBox.confirm('确认删除该收款配置？', '删除确认', { type: 'warning' }); await deletePaymentConfig(id); ElMessage.success('已删除'); loadPayConfigs() }
watch(() => route.params.id, () => { if (route.params.id && !isSelf.value) loadOtherUser() })
onMounted(() => {
  if (route.query.tab) {
    activeTab.value = String(route.query.tab)
    window.history.replaceState({}, '', route.path)
  }
  if (userStore.userInfo) { infoForm.nickname = userStore.userInfo.nickname || ''; infoForm.phone = userStore.userInfo.phone || ''; infoForm.email = userStore.userInfo.email || '' }
  if (isSelf.value && userStore.token) {
    getUserStats().then(s => { stats.value = s }).catch((e) => { console.error(e) })
    loadPayConfigs()
    if (userStore.userInfo?.id) {
      getFollowCounts(userStore.userInfo.id).then(c => { selfFollowCounts.value = c }).catch((e) => { console.error(e) })
      getAverageRating(userStore.userInfo.id).then(r => { selfAvgRating.value = r }).catch((e) => { console.error(e) })
    }
  }
  if (route.params.id && !isSelf.value) loadOtherUser()
})
</script>

<style scoped lang="scss">
.profile-page { padding: 20px; display: flex; flex-direction: column; gap: 16px;
  :deep(.el-card) { border-radius: var(--radius-lg); border: 1px solid var(--border); box-shadow: var(--shadow-sm); }
}
.profile-header-card { margin-bottom: 0; }
.profile-header { display: flex; align-items: flex-start; gap: 20px; }
.header-avatar { flex-shrink: 0; position: relative;
  :deep(.el-avatar) { border: 3px solid var(--bg-card); box-shadow: 0 4px 16px rgba(0,0,0,0.12); }
}
.avatar-clickable { cursor: pointer; position: relative; z-index: 1; }
.avatar-overlay { position: absolute; top: 0; left: 50%; transform: translateX(-50%); width: 90px; height: 90px; border-radius: 50%; background: rgba(0,0,0,0.5); color: #fff; font-size: 12px; display: flex; align-items: center; justify-content: center; opacity: 0; transition: opacity 0.3s; cursor: pointer; pointer-events: none; z-index: 2; }
.header-avatar:hover .avatar-overlay { opacity: 1; }
.header-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 8px; }
.header-name-row { display: flex; align-items: center; gap: 8px; }
.profile-name { font-size: 18px; font-weight: 700; letter-spacing: -0.3px; margin: 0; }
.profile-stats { color: var(--text-secondary); font-size: 13px; display: flex; align-items: center; gap: 6px; }
.stat-dot { color: var(--text-muted); }
.rate-inline { vertical-align: middle; }
.text-muted-sm { font-size: 12px; color: var(--text-muted); }
.verify-badge { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; font-weight: 600; padding: 3px 10px; border-radius: 20px; white-space: nowrap;
  &--ok { background: var(--success-light); color: var(--success); }
  &--no { background: var(--bg-hover); color: var(--text-muted); }
}
.profile-info-list { display: flex; flex-wrap: wrap; gap: 8px 24px; margin-top: 8px; }
.info-item { display: flex; align-items: center; gap: 6px; font-size: 13px; }
.info-label { color: var(--text-muted); flex-shrink: 0; }
.info-value { color: var(--text-primary); font-weight: 500; }
.header-actions { flex-shrink: 0; display: flex; flex-direction: column; gap: 8px; }
.profile-content-card { flex: 1; min-height: 0; }
.profile-tabs {
  :deep(.el-tabs__header) { margin-bottom: 20px; }
  :deep(.el-tabs__nav-wrap::after) { height: 1px; }
  :deep(.el-tabs__content) { overflow: visible; }
}
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; padding: 4px 0; margin: -4px 0;
  @media (max-width: 768px) { grid-template-columns: repeat(2, 1fr); }
}
.stat-card { background: var(--bg-card); border-radius: var(--radius-lg); padding: 20px 14px; border: 1px solid var(--border); text-align: center; cursor: pointer; transition: var(--transition-slow);
  &:hover { border-color: var(--primary-light); box-shadow: var(--shadow-lg); transform: translateY(-4px); }
}
.stat-icon { width: 44px; height: 44px; border-radius: 12px; display: flex; align-items: center; justify-content: center; color: #fff; margin: 0 auto 10px;
  &--sky { background: linear-gradient(135deg, #0EA5E9, #38BDF8); }
  &--green { background: linear-gradient(135deg, #10b981, #34d399); }
  &--amber { background: linear-gradient(135deg, #f59e0b, #fbbf24); }
  &--teal { background: linear-gradient(135deg, #14B8A6, #2DD4BF); }
  &--cyan { background: linear-gradient(135deg, #06b6d4, #22d3ee); }
  &--pink { background: linear-gradient(135deg, #0891B2, #06B6D4); }
  &--red { background: linear-gradient(135deg, #ef4444, #f87171); }
  &--emerald { background: linear-gradient(135deg, #22c55e, #4ade80); }
}
.stat-value { font-size: 28px; font-weight: 800; color: var(--primary); letter-spacing: -0.5px; }
.stat-label { font-size: 13px; color: var(--text-muted); margin-top: 4px; font-weight: 500; }
.form-section { max-width: 560px; margin-bottom: 8px; &:not(:first-child) { margin-top: 24px; } }
.form-section-title { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; color: var(--text-primary); margin-bottom: 16px; padding-bottom: 10px; border-bottom: 1px solid var(--border);
  svg { color: var(--primary); }
}
.profile-form { max-width: 500px; }
.form-actions { display: flex; gap: 12px; align-items: center; }
.verify-flow { max-width: 600px; }
.verify-notice { display: flex; gap: 12px; padding: 16px 20px; background: var(--primary-lighter); border-radius: var(--radius-md); border: 1px solid var(--primary-light); margin-bottom: 24px; }
.verify-notice-icon { color: var(--primary); flex-shrink: 0; padding-top: 2px; }
.verify-notice-content {
  h4 { font-size: 14px; font-weight: 600; margin: 0 0 8px; color: var(--text-primary); }
  ul { margin: 0; padding-left: 20px; }
  li { font-size: 13px; color: var(--text-secondary); line-height: 1.8; }
}
.verify-steps { margin-bottom: 32px; }
.verify-step-content { max-width: 500px; margin: 0 auto; }
.verify-confirm-card { background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius-md); padding: 24px; }
.verify-confirm-title { font-size: 16px; font-weight: 600; margin: 0 0 16px; text-align: center; }
.verify-confirm-list { display: flex; flex-direction: column; gap: 12px; margin-bottom: 16px; }
.verify-confirm-item { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; background: var(--bg-hover); border-radius: var(--radius-sm); }
.verify-confirm-label { color: var(--text-muted); font-size: 14px; }
.verify-confirm-value { color: var(--text-primary); font-weight: 600; font-size: 14px; }
.verify-confirm-warn { display: flex; align-items: center; gap: 6px; color: var(--warning); font-size: 13px; margin-bottom: 20px; padding: 10px 14px; background: var(--warning-light); border-radius: var(--radius-sm);
  svg { flex-shrink: 0; }
}
.verify-success-card { max-width: 500px; margin: 0 auto; text-align: center; padding: 40px 24px; background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius-lg); }
.verify-success-icon { color: var(--success); margin-bottom: 16px; display: flex; justify-content: center; }
.verify-success-title { font-size: 20px; font-weight: 700; margin: 0 0 8px; }
.verify-success-desc { color: var(--text-secondary); font-size: 14px; margin: 0 0 24px; }
.verify-info-grid { display: flex; flex-direction: column; gap: 12px; padding: 20px; background: var(--bg-hover); border-radius: var(--radius-md); }
.verify-info-item { display: flex; justify-content: space-between; align-items: center; }
.verify-info-label { color: var(--text-muted); font-size: 14px; }
.verify-info-value { color: var(--text-primary); font-weight: 600; font-size: 14px; }
.verify-info-status { color: var(--success); }
.verify-success-inline { text-align: center; padding: 40px 0;
  svg { color: var(--success); margin-bottom: 16px; }
  h3 { font-size: 20px; font-weight: 700; margin: 0 0 8px; }
  p { color: var(--text-secondary); font-size: 14px; margin: 0; }
}
.section-title { margin: 0 0 16px; }
.goods-pagination { margin-top: 16px; justify-content: center; }
.other-goods-card { margin-bottom: 16px; }
.payment-tab { max-width: 500px; }
.payment-tab-header { margin-bottom: 16px; }
.pay-config-list { display: flex; flex-direction: column; gap: 10px; }
.pay-config-card { display: flex; justify-content: space-between; align-items: center; padding: 14px 16px; border-radius: var(--radius-md); border: 1px solid var(--border); background: var(--bg-card); transition: var(--transition);
  &.is-default { border-color: var(--primary-light); box-shadow: 0 0 0 1px var(--primary-lighter); }
}
.pay-config-info { display: flex; align-items: center; gap: 10px; }
.pay-config-detail { display: flex; flex-direction: column; gap: 2px; }
.pay-config-account { font-weight: 600; color: var(--text-primary); font-size: 14px; }
.pay-config-name { font-size: 12px; color: var(--text-muted); }
.pay-config-actions { display: flex; gap: 4px; }
@media (max-width: 768px) {
  .profile-page { padding: 16px; }
  .profile-header { gap: 12px; }
  .header-avatar { :deep(.el-avatar) { width: 64px !important; height: 64px !important; } }
  .avatar-overlay { width: 64px; height: 64px; }
  .stat-card { padding: 12px 10px; }
  .stat-icon { width: 36px; height: 36px; margin-bottom: 6px; }
  .stat-value { font-size: 22px; }
  .stat-label { font-size: 12px; }
  .verify-notice { padding: 12px 14px; }
  .verify-confirm-card { padding: 16px; }
  .verify-success-card { padding: 28px 16px; }
}
</style>
