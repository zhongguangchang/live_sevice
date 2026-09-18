--[[
  时段名额预扣脚本（防超卖的核心）

  为什么必须用 Lua：
    如果用 Java 写「先 GET 判断，再 DECR 扣减」，这两步之间会有时间窗口，
    并发请求同时通过 GET 判断时都会认为有名额，结果就超卖了。
    Redis 执行 Lua 脚本是原子的（单线程模型保证脚本执行期间不会插入
    其他命令），所以「校验 + 扣减 + 记录」三步必须写在一个脚本里。

  KEYS[1] 时段库存 key        slot:stock:{slotId}
  KEYS[2] 已预约用户集合 key   slot:users:{slotId}
  ARGV[1] 用户id

  返回值：
    >= 0  扣减成功，返回剩余名额
    -1    库存未初始化（说明这个时段还没预热到 Redis）
    -2    名额已抢完
    -3    该用户已经约过这个时段
]]

local stockKey = KEYS[1]
local userSetKey = KEYS[2]
local userId = ARGV[1]

-- 1. 校验库存是否已预热
local stock = redis.call('GET', stockKey)
if stock == false then
    return -1
end

-- 2. 校验名额是否还有剩余
if tonumber(stock) <= 0 then
    return -2
end

-- 3. 校验该用户是否已经约过这个时段（一人一时段一单）
if redis.call('SISMEMBER', userSetKey, userId) == 1 then
    return -3
end

-- 4. 扣减名额并记录用户
--    这两步必须在同一个脚本里，否则并发下仍可能出现
--   「扣了名额但没记录用户」导致同一个人重复下单
redis.call('DECR', stockKey)
redis.call('SADD', userSetKey, userId)

-- 5. 给用户集合设置过期时间，避免长期占用内存
--    过期时间要覆盖「服务日期 + 几个月的纠纷追溯期」，这里设 7 天
redis.call('EXPIRE', userSetKey, 604800)

return tonumber(stock) - 1
