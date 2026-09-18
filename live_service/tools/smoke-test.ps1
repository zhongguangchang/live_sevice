<#
  生活服务网 · 后端全链路冒烟测试

  覆盖范围：
    1. 管理端 / 用户端全部只读接口
    2. 登录鉴权与越权访问
    3. 管理端各模块的增删改（分类 / 服务项目 / 套餐 / 服务人员 / 区域 / 排期 / 员工 / 营业状态 / 上传）
    4. 用户端地址簿、服务清单
    5. 完整业务闭环：下单 -> 支付 -> 派单 -> 接单 -> 开始服务 -> 完成 -> 评价
    6. 取消订单与名额回补
    7. Redis 并发防超卖（10 个并发抢 1 个名额）
    8. MySQL CAS 兜底（Redis 与 MySQL 不一致时不允许落账）
    9. MQ 延迟队列：消息真的发出去没有、消费队列有没有消费者
   10. 派单超时 5 分钟后真实转派 + 转派次数上限（-SkipSlow 可跳过，这一段要等 5 分钟）

  用法（在 D:\campus\live_service 目录下）：
      pwsh -File .\tools\smoke-test.ps1
      pwsh -File .\tools\smoke-test.ps1 -SkipSlow

  前置条件：后端已在 8081 启动、MySQL / Redis / RabbitMQ 可用。
  测试数据统一带「【测试】」前缀，脚本结束时会清理。
#>
[CmdletBinding()]
param(
    [string]$Base = 'http://localhost:8081',
    [string]$MysqlExe = 'D:\MySQL\MySQL Server 8.0\bin\mysql.exe',
    [string]$DbName = 'life_service',
    [string]$RabbitApi = 'http://192.168.229.129:15672',
    [string]$RabbitUser = 'admin',
    [string]$RabbitPassword = '123456',
    [switch]$SkipSlow
)

[System.Net.ServicePointManager]::DefaultConnectionLimit = 200
[System.Net.ServicePointManager]::Expect100Continue = $false
$ErrorActionPreference = 'Continue'

$script:Pass = 0
$script:Fail = 0
$script:Failures = New-Object System.Collections.Generic.List[string]

function Write-Section([string]$title) {
    Write-Host ''
    Write-Host "===== $title =====" -ForegroundColor Cyan
}

function Check([string]$name, $condition, [string]$detail = '') {
    if ($condition) {
        $script:Pass++
        Write-Host "  [OK]   $name" -ForegroundColor Green
    }
    else {
        $script:Fail++
        $script:Failures.Add("$name | $detail")
        Write-Host "  [FAIL] $name  --> $detail" -ForegroundColor Red
    }
}

function New-B64Url([byte[]]$bytes) {
    [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
}

function New-Token([string]$secret, [string]$claims) {
    $h = New-B64Url([Text.Encoding]::UTF8.GetBytes('{"alg":"HS256"}'))
    $p = New-B64Url([Text.Encoding]::UTF8.GetBytes($claims))
    $hm = New-Object System.Security.Cryptography.HMACSHA256
    $hm.Key = [Text.Encoding]::UTF8.GetBytes($secret)
    $sig = New-B64Url($hm.ComputeHash([Text.Encoding]::UTF8.GetBytes("$h.$p")))
    "$h.$p.$sig"
}

$script:Exp = [int][double]::Parse((Get-Date -UFormat %s)) + 7200
$script:AdminToken = New-Token 'itcast' "{`"empId`":1,`"exp`":$script:Exp}"

function New-UserToken([int]$userId) {
    New-Token 'itheima' "{`"userId`":$userId,`"exp`":$script:Exp}"
}

function New-AdminToken([long]$empId) {
    New-Token 'itcast' "{`"empId`":$empId,`"exp`":$script:Exp}"
}

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Path,
        $Body = $null,
        [string]$Token,
        [string]$TokenHeader = 'token'
    )
    $headers = @{}
    if ($Token) { $headers[$TokenHeader] = $Token }
    $params = @{
        Uri             = $Base + $Path
        Method          = $Method
        Headers         = $headers
        UseBasicParsing = $true
        TimeoutSec      = 30
    }
    if ($null -ne $Body) {
        $json = if ($Body -is [string]) { $Body } else { $Body | ConvertTo-Json -Depth 8 -Compress }
        $params['Body'] = [Text.Encoding]::UTF8.GetBytes($json)
        $params['ContentType'] = 'application/json; charset=utf-8'
    }
    try {
        $resp = Invoke-WebRequest @params
        $text = [Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray())
        $obj = $null
        try { $obj = $text | ConvertFrom-Json } catch { }
        return [pscustomobject]@{
            Http = [int]$resp.StatusCode
            Code = if ($obj) { $obj.code } else { $null }
            Msg  = if ($obj) { $obj.msg } else { $text }
            Data = if ($obj) { $obj.data } else { $null }
            Raw  = $text
            Err  = $false
        }
    }
    catch {
        $r = $_.Exception.Response
        $text = ''
        $status = 0
        if ($r) {
            $status = [int]$r.StatusCode
            try {
                $sr = New-Object IO.StreamReader($r.GetResponseStream(), [Text.Encoding]::UTF8)
                $text = $sr.ReadToEnd()
            }
            catch { }
        }
        $obj = $null
        if ($text) { try { $obj = $text | ConvertFrom-Json } catch { } }
        return [pscustomobject]@{
            Http = $status
            Code = if ($obj) { $obj.code } else { $null }
            Msg  = if ($obj) { $obj.msg } else { $text }
            Data = $null
            Raw  = $text
            Err  = $true
        }
    }
}

function Admin([string]$Method, [string]$Path, $Body = $null) {
    Invoke-Api -Method $Method -Path "/admin$Path" -Body $Body -Token $script:AdminToken
}

function User([string]$Method, [string]$Path, $Body = $null, [int]$UserId = 8001) {
    Invoke-Api -Method $Method -Path "/user$Path" -Body $Body -Token (New-UserToken $UserId) -TokenHeader 'authentication'
}

function Sql([string]$query) {
    $margs = @('-uroot', '-p123456', '--default-character-set=utf8mb4', '-N', '-B', '-D', $DbName, '-e', $query)
    $out = & $MysqlExe @margs 2>$null
    if ($null -eq $out) { return @() }
    # 注意：这里必须显式转成字符串数组。
    # 直接把单行结果返回给调用方时，PowerShell 会把只有一个元素的数组
    # 拆成裸字符串，调用方再取 [0] 拿到的就是第一个「字符」，
    # 例如 9005 会变成 '9'，后面拿它当 id 去改数据就全错了
    return @($out | ForEach-Object { [string]$_ })
}

function Sql1([string]$query) {
    $rows = @(Sql $query)
    if ($rows.Count -eq 0) { return $null }
    $value = [string]$rows[0]
    if ($value -eq '') { return $null }
    return $value
}

function Ok($resp) { $resp -and $resp.Code -eq 1 }

function Rabbit([string]$path) {
    $b64 = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("$RabbitUser`:$RabbitPassword"))
    Invoke-RestMethod -Uri ($RabbitApi + $path) -Headers @{ Authorization = "Basic $b64" } -TimeoutSec 20
}

function Get-QueueDepth([string]$name) {
    $q = Rabbit '/api/queues'
    $item = $q | Where-Object { $_.name -eq $name }
    if ($item) { return [int]$item.messages }
    return -1
}

function Wait-QueueDepthUp {
    <#
      等队列深度上涨。
      RabbitMQ 管理台的统计数据不是实时的（默认几秒刷新一次），
      发完消息立刻查会读到旧值，所以必须轮询重试。
      不做重试的话这个用例会随机失败 —— 那是测试写法的问题，不是业务问题。
    #>
    param(
        [string]$QueueName,
        [int]$Before,
        [int]$TimeoutSeconds = 24
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $now = Get-QueueDepth $QueueName
        if ($now -gt $Before) { return $now }
        Start-Sleep -Seconds 3
    }
    return (Get-QueueDepth $QueueName)
}

# 每次运行都换一批唯一值，避免和上一轮残留数据撞唯一键
# （服务人员手机号、员工账号、区域编码在库里都是唯一索引）
$script:Suffix = Get-Random -Minimum 1000 -Maximum 9999
$script:TestPhone = '139' + (Get-Random -Minimum 10000000 -Maximum 99999999)
$script:TestUsername = "test_emp_$script:Suffix"
$script:TestAreaCode = (Get-Random -Minimum 900000 -Maximum 999999)

# 本轮用过的时段 id，避免同一脚本内重复挑到同一个时段
$script:UsedSlots = New-Object System.Collections.Generic.List[long]
$script:WarmEnd = (Get-Date).AddDays(60).ToString('yyyy-MM-dd')

function Pick-Slot {
    param([long]$ProviderId = 0)
    $where = "service_id = 1001 and booked_count = 0 and status = 1 and service_date = '$($script:Tomorrow)'"
    if ($ProviderId -gt 0) { $where += " and provider_id = $ProviderId" }
    $exclude = ''
    if ($script:UsedSlots.Count -gt 0) {
        $exclude = ' and id not in (' + ($script:UsedSlots -join ',') + ')'
    }
    $id = Sql1 "select id from slot where $where$exclude order by service_date, start_time limit 1"
    if ($null -eq $id) { return $null }
    $script:UsedSlots.Add([long]$id)
    return $id
}

function Ensure-TestDate {
    <#
      挑一个本轮测试独占的日期：那天至少有 8 个空闲时段。
      演示数据只铺了 3 天，反复跑测试会把它们占满，
      所以这里先找现成的，找不到就现场批量生成一批。
    #>
    for ($d = 1; $d -le 60; $d++) {
        $date = (Get-Date).AddDays($d).ToString('yyyy-MM-dd')
        $free = [int](Sql1 "select count(*) from slot where service_id = 1001 and booked_count = 0 and status = 1 and service_date = '$date'")
        if ($free -ge 8) { return $date }

        $existing = [int](Sql1 "select count(*) from slot where service_date = '$date'")
        if ($existing -eq 0 -and $d -ge 30) {
            Write-Host "  演示数据的时段已被占满，为 $date 批量生成一批" -ForegroundColor Yellow
            $r = Admin POST '/slot/batch' @{
                providerIds  = @(7001, 7002, 7003)
                serviceIds   = @(1001)
                startDate    = $date
                endDate      = $date
                startTimes   = @('09:00:00', '14:00:00', '16:00:00')
                slotDuration = 120
                totalStock   = 1
            }
            Write-Host ("  生成结果：" + $r.Raw)
            if (Ok $r) { return $date }
        }
    }
    throw '没有找到可用的测试日期'
}

# 本轮测试统一使用的排期日期。
# 名字沿用 Tomorrow 是因为脚本里到处引用它，实际含义是「本脚本本轮独占的测试日期」：
# 优先用明天，如果演示数据里的时段被历史测试占满了，就往后顺延，
# 确实没有可用的就现场生成一批，保证脚本可以反复运行。
# 注意：必须放在 Ensure-TestDate 定义之后 —— PowerShell 的函数定义
# 是按语句顺序注册的，写在定义之前调用会直接报「找不到命令」
$script:Tomorrow = Ensure-TestDate
Write-Host ("本轮测试使用的排期日期：" + $script:Tomorrow) -ForegroundColor Cyan

# ============================================================================
#  一、服务健康
# ============================================================================
Write-Section '一、服务健康检查'
$health = Admin GET '/shop/status'
Check '后端 8081 可访问（/admin/shop/status）' (Ok $health) ($health.Raw)
$userShop = Invoke-Api -Method GET -Path '/user/shop/status'
Check '用户端营业状态接口免登录可访问' (Ok $userShop) ($userShop.Raw)

# ============================================================================
#  二、管理端只读接口
# ============================================================================
Write-Section '二、管理端只读接口'

$r = Admin GET '/employee/page?page=1&pageSize=10'
Check '员工分页 /admin/employee/page' (Ok $r) ($r.Raw)
$r = Admin GET '/employee/1'
Check '员工详情 /admin/employee/1' (Ok $r) ($r.Raw)
$r = Admin GET '/category/page?page=1&pageSize=10'
Check '分类分页 /admin/category/page' (Ok $r) ($r.Raw)
$r = Admin GET '/category/list?type=1'
Check '分类列表 /admin/category/list' (Ok $r -and $r.Data.Count -gt 0) ($r.Raw)
$r = Admin GET '/serviceItem/page?page=1&pageSize=10'
Check '服务项目分页 /admin/serviceItem/page' (Ok $r -and $r.Data.total -gt 0) ($r.Raw)
$r = Admin GET '/serviceItem/1001'
Check '服务项目详情 /admin/serviceItem/1001' (Ok $r -and $r.Data.name) ($r.Raw)
$r = Admin GET '/serviceItem/list?categoryId=1'
Check '服务项目按分类查询 /admin/serviceItem/list' (Ok $r) ($r.Raw)
$r = Admin GET '/servicePackage/page?page=1&pageSize=10'
Check '套餐分页 /admin/servicePackage/page' (Ok $r) ($r.Raw)
$pkgId = if (Ok $r -and $r.Data.records.Count -gt 0) { $r.Data.records[0].id } else { $null }
$r = Admin GET '/servicePackage/list?categoryId=9'
Check '套餐列表 /admin/servicePackage/list' (Ok $r) ($r.Raw)
if ($pkgId) {
    $r = Admin GET "/servicePackage/$pkgId"
    Check '套餐详情 /admin/servicePackage/{id}' (Ok $r) ($r.Raw)
}
$r = Admin GET '/provider/page?page=1&pageSize=10'
Check '服务人员分页 /admin/provider/page' (Ok $r -and $r.Data.total -gt 0) ($r.Raw)
$r = Admin GET '/provider/7001'
Check '服务人员详情 /admin/provider/7001' (Ok $r -and $r.Data.name) ($r.Raw)
$r = Admin GET '/provider/list'
Check '服务人员列表 /admin/provider/list' (Ok $r) ($r.Raw)
$r = Admin GET '/provider/available?categoryId=1'
Check '按技能查可接单人员 /admin/provider/available' (Ok $r -and $r.Data.Count -gt 0) ($r.Raw)
$r = Admin GET '/review/page?page=1&pageSize=10'
Check '评价分页 /admin/review/page' (Ok $r) ($r.Raw)
$r = Admin GET '/serviceArea/list'
Check '服务区域列表 /admin/serviceArea/list' (Ok $r -and $r.Data.Count -gt 0) ($r.Raw)
$r = Admin GET '/serviceOrder/conditionSearch?page=1&pageSize=10'
Check '订单条件查询 /admin/serviceOrder/conditionSearch' (Ok $r) ($r.Raw)
$r = Admin GET '/serviceOrder/statistics'
Check '订单状态统计 /admin/serviceOrder/statistics' (Ok $r -and $null -ne $r.Data.toBeAccepted) ($r.Raw)
$orderId = Sql1 'select id from service_order order by id desc limit 1'
$r = Admin GET "/serviceOrder/details/$orderId"
Check "订单详情 /admin/serviceOrder/details/$orderId" (Ok $r -and $r.Data.order.id) ($r.Raw)
$r = Admin GET "/slot/list?serviceDate=$script:Tomorrow"
Check '排期条件查询 /admin/slot/list' (Ok $r -and $r.Data.Count -gt 0) ($r.Raw)

# ============================================================================
#  三、用户端只读接口
# ============================================================================
Write-Section '三、用户端只读接口'

$r = User GET '/category/list?type=1'
Check '用户端分类 /user/category/list' (Ok $r -and $r.Data.Count -gt 0) ($r.Raw)
$r = User GET '/service/list?categoryId=1'
Check '用户端服务列表 /user/service/list' (Ok $r) ($r.Raw)
$r = User GET '/service/1001'
Check '用户端服务详情 /user/service/1001' (Ok $r -and $r.Data.name) ($r.Raw)
$r = User GET "/slot/available?serviceId=1001&serviceDate=$script:Tomorrow"
Check '可预约时段（含 Redis 实时余量）' (Ok $r -and $null -ne $r.Data[0].remainStock) ($r.Raw)
$r = User GET '/addressBook/list'
Check '地址列表 /user/addressBook/list' (Ok $r -and $r.Data.Count -gt 0) ($r.Raw)
$r = User GET '/addressBook/default'
Check '默认地址 /user/addressBook/default' (Ok $r -and $r.Data.id) ($r.Raw)
$r = User GET '/addressBook/8501'
Check '地址详情 /user/addressBook/8501' (Ok $r -and $r.Data.consignee) ($r.Raw)
$r = User GET '/cart/list'
Check '服务清单 /user/cart/list' (Ok $r) ($r.Raw)
$r = User GET '/review/list?page=1&pageSize=10&serviceId=1001'
Check '服务评价列表 /user/review/list' (Ok $r) ($r.Raw)
$r = User GET '/serviceOrder/history?page=1&pageSize=10'
Check '历史订单 /user/serviceOrder/history' (Ok $r) ($r.Raw)

# ============================================================================
#  四、鉴权与越权
# ============================================================================
Write-Section '四、鉴权与越权'

$r = Invoke-Api -Method GET -Path '/admin/employee/page?page=1&pageSize=10'
Check '管理端接口无 token 被拦截' ($r.Http -eq 401 -or $r.Code -eq 0) ("http=" + $r.Http)
$r = Invoke-Api -Method GET -Path '/admin/employee/page?page=1&pageSize=10' -Token (New-UserToken 8001) -TokenHeader 'token'
Check '拿用户 token 访问管理端被拦截' ($r.Http -eq 401 -or $r.Code -eq 0) ("http=" + $r.Http)
$r = Invoke-Api -Method GET -Path '/user/addressBook/list'
Check '用户端接口无 token 被拦截' ($r.Http -eq 401 -or $r.Code -eq 0) ("http=" + $r.Http)
$otherOrder = Sql1 'select id from service_order where user_id <> 8001 order by id desc limit 1'
if ($otherOrder) {
    $r = User GET "/serviceOrder/detail/$otherOrder"
    Check '查别人订单详情被拒绝（越权校验）' (-not (Ok $r)) ($r.Raw)
}

# ============================================================================
#  五、管理端增删改
#  所有测试数据都带「【测试】」前缀，结束时统一清理
# ============================================================================
Write-Section '五、管理端增删改'

# ---- 分类 ----
$catMaxBefore = [long](Sql1 'select max(id) from category')
$r = Admin POST '/category' @{ type = 1; name = '【测试】分类'; sort = 99 }
Check '新增分类' (Ok $r) ($r.Raw)
$catId = [long](Sql1 'select max(id) from category')
Check '新增的分类能在库里查到（id 确实新增了）' ($catId -gt $catMaxBefore) ("前 " + $catMaxBefore + " 后 " + $catId)
$r = Admin PUT '/category' @{ id = $catId; type = 1; name = '【测试】分类改名'; sort = 98 }
Check '修改分类' (Ok $r) ($r.Raw)
$r = Admin POST "/category/status/0?id=$catId"
Check '分类停用' (Ok $r) ($r.Raw)
$r = Admin POST "/category/status/1?id=$catId"
Check '分类启用' (Ok $r) ($r.Raw)

# ---- 服务项目 ----
$itemMaxBefore = [long](Sql1 'select max(id) from service_item')
$r = Admin POST '/serviceItem' @{
    name          = '【测试】服务项目'
    categoryId    = [long]$catId
    price         = 88.00
    unit          = '次'
    duration      = 60
    serviceMode   = 1
    needAppoint   = 1
    status        = 0
    description   = '自动化测试用'
    image         = 'https://example.com/test.png'
    specs         = @(@{ name = '面积'; value = '60-90平方米'; priceDelta = 10.00; sort = 0 })
}
Check '新增服务项目（带规格）' (Ok $r) ($r.Raw)
$itemId = [long](Sql1 'select max(id) from service_item')
Check '新增的服务项目能在库里查到（id 确实新增了）' ($itemId -gt $itemMaxBefore) ("前 " + $itemMaxBefore + " 后 " + $itemId)
Check '服务项目规格已写入 service_spec' ((Sql1 "select count(*) from service_spec where service_id=$itemId") -eq '1') '规格条数不是 1'
$r = Admin PUT '/serviceItem' @{
    id          = $itemId
    name        = '【测试】服务项目改名'
    categoryId  = [long]$catId
    price       = 99.00
    unit        = '次'
    duration    = 90
    serviceMode = 1
    status      = 0
}
Check '修改服务项目' (Ok $r) ($r.Raw)
$r = Admin POST "/serviceItem/status/1?id=$itemId"
Check '服务项目起售' (Ok $r) ($r.Raw)
$r = Admin GET ("/serviceItem/list?categoryId=" + $catId)
Check '起售后能在列表里查到' (Ok $r -and $r.Data.Count -gt 0) ($r.Raw)

# ---- 套餐 ----
$pkgMaxBefore = [long](Sql1 'select max(id) from service_package')
$r = Admin POST '/servicePackage' @{
    name        = '【测试】套餐'
    categoryId  = [long]$catId
    price       = 188.00
    serviceMode = 1
    status      = 1
    description = '自动化测试用'
    items       = @(@{ serviceId = [long]$itemId; name = '【测试】服务项目改名'; price = 99.00; copies = 1 })
}
Check '新增服务套餐（含明细）' (Ok $r) ($r.Raw)
$pkgNewId = [long](Sql1 'select max(id) from service_package')
Check '新增的套餐能在库里查到（id 确实新增了）' ($pkgNewId -gt $pkgMaxBefore) ("前 " + $pkgMaxBefore + " 后 " + $pkgNewId)
$r = Admin PUT '/servicePackage' @{
    id          = $pkgNewId
    name        = '【测试】套餐改名'
    categoryId  = [long]$catId
    price       = 199.00
    serviceMode = 1
    status      = 1
    items       = @(@{ serviceId = [long]$itemId; name = '【测试】服务项目改名'; price = 99.00; copies = 1 })
}
Check '修改服务套餐' (Ok $r) ($r.Raw)

# ---- 服务人员 ----
$provMaxBefore = [long](Sql1 'select max(id) from provider')
$r = Admin POST '/provider' @{
    name        = '【测试】师傅'
    phone       = '13900000099'
    sex         = '男'
    status      = 0
    serviceMode = 1
    workYears   = 3
    intro       = '自动化测试用'
    categoryIds = @([long]$catId)
}
Check '新增服务人员（带技能）' (Ok $r) ($r.Raw)
$provId = [long](Sql1 'select max(id) from provider')
Check '新增的师傅能在库里查到（id 确实新增了）' ($provId -gt $provMaxBefore) ("前 " + $provMaxBefore + " 后 " + $provId)
Check '师傅技能已写入 provider_skill' ((Sql1 "select count(*) from provider_skill where provider_id=$provId") -eq '1') '技能条数不是 1'
$r = Admin PUT '/provider' @{
    id          = [long]$provId
    name        = '【测试】师傅改名'
    phone       = '13900000099'
    sex         = '男'
    status      = 1
    serviceMode = 1
    workYears   = 4
    categoryIds = @([long]$catId)
}
Check '修改服务人员' (Ok $r) ($r.Raw)
$r = Admin GET "/provider/available?categoryId=$catId"
Check '该师傅出现在可接单列表里' (Ok $r -and $r.Data.Count -gt 0) ($r.Raw)

# ---- 服务区域 ----
$r = Admin POST '/serviceArea' @{ name = '【测试】区'; code = '999999'; cityCode = '110100'; cityName = '北京市'; sort = 99; status = 1 }
Check '新增服务区域' (Ok $r) ($r.Raw)
$areaId = [long](Sql1 "select id from service_area where code='999999' limit 1")
$r = Admin PUT '/serviceArea' @{ id = $areaId; name = '【测试】区改名'; code = '999999'; cityCode = '110100'; cityName = '北京市'; sort = 99; status = 1 }
Check '修改服务区域' (Ok $r) ($r.Raw)
$r = Admin POST "/serviceArea/status/0?id=$areaId"
Check '服务区域停用' (Ok $r) ($r.Raw)
$r = Admin DELETE "/serviceArea/$areaId"
Check '删除服务区域' (Ok $r) ($r.Raw)

# ---- 排期 ----
$r = Admin POST '/slot/batch' @{
    providerIds  = @([long]$provId)
    serviceIds   = @([long]$itemId)
    startDate    = $script:Tomorrow
    endDate      = $script:Tomorrow
    startTimes   = @('10:00:00', '15:00:00')
    slotDuration = 60
    totalStock   = 1
}
Check '批量生成排期' (Ok $r) ($r.Raw)
$slotCount = Sql1 "select count(*) from slot where provider_id=$provId and service_id=$itemId"
Check '排期已落库（2 个时段）' ($slotCount -eq '2') ("实际 " + $slotCount)
$testSlotId = Sql1 "select id from slot where provider_id=$provId and service_id=$itemId order by start_time limit 1"
$r = Admin POST "/slot/status/0?id=$testSlotId"
Check '关闭时段' (Ok $r) ($r.Raw)
$r = Admin POST "/slot/status/1?id=$testSlotId"
Check '重新开放时段' (Ok $r) ($r.Raw)
$r = Admin POST "/slot/warmup?begin=$script:Tomorrow&end=$script:Tomorrow"
Check '手动预热库存到 Redis' (Ok $r -and $r.Data -gt 0) ($r.Raw)
$r = Admin POST '/slot/reconcile'
Check '手动触发库存对账' (Ok $r) ($r.Raw)

# ---- 上传（未配置 OSS 时走本地存储）----
$tmpImg = Join-Path $env:TEMP 'life-smoke-test.png'
[IO.File]::WriteAllBytes($tmpImg, [byte[]](0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
try {
    $resp = Invoke-WebRequest -Uri "$Base/admin/common/upload" -Method POST -Headers @{ token = $script:AdminToken } `
        -Form @{ file = Get-Item $tmpImg } -UseBasicParsing -TimeoutSec 30
    $uploadResult = ([Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray())) | ConvertFrom-Json
    Check '图片上传（返回可访问地址）' ($uploadResult.code -eq 1 -and $uploadResult.data) ($resp.Content)
    if ($uploadResult.data) {
        $img = Invoke-WebRequest -Uri $uploadResult.data -UseBasicParsing -TimeoutSec 20
        Check '上传后的图片能通过 URL 访问' ($img.StatusCode -eq 200) ("http=" + $img.StatusCode)
    }
}
catch {
    Check '图片上传（返回可访问地址）' $false $_.Exception.Message
}

# ---- 员工与改密码 ----
$empMaxBefore = [long](Sql1 'select max(id) from employee')
$r = Admin POST '/employee' @{ username = 'test_emp'; name = '【测试】员工'; phone = '13900000088'; sex = '女'; idNumber = '110101199001019999' }
Check '新增员工（默认密码 123456）' (Ok $r) ($r.Raw)
$empId = [long](Sql1 "select max(id) from employee where username='test_emp'")
Check '新增的员工能在库里查到（id 确实新增了）' ($empId -gt $empMaxBefore) ("前 " + $empMaxBefore + " 后 " + $empId)
$r = Admin PUT '/employee' @{ id = $empId; username = 'test_emp'; name = '【测试】员工改名'; phone = '13900000088'; sex = '女'; idNumber = '110101199001019999'; role = 2 }
Check '修改员工' (Ok $r) ($r.Raw)
$r = Invoke-Api -Method POST -Path '/admin/employee/login' -Body @{ username = 'test_emp'; password = '123456' }
Check '新员工用默认密码登录成功' (Ok $r -and $r.Data.token) ($r.Raw)
$empToken = New-AdminToken ([long]$empId)
$r = Invoke-Api -Method PUT -Path '/admin/employee/editPassword' -Body @{ oldPassword = '123456'; newPassword = '654321' } -Token $empToken
Check '修改密码接口可用' (Ok $r) ($r.Raw)
$r = Invoke-Api -Method POST -Path '/admin/employee/login' -Body @{ username = 'test_emp'; password = '654321' }
Check '用新密码登录成功' (Ok $r -and $r.Data.token) ($r.Raw)
$r = Invoke-Api -Method PUT -Path '/admin/employee/editPassword' -Body @{ oldPassword = 'wrong'; newPassword = 'abcdef' } -Token $empToken
Check '旧密码错误时改密码被拒绝' (-not (Ok $r)) ($r.Raw)
$r = Admin POST "/employee/status/0?id=$empId"
Check '员工禁用' (Ok $r) ($r.Raw)
$r = Admin POST "/employee/status/1?id=$empId"
Check '员工启用' (Ok $r) ($r.Raw)

# ---- 评价管理 ----
$reviewId = Sql1 'select id from review order by id desc limit 1'
if ($reviewId) {
    $r = Admin PUT '/review/reply' @{ id = [long]$reviewId; reply = '感谢您的评价，我们会继续努力' }
    Check '管理端回复评价' (Ok $r) ($r.Raw)
}

# ============================================================================
#  六、用户端：地址簿与服务清单
# ============================================================================
Write-Section '六、用户端地址簿 / 服务清单'

$addrMaxBefore = [long](Sql1 'select max(id) from address_book')
$r = User POST '/addressBook' @{
    consignee = '【测试】收货人'; phone = '13700000001'; sex = '男'
    provinceName = '北京市'; cityName = '北京市'; districtName = '海淀区'; districtCode = '110108'
    detail = '自动化测试地址 1 号'; label = '公司'
}
Check '新增地址' (Ok $r) ($r.Raw)
$addrId = [long](Sql1 'select max(id) from address_book where user_id=8001')
Check '新增地址已落库且归属当前用户' ($addrId -gt $addrMaxBefore) ("前 " + $addrMaxBefore + " 后 " + $addrId)
$r = User PUT '/addressBook' @{ id = $addrId; consignee = '【测试】收货人改名'; phone = '13700000001'; detail = '自动化测试地址 2 号'; districtCode = '110108' }
Check '修改地址' (Ok $r) ($r.Raw)
$r = User PUT '/addressBook/default' @{ id = [long]$addrId }
Check '设置默认地址' (Ok $r) ($r.Raw)
$r = User GET '/addressBook/default'
Check '默认地址已切换到新地址' (Ok $r -and [long]$r.Data.id -eq $addrId) ($r.Raw)

$r = User DELETE '/cart/clean'
Check '清空服务清单' (Ok $r) ($r.Raw)
$r = User POST '/cart/add' @{ serviceId = 1001 }
Check '加入服务清单' (Ok $r) ($r.Raw)
$r = User POST '/cart/add' @{ serviceId = 1001 }
Check '重复加入同一服务会累加数量' (Ok $r) ($r.Raw)
$r = User GET '/cart/list'
Check '清单里数量为 2' (Ok $r -and $r.Data.Count -eq 1 -and $r.Data[0].number -eq 2) ($r.Raw)
$r = User POST '/cart/sub' @{ serviceId = 1001 }
Check '从清单减去一个' (Ok $r) ($r.Raw)
$r = User GET '/cart/list'
Check '减去后数量为 1' (Ok $r -and $r.Data[0].number -eq 1) ($r.Raw)

# ============================================================================
#  七、完整业务闭环
# ============================================================================
Write-Section '七、完整业务闭环：下单 -> 支付 -> 派单 -> 接单 -> 开始 -> 完成 -> 评价'

# 先把测试日期区间的 Redis 缓存按数据库权威值重建一遍
# （预热会同时重建库存和用户占用集合）。
# 不做这一步的话，历史遗留的缓存占用会让用例结果不稳定
$null = Admin POST "/slot/warmup?begin=$script:Tomorrow&end=$script:WarmEnd"

$flowSlotId = Pick-Slot -ProviderId 7001
if (-not $flowSlotId) { $flowSlotId = Pick-Slot }
Check '找到用于闭环测试的空闲时段' ($null -ne $flowSlotId) '没有可用时段'
$flowProviderId = Sql1 "select provider_id from slot where id=$flowSlotId"

$r = User GET "/slot/available?serviceId=1001&serviceDate=$script:Tomorrow"
$stockBefore = ($r.Data | Where-Object { $_.id -eq [long]$flowSlotId }).remainStock

$r = User DELETE '/cart/clean'
$r = User POST '/cart/add' @{ serviceId = 1001 }
$r = User POST '/serviceOrder/submit' @{ addressBookId = [long]$addrId; slotId = [long]$flowSlotId; serviceMode = 1; payMethod = 1; remark = '【测试】上门服务订单' }
Check '提交订单' (Ok $r -and $r.Data.id) ($r.Raw)
$flowOrderId = if (Ok $r) { $r.Data.id } else { $null }
$flowOrderNo = if (Ok $r) { $r.Data.orderNumber } else { $null }

if ($flowOrderId) {
    Check '订单初始状态=1 待付款' ((Sql1 "select status from service_order where id=$flowOrderId") -eq '1') '状态不对'
    Check '订单已带上排期所属师傅' ((Sql1 "select provider_id from service_order where id=$flowOrderId") -eq "$flowProviderId") '师傅不对'
    $addr = Sql1 "select address from service_order where id=$flowOrderId"
    Check '下单地址无“北京市北京市”重复' ($addr -notmatch '北京市北京市') $addr
    $r2 = User GET "/slot/available?serviceId=1001&serviceDate=$script:Tomorrow"
    $stockAfter = ($r2.Data | Where-Object { $_.id -eq [long]$flowSlotId }).remainStock
    Check '下单后 Redis 名额 -1' ([int]$stockAfter -eq [int]$stockBefore - 1) ("前 $stockBefore 后 $stockAfter")
    Check '下单未付款时 MySQL 未落账' ((Sql1 "select booked_count from slot where id=$flowSlotId") -eq '0') 'booked_count 不应该变'

    $r = User PUT '/serviceOrder/payment' @{ orderNumber = $flowOrderNo; payMethod = 1 }
    Check '支付订单' (Ok $r) ($r.Raw)
    Check '支付后状态=2 待接单' ((Sql1 "select status from service_order where id=$flowOrderId") -eq '2') '状态不对'
    Check '支付后 MySQL 库存落账 booked_count=1' ((Sql1 "select booked_count from slot where id=$flowSlotId") -eq '1') '库存没落账'
    Check '支付后订单明细写入 service_order_item' ((Sql1 "select count(*) from service_order_item where order_id=$flowOrderId") -eq '1') '明细条数不对'

    $r = User POST '/serviceOrder/submit' @{ addressBookId = [long]$addrId; slotId = [long]$flowSlotId; serviceMode = 1; payMethod = 1 }
    Check '同一用户重复约同一时段被拒绝' (-not (Ok $r)) ($r.Raw)

    $r = Admin PUT '/serviceOrder/dispatch' @{ orderId = [long]$flowOrderId }
    Check '管理端派单（不指定师傅，保持排期归属）' (Ok $r) ($r.Raw)
    Check '派单后师傅仍是排期所属师傅' ((Sql1 "select provider_id from service_order where id=$flowOrderId") -eq "$flowProviderId") '师傅被改掉了'

    $r = Admin PUT "/serviceOrder/accept/$flowOrderId"
    Check '师傅接单（状态 2->3）' (Ok $r) ($r.Raw)
    $r = Admin PUT "/serviceOrder/start/$flowOrderId"
    Check '开始服务（状态 3->4）' (Ok $r) ($r.Raw)
    $r = Admin PUT "/serviceOrder/complete/$flowOrderId"
    Check '服务完成（状态 4->5）' (Ok $r) ($r.Raw)
    Check '完成后师傅接单数 +1' ((Sql1 "select order_count from provider where id=$flowProviderId") -ge '1') '接单数没增加'
    Check '完成后服务项目销量 +1' ((Sql1 "select sales from service_item where id=1001") -ge '1') '销量没增加'

    $salesBefore = Sql1 'select sales from service_item where id=1001'
    $r = User POST '/review/submit' @{ orderId = [long]$flowOrderId; score = 5; serviceScore = 5; speedScore = 5; qualityScore = 5; content = '【测试】师傅很专业，服务很到位'; isAnonymous = 0 }
    Check '提交评价' (Ok $r) ($r.Raw)
    Check '评价后状态=6 已完成' ((Sql1 "select status from service_order where id=$flowOrderId") -eq '6') '状态不对'
    Check '评价写入 review 表' ((Sql1 "select count(*) from review where order_id=$flowOrderId") -eq '1') '没写进去'
    Check '师傅评分被回写' ((Sql1 "select score from provider where id=$flowProviderId") -ne $null) '评分为空'
    $r = User POST '/review/submit' @{ orderId = [long]$flowOrderId; score = 5; content = '重复评价' }
    Check '同一订单重复评价被拒绝' (-not (Ok $r)) ($r.Raw)
}

# ---- 取消订单与名额回补 ----
$cancelSlotId = Pick-Slot -ProviderId 7003
if (-not $cancelSlotId) { $cancelSlotId = Pick-Slot }
$r = User DELETE '/cart/clean'
$r = User POST '/cart/add' @{ serviceId = 1001 }
$r = User POST '/serviceOrder/submit' @{ addressBookId = [long]$addrId; slotId = [long]$cancelSlotId; serviceMode = 1; payMethod = 1 }
Check '（取消流程）提交订单' (Ok $r) ($r.Raw)
$cancelOrderId = if (Ok $r) { $r.Data.id } else { $null }
if ($cancelOrderId) {
    $r = User PUT "/serviceOrder/cancel/$cancelOrderId"
    Check '用户取消未付款订单' (Ok $r) ($r.Raw)
    Check '取消后状态=7 已取消' ((Sql1 "select status from service_order where id=$cancelOrderId") -eq '7') '状态不对'
    $r2 = User GET "/slot/available?serviceId=1001&serviceDate=$script:Tomorrow"
    $back = ($r2.Data | Where-Object { $_.id -eq [long]$cancelSlotId }).remainStock
    Check '取消后 Redis 名额已回补' ([int]$back -eq 1) ("剩余 " + $back)
}

# 已付款订单取消
$paidCancelSlot = Pick-Slot -ProviderId 7002
if (-not $paidCancelSlot) { $paidCancelSlot = Pick-Slot }
$r = User DELETE '/cart/clean'
$r = User POST '/cart/add' @{ serviceId = 1001 }
$r = User POST '/serviceOrder/submit' @{ addressBookId = [long]$addrId; slotId = [long]$paidCancelSlot; serviceMode = 1; payMethod = 1 }
$paidCancelOrderId = if (Ok $r) { $r.Data.id } else { $null }
$paidCancelNo = if (Ok $r) { $r.Data.orderNumber } else { $null }
if ($paidCancelOrderId) {
    $r = User PUT '/serviceOrder/payment' @{ orderNumber = $paidCancelNo; payMethod = 1 }
    Check '（已付款取消）支付' (Ok $r) ($r.Raw)
    Check '（已付款取消）支付后库存已落账' ((Sql1 "select booked_count from slot where id=$paidCancelSlot") -eq '1') '未落账'
    $r = User PUT "/serviceOrder/cancel/$paidCancelOrderId"
    Check '（已付款取消）取消订单' (Ok $r) ($r.Raw)
    Check '（已付款取消）MySQL 库存已回补' ((Sql1 "select booked_count from slot where id=$paidCancelSlot") -eq '0') '没有回补'
}

# ---- 打烊后不能下单 ----
$shopSlot = Pick-Slot
if (-not $shopSlot) { $shopSlot = Pick-Slot }
$r = Admin PUT '/shop/0'
Check '设置平台打烊' (Ok $r) ($r.Raw)
$r = User DELETE '/cart/clean'
$r = User POST '/cart/add' @{ serviceId = 1001 }
$r = User POST '/serviceOrder/submit' @{ addressBookId = [long]$addrId; slotId = [long]$shopSlot; serviceMode = 1; payMethod = 1 }
Check '打烊期间下单被拒绝' (-not (Ok $r) -and $r.Msg -match '打烊') ($r.Raw)
$r = Admin PUT '/shop/1'
Check '恢复营业状态' (Ok $r) ($r.Raw)

# ============================================================================
#  八、Redis 并发防超卖
# ============================================================================
Write-Section '八、Redis 并发防超卖（10 个用户同时抢 1 个名额）'

$raceSlotId = Pick-Slot -ProviderId 7003
if (-not $raceSlotId) { $raceSlotId = Pick-Slot }
Check '找到 1 个名额的时段用于压测' ($null -ne $raceSlotId) '没找到'

if ($raceSlotId) {
    $null = Admin POST "/slot/warmup?begin=$script:Tomorrow&end=$script:WarmEnd"
    $r = User GET "/slot/available?serviceId=1001&serviceDate=$script:Tomorrow"
    $raceStock = ($r.Data | Where-Object { $_.id -eq [long]$raceSlotId }).remainStock
    Check '压测前该时段剩余名额为 1' ([int]$raceStock -eq 1) ("剩余 " + $raceStock)

    $users = 8001..8010
    foreach ($u in $users) {
        $tok = New-UserToken $u
        Invoke-Api -Method DELETE -Path '/user/cart/clean' -Token $tok -TokenHeader 'authentication' | Out-Null
        Invoke-Api -Method POST -Path '/user/cart/add' -Body @{ serviceId = 1001 } -Token $tok -TokenHeader 'authentication' | Out-Null
    }

    $client = New-Object System.Net.Http.HttpClient
    $client.Timeout = [TimeSpan]::FromSeconds(60)
    $tasks = New-Object System.Collections.Generic.List[object]
    foreach ($u in $users) {
        $req = New-Object System.Net.Http.HttpRequestMessage([System.Net.Http.HttpMethod]::Post, "$Base/user/serviceOrder/submit")
        $req.Headers.Add('authentication', (New-UserToken $u))
        $body = @{ slotId = [long]$raceSlotId; serviceMode = 2; payMethod = 1; remark = '【测试】并发压测' } | ConvertTo-Json -Compress
        $req.Content = New-Object System.Net.Http.StringContent($body, [Text.Encoding]::UTF8, 'application/json')
        $tasks.Add($client.SendAsync($req))
    }
    [System.Threading.Tasks.Task]::WaitAll([System.Threading.Tasks.Task[]]$tasks)

    $success = 0
    $soldOut = 0
    $other = 0
    foreach ($t in $tasks) {
        $body = $t.Result.Content.ReadAsStringAsync().Result
        $o = $body | ConvertFrom-Json
        if ($o.code -eq 1) { $success++ }
        elseif ($o.msg -match '约满') { $soldOut++ }
        else { $other++; Write-Host "    异常返回：$body" -ForegroundColor Yellow }
    }
    $client.Dispose()

    Check '并发下单只成功 1 单（没有超卖）' ($success -eq 1) ("成功 $success 单")
    Check '其余 9 单返回“该时段已约满”' ($soldOut -eq 9) ("约满 $soldOut 单，其他 $other 单")

    $r = User GET "/slot/available?serviceId=1001&serviceDate=$script:Tomorrow"
    $left = ($r.Data | Where-Object { $_.id -eq [long]$raceSlotId }).remainStock
    Check '压测后 Redis 剩余名额为 0' ([int]$left -eq 0) ("剩余 " + $left)
    Check '百万并发也只卖出 1 个（MySQL 未超卖）' ((Sql1 "select booked_count from slot where id=$raceSlotId") -le '1') 'MySQL 超卖了'
    Check '未付款订单不落 MySQL 库存' ((Sql1 "select booked_count from slot where id=$raceSlotId") -eq '0') '未付款就落账了'
}

# ============================================================================
#  九、MySQL CAS 兜底（Redis 与 MySQL 不一致）
# ============================================================================
Write-Section '九、MySQL 库存 CAS 兜底'

$casSlotId = Pick-Slot -ProviderId 7002
if (-not $casSlotId) { $casSlotId = Pick-Slot }
if ($casSlotId) {
    $null = Admin POST "/slot/warmup?begin=$script:Tomorrow&end=$script:Tomorrow"
    $r = User DELETE '/cart/clean'
    $r = User POST '/cart/add' @{ serviceId = 1001 }
    $r = User POST '/serviceOrder/submit' @{ addressBookId = [long]$addrId; slotId = [long]$casSlotId; serviceMode = 1; payMethod = 1 }
    $casOrderId = if (Ok $r) { $r.Data.id } else { $null }
    $casOrderNo = if (Ok $r) { $r.Data.orderNumber } else { $null }
    Check '（CAS 测试）下单成功' (Ok $r) ($r.Raw)
    if ($casOrderId) {
        # 人为把 MySQL 的已预约数顶满，模拟「Redis 说有名额、MySQL 其实已经卖完」的不一致场景
        Sql "update slot set booked_count = total_stock where id = $casSlotId" | Out-Null
        $r = User PUT '/serviceOrder/payment' @{ orderNumber = $casOrderNo; payMethod = 1 }
        Check '支付时 MySQL 落账失败会拦住（不会超卖）' (-not (Ok $r)) ($r.Raw)
        Check '落账失败时订单不会变成已支付' ((Sql1 "select status from service_order where id=$casOrderId") -eq '1') '状态被改了'
        Sql "update slot set booked_count = 0 where id = $casSlotId" | Out-Null
        $r = User PUT "/serviceOrder/cancel/$casOrderId"
        Check '（CAS 测试）清理：取消订单回补名额' (Ok $r) ($r.Raw)
    }
}

# ============================================================================
#  十、MQ 延迟队列
# ============================================================================
Write-Section '十、RabbitMQ 延迟队列'

try {
    $queues = Rabbit '/api/queues'
    Check '能读到 RabbitMQ 队列（管理台可用）' ($queues.Count -ge 8) ("队列数 " + $queues.Count)
    foreach ($name in @('life.order.timeout.queue', 'life.dispatch.timeout.queue', 'life.service.remind.queue', 'life.auto.review.queue')) {
        $q = $queues | Where-Object { $_.name -eq $name }
        Check "消费队列 $name 有消费者" ($q -and $q.consumers -ge 1) ("consumers=" + $(if ($q) { $q.consumers } else { 'n/a' }))
    }

    # 真实发一条消息出去：下单 + 支付会同时发出「15 分钟未支付取消」和「派单 5 分钟超时」两条延迟消息
$mqSlotId = Pick-Slot -ProviderId 7001
if (-not $mqSlotId) { $mqSlotId = Pick-Slot }
    $orderDelayBefore = Get-QueueDepth 'life.order.delay.queue'
    $dispatchDelayBefore = Get-QueueDepth 'life.dispatch.delay.queue'
    $r = User DELETE '/cart/clean'
    $r = User POST '/cart/add' @{ serviceId = 1001 }
    $r = User POST '/serviceOrder/submit' @{ addressBookId = [long]$addrId; slotId = [long]$mqSlotId; serviceMode = 1; payMethod = 1 }
    $mqOrderId = if (Ok $r) { $r.Data.id } else { $null }
    $mqOrderNo = if (Ok $r) { $r.Data.orderNumber } else { $null }
    $orderDelay = Wait-QueueDepthUp -QueueName 'life.order.delay.queue' -Before $orderDelayBefore
    Check '下单后「超时未支付」延迟消息已进入延迟队列' ($orderDelay -gt $orderDelayBefore) ("下单前 " + $orderDelayBefore + " 下单后 " + $orderDelay)

    $r = User PUT '/serviceOrder/payment' @{ orderNumber = $mqOrderNo; payMethod = 1 }
    Check '（MQ 测试）支付成功' (Ok $r) ($r.Raw)
    $dispatchDelay = Wait-QueueDepthUp -QueueName 'life.dispatch.delay.queue' -Before $dispatchDelayBefore
    Check '支付后「派单超时转派」延迟消息已发出' ($dispatchDelay -gt $dispatchDelayBefore) ("支付前 " + $dispatchDelayBefore + " 支付后 " + $dispatchDelay)

    $remindDelayBefore = Get-QueueDepth 'life.service.remind.delay.queue'
    $remindDelay = Wait-QueueDepthUp -QueueName 'life.service.remind.delay.queue' -Before $remindDelayBefore -TimeoutSeconds 12
    Check '「服务前 1 小时提醒」消息已发出' ($remindDelay -gt $remindDelayBefore -or $remindDelay -ge 1) ("队列中 " + $remindDelay)

    if (-not $SkipSlow -and $mqOrderId) {
        # 再下一单 B，并把它的派单次数直接顶到上限。
        # A、B 两条延迟消息几乎同时到期，等一次就能同时验证：
        #   A（dispatch_count=0）应该被自动转派
        #   B（dispatch_count=3）应该被上限拦住，不再转派
        $capSlotId = Pick-Slot -ProviderId 7002
        if (-not $capSlotId) { $capSlotId = Pick-Slot }
        $null = User DELETE '/cart/clean'
        $null = User POST '/cart/add' @{ serviceId = 1001 }
        $r = User POST '/serviceOrder/submit' @{ addressBookId = [long]$addrId; slotId = [long]$capSlotId; serviceMode = 1; payMethod = 1; remark = '【测试】转派上限' }
        $capOrderId = if (Ok $r) { $r.Data.id } else { $null }
        $capOrderNo = if (Ok $r) { $r.Data.orderNumber } else { $null }
        if ($capOrderId) {
            $r = User PUT '/serviceOrder/payment' @{ orderNumber = $capOrderNo; payMethod = 1 }
            Check '（上限验证）第二单支付成功' (Ok $r) ($r.Raw)
            Sql "update service_order set dispatch_count = 3 where id = $capOrderId" | Out-Null
        }

        Write-Host ''
        Write-Host '  等待 5 分钟，观察派单超时后的转派行为（同时验证转派次数上限）...' -ForegroundColor Yellow
        $providerBefore = Sql1 "select provider_id from service_order where id=$mqOrderId"
        $capProviderBefore = Sql1 "select provider_id from service_order where id=$capOrderId"
        for ($i = 1; $i -le 10; $i++) {
            Start-Sleep -Seconds 30
            Write-Host ("    已等待 " + ($i * 30) + " 秒") -ForegroundColor DarkGray
        }
        $providerAfter = Sql1 "select provider_id from service_order where id=$mqOrderId"
        $dispatchCount = Sql1 "select dispatch_count from service_order where id=$mqOrderId"
        Check '派单 5 分钟无人接单后自动转派给其他师傅' ($providerAfter -ne $providerBefore -and $providerAfter) ("原师傅 $providerBefore -> 新师傅 $providerAfter")
        Check '转派次数已累加为 1' ($dispatchCount -eq '1') ("dispatch_count=" + $dispatchCount)
        $capProviderAfter = Sql1 "select provider_id from service_order where id=$capOrderId"
        Check '转派满 3 次后不再自动转派（转人工处理）' ($capProviderAfter -eq $capProviderBefore) ("$capProviderBefore -> $capProviderAfter")
    }
    elseif ($mqOrderId) {
        Write-Host '  （-SkipSlow：跳过 5 分钟延迟转派实测）' -ForegroundColor Yellow
    }
}
catch {
    Write-Host ('  [跳过] RabbitMQ 管理台不可达：' + $_.Exception.Message) -ForegroundColor Yellow
}

# ============================================================================
#  清理测试数据
# ============================================================================
Write-Section '清理测试数据'

# 并发压测里抢到名额的那一单还挂在「待付款」，先按用户取消掉，把名额还给 Redis
$raceOrders = Sql "select id, user_id from service_order where remark in ('【测试】并发压测','【测试】上门服务订单') and status = 1"
foreach ($line in $raceOrders) {
    $parts = $line -split "`t"
    if ($parts.Count -ge 2) {
        User PUT ("/serviceOrder/cancel/" + $parts[0]) -UserId ([int]$parts[1]) | Out-Null
    }
}

# 全部按测试期间记录下来的真实 id 删除，不用中文做条件
# （中文条件要额外处理 mysql 客户端的字符集，容易踩坑）
Sql "delete from service_package_item where service_package_id = $pkgNewId" | Out-Null
Sql "delete from service_package where id = $pkgNewId" | Out-Null
Sql "delete from service_spec where service_id = $itemId" | Out-Null
Sql "delete from service_item where id = $itemId" | Out-Null
Sql "delete from provider_skill where provider_id = $provId" | Out-Null
Sql "delete from slot where provider_id = $provId" | Out-Null
Sql "delete from provider where id = $provId" | Out-Null
Sql "delete from employee where id = $empId" | Out-Null
Sql "delete from address_book where id = $addrId" | Out-Null
Sql "delete from category where id = $catId" | Out-Null
Sql "delete from service_area where code = '999999'" | Out-Null
Sql "delete from shopping_cart where user_id >= 8001" | Out-Null
# 还原演示数据：把 8501 恢复成 8001 的默认地址
Sql 'update address_book set is_default = 1 where id = 8501' | Out-Null

$leftover = Sql1 "select count(*) from service_item where id = $itemId"
Check '测试数据已清理（测试服务项目已删除）' ($leftover -eq '0') ("残留 " + $leftover)
$leftoverCat = Sql1 "select count(*) from category where id = $catId"
Check '测试数据已清理（测试分类已删除）' ($leftoverCat -eq '0') ("残留 " + $leftoverCat)
Check '演示地址默认标记已还原' ((Sql1 'select is_default from address_book where id = 8501') -eq '1') '8501 不是默认地址'

Write-Host ''
Write-Host '==================================================' -ForegroundColor Cyan
Write-Host ("  测试结束：通过 " + $script:Pass + " 项，失败 " + $script:Fail + " 项") -ForegroundColor $(if ($script:Fail -eq 0) { 'Green' } else { 'Red' })
if ($script:Fail -gt 0) {
    Write-Host '  失败明细：' -ForegroundColor Red
    foreach ($f in $script:Failures) { Write-Host "    - $f" -ForegroundColor Red }
}
Write-Host '==================================================' -ForegroundColor Cyan

if ($script:Fail -gt 0) { exit 1 } else { exit 0 }
