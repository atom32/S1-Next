package me.ykrank.s1next.view.fragment;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.github.ykrank.androidautodispose.AndroidRxDispose;
import com.github.ykrank.androidlifecycle.event.FragmentEvent;

import io.reactivex.Observable;
import me.ykrank.s1next.data.api.ApiFlatTransformer;
import me.ykrank.s1next.util.RxJavaUtil;
import me.ykrank.s1next.view.adapter.BaseRecyclerViewAdapter;
import me.ykrank.s1next.viewmodel.LoadingViewModel;

/**
 * Created by ykrank on 2016/11/12 0012.
 */

public abstract class BaseLoadMoreRecycleViewFragment<D> extends BaseRecyclerViewFragment<D> {

    /**
     * The serialization (saved instance state) Bundle key representing
     * current page num.
     */
    private static final String STATE_PAGE_NUM = "page_num";

    private int mPageNum = 1;
    private int mPageCount;

    @CallSuper
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            mPageNum = 1;
        } else {
            mPageNum = savedInstanceState.getInt(STATE_PAGE_NUM);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getRecyclerView().addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (!isPullUpToRefresh()
                        && mPageNum < mPageCount
                        && !isLoading()
                        && getRecyclerViewAdapter().getItemCount() != 0
                        && !getRecyclerView().canScrollVertically(1)) {
                    startPullUpLoadMore();
                }
            }
        });
    }


    public void startPullUpLoadMore() {
        getRecyclerViewAdapter().showFooterProgress();
        setLoading(LoadingViewModel.LOADING_PULL_UP_TO_REFRESH);
        getRecyclerView().scrollToPosition(getRecyclerViewAdapter().getItemCount());
        loadMore();
    }

    /**
     * Starts to load more data.
     * <p>
     * Subclass should implement {@link BaseRecyclerViewFragment#getSourceObservable(int)}
     * in oder to provider its own data source {@link Observable}.
     */
    private void loadMore() {
        mPageNum++;
        // dismiss Snackbar in order to let user see the ProgressBar
        // when we start to loadViewPager new data
        mCoordinatorLayoutAnchorDelegate.dismissSnackbarIfExist();
        getPageSourceObservable(mPageNum)
                .map(d -> appendNewData(getRetainedFragment().data, d))
                .compose(ApiFlatTransformer.INSTANCE.apiErrorTransformer())
                .compose(RxJavaUtil.iOTransformer())
                .doOnNext(mUserValidator::validateIntercept)
                .doAfterTerminate(this::finallyDo)
                .to(AndroidRxDispose.withObservable(this, FragmentEvent.DESTROY))
                .subscribe(this::onLoadMoreNext, this::onError);
    }

    /**
     * only when loadMore
     */
    private void onLoadMoreNext(D data) {
        onNext(data);
    }

    @Override
    void onError(Throwable throwable) {
        mPageNum--;
        super.onError(throwable);
    }

    @Override
    @CallSuper
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(STATE_PAGE_NUM, mPageNum);
    }

    void setTotalPages(int pageCount) {
        this.mPageCount = pageCount;
    }

    abstract BaseRecyclerViewAdapter getRecyclerViewAdapter();

    public int getPageNum() {
        return mPageNum;
    }

    /**
     * append new load data to old data
     *
     * @param oldData data show in recycleView
     * @param newData new load data
     * @return compound data. not same object of oldData, but could newData
     */
    @NonNull
    abstract D appendNewData(@Nullable D oldData, @NonNull D newData);

    /**
     * Subclass should implement this in order to provider its
     * data source {@link Observable}.
     * <p>
     * The data source {@link Observable} often comes from network
     * or database.
     *
     * @return The data source {@link Observable}.
     */
    abstract Observable<D> getPageSourceObservable(int pageNum);

    @Override
    final Observable<D> getSourceObservable(@LoadingViewModel.LoadingDef int loading) {
        mPageNum = 1;
        return getPageSourceObservable(1);
    }
}
