package com.zhangwuji.im.ui.activity;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationClientOption.AMapLocationMode;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.AMapOptions;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.LocationSource;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.SupportMapFragment;
import com.amap.api.maps2d.model.BitmapDescriptor;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.CameraPosition;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.maps2d.model.MyLocationStyle;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.zhangwuji.im.R;
import com.zhangwuji.im.ui.base.TTBaseActivity;
import com.zhangwuji.im.ui.plugin.message.entity.LocationMessage;

/**
 * Created by AMing on 16/5/9.
 * Company RongCloud
 */
public class AMAPLocationActivity extends TTBaseActivity implements View.OnClickListener, LocationSource, GeocodeSearch.OnGeocodeSearchListener, AMapLocationListener, AMap.OnCameraChangeListener {
    static public final int REQUEST_CODE_ASK_PERMISSIONS = 101;
    private MapView mapView;
    private AMap aMap;
   // private LocationManagerProxy mLocationManagerProxy;
    private Handler handler = new Handler();
    private OnLocationChangedListener listener;
    private LatLng myLocation = null;
    private Marker centerMarker;
    private boolean isMovingMarker = false;
    private BitmapDescriptor successDescripter;
    private GeocodeSearch geocodeSearch;
    private LocationMessage mMsg;
    private TextView tvCurLocation;
    private boolean model = false;
    private boolean isPerview;
    public AMapLocationClientOption mLocationOption = null;
    public AMapLocationClient mlocationClient=null;
    public String getTargetId="";
    public boolean isgroup=false;
    public TextView r_bnt;
    @Override
    @TargetApi(23)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutInflater.from(this).inflate(R.layout.activity_amap, topContentView);

        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);

        setRightButton(R.drawable.rc_voip_icon_checkbox_hover);
        setLeftButton(R.drawable.ac_back_icon);
        topLeftBtn.setOnClickListener(this);
        topRightBtn.setOnClickListener(this);
        setTitle(R.string.mylocation);


        if (Build.VERSION.SDK_INT >= 23) {
            int checkPermission = this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            if (checkPermission != PackageManager.PERMISSION_GRANTED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_ASK_PERMISSIONS);
                } else {
                    new AlertDialog.Builder(this)
                    .setMessage("您需要在设置里打开位置权限。")
                    .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_ASK_PERMISSIONS);
                        }
                    })
                    .setNegativeButton("取消", null)
                    .create().show();
                }
                return;
            }
        }


        initUI();
        initAmap();

        setUpLocationStyle();
    }

    private void initAmap() {
        if (aMap == null) {
            aMap = mapView.getMap();
        }

        
        if (getIntent().hasExtra("location") || getIntent().hasExtra("location_lat")) {
            isPerview = true;
            if(getIntent().hasExtra("location_lat"))
            {
            	double latitude= getIntent().getDoubleExtra("location_lat",0.00000);
            	double longitude=getIntent().getDoubleExtra("location_lng",0.00000);
             	String addressName=getIntent().getStringExtra("location_name");
            	mMsg= new LocationMessage(latitude, longitude,addressName, getMapUrl(latitude, longitude));
            }
            else
            {
                mMsg = (LocationMessage) getIntent().getSerializableExtra("location");
            }
            
            tvCurLocation.setVisibility(View.GONE);
            returns.setVisibility(View.GONE);
            topRightBtn.setVisibility(View.GONE);
            
            if (model) {
                CameraPosition location = new CameraPosition.Builder()
                .target(new LatLng(mMsg.getmLat(), mMsg.getmLng())).zoom(18).bearing(0).tilt(30).build();
                show(location);
            } else {
                aMap.addMarker(new MarkerOptions().anchor(0.5f, 0.5f)
                               .position(new LatLng(mMsg.getmLat(), mMsg.getmLng())).title(mMsg.getmPoi())
                               .snippet(mMsg.getmLat() + "," + mMsg.getmLng()).draggable(false));
                aMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                                .target(new LatLng(mMsg.getmLat(), mMsg.getmLng())).zoom(16).bearing(0).tilt(30).build()));
            }
            return;
        }


      getTargetId=this.getIntent().getStringExtra("targetId");
      isgroup=this.getIntent().getBooleanExtra("isgroup", false);
        
      mlocationClient = new AMapLocationClient(this);
      //初始化定位参数
      mLocationOption = new AMapLocationClientOption();
      //设置定位监听
      mlocationClient.setLocationListener(this);
      //设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
      mLocationOption.setLocationMode(AMapLocationMode.Hight_Accuracy);
      //设置定位间隔,单位毫秒,默认为2000ms
      mLocationOption.setInterval(2000);
      //设置定位参数
      mlocationClient.setLocationOption(mLocationOption);
      // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
      // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
      // 在定位结束后，在合适的生命周期调用onDestroy()方法
      // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
      //启动定位
      mlocationClient.startLocation();
      

        aMap.setLocationSource(this);// 设置定位监听
        aMap.setMyLocationEnabled(true);
        aMap.getUiSettings().setZoomControlsEnabled(false);
        aMap.getUiSettings().setMyLocationButtonEnabled(false);
        CameraUpdate cameraUpdate = CameraUpdateFactory.zoomTo(15);//设置缩放监听
        aMap.moveCamera(cameraUpdate);

        successDescripter = BitmapDescriptorFactory.fromResource(R.drawable.icon_usecarnow_position_succeed);
        geocodeSearch = new GeocodeSearch(this);
        geocodeSearch.setOnGeocodeSearchListener(this);
        
  
        
    }

    private static final String MAP_FRAGMENT_TAG = "map";
    private SupportMapFragment aMapFragment;


    private void show(CameraPosition location) {
        AMapOptions aOptions = new AMapOptions();
        aOptions.zoomGesturesEnabled(true);
        aOptions.scrollGesturesEnabled(false);

        aOptions.camera(location);

        if (aMapFragment == null) {
            aMapFragment = SupportMapFragment.newInstance(aOptions);
            FragmentTransaction fragmentTransaction = getSupportFragmentManager()
                    .beginTransaction();
            fragmentTransaction.add(android.R.id.content, aMapFragment,
                                    MAP_FRAGMENT_TAG);
            fragmentTransaction.commit();
        }
    }

    private ImageView returns;

    private void initUI() {
        returns = (ImageView) findViewById(R.id.myLocation);
        returns.setOnClickListener(this);
        tvCurLocation = (TextView) findViewById(R.id.location);
    }


    @SuppressLint("NewApi") @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.myLocation:
                if (myLocation != null) {
                    CameraUpdate update = CameraUpdateFactory.changeLatLng(myLocation);
                    aMap.animateCamera(update);
                }
                break;
            case R.id.right_btn:
            	 if (mMsg != null) {
                     if (Build.VERSION.SDK_INT >= 23) {
                         int checkPermission = this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                         if (checkPermission != PackageManager.PERMISSION_GRANTED) {
                             if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                                 requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_ASK_PERMISSIONS);
                             } else {
                                 new AlertDialog.Builder(this)
                                 .setMessage("您需要在设置里打开存储权限。")
                                 .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                                     @SuppressLint("NewApi") @Override
                                     public void onClick(DialogInterface dialog, int which) {
                                         requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_ASK_PERMISSIONS);
                                     }
                                 })
                                 .setNegativeButton("取消", null)
                                 .create().show();
                             }
                         }
                     }

                     Intent intent2 = new Intent();
                     intent2.putExtra("latitude", mMsg.getmLat());
                     intent2.putExtra("longitude", mMsg.getmLng());
                     intent2.putExtra("address",mMsg.getmPoi());
                     intent2.putExtra("locuri",getMapUrl(mMsg.getmLat(),mMsg.getmLng()));
                     setResult(Activity.RESULT_OK , intent2);
                     finish();
                     
                 } else {
                	finish();
                 }
                break;
            case R.id.left_btn:
                finish();
                break;
            default:
                break;
        }
    }

    @Override
    public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {
            if (regeocodeResult != null && regeocodeResult.getRegeocodeAddress() != null) {
                endAnim();
                centerMarker.setIcon(successDescripter);
                RegeocodeAddress regeocodeAddress = regeocodeResult.getRegeocodeAddress();
                String formatAddress = regeocodeResult.getRegeocodeAddress().getFormatAddress();
                String shortAdd = formatAddress.replace(regeocodeAddress.getProvince(), "").replace(regeocodeAddress.getCity(), "").replace(regeocodeAddress.getDistrict(), "");
                tvCurLocation.setText(shortAdd);
                double latitude = regeocodeResult.getRegeocodeQuery().getPoint().getLatitude();
                double longitude = regeocodeResult.getRegeocodeQuery().getPoint().getLongitude();
                mMsg =new LocationMessage(latitude, longitude, shortAdd, getMapUrl(latitude, longitude));

            } else {

                Toast.makeText(AMAPLocationActivity.this,"没有搜索到结果",Toast.LENGTH_LONG).show();
            }
    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

    }


    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation != null && aMapLocation.getErrorCode() == 0) {
            if (listener != null) {
                listener.onLocationChanged(aMapLocation);// 显示系统小蓝点
            }
            
            mlocationClient.stopLocation();
            myLocation = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude());//获取当前位置经纬度
            tvCurLocation.setText(aMapLocation.getRoad() + aMapLocation.getStreet() + aMapLocation.getPoiName());//当前位置信息

            double latitude = aMapLocation.getLatitude();
            double longitude = aMapLocation.getLongitude();
            
            mMsg = new LocationMessage(latitude, longitude, aMapLocation.getRoad() + aMapLocation.getStreet() + aMapLocation.getPoiName(), getMapUrl(latitude, longitude));
            



            addChooseMarker();
            
            CameraUpdate update = CameraUpdateFactory.changeLatLng(myLocation);
            aMap.animateCamera(update);

        }
    }

    private void addChooseMarker() {
        //加入自定义标签
        MarkerOptions centerMarkerOption = new MarkerOptions().position(myLocation).icon(successDescripter);
        centerMarker = aMap.addMarker(centerMarkerOption);
        centerMarker.setPositionByPixels(mapView.getWidth() / 2, mapView.getHeight() / 2);
        aMap.setOnCameraChangeListener(AMAPLocationActivity.this);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                CameraUpdate update = CameraUpdateFactory.zoomTo(17f);
                aMap.animateCamera(update, 1000, new AMap.CancelableCallback() {
                    @Override
                    public void onFinish() {
                        aMap.setOnCameraChangeListener(AMAPLocationActivity.this);
                    }

                    @Override
                    public void onCancel() {
                    }
                });
            }
        }, 1000);
    }

    private void setMovingMarker() {
        if (isMovingMarker)
            return;

        isMovingMarker = true;
        centerMarker.setIcon(successDescripter);
        hideLocationView();
    }


    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        if (centerMarker != null) {
            setMovingMarker();
        }
    }

    @Override
    public void onCameraChangeFinish(CameraPosition cameraPosition) {
        LatLonPoint point = new LatLonPoint(cameraPosition.target.latitude, cameraPosition.target.longitude);
        RegeocodeQuery query = new RegeocodeQuery(point, 50, GeocodeSearch.AMAP);
        geocodeSearch.getFromLocationAsyn(query);
        if (centerMarker != null) {
            animMarker();
        }
        showLocationView();
    }



    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        deactivate();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
    }

    private ValueAnimator animator = null;

    private void animMarker() {
        isMovingMarker = false;
        if (animator != null) {
            animator.start();
            return;
        }
        animator = ValueAnimator.ofFloat(mapView.getHeight() / 2, mapView.getHeight() / 2 - 30);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.setDuration(150);
        animator.setRepeatCount(1);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Float value = (Float) animation.getAnimatedValue();
                centerMarker.setPositionByPixels(mapView.getWidth() / 2, Math.round(value));
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                centerMarker.setIcon(successDescripter);
            }
        });
        animator.start();
    }

    private void endAnim() {
        if (animator != null && animator.isRunning())
            animator.end();
    }

    private void hideLocationView() {
        ObjectAnimator animLocation = ObjectAnimator.ofFloat(tvCurLocation, "TranslationY", -tvCurLocation.getHeight() * 2);
        animLocation.setDuration(200);
        animLocation.start();
    }

    private void showLocationView() {
        ObjectAnimator animLocation = ObjectAnimator.ofFloat(tvCurLocation, "TranslationY", 0);
        animLocation.setDuration(200);
        animLocation.start();
    }

    private void setUpLocationStyle() {
        // 自定义系统定位蓝点
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.
                                       fromResource(R.drawable.img_location_now));
        myLocationStyle.strokeWidth(0);
        myLocationStyle.strokeColor(R.color.main_theme_color);
        myLocationStyle.radiusFillColor(Color.TRANSPARENT);
        aMap.setMyLocationStyle(myLocationStyle);
    }
    private String getMapUrl(double latitude, double longitude) {
        String url="http://restapi.amap.com/v3/staticmap?location=" + longitude + "," + latitude
                + "&zoom=16&scale=2&size=408*240&markers=mid,,A:" + longitude + "," + latitude + "&key="
                + "e09af6a2b26c02086e9216bd07c960ae";
        return url;
    }
    private Uri getMapUrl2(double latitude, double longitude) {
        String url="http://restapi.amap.com/v3/staticmap?location=" + longitude + "," + latitude
		+ "&zoom=16&scale=2&size=408*240&markers=mid,,A:" + longitude + "," + latitude + "&key="
		+ "e09af6a2b26c02086e9216bd07c960ae";
        return Uri.parse(url);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    if (permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        initUI();
                        initAmap();
                        setUpLocationStyle();
                    }
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

	@Override
	public void activate(OnLocationChangedListener arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deactivate() {
		// TODO Auto-generated method stub
		
	}
    
}
