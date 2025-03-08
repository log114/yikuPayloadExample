package com.yiku.yikupayload.protocol

const val THROWER_UPDATE = 0x0A // 抛投板子程序升级，4字节，分别是 空、空、空、空
const val THROWER_CONTROL_ONE = 0x21 // 单舵机动作控制，4字节，分别是 舵机号（0-7）、关闭/开启（0/1）、空、空
const val THROWER_CONTROL_ALL = 0x22 // 全部舵机动作控制，4字节，分别是 关闭/开启（0/1）、空、空、空
const val THROWER_CHARGING = 0x23 // 充电放电，4字节，分别是 放电/充电（0/1）、空、空、空
const val THROWER_ALLOW_DETONATION= 0x24 // 允许起爆，4字节，分别是 取消/允许起爆（0/1）、空、空、空
const val THROWER_STATE = 0x25 // 舵机状态，无需发送，每1秒自动上报一次，6字节，分别是 高度、起爆状态、充电状态、温度、总状态、起爆高度
const val THROWER_CONNECT_TEST = 0x26 // 连接测试，心跳包，定时发送，4字节，分别是 空、空、空、空
const val THROWER_DETONATE_HEIGHT = 0x27 // 设置起爆高度，4字节，分别是 起爆高度、空、空、空
const val THROWER_CONTROL_TWO_CENTER = 0x28 // 双舵机动作控制(中间俩)，4字节，分别是 关闭/开启（0/1）、空、空、空
const val THROWER_CONTROL_TWO_LEFT = 0x29 // 双舵机动作控制(左侧俩1/2)，4字节，分别是 关闭/开启（0/1）、空、空、空
const val THROWER_CONTROL_TWO_RIGHT = 0x2A // 双舵机动作控制(右侧俩7/8)，4字节，分别是 关闭/开启（0/1）、空、空、空

