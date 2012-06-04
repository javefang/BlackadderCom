MY_LOCAL_PATH := $(call my-dir)
LOCAL_PATH := $(MY_LOCAL_PATH)

include $(MY_LOCAL_PATH)/blackadderWrapper/Android.mk
include $(MY_LOCAL_PATH)/libjpeg/Android.mk
#include $(MY_LOCAL_PATH)/platform_external_jpeg/Android.mk
include $(MY_LOCAL_PATH)/speex/Android.mk
include $(MY_LOCAL_PATH)/pubsub/Android.mk
include $(MY_LOCAL_PATH)/netperf/Android.mk

