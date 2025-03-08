package com.yiku.yikupayload.protocol;

const val OPEN_CLOSE_LIGHT: Byte = 0x01 // 开关
const val LUMINANCE_CHANGE: Byte = 0x02 // 音量
const val SHARP_FLASH: Byte = 0x03      // 爆闪
const val FETCH_TEMPERATURE: Byte = 0x04// 获取温度
const val TRIPOD_HEAD: Byte = 0x06      // 云台控制(探照灯用)
const val RED_BLUE_FLASHES:Byte = 0x07  // 红蓝
const val SEROV_CONTROL = 0x09          // 俯仰控制(四合一灯用)