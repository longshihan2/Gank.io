package org.jokar.gankio.model.impl;

import org.jokar.gankio.di.component.network.DaggerDownNetComponent;
import org.jokar.gankio.di.component.network.DaggerDownloadComponent;
import org.jokar.gankio.di.component.network.DownNetComponent;
import org.jokar.gankio.di.module.network.DownNetModule;
import org.jokar.gankio.di.module.network.DownloadModule;
import org.jokar.gankio.model.event.DownloadModel;
import org.jokar.gankio.model.network.down.DownloadProgressListener;
import org.jokar.gankio.model.network.services.DownloadService;
import org.jokar.gankio.utils.FileUtils;
import org.jokar.gankio.utils.Schedulers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import retrofit2.Retrofit;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

import static org.jokar.gankio.utils.Preconditions.checkNotNull;
/**
 * Created by JokAr on 2016/9/24.
 */

public class DownloadModelImpl implements DownloadModel {

    @Inject
    Retrofit mRetrofit;
    @Inject
    DownloadService mDownloadService;
    public DownloadModelImpl(DownloadProgressListener listener) {
        checkNotNull(listener);

        DownNetComponent netComponent = DaggerDownNetComponent.builder()
                .downNetModule(new DownNetModule(listener))
                .build();

        DaggerDownloadComponent.builder()
                .downNetComponent(netComponent)
                .downloadModule(new DownloadModule())
                .build()
                .inject(this);

    }

    @Override
    public void down(String url, File file, DownloadCallBack callBack) {
        checkNotNull(callBack);

        mDownloadService.download(url)
                .compose(Schedulers.applySchedulersIO())
                .map(responseBody ->  responseBody.byteStream())
                .observeOn(rx.schedulers.Schedulers.computation())
                .doOnNext(inputStream -> {

                    try {
                        FileUtils.writeFile(inputStream,file);
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<InputStream>() {
                    @Override
                    public void onCompleted() {
                        callBack.downloadSuccess();
                    }

                    @Override
                    public void onError(Throwable e) {
                        callBack.downloadFail(e);
                    }

                    @Override
                    public void onNext(InputStream inputStream) {

                    }
                });

    }
}
