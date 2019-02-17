package com.zhangwuji.im.ui.entity;

/**
 * Created by jackkim on 2018/8/8.
 */

public class IMCallCommon {

    public static enum CallVideoProfile {
        VIDEO_PROFILE_240P(20),
        VIDEO_PROFILE_360P(30),
        VIDEO_PROFILE_480P(40),
        VIDEO_PROFILE_720P(50);

        private int value;

        private CallVideoProfile(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }

        public static IMCallCommon.CallVideoProfile valueOf(int value) {
            IMCallCommon.CallVideoProfile[] var1 = values();
            int var2 = var1.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                IMCallCommon.CallVideoProfile v = var1[var3];
                if(v.value == value) {
                    return v;
                }
            }

            return null;
        }
    }

    public static enum CallPermission {
        PERMISSION_AUDIO,
        PERMISSION_CAMERA,
        PERMISSION_AUDIO_AND_CAMERA;

        private CallPermission() {
        }
    }
    public static enum CallStatus {
        OUTGOING(1),
        INCOMING(2),
        RINGING(3),
        CONNECTED(4),
        IDLE(5);

        private int value;

        private CallStatus(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }

        public static IMCallCommon.CallStatus valueOf(int value) {
            IMCallCommon.CallStatus[] var1 = values();
            int var2 = var1.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                IMCallCommon.CallStatus v = var1[var3];
                if(v.value == value) {
                    return v;
                }
            }

            return null;
        }
    }
    public static enum CallErrorCode {
        ENGINE_ERROR(1),
        SIGNAL_ERROR(2);

        private int value;

        private CallErrorCode(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }

        public static IMCallCommon.CallErrorCode valueOf(int value) {
            IMCallCommon.CallErrorCode[] var1 = values();
            int var2 = var1.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                IMCallCommon.CallErrorCode v = var1[var3];
                if(v.value == value) {
                    return v;
                }
            }

            return null;
        }
    }
    public static enum CallMediaType {
        AUDIO(1),
        VIDEO(2);

        private int value;

        private CallMediaType(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }

        public static IMCallCommon.CallMediaType valueOf(int value) {
            IMCallCommon.CallMediaType[] var1 = values();
            int var2 = var1.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                IMCallCommon.CallMediaType v = var1[var3];
                if(v.value == value) {
                    return v;
                }
            }

            return null;
        }
    }
}
