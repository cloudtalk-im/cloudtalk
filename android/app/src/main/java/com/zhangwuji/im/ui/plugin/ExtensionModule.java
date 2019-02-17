package com.zhangwuji.im.ui.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.zhangwuji.im.ui.plugin.message.LocationRenderView;
import com.zhangwuji.im.ui.plugin.module.AudoPlugin;
import com.zhangwuji.im.ui.plugin.module.LocationPlugin;
import com.zhangwuji.im.ui.plugin.module.TakeCameraPlugin;
import com.zhangwuji.im.ui.plugin.module.TakePhotoPlugin;
import com.zhangwuji.im.ui.plugin.module.VideoPlugin;
import com.zhangwuji.im.ui.plugin.message.BigMojiImageRenderView;

import java.util.ArrayList;
import java.util.List;

public class ExtensionModule {
    public List<IPluginModule> getPluginModules(int conversationType) {
        LocationPlugin locationPlugin=new LocationPlugin();
        TakeCameraPlugin takeCameraPlugin=new TakeCameraPlugin();
        TakePhotoPlugin takePhotoPlugin=new TakePhotoPlugin();
        AudoPlugin audoPlugin=new AudoPlugin();
        VideoPlugin videoPluginn=new VideoPlugin();


        List<IPluginModule> pluginModules =new ArrayList<IPluginModule>();
        pluginModules.add(takeCameraPlugin);
        pluginModules.add(takePhotoPlugin);
        pluginModules.add(videoPluginn);
        pluginModules.add(audoPlugin);
        pluginModules.add(locationPlugin);
        return pluginModules;
    }

    public static void startActivityForPluginResult(Activity activity,Intent intent, int requestCode, IPluginModule pluginModule, int position) {
        if ((requestCode & -256) != 0) {
            throw new IllegalArgumentException("requestCode does not over 255.");
        } else {
            activity.startActivityForResult(intent, (position + 1 << 8) + (requestCode & 255));
        }
    }

    public List<IMessageModule> getMessageModule(Context ctx,int conversationType)
    {
        List<IMessageModule> messageModules=new ArrayList<>();

        BigMojiImageRenderView bigMojiImageRenderView=new BigMojiImageRenderView(ctx,null);
        LocationRenderView locationRenderView=new LocationRenderView(ctx,null);

        messageModules.add(locationRenderView);
        messageModules.add(bigMojiImageRenderView);



        return  messageModules;
    }

}
