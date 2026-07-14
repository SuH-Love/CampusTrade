# Redis规范

## Key命名规范

| Key | 说明 |
|-----|------|
| token:user:{id} | 用户AccessToken |
| refresh:user:{id} | 刷新Token |
| captcha:{uuid} | 验证码 |
| goods:detail:{id} | 商品详情 |
| goods:list:{page} | 商品列表 |
| goods:hot | 热门商品 |
| goods:recommend | 推荐商品 |
| chat:recent:{userId} | 最近聊天 |
| notify:user:{id}:unread | 用户未读通知计数 |
| permissions:user:{id} | 用户权限列表 |
| lock:goods:{id} | 商品缓存互斥锁 |
| repeat:{userId}:{apiMd5} | 防重复提交 |
| rate_limit:{ip} | 限流 |
| blacklist:token:{token} | Token黑名单 |
| login:fail:{username} | 登录失败计数 |
| register:limit:{ip} | 注册频率限制 |
| mq:consumed:{queue}:{id} | MQ幂等消费标记 |

## TTL配置

| Key | TTL |
|-----|-----|
| token | 7200秒 |
| refresh | 604800秒 |
| captcha | 300秒 |
| goods详情 | 1800秒 |
| 商品列表 | 600秒 |
| 热门商品 | 600秒 |
| 推荐商品 | 600秒 |
| 最近聊天 | 3600秒 |
| 未读通知 | 300秒 |
| 用户权限 | 7200秒（跟随Token） |
| 商品互斥锁 | 10秒 |
| 防重复 | 5秒 |
| 限流 | 60秒 |
| 登录失败 | 1800秒 |
| 注册限流 | 3600秒 |
| MQ幂等 | 86400秒 |
| NULL值缓存 | 60秒 |
