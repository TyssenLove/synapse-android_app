package tech.synapsenetwork.app.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;

import tech.synapsenetwork.app.R;
import tech.synapsenetwork.app.entity.Wallet;
import tech.synapsenetwork.app.router.HomeRouter;
import tech.synapsenetwork.app.router.ManageWalletsRouter;
import tech.synapsenetwork.app.router.TransactionsRouter;
import tech.synapsenetwork.app.viewmodel.SplashViewModel;
import tech.synapsenetwork.app.viewmodel.SplashViewModelFactory;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.fabric.sdk.android.Fabric;
import tech.synapsenetwork.app.BuildConfig;

public class SplashActivity extends AppCompatActivity {

    @Inject
    SplashViewModelFactory splashViewModelFactory;
    SplashViewModel splashViewModel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_splash);
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        Fabric.with(this, new Crashlytics.Builder()
                .core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build()).build());

        splashViewModel = ViewModelProviders.of(this, splashViewModelFactory)
                .get(SplashViewModel.class);
        splashViewModel.wallets().observe(this, this::onWallets);
    }

    private void onWallets(Wallet[] wallets) {
        // Start home activity
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (wallets.length == 0) {
                    new ManageWalletsRouter().open(getApplication(), true);
                } else {
                    new HomeRouter().open(getApplication(), true);
                }
            }
        }, 500);

    }

}
