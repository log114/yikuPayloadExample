package com.yiku.yikupayload.protocol

/* 缓降器使能控制
* 发送1个字节
* byte0: 0: Disable缓降器，1: Enable缓降器
* */
const val DESCENT_CONTROL = 0x12

/* 缓降器紧急控制
* 发送1个字节
* byte0: 0: 解除紧急状态，1: 紧急停车，2: 紧急熔断
* */
const val DESCENT_URGENT_CONTROL = 0x13

/* 缓降器动作控制
* 发送3个字节
* byte0: 0: 长度控制模式(0 ~ 300分米)，1: 速度控制模式(-20 ~ +20米/分钟)（实际传0~40，0是-20米/分钟，40是20米/分钟）
* byte1: 目标长度或速度高8位
* byte2: 目标长度或速度低8位
* */
const val DESCENT_ACTION_CONTROL = 0x14

/* 缓降器状态返回
* 无需发送，返回8个字节
* byte0: 0：缓降器已Disable，1: 缓降器已Enable
* byte1: 0: 长度控制模式，1: 速度控制模式
* byte2: 当前速度m/min
* byte3: 已释放长度高8位
* byte4: 已释放长度低8位
* byte5: 0: 缓降器未到限位，1: 缓降器已到顶，2: 缓降器已到底
* byte6: 载重 kg/LSB
* byte7:
* 0: 已解除紧急状态
* 1: 紧急刹车
* 2: 紧急熔断
* 3: 紧急刹车+熔断
* */
const val DESCENT_STATE_GET = 0x15