#!/bin/bash

# GitHub推送脚本
# 用法: ./push.sh

cd /root/.openclaw/workspace/scaffold-project

echo "=== 检查本地提交 ==="
git log --oneline -3

echo ""
echo "=== 推送到GitHub ==="
git push origin main

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ 推送成功！"
    echo "查看提交: https://github.com/leeezw/spring-base/commits/main"
else
    echo ""
    echo "❌ 推送失败，请检查网络连接"
    echo ""
    echo "可以尝试："
    echo "1. 检查网络: curl -I https://github.com"
    echo "2. 重试推送: git push origin main"
    echo "3. 使用代理: git config --global http.proxy http://127.0.0.1:7890"
fi
