# 贴卡拨号 TapCall —— 一键推送到 GitHub（中文版）
param(
  [string]$RepoUrl = "",
  [switch]$Test
)
# 用 Continue：git 的正常提示（例如"还没有远程仓库"）不能被当成致命错误中断脚本
$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$env:Path = "D:\软件\Git\cmd;" + $env:Path
Set-Location -LiteralPath $PSScriptRoot

Write-Host ""
Write-Host "=============== 推送到 GitHub ===============" -ForegroundColor Cyan
Write-Host "作用：把本机这个 TapCall 项目上传到你自己的 GitHub 仓库。" -ForegroundColor Gray
Write-Host ""

$count = (git rev-list --count HEAD 2>$null)
if (-not $count -or [int]$count -lt 1) {
  Write-Host "本地还没有提交内容，请先告诉我，我帮你提交。" -ForegroundColor Red
  exit 1
}
if ($Test) {
  Write-Host "自检模式：中文显示正常，脚本可运行。" -ForegroundColor Green
  git log --oneline -1
  exit 0
}

if ([string]::IsNullOrWhiteSpace($RepoUrl)) {
  Write-Host "第 1 步：请到 https://github.com 建一个空仓库（New repository）" -ForegroundColor Yellow
  Write-Host "        仓库名填 TapCall，类型选 Public（公开），" -ForegroundColor Yellow
  Write-Host "        ☆ 三个勾都不要勾（README / gitignore / license）" -ForegroundColor Yellow
  Write-Host "        建好后复制那行地址，粘贴到下面并回车。" -ForegroundColor Yellow
  Write-Host ""
  Write-Host "地址形如： https://github.com/你的用户名/TapCall.git" -ForegroundColor Yellow
  $RepoUrl = Read-Host "请粘贴仓库地址"
}

$RepoUrl = $RepoUrl.Trim()
if ($RepoUrl -notmatch "^https?://" -and $RepoUrl -notmatch "^file://") {
  Write-Host "这个地址看起来不对，应该以 https:// 开头。" -ForegroundColor Red
  exit 1
}
if ($RepoUrl -notmatch "\.git$") { $RepoUrl = $RepoUrl + ".git" }

# 已存在 origin 就改地址，不存在就新增 —— 两种情况都不报错，脚本可反复运行
$remotes = @(git remote 2>$null)
if ($remotes -contains "origin") {
  git remote set-url origin $RepoUrl 2>$null | Out-Null
  Write-Host ("已更新仓库地址：" + $RepoUrl) -ForegroundColor Green
} else {
  git remote add origin $RepoUrl 2>$null | Out-Null
  Write-Host ("已绑定仓库：" + $RepoUrl) -ForegroundColor Green
}
if ($LASTEXITCODE -ne 0) {
  Write-Host "绑定仓库失败，请检查地址是否正确。" -ForegroundColor Red
  exit 1
}

Write-Host ""
Write-Host "正在上传…… 第一次会弹出 GitHub 登录窗口，登录一次以后就不用了。" -ForegroundColor Cyan
git push -u origin main

if ($LASTEXITCODE -eq 0) {
  Write-Host ""
  Write-Host "=============== 上传成功 ===============" -ForegroundColor Green
  Write-Host "打开你的仓库页面就能看到代码了。" -ForegroundColor Green
} else {
  Write-Host ""
  Write-Host "上传失败。常见原因：" -ForegroundColor Red
  Write-Host "  1) 仓库地址填错（注意用户名和仓库名）" -ForegroundColor Red
  Write-Host "  2) 建仓库时勾了 README，远端已有内容 → 删掉重建一个空仓库" -ForegroundColor Red
  Write-Host "  3) 登录窗口被关掉 → 重新双击本脚本再登录一次" -ForegroundColor Red
  Write-Host "  4) 网络问题 → 过一会儿再试（GitHub 在国内偶尔连不上）" -ForegroundColor Red
}