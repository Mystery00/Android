package me.ghui.v2er.module.drawer.care;

import com.orhanobut.logger.Logger;

import me.ghui.v2er.network.APIService;

/**
 * Created by ghui on 27/03/2017.
 */

public class SpecialCarePresenter implements SpecialCareContract.IPresenter {

    private SpecialCareContract.IView mView;

    public SpecialCarePresenter(SpecialCareContract.IView spcialCareView) {
        this.mView = spcialCareView;
    }

    @Override
    public void start() {
        loadMore(1);
    }

    @Override
    public void loadMore(int page) {
        APIService.get().specialCareInfo(page)
                .compose(mView.rx())
                .subscribe(careInfo -> {
                    Logger.d("CareInfo: " + careInfo);
                    mView.fillView(careInfo, page > 1);
                });
    }
}