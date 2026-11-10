-- 秒杀预扣库存 Lua：原子判断库存 + 防重复下单 + 扣减
-- KEYS[1] = 库存 key: seckill:stock:{tenantId}:{activityId}
-- KEYS[2] = 用户购买标记: seckill:bought:{tenantId}:{activityId}
-- ARGV[1] = userId
local stock = tonumber(redis.call('get', KEYS[1]) or '0')
if stock <= 0 then
    return 0  -- 售罄
end
if redis.call('sismember', KEYS[2], ARGV[1]) == 1 then
    return -1 -- 重复下单
end
redis.call('decr', KEYS[1])
redis.call('sadd', KEYS[2], ARGV[1])
return 1
