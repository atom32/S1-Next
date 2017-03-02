package me.ykrank.s1next.view.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.SharedElementCallback;
import android.transition.Transition;
import android.view.MenuItem;
import android.view.View;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.disposables.Disposable;
import me.ykrank.s1next.App;
import me.ykrank.s1next.R;
import me.ykrank.s1next.data.api.Api;
import me.ykrank.s1next.data.api.S1Service;
import me.ykrank.s1next.data.api.model.Profile;
import me.ykrank.s1next.databinding.ActivityHomeBinding;
import me.ykrank.s1next.util.ActivityUtils;
import me.ykrank.s1next.util.AnimUtils;
import me.ykrank.s1next.util.L;
import me.ykrank.s1next.util.RxJavaUtil;
import me.ykrank.s1next.util.TransitionUtils;
import me.ykrank.s1next.widget.AppBarOffsetChangedListener;
import me.ykrank.s1next.widget.glide.ImageInfo;
import me.ykrank.s1next.widget.track.event.ViewHomeTrackEvent;
import me.ykrank.s1next.widget.track.event.page.PageEndEvent;
import me.ykrank.s1next.widget.track.event.page.PageStartEvent;

/**
 * Created by ykrank on 2017/1/8.
 */

public class UserHomeActivity extends BaseActivity {

    private static final float PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR = 0.71f;
    private static final int TITLE_ANIMATIONS_DURATION = 300;

    private static final String ARG_UID = "uid";
    private static final String ARG_USERNAME = "username";
    private static final String ARG_IMAGE_INFO = "image_info";

    @Inject
    S1Service s1Service;

    private ActivityHomeBinding binding;
    private Disposable mDisposable;

    public static void start(Context context, String uid, String userName) {
        Intent intent = new Intent(context, UserHomeActivity.class);
        intent.putExtra(ARG_UID, uid);
        intent.putExtra(ARG_USERNAME, userName);
        context.startActivity(intent);
    }

    public static void start(Context context, String uid, String userName, View avatarView) {
        //@see http://stackoverflow.com/questions/31381385/nullpointerexception-drawable-setbounds-probably-due-to-fragment-transitions#answer-31383033
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            start(context, uid, userName);
            return;
        }
        Context baseContext = ActivityUtils.getBaseContext(context);
        if (!(baseContext instanceof Activity)) {
            L.leaveMsg("uid:" + uid);
            L.leaveMsg("userName:" + userName);
            L.report(new IllegalStateException("UserHomeActivity start error: context not instance of activity"));
            return;
        }
        ImageInfo imageInfo = (ImageInfo) avatarView.getTag(R.id.tag_drawable_info);
        Activity activity = (Activity) baseContext;
        Intent intent = new Intent(activity, UserHomeActivity.class);
        intent.putExtra(ARG_UID, uid);
        intent.putExtra(ARG_USERNAME, userName);
        if (imageInfo != null) {
            intent.putExtra(ARG_IMAGE_INFO, imageInfo);
        }
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                activity, avatarView, activity.getString(R.string.transition_avatar));
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.getPrefComponent().inject(this);

        String uid = getIntent().getStringExtra(ARG_UID);
        String name = getIntent().getStringExtra(ARG_USERNAME);
        ImageInfo thumbImageInfo = getIntent().getParcelableExtra(ARG_IMAGE_INFO);
        trackAgent.post(new ViewHomeTrackEvent(uid, name));
        L.leaveMsg("UserHomeActivity##uid:" + uid + ",name:" + name);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_home);
        binding.setDownloadPreferencesManager(mDownloadPreferencesManager);
        binding.setBig(true);
        binding.setPreLoad(true);
        binding.setThumb(thumbImageInfo == null ? null : thumbImageInfo.getUrl());
        Profile profile = new Profile();
        profile.setHomeUid(uid);
        profile.setHomeUsername(name);
        binding.setData(profile);

        binding.appBar.addOnOffsetChangedListener(new AppBarOffsetChangedListener() {
            @Override
            public void onStateChanged(AppBarLayout appBarLayout, int oldVerticalOffset, int verticalOffset) {
                int maxScroll = appBarLayout.getTotalScrollRange();
                float oldPercentage = (float) Math.abs(oldVerticalOffset) / (float) maxScroll;
                float percentage = (float) Math.abs(verticalOffset) / (float) maxScroll;
                if (oldPercentage < PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR && percentage >= PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR) {
                    //Move up
                    AnimUtils.startAlphaAnimation(binding.toolbarTitle, TITLE_ANIMATIONS_DURATION, View.VISIBLE);
                } else if (oldPercentage >= PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR && percentage < PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR) {
                    //Move down
                    AnimUtils.startAlphaAnimation(binding.toolbarTitle, TITLE_ANIMATIONS_DURATION, View.INVISIBLE);
                }
            }
        });

        binding.avatar.setOnClickListener(v -> {
            String bigAvatarUrl = Api.getAvatarBigUrl(uid);
            GalleryActivity.startGalleryActivity(v.getContext(), bigAvatarUrl);
        });

        binding.ivNewPm.setOnClickListener(v -> NewPmActivity.startNewPmActivityForResultMessage(this,
                binding.getData().getHomeUid(), binding.getData().getHomeUsername()));

        binding.tvFriends.setOnClickListener(v -> FriendListActivity.start(this, uid, name));

        binding.tvThreads.setOnClickListener(v -> UserThreadActivity.start(this, uid, name));

        binding.tvReplies.setOnClickListener(v -> UserReplyActivity.start(this, uid, name));

        setupImage();
        loadData();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        trackAgent.post(new PageStartEvent(this, "个人中心-UserHomeActivity"));
    }

    @Override
    protected void onPause() {
        trackAgent.post(new PageEndEvent(this, "个人中心-UserHomeActivity"));
        super.onPause();
    }

    @Override
    public boolean isTranslucent() {
        return true;
    }

    @Override
    protected void onDestroy() {
        RxJavaUtil.disposeIfNotNull(mDisposable);
        super.onDestroy();
    }

    private void setupImage() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getSharedElementEnterTransition().addListener(new TransitionUtils.TransitionListenerAdapter(){
                @Override
                public void onTransitionEnd(Transition transition) {
                    super.onTransitionEnd(transition);
                    binding.setBig(true);
                    binding.setPreLoad(false);
                }
            });
            setEnterSharedElementCallback(new SharedElementCallback() {
                @Override
                public void onSharedElementStart(List<String> sharedElementNames, List<View> sharedElements, List<View> sharedElementSnapshots) {
                    super.onSharedElementStart(sharedElementNames, sharedElements, sharedElementSnapshots);
                    binding.setBig(false);
                    binding.setPreLoad(false);
                }
            });
        }
    }

    private void loadData() {
        RxJavaUtil.disposeIfNotNull(mDisposable);
        mDisposable = s1Service.getProfile(binding.getData().getHomeUid())
                .compose(RxJavaUtil.iOTransformer())
                .subscribe(wrapper -> {
                    binding.setData(wrapper.getData());
                }, L::e);
    }
}