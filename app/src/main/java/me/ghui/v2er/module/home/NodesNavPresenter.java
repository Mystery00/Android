package me.ghui.v2er.module.home;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import me.ghui.v2er.network.APIService;
import me.ghui.v2er.network.GeneralConsumer;
import me.ghui.v2er.network.bean.NodesNavInfo;

/**
 * Created by ghui on 22/05/2017.
 */

public class NodesNavPresenter implements NodesNavConstract.IPresenter {

    private NodesNavConstract.IView mView;

    public NodesNavPresenter(NodesNavConstract.IView view) {
        mView = view;
    }

    @Override
    public void start() {
        APIService.get().nodesNavInfo()
                .compose(mView.rx())
                .subscribe(new GeneralConsumer<NodesNavInfo>(mView) {
                    @Override
                    public void onConsume(NodesNavInfo items) {
                        items.removeIf(it -> it.getCategory().contains("•"));
                        mView.fillView(items);
                    }
                });
    }

}
