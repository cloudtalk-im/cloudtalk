package com.zhangwuji.im.utils;

import android.graphics.Color;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.TextView.BufferType;


import com.zhangwuji.im.DB.entity.Department;
import com.zhangwuji.im.DB.entity.Group;
import com.zhangwuji.im.DB.entity.User;
import com.zhangwuji.im.config.SysConstant;
import com.zhangwuji.im.imcore.entity.SearchElement;
import com.zhangwuji.im.utils.pinyin.PinYin.PinYinElement;

public class IMUIHelper {


    // 对话框回调函数
    public interface dialogCallback{
        public void callback();
    }
    // 文字高亮显示
    public static void setTextHilighted(TextView textView, String text,SearchElement searchElement) {
        textView.setText(text);
        if (textView == null
                || TextUtils.isEmpty(text)
                || searchElement ==null) {
            return;
        }

        int startIndex = searchElement.startIndex;
        int endIndex = searchElement.endIndex;
        if (startIndex < 0 || endIndex > text.length()) {
            return;
        }
        // 开始高亮处理
        int color =  Color.rgb(69, 192, 26);
        textView.setText(text, BufferType.SPANNABLE);
        Spannable span = (Spannable) textView.getText();
        span.setSpan(new ForegroundColorSpan(color), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }


    /**
     * 如果图片路径是以  http开头,直接返回
     * 如果不是， 需要集合自己的图像路径生成规律
     * @param avatarUrl
     * @return
     */
    public static String getRealAvatarUrl(String avatarUrl) {
        if (avatarUrl.toLowerCase().contains("http")) {
            return avatarUrl;
        } else if (avatarUrl.trim().isEmpty()) {
            return "";
        } else {
            return SysConstant.AVATAR_URL_PREFIX + avatarUrl;
        }
    }



    // search helper start
	public static boolean handleDepartmentSearch(String key, Department department) {
		if (TextUtils.isEmpty(key) || department == null) {
			return false;
		}
		department.getSearchElement().reset();

		return handleTokenFirstCharsSearch(key, department.getPinyinElement(), department.getSearchElement())
		|| handleTokenPinyinFullSearch(key, department.getPinyinElement(), department.getSearchElement())
		|| handleNameSearch(department.getDepartName(), key, department.getSearchElement());
	}


	public static boolean handleGroupSearch(String key, Group group) {
		if (TextUtils.isEmpty(key) || group == null) {
			return false;
		}
		group.getSearchElement().reset();

		return handleTokenFirstCharsSearch(key, group.getPinyinElement(), group.getSearchElement())
		|| handleTokenPinyinFullSearch(key, group.getPinyinElement(), group.getSearchElement())
		|| handleNameSearch(group.getMainName(), key, group.getSearchElement());
	}

	public static boolean handleContactSearch(String key, User contact) {
		if (TextUtils.isEmpty(key) || contact == null) {
			return false;
		}

		contact.getSearchElement().reset();

		return handleTokenFirstCharsSearch(key, contact.getPinyinElement(), contact.getSearchElement())
		|| handleTokenPinyinFullSearch(key, contact.getPinyinElement(), contact.getSearchElement())
		|| handleNameSearch(contact.getMainName(), key, contact.getSearchElement());
        // 原先是 contact.name 代表花名的意思嘛??
	}

	public static boolean handleNameSearch(String name, String key,
			SearchElement searchElement) {
		int index = name.indexOf(key);
		if (index == -1) {
			return false;
		}

		searchElement.startIndex = index;
		searchElement.endIndex = index + key.length();

		return true;
	}

	public static boolean handleTokenFirstCharsSearch(String key, PinYinElement pinYinElement, SearchElement searchElement) {
		return handleNameSearch(pinYinElement.tokenFirstChars, key.toUpperCase(), searchElement);
	}

	public static boolean handleTokenPinyinFullSearch(String key, PinYinElement pinYinElement, SearchElement searchElement) {
		if (TextUtils.isEmpty(key)) {
			return false;
		}

		String searchKey = key.toUpperCase();

		//onLoginOut the old search result
		searchElement.reset();

		int tokenCnt = pinYinElement.tokenPinyinList.size();
		int startIndex = -1;
		int endIndex = -1;

		for (int i = 0; i < tokenCnt; ++i) {
			String tokenPinyin = pinYinElement.tokenPinyinList.get(i);

			int tokenPinyinSize = tokenPinyin.length();
			int searchKeySize = searchKey.length();

			int keyCnt = Math.min(searchKeySize, tokenPinyinSize);
			String keyPart = searchKey.substring(0, keyCnt);

			if (tokenPinyin.startsWith(keyPart)) {

				if (startIndex == -1) {
					startIndex = i;
				}

				endIndex = i + 1;
			} else {
				continue;
			}

			if (searchKeySize <= tokenPinyinSize) {
				searchKey = "";
				break;
			}

			searchKey = searchKey.substring(keyCnt, searchKeySize);
		}

		if (!searchKey.isEmpty()) {
			return false;
		}

		if (startIndex >= 0 && endIndex > 0) {
			searchElement.startIndex = startIndex;
			searchElement.endIndex = endIndex;

			return true;
		}

		return false;
	}

    // search helper end



	public static void setViewTouchHightlighted(final View view) {
		if (view == null) {
			return;
		}

		view.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					view.setBackgroundColor(Color.rgb(1, 175, 244));
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					view.setBackgroundColor(Color.rgb(255, 255, 255));
				}
				return false;
			}
		});
	}




    // 这个还是蛮有用的,方便以后的替换
	public static int getDefaultAvatarResId(int sessionType) {
    	return 0;
	}



}
