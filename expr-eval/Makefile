.PHONY: all compile test run clean help

# 项目根目录（相对于 expr-eval 目录）
PROJECT_ROOT := ..
MODULE_DIR := expr-eval
MAIN_CLASS := com.github.bingoohuang.expreval.ExpressionEvaluatorTest

# 默认目标
all: compile test

# 编译项目（包括依赖）
compile:
	@echo "=== 编译项目 ==="
	cd $(PROJECT_ROOT) && mvn clean compile -pl $(MODULE_DIR) -am

# 运行测试程序
run: compile
	@echo "=== 运行表达式评估测试 ==="
	cd $(PROJECT_ROOT)/$(MODULE_DIR) && mvn exec:java -Dexec.mainClass="$(MAIN_CLASS)" -Dexec.classpathScope=compile -q

# 运行测试（别名）
test: run

# 清理编译产物
clean:
	@echo "=== 清理编译产物 ==="
	cd $(PROJECT_ROOT) && mvn clean -pl $(MODULE_DIR) -am

# 显示帮助信息
help:
	@echo "可用的 Make 目标："
	@echo "  make          - 编译并运行测试（默认）"
	@echo "  make compile  - 编译项目"
	@echo "  make run      - 运行表达式评估测试"
	@echo "  make test     - 运行测试（run 的别名）"
	@echo "  make clean    - 清理编译产物"
	@echo "  make help     - 显示此帮助信息"

