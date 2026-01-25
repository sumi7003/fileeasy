#!/bin/bash

# NestJS服务器废弃迁移验证脚本
# 执行日期: 2026-01-16

echo "🔍 开始验证NestJS服务器废弃迁移..."
echo ""

ERRORS=0
WARNINGS=0

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 1. 检查目录结构
echo "📂 1. 检查目录结构..."

if [ -d "apps/_deprecated_server" ]; then
    echo -e "   ${GREEN}✅ apps/_deprecated_server/ 存在${NC}"
else
    echo -e "   ${RED}❌ apps/_deprecated_server/ 不存在${NC}"
    ((ERRORS++))
fi

if [ -d "apps/server" ]; then
    echo -e "   ${YELLOW}⚠️  apps/server/ 仍然存在（应该被移除）${NC}"
    ((WARNINGS++))
else
    echo -e "   ${GREEN}✅ apps/server/ 已移除${NC}"
fi

# 2. 检查关键文件
echo ""
echo "📄 2. 检查关键文件..."

FILES=(
    "ARCHITECTURE_CHANGE_NOTICE.md"
    "MIGRATION_SUMMARY.md"
    "apps/_deprecated_server/README_IMPORTANT.md"
    "apps/_deprecated_server/README_DEPRECATED.md"
    "docs/DEPRECATED_NESTJS_SERVER.md"
    "docs/MASTER_PAD_DESIGN.md"
)

for file in "${FILES[@]}"; do
    if [ -f "$file" ]; then
        echo -e "   ${GREEN}✅ $file${NC}"
    else
        echo -e "   ${RED}❌ $file 不存在${NC}"
        ((ERRORS++))
    fi
done

# 3. 检查pnpm配置
echo ""
echo "⚙️  3. 检查pnpm workspace配置..."

if grep -q "_deprecated_server" pnpm-workspace.yaml; then
    echo -e "   ${GREEN}✅ pnpm-workspace.yaml 已更新排除规则${NC}"
else
    echo -e "   ${YELLOW}⚠️  pnpm-workspace.yaml 未找到排除规则${NC}"
    ((WARNINGS++))
fi

# 4. 检查main.ts是否已注释
echo ""
echo "💻 4. 检查NestJS代码状态..."

if [ -f "apps/_deprecated_server/src/main.ts" ]; then
    if grep -q "此服务器代码已注释" apps/_deprecated_server/src/main.ts; then
        echo -e "   ${GREEN}✅ main.ts 已添加废弃警告${NC}"
    else
        echo -e "   ${YELLOW}⚠️  main.ts 可能未正确注释${NC}"
        ((WARNINGS++))
    fi
else
    echo -e "   ${RED}❌ apps/_deprecated_server/src/main.ts 不存在${NC}"
    ((ERRORS++))
fi

# 5. 检查文档路径引用（应该很少有未更新的apps/server引用）
echo ""
echo "🔗 5. 检查文档路径引用..."

# 排除重启指南中的说明性文字（指导用户恢复时的路径）
OLD_REFS=$(grep -r "apps/server" docs/ README.md ARCHITECTURE_CHANGE_NOTICE.md 2>/dev/null | \
    grep -v "_deprecated_server" | \
    grep -v "Binary file" | \
    grep -v "如已恢复" | \
    grep -v "假设已恢复" | \
    grep -v "或恢复后" | \
    grep -v "DEPRECATED_NESTJS_SERVER.md" | \
    wc -l | tr -d ' ')

if [ "$OLD_REFS" -eq "0" ]; then
    echo -e "   ${GREEN}✅ 所有关键文档路径已更新${NC}"
    echo -e "   ${GREEN}   (重启指南中的说明性引用已忽略)${NC}"
elif [ "$OLD_REFS" -lt "3" ]; then
    echo -e "   ${YELLOW}⚠️  发现 $OLD_REFS 处未更新的 apps/server 引用${NC}"
    echo "   运行以下命令查看详情:"
    echo "   grep -rn 'apps/server' docs/ README.md | grep -v '_deprecated_server'"
    ((WARNINGS++))
else
    echo -e "   ${RED}❌ 发现 $OLD_REFS 处未更新的 apps/server 引用${NC}"
    echo "   运行以下命令查看详情:"
    echo "   grep -rn 'apps/server' docs/ README.md | grep -v '_deprecated_server'"
    ((ERRORS++))
fi

# 6. 检查Android Host Mode代码
echo ""
echo "📱 6. 检查Android Host Mode实现..."

if [ -f "apps/android-player/src/main/java/com/xplay/player/server/LocalServerService.kt" ]; then
    echo -e "   ${GREEN}✅ LocalServerService.kt 存在${NC}"
else
    echo -e "   ${RED}❌ LocalServerService.kt 不存在${NC}"
    ((ERRORS++))
fi

if [ -f "apps/android-player/src/main/java/com/xplay/player/MonitorScreen.kt" ]; then
    echo -e "   ${GREEN}✅ MonitorScreen.kt 存在${NC}"
else
    echo -e "   ${YELLOW}⚠️  MonitorScreen.kt 不存在${NC}"
    ((WARNINGS++))
fi

# 总结
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 验证结果总结"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}🎉 完美！所有检查都通过了！${NC}"
    echo ""
    echo "✅ NestJS服务器已成功移至废弃目录"
    echo "✅ 所有文档已更新"
    echo "✅ 配置文件已正确修改"
    echo "✅ Android Host Mode实现完整"
    echo ""
    echo "🚀 现在可以专注于Android Host Mode的开发了！"
elif [ $ERRORS -eq 0 ]; then
    echo -e "${YELLOW}⚠️  验证通过，但有 $WARNINGS 个警告${NC}"
    echo ""
    echo "迁移基本完成，但有一些小问题需要注意。"
    echo "请查看上面的警告信息。"
else
    echo -e "${RED}❌ 发现 $ERRORS 个错误和 $WARNINGS 个警告${NC}"
    echo ""
    echo "迁移可能未完全完成，请检查上面的错误信息。"
    exit 1
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📚 后续步骤"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "1. 阅读架构变更通知:"
echo "   cat ARCHITECTURE_CHANGE_NOTICE.md"
echo ""
echo "2. 查看迁移总结:"
echo "   cat MIGRATION_SUMMARY.md"
echo ""
echo "3. 开始使用Android Host Mode:"
echo "   cat docs/MASTER_PAD_DESIGN.md"
echo ""
echo "4. 如需重启NestJS (不推荐):"
echo "   cat apps/_deprecated_server/README_IMPORTANT.md"
echo ""

exit 0
