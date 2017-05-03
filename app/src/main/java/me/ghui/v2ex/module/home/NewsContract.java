package me.ghui.v2ex.module.home;

import me.ghui.v2ex.module.base.BaseContract;
import me.ghui.v2ex.network.bean.NewsInfo;

/**
 * Created by ghui on 03/04/2017.
 */

public class NewsContract {

	public interface IView extends BaseContract.IView {
		void fillView(NewsInfo newsInfos, boolean isLoadMore);
	}

	public interface IPresenter extends BaseContract.IPresenter {
		void loadMore(int page);
	}

}