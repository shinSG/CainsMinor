# Markdown示例文档

欢迎使用Markdown阅读器！这是一个示例文档，展示各种Markdown语法。

## 基础语法

### 文本格式

这是**粗体文本**，这是*斜体文本*，这是~~删除线文本~~。

这是`行内代码`示例。

### 链接和图片

[点击访问GitHub](https://github.com)

![示例图片](https://via.placeholder.com/300x200)

### 引用

> 这是一段引用文本。
> 
> 可以有多行。

### 列表

**无序列表:**
- 第一项
- 第二项
  - 子项 A
  - 子项 B
- 第三项

**有序列表:**
1. 步骤一
2. 步骤二
3. 步骤三

### 代码块

```kotlin
fun main() {
    println("Hello, Markdown!")
    
    val numbers = listOf(1, 2, 3, 4, 5)
    numbers.filter { it > 2 }
           .map { it * 2 }
           .forEach { println(it) }
}
```

```python
def fibonacci(n):
    if n <= 1:
        return n
    return fibonacci(n-1) + fibonacci(n-2)

print(fibonacci(10))
```

## 扩展语法

### 表格

| 姓名 | 年龄 | 城市 |
|------|------|------|
| 张三 | 25   | 北京 |
| 李四 | 30   | 上海 |
| 王五 | 28   | 广州 |

### 任务列表

- [x] 学习Markdown基础语法
- [x] 创建Android应用
- [ ] 添加更多功能
- [ ] 发布到Google Play

### 数学公式

行内公式: $E = mc^2$

块级公式:

$$
\sum_{i=1}^{n} i = \frac{n(n+1)}{2}
$$

## 实际使用场景

### 技术文档

# API文档

## 认证

使用Bearer Token进行认证:

```http
Authorization: Bearer <your_token>
```

## 获取用户列表

```http
GET /api/users
```

**响应:**
```json
{
  "users": [
    {
      "id": 1,
      "name": "张三",
      "email": "zhangsan@example.com"
    }
  ]
}
```

### 笔记示例

## 会议记录

**日期:** 2024年1月15日  
**参会人员:** 张三、李四、王五

### 议题

1. 项目进度汇报
2. 技术方案讨论
3. 下一步计划

### 决议

- 确定采用方案A
- 下周三前完成原型设计
- 每日站会改为每周三次

---

*文档创建于 2024年1月*