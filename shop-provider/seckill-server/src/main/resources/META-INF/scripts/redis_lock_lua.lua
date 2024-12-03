-- string key
local stringKey = KEYS[1]
--string value
local stringVal =tonumber(ARGV[1])
--过期时间
local expireAt =tonumber(ARGV[2])
--check 值是否已存在，不存在先插入key，并初始化值
local keyExist = redis.call("SETNX",KEYS[1],stringVal);
if(keyExist>= 1)then
    --设置过期时间
    redis.call("EXPIREAT",KEYS[1],expireAt)
end
return tonumber(keyExist)