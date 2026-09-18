--[[
  时段名额回补脚本

  订单取消 / 超时未支付 / 师傅拒单时调用，把之前预扣的名额还回去。

  为什么要用 SREM 的返回值做判断：
    回补必须幂等。如果订单取消的接口被重复调用（用户连点、或者 MQ
    消息重复投递），没有这道判断的话会把名额越加越多，最后凭空多出
    一堆订单。SREM 返回 1 表示这个用户确实在集合里（第一次回补），
    返回 0 表示已经回补过了，直接跳过。

  KEYS[1] 时段库存 key
  KEYS[2] 已预约用户集合 key
  ARGV[1] 用户id

  返回值：
     1  回补成功
     0  已经回补过，本次跳过（幂等）
    -1  库存 key 不存在（可能已过期，无需回补）
]]

local stockKey = KEYS[1]
local userSetKey = KEYS[2]
local userId = ARGV[1]

-- 库存 key 都没了就不用回补了
if redis.call('EXISTS', stockKey) == 0 then
    return -1
end

-- SREM 返回实际移除的元素个数，用它来实现幂等
local removed = redis.call('SREM', userSetKey, userId)
if removed == 1 then
    redis.call('INCR', stockKey)
    return 1
end

return 0
