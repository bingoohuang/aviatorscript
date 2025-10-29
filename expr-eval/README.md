# 表达式求值引擎对比 - expr-eval

本项目对比了四种 Java 表达式求值引擎对复杂字符串表达式的支持情况和性能表现。


## 测试表达式

```java
substr(IDENTITY_DOC_ID, greatest(length(IDENTITY_DOC_ID) - 5, 1))
```

**测试用例：**
- 变量名：`IDENTITY_DOC_ID`
- 测试值：`123456789012345`（长度为 15）
- 预期结果：`012345`

**表达式解析：**
- `length(IDENTITY_DOC_ID)` = 15
- `greatest(15 - 5, 1)` = `greatest(10, 1)` = 10
- `substr(IDENTITY_DOC_ID, 10)` = 从第 10 个字符开始（1-based）= `012345`

## 性能对比总结

| 引擎 | 支持情况 | 平均耗时 (ns) | 相对性能 |
|------|---------|--------------|---------|
| **Aviator** | ✅ 完全支持 | **163.3** | **1.00x** ⚡ |
| **MVEL** | ✅ 完全支持 | **488.3** | **2.99x** |
| **JEXL** | ✅ 完全支持 | **529.3** | **3.24x** |
| **EvalEx** | ❌ 不支持 | N/A | N/A |

### 性能结论

1. **Aviator 性能最优**：比其他支持字符串操作的引擎快约 3 倍
2. **MVEL 和 JEXL** 性能相近，都在 480-530 ns 范围内
3. **所有支持引擎**都能正确计算结果 `012345`

---

## 支持的表达式引擎

### 1. EvalEx 2.7

**支持情况：** ❌ 不支持

EvalEx 是一个轻量级的数学表达式求值库，主要用于数学表达式计算。它不支持字符串操作函数（如 `substr`），仅支持数值表达式。

**特点：**
- 专注于数学表达式
- 支持变量、函数和常量
- 不支持字符串操作

**性能：** N/A（无法完成测试表达式）

---

### 2. Apache Commons JEXL 2.1.1

**支持情况：** ✅ 完全支持

JEXL（Java Expression Language）是 Apache Commons 提供的表达式语言，支持通过自定义函数扩展功能。

**实现方式：**
- 通过 `CustomFunctions` 类提供自定义函数
- 使用 `null` 命名空间实现无前缀函数调用
- 支持表达式预编译

**性能表现：**
- 100,000 次求值总时间：52,930,041 ns
- **平均每次求值：529.3 ns**

**优点：**
- 灵活的扩展机制
- 支持命名空间管理
- 表达式可预编译

**缺点：**
- 性能中等

---

### 3. Aviator 5.4.3

**支持情况：** ✅ 完全支持

Aviator 是一个高性能的表达式求值引擎，支持自定义函数和类型系统。

**实现方式：**
- 通过继承 `AbstractFunction` 实现自定义函数
- 支持表达式预编译
- 类型系统完善（`AviatorLong`、`AviatorString` 等）

**性能表现：**
- 100,000 次求值总时间：16,326,958 ns
- **平均每次求值：163.3 ns**

**优点：**
- ✅ **性能最优**（相比其他引擎快约 3 倍）
- 完整的类型系统
- 支持表达式预编译和优化
- 丰富的内置函数

**缺点：**
- 需要继承特定基类实现自定义函数

---

### 4. MVEL 2.0.19

**支持情况：** ✅ 完全支持

MVEL（MVFLEX Expression Language）是一个高性能的表达式语言和模板引擎。

**实现方式：**
- 通过静态方法导入实现自定义函数
- 需要使用类名作为前缀（如 `CustomFunctions.substr`）
- 需要修改表达式以包含类前缀

**性能表现：**
- 100,000 次求值总时间：48,826,791 ns
- **平均每次求值：488.3 ns**

**优点：**
- 性能良好
- 灵活的表达式语法
- 支持 Java 语法特性

**缺点：**
- 需要在表达式中包含类前缀
- 需要禁用动态优化器以避免 Java 8 兼容性问题

---



## 使用方式

### 编译

```bash
# 使用 Make
make compile

# 或使用 Maven
cd .. && mvn clean compile -pl expr-eval -am
```

### 运行测试

```bash
# 使用 Make
make run

# 或使用 Maven
cd .. && cd expr-eval && mvn exec:java -Dexec.mainClass="com.github.bingoohuang.expreval.ExpressionEvaluatorTest"
```

### 清理

```bash
make clean
```

## 项目结构

```
expr-eval/
├── pom.xml                          # Maven 配置文件
├── Makefile                         # Make 构建脚本
├── README.md                        # 本文档
└── src/main/java/com/github/bingoohuang/expreval/
    └── ExpressionEvaluatorTest.java # 测试主类
```

## 依赖版本

- **Aviator**: 5.4.3
- **EvalEx**: 2.7
- **Apache Commons JEXL**: 2.1.1
- **MVEL**: 2.0.19

## 结论

对于需要复杂字符串操作的表达式求值场景：

1. **推荐使用 Aviator**：性能最佳，功能完善，易于扩展
2. **MVEL 和 JEXL**：功能完整，性能可接受，适合对性能要求不高的场景
3. **EvalEx**：不适合字符串操作场景，仅适用于纯数学表达式

---

*最后更新：基于 100,000 次求值性能测试*

