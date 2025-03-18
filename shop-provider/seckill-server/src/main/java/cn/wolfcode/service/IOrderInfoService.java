package cn.wolfcode.service;


import cn.wolfcode.common.domain.UserInfo;
import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.domain.SeckillProductVo;

import java.util.Map;

/**
 * Created by wolfcode
 */
public interface IOrderInfoService {

    OrderInfo selectByUserIdAndSeckillId(Long phone, Long seckillId);

    String doSeckill(Long phone, SeckillProductVo vo) throws Exception;

    String doSeckill(Long phone, Long seckillId, Integer time);

    OrderInfo selectByOrderNo(String orderNo);
}
