package me.ykrank.s1next.data.db;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import me.ykrank.s1next.BuildConfig;

@Module
public class DbModule {

    @Provides
    @Singleton
    AppDaoOpenHelper provideAppDaoOpenHelper(Context context) {
        return new AppDaoOpenHelper(context, BuildConfig.DB_NAME);
    }

    @Provides
    @Singleton
    AppDaoSessionManager provideAppDaoSessionManager(AppDaoOpenHelper helper) {
        return new AppDaoSessionManager(helper);
    }

    @Provides
    @Singleton
    BlackListDbWrapper provideBlackListDbWrapper(AppDaoSessionManager appDaoSessionManager) {
        return new BlackListDbWrapper(appDaoSessionManager);
    }

    @Provides
    @Singleton
    ReadProgressDbWrapper provideReadProgressDbWrapper(AppDaoSessionManager appDaoSessionManager) {
        return new ReadProgressDbWrapper(appDaoSessionManager);
    }

    @Provides
    @Singleton
    ThreadDbWrapper provideThreadDbWrapperr(AppDaoSessionManager appDaoSessionManager) {
        return new ThreadDbWrapper(appDaoSessionManager);
    }

    @Provides
    @Singleton
    HistoryDbWrapper provideHistoryDbWrapper(AppDaoSessionManager appDaoSessionManager) {
        return new HistoryDbWrapper(appDaoSessionManager);
    }
}
