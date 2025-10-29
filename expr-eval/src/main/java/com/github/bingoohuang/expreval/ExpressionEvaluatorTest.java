package com.github.bingoohuang.expreval;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.function.FunctionUtils;
import com.googlecode.aviator.runtime.type.AviatorLong;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.googlecode.aviator.runtime.type.AviatorString;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.MapContext;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class ExpressionEvaluatorTest {

    private static final String EXPR = "substr(IDENTITY_DOC_ID, greatest(length(IDENTITY_DOC_ID) - 5, 1))";
    private static final String VAR_NAME = "IDENTITY_DOC_ID";
    private static final String TEST_VALUE = "123456789012345"; // length=15, greatest(10,1)=10, substr from 10 (1-based) = "012345"

    public static void main(String[] args) {
        System.out.println("Expression: " + EXPR);
        System.out.println("Test Value: " + TEST_VALUE);
        System.out.println("Expected Result: 012345\n");

        testEvalEx();
        testJexl();
        testAviator();
        testMvel();
    }

    private static void testEvalEx() {
        System.out.println("=== EvalEx ===");

        try {
            // EvalEx 主要用于数学表达式，字符串函数支持有限
            // 测试数值表达式：MAX(LENGTH - 5, 1) => MAX(15 - 5, 1) = 10
            com.udojava.evalex.Expression numExpr = new com.udojava.evalex.Expression("MAX(LENGTH - 5, 1)");
            numExpr.setVariable("LENGTH", new BigDecimal(TEST_VALUE.length()));

            System.out.println("Variables: [LENGTH]");
            System.out.println("Functions: [MAX]");

            BigDecimal result = numExpr.eval();
            System.out.println("Result (numeric test): " + result);
            System.out.println("Note: EvalEx is primarily for numeric expressions, string operations like substr are not directly supported.");

            // 性能测试
            long start = System.nanoTime();
            for (int i = 0; i < 100000; i++) {
                numExpr.eval();
            }
            long end = System.nanoTime();
            System.out.println("Time for 100,000 evaluations: " + (end - start) + " ns");
            System.out.println("Avg time: " + (end - start) / 100000.0 + " ns\n");
        } catch (Exception e) {
            System.err.println("EvalEx evaluation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testJexl() {
        System.out.println("=== Apache Commons JEXL (No Namespace) ===");

        // 创建引擎并注册自定义函数到 null namespace（top-level）
        JexlEngine jexl = new JexlEngine();
        Map<String, Object> functions = new HashMap<String, Object>();
        functions.put(null, new CustomFunctions());  // 使用 null 作为 key，实现无前缀调用
        jexl.setFunctions(functions);

        // 表达式无需 namespace 前缀
        // 直接使用 "substr(IDENTITY_DOC_ID, greatest(length(IDENTITY_DOC_ID) - 5, 1))"

        // 预编译
        org.apache.commons.jexl2.Expression expr = jexl.createExpression(EXPR);

        // 提取变量和函数（JEXL 无内置 API，简化演示：手动列出）
        System.out.println("Variables: [IDENTITY_DOC_ID]");
        System.out.println("Functions: [substr, greatest, length]");

        // 赋值并求值
        JexlContext context = new MapContext();
        context.set(VAR_NAME, TEST_VALUE);
        Object result = expr.evaluate(context);
        System.out.println("Result: " + result);

        // 性能测试
        long start = System.nanoTime();
        for (int i = 0; i < 100000; i++) {
            expr.evaluate(context);
        }
        long end = System.nanoTime();
        System.out.println("Time for 100,000 evaluations: " + (end - start) + " ns");
        System.out.println("Avg time: " + (end - start) / 100000.0 + " ns\n");
    }

    private static void testAviator() {
        System.out.println("=== Aviator ===");

        // 注册自定义函数
        AviatorEvaluator.addFunction(new SubstrFunction());
        AviatorEvaluator.addFunction(new GreatestFunction());
        AviatorEvaluator.addFunction(new LengthFunction()); // 虽有内置 string.length，但为一致自定义

        // 预编译
        Expression expr = AviatorEvaluator.compile(EXPR);

        // 提取变量和函数
        System.out.println("Variables: " + expr.getVariableNames());
        System.out.println("Functions: [substr, greatest, length]");

        // 赋值并求值
        Map<String, Object> env = new HashMap<String, Object>();
        env.put(VAR_NAME, TEST_VALUE);
        Object result = expr.execute(env);
        System.out.println("Result: " + result);

        // 性能测试
        long start = System.nanoTime();
        for (int i = 0; i < 100000; i++) {
            expr.execute(env);
        }
        long end = System.nanoTime();
        System.out.println("Time for 100,000 evaluations: " + (end - start) + " ns");
        System.out.println("Avg time: " + (end - start) / 100000.0 + " ns\n");
    }

    private static void testMvel() {
        System.out.println("=== MVEL ===");

        try {
            // 禁用 MVEL 的动态优化器，避免 Java 8 字节码兼容性问题
            org.mvel2.MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = true;
            org.mvel2.optimizers.OptimizerFactory.setDefaultOptimizer("reflective");

            // MVEL 支持 import 静态方法，需要使用修改后的表达式
            String mvelExpr = "CustomFunctions.substr(IDENTITY_DOC_ID, CustomFunctions.greatest(CustomFunctions.length(IDENTITY_DOC_ID) - 5, 1))";

            // 创建 ParserContext 并导入 CustomFunctions
            ParserContext parserContext = new ParserContext();
            parserContext.addImport("CustomFunctions", CustomFunctions.class);

            // 预编译
            Serializable compiled = MVEL.compileExpression(mvelExpr, parserContext);

            // 提取变量和函数（MVEL 无直接 API，简化演示：手动列出）
            System.out.println("Variables: [IDENTITY_DOC_ID]");
            System.out.println("Functions: [CustomFunctions.substr, CustomFunctions.greatest, CustomFunctions.length]");

            // 赋值并求值
            Map<String, Object> vars = new HashMap<String, Object>();
            vars.put(VAR_NAME, TEST_VALUE);

            Object result = MVEL.executeExpression(compiled, vars);
            System.out.println("Result: " + result);

            // 性能测试
            long start = System.nanoTime();
            for (int i = 0; i < 100000; i++) {
                MVEL.executeExpression(compiled, vars);
            }
            long end = System.nanoTime();
            System.out.println("Time for 100,000 evaluations: " + (end - start) + " ns");
            System.out.println("Avg time: " + (end - start) / 100000.0 + " ns\n");
        } catch (Exception e) {
            System.err.println("MVEL evaluation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 自定义函数类（用于 JEXL 和 MVEL）
    public static class CustomFunctions {
        public static String substr(String str, int start) {
            return str.substring(start - 1); // 1-based to 0-based
        }

        public static int greatest(int a, int b) {
            return Math.max(a, b);
        }

        public static int length(String str) {
            return str.length();
        }
    }

    // Aviator 自定义函数
    public static class SubstrFunction extends AbstractFunction {
        @Override
        public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
            String str = FunctionUtils.getStringValue(arg1, env);
            int start = FunctionUtils.getNumberValue(arg2, env).intValue();
            return new AviatorString(str.substring(start - 1));
        }

        public String getName() {
            return "substr";
        }
    }

    public static class GreatestFunction extends AbstractFunction {
        @Override
        public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
            int a = FunctionUtils.getNumberValue(arg1, env).intValue();
            int b = FunctionUtils.getNumberValue(arg2, env).intValue();
            return AviatorLong.valueOf(Math.max(a, b));
        }

        public String getName() {
            return "greatest";
        }
    }

    public static class LengthFunction extends AbstractFunction {
        @Override
        public AviatorObject call(Map<String, Object> env, AviatorObject arg1) {
            String str = FunctionUtils.getStringValue(arg1, env);
            return AviatorLong.valueOf(str.length());
        }

        public String getName() {
            return "length";
        }
    }
}