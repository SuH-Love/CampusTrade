# 后端开发规范

## 代码规范

### 命名规范

- 类名：大驼峰（PascalCase）
- 方法名：小驼峰（camelCase）
- 常量：全大写下划线（UPPER_SNAKE_CASE）
- 包名：全小写

### 注解使用

- Controller：@RestController, @RequestMapping, @Api
- Service：@Service, @Transactional
- Mapper：@Mapper
- 参数校验：@Valid, @NotNull, @NotBlank, @Size

### 异常处理

- 业务异常：自定义 BusinessException
- 参数异常：MethodArgumentNotValidException
- 权限异常：AccessDeniedException
- 统一异常处理器：@RestControllerAdvice

### 事务管理

- 只读操作：@Transactional(readOnly = true)
- 写操作：@Transactional(rollbackFor = Exception.class)
- 禁止在Controller层加事务

### 参数校验

- 必须使用 JSR303 校验
- DTO必须加校验注解
- 禁止在Controller手动if-else校验

## 分层规范

- Controller只做参数接收和返回
- Service处理业务逻辑
- Mapper只做数据访问
- 禁止跨层调用

## 返回规范

- 统一使用 Result<T> 包装返回
- 禁止直接返回Map或Entity
- 分页统一使用 PageResult<T>