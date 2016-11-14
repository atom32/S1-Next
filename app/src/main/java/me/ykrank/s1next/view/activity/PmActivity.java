package me.ykrank.s1next.view.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;

import me.ykrank.s1next.R;
import me.ykrank.s1next.data.api.model.PmGroup;
import me.ykrank.s1next.data.event.PmGroupClickEvent;
import me.ykrank.s1next.util.RxJavaUtil;
import me.ykrank.s1next.view.fragment.PmFragment;
import me.ykrank.s1next.view.fragment.PmGroupsFragment;
import rx.Subscription;


public class PmActivity extends BaseActivity {

    private Subscription mSubscription;

    private Fragment fragment;

    public static void startPmActivity(Context context) {
        Intent intent = new Intent(context, PmActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base_without_drawer_and_scrolling_effect);

        if (savedInstanceState == null) {
            fragment = PmGroupsFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.frame_layout, fragment, PmGroupsFragment.TAG)
                    .commit();
        }

        mSubscription = mEventBus.get()
                .subscribe(o -> {
                    if (o instanceof PmGroupClickEvent) {
                        PmGroupClickEvent event = (PmGroupClickEvent) o;
                        PmGroup pmGroup = event.getPmGroup();
                        fragment = PmFragment.newInstance(pmGroup.getToUid(), pmGroup.getToUsername());
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.frame_layout, fragment, PmFragment.TAG)
                                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                                .addToBackStack(null)
                                .commit();
                    }
                });
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
    protected void onDestroy() {
        RxJavaUtil.unsubscribeIfNotNull(mSubscription);
        super.onDestroy();
    }
}