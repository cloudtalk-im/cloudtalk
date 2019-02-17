package com.zhangwuji.im.ui.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.zhangwuji.im.R;
import com.zhangwuji.im.utils.CommonUtil;
import com.zhangwuji.im.utils.ImageLoaderUtil;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.lang.ref.WeakReference;

/**
 * Created by zhujian on 15/1/14.
 */
@SuppressLint("AppCompatCustomView")
public class IMBaseImageView extends ImageView {

    protected String imageUrl="";
    protected int imageId;
    protected boolean isAttachedOnWindow=false;
    protected String avatarAppend= null;
    protected int defaultImageRes = R.drawable.tt_default_user_portrait_corner;
    protected int corner=0;
    private boolean mHasMask;
    private Drawable mDefaultDrawable;
    private WeakReference<Bitmap> mWeakBitmap;
    private WeakReference<Bitmap> mShardWeakBitmap;

    public IMBaseImageView(Context context) {
        super(context);
    }

    public IMBaseImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!this.isInEditMode()) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AsyncImageView);
            this.mHasMask = a.getBoolean(R.styleable.AsyncImageView_RCMask, false);
        }
    }

    public IMBaseImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    protected void onDraw(Canvas canvas) {
        if (this.mHasMask) {
            Bitmap bitmap = this.mWeakBitmap == null ? null : (Bitmap)this.mWeakBitmap.get();
            Drawable drawable = this.getDrawable();
            CTMessageFrameLayout parent = (CTMessageFrameLayout)this.getParent();
            Drawable background = parent.getBackgroundDrawable();
            if (bitmap != null && !bitmap.isRecycled()) {
                canvas.drawBitmap(bitmap, 0.0F, 0.0F, (Paint)null);
                this.getShardImage(background, bitmap, canvas);
            } else {
                int width = this.getWidth();
                int height = this.getHeight();
                if (width <= 0 || height <= 0) {
                    return;
                }

                try {
                    bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                } catch (OutOfMemoryError var11) {
                    var11.printStackTrace();
                    System.gc();
                }

                if (bitmap != null) {
                    Canvas rCanvas = new Canvas(bitmap);
                    if (drawable != null) {
                        drawable.setBounds(0, 0, width, height);
                        drawable.draw(rCanvas);
                        if (background != null && background instanceof NinePatchDrawable) {
                            NinePatchDrawable patchDrawable = (NinePatchDrawable)background;
                            patchDrawable.setBounds(0, 0, width, height);
                            Paint maskPaint = patchDrawable.getPaint();
                            maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
                            patchDrawable.draw(rCanvas);
                        }

                        this.mWeakBitmap = new WeakReference(bitmap);
                    }

                    canvas.drawBitmap(bitmap, 0.0F, 0.0F, (Paint)null);
                    this.getShardImage(background, bitmap, canvas);
                }
            }
        } else {
            super.onDraw(canvas);
        }

    }
    private void getShardImage(Drawable drawable_bg, Bitmap bp, Canvas canvas) {
        int width = bp.getWidth();
        int height = bp.getHeight();
        Bitmap bitmap = this.mShardWeakBitmap == null ? null : (Bitmap)this.mShardWeakBitmap.get();
        if (width > 0 && height > 0) {
            if (bitmap != null && !bitmap.isRecycled()) {
                canvas.drawBitmap(bitmap, 0.0F, 0.0F, (Paint)null);
            } else {
                try {
                    bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                } catch (OutOfMemoryError var14) {
                    var14.printStackTrace();
                    System.gc();
                }

                if (bitmap != null) {
                    Canvas rCanvas = new Canvas(bitmap);
                    Paint paint = new Paint();
                    paint.setAntiAlias(true);
                    Rect rect = new Rect(0, 0, width, height);
                    Rect rectF = new Rect(1, 1, width - 1, height - 1);
                    BitmapDrawable drawable_in = new BitmapDrawable(bp);
                    drawable_in.setBounds(rectF);
                    drawable_in.draw(rCanvas);
                    if (drawable_bg instanceof NinePatchDrawable) {
                        NinePatchDrawable patchDrawable = (NinePatchDrawable)drawable_bg;
                        patchDrawable.setBounds(rect);
                        Paint maskPaint = patchDrawable.getPaint();
                        maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));
                        patchDrawable.draw(rCanvas);
                    }

                    this.mShardWeakBitmap = new WeakReference(bitmap);
                    canvas.drawBitmap(bitmap, 0.0F, 0.0F, paint);
                }
            }

        }
    }
    public void setImageUrl(String url) {
        this.imageUrl = url;
        if (isAttachedOnWindow){
            if (!TextUtils.isEmpty(this.imageUrl)&&this.imageUrl.equals(CommonUtil.matchUrl(this.imageUrl))) {
                if(this.imageUrl.contains("file://"))
                {
                    ImageLoaderUtil.getImageLoaderInstance().displayImage(this.imageUrl, this, ImageLoaderUtil.getAvatarOptions(corner, defaultImageRes));
                }
                else {
                    ImageLoaderUtil.getImageLoaderInstance().displayImage(this.imageUrl, this, ImageLoaderUtil.getAvatarOptions(corner, defaultImageRes));
                }
            }else{
                String defaultUri="drawable://" + defaultImageRes;
                if(imageId!=0)
                {
                     defaultUri="drawable://" + imageId;
                }
                ImageLoaderUtil.getImageLoaderInstance().displayImage(defaultUri, this, ImageLoaderUtil.getAvatarOptions(corner, defaultImageRes));
            }
        }
    }

    public void setImageId(int id) {
        this.imageId=id;
    }

    public void setDefaultImageRes(int defaultImageRes) {
        this.defaultImageRes = defaultImageRes;

    }

    public void setCorner(int corner) {
        this.corner = corner;
    }

    @Deprecated
    public void loadImage(String imageUrl,ImageSize imageSize,ImageLoaddingCallback imageLoaddingCallback){
        ImageLoaderUtil.getImageLoaderInstance().loadImage(imageUrl,imageSize,new ImageLoaddingListener(imageLoaddingCallback));
    }

    public int getCorner() {
        return corner;
    }

    public int getDefaultImageRes() {
        return defaultImageRes;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        isAttachedOnWindow = true;
        setImageUrl(this.imageUrl);
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.isAttachedOnWindow=false;
        ImageLoaderUtil.getImageLoaderInstance().cancelDisplayTask(this);
    }

    private static class ImageLoaddingListener extends SimpleImageLoadingListener {
        private ImageLoaddingCallback imageLoaddingCallback;

        public ImageLoaddingListener(ImageLoaddingCallback callback) {
            this.imageLoaddingCallback=callback;
        }
        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage){
            imageLoaddingCallback.onLoadingComplete(imageUri,view,loadedImage);
        }

    }

    public interface ImageLoaddingCallback {
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage);
    }
}
