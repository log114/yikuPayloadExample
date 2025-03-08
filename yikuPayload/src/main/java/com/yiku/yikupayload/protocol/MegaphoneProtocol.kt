package com.yiku.yikupayload.protocol;

// 实时喊话
const val REAL_TIME_SHOUT = "[10]"

// 停止实时喊话
const val STOP_REAL_TIME_SHOUT = "[11]"

// 录音播放
const val AUDIO_PLAY = "[12]"

// 停止录音播放
const val STOP_AUDIO_PLAY = "[13]"

// 设置音量
const val SET_VOLUME = "[14]"

// tts播放
const val TTS_PLAY = "[15]"

// tts循环播放
const val TTS_LOOP_PLAY = "[16]"

// 停止tts循环播放
const val STOP_TTS_LOOP_PLAY = "[17]"

// 警报播放
const val ALARM_PLAY = "[18]"

// 停止警报播放
const val STOP_ALARM_PLAY = "[19]"

// 开始上传文件
const val BEGIN_UPLOAD_AUDIO = "[20]"

// 上传文件
const val UPLOAD_AUDIO = "[21]"

// 获取录音文件
const val GET_AUDIO_FILES = "[22]"

// 删除录音文件
const val AUDIO_DEL = "[23]"

const val TTS_PLAY_V2 = "[31]"
const val TTS_LOOP_PLAY_V2 = "[32]"

const val START_RADIO = "[40]"

const val STOP_RADIO = "[41]"
const val DISABLE_RADIO = "[45]"
const val RESTART_RADIO = "[46]"
const val REBOOT = "[88]"